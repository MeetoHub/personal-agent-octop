# 手机端 UI 复刻本体 · 实施方案 v2（可直接开发）

> 2026-11-20 ｜ 魏方乐
> 指令：**逻辑全部保持原样，只改 UI；设计照抄 Octop 本体源码。**
> 本文是施工图。每条都注明源文件、目标文件、具体值。开发时不需要再判断设计。

---

## 0. 一句话

把本体 `dashboard/src/styles/theme-vars.css`（891 行）的设计令牌完整翻译成 Kotlin，
**用 `@Composable` getter 保留现有变量名**，于是所有业务文件的调用点零改动，
配色/圆角/暗色/8 套主题一次性全部对齐本体。

---

## 1. 为什么这样做能做到"逻辑零改动"

现在已有 8 个文件、35 处直接引用 `Bg` / `TextPrimary` / `Accent` 这类顶层 `val`：

```
20  ui/theme/Color.kt        （定义处，本就要重写）
 6  ui/screens/ScheduleScreen.kt
 3  ui/OctopRoot.kt
 1  ui/screens/TaskDetailScreen.kt
 1  ui/screens/MemoryScreen.kt
 1  ui/screens/LoginScreen.kt
 1  ui/screens/ChatScreen.kt
 1  ui/components/Common.kt
```

Kotlin 支持**同名、同类型、带 `@Composable` getter 的顶层属性**：

```kotlin
val Bg: Color
    @Composable @ReadOnlyComposable
    get() = LocalOctopColors.current.bgPrimary
```

调用点写 `Bg` 不变，但它现在从 CompositionLocal 取值 → **自动跟随主题/配色/暗色**。
这 35 处调用点一行都不用改，也不会编译失败。

**这就是"只改 UI 不改逻辑"的技术落点。**

---

## 2. 设计令牌对照表（照抄 `theme-vars.css`）

### 2.1 品牌色（8 套 × 明暗）

源：`theme-vars.css` L9-193（rose 明）、L194-371（rose 暗）、L376-871（tech/indigo/teal/violet/emerald/amber/slate 覆盖块）

| 配色 | 明色 brand | 明色 hover | 明色 light底 | 明色 active项底 | 明色 active文字 | 暗色 brand | 暗色 light底 | 暗色 active项底 | 暗色 active文字 |
|---|---|---|---|---|---|---|---|---|---|
| rose | `#E85D75` | `#D14A62` | `#FDE8EC` | `#FDE8EC` | `#E85D75` | `#F08B9A` | `rgba(232,93,117,.16)` | `rgba(232,93,117,.14)` | `#F08B9A` |
| tech | `#3A5FE0` | `#2E4FD4` | `#F1F4FF` | `#F1F4FF` | `#4B74FA` | `#3A5FE0` | `rgba(75,116,250,.18)` | `rgba(75,116,250,.16)` | `#7B9BFC` |
| indigo | `#4F46E5` | `#4338CA` | `#EEF2FF` | `#EEF2FF` | `#6366F1` | `#4F46E5` | `rgba(99,102,241,.18)` | `rgba(99,102,241,.16)` | `#818CF8` |
| teal | `#0F766E` | `#115E59` | `#F0FDFA` | `#F0FDFA` | `#0D9488` | `#0F766E` | `rgba(13,148,136,.18)` | `rgba(13,148,136,.16)` | `#2DD4BF` |
| violet | `#7C3AED` | `#6D28D9` | `#F5F3FF` | `#F5F3FF` | `#7C3AED` | `#7C3AED` | `rgba(124,58,237,.18)` | `rgba(124,58,237,.16)` | `#A78BFA` |
| emerald | `#047857` | `#065F46` | `#ECFDF5` | `#ECFDF5` | `#047857` | `#047857` | `rgba(16,185,129,.18)` | `rgba(16,185,129,.16)` | `#34D399` |
| amber | `#B45309` | `#92400E` | `#FFFBEB` | `#FFFBEB` | `#B45309` | `#B45309` | `rgba(245,158,11,.18)` | `rgba(245,158,11,.16)` | `#FBBF24` |
| slate | `#475569` | `#334155` | `#F1F5F9` | `#F1F5F9` | `#475569` | `#475569` | `rgba(148,163,184,.18)` | `rgba(148,163,184,.16)` | `#94A3B8` |

> 注意 rose 的 hover 在暗色下是**反向变深** `#E85D75`（`theme-vars.css` L246）。
> amber 明色 brand 是 `#B45309` 而非 `#F59E0B`——`#F59E0B` 只是色板圆点（`themePalettes.ts` L52 的 `PALETTE_SWATCH`）。

