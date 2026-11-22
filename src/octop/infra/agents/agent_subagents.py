"""子智能体安装工具（模型侧）。

Octop 的子智能体是**无记忆的提示词包**（`agents/<slug>.md`），装在 agent 自己的
工作区里，跟"有记忆的队员 agent"是两回事——这正好对应产品里的两层：

- 队员（agent）：有记忆、能长期成长；
- 子智能体（subagent）：同一个 agent 下的一组专业提示词，用完即走、不留记忆。

这里只让 agent 给自己装，不给跨 agent 装的口子。
"""

from __future__ import annotations

import json
from collections.abc import Awaitable, Callable
from pathlib import Path
from typing import Annotated, Any

from langchain_core.tools import StructuredTool
from pydantic import Field

SUBAGENT_INSTALL = "subagent_install"


def _catalog_root() -> Path:
    from octop.infra.agents import subagents as pkg  # noqa: PLC0415

    return Path(pkg.__file__).resolve().parent


def build_subagent_tools(
    *,
    workspace_for: Callable[[str], Any],
    reload_agent: Callable[[str], Awaitable[None]],
    caller_agent_id: str,
) -> list[StructuredTool]:
    async def subagent_install(
        slug: Annotated[
            str,
            Field(description="子智能体 slug，例如 research/web-researcher；先查目录再装"),
        ],
    ) -> str:
        try:
            from octop.infra.agents.subagents.catalog import SubagentCatalog  # noqa: PLC0415

            clean = slug.strip()
            if not clean or "/" in clean or clean.startswith("."):
                raise ValueError("slug 不能为空，且不能包含 / 或以 . 开头")

            catalog = SubagentCatalog(_catalog_root())
            catalog.refresh()
            item = catalog.get(clean)
            if item is None:
                raise ValueError(f"没有这个子智能体：{clean}")

            workspace = workspace_for(caller_agent_id)
            if workspace is None:
                raise ValueError("agent workspace 还没准备好，稍后再试")

            available = item.summary.available_locales
            locale = "zh" if "zh" in available else ("en" if "en" in available else None)
            if locale is None:
                raise ValueError(f"子智能体 {clean} 没有任何可用语言版本")
            content = item.content_for(locale)

            dest = f"agents/{clean}.md"
            await workspace.aupload_bytes(dest, content.encode("utf-8"))
            await reload_agent(caller_agent_id)
            return json.dumps(
                {"ok": True, "slug": clean, "path": dest, "locale": locale},
                ensure_ascii=False,
            )
        except Exception as exc:  # noqa: BLE001 - 工具永不把异常抛进 harness
            return json.dumps({"ok": False, "error": str(exc)}, ensure_ascii=False)

    return [
        StructuredTool.from_function(
            coroutine=subagent_install,
            name=SUBAGENT_INSTALL,
            description=(
                "Install a bundled subagent (a stateless prompt pack, no memory) into your "
                "own workspace under agents/. Use it when a recurring kind of sub-task "
                "deserves its own instruction file."
            ),
        )
    ]
