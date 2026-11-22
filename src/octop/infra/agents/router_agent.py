"""平台内建的调度层（Router）。

它不是一个"用户配置的专家"：

- 没有头像入口、不出现在专家/团队列表里（`config.hidden`）；
- 不承担人格与长期记忆，只做判断：这句话谁来处理；
- 平台第一次被使用时自动创建，用户不需要也不应该配置它。

用户看到的所有智能体（生活管家、运维工程师……）都是 Worker。
Router 的职责只有三件：直接回答、派给某个 Worker、先建一个 Worker 再派。
"""

from __future__ import annotations

import json
import logging
from typing import Any

logger = logging.getLogger(__name__)

ROUTER_AGENT_NAME = "Octop"

ROUTER_SYSTEM_PROMPT = """你是 Octop 的调度层，用户看不到你，也不应该察觉到你存在。

判断用户这句话怎么处理：
1. 闲聊、或者一两句就能答完的 → 直接回答。
2. 某位专家更合适 → 用 ask_agent 派给他，再把他的回答用自己的话转述给用户。
3. 缺一个长期专门做某类事的人 → 先用 agent_create 建一个，再 ask_agent 派活。
4. 需要多步完成、要花时间的 → 用 ticket_create 建单（能判断执行者就顺手指派）。

说话要求：不要提"调度""路由""我派给"这类内部词；用自然口吻把结果讲给用户，
就像一直是他本人在处理。"""


def _cfg(row: Any) -> dict[str, Any]:
    try:
        data = json.loads(getattr(row, "config_json", "") or "{}")
    except (TypeError, ValueError):
        return {}
    return data if isinstance(data, dict) else {}


def find_router_agent(repo: Any, user_id: int) -> Any | None:
    """在用户名下找调度层。按 config 标记找，不依赖固定 id。"""
    try:
        rows = repo.list_by_user(int(user_id))
    except Exception:  # noqa: BLE001
        return None
    for row in rows:
        if _cfg(row).get("is_system_router"):
            return row
    return None


async def ensure_router_agent(manager: Any, user_id: int) -> Any:
    """取调度层；没有就建一个。并发下可能建两个，取先查到的那个即可。"""
    repo = manager._repos.agent_repo
    existing = find_router_agent(repo, user_id)
    if existing is not None:
        return existing

    from octop.infra.agents.manager import AgentCreateSpec  # noqa: PLC0415
    from octop.infra.agents.teams.service import HOST_TOOLS_DISABLED  # noqa: PLC0415

    spec = AgentCreateSpec(
        name=ROUTER_AGENT_NAME,
        user_id=int(user_id),
        description="",
        system_prompt=ROUTER_SYSTEM_PROMPT,
        config={
            "is_system_router": True,
            "hidden": True,
            # 只留派活与记忆检索；agent_create / ticket_* 不在 catalog 里，不会被这份清单关掉
            "tools_disabled": sorted(HOST_TOOLS_DISABLED),
        },
    )
    logger.info("creating system router agent for user %s", user_id)
    return await manager.create(spec)
