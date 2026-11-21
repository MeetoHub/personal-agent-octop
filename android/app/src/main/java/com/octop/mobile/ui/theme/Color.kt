package com.octop.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/* ───────────────────────── 颜色入口 ─────────────────────────
 * 全部颜色来自 Octop 本体的 theme-vars.css（dashboard/src/styles/theme-vars.css），
 * 分三层：
 *   1) 语义色（BgPrimary / Brand / TextSecondary …）—— 新代码请直接用这些；
 *   2) 旧名兼容（Bg / Surface1 / Accent / Teal …）—— 映射规则见方案 §4.2，老页面不用改；
 *   3) *Fallback 常量 —— 给非 @Composable 上下文（when 表达式、普通函数）用。
 *
 * 因为第 1、2 层是 @Composable getter，写法上和原来的 val 常量一模一样，
 * 所有调用点都不需要加 import、不需要改括号。
 */

/** 语义色板，切换配色/明暗时由 OctopTheme 通过 CompositionLocal 下发 */
val octop: OctopColors
    @Composable @ReadOnlyComposable get() = LocalOctopColors.current

/* ───────────────────── 1. 新语义名 ───────────────────── */

// 背景（theme-vars.css --fn-bg-*）
val BgPrimary: Color @Composable @ReadOnlyComposable get() = octop.bgPrimary
val BgSecondary: Color @Composable @ReadOnlyComposable get() = octop.bgSecondary
val BgTertiary: Color @Composable @ReadOnlyComposable get() = octop.bgTertiary
val BgElevated: Color @Composable @ReadOnlyComposable get() = octop.bgElevated
val BgLayout: Color @Composable @ReadOnlyComposable get() = octop.bgLayout
val BgContainer: Color @Composable @ReadOnlyComposable get() = octop.bgContainer
val BgSidebar: Color @Composable @ReadOnlyComposable get() = octop.bgSidebar
val BgPage: Color @Composable @ReadOnlyComposable get() = octop.bgPage
val BgSubtle: Color @Composable @ReadOnlyComposable get() = octop.bgSubtle
val BgCode: Color @Composable @ReadOnlyComposable get() = octop.bgCode
val BgToolbar: Color @Composable @ReadOnlyComposable get() = octop.bgToolbar
val BgGlass: Color @Composable @ReadOnlyComposable get() = octop.bgGlass
val BgHover: Color @Composable @ReadOnlyComposable get() = octop.bgHover
val BgActive: Color @Composable @ReadOnlyComposable get() = octop.bgActive
val BgSelected: Color @Composable @ReadOnlyComposable get() = octop.bgSelected

// 面（--fn-surface）
val Surface: Color @Composable @ReadOnlyComposable get() = octop.surface
val SurfaceSunken: Color @Composable @ReadOnlyComposable get() = octop.surfaceSunken

// 文字（--fn-text-*）
val TextPrimaryColor: Color @Composable @ReadOnlyComposable get() = octop.textPrimary
val TextSecondaryColor: Color @Composable @ReadOnlyComposable get() = octop.textSecondary
val TextTertiaryColor: Color @Composable @ReadOnlyComposable get() = octop.textTertiary
val TextQuaternary: Color @Composable @ReadOnlyComposable get() = octop.textQuaternary
val TextPlaceholder: Color @Composable @ReadOnlyComposable get() = octop.textPlaceholder
val TextInverse: Color @Composable @ReadOnlyComposable get() = octop.textInverse

// 边框（--fn-border-*）
val BorderPrimary: Color @Composable @ReadOnlyComposable get() = octop.borderPrimary
val BorderSecondary: Color @Composable @ReadOnlyComposable get() = octop.borderSecondary
val BorderInput: Color @Composable @ReadOnlyComposable get() = octop.borderInput
val BorderStrong: Color @Composable @ReadOnlyComposable get() = octop.borderStrong
val BorderFocus: Color @Composable @ReadOnlyComposable get() = octop.borderFocus

// 品牌（--fn-color-brand-*）
val Brand: Color @Composable @ReadOnlyComposable get() = octop.brand
val BrandHover: Color @Composable @ReadOnlyComposable get() = octop.brandHover
val BrandActive: Color @Composable @ReadOnlyComposable get() = octop.brandActive
val BrandSoft: Color @Composable @ReadOnlyComposable get() = octop.brandSoft
val BrandLight: Color @Composable @ReadOnlyComposable get() = octop.brandLight
val BrandBg: Color @Composable @ReadOnlyComposable get() = octop.brandBg
val BrandBorder: Color @Composable @ReadOnlyComposable get() = octop.brandBorder
val TextBrand: Color @Composable @ReadOnlyComposable get() = octop.textBrand
/**
 * 压在品牌色/状态色实心块上的文字色（--fn-color-on-brand: #ffffff）。
 * 明暗都是纯白，千万别用 TextInverse 代替：暗色下 TextInverse 是近黑色，会看不清。
 */
val OnBrand: Color @Composable @ReadOnlyComposable get() = octop.onBrand
val AssistantBubbleBg: Color @Composable @ReadOnlyComposable get() = octop.assistantBubbleBg
val AssistantBubbleBorder: Color @Composable @ReadOnlyComposable get() = octop.assistantBubbleBorder

// 语义（--fn-color-success / -danger / -warning / -info）
val Success: Color @Composable @ReadOnlyComposable get() = octop.success
val Danger: Color @Composable @ReadOnlyComposable get() = octop.danger
val Warning: Color @Composable @ReadOnlyComposable get() = octop.warning
val WarningText: Color @Composable @ReadOnlyComposable get() = octop.warningText
val Info: Color @Composable @ReadOnlyComposable get() = octop.info

