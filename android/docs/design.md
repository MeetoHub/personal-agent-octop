# Octop Mobile 页面设计

## 一、定位（这决定了页面怎么排）

- **本体跑在 NAS 上**，App 只是它的手机端。agent 的推理、工具、记忆、定时都在 NAS。
- **手机的权限数据由 App 本地取**（通讯录 / 短信 / 日历 / 闹钟 / 通知 / 录音），
  通过 `core/system/SystemBridge.kt` 读，再上报或按需喂给 agent。
- **不走 `/api/mobile/*`**——那是云手机（容器内安卓）的 adb 通道，与本 App 无关。

一句话：**NAS 是脑子，手机是感官**。页面要把「脑子在想什么」和「感官看到了什么」分开呈现。

## 二、底部五个 tab

| tab | 位置 | 为什么放这儿 |
|---|---|---|
| **对话** | 1 | 主入口。会话列表 → 聊天。最高频，必须第一个 |
| **专家** | 2 | agent 的人格/工具/知识库配置。换专家 = 换一个助手 |
| **记忆** | 3 | 它记得你什么。这是「长期陪伴」的可见证据，别的 App 没有 |
| **定时** | 4 | 主动找你的事（早报、提醒、日报） |
| **我的** | 5 | 连接状态、设备权限、技能包、知识库、退出 |

> 现有 tab 是「任务/专家/资料库/定时/设置」。改成上面这套，理由：
> 「任务」本质就是会话（thread），叫任务会让人以为是待办清单；
> 「资料库」太虚，拆成「记忆」（它对你的认知）+「我的 → 知识库」（你喂给它的文件）。

## 三、每页的数据来源（全部对应已接的 375 个接口）

### 1. 对话列表
| 元素 | 接口 |
|---|---|
| 会话列表 | `GET /api/agents/{id}/threads` |
| 未读/进行中标记 | 同上，`unread_count` / `turn_active` / `awaiting_user` |
| 新建会话 | `POST /api/agents/{id}/threads` |
| 重命名 / 删除 / 置顶 | `PATCH` / `DELETE /api/agents/{id}/threads/{tid}` |
| 分支 | `POST .../threads/{tid}/fork` |
| 已读 | `POST .../threads/{tid}/read` |

### 2. 聊天页（已做，需补）
| 元素 | 接口 |
|---|---|
| 历史消息 | `GET .../threads/{tid}/history?limit=200` |
| 实时流 | `WS /api/agents/{id}/chat/ws` |
| 审批卡片 | `POST /api/agents/{id}/chat/hitl/resume` |
| **执行轨迹（新）** | `chat/trajectory` 5 条 —— 每步工具调用、耗时、结果 |
| 上下文用量 | `GET .../threads/{tid}/context-usage` |
| 中途打断 | `POST .../threads/{tid}/cancel` |
| 提示词润色 | `POST /api/agents/{id}/chat/polish` |
| 附件上传 | `POST /api/agents/{id}/upload` |
| 斜杠命令 | `GET /api/slash/commands` |

### 3. 专家
| 元素 | 接口 |
|---|---|
| 我的专家 | `GET /api/agents`、`GET /api/experts` |
| 专家广场 | `GET /api/experts/hub`、`POST /api/experts/hub/{slug}/install` |
| 已发布 | `GET /api/experts/published` |
| 详情/编辑 | `PATCH /api/agents/{id}` |
| 头像 | `POST/GET/DELETE /api/agents/{id}/avatar` |
| 启停 | `POST /api/agents/{id}/start|stop|reload` |
| 工具开关 | `GET/PUT /api/agents/{id}/tool-settings`、`PATCH .../tool-settings/{tool}` |
| 挂载技能包 | `GET /api/skill-packages` |

### 4. 记忆
| 元素 | 接口 |
|---|---|
| 概览数字 | `GET .../memory/stats/counts`、`stats/growth`、`stats/atom_kinds` |
| 原子列表 | `POST .../memory/atoms/list` |
| 待确认（可审批） | `POST .../memory/candidates/list` + `:promote` / `:reject` |
| 实体（人物/地点/项目） | `POST .../memory/entities/list` |
| 事件片段 | `POST .../memory/episodes/list` |
| 日记 | `GET .../memory/journal/recent` |
| 手动加一条 | `POST .../memory/atoms` |
| 废弃一条 | `POST .../memory/atoms/{atom}:deprecate` |
| 导出/导入 | `memory_portable` 4 条 |

### 5. 定时
`GET/POST/PATCH/DELETE /api/agents/{id}/cron` + `run-now` + `examples`

### 6. 我的
| 元素 | 接口 |
|---|---|
| 连接状态 | `GET /api/health`、`GET /api/agents/{id}/status` |
| **本机权限状态** | 本地 `SystemBridge`（通讯录/短信/日历/通知/录音已授权？） |
| 知识库 | `GET/POST /api/knowledge-bases`、文档上传/分块/重排 |
| 技能 | `GET /api/skills`、`GET /api/skill-packages` |
| 通道（微信/QQ…） | `GET /api/channels` |
| 连接器 | `GET /api/connectors` |
| 工作区文件 | `GET /api/workspace` |
| 语音 | `GET /api/voice` 13 条 |
| 退出登录 | 本地清 token |

## 四、视觉规范（照 OpenFuse 源码数值，已落地）

| token | 值 |
|---|---|
| 画布 | `#FCFCFC` |
| 卡片 | `#FFFFFF` |
| 分隔线 | `#EEEEF0` |
| 主色（文字/图标） | `#1473C8` |
| 主色（浅底） | `#C8E7FF` |
| 用户气泡 | `#C8E7FF` 85% 宽 |
| 助手气泡 | `#EEEEF0` 95% 宽 |
| 圆角 | 气泡 22 / 输入条 32 / tab 容器 40 |
| 正文 | 16sp / 行高 24 |
| 标题 | 16sp / 600 / 字距 -0.4，居中 |
| tab item | 高 47、圆角 28，选中 `#F0F1F2` |

## 五、实现顺序

1. **接口层** ✅ 375 个方法已生成并编译通过（`core/sync/OctopApi.kt`）
2. **对话列表 + 聊天补全**（轨迹、上下文用量、取消、附件）
3. **记忆页**（这页最能体现产品差异）
4. **专家页**
5. **定时页**
6. **我的页**（含本机权限面板）
