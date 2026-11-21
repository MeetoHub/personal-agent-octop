package com.octop.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/* ───────────────────────── 主题装配 ─────────────────────────
 * 色值全部来自 Octop 本体 theme-vars.css，这里只做两件事：
 *   1) 把语义色板塞进 CompositionLocal，供 Color.kt 的 @Composable getter 读；
 *   2) 派生一份 Material3 ColorScheme，让 Material 组件（TextField/Switch/Card…）也跟着变色。
 * 明暗由参数 darkTheme 决定，配色由参数 palette 决定，两者互相独立。
 */

/** 由语义色板派生 Material3 浅色 scheme（方案 §4.3 映射表） */
private fun lightSchemeOf(c: OctopColors) = lightColorScheme(
    primary = c.brand,
    onPrimary = c.onBrand,
    primaryContainer = c.brandLight,
    onPrimaryContainer = c.brand,
    secondary = c.info,
    onSecondary = c.onBrand,
    secondaryContainer = c.bgSecondary,
    onSecondaryContainer = c.textPrimary,
    tertiary = c.warningText,
    onTertiary = c.onBrand,
    background = c.bgPage,
    onBackground = c.textPrimary,
    surface = c.surface,
    onSurface = c.textPrimary,
    surfaceVariant = c.bgSecondary,
    onSurfaceVariant = c.textSecondary,
    surfaceContainer = c.bgPrimary,
    surfaceContainerHigh = c.bgSecondary,
    surfaceContainerHighest = c.bgTertiary,
    outline = c.borderPrimary,
    outlineVariant = c.borderSecondary,
    error = c.danger,
    onError = c.onBrand,
    errorContainer = c.danger.copy(alpha = 0.08f),
    onErrorContainer = c.danger,
    scrim = Color.Black,
)

/** 由语义色板派生 Material3 暗色 scheme，字段与浅色一一对应 */
private fun darkSchemeOf(c: OctopColors) = darkColorScheme(
    primary = c.brand,
    onPrimary = c.onBrand,
    primaryContainer = c.brandSoft,
    onPrimaryContainer = c.textBrand,
    secondary = c.info,
    onSecondary = c.onBrand,
    secondaryContainer = c.bgTertiary,
    onSecondaryContainer = c.textPrimary,
    tertiary = c.warning,
    onTertiary = c.onBrand,
    background = c.bgPage,
    onBackground = c.textPrimary,
    surface = c.surface,
    onSurface = c.textPrimary,
    surfaceVariant = c.bgSecondary,
    onSurfaceVariant = c.textSecondary,
    surfaceContainer = c.bgPrimary,
    surfaceContainerHigh = c.bgSecondary,
    surfaceContainerHighest = c.bgTertiary,
    outline = c.borderPrimary,
    outlineVariant = c.borderSecondary,
    error = c.danger,
    onError = c.onBrand,
    errorContainer = c.danger.copy(alpha = 0.08f),
    onErrorContainer = c.danger,
    scrim = Color.Black,
)

/**
 * 应用主题。
 * darkTheme 默认跟随系统、palette 默认玫瑰，保证老的 `OctopTheme { }` 写法照常编译。
 */
@Composable
fun OctopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: OctopPalette = OctopPalette.DEFAULT,
    content: @Composable () -> Unit,
) {
    val colors = octopColors(palette, darkTheme)
    // 缓存 scheme：只有配色或明暗变了才重建 ColorScheme 对象
    val scheme = remember(colors) {
        if (darkTheme) darkSchemeOf(colors) else lightSchemeOf(colors)
    }

    CompositionLocalProvider(LocalOctopColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = OctopTypography,
            content = content,
        )
    }
}
