"""规则层第一步：明显是长任务的用户消息，自动开一张工单。

DSH 的 Router 分三档，这里是第①档里唯一会写数据的动作：
纯规则、0 token、不经过模型。`is_long_task` 本身"不确定返 False"，
所以宁可少建，也不要把一句闲聊变成工单。

去重：同一条会话里已经有一张没关的工单就不再开——用户接着追问细节时，
不应该每次追问都多一条「活」。
"""

from __future__ import annotations

import json
import logging
from typing import Any

from octop.infra.tickets.stage import is_long_task
from octop.infra.tickets.store import Ticket, TicketStore

logger = logging.getLogger(__name__)

MAX_TITLE = 60
MAX_GOAL = 1000


def ticket_title(text: str) -> str:
    """第一行非空文本，截到 60 字。"""
    first = next((line.strip() for line in text.splitlines() if line.strip()), "")
    return first[:MAX_TITLE] or "未命名任务"


def should_auto_open(text: str) -> bool:
    """只有真像长任务的才建单。空文本、单句寒暄一律不建。"""
    trimmed = text.strip()
    return bool(trimmed) and is_long_task(trimmed)


def open_ticket_for_turn(
    store: TicketStore,
    *,
    owner_user_id: int,
    agent_id: str,
    thread_id: str | None,
    text: str,
    assignee_id: str | None = None,
) -> Ticket | None:
    """Best-effort：任何异常都吞掉，绝不让建单失败影响对话本身。

    ``assignee_id`` 有值就顺手派给队员（assignment_kind=expert），这样「活」页
    一出现就是"已有人在做"，而不是一张没人认领的单。
    """
    if not should_auto_open(text):
        return None
    try:
        if thread_id:
            rows = store.list_for_owner(owner_user_id, agent_id=agent_id, limit=50)
            if any(row.thread_id == thread_id and row.ended_at is None for row in rows):
                return None
        ticket = store.create(
            owner_user_id=owner_user_id,
            agent_id=agent_id,
            thread_id=thread_id,
            title=ticket_title(text),
            goal=text.strip()[:MAX_GOAL],
            actor="router",
        )
        if assignee_id:
            ticket = store.assign(
                ticket.ticket_id,
                owner_user_id=owner_user_id,
                assignment_kind="expert",
                assignee_id=assignee_id,
                actor="router",
            )
        return ticket
    except Exception:  # noqa: BLE001 - 规则层不该让对话失败
        logger.exception("auto-open ticket failed agent=%s thread=%s", agent_id, thread_id)
        return None


def pick_worker(
    server: Any,
    *,
    agent_id: str,
    user_id: int,
    cfg: dict[str, Any] | None = None,
) -> str | None:
    """给工单挑一个执行者：先看专家配置，再找"这个主智能体招的第一个队员"。"""
    explicit = (cfg or {}).get("default_worker_agent_id")
    if isinstance(explicit, str) and explicit.strip():
        return explicit.strip()
    try:
        rows = server.services.repos.agent_repo.list_by_user(int(user_id))
    except Exception:  # noqa: BLE001
        return None
    for row in rows:
        if str(getattr(row, "agent_id", "")) == agent_id:
            continue
        try:
            row_cfg = json.loads(getattr(row, "config_json", "") or "{}")
        except (TypeError, ValueError):
            continue
        if isinstance(row_cfg, dict) and row_cfg.get("created_by_agent_id") == agent_id:
            return str(row.agent_id)
    return None
