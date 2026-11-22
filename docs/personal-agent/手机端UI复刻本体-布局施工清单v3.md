# 手机端 UI 复刻本体 · 布局施工清单 v3

> 2026-11-20 ｜ 魏方乐
> 指令：**全部逻辑和本体一样，仅改 UI；必须源码级复刻，不能套网页（PWA/WebView/TWA）。**
> v2 解决了配色/令牌/暗色/8 套主题；v3 解决 v1/v2 没覆盖的**布局骨架与信息层级**。
> 每条都写：本体依据（文件:行）→ 手机端目标文件 → 具体改法 → 状态。开发时不需要再判断设计。

---

## 0. 铁律（不做就会翻车）

1. **不动业务逻辑**：`core/**`、网络、协议、状态机、VM 的方法体一律保持原样，byte 级不改。
2. **保留顶层 Color 名**：`ui/theme/Color.kt` 里 `Bg`/`TextPrimary`/`Brand` 等 20 个名字保留为 `@Composable @ReadOnlyComposable get()`，指向 `LocalOctopColors.current`；调用点零改动。
3. **颜色唯一权威**：`dashboard/src/styles/theme-vars.css`。文档和旧代码与它冲突时以 CSS 为准（例：amber 品牌 CSS 浅色是 `#b45309`，不是 swatch 的 `#f59e0b`）。
4. **图标按本体 lucide**：`iconSize=16`、`strokeWidth=1.8` → Compose 线宽比例 `0.1125f`（`TabIcons.kt:44`、`HubIcons.kt:36` 已实现）。
5. **断点按本体**：`MOBILE_BREAKPOINT=768`（`hooks/useIsMobile.ts`）、`<480px` 快捷卡单列、`<=767px` 头像槽 36px、键盘阈值 80px（`hooks/viewport.ts`）。

---

## 1. 主题内核（v2 已落地，v3 不改）

| 本体依据 | 手机端文件 | 结果 |
|---|---|---|
| `theme-vars.css` 891 行 | `ui/theme/OctopTokens.kt`（98 色 / 8 套 / 圆角 5·8·12·18·9999 / 时长 120·180·280·320） | ✅ |
| `themePalettes.ts` 8 套 | `ui/theme/ThemeState.kt`（`octop:ui-palette` 持久化） | ✅ |
| 顶栏/状态栏跟随 | `OctopRoot.kt` + `MainActivity.kt`（透明栏 + 80px IME 阈值 + IME 显示时隐藏 BottomBar） | ✅ |
| 设置页外观入口 | `screens/settings/AppSettingsScreen.kt`（模式 + 8 配色，即时生效） | ✅ |
| 旧名零改动 | `ui/theme/Color.kt`（20/20 旧名保留为 composable getter） | ✅ |

**已知待决（非阻塞）**：本体部分页面暗色底用 `#0f1117`/`#0b0d14`，CSS 变量是 `#141414`/`#0e0e0e`。当前按 CSS 变量走；若要求逐页面像素级一致，需逐页确认后决定是否引入页面级底色覆盖。

---

## 2. 对话页（ChatScreen.kt）——v3 重点

### 2.1 欢迎态（✅ 已落地）

本体 `pages/Chat/components/WelcomeScreen.tsx` + `chatWelcome.partial.less` + `WelcomeQuickCards.tsx`。

| 区块 | 本体值 | 手机端实现 |
|---|---|---|
| 容器 | `.welcome` flex 居中，padding 24/20，`.welcomeInner` 移动 padding 0/12、gap 10 | `WelcomeView`：`verticalScroll` + `padding(horizontal=12, vertical=24)` + `spacedBy(10)` ✅ |
| 吉祥物 | `.welcomeMascot` 132px / 移动 110px、aspect 352/320、marginBottom -10px | `Image(R.drawable.octop_mascot)` 110dp 宽 × 100dp 高 + `offset(y=10dp)` ✅ |
| 标题 | `.welcomeTitle` 移动 23px / 600 / line-height 1.4 | `23.sp` / SemiBold / `(23*1.4).sp` ✅ |
| 副标题 | 移动 13px / 1.55 | `13.sp` / `(13*1.55).sp`；有 agent 时上方加品牌色衬线 `@name`（本体 `welcomeAgentMention`）✅ |
| 快速开始标题 | 12px / 500 / quaternary / letter-spacing .06em / uppercase | `12.sp` / Medium / `textQuaternary` / `letterSpacing=0.06.em` ✅ |
| 卡片网格 | 2 列，`<=479px` 1 列，gap 8 | 手机窄屏 `Column spacedBy(8)` 单列 ✅ |
| 卡片 | `.quickCard` row / gap 11 / padding 12·14 / radius 12 / bg primary / 1dp border | `WelcomeQuickCard` 同值 ✅ |
| 卡图标 | 36px / radius 9 / padding 7 | `36.dp` / `RoundedCornerShape(9)` ✅ |
| 卡标题 / 描述 | 13px/600；11.5px tertiary 单行省略 | 同值 ✅ |
| 点击 | `onPromptClick(expertName ? withExpertMention : prompt, {prefill})` | 点击预填输入框；`prefillNonce` 递增解决同一 prompt 连点不重触发 ✅ |

