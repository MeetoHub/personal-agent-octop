"""人际关系工具：让管家把"谁是谁"记下来，下次直接用。

只在根 agent / 团队主持上挂载。写入走 ``RelationStore``，同一人名重复提起只更新，
所以模型可以放心地每次听到就记一遍。
"""

from __future__ import annotations

import asyncio
import json
from typing import Annotated

from langchain_core.tools import StructuredTool
from langgraph.config import get_config
from pydantic import Field

from octop.infra.relations.store import RelationStore

RELATION_REMEMBER = "relation_remember"
RELATION_LOOKUP = "relation_lookup"


def _owner() -> int:
    ctx = dict(get_config().get("configurable") or {})
    raw = ctx.get("user")
    if raw is None:
        raise ValueError("missing configurable.user")
    return int(raw)


def _error(exc: Exception) -> str:
    return json.dumps({"ok": False, "error": str(exc)}, ensure_ascii=False)


def build_relation_tools(*, store: RelationStore, agent_id: str) -> list[StructuredTool]:
    async def relation_remember(
        person: Annotated[str, Field(description="人名或称呼，例如「张总」「我老婆」")],
        relation: Annotated[str, Field(description="和用户的关系，例如「老板」「妻子」")],
        notes: Annotated[str, Field(description="补充信息，可空，例如「偏好邮件沟通」")] = "",
    ) -> str:
        try:
            saved = await asyncio.to_thread(
                store.upsert,
                owner_user_id=_owner(),
                person=person,
                relation=relation,
                notes=notes,
                source_agent_id=agent_id,
            )
            return json.dumps({"ok": True, "relation": saved.to_dict()}, ensure_ascii=False)
        except Exception as exc:  # noqa: BLE001
            return _error(exc)

    async def relation_lookup(
        person: Annotated[str, Field(description="要查的人名；留空则列出全部关系")] = "",
    ) -> str:
        try:
            rows = await asyncio.to_thread(store.lookup, _owner(), person)
            return json.dumps(
                {"ok": True, "relations": [r.to_dict() for r in rows]}, ensure_ascii=False
            )
        except Exception as exc:  # noqa: BLE001
            return _error(exc)

    return [
        StructuredTool.from_function(
            coroutine=relation_remember,
            name=RELATION_REMEMBER,
            description=(
                "Remember who someone is to the user (name → relationship). Call it whenever "
                "the user mentions a person's role, family tie or preference; repeats update "
                "the same entry instead of duplicating."
            ),
        ),
        StructuredTool.from_function(
            coroutine=relation_lookup,
            name=RELATION_LOOKUP,
            description=(
                "Look up saved relationships by name, or list them all when the name is empty. "
                "Use it before asking the user 'who is X?' again."
            ),
        ),
    ]
