package com.octop.mobile.ui.screens.settings

import androidx.compose.foundation.background
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
import com.octop.mobile.ui.components.Tag
import com.octop.mobile.ui.theme.Accent
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.Rose
import com.octop.mobile.ui.theme.SoftGreen
import com.octop.mobile.ui.theme.Surface1
import com.octop.mobile.ui.theme.Surface2
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import org.json.JSONObject

/** 连接器（网页端「连接器」）。手机端只读，授权/填密钥在网页端完成。
 *  GET /api/connectors/instances 已连接实例
 *  GET /api/connectors/catalog   可连接目录
 *  页面状态只依赖这两个 GET，不做 POST/PATCH。
 */
@Composable
fun ConnectorsScreen(onBack: () -> Unit) {
    var instances by remember { mutableStateOf<LoadState<List<JSONObject>>>(LoadState.Loading) }
    var catalog by remember { mutableStateOf<LoadState<List<JSONObject>>>(LoadState.Loading) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick) {
        instances = LoadState.Loading
        catalog = LoadState.Loading
        val i = SyncRepository.api.listInstances()
        instances = if (!i.ok) LoadState.Failed(reasonOf(i))
        else jsonList(i.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是列表：${i.body.take(80)}")
        val c = SyncRepository.api.getCatalog()
        catalog = if (!c.ok) LoadState.Failed(reasonOf(c))
        else jsonList(c.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是列表：${c.body.take(80)}")
    }

    SubPage(
        title = "连接器",
        subtitle = "它伸手能碰到哪些外部服务",
        onBack = onBack,
        onReload = { tick++ },
        note = "手机端只读。连接 / 授权需要跳转登录或填密钥，请用网页端完成。"
    ) {
        item {
            ConnectorOverviewCard(
                connected = (instances as? LoadState.Ready)?.value?.size,
                available = (catalog as? LoadState.Ready)?.value?.let { list ->
                    catalogKindCount(list, (instances as? LoadState.Ready)?.value.orEmpty())
                }
            )
            Spacer(Modifier.height(10.dp))
        }

        item {
            ConnectorSectionCard(
                glyph = HubGlyph.CONNECTOR,
                title = "已连接服务",
                description = "这些外部服务已经授权给本体；手机端只展示状态，不做断开。",
                trailing = (instances as? LoadState.Ready)?.value?.size?.let { "$it 个" }
            ) {
                when (val s = instances) {
                    is LoadState.Loading -> ConnectorLoadingRow("正在读取已连接服务…")
                    is LoadState.Failed -> ConnectorErrorRow(s.reason) { tick++ }
                    is LoadState.Ready -> {
                        if (s.value.isEmpty()) ConnectorEmptyRow("还没有连接任何外部服务")
                        else s.value.forEachIndexed { index, inst ->
                            if (index > 0) ConnectorDivider()
                            InstanceCard(inst)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }

        item {
            ConnectorSectionCard(
                glyph = HubGlyph.CLOUD,
                title = "可连接目录",
                description = "目录列出可授权的外部服务；连接与授权请到网页端完成。",
                trailing = (instances as? LoadState.Ready)?.value?.let { connected ->
                    (catalog as? LoadState.Ready)?.value?.let { "${catalogKindCount(it, connected)} 个未连" }
                }
            ) {
                when (val s = catalog) {
                    is LoadState.Loading -> ConnectorLoadingRow("正在读取服务目录…")
                    is LoadState.Failed -> ConnectorErrorRow(s.reason) { tick++ }
                    is LoadState.Ready -> {
                        if (s.value.isEmpty()) ConnectorEmptyRow("目录为空或暂时读取不到")
                        else s.value.forEachIndexed { index, cat ->
                            if (index > 0) ConnectorDivider()
                            CatalogCard(cat)
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(10.dp))
            ConnectorReadonlyHint("连接、授权与密钥填写需要网页端跳转；这里不会保存任何凭据。")
        }
    }
}

@Composable
private fun ConnectorOverviewCard(connected: Int?, available: Int?) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Surface1, RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Surface2, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) { HubIcon(HubGlyph.CONNECTOR, tint = Accent, size = 22.dp) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "连接概览", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = "本页只读 · 连接与授权请用网页端", fontSize = 10.sp, color = TextTertiary)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag("已连接 ${connected ?: "…"}", color = if (connected == null || connected == 0) TextTertiary else Green)
            Tag("可连接 ${available ?: "…"}", color = Accent)
        }
    }
}

@Composable
private fun ConnectorSectionCard(
    glyph: HubGlyph,
    title: String,
    description: String,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Surface1, RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).background(Surface2, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) { HubIcon(glyph, tint = Accent, size = 18.dp) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Accent)
                Spacer(Modifier.height(3.dp))
                Text(text = description, fontSize = 11.sp, lineHeight = 16.sp, color = TextSecondary)
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
private fun ConnectorLoadingRow(text: String) {
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
private fun ConnectorErrorRow(reason: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Rose.copy(alpha = 0.08f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(text = "读取失败：$reason", fontSize = 12.sp, lineHeight = 17.sp, color = Rose)
        Spacer(Modifier.height(8.dp))
        com.octop.mobile.ui.components.PillButton("重试", filled = false) { onRetry() }
    }
}

@Composable
private fun ConnectorEmptyRow(text: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(36.dp).background(Surface2, CircleShape), contentAlignment = Alignment.Center) {
            HubIcon(HubGlyph.CLOUD, tint = TextTertiary, size = 18.dp)
        }
        Spacer(Modifier.height(8.dp))
        Text(text = text, fontSize = 12.sp, color = TextTertiary)
    }
}

@Composable
private fun ConnectorDivider() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(Surface2))
}

@Composable
private fun ConnectorReadonlyHint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Surface2, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HubIcon(HubGlyph.STORAGE, tint = TextTertiary, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        Text(text = text, fontSize = 10.sp, lineHeight = 16.sp, color = TextSecondary)
    }
}

@Composable
private fun InstanceCard(inst: JSONObject) {
    val kind = inst.str("kind", "type", "provider")
    val status = inst.str("status", "state")
    val okStatus = status.equals("active", true) || status.equals("connected", true) || status.equals("ok", true)
    val emoji = inst.str("emoji", "icon").ifBlank { "🔗" }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(38.dp).background(SoftGreen, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text(text = emoji, fontSize = 17.sp, color = TextPrimary)
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = inst.str("name", "title").ifBlank { kind.ifBlank { "未命名连接" } },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Tag(
                    if (status.isNotBlank()) status else if (okStatus) "已连接" else "未知",
                    color = if (okStatus) Green else TextTertiary
                )
            }
            Spacer(Modifier.height(4.dp))
            val desc = inst.str("description", "desc")
            if (desc.isNotBlank()) {
                Text(text = desc, fontSize = 11.sp, lineHeight = 15.sp, color = TextSecondary)
                Spacer(Modifier.height(3.dp))
            }
            Text(
                text = listOfNotNull(
                    kind.takeIf { it.isNotBlank() },
                    inst.optBoolean("is_default", false).takeIf { it }?.let { "默认打开" },
                    inst.optBoolean("is_shared", false).takeIf { it }?.let { "共享" },
                    if (inst.optBoolean("has_credentials", false)) "已存凭据" else "无凭据"
                ).joinToString(" · "),
                fontSize = 10.sp,
                color = TextTertiary
            )
            val updated = fmtTime(inst.optLong("updated_at", 0L))
            if (updated.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(text = "更新于 $updated", fontSize = 10.sp, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun CatalogCard(cat: JSONObject) {
    val kind = cat.str("kind", "id")
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).background(Surface2, CircleShape),
            contentAlignment = Alignment.Center
        ) { HubIcon(HubGlyph.CLOUD, tint = Accent, size = 18.dp) }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cat.str("name", "title").ifBlank { kind },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Tag("未连接", color = TextTertiary)
            }
            Spacer(Modifier.height(4.dp))
            val desc = cat.str("description", "desc")
            if (desc.isNotBlank()) {
                Text(text = desc, fontSize = 11.sp, lineHeight = 15.sp, color = TextSecondary)
                Spacer(Modifier.height(3.dp))
            }
            Text(
                text = listOfNotNull(
                    categoryLabel(cat.str("category")).takeIf { it.isNotBlank() },
                    kind.takeIf { it.isNotBlank() }?.let { "$it · ${authLabel(cat.str("auth_kind"))}" }
                ).joinToString(" · "),
                fontSize = 10.sp,
                color = TextTertiary
            )
        }
    }
}

private fun catalogKindCount(catalog: List<JSONObject>, connected: List<JSONObject>): Int {
    val kinds = connected.mapNotNull { it.optString("kind").takeIf { k -> k.isNotBlank() } }.toSet()
    return catalog.count { it.optString("kind") !in kinds }
}

private fun categoryLabel(c: String): String = when (c.lowercase()) {
    "office" -> "办公协作"
    "knowledge" -> "知识库"
    "travel" -> "出行"
    "media" -> "影音媒体"
    "productivity" -> "效率工具"
    "professional" -> "专业服务"
    "self_hosted" -> "自托管"
    else -> c
}

private fun authLabel(a: String): String = when (a.lowercase()) {
    "personal_token" -> "个人令牌"
    "api_key" -> "API Key"
    "oauth" -> "OAuth"
    "" -> "无需授权"
    else -> a
}
