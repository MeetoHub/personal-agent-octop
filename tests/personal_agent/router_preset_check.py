"""Router 预设的纯逻辑检查（不连库、不起服务、不装依赖）。

    python tests/personal_agent/router_preset_check.py

tool_catalog 只依赖标准库，但它所在的包 __init__ 会拉起 octop_harness；
所以这里按路径加载，绕开包导入。
"""

from __future__ import annotations

import importlib.util
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]


def load(name: str, rel: str):
    spec = importlib.util.spec_from_file_location(name, REPO / rel)
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


catalog_mod = load("_tool_catalog", "src/octop/infra/agents/settings/tool_catalog.py")
router_mod = load("_router_preset", "src/octop/infra/agents/presets/router.py")

CATALOG = {entry.name for entry in catalog_mod.BUILTIN_TOOL_CATALOG}
CRITICAL = set(catalog_mod.CRITICAL_TOOLS)
ROUTER_TOOLS = router_mod.ROUTER_TOOLS

fail = 0


def check(name: str, ok: bool, detail: str = "") -> None:
    global fail
    print(("PASS  " if ok else "FAIL  ") + name + (f"  {detail}" if detail else ""))
    if not ok:
        fail += 1


disabled = set(router_mod.router_tools_disabled(catalog=CATALOG, critical=CRITICAL))

check("白名单非空", bool(ROUTER_TOOLS))
check("白名单与禁用清单不相交", not (ROUTER_TOOLS & disabled), str(sorted(ROUTER_TOOLS & disabled)))
check(
    "catalog 全部被分类",
    not (CATALOG - disabled - ROUTER_TOOLS - CRITICAL),
    str(sorted(CATALOG - disabled - ROUTER_TOOLS - CRITICAL)),
)
check("关键工具不被禁用", not (CRITICAL & disabled))
check("禁用清单确实砍掉了大部分工具", len(disabled) >= 30, f"disabled={len(disabled)}")
check("ask_agent / agent_list 保留", {"ask_agent", "agent_list"} <= ROUTER_TOOLS)
check(
    "工单工具不在 catalog 里（不会被误关）",
    not ({"ticket_create", "ticket_list", "ticket_report"} & CATALOG),
)

cfg: dict = {"is_router": False, "tools_disabled": ["browser_use"]}
check("未标记时 is_router_agent=False", not router_mod.is_router_agent(cfg))
router_mod.apply_router_preset(cfg, catalog=CATALOG, critical=CRITICAL)
check("apply 后标记为 router", router_mod.is_router_agent(cfg))
check("apply 保留原有禁用项", "browser_use" in cfg["tools_disabled"])
check("apply 后白名单仍然可用", not (ROUTER_TOOLS & set(cfg["tools_disabled"])))
check(
    "apply 幂等",
    router_mod.apply_router_preset(dict(cfg), catalog=CATALOG, critical=CRITICAL)["tools_disabled"]
    == cfg["tools_disabled"],
)

print()
if fail:
    print(f"FAILED: {fail}")
    sys.exit(1)
print("全部通过")