### 2.2 中性色（明 / 暗）

| 令牌 | 明 | 暗 |
|---|---|---|
| bgPrimary | `#FFFFFF` | `#141414` |
| bgSecondary | `#F7F8FA` | `#1A1A1A` |
| bgTertiary | `#F0F1F3` | `#222222` |
| bgElevated | `#FFFFFF` | `#1E1E1E` |
| bgLayout | `#F5F6F8` | `#0E0E0E` |
| bgContainer | `#FFFFFF` | `#141414` |
| bgSidebar | `#FFFFFF` | `#141414` |
| bgPage | `#F5F6F8` | `#0E0E0E` |
| bgSubtle | `#F3F4F6` | `#1E1E1E` |
| bgCode | `#F5F6F8` | `#1E1E1E` |
| bgToolbar | `#FAFBFC` | `#161616` |
| bgGlass | `rgba(255,255,255,.8)` | `rgba(14,14,14,.8)` |
| surface | `#FFFFFF` | `#1E1E1E` |
| surfaceSunken | `#F7F8FA` | `#1A1A1A` |
| textPrimary | `#111827` | `rgba(255,255,255,.9)` |
| textSecondary | `#4B5563` | `rgba(255,255,255,.6)` |
| textTertiary | `#9CA3AF` | `rgba(255,255,255,.35)` |
| textQuaternary | `#D1D5DB` | `rgba(255,255,255,.2)` |
| textPlaceholder | `#C0C5CE` | `rgba(255,255,255,.16)` |
| textInverse | `#FFFFFF` | `#0E0E0E` |
| borderPrimary | `#E5E7EB` | `rgba(255,255,255,.08)` |
| borderSecondary | `#F3F4F6` | `rgba(255,255,255,.05)` |
| borderInput | `#D1D5DB` | `rgba(255,255,255,.12)` |
| borderStrong | `#D1D5DB` | `rgba(255,255,255,.16)` |
| sidebarBorder | `#EBEBEB` | `rgba(255,255,255,.06)` |
| sidebarItemHover | `#F5F5F5` | `rgba(255,255,255,.05)` |
| sidebarGroupText | `#B0B5BE` | `rgba(255,255,255,.28)` |

语义色：

| 令牌 | 明 | 暗 |
|---|---|---|
| success | `#10B981` | `#34D399` |
| danger | `#EF4444` | `#F87171` |
| warning | `#F59E0B` | `#FBBF24` |
| warningText | `#D97706` | `#FBBF24` |
| info | `#3B82F6` | `#60A5FA` |

### 2.3 圆角 / 阴影 / 动效（明暗共用）

```
radiusSm 5dp   radiusMd 8dp   radiusLg 12dp   radiusXl 18dp   radiusFull 9999dp
cardRadius 12dp
shadowXs  0/1/2   #000 4%
shadowSm  0/1/3   #000 6%  + 0/1/2 #000 3%
shadowMd  0/4/6-1 #000 6%  + 0/2/4-2 #000 4%
shadowLg  0/10/15-3 #000 7% + 0/4/6-4 #000 4%
shadowBrand   0/4/14 rgba(brand,.18)
shadowBrandLg 0/8/24 rgba(brand,.24)
transitionFast 120ms  transition 180ms  transitionSlow 280ms
cubic-bezier(.4,0,.2,1)
```

暗色阴影改为 `#000` 25%~40% 不透明（`theme-vars.css` L296-303）。

### 2.4 AI 气泡（磨砂）

| | 明 | 暗 |
|---|---|---|
| assistantBubbleBg | `rgba(255,255,255,.45)` | `rgba(255,255,255,.06)` |
| assistantBubbleBorder | `rgba(232,93,117,.12)` | `rgba(240,139,154,.14)` |
| 渐变 | `135deg, rgba(255,240,243,.6), rgba(255,255,255,.35) 50%, rgba(255,243,245,.5)` | `135deg, rgba(240,139,154,.1), rgba(255,255,255,.04) 50%, rgba(240,139,154,.08)` |

每套配色有自己的渐变与 border（见源文件对应 palette 块），实现时按 palette 覆写这两个值即可。

---

## 3. 字号体系（照抄本体，不是 OpenMuse）

源：`dashboard/src/App.tsx` L75-83 —— 本体在 `isMobile` 分支显式覆写 antd 字号：

