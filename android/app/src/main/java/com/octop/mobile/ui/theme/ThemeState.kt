package com.octop.mobile.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/* 主题偏好的三种取值，对应桌面端 appearanceStorage 的 preference: "system" | "light" | "dark" */
enum class ThemeMode(val key: String, val labelZh: String) {
    SYSTEM("system", "跟随系统"),
    LIGHT("light", "浅色"),
    DARK("dark", "深色");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

private const val PREF_NAME = "octop_ui"
/* 键名与桌面端 localStorage 对齐（widgets/appearance 用的就是这两个名字） */
private const val KEY_THEME = "theme"
private const val KEY_PALETTE = "octop:ui-palette"

/**
 * 主题状态：明暗偏好 + 品牌配色，落 SharedPreferences。
 * MainActivity 读 [isDark] 给系统状态栏用，读 [palette] 传给 OctopTheme。
 */
@Stable
class ThemeState(
    initialMode: ThemeMode,
    initialPalette: OctopPalette,
    initialSystemDark: Boolean,
) {
    var mode: ThemeMode by mutableStateOf(initialMode)
        private set

    var palette: OctopPalette by mutableStateOf(initialPalette)
        private set

    /** 系统当前是否暗色；由 [rememberThemeState] 每帧同步，系统切换能立刻生效 */
    var systemDark: Boolean by mutableStateOf(initialSystemDark)
        internal set

    /** 实际是否走暗色：SYSTEM 时看系统 */
    val isDark: Boolean
        get() = when (mode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    /** 当前配色值，UI 可直接取 */
    val colors: OctopColors
        get() = octopColors(palette, isDark)

    /** 切换明暗偏好（不叫 setMode：会和 mode 的 setter 撞 JVM 签名） */
    fun updateMode(next: ThemeMode) {
        mode = next
    }

    /** 切换品牌配色（不叫 setPalette：会和 palette 的 setter 撞 JVM 签名） */
    fun updatePalette(next: OctopPalette) {
        palette = next
    }
}

/**
 * 读 SharedPreferences 建一个 ThemeState，偏好变化时写回。
 * 必须在 setContent 内部调用（要读系统暗色）。
 */
@Composable
fun rememberThemeState(): ThemeState {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val prefs = remember(context) {
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    val state = remember(prefs) {
        ThemeState(
            initialMode = ThemeMode.fromKey(prefs.getString(KEY_THEME, null)),
            initialPalette = OctopPalette.fromKey(prefs.getString(KEY_PALETTE, null)),
            initialSystemDark = systemDark,
        )
    }

    // 系统暗色可能在运行中变化，同步给 state（SYSTEM 模式下立刻跟着变）
    LaunchedEffect(systemDark) { state.systemDark = systemDark }

    // 偏好变化后落盘；LaunchedEffect 只在键变化时跑，不会每帧写 IO
    LaunchedEffect(state.mode, state.palette) {
        prefs.edit()
            .putString(KEY_THEME, state.mode.key)
            .putString(KEY_PALETTE, state.palette.key)
            .apply()
    }

    return state
}
