# P3 设计：Router / Worker 分层 + 手机端配置（2026-11-21）

> 借鉴 DSH `docs/003-execution.md`：主会话只带**路由用的小工具集**，具体长活由 `worker-<ticket>` 会话执行。
> 本文把这个思路落到 Octop 本体，并给出手机端配置的两端分工。

---

## 1. 现状与差距

| 项 | 现状 | 差距 |
|---|---|---|
| 工具装配 | `manager.py` 给每个专家挂全部工具（catalog 45+，再叠加 cron/knowledge/mobile/phone/plugin） | 每轮 3-8k token 工具定义；工具选错率约 15%；首字 1.5-3s |
| 工单 | 表 + 12 个 REST 端点已上线，**没有 agent 工具** | 专家不能自己建单/报进度 |
| 工具开关 | 已有 `tools_disabled` 机制（`tool_catalog.effective_tools_disabled`），可按专家热更新 | 没有被用来做"预设" |
| 手机通道 | 反向 WS 已通，App 端 3/7 工具 | 没有配置面：看不到状态、管不了能力 |

**结论：Router 不需要改核心，用现成的 `tools_disabled` 就能做。**

---

## 2. Router / Worker

### 2.1 三层

```
① 规则先筛（0 成本，覆盖 ~80%）
   纯寒暄 / 单问单答       → Router 直接回
   文本里带 @专家          → 直接 ask_agent，跳过判定
   明显多步（≥2 动词或含"然后/先…再…"）→ 直接 ticket_create

② Router 判定（剩下 ~20%）
   要人格/语气    → subagent 切换（同进程）
   要某专家      → ask_agent（独立会话 + 长期记忆）
   要多专家并行   → ticket + team

③ Worker 执行
   独立会话 worker-<ticket id>，工具全开
   长活先回执（ticket_report=working），再干活
```

规则层放在**入口**（聊天 WS 收到 user_turn 时），不是放在模型里——规则不产生 token，也不增加延迟。

### 2.2 Router 的工具白名单

只保留判定和派活必需的工具（catalog 名 + 我们新增的工单工具）：

| 工具 | 来源 | 作用 |
|---|---|---|
| `agent_list` | teams（PeerAgentMiddleware） | 看有哪些专家 |
| `ask_agent` | teams | 派给专家 |
| `ticket_create` | **新增** | 长活建单 |
| `ticket_list` | **新增** | 看自己派出去的活 |
| `ticket_report` | **新增** | 给工单报进度 |
| `ask_user_question` | interaction | 信息不足时追问 |
| `memory_search` | memory | 查长期记忆 |

其余全部进 `tools_disabled`。计算方式：

```python
from octop.infra.agents.presets.router import router_tools_disabled
disabled = router_tools_disabled()   # = 全部 catalog − 白名单
agent_cfg["tools_disabled"] = disabled
```

**不新增核心分支**：`manager.py` 现有的 `sync_effective_tools_disabled()` 会把这份清单同步给运行中的 harness。

### 2.3 工单工具（新增，所有专家可用）

与 `infra/phone/tools.py` 同款 `StructuredTool`，直接调 `TicketStore`，不走 HTTP：

| 工具 | 参数 | 返回 |
|---|---|---|
| `ticket_create` | `title`, `goal`, `priority` | `{ticket_id, stage}` |
| `ticket_list` | `stages?`, `limit` | `[{ticket_id,title,stage,updated_at}]` |
| `ticket_report` | `ticket_id`, `report`(6 种取值), `detail` | `{stage}` |

`owner_user_id` 从 `get_config()["configurable"]["user"]` 取，与 phone 工具一致；没有 user 直接返回错误，不落库。

### 2.4 验收

| 指标 | 目标 | 怎么测 |
|---|---|---|
| Router 工具数 | ≤ 8 | `agent_list` 之外打印 `tools` 长度 |
| 首字延迟 | < 800ms | 同一句话对比改造前后 |
| 每轮 token | 降 40-60% | harness usage 里的 input tokens |
| 长活感知延迟 | 0 | 建单后 3s 内回执 |

---

## 3. 手机端配置（两端分工）

A 方案（反向 WS）下，**手机是客户端，NAS 是服务端**，配置要分清楚谁管什么：

| 配置项 | 归属 | 说明 |
|---|---|---|
| NAS 地址 / 账号 | **手机**（登录页） | 已有；决定连谁 |
| token | **手机**（TokenStore） | 已有；WS 鉴权用它 |
| 反向通道开关 | **手机**（设置页） | 关掉后不再外连，NAS 视为离线 |
| 能力开关（通讯录/短信/…） | **手机**（设置页） | 关掉后 hello 里不上报，NAS 不会调用 |
| 运行时权限 | **手机**（系统授权页） | 缺权限时工具返回 `permission_denied` |
| `mobile` 权限 | **NAS**（用户角色） | 没有该权限的账号，工具直接拒绝 |
| `capabilities.mobile.enabled` | **NAS**（配置文件） | 总开关，关掉则 7 个工具不注册 |

**不做**：NAS 侧再存一份手机配置。手机是唯一事实来源，NAS 只读 `hello` 上报的能力。

### 3.1 App 设置页（新增一张卡）

```
手机能力
  反向通道     ● 已连接 · 12 秒前
  能力         通讯录 ✓  短信 ✓  日历 ✓  位置 —  相册 —
  权限         3/3 已授权            [去授权]
  [断开] [重连]
```

- 状态来自 `PhoneLink.state`（`StateFlow`）；
- 能力来自 `PhoneTools.SUPPORTED`（关掉的能力不报）；
- 权限用 `SystemBridge.REQUIRED` 判断。

### 3.2 NAS 侧接口（扩展现有）

`GET /api/phone/status` 已有，补两个字段即可：

```json
{
  "connected": true,
  "device": {"model": "...", "android": 35},
  "capabilities": ["contacts.search", "sms.recent", "calendar.today"],
  "last_seen": 1795259458,
  "server_tools": ["phone_contacts_search", "..."]
}
```

`last_seen` 现在只在 hub 里有，加进 `describe()` 即可（1 行）。

### 3.3 前端（dashboard）

| 页面 | 改动 | 优先级 |
|---|---|---|
| 设置 → **手机能力** | 在线设备、最后在线、能力清单、总开关 | P4 |
| 侧栏「活」页 | 工单列表（七态分组、person/* 置顶） | P4 |
| 聊天页 | 工具调用卡片显示「手机：通讯录查询」 | P5 |

手机能力页在 A 方案下**只能看不能配**（配置在手机上），除总开关外不提供编辑——这点要在页面上写清楚，避免用户以为能远程改手机。

---

## 4. 施工顺序（建议）

| 步 | 内容 | 验收 |
|---|---|---|
| 1 | 工单工具 `infra/tickets/tools.py` + 注册进 manager | 专家能建单，`GET /api/tickets` 能看到 |
| 2 | Router 预设 `infra/agents/presets/router.py`（白名单 + 禁用清单） | 给某个专家打上后，工具数 ≤8 |
| 3 | 规则前置筛选（聊天入口） | 「你好」不进模型；「@小薇 查天气」直接派给该专家 |
| 4 | 前端「活」页 | 手机上看得到工单列表与状态 |
| 5 | 手机设置卡 + `/api/phone/status` 补字段 | 设置页显示已连接与能力 |

1 和 2 是纯增量，不动现有链路；3 改聊天入口，需要灰度。