```
fontSize: 15      → body 15sp / 行高 23sp
fontSizeSM: 13    → 辅助文字 13sp / 19sp
fontSizeLG: 17    → 小标题 17sp / 24sp
fontSizeXL: 22    → 页面标题 22sp / 30sp
controlHeight: 36 → 控件高 36dp
borderRadius: 10  → 控件圆角 10dp
```

字族：`-apple-system, BlinkMacSystemFont, "Segoe UI", "Inter", "Noto Sans SC", sans-serif`
（`layout.css` L11-13）→ Compose 用系统默认 + `FontFamily.Default` 即可，中文自动落 Noto Sans SC。

**`Type.kt` 现在的 23/19/16/14/15/10 全部作废**（那是 OpenMuse 的）。

新 `OctopTypography`：

| M3 槽位 | 字号/行高/字重 |
|---|---|
| headlineMedium | 22 / 30 / SemiBold |
| headlineSmall | 17 / 24 / SemiBold |
| titleMedium | 15 / 22 / SemiBold |
| titleSmall | 13 / 19 / Medium |
| bodyLarge | 15 / 23 / Normal |
| bodyMedium | 15 / 23 / Normal |
| bodySmall | 13 / 19 / Normal |
| labelLarge | 15 / 20 / Medium |
| labelMedium | 13 / 18 / Medium |
| labelSmall | 11 / 16 / Medium |

字距：本体未设负字距（`layout.css` 无 `letter-spacing`），**全部归零**，删掉 OpenMuse 的 −0.7/−0.4/−0.25 与 +1.2。

---

## 4. 文件级施工清单

### 4.1 新增 `ui/theme/OctopTokens.kt`

```kotlin
package com.octop.mobile.ui.theme

enum class OctopPalette { ROSE, TECH, INDIGO, TEAL, VIOLET, EMERALD, AMBER, SLATE;
    companion object { val DEFAULT = ROSE }
}

@Immutable
data class OctopColors(
    val bgPrimary: Color, val bgSecondary: Color, val bgTertiary: Color,
    val bgElevated: Color, val bgLayout: Color, val bgContainer: Color,
    val bgSidebar: Color, val bgPage: Color, val bgSubtle: Color,
    val bgCode: Color, val bgToolbar: Color, val bgGlass: Color,
    val bgHover: Color, val bgActive: Color, val bgSelected: Color,
    val surface: Color, val surfaceSunken: Color,
    val textPrimary: Color, val textSecondary: Color, val textTertiary: Color,
    val textQuaternary: Color, val textPlaceholder: Color, val textInverse: Color,
    val borderPrimary: Color, val borderSecondary: Color, val borderInput: Color,
    val borderStrong: Color, val borderFocus: Color,
    val brand: Color, val brandHover: Color, val brandActive: Color,
    val brandSoft: Color, val brandLight: Color, val brandBg: Color,
    val brandBorder: Color,
    val assistantBubbleBg: Color, val assistantBubbleBorder: Color,
    val success: Color, val danger: Color, val warning: Color,
    val warningText: Color, val info: Color,
    val sidebarBorder: Color, val sidebarItemHover: Color,
    val sidebarItemActiveBg: Color, val sidebarItemActiveText: Color,
    val sidebarGroupText: Color,
    val rowSelectedBg: Color, val rowSelectedBorder: Color,
    val isDark: Boolean,
)

val LocalOctopColors = staticCompositionLocalOf { octopColors(OctopPalette.DEFAULT, false) }
fun octopColors(palette: OctopPalette, dark: Boolean): OctopColors
```

`octopColors()` 用 `when(palette){...}` × `when(dark)` 返回，数值照 §2 表。
`brandActive` 取 `ANTD_BRAND_TOKENS` 的 `colorPrimaryActive`（rose 明 = `#B83A50`）。

### 4.2 重写 `ui/theme/Color.kt`（保留全部旧名）

旧名 → 新令牌映射（**必须全部保留，否则 35 个调用点崩**）：

| 旧名 | 新映射 |
|---|---|
| `Bg` | bgPage |
| `Surface1` | surface |
| `Surface2` | bgSecondary |
| `Surface3` | bgTertiary |
| `SurfaceDeep` | bgTertiary 加深（用 `bgActive`） |
| `Accent` | brand |
| `AccentSoft` | brandLight |
| `Teal` | info（原语义色已不存在，用 info 兜底） |
| `Amber` | warningText |
| `Rose` | danger |
| `Green` | success |
| `TextPrimary` | textPrimary |
| `TextSecondary` | textSecondary |
| `TextTertiary` | textTertiary |
| `OutlineCol` | borderPrimary |
| `SkyBlue` / `HeroBlue` / `SoftGreen` / `SoftLavender` / `SoftOrange` | 分别 → brandBg / bgSecondary / success 8% / brandBg / warning 8%（Compose 用 `copy(alpha=)` 合成） |

