# 手机端复用 Octop 本体 UI · 分析与实施方案

> 2026-11-20 ｜ 目标读者：魏方乐
> 前提指令：**完全复用 Octop 本体的源码与 UI 设计，只把布局改成手机端**。
> 本文所有结论都对应到仓库里的具体文件与行号，不含空话。

---

## 0. 一句话结论

**本体的 Web 控制台（`dashboard/`）本身就已经是移动端适配过的**——它有 `useIsMobile`（断点 768px）、36 个页面/组件按手机重排结构、`viewport-fit=cover`、软键盘偏移修正、PWA manifest。

所以"复用本体 UI 做成手机端"这件事，**不需要把 React 翻写成 Kotlin**，最省且最像的做法是：

> **手机上直接跑本体的 dashboard（WebView 或 PWA 直连），原生壳只保留手机独有的能力（能力桥 / Shizuku / 本地工具）。**

现有 `octop-mobile` 之所以"设计不行"，根因很明确：**它的配色和字号是抄 OpenMuse 的，不是抄 Octop 的**（见 §2）。

---

## 1. 源码已就位

| 内容 | 路径 | 规模 |
|---|---|---|
| Octop 本体（`TencentCloud/Octop`，浅克隆） | `G:\个人智能体开发\05-Octop本体` | 3883 文件 |
| 本体 Web UI | `05-Octop本体\dashboard` | 1581 文件 |
| 本体后端 | `05-Octop本体\src` | 1323 文件 |
| 手机端（现工程） | `G:\个人智能体开发\02-Octop源码\octop-mobile` | Kotlin + Compose |

dashboard 技术栈：React 18 + TypeScript + Vite 6 + **antd 5.29** + `antd-style` + react-router 7 + LESS modules + `vite-plugin-pwa`。

---

## 2. "设计不行"的根因（三条，可核对）

### 2.1 配色借错了产品

`app\src\main\java\com\octop\mobile\ui\theme\Color.kt` 第 6 行注释原文：

> 直接对齐 **OpenMuse（CopilotKit/openmuse，MIT）** 的 ui.tsx 设计系统。

于是主色是蓝 `#1473C8`，画布 `#FCFCFC`。

而 Octop 本体 Design System v3 的默认品牌色是 **Elegant Rose `#E85D75`**
（`dashboard\src\styles\themePalettes.ts` 第 28 行 `DEFAULT_PALETTE = "rose"`，第 47 行 `rose: "#E85D75"`）。

**这不是"风格偏好"问题，是抄错了源。用户看着不像 Octop，原因就在这里。**

### 2.2 字号借错了产品

`ui\theme\Type.kt` 第 8 行注释：

> 字号与字重照抄 **OpenMuse** ui.tsx 的 s.title / s.heading / s.text ...

### 2.3 没有暗色主题

```
Theme.kt  → 只有 lightColorScheme(...)，无 darkColorScheme
全工程 isSystemInDarkTheme / darkColorScheme 出现次数 = 0
```

本体是 **8 套配色 × 明暗双主题**正交切换（`themePalettes.ts` 的 `VALID_PALETTES`：
rose / tech / indigo / teal / violet / emerald / amber / slate；`theme-vars.css` 844 行，16 个 palette 选择器块）。
手机端现在一套都没有。

### 2.4 渲染能力缺一大块

`app\build.gradle.kts` 依赖里**没有**任何 Markdown / 代码高亮 / 图片加载库。

而本体一条助手消息要渲染：`react-markdown` + `remark-gfm` + `remark-math` + `rehype-katex` + `mermaid` + `react-syntax-highlighter` + `monaco-editor` + `@xterm/xterm` + `react-pdf` + `docx-preview` + `xlsx` + `@aiden0z/pptx-renderer`。

**想用 Compose 手写这些渲染器 ≈ 重写一个中型产品。** 这是"必须复用 Web 层"的硬理由，不是偷懒。

---

## 3. 本体 UI 的真实样子

### 3.1 信息架构（侧边栏一级导航）

`dashboard\src\layouts\sidebarNav.tsx`：

| key | path | 图标 |
|---|---|---|
| chat | `/chat` | MessageSquareText |
| experts | `/experts` | GraduationCap |
| tasks | `/tasks` | Timer |
| token-usage | `/token-usage` | Activity |
| personalization | `/personalization` | Sparkles |
| channels | `/personalization/channels` | Waypoints |
| connectors | `/connectors` | Link2 |
| skill-packages | `/skill-packages` | Package |
| knowledge-bases | `/knowledge-bases` | Database |
| bridge | `/bridge` | Cloudy |
| workbench | `/workbench` | PanelsTopLeft |
| settings（分组） | — | — |

`personalization` 下还有二级：skills / subagents / tools / agent_plugins / mbti / memory。

