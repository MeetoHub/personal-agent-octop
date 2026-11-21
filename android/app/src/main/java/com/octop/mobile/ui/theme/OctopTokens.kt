package com.octop.mobile.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ───────────────────────── 品牌配色 ─────────────────────────
 * 8 套配色，键名与 theme-vars.css 的 [data-palette="…"] 段一致（rose/tech/indigo/teal/violet/emerald/amber/slate）。
 * 桌面端存 localStorage 的 palette 字段，手机端存 SharedPreferences（键 octop:ui-palette）。
 */
enum class OctopPalette(val key: String, val labelZh: String) {
    ROSE("rose", "玫瑰"),
    TECH("tech", "科技蓝"),
    INDIGO("indigo", "靛蓝"),
    TEAL("teal", "青绿"),
    VIOLET("violet", "紫罗兰"),
    EMERALD("emerald", "翡翠"),
    AMBER("amber", "琥珀"),
    SLATE("slate", "石墨");

    companion object {
        /** 默认玫瑰，与 themePalettes.ts 的 DEFAULT_PALETTE 一致 */
        val DEFAULT = ROSE

        fun fromKey(key: String?): OctopPalette = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/** 带 alpha 的颜色：把 0xRRGGBB 换成 CSS 里的 rgba(r,g,b,a) */
private fun withA(base: Long, alpha: Float): Color = Color(base).copy(alpha = alpha)

/**
 * 一套配色里「跟品牌色挂钩」的那部分 token。
 * 每套配色、明暗各一份，其余中性色两套配色共用（见 [Neutrals]）。
 */
private data class Tint(
    val brand: Color,                 // --fn-color-brand
    val brandHover: Color,            // --fn-color-brand-hover
    val brandActive: Color,           // themePalettes.ts colorPrimaryActive
    val textBrand: Color,             // --fn-text-brand
    val brandBg: Color,               // --fn-color-brand-bg
    val brandLight: Color,            // --fn-color-brand-light
    val brandBorder: Color,           // --fn-color-brand-border
    val bubbleBg: Color,              // --fn-assistant-bubble-bg
    val bubbleBorder: Color,          // --fn-assistant-bubble-border
    val hover: Color,                 // --fn-bg-hover
    val active: Color,                // --fn-bg-active
    val selected: Color,              // --fn-bg-selected
    val sidebarActiveBg: Color,       // --fn-sidebar-item-active-bg
    val sidebarActiveText: Color,     // --fn-sidebar-item-active-text
    val rowBorder: Color,             // --fn-row-selected-border
)

/** 与配色无关的中性色 + 语义色，明暗各一份 */
private data class Neutrals(
    val bgPrimary: Color,
    val bgSecondary: Color,
    val bgTertiary: Color,
    val bgElevated: Color,
    val bgLayout: Color,
    val bgContainer: Color,
    val bgSidebar: Color,
    val bgPage: Color,
    val bgSubtle: Color,
    val bgCode: Color,
    val bgToolbar: Color,
    val bgGlass: Color,
    val surface: Color,
    val surfaceSunken: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textQuaternary: Color,
    val textPlaceholder: Color,
    val textInverse: Color,
    val borderPrimary: Color,
    val borderSecondary: Color,
    val borderInput: Color,
    val borderStrong: Color,
    val success: Color,
    val danger: Color,
    val warning: Color,
    val warningText: Color,
    val info: Color,
    val sidebarBorder: Color,
    val sidebarItemHover: Color,
    val sidebarGroupText: Color,
)

/** 浅色中性色：theme-vars.css `:root` 段（第 10–120 行） */
private val LightNeutrals = Neutrals(
    bgPrimary = Color(0xFFFFFFFF),            // --fn-bg-primary
    bgSecondary = Color(0xFFF7F8FA),          // --fn-bg-secondary
    bgTertiary = Color(0xFFF0F1F3),           // --fn-bg-tertiary
    bgElevated = Color(0xFFFFFFFF),           // --fn-bg-elevated
    bgLayout = Color(0xFFF5F6F8),             // --fn-bg-layout
    bgContainer = Color(0xFFFFFFFF),          // --fn-bg-container
    bgSidebar = Color(0xFFFFFFFF),            // --fn-bg-sidebar
    bgPage = Color(0xFFF5F6F8),               // --fn-bg-page
    bgSubtle = Color(0xFFF3F4F6),             // --fn-bg-subtle
    bgCode = Color(0xFFF5F6F8),               // --fn-bg-code
    bgToolbar = Color(0xFFFAFBFC),            // --fn-bg-toolbar
    bgGlass = withA(0xFFFFFFFF, 0.8f),        // --fn-bg-glass
    surface = Color(0xFFFFFFFF),              // --fn-surface
    surfaceSunken = Color(0xFFF7F8FA),        // --fn-surface-sunken
    textPrimary = Color(0xFF111827),          // --fn-text-primary
    textSecondary = Color(0xFF4B5563),        // --fn-text-secondary
    textTertiary = Color(0xFF9CA3AF),         // --fn-text-tertiary
    textQuaternary = Color(0xFFD1D5DB),       // --fn-text-quaternary
    textPlaceholder = Color(0xFFC0C5CE),      // --fn-text-placeholder
    textInverse = Color(0xFFFFFFFF),          // --fn-text-inverse
    borderPrimary = Color(0xFFE5E7EB),        // --fn-border-primary
    borderSecondary = Color(0xFFF3F4F6),      // --fn-border-secondary
    borderInput = Color(0xFFD1D5DB),          // --fn-border-input
    borderStrong = Color(0xFFD1D5DB),         // --fn-border-strong
    success = Color(0xFF10B981),              // --fn-color-success
    danger = Color(0xFFEF4444),               // --fn-color-danger / -error
    warning = Color(0xFFF59E0B),              // --fn-color-warning
    warningText = Color(0xFFD97706),          // --fn-color-warning-text
    info = Color(0xFF3B82F6),                 // --fn-color-info
    sidebarBorder = Color(0xFFEBEBEB),        // --fn-sidebar-border
    sidebarItemHover = Color(0xFFF5F5F5),     // --fn-sidebar-item-hover
    sidebarGroupText = Color(0xFFB0B5BE),     // --fn-sidebar-group-text
)

/** 暗色中性色：theme-vars.css `[data-theme="dark"]` 段（第 195–330 行） */
private val DarkNeutrals = Neutrals(
    bgPrimary = Color(0xFF141414),
    bgSecondary = Color(0xFF1A1A1A),
    bgTertiary = Color(0xFF222222),
    bgElevated = Color(0xFF1E1E1E),
    bgLayout = Color(0xFF0E0E0E),
    bgContainer = Color(0xFF141414),
    bgSidebar = Color(0xFF141414),
    bgPage = Color(0xFF0E0E0E),
    bgSubtle = Color(0xFF1E1E1E),
    bgCode = Color(0xFF1E1E1E),
    bgToolbar = Color(0xFF161616),
    bgGlass = withA(0xFF0E0E0E, 0.8f),
    surface = Color(0xFF1E1E1E),
    surfaceSunken = Color(0xFF1A1A1A),
    textPrimary = withA(0xFFFFFFFF, 0.9f),
    textSecondary = withA(0xFFFFFFFF, 0.6f),
    textTertiary = withA(0xFFFFFFFF, 0.35f),
    textQuaternary = withA(0xFFFFFFFF, 0.2f),
    textPlaceholder = withA(0xFFFFFFFF, 0.16f),
    textInverse = Color(0xFF0E0E0E),
    borderPrimary = withA(0xFFFFFFFF, 0.08f),
    borderSecondary = withA(0xFFFFFFFF, 0.05f),
    borderInput = withA(0xFFFFFFFF, 0.12f),
    borderStrong = withA(0xFFFFFFFF, 0.16f),
    success = Color(0xFF34D399),
    danger = Color(0xFFF87171),
    warning = Color(0xFFFBBF24),
    warningText = Color(0xFFFBBF24),
    info = Color(0xFF60A5FA),
    sidebarBorder = withA(0xFFFFFFFF, 0.06f),
    sidebarItemHover = withA(0xFFFFFFFF, 0.05f),
    sidebarGroupText = withA(0xFFFFFFFF, 0.28f),
)

/**
 * 取某套配色在明/暗下的品牌 token。
 * 数值逐条来自 theme-vars.css 的 8 个 [data-palette] 段 × 明暗两段；
 * brandActive 只有桌面端 themePalettes.ts 的 colorPrimaryActive 提供。
 */
private fun tintOf(palette: OctopPalette, dark: Boolean): Tint = when (palette) {
    // ── 玫瑰（默认）──
    OctopPalette.ROSE -> if (dark) Tint(
        brand = Color(0xFFF08B9A), brandHover = Color(0xFFE85D75), brandActive = Color(0xFFE85D75),
        textBrand = Color(0xFFF08B9A), brandBg = withA(0xFFE85D75, 0.12f),
        brandLight = withA(0xFFE85D75, 0.16f), brandBorder = withA(0xFFE85D75, 0.35f),
        bubbleBg = withA(0xFFFFFFFF, 0.06f),
        bubbleBorder = withA(0xFFF08B9A, 0.14f),
        hover = withA(0xFFE85D75, 0.08f), active = withA(0xFFE85D75, 0.14f), selected = withA(0xFFE85D75, 0.10f),
        sidebarActiveBg = withA(0xFFE85D75, 0.14f), sidebarActiveText = Color(0xFFF08B9A),
        rowBorder = Color(0xFFF08B9A),
    ) else Tint(
        brand = Color(0xFFE85D75), brandHover = Color(0xFFD14A62), brandActive = Color(0xFFB83A50),
        textBrand = Color(0xFFE85D75), brandBg = withA(0xFFE85D75, 0.06f),
        brandLight = Color(0xFFFDE8EC), brandBorder = withA(0xFFE85D75, 0.28f),
        bubbleBg = withA(0xFFFFFFFF, 0.45f), bubbleBorder = withA(0xFFE85D75, 0.12f),
        hover = withA(0xFFE85D75, 0.04f), active = withA(0xFFE85D75, 0.08f), selected = withA(0xFFE85D75, 0.06f),
        sidebarActiveBg = Color(0xFFFDE8EC), sidebarActiveText = Color(0xFFE85D75),
        rowBorder = Color(0xFFE85D75),
    )

    // ── 科技蓝 ──
    OctopPalette.TECH -> if (dark) Tint(
        brand = Color(0xFF3A5FE0), brandHover = Color(0xFF2E4FD4), brandActive = Color(0xFF233FB8),
        textBrand = Color(0xFF7B9BFC), brandBg = withA(0xFF4B74FA, 0.14f),
        brandLight = withA(0xFF4B74FA, 0.18f), brandBorder = withA(0xFF3A5FE0, 0.35f),
        bubbleBg = withA(0xFFFFFFFF, 0.06f), bubbleBorder = withA(0xFF7B9BFC, 0.16f),
        hover = withA(0xFF4B74FA, 0.10f), active = withA(0xFF4B74FA, 0.16f), selected = withA(0xFF4B74FA, 0.12f),
        sidebarActiveBg = withA(0xFF4B74FA, 0.16f), sidebarActiveText = Color(0xFF7B9BFC),
        rowBorder = Color(0xFF7B9BFC),
    ) else Tint(
        brand = Color(0xFF3A5FE0), brandHover = Color(0xFF2E4FD4), brandActive = Color(0xFF233FB8),
        textBrand = Color(0xFF3A5FE0), brandBg = withA(0xFF4B74FA, 0.06f),
        brandLight = Color(0xFFF1F4FF), brandBorder = withA(0xFF3A5FE0, 0.28f),
        bubbleBg = withA(0xFFFFFFFF, 0.45f), bubbleBorder = withA(0xFF4B74FA, 0.12f),
        hover = withA(0xFF4B74FA, 0.04f), active = withA(0xFF4B74FA, 0.08f), selected = withA(0xFF4B74FA, 0.06f),
        sidebarActiveBg = Color(0xFFF1F4FF), sidebarActiveText = Color(0xFF4B74FA),
        rowBorder = Color(0xFF4B74FA),
    )

    // ── 靛蓝 ──
    OctopPalette.INDIGO -> if (dark) Tint(
        brand = Color(0xFF4F46E5), brandHover = Color(0xFF4338CA), brandActive = Color(0xFF3730A3),
        textBrand = Color(0xFF818CF8), brandBg = withA(0xFF6366F1, 0.14f),
        brandLight = withA(0xFF6366F1, 0.18f), brandBorder = withA(0xFF4F46E5, 0.35f),
        bubbleBg = withA(0xFFFFFFFF, 0.06f), bubbleBorder = withA(0xFF818CF8, 0.16f),
        hover = withA(0xFF6366F1, 0.10f), active = withA(0xFF6366F1, 0.16f), selected = withA(0xFF6366F1, 0.12f),
        sidebarActiveBg = withA(0xFF6366F1, 0.16f), sidebarActiveText = Color(0xFF818CF8),
        rowBorder = Color(0xFF818CF8),
    ) else Tint(
        brand = Color(0xFF4F46E5), brandHover = Color(0xFF4338CA), brandActive = Color(0xFF3730A3),
        textBrand = Color(0xFF4F46E5), brandBg = withA(0xFF6366F1, 0.06f),
        brandLight = Color(0xFFEEF2FF), brandBorder = withA(0xFF4F46E5, 0.28f),
        bubbleBg = withA(0xFFFFFFFF, 0.45f), bubbleBorder = withA(0xFF6366F1, 0.12f),
        hover = withA(0xFF6366F1, 0.04f), active = withA(0xFF6366F1, 0.08f), selected = withA(0xFF6366F1, 0.06f),
        sidebarActiveBg = Color(0xFFEEF2FF), sidebarActiveText = Color(0xFF6366F1),
        rowBorder = Color(0xFF6366F1),
    )

    // ── 青绿 ──
    OctopPalette.TEAL -> if (dark) Tint(
        brand = Color(0xFF0F766E), brandHover = Color(0xFF115E59), brandActive = Color(0xFF134E4A),
        textBrand = Color(0xFF2DD4BF), brandBg = withA(0xFF0D9488, 0.14f),
        brandLight = withA(0xFF0D9488, 0.18f), brandBorder = withA(0xFF0F766E, 0.35f),
        bubbleBg = withA(0xFFFFFFFF, 0.06f), bubbleBorder = withA(0xFF2DD4BF, 0.16f),
        hover = withA(0xFF0D9488, 0.10f), active = withA(0xFF0D9488, 0.16f), selected = withA(0xFF0D9488, 0.12f),
        sidebarActiveBg = withA(0xFF0D9488, 0.16f), sidebarActiveText = Color(0xFF2DD4BF),
        rowBorder = Color(0xFF2DD4BF),
    ) else Tint(
        brand = Color(0xFF0F766E), brandHover = Color(0xFF115E59), brandActive = Color(0xFF134E4A),
        textBrand = Color(0xFF0F766E), brandBg = withA(0xFF0D9488, 0.06f),
        brandLight = Color(0xFFF0FDFA), brandBorder = withA(0xFF0F766E, 0.28f),
        bubbleBg = withA(0xFFFFFFFF, 0.45f), bubbleBorder = withA(0xFF0D9488, 0.12f),
        hover = withA(0xFF0D9488, 0.04f), active = withA(0xFF0D9488, 0.08f), selected = withA(0xFF0D9488, 0.06f),
        sidebarActiveBg = Color(0xFFF0FDFA), sidebarActiveText = Color(0xFF0D9488),
        rowBorder = Color(0xFF0D9488),
    )

    // ── 紫罗兰 ──
    OctopPalette.VIOLET -> if (dark) Tint(
        brand = Color(0xFF7C3AED), brandHover = Color(0xFF6D28D9), brandActive = Color(0xFF5B21B6),
        textBrand = Color(0xFFA78BFA), brandBg = withA(0xFF7C3AED, 0.14f),
        brandLight = withA(0xFF7C3AED, 0.18f), brandBorder = withA(0xFF7C3AED, 0.35f),
        bubbleBg = withA(0xFFFFFFFF, 0.06f), bubbleBorder = withA(0xFFA78BFA, 0.16f),
        hover = withA(0xFF7C3AED, 0.10f), active = withA(0xFF7C3AED, 0.16f), selected = withA(0xFF7C3AED, 0.12f),
        sidebarActiveBg = withA(0xFF7C3AED, 0.16f), sidebarActiveText = Color(0xFFA78BFA),
        rowBorder = Color(0xFFA78BFA),
    ) else Tint(
        brand = Color(0xFF7C3AED), brandHover = Color(0xFF6D28D9), brandActive = Color(0xFF5B21B6),
        textBrand = Color(0xFF7C3AED), brandBg = withA(0xFF7C3AED, 0.06f),
        brandLight = Color(0xFFF5F3FF), brandBorder = withA(0xFF7C3AED, 0.28f),
        bubbleBg = withA(0xFFFFFFFF, 0.45f), bubbleBorder = withA(0xFF7C3AED, 0.12f),
        hover = withA(0xFF7C3AED, 0.04f), active = withA(0xFF7C3AED, 0.08f), selected = withA(0xFF7C3AED, 0.06f),
        sidebarActiveBg = Color(0xFFF5F3FF), sidebarActiveText = Color(0xFF7C3AED),
        rowBorder = Color(0xFF7C3AED),
    )

    // ── 翡翠 ──
    OctopPalette.EMERALD -> if (dark) Tint(
        brand = Color(0xFF047857), brandHover = Color(0xFF065F46), brandActive = Color(0xFF064E3B),
        textBrand = Color(0xFF34D399), brandBg = withA(0xFF10B981, 0.14f),
        brandLight = withA(0xFF10B981, 0.18f), brandBorder = withA(0xFF047857, 0.35f),
        bubbleBg = withA(0xFFFFFFFF, 0.06f), bubbleBorder = withA(0xFF34D399, 0.16f),
        hover = withA(0xFF10B981, 0.10f), active = withA(0xFF10B981, 0.16f), selected = withA(0xFF10B981, 0.12f),
        sidebarActiveBg = withA(0xFF10B981, 0.16f), sidebarActiveText = Color(0xFF34D399),
        rowBorder = Color(0xFF34D399),
    ) else Tint(
        brand = Color(0xFF047857), brandHover = Color(0xFF065F46), brandActive = Color(0xFF064E3B),
        textBrand = Color(0xFF047857), brandBg = withA(0xFF10B981, 0.06f),
        brandLight = Color(0xFFECFDF5), brandBorder = withA(0xFF047857, 0.28f),
        bubbleBg = withA(0xFFFFFFFF, 0.45f), bubbleBorder = withA(0xFF10B981, 0.12f),
        hover = withA(0xFF10B981, 0.04f), active = withA(0xFF10B981, 0.08f), selected = withA(0xFF10B981, 0.06f),
        sidebarActiveBg = Color(0xFFECFDF5), sidebarActiveText = Color(0xFF047857),
        rowBorder = Color(0xFF047857),
    )

    // ── 琥珀 ──
    OctopPalette.AMBER -> if (dark) Tint(
        brand = Color(0xFFB45309), brandHover = Color(0xFF92400E), brandActive = Color(0xFF78350F),
        textBrand = Color(0xFFFBBF24), brandBg = withA(0xFFF59E0B, 0.14f),
        brandLight = withA(0xFFF59E0B, 0.18f), brandBorder = withA(0xFFB45309, 0.35f),
        bubbleBg = withA(0xFFFFFFFF, 0.06f), bubbleBorder = withA(0xFFFBBF24, 0.16f),
        hover = withA(0xFFF59E0B, 0.10f), active = withA(0xFFF59E0B, 0.16f), selected = withA(0xFFF59E0B, 0.12f),
        sidebarActiveBg = withA(0xFFF59E0B, 0.16f), sidebarActiveText = Color(0xFFFBBF24),
        rowBorder = Color(0xFFFBBF24),
    ) else Tint(
        brand = Color(0xFFB45309), brandHover = Color(0xFF92400E), brandActive = Color(0xFF78350F),
        textBrand = Color(0xFFB45309), brandBg = withA(0xFFF59E0B, 0.06f),
        brandLight = Color(0xFFFFFBEB), brandBorder = withA(0xFFB45309, 0.28f),
        bubbleBg = withA(0xFFFFFFFF, 0.45f), bubbleBorder = withA(0xFFF59E0B, 0.14f),
        hover = withA(0xFFF59E0B, 0.04f), active = withA(0xFFF59E0B, 0.08f), selected = withA(0xFFF59E0B, 0.06f),
        sidebarActiveBg = Color(0xFFFFFBEB), sidebarActiveText = Color(0xFFB45309),
        rowBorder = Color(0xFFB45309),
    )

    // ── 石墨（唯一一套灰度品牌色，浅色态的 hover/active/selected 比别套各高 1 个百分点）──
    OctopPalette.SLATE -> if (dark) Tint(
        brand = Color(0xFF475569), brandHover = Color(0xFF334155), brandActive = Color(0xFF1E293B),
        textBrand = Color(0xFF94A3B8), brandBg = withA(0xFF94A3B8, 0.14f),
        brandLight = withA(0xFF94A3B8, 0.18f), brandBorder = withA(0xFF475569, 0.35f),
        bubbleBg = withA(0xFFFFFFFF, 0.06f), bubbleBorder = withA(0xFF94A3B8, 0.16f),
        hover = withA(0xFF94A3B8, 0.10f), active = withA(0xFF94A3B8, 0.16f), selected = withA(0xFF94A3B8, 0.12f),
        sidebarActiveBg = withA(0xFF94A3B8, 0.16f), sidebarActiveText = Color(0xFF94A3B8),
        rowBorder = Color(0xFF94A3B8),
    ) else Tint(
        brand = Color(0xFF475569), brandHover = Color(0xFF334155), brandActive = Color(0xFF1E293B),
        textBrand = Color(0xFF475569), brandBg = withA(0xFF64748B, 0.06f),
        brandLight = Color(0xFFF1F5F9), brandBorder = withA(0xFF475569, 0.28f),
        bubbleBg = withA(0xFFFFFFFF, 0.45f), bubbleBorder = withA(0xFF64748B, 0.14f),
        hover = withA(0xFF64748B, 0.05f), active = withA(0xFF64748B, 0.09f), selected = withA(0xFF64748B, 0.07f),
        sidebarActiveBg = Color(0xFFF1F5F9), sidebarActiveText = Color(0xFF475569),
        rowBorder = Color(0xFF475569),
    )
}

/**
 * 语义色板（方案 §4.1）。所有 UI 都从这里取色，不再直接写 Color(0x…)。
 * [isDark] 让组件能自己判断该用哪套阴影/叠加。
 */
@Immutable
data class OctopColors(
    // 背景
    val bgPrimary: Color,
    val bgSecondary: Color,
    val bgTertiary: Color,
    val bgElevated: Color,
    val bgLayout: Color,
    val bgContainer: Color,
    val bgSidebar: Color,
    val bgPage: Color,
    val bgSubtle: Color,
    val bgCode: Color,
    val bgToolbar: Color,
    val bgGlass: Color,
    val bgHover: Color,
    val bgActive: Color,
    val bgSelected: Color,
    val surface: Color,
    val surfaceSunken: Color,
    // 文字
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textQuaternary: Color,
    val textPlaceholder: Color,
    val textInverse: Color,
    // 边框
    val borderPrimary: Color,
    val borderSecondary: Color,
    val borderInput: Color,
    val borderStrong: Color,
    val borderFocus: Color,
    // 品牌
    val brand: Color,
    val brandHover: Color,
    val brandActive: Color,
    val brandSoft: Color,
    val brandLight: Color,
    val brandBg: Color,
    val brandBorder: Color,
    val textBrand: Color,
    val onBrand: Color,
    val assistantBubbleBg: Color,
    val assistantBubbleBorder: Color,
    // 语义
    val success: Color,
    val danger: Color,
    val warning: Color,
    val warningText: Color,
    val info: Color,
    // 侧栏 / 列表
    val sidebarBorder: Color,
    val sidebarItemHover: Color,
    val sidebarItemActiveBg: Color,
    val sidebarItemActiveText: Color,
    val sidebarGroupText: Color,
    val rowSelectedBg: Color,
    val rowSelectedBorder: Color,
    val isDark: Boolean,
)

/** 按配色 + 明暗算出整套语义色 */
fun octopColors(palette: OctopPalette, dark: Boolean): OctopColors {
    val t = tintOf(palette, dark)
    val n = if (dark) DarkNeutrals else LightNeutrals
    return OctopColors(
        bgPrimary = n.bgPrimary,
        bgSecondary = n.bgSecondary,
        bgTertiary = n.bgTertiary,
        bgElevated = n.bgElevated,
        bgLayout = n.bgLayout,
        bgContainer = n.bgContainer,
        bgSidebar = n.bgSidebar,
        bgPage = n.bgPage,
        bgSubtle = n.bgSubtle,
        bgCode = n.bgCode,
        bgToolbar = n.bgToolbar,
        bgGlass = n.bgGlass,
        bgHover = t.hover,
        bgActive = t.active,
        bgSelected = t.selected,
        surface = n.surface,
        surfaceSunken = n.surfaceSunken,
        textPrimary = n.textPrimary,
        textSecondary = n.textSecondary,
        textTertiary = n.textTertiary,
        textQuaternary = n.textQuaternary,
        textPlaceholder = n.textPlaceholder,
        textInverse = n.textInverse,
        borderPrimary = n.borderPrimary,
        borderSecondary = n.borderSecondary,
        borderInput = n.borderInput,
        borderStrong = n.borderStrong,
        borderFocus = t.textBrand,
        brand = t.brand,
        brandHover = t.brandHover,
        brandActive = t.brandActive,
        // brandSoft = 品牌色 8%（浅）/14%（暗）的浅底，用于 chip、图标底；
        // brandLight 是 CSS 里 solid 的浅底（#fde8ec 这类），不是透明色
        brandSoft = t.active,
        brandLight = t.brandLight,
        brandBg = t.brandBg,
        brandBorder = t.brandBorder,
        textBrand = t.textBrand,
        // --fn-color-on-brand: #ffffff，明暗两种模式都是纯白；
        // 压在品牌色/状态色实心块上的文字必须用它，不能用 textInverse（暗色下 textInverse 是近黑色）
        onBrand = Color(0xFFFFFFFF),
        assistantBubbleBg = t.bubbleBg,
        assistantBubbleBorder = t.bubbleBorder,
        success = n.success,
        danger = n.danger,
        warning = n.warning,
        warningText = n.warningText,
        info = n.info,
        sidebarBorder = n.sidebarBorder,
        sidebarItemHover = n.sidebarItemHover,
        sidebarItemActiveBg = t.sidebarActiveBg,
        sidebarItemActiveText = t.sidebarActiveText,
        sidebarGroupText = n.sidebarGroupText,
        rowSelectedBg = t.selected,
        rowSelectedBorder = t.rowBorder,
        isDark = dark,
    )
}

/**
 * 当前色板。用 staticCompositionLocalOf：切换主题时整棵树重组，符合预期；
 * 默认值取玫瑰浅色，方便 @Preview 和没有 Provider 的单测。
 */
val LocalOctopColors = staticCompositionLocalOf { octopColors(OctopPalette.DEFAULT, false) }

/* ───────────────────────── 圆角 ─────────────────────────
 * theme-vars.css `:root`：--fn-radius-sm 5px / -md 8px / -lg 12px / -xl 18px / -full 9999px，
 * --fn-card-radius 12px。
 */
val RadiusSm: Dp = 5.dp
val RadiusMd: Dp = 8.dp
val RadiusLg: Dp = 12.dp
val RadiusXl: Dp = 18.dp
val RadiusFull: Dp = 9999.dp
val CardRadius: Dp = 12.dp

/* ───────────────────────── 阴影 ─────────────────────────
 * CSS 阴影换算成 Compose 的 elevation：浅色靠低透明度黑，暗色要更黑更重才看得出层次
 * （theme-vars.css `:root` 第 130–150 行 vs `[data-theme="dark"]` 第 330–360 行）。
 */
enum class OctopShadowSize { XS, SM, MD, LG, XL }

/** 阴影颜色：浅色 8% 黑，暗色 40% 黑 */
fun shadowColor(dark: Boolean): Color =
    if (dark) Color(0x66000000) else Color(0x14000000)

/** 阴影高度：暗色整体抬一档，补偿暗背景上阴影不明显 */
fun shadowElevation(size: OctopShadowSize, dark: Boolean): Dp {
    val base = when (size) {
        OctopShadowSize.XS -> 1
        OctopShadowSize.SM -> 2
        OctopShadowSize.MD -> 4
        OctopShadowSize.LG -> 8
        OctopShadowSize.XL -> 16
    }
    return (if (dark) base * 3 / 2 else base).dp
}

/** 品牌色发光阴影（--fn-shadow-brand），用于主按钮 */
fun shadowBrandElevation(dark: Boolean): Dp = if (dark) 6.dp else 4.dp

/**
 * 一处阴影的完整描述：高度 + 阴影色。
 * 用法：`modifier.shadow(OctopShadows.card(dark).elevation, shape, ambientColor = ..., spotColor = ...)`。
 */
@Immutable
data class OctopShadowSpec(val elevation: Dp, val color: Color)

/**
 * 阴影预设，对应 theme-vars.css 的 --fn-shadow-* 五档。
 * 导航壳（侧栏/底栏/浮层）直接用这几个，不要再自己写死 elevation。
 */
object OctopShadows {
    /** 按档位取阴影 */
    fun of(size: OctopShadowSize, dark: Boolean) =
        OctopShadowSpec(shadowElevation(size, dark), shadowColor(dark))

    /** 卡片：--fn-card-shadow-normal，浅色 3%、暗色 35% 黑 */
    fun card(dark: Boolean) =
        OctopShadowSpec(shadowElevation(OctopShadowSize.SM, dark), shadowColor(dark))

    /** 悬浮层/弹窗：--fn-shadow-lg */
    fun popover(dark: Boolean) =
        OctopShadowSpec(shadowElevation(OctopShadowSize.LG, dark), shadowColor(dark))

    /** 底部导航栏/侧栏：--fn-shadow-md，抬一档保证浮起感 */
    fun navigation(dark: Boolean) =
        OctopShadowSpec(shadowElevation(OctopShadowSize.MD, dark), shadowColor(dark))

    /** 品牌发光：--fn-shadow-brand */
    fun brand(dark: Boolean) = OctopShadowSpec(shadowBrandElevation(dark), shadowColor(dark))
}

/* ───────────────────────── 动效 ─────────────────────────
 * theme-vars.css：--fn-transition-fast .12s / -base .18s / -slow .28s，
 * --fn-transition-spring .32s cubic-bezier(0.34, 1.56, 0.64, 1)。
 */
const val DurationFast = 120
const val DurationBase = 180
const val DurationSlow = 280
const val DurationSpring = 320
val OctopSpringEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
