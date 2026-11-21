package com.octop.mobile.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.core.sync.SyncRepository
import com.octop.mobile.ui.components.HubGlyph
import com.octop.mobile.ui.components.HubIcon
import com.octop.mobile.ui.components.PillButton
import com.octop.mobile.ui.components.Tag
import com.octop.mobile.ui.theme.Accent
import com.octop.mobile.ui.theme.Amber
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.Rose
import com.octop.mobile.ui.theme.Surface1
import com.octop.mobile.ui.theme.Surface2
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 插件（网页端「插件管理」）。
 *
 * 接口：
 *   GET   /api/plugins        已安装插件及其 tools / enabled / registration_error
 *   GET   /api/plugins/market 市场插件（注册表同步后的列表）
 *   PATCH /api/plugins/{id}   {"enabled": bool}  启停已安装插件
 *
 * 安装/卸载涉及依赖与扫描目录，留给网页端；这里只做「启停」这种可逆操作。
 */
@Composable
fun PluginsScreen(onBack: () -> Unit) {
    var segment by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    var installed by remember { mutableStateOf<LoadState<List<JSONObject>>>(LoadState.Loading) }
    var market by remember { mutableStateOf<LoadState<List<JSONObject>>>(LoadState.Loading) }
    var busy by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tick) {
        installed = LoadState.Loading
        market = LoadState.Loading

        val i = SyncRepository.hub.plugins()
        installed = if (!i.ok) LoadState.Failed(reasonOf(i))
        else jsonList(i.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是列表：${i.body.take(80)}")

        val m = SyncRepository.hub.pluginMarket()
        market = if (!m.ok) LoadState.Failed(reasonOf(m))
        else jsonList(m.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是列表：${m.body.take(80)}")
    }

    SubPage(
        title = "插件",
        subtitle = "给本体装的新本事",
        onBack = onBack,
        onReload = { tick++ },
        note = "停用会让所有专家立刻用不了这个插件的工具，但随时能再打开。安装和卸载请用网页端。"
    ) {
        if (err != null) item { SettingsNoticeCard(err!!, onDismiss = { err = null }) }

        item {
            SettingsOverviewCard(
                glyph = HubGlyph.PLUGIN,
                title = "插件能力一览",
                description = "已安装列表显示当前能被调用的工具；市场列表只用于发现，不在这里直接安装。",
                installedCount = (installed as? LoadState.Ready)?.value?.size,
                marketCount = (market as? LoadState.Ready)?.value?.size
            )
        }

        item {
            SettingsSegment(
                labels = listOf(
                    "已安装 ${installedCount(installed)}",
                    "市场 ${marketInstalledCount(market)}"
                ),
                selected = segment,
                onSelect = { segment = it }
            )
        }

        item {
            SettingsSectionCard(
                icon = HubGlyph.PLUGIN,
                title = if (segment == 0) "当前可用插件" else "市场候选",
                description = if (segment == 0) {
                    "启停立即影响所有专家；带「未加载」标记的插件可能有依赖或注册错误。"
                } else {
                    "这里展示注册表同步来的插件；标记为已安装的才出现在上方列表。"
                },
                trailing = if (segment == 0) {
                    (installed as? LoadState.Ready)?.value?.size?.let { "${it} 个" }
                } else {
                    (market as? LoadState.Ready)?.value?.size?.let { "${it} 个" }
                }
            ) {
                when {
                    segment == 0 && installed is LoadState.Loading -> SettingsLoadingRow("正在读取已安装插件…")
                    segment == 1 && market is LoadState.Loading -> SettingsLoadingRow("正在读取市场列表…")
                    segment == 0 && installed is LoadState.Failed -> SettingsInlineError((installed as LoadState.Failed).reason) { tick++ }
                    segment == 1 && market is LoadState.Failed -> SettingsInlineError((market as LoadState.Failed).reason) { tick++ }
                    segment == 0 && (installed as LoadState.Ready).value.isEmpty() -> SettingsEmptyRow("还没有安装插件")
                    segment == 1 && (market as LoadState.Ready).value.isEmpty() -> SettingsEmptyRow("市场列表为空")
                    segment == 0 -> (installed as LoadState.Ready).value.forEachIndexed { index, p ->
                        if (index > 0) SettingsListDivider()
                        InstalledCard(p, busy == p.str("id")) { id ->
                            val enabled = p.optBoolean("enabled", false)
                            busy = id
                            err = null
                            scope.launch {
                                val res = SyncRepository.hub.setPluginEnabled(id, !enabled)
                                busy = null
                                if (res.ok) tick++ else err = reasonOf(res)
                            }
                        }
                    }
                    else -> (market as LoadState.Ready).value.forEachIndexed { index, p ->
                        if (index > 0) SettingsListDivider()
                        MarketCard(p)
                    }
                }
            }
        }

        item {
            SettingsReadonlyHint("安装、卸载与依赖处理请用网页端；手机端不会改动插件目录。")
        }
    }
}

private fun installedCount(state: LoadState<List<JSONObject>>): String =
    (state as? LoadState.Ready)?.value?.size?.toString() ?: "-"

private fun marketInstalledCount(state: LoadState<List<JSONObject>>): String =
    (state as? LoadState.Ready)?.value?.count { it.optBoolean("installed", false) }?.toString() ?: "-"

@Composable
private fun InstalledCard(p: JSONObject, busy: Boolean, onToggle: (String) -> Unit) {
    val id = p.str("id")
    val name = p.str("name").ifBlank { id }
    val enabled = p.optBoolean("enabled", false)
    val loaded = p.optBoolean("loaded", true)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).background(Surface2, CircleShape),
            contentAlignment = Alignment.Center
        ) { HubIcon(HubGlyph.PLUGIN, tint = if (enabled && loaded) Accent else TextTertiary, size = 18.dp) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Tag(
                    when {
                        !enabled -> "已停用"
                        !loaded -> "未加载"
                        else -> "运行中"
                    },
                    color = when {
                        !enabled -> TextTertiary
                        !loaded -> Amber
                        else -> Green
                    }
                )
            }
            Spacer(Modifier.height(4.dp))
            val desc = p.str("description", "desc").ifBlank { "（无描述）" }
            Text(text = desc, fontSize = 11.sp, lineHeight = 15.sp, color = if (desc == "（无描述）") TextTertiary else TextSecondary)
            Spacer(Modifier.height(3.dp))
            Text(
                text = listOfNotNull(
                    id.takeIf { it.isNotBlank() },
                    p.str("version").takeIf { it.isNotBlank() }?.let { "v$it" },
                    p.str("kind").takeIf { it.isNotBlank() },
                    "${p.optJSONArray("tools")?.length() ?: 0} 个工具"
                ).joinToString(" · "),
                fontSize = 10.sp,
                color = TextTertiary
            )
            val regError = p.str("registration_error", "error")
            if (regError.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(text = "注册失败：$regError", fontSize = 10.sp, lineHeight = 14.sp, color = Rose)
            }
        }
        Spacer(Modifier.width(10.dp))
        if (busy) {
            Text("处理中…", fontSize = 10.sp, color = TextTertiary)
        } else {
            PillButton(if (enabled) "停用" else "启用", filled = !enabled) { onToggle(id) }
        }
    }
}

