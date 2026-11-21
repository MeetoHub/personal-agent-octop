package com.octop.mobile.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.core.sync.SyncRepository
import com.octop.mobile.ui.components.Tag
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.OctopPalette
import com.octop.mobile.ui.theme.Rose
import com.octop.mobile.ui.theme.Surface1
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import com.octop.mobile.ui.theme.ThemeMode
import com.octop.mobile.ui.theme.ThemeState
import org.json.JSONObject

/**
 * 应用设置（网页端「应用设置」，对应 GET /api/admin/app-config）。
 *
 * 返回的是 config.json 的完整视图：path / valid / config{...} / overrides / restart_available。
 * 这里只读展示，不提供保存：改的是进程启动配置，改完必须重启本体才生效，
 * 手机上点了「保存」但没重启，用户会以为改了没用。
 * 数据库密码服务端本来就不下发（has_database_password 只给布尔）。
 *
 * 顶部「外观」区块是可交互的本机偏好（主题模式 / 品牌配色），落在 SharedPreferences，
 * 与下面的只读本体配置分开。
 */
@Composable
fun AppSettingsScreen(onBack: () -> Unit, themeState: ThemeState) {
    var state by remember { mutableStateOf<LoadState<JSONObject>>(LoadState.Loading) }
    var tick by remember { mutableIntStateOf(0) }

    // 两个选择器的弹窗开关（null 表示不弹）
    var modePicker by remember { mutableStateOf(false) }
    var palettePicker by remember { mutableStateOf(false) }

    LaunchedEffect(tick) {
        state = LoadState.Loading
        val res = SyncRepository.hub.appConfig()
        state = if (!res.ok) LoadState.Failed(reasonOf(res))
        else jsonObj(res.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是对象：${res.body.take(80)}")
    }

    SubPage(
        title = "应用设置",
        subtitle = "外观偏好与本体启动配置",
        onBack = onBack,
        onReload = { tick++ },
        note = "下方本体配置为只读展示。这些是进程启动参数，改完要重启本体才生效，所以不在手机端提供编辑。"
    ) {
        // ── 外观：本机主题偏好，可交互 ──────────────────────
        item { SectionTitle("外观") }
        item {
            KvCard {
                // 主题模式：点击弹单选列表
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { modePicker = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "主题模式", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Spacer(Modifier.height(2.dp))
                        Text(text = "跟随系统 / 浅色 / 深色", fontSize = 11.sp, color = TextTertiary)
                    }
                    Text(text = themeState.mode.labelZh, fontSize = 12.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(10.dp))
                // 品牌配色：点击弹 8 色选择
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { palettePicker = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "品牌配色", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Spacer(Modifier.height(2.dp))
                        Text(text = "选一个你喜欢的颜色", fontSize = 11.sp, color = TextTertiary)
                    }
                    Spacer(Modifier.width(8.dp))
                    SwatchDot(paletteSwatch(themeState.palette))
                    Spacer(Modifier.width(6.dp))
                    Text(text = themeState.palette.labelZh, fontSize = 12.sp, color = TextSecondary)
                }
            }
        }

        when (val s = state) {
            is LoadState.Loading -> item { LoadingBlock() }
            is LoadState.Failed -> item { ErrorBlock(s.reason) { tick++ } }
            is LoadState.Ready -> {
                val o = s.value
                val cfg = o.optJSONObject("config")
                item {
                    KvCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "配置文件",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Spacer(Modifier.weight(1f))
                            Tag(
                                if (o.optBoolean("valid", false)) "格式正确" else "格式错误",
                                color = if (o.optBoolean("valid", false)) Green else Rose
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = o.str("path"),
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            color = TextTertiary
                        )
                        val errText = o.str("error")
                        if (errText.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(text = errText, fontSize = 11.sp, color = Rose)
                        }
                    }
                }

                if (cfg == null) {
                    item {
                        com.octop.mobile.ui.components.EmptyHint(
                            "服务端没解析出配置结构，可能是文件被手工改坏了。原始内容在网页端可见。"
                        )
                    }
                } else {
                    item { SectionTitle("网络") }
                    item {
                        KvCard {
                            KvRow("监听地址", cfg.str("bind_host"))
                            KvRow("端口", cfg.optInt("port", 0).toString())
                            KvRow("仪表盘", if (cfg.optBoolean("enable_dashboard", false)) "开启" else "关闭")
                            KvRow("API 文档", if (cfg.optBoolean("enable_api_docs", false)) "开启" else "关闭")
                        }
                    }

                    item { SectionTitle("登录与令牌") }
                    item {
                        KvCard {
                            val ttl = cfg.optInt("access_token_ttl_seconds", 0)
                            KvRow("令牌有效期", if (ttl > 0) "${ttl / 3600} 小时" else "—")
                            KvRow("登录失败上限", cfg.optInt("login_max_attempts", 0).toString() + " 次")
                            KvRow("锁定时长", "${cfg.optInt("login_lockout_seconds", 0) / 60} 分钟")
                            KvRow("需安装口令", if (cfg.optBoolean("require_setup_password", false)) "是" else "否")
                            KvRow("时区", cfg.str("default_timezone"))
                        }
                    }

                    item { SectionTitle("存储与日志") }
                    item {
                        KvCard {
                            val db = cfg.optJSONObject("database")
                            KvRow("数据库", db?.str("driver") ?: "—")
                            KvRow("数据文件", db?.str("sqlite_path") ?: "—")
                            KvRow("日志级别", cfg.str("log_level"))
                            KvRow("上传上限", "${cfg.optInt("max_upload_mb", 0)} MB")
                        }
                    }

                    item { SectionTitle("证书") }
                    item {
                        KvCard {
                            val tls = cfg.optJSONObject("tls")
                            KvRow("TLS", if (tls?.optBoolean("enabled", false) == true) "开启" else "关闭")
                            KvRow("模式", tls?.str("mode") ?: "—")
                            KvRow("域名", tls?.optJSONArray("domains")?.let { a ->
                                (0 until a.length()).joinToString(", ") { a.optString(it) }
                            }?.ifBlank { "—" } ?: "—")
                        }
                    }

                    val overrides = o.optJSONArray("overrides")
                    if (overrides != null && overrides.length() > 0) {
                        item { SectionTitle("环境变量覆盖") }
                        item {
                            KvCard {
                                Text(
                                    text = "以下环境变量会覆盖配置文件里的同名项：",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(6.dp))
                                (0 until overrides.length()).forEach {
                                    Text(
                                        text = overrides.optString(it),
                                        fontSize = 11.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (o.optBoolean("restart_available", false)) {
                        item {
                            HintCard("这个部署支持重启本体。改完配置后请在网页端重启，手机端不发重启请求。")
                        }
                    }
                }
            }
        }
    }

    // ── 主题模式：单选弹窗 ────────────────────────────────
    if (modePicker) {
        AlertDialog(
            onDismissRequest = { modePicker = false },
            title = { Text(text = "主题模式", color = TextPrimary) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    themeState.updateMode(mode) // 切换即时生效
                                    modePicker = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeState.mode == mode,
                                onClick = {
                                    themeState.updateMode(mode)
                                    modePicker = false
                                }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(text = mode.labelZh, fontSize = 14.sp, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { modePicker = false }) { Text(text = "关闭", color = TextSecondary) }
            }
        )
    }

    // ── 品牌配色：8 色单选弹窗 ────────────────────────────
    if (palettePicker) {
        AlertDialog(
            onDismissRequest = { palettePicker = false },
            title = { Text(text = "品牌配色", color = TextPrimary) },
            text = {
                Column {
                    OctopPalette.entries.forEach { palette ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    themeState.updatePalette(palette) // 切换即时生效
                                    palettePicker = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeState.palette == palette,
                                onClick = {
                                    themeState.updatePalette(palette)
                                    palettePicker = false
                                }
                            )
                            Spacer(Modifier.width(6.dp))
                            SwatchDot(paletteSwatch(palette))
                            Spacer(Modifier.width(10.dp))
                            Text(text = palette.labelZh, fontSize = 14.sp, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { palettePicker = false }) { Text(text = "关闭", color = TextSecondary) }
            }
        )
    }
}

/** 色块圆点，颜色由调用方传入候选色。 */
@Composable
private fun SwatchDot(color: Color) {
    Spacer(
        Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * 8 套配色的候选色块。取各 palette 在 OctopTokens 中明色态的 brand 值
 * （即 CSS theme-vars.css 的 --fn-color-brand 明色），与桌面端选色一致。
 * 只在设置页本地持有，避免依赖主题内核再暴露 swatch 常量。
 */
private fun paletteSwatch(palette: OctopPalette): Color = when (palette) {
    OctopPalette.ROSE -> Color(0xFFE85D75)
    OctopPalette.TECH -> Color(0xFF4B74FA)
    OctopPalette.INDIGO -> Color(0xFF6366F1)
    OctopPalette.TEAL -> Color(0xFF0D9488)
    OctopPalette.VIOLET -> Color(0xFF7C3AED)
    OctopPalette.EMERALD -> Color(0xFF10B981)
    OctopPalette.AMBER -> Color(0xFFB45309)
    OctopPalette.SLATE -> Color(0xFF64748B)
}