对比现在手机端的 5 个底部 tab（对话 / 专家 / 记忆 / 定时 / 我的）——**IA 就不一样**，所以不只是换皮。

### 3.2 设计令牌契约

`dashboard\src\styles\theme-vars.css`（844 行）前缀 `--fn-*`：
`--fn-bg-*` / `--fn-text-*` / `--fn-border-*` / `--fn-brand-*` / 语义色 / 阴影 / 卡片 / 侧边栏。
切换方式：`html[data-palette="rose|tech|..."]` × `html[data-theme="light|dark"]`。

`themePalettes.ts` 里 `ANTD_BRAND_TOKENS` 给了每套配色 × 明暗的 `colorPrimary/Hover/Active/Link`（rose light = `#E85D75`，dark = `#F08B9A`）。

### 3.3 移动适配是"已经有"的，不是"要做"的

- `hooks\useIsMobile.ts`：`MOBILE_BREAKPOINT = 768`，`matchMedia` 监听。
- **36 个页面/组件调用 `useIsMobile()`，95 处引用**，包括 `App.tsx`、`layouts\MainLayout\index.tsx`、`layouts\PageShell.tsx`、`pages\Chat\index.tsx`、`pages\Chat\components\ChatInput.tsx`、`pages\Experts\index.tsx`、`pages\KnowledgeBases\index.tsx` 等。
- `layouts\MainLayout\index.tsx`：手机上侧边栏自动折叠（L153-155）、改成抽屉 + 遮罩（L276 "Mobile overlay backdrop"）、Escape 关闭（L162-186）、路由切换自动收起（L182-186）。
- `hooks\viewport.ts`：PWA 软键盘偏移 `--keyboard-offset`、Android Chrome visualViewport 修正、`MOBILE_VIEWPORT_MQ = "(max-width: 767px)"`。
- `index.html`：`viewport-fit=cover, interactive-widget=resizes-content`、`apple-mobile-web-app-capable`、theme-color 双 meta。
- `public\manifest.json`：`display: standalone`、192/512 图标 + maskable、`lang: zh-CN`。
- 聊天消息气泡在 `pages\Chat\chatMessages.partial.less` L898 起有专门 `@media (max-width: 767px)` 块：助手头像上置、正文占满宽、用户气泡右对齐。

**注意一个真实缺口：本体的手机端是"抽屉式侧边栏"，不是底部 tab。**
底部 tab 才是手机原生手感。这是 §5 要做的事，也是唯一需要新增的设计层。

---

## 4. 三条路线对比

| | A. WebView 直挂本体 dashboard | B. Compose 全量重写 | C. 原生壳 + WebView 混合 |
|---|---|---|---|
| UI 复用率 | **100%**（就是本体本身） | 0%（照抄设计重画） | 对话/复杂页 100%，壳页 0% |
| 设计还原度 | **完全一致** | 靠人肉对齐，易走偏 | 高 |
| 工作量 | **最小**：加 WebView + 登录态透传 | **极大**：markdown/mermaid/katex/monaco/xterm/pdf/xlsx 全要重写 | 中 |
| 暗色/8 配色 | **白送** | 全要手写 | 部分白送 |
| 手机独有能力（能力桥/Shizuku/工具） | 需做 JS Bridge | 原生天然有 | **天然有 + JS Bridge** |
| 底部 tab 手感 | 无（要用本体抽屉导航） | 天然有 | 可加原生壳导航 |
| 主要风险 | WebView 性能、键盘、文件上传 | 永远追不上本体迭代 | 两套 UI 边界要划清 |

**B 路线要放弃。** 本体 Chat 页 309 个文件、`Chat` 组件里 `MessageBubble.tsx` 40KB、`ChatInput.tsx` 39KB、`ChatInputActionsRow.tsx` 56KB，且依赖上面那串渲染器。手写重画等于把本体 UI 重做一遍，迭代一次就跟不上一次。

---

## 5. 推荐方案：C（原生壳 + WebView 混合）

### 5.1 分工

```
┌──────────────────────────────────────────────┐
│ 原生壳（Compose）                             │
│  · 底部 5 tab：对话 / 专家 / 记忆 / 定时 / 我的 │
│  · 登录页、连接设置、能力桥状态                 │
│  · Shizuku / 本地工具 / 前台服务               │
│  · 与本体通信：token 保存与透传                │
├──────────────────────────────────────────────┤
│ WebView（本体 dashboard 原样）                 │
│  · 对话全流程（消息/富文本/轨迹/审批/文件）      │
│  · 专家、记忆、知识库、设置等所有复杂页          │
│  · 8 配色 × 明暗主题，本体原样                 │
└──────────────────────────────────────────────┘
```

原则：**原生只做"手机必须有而网页给不了"的东西，其余一律交给本体 dashboard。**