@Composable
private fun MarketCard(p: JSONObject) {
    val id = p.str("id")
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).background(Surface2, CircleShape),
            contentAlignment = Alignment.Center
        ) { HubIcon(HubGlyph.APP, tint = Accent, size = 18.dp) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = p.str("name").ifBlank { id }, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                when {
                    p.optBoolean("has_update", false) -> Tag("可更新", color = Accent)
                    p.optBoolean("installed", false) -> Tag("已安装", color = Green)
                    else -> Tag("未安装", color = TextTertiary)
                }
            }
            Spacer(Modifier.height(4.dp))
            val desc = p.str("description", "desc")
            Text(
                text = desc.ifBlank { "（无描述）" },
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = if (desc.isBlank()) TextTertiary else TextSecondary
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = listOfNotNull(
                    p.str("version").takeIf { it.isNotBlank() }?.let { "v$it" },
                    p.str("group").takeIf { it.isNotBlank() },
                    p.str("kind").takeIf { it.isNotBlank() }
                ).joinToString(" · "),
                fontSize = 10.sp,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun SettingsOverviewCard(
    glyph: HubGlyph,
    title: String,
    description: String,
    installedCount: Int?,
    marketCount: Int?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Surface2, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) { HubIcon(glyph, tint = Accent, size = 22.dp) }
            Spacer(Modifier.width(12.dp))
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        Spacer(Modifier.height(10.dp))
        Text(text = description, fontSize = 12.sp, lineHeight = 18.sp, color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag("已安装 ${installedCount ?: "…"}", color = if (installedCount == 0) TextTertiary else Accent)
            Tag("市场 ${marketCount ?: "…"}", color = if (marketCount == 0) TextTertiary else Accent)
        }
    }
}

@Composable
private fun SettingsSegment(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Surface2, RoundedCornerShape(14.dp)).padding(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val active = selected == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (active) Surface1 else Color.Transparent, RoundedCornerShape(10.dp))
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) TextPrimary else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon: HubGlyph? = null,
    title: String,
    description: String? = null,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Surface1, RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(
                    modifier = Modifier.size(34.dp).background(Surface2, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) { HubIcon(icon, tint = Accent, size = 18.dp) }
                Spacer(Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Accent)
                if (!description.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(text = description, fontSize = 11.sp, lineHeight = 16.sp, color = TextSecondary)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                Text(text = trailing, fontSize = 10.sp, color = TextTertiary)
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(content = content)
    }
}

@Composable
private fun SettingsListDivider() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).padding(vertical = 1.dp).background(Surface2))
}

@Composable
private fun SettingsLoadingRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Accent)
        Spacer(Modifier.width(10.dp))
        Text(text = text, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun SettingsEmptyRow(text: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(36.dp).background(Surface2, CircleShape), contentAlignment = Alignment.Center) {
            HubIcon(HubGlyph.STORAGE, tint = TextTertiary, size = 18.dp)
        }
        Spacer(Modifier.height(8.dp))
        Text(text = text, fontSize = 12.sp, color = TextTertiary)
    }
}

@Composable
private fun SettingsInlineError(reason: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Rose.copy(alpha = 0.08f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(text = "读取失败：$reason", fontSize = 12.sp, lineHeight = 17.sp, color = Rose)
        Spacer(Modifier.height(8.dp))
        PillButton("重试", filled = false) { onRetry() }
    }
}

@Composable
private fun SettingsNoticeCard(text: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Rose.copy(alpha = 0.08f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(text = "操作失败：$text", fontSize = 12.sp, lineHeight = 17.sp, color = Rose)
        Spacer(Modifier.height(6.dp))
        PillButton("关闭", filled = false) { onDismiss() }
    }
}

@Composable
private fun SettingsReadonlyHint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Surface2, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HubIcon(HubGlyph.STORAGE, tint = TextTertiary, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        Text(text = text, fontSize = 10.sp, lineHeight = 16.sp, color = TextSecondary)
    }
}
