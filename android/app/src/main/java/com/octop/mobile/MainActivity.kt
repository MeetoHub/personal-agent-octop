package com.octop.mobile

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.octop.mobile.ui.OctopRoot
import com.octop.mobile.ui.theme.Bg
import com.octop.mobile.ui.theme.OctopTheme
import com.octop.mobile.ui.theme.ThemeState
import com.octop.mobile.ui.theme.rememberThemeState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 边到边：内容铺到系统栏下面，安全区由 Compose 的 WindowInsets 消费（方案 §4.8）
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            // rememberThemeState() 必须在 setContent 内部（它读系统暗色 / CompositionLocal）
            val themeState = rememberThemeState()
            OctopTheme(darkTheme = themeState.isDark, palette = themeState.palette) {
                // 系统栏图标亮暗跟随主题（原本写死浅色，切暗色后白图标压深底会看不清）
                SideEffect { applySystemBarAppearance(this@MainActivity, lightIcons = !themeState.isDark) }
                // 只铺底色；安全区 padding 留给 OctopRoot / Scaffold，避免双倍留白
                Box(modifier = Modifier.fillMaxSize().background(Bg)) {
                    OctopRoot(themeState = themeState)
                }
            }
        }
    }
}

/**
 * 系统栏外观：背景透明 + 图标亮暗跟随主题。
 * lightIcons=true 表示深色图标（浅色主题），false 表示浅色图标（暗色主题）。
 * statusBarColor / navigationBarColor 在 API 35 已废弃（一般由边到边接管），
 * 但为了低版本一致仍显式置透明，因此这里集中抑制废弃告警。
 */
@Suppress("DEPRECATION")
private fun applySystemBarAppearance(activity: Activity, lightIcons: Boolean) {
    val window = activity.window
    WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = lightIcons
        isAppearanceLightNavigationBars = lightIcons
    }
    window.statusBarColor = android.graphics.Color.TRANSPARENT
    window.navigationBarColor = android.graphics.Color.TRANSPARENT
}