**受限项**：本体两张吉祥物 webp 交替（`MASCOT_PEEK`/`MASCOT_TYPE`，`getRandomMascot` 去重）。手机端只复制了 `octop-mascot-empty.png`。要像素级一致需再复制 `octop-mascot-peek.webp` / `octop-mascot-type.webp` 并实现随机切换；当前用单张静态图 + 点击缩放反馈替代。**是否补齐待定。**

### 2.2 消息区（✅ 已对齐）

- 头像槽 36dp（`chatMessages.partial.less <=767px`）✅
- 用户气泡 `16/16/6/16` + padding 14/10 + 15sp/23sp ✅
- 助手气泡 14dp + 三层（底色 + 135° 品牌浅渐变 + 1dp border）+ 16sp/27sp ✅
- 横向留白 14dp 只在 `MessageRow`，列表不再叠加 ✅

### 2.3 滚动到底按钮（✅ 已落地）

本体 `chatMessages.partial.less:53` `.scrollToBottomBtn`：底部 20px 居中、pill、半透明 elevated 底 88%、1dp `border-secondary`、radius 9999、12px/500、轻阴影。
手机端 `ScrollToBottomButton`：`derivedStateOf` 判断最后可见项 < 末项时显示，点击 `animateScrollToItem(lastIndex)` ✅。

### 2.4 轨迹面板（✅ 形态已对齐）

本体 trajectory drawer / side panel。手机端 `TraceOverlay` 底部抽屉（scrim 暗色 0.55 / 浅色 0.33 + 圆角顶 18dp），入口在用量条「轨迹」；`vm.refreshTrajectory()` 后打开 ✅。

### 2.5 顶栏（保持移动适配，未强行照搬）

本体 `ChatTitleBar`：会话标题可编辑 + 更多菜单（置顶/分叉/删除/折叠思考）+ 侧栏开关。
手机端顶栏：返回 + 标题 + 连接状态点/文案 + 新对话。侧栏开关在手机上对应抽屉、更多菜单操作在手机端由其它入口承担；当前形态属合理移动适配，**不做无依据的照搬**。

**未做（需产品确认再做）**：右侧浮动工具竖列（工作区/修改文件/终端/浏览器，本体 `chatFloatActions`）。这些功能手机端 VM 无对应能力或入口在别处；在确认手机端是否要暴露前不写死按钮。

---

## 3. 登录页（进行中，agent 并行）

本体 `pages/Login/index.tsx` + 同目录 `*.module.less`；权威色 `theme-vars.css`。
目标文件：`ui/screens/LoginScreen.kt`（单一所有权，仅此 agent 写）。
要求：逐项对齐整体布局（居中卡片 vs 分栏、品牌区、表单字段顺序/样式、主按钮、辅助链接、背景装饰、lucide 图标），保留全部登录逻辑，颜色全用 token。

---

## 4. 专家列表 / 详情（进行中，agent 并行）

本体 `pages/Experts/index.tsx` + `*.module.less`；专家详情弹层/抽屉用 glob 搜 expert 相关 tsx。
目标文件：`ui/screens/ExpertsScreen.kt`、`ui/screens/ExpertDetailScreen.kt`（仅此 agent 写）。
要求：对齐列表形态（卡片/行）、头像形状大小、状态点/标签、分区标题、详情头部英雄区/信息分组/操作按钮排布。

---

## 5. 设置页（v2 已基本逐项对齐）

`screens/settings/*`（Skills/Connectors/Models/Channels/Plugins/Security/Storage/Usage/Users/Cloud/KnowledgeBases）已按本体 IA 建好分组页。v3 只做外观 token 化清扫（已由 agent D/E 完成主体）。**剩余**：逐页核对每个条目的图标/副标题文案与本体 `zh.json` 是否一致（低优先，非布局骨架）。

---

## 6. 验证（统一在改完后做）

1. 编译：`cd G:\个人智能体开发\02-Octop源码\octop-mobile; .\gradlew :app:compileDebugKotlin --console=plain`
2. 安装真机：`cd G:\个人智能体开发; .\dev.ps1 run`（vivo V2453A / Android 16）
3. 布局核验：`adb shell uiautomator dump /sdcard/ui.xml` 看节点结构；当前模型不支持看图，截图仅留档 `dev.ps1 shot`。
4. 暗色 + 8 套主题逐一切换，确认无硬编码色残留（grep `Color(0x` 于 `ui/screens` 与 `ui/components`，应为 0）。
5. 长回答验证头像固定、正文填满手机宽度；键盘弹出时输入条贴底、BottomBar 隐藏。

---

## 7. 文件所有权（并行不冲突）

| 文件 | 所有者 |
|---|---|
| `ChatScreen.kt` / `Common.kt` | 主线程 |
| `LoginScreen.kt` | agent 登录 |
| `ExpertsScreen.kt` / `ExpertDetailScreen.kt` | agent 专家 |
| `theme/**`、`core/**` | 冻结，谁都不改 |
| `OctopRoot.kt` / `MainActivity.kt` / `Navigation` | 冻结（v2 已完成） |
