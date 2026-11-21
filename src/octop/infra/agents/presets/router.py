"""Router 预设：主会话只带"判断 + 派活"用的小工具集。

对应 DSH `docs/003-execution.md`：

    The main Session uses dsh-superagent-router ... with a smaller tool set for routing.
    Each Ticket gets an Executor Session named worker-<ticket id> ... contains the tools
    needed for work.

落地方式刻意做到**零核心改动**：这里只算一份 ``tools_disabled`` 写进专家 config，
现有的 ``Manager.sync_effective_tools_disabled()`` 会把它同步给运行中的 harness。

工具目录默认从 ``tool_catalog`` 读；测试可以显式传进来，避免拉起整个依赖树。
"""

from __future__ import annotations

from collections.abc import Iterable, Mapping
from typing import Any

# Router 需要且只需要这些：看专家、派活、建单/报进度、追问、查记忆。
#
# agent_list / ask_agent 由 PeerAgentMiddleware 提供（team_enabled=True 时才存在），
# ticket_* 由 infra.tickets.tools 注册，其余是内置工具。
ROUTER_TOOLS: frozenset[str] = frozenset(
    {
        "agent_list",
        "ask_agent",
        "ask_user_question",
        "memory_search",
        "ticket_create",
        "ticket_list",
        "ticket_report",
    }
)


def router_tools() -> frozenset[str]:
    """Router 应当保留的工具名。"""
    return ROUTER_TOOLS


def router_tools_disabled(
    *,
    catalog: Iterable[str] | None = None,
    critical: Iterable[str] | None = None,
) -> list[str]:
    """catalog 里除白名单外的全部工具名，可直接写进 ``config['tools_disabled']``。

    只返回 catalog 内的名字：不在 catalog 的插件/工单/手机工具本来就不受它控制，
    也不能被误关。``CRITICAL_TOOLS`` 会被 ``effective_tools_disabled`` 再减一次，
    这里提前剔除只是让结果更接近最终生效值。
    """
    if catalog is None:
        from octop.infra.agents.settings.tool_catalog import (  # noqa: PLC0415
            BUILTIN_TOOL_CATALOG,
            CRITICAL_TOOLS,
        )

        names = {entry.name for entry in BUILTIN_TOOL_CATALOG}
        protected = set(CRITICAL_TOOLS) if critical is None else set(critical)
    else:
        names = set(catalog)
        protected = set(critical or ())
    return sorted(names - ROUTER_TOOLS - protected)


def is_router_agent(cfg: Mapping[str, Any]) -> bool:
    """专家 config 里是否打了 router 标记。"""
    return bool(cfg.get("is_router"))


def apply_router_preset(
    cfg: dict[str, Any],
    *,
    catalog: Iterable[str] | None = None,
    critical: Iterable[str] | None = None,
) -> dict[str, Any]:
    """原地给专家 config 打上 router 标记与禁用清单，返回同一个 dict。

    调用方负责持久化（``persist_tools_disabled`` 或直接写 ``config_json``）。
    """
    cfg["is_router"] = True
    # 保留用户可能手工加过的其它禁用项，只做并集，避免把别人的设置抹掉
    existing = cfg.get("tools_disabled")
    current = {str(name) for name in existing} if isinstance(existing, list) else set()
    disabled = router_tools_disabled(catalog=catalog, critical=critical)
    cfg["tools_disabled"] = sorted(current | set(disabled))
    return cfg