### 5.2 落地步骤

#### 阶段 0：先看到真实设计（半天，零风险）

不改任何代码，先用手机浏览器打开本体控制台：

```
http://<NAS-IP>:8089/          # 本体端口 8089
```

确认三件事：手机宽度下侧边栏变抽屉、聊天气泡重排正确、暗色主题可切。
**这一步做完，你就能判断"本体设计搬到手机上到底行不行"，再决定要不要写代码。**

#### 阶段 1：WebView 容器（1 天）

在 `octop-mobile` 里新增：

```
app/src/main/java/com/octop/mobile/ui/screens/OctopWebScreen.kt
```

要点（逐条都有坑）：
- `settings.javaScriptEnabled = true`、`domStorageEnabled = true`（本体用 localStorage 存主题与登录）。
- `WebView.setWebViewClient` 里放行白名单域（NAS 局域网 + Tailscale），其余 `shouldOverrideUrlLoading` 拦截。
- **token 透传**：改 `core\auth\TokenStore.kt`，把 token 通过 `evaluateJavascript` 写进 localStorage（键名照抄本体的存储实现，见 `dashboard\src\styles\appearanceStorage.ts` 与登录模块）。
- **文件上传**：实现 `WebChromeClient.onShowFileChooser`，否则本体的图片/附件上传按钮点了没反应。
- **软键盘**：`android:windowSoftInputMode="adjustResize"` + `WindowCompat.setDecorFitsSystemWindows(false)`，配合本体自带的 `--keyboard-offset`。
- 返回键：`OnBackPressedCallback` 里先 `webView.canGoBack()`，否则退出。

#### 阶段 2：原生底部 tab 与 WebView 的边界（1 天）

`ui\OctopRoot.kt` 的 `Tab.entries` 保持不变，但每个 tab 的目标改成：

| tab | 承载 |
|---|---|
| 对话 | WebView → `/#/chat` |
| 专家 | WebView → `/#/experts` |
| 记忆 | WebView → `/#/personalization/memory` |
| 定时 | WebView → `/#/tasks` |
| 我的 | **原生**（连接、能力桥、Shizuku、账号、关于） |

WebView 复用同一个实例、按 tab 改 URL，避免 5 个 WebView 各吃一份内存。

#### 阶段 3：设计令牌对齐（0.5 天，只在原生壳内）

原生"我的"页要看起来像本体，就得用本体的 token。把 `ui\theme\Color.kt` 重写为：

```
rose  light: #E85D75 / hover #D14A62 / active #B83A50
rose  dark : #F08B9A / hover #F5A8B4 / active #E85D75
```

数据源就两处：`themePalettes.ts` 的 `ANTD_BRAND_TOKENS`、`theme-vars.css` 的 `--fn-*` 块。
同时 `Theme.kt` 补 `darkColorScheme(...)`。
**`docs\design.md` 里的蓝色 `#1473C8` 规格作废**，它和本体冲突，以 `themePalettes.ts` 为准。

#### 阶段 4：把手机独有能力接回网页（按需）

现有 `core\system\BridgeManager.kt`（`http://192.168.1.126:8765` 能力桥）已经是 HTTP 服务，
网页侧只需要 fetch 调它即可，**不用写 JS Bridge**。这是现成资产，别浪费。

---

## 6. 需要你定的三件事

1. **路线确认**：走 C（原生壳 + WebView）还是先只做阶段 0 看效果？
2. **本体地址**：NAS 上 Octop 的实际地址和端口（现在 `OctopClient.kt` 里写的是 `http://192.168.1.100:8089`，和本机网段 `192.168.1.x` 不一致，需要核对）。
3. **底层页面归属**：`/token-usage`、`/bridge`、`/connectors`、`/skill-packages` 这些本体有、手机端没有的入口，是放进"我的"页里做二级入口，还是暂时不暴露？

---

## 附：核对清单（自己验，别听我说）

| 结论 | 怎么验 |
|---|---|
| 手机端配色来自 OpenMuse 不是 Octop | 读 `ui\theme\Color.kt` 第 5-9 行注释 || 本体默认品牌色是 `#E85D75` | 读 `dashboard\src\styles\themePalettes.ts` 第 28、47 行 |
| 本体有 8 套配色 | 同上第 14-25 行 `VALID_PALETTES` |
| 本体手机适配已完成 | 读 `dashboard\src\hooks\useIsMobile.ts`，再数 `useIsMobile(` 在多少文件里 |
| 手机端无暗色 | 全工程搜 `darkColorScheme`，结果 0 |
| 手机端无 WebView | 全工程搜 `WebView`，结果 0 |
| 手机端缺富文本渲染 | 读 `app\build.gradle.kts` 的 dependencies，无 markdown/高亮/图片库 |
