"""工单工具：让专家自己建单、看单、报进度。

和 ``infra/phone/tools.py`` 同款：``StructuredTool`` + ``configurable`` 里的当前用户。
纯 I/O，异常一律转成 JSON 错误，不往 harness 里抛。

store 是同步 SQLite 封装，这里统一走 ``asyncio.to_thread``，别堵事件循环。
"""

from __future__ import annotations

import asyncio
import json
from typing import Annotated, Any

from langchain_core.tools import StructuredTool
from langgraph.config import get_config
from pydantic import Field

from octop.config import OctopConfig
from octop.infra.tickets.stage import REPORTS
from octop.infra.tickets.store import Ticket, TicketStore

TICKET_CREATE = "ticket_create"
TICKET_LIST = "ticket_list"
TICKET_REPORT = "ticket_report"

_REPORT_HELP = " / ".join(REPORTS)


def _ctx() -> dict[str, Any]:
    return dict(get_config().get("configurable") or {})


def _owner(ctx: dict[str, Any]) -> int:
    raw = ctx.get("user")
    if raw is None:
        raise ValueError("missing configurable.user")
    return int(raw)


def _ticket_payload(ticket: Ticket) -> dict[str, Any]:
    return {
        "ticket_id": ticket.ticket_id,
        "title": ticket.title,
        "stage": ticket.stage,
        "goal": ticket.goal,
        "latest_report": ticket.latest_report,
        "updated_at": ticket.updated_at,
    }


def _error(exc: Exception) -> str:
    return json.dumps({"ok": False, "error": str(exc)}, ensure_ascii=False)


def build_ticket_tools(
    config: OctopConfig,
    *,
    store: TicketStore,
    agent_id: str,
) -> list[StructuredTool]:
    """当前专家的工单工具。``config`` 只为与其它工具保持同样的签名。"""
    _ = config

    async def ticket_create(
        title: Annotated[str, Field(description="一句话说清要做什么")],
        goal: Annotated[str, Field(description="完成标准或背景，可空")] = "",
        priority: Annotated[int, Field(description="0 普通，数字越大越急", ge=0, le=9)] = 0,
    ) -> str:
        try:
            owner = _owner(_ctx())
            ticket = await asyncio.to_thread(
                store.create,
                owner_user_id=owner,
                agent_id=agent_id,
                title=title,
                goal=goal,
                priority=priority,
                actor="agent",
            )
            return json.dumps({"ok": True, "ticket": _ticket_payload(ticket)}, ensure_ascii=False)
        except Exception as exc:  # noqa: BLE001 - 工具永不抛进 harness
            return _error(exc)

    async def ticket_list(
        stages: Annotated[
            str,
            Field(
                description=(
                    "逗号分隔的 stage 过滤，例如 person/review,working；留空表示全部。"
                    "七态：person/delivery person/answer person/review person/blocked "
                    "working/queued working idle"
                )
            ),
        ] = "",
        limit: Annotated[int, Field(description="最多返回几条", ge=1, le=100)] = 20,
    ) -> str:
        try:
            owner = _owner(_ctx())
            wanted = [s.strip() for s in stages.split(",") if s.strip()] or None
            rows = await asyncio.to_thread(store.list_for_owner, owner, stages=wanted, limit=limit)
            return json.dumps(
                {"ok": True, "tickets": [_ticket_payload(t) for t in rows]},
                ensure_ascii=False,
            )
        except Exception as exc:  # noqa: BLE001
            return _error(exc)

    async def ticket_report(
        ticket_id: Annotated[str, Field(description="工单 id")],
        report: Annotated[str, Field(description=f"进度：{_REPORT_HELP}")],
        detail: Annotated[str, Field(description="给人看的一句话进度说明")] = "",
    ) -> str:
        try:
            owner = _owner(_ctx())
            ticket = await asyncio.to_thread(
                store.file_report,
                ticket_id,
                owner_user_id=owner,
                report=report,
                actor="agent",
                detail=detail,
            )
            return json.dumps({"ok": True, "ticket": _ticket_payload(ticket)}, ensure_ascii=False)
        except Exception as exc:  # noqa: BLE001
            return _error(exc)

    return [
        StructuredTool.from_function(
            coroutine=ticket_create,
            name=TICKET_CREATE,
            description=(
                "Create a work ticket for a multi-step task you are delegating or will do "
                "later. Returns the ticket id and its stage."
            ),
        ),
        StructuredTool.from_function(
            coroutine=ticket_list,
            name=TICKET_LIST,
            description="List the current user's tickets, optionally filtered by stage.",
        ),
        StructuredTool.from_function(
            coroutine=ticket_report,
            name=TICKET_REPORT,
            description=(
                "Record progress on a ticket. Use 'progress' when starting, 'waiting' when "
                "blocked on the user, 'ready' when done and awaiting review."
            ),
        ),
    ]