全部写成：

```kotlin
val Bg: Color @Composable @ReadOnlyComposable get() = LocalOctopColors.current.bgPage
```

同时新增一批语义名供新代码用（`BgPrimary`、`BorderPrimary`、`Brand`、`BrandLight`…），
新代码只用新名，旧代码继续用旧名。

### 4.3 重写 `ui/theme/Theme.kt`

```kotlin
@Composable
fun OctopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: OctopPalette = OctopPalette.DEFAULT,
    content: @Composable () -> Unit
)
```
内部：`CompositionLocalProvider(LocalOctopColors provides octopColors(palette, darkTheme))`
+ `MaterialTheme(colorScheme = if (darkTheme) darkScheme else lightScheme, typography = OctopTypography)`。

`darkScheme` / `lightScheme` 全部由 `OctopColors` 派生（primary=brand、background=bgPage、surface=surface、error=danger…），
删掉现有的 `lightColorScheme(...)` 硬编码。

### 4.4 新增 `ui/theme/ThemeState.kt`

照抄 `dashboard/src/styles/appearanceStorage.ts` 的语义：
- `ThemeMode = SYSTEM | LIGHT | DARK`
- `palette: OctopPalette`
- 存 `SharedPreferences`（键名对齐本体 `THEME_STORAGE_KEY = "theme"`、`octop:ui-palette`）
- `@Composable fun rememberThemeState(): ThemeState`，对外 `mode` / `palette` / `isDark`（SYSTEM 时取 `isSystemInDarkTheme()`）

`MainActivity.kt` 里把 `OctopTheme { }` 换成 `OctopTheme(darkTheme = themeState.isDark, palette = themeState.palette) { }`。

### 4.5 `ui/OctopRoot.kt` 底部 tab（**唯一需要重新设计的部件**）

现在的问题：`background(Color.White, RoundedCornerShape(40.dp))`、选中态 `Color(0xFFF0F1F2)` —— 硬白，暗色下彻底崩。

本体没有底部 tab（它用抽屉侧边栏），所以这里**不是抄，是沿用本体令牌做等价物**：

| 部件 | 现值 | 改为 |
|---|---|---|
| 外容器底色 | `Color.White` | `BgPrimary`（surface） |
| 外容器阴影 | `shadow(ambient 5%, spot 10%)` | `ShadowLg` 等价 elevation，暗色降为 `rgba(0,0,0,.35)` |
| 选中项底色 | `Color(0xFFF0F1F2)` | `SidebarItemActiveBg`（对齐本体侧边栏选中态） |
| 选中项图标/文字 | 无（默认） | `SidebarItemActiveText` |
| 未选中图标/文字 | `TextTertiary` | `TextSecondary` / `TextTertiary` 保持 |
| 徽标色 | `Rose` | `Danger` |
| 圆角 40 / 28 / 高 47 | 保留 | 保留（这是手机手势区合理值） |
| 标签字号 9sp | 保留 | 保留（不影响一致性，本体无此部件） |

### 4.6 `ui/components/Common.kt`

| 部件 | 现值 | 改为 |
|---|---|---|
| `PageHeader` 标题 22sp SemiBold ls −0.3 | OpenMuse | 22sp SemiBold **ls 0**（本体无负字距） |
| `PageHeader` 副标题 13sp/18sp | 近 | 13sp/19sp（本体 fontSizeSM） |
| `SectionHeader` | 16sp | 15sp SemiBold（本体 titleMedium） |
| `Pill` 底色 `AccentSoft` + 圆角 24 | OpenMuse 蓝 | `BrandLight` + `RadiusFull`；未选中描边 `BorderInput` |
| `EmptyHint` | `TextTertiary` | `TextTertiary`（不变） |
| `Pill` 内边距 18/9 | 保留 | 保留 |

### 4.7 `ui/screens/ChatScreen.kt` —— 抄本体气泡栅格

源：`dashboard/src/pages/Chat/chatMessages.partial.less` L898-1000 的 `@media (max-width:767px)` 块。

本体桌面是 3 列 grid，手机断点下改成 2 列。逐条翻译：