// 侧栏 / 列表选中（--fn-sidebar-* / --fn-row-selected-*）
val SidebarBorder: Color @Composable @ReadOnlyComposable get() = octop.sidebarBorder
val SidebarItemHover: Color @Composable @ReadOnlyComposable get() = octop.sidebarItemHover
val SidebarItemActiveBg: Color @Composable @ReadOnlyComposable get() = octop.sidebarItemActiveBg
val SidebarItemActiveText: Color @Composable @ReadOnlyComposable get() = octop.sidebarItemActiveText
val SidebarGroupText: Color @Composable @ReadOnlyComposable get() = octop.sidebarGroupText
val RowSelectedBg: Color @Composable @ReadOnlyComposable get() = octop.rowSelectedBg
val RowSelectedBorder: Color @Composable @ReadOnlyComposable get() = octop.rowSelectedBorder

/** 当前是否暗色，组件需要按明暗微调时用 */
val IsDark: Boolean @Composable @ReadOnlyComposable get() = octop.isDark

/* ───────────────────── 2. 旧名兼容层 ─────────────────────
 * 方案 §4.2 冻结的映射关系。老页面写 Bg / Surface1 / Accent，编译后取的是新语义色。
 */

// 背景：原来的 Bg 是页面底色，现在落到 --fn-bg-page
val Bg: Color @Composable @ReadOnlyComposable get() = octop.bgPage
// Surface1 是卡片白面 → --fn-surface
val Surface1: Color @Composable @ReadOnlyComposable get() = octop.surface
// Surface2 是浅一档的分块底 → --fn-bg-secondary
val Surface2: Color @Composable @ReadOnlyComposable get() = octop.bgSecondary
// Surface3 是再深一档 → --fn-bg-tertiary
val Surface3: Color @Composable @ReadOnlyComposable get() = octop.bgTertiary
// SurfaceDeep 是按下态的最深底 → --fn-bg-active
val SurfaceDeep: Color @Composable @ReadOnlyComposable get() = octop.bgActive

// Accent 是品牌主色 → --fn-color-brand
val Accent: Color @Composable @ReadOnlyComposable get() = octop.brand
// AccentSoft 是品牌浅底 → --fn-color-brand-light
val AccentSoft: Color @Composable @ReadOnlyComposable get() = octop.brandLight

// Teal 在老代码里表示"辅助/信息"色 → --fn-color-info
val Teal: Color @Composable @ReadOnlyComposable get() = octop.info
// Amber 在老代码里表示"警告文字" → --fn-color-warning-text
val Amber: Color @Composable @ReadOnlyComposable get() = octop.warningText
// Rose 在老代码里表示"危险/错误" → --fn-color-danger
val Rose: Color @Composable @ReadOnlyComposable get() = octop.danger
// Green 在老代码里表示"成功" → --fn-color-success
val Green: Color @Composable @ReadOnlyComposable get() = octop.success

val TextPrimary: Color @Composable @ReadOnlyComposable get() = octop.textPrimary
val TextSecondary: Color @Composable @ReadOnlyComposable get() = octop.textSecondary
val TextTertiary: Color @Composable @ReadOnlyComposable get() = octop.textTertiary
// OutlineCol 是分割线/描边 → --fn-border-primary
val OutlineCol: Color @Composable @ReadOnlyComposable get() = octop.borderPrimary

// SkyBlue 是品牌浅蓝底 → 用 --fn-color-brand-bg
val SkyBlue: Color @Composable @ReadOnlyComposable get() = octop.brandBg
// HeroBlue 是老登录页的蓝底块 → 用 --fn-bg-secondary
val HeroBlue: Color @Composable @ReadOnlyComposable get() = octop.bgSecondary
// SoftGreen 原来是固定色块，现在改成成功色 8% 透明底
val SoftGreen: Color @Composable @ReadOnlyComposable get() = octop.success.copy(alpha = 0.08f)
// SoftLavender 是紫色浅底 → 归到品牌浅底
val SoftLavender: Color @Composable @ReadOnlyComposable get() = octop.brandBg
// SoftOrange 是橙色浅底，现在改成警告色 8% 透明底
val SoftOrange: Color @Composable @ReadOnlyComposable get() = octop.warning.copy(alpha = 0.08f)

/* ───────────────────── 3. 非 @Composable 兜底 ─────────────────────
 * @Composable getter 不能在普通函数里读。下面这组常量取"玫瑰浅色"一套固定值，
 * 只给确实拿不到 CompositionLocal 的地方用（已知调用点见交付说明），
 * 新代码不要用；暗色下会有色差。
 */

/** 玫瑰浅色的品牌主色，非 @Composable 兜底 */
val AccentFallback = Color(0xFFE85D75)
/** 品牌实心块上的文字色，两种模式都是纯白，任何上下文都能直接用 */
val OnBrandFallback = Color(0xFFFFFFFF)
val BgFallback = Color(0xFFF5F6F8)
val Surface1Fallback = Color(0xFFFFFFFF)
val Surface2Fallback = Color(0xFFF7F8FA)
val Surface3Fallback = Color(0xFFF0F1F3)
val SurfaceDeepFallback = Color(0xFFF0F1F3)
val TextPrimaryFallback = Color(0xFF111827)
val TextSecondaryFallback = Color(0xFF4B5563)
val TextTertiaryFallback = Color(0xFF9CA3AF)
val OutlineFallback = Color(0xFFE5E7EB)
val SuccessFallback = Color(0xFF10B981)
val DangerFallback = Color(0xFFEF4444)
val WarningFallback = Color(0xFFF59E0B)
val WarningTextFallback = Color(0xFFD97706)
val InfoFallback = Color(0xFF3B82F6)
