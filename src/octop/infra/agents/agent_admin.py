"""超级智能体的「招人」工具。

只在两种 agent 上挂载：团队主持（`kind=team`）和主智能体（`config.is_primary`）。
它们代表一个用户去创建**有记忆的队员 agent**，队员再带自己的子智能体。

约束（刻意写死，不做成配置项）：
- 只能建在调用者所属用户名下，跨用户建人不给开口子；
- 团队人数有上限，防止模型无限招人；
- 建出来的 agent 在 config 里记 `created_by_agent_id`，团队页据此展示血缘。
"""

from __future__ import annotations

import json
from collections.abc import Awaitable, Callable
from typing import Annotated, Any

from langchain_core.tools import StructuredTool
from langgraph.config import get_config
from pydantic import Field

MAX_TEAM_MEMBERS = 20

AGENT_CREATE = "agent_create"


def build_agent_admin_tools(
    *,
    create_agent: Callable[[Any], Awaitable[Any]],
    count_agents: Callable[[int], int],
    caller_agent_id: str,
    max_members: int = MAX_TEAM_MEMBERS,
) -> list[StructuredTool]:
    """``create_agent`` 收一个 ``AgentCreateSpec``；这里只负责拼参数与护栏。"""

    async def agent_create(
        name: Annotated[str, Field(description="队员的名字，2-12 个字，例如「竞品分析师」")],
        description: Annotated[str, Field(description="一句话说清它负责什么")] = "",
        system_prompt: Annotated[
            str, Field(description="它的人格与工作方式；留空则用默认人格")
        ] = "",
    ) -> str:
        try:
            ctx = dict(get_config().get("configurable") or {})
            raw_owner = ctx.get("user")
            if raw_owner is None:
                raise ValueError("missing configurable.user")
            owner = int(raw_owner)
            if count_agents(owner) >= max_members:
                raise ValueError(f"团队人数已达上限 {max_members}，先停用或删除一些队员")

            clean_name = name.strip()
            if not clean_name:
                raise ValueError("name must not be empty")

            # 延迟导入：manager 会 import 本模块，避免循环
            from octop.infra.agents.manager import AgentCreateSpec  # noqa: PLC0415

            spec = AgentCreateSpec(
                name=clean_name,
                user_id=owner,
                description=description.strip() or None,
                system_prompt=system_prompt.strip() or None,
                config={"created_by_agent_id": caller_agent_id},
            )
            row = await create_agent(spec)
            return json.dumps(
                {
                    "ok": True,
                    "agentId": row.agent_id,
                    "name": row.name,
                    "createdBy": caller_agent_id,
                },
                ensure_ascii=False,
            )
        except Exception as exc:  # noqa: BLE001 - 工具永不把异常抛进 harness
            return json.dumps({"ok": False, "error": str(exc)}, ensure_ascii=False)

    return [
        StructuredTool.from_function(
            coroutine=agent_create,
            name=AGENT_CREATE,
            description=(
                "Create a persistent teammate agent (with its own memory) under the current "
                "user. The teammate can then be addressed with ask_agent. Use it when a kind "
                "of work will repeat and deserves a dedicated expert."
            ),
        ),
    ]