```
容器 grid-template-columns = var(--chat-msg-avatar-slot, 44px) minmax(0, 1fr)
助手气泡：avatar 在第 1 行、bubble 在第 2 行横跨 1/-1，row-gap 6dp
用户气泡：镜像 —— minmax(0,1fr) … 44px，avatar 在右
.processSummary  padding-inline 14dp（普通态 8/12）
.messageList    隐藏滚动条
```

Compose 实现：

```kotlin
val AvatarSlot = 44.dp
Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
    if (isAssistant) { Avatar(...); Spacer(8.dp) }
    Column(Modifier.weight(1f)) {
        Text(content, style = bodyLarge, color = TextPrimary)
    }
    if (!isAssistant) { Spacer(8.dp); Avatar(...) }
}
```

关键点（都是本体 CSS 里明确写的）：
1. 头像**固定 44dp 槽位**，不随文字宽度浮动。
2. 助手正文 `fillMaxWidth()` 占满剩余宽度，不再有 `max-width: 75%` 限制 —— 手机上 75% 会让长回答窄得难受。用户气泡保留最大宽限制。
3. 助手：头像与正文**同一行**；若头像独立成行则 `row-gap 6dp`。
4. 气泡背景用 `AssistantBubbleBg` + `AssistantBubbleBorder`（磨砂），**不是**现在的 `Surface2` 纯灰。
5. `.processSummary`（思考过程折叠条）`padding-inline = 14.dp`，背景 `rgba(15,23,42,.025)` → 暗色 `rgba(255,255,255,.04)`。
6. 消息列表**隐藏滚动条**（本体 `scrollbar-width: none`）。

### 4.8 键盘与安全区（本体已解决，直接抄结论）

源：`dashboard/src/hooks/viewport.ts`

| 本体常量 | 值 | Compose 对应 |
|---|---|---|
| `KEYBOARD_GAP_THRESHOLD_PX` | 80 | `ime` insets < 80px 时不加底部 padding（避开手势条，别重复留白） |
| `MOBILE_VIEWPORT_MQ` | `(max-width:767px)` | 不用，手机恒真 |
| 安全区 | `env(safe-area-inset-*)` | `WindowInsets.safeDrawing` |

`MainActivity.kt`：
- `WindowCompat.setDecorFitsSystemWindows(window, false)`
- 输入框容器 `.imePadding()` + `.navigationBarsPadding()`
- `android:windowSoftInputMode="adjustResize"`（`AndroidManifest.xml`，现在若缺就补）

---

## 5. 并行开发分工（文件所有权，互不冲突）

| 智能体 | 独占文件 | 依赖 |
|---|---|---|
| **A 主题内核** | 新建 `Old→OctopTokens.kt`、`ThemeState.kt`；重写 `Color.kt`、`Theme.kt`、`Type.kt` | 无（最先落地） |
| **B 导航壳** | `ui/OctopRoot.kt`、`MainActivity.kt`、`AndroidManifest.xml` | 读 A 的契约，不读 A 的文件 |
| **C 对话页** | `ui/screens/ChatScreen.kt`、`ui/components/Common.kt` | 同上 |
| **D 设置页清扫** | `ui/screens/settings/*.kt`（12 个文件里的硬编码色） | 同上 |

**契约冻结**：§4.1 的 `OctopColors` 字段名、`OctopPalette` 枚举名、`octopColors()` 签名、
§4.2 的旧名映射表 —— 四方都按这份写，谁也不许改。A 不实现完之前 B/C/D 也能编译通过吗？
不能，所以顺序是：**先派 A，A 落地后 B/C/D 同时开工**。但这三步之间只有一次等待，整体仍是并行的。

---

## 6. 验收

1. `dev.ps1 build` 通过。
2. 手机暗色模式下：底部 tab 不再是硬白色、气泡不再是纯灰块、文字不消失。
3. 设置里切 8 套配色，主色随之变化。
4. `grep -c "Color(0x"` 在 `ui/screens/` 下归零（除 `Color.Transparent`/`Color.White` 这类中性值）。
5. 对话页长回答在手机宽度下占满行宽，头像固定 44dp。

---

## 7. 明确不做

- 不改任何网络/协议/状态管理代码（`OctopApi.kt`、`ChatSocket.kt`、`AppViewModel.kt`、`SyncRepository.kt` 一个字节都不动）。
- 不改 `docs/design.md` 的业务描述，但其中蓝色 `#1473C8` 规格作废。
- 不引入 WebView（v1 方案的路线 C 暂不启动，先做原生令牌移植；两者不冲突，可后续再加）。
