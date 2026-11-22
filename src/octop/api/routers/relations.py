"""人际关系 REST：给管理端展示"它记得谁"。"""

from __future__ import annotations

import asyncio
from typing import Any

from fastapi import APIRouter, Depends, Query

from octop.api.deps import current_user, get_server
from octop.infra.relations.store import RelationStore

router = APIRouter()


@router.get("/relations", summary="List my saved relationships")
async def list_relations(
    person: str | None = Query(default=None, description="按人名模糊过滤"),
    limit: int = Query(default=200, ge=1, le=500),
    user: Any = Depends(current_user),
    server: Any = Depends(get_server),
) -> dict[str, Any]:
    """只返回当前用户自己的关系簿；管理员也没有跨用户视角。"""
    store = RelationStore(server.services.db)
    rows = await asyncio.to_thread(store.lookup, user.id, person or "", limit=limit)
    return {"items": [row.to_dict() for row in rows]}
