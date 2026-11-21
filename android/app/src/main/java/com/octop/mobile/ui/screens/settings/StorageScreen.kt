package com.octop.mobile.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.core.sync.SyncRepository
import com.octop.mobile.ui.components.HubGlyph
import com.octop.mobile.ui.components.HubIcon
import com.octop.mobile.ui.components.OctopCard
import com.octop.mobile.ui.components.SectionHeader
import com.octop.mobile.ui.components.Tag
import com.octop.mobile.ui.theme.Amber
import com.octop.mobile.ui.theme.BgPrimary
import com.octop.mobile.ui.theme.BgSecondary
import com.octop.mobile.ui.theme.BorderPrimary
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import org.json.JSONObject

@Composable
fun StorageScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf<LoadState<List<JSONObject>>>(LoadState.Loading) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick) {
        state = LoadState.Loading
        val res = runCatching { SyncRepository.hub.adminStorageBackends() }
            .getOrElse { return@LaunchedEffect }
        state = if (!res.ok) LoadState.Failed(reasonOf(res))
        else jsonList(res.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是列表：${res.body.take(80)}")
    }

    SubPage(
        title = "存储后端",
        subtitle = "查看已配置的存储服务与连接状态",
        onBack = onBack,
        onReload = { tick++ },
        note = "这里只展示已经启用的后端配置，修改存储请前往网页端管理后台。"
    ) {
        when (val s = state) {
            is LoadState.Loading -> item { LoadingBlock() }
            is LoadState.Failed -> item { ErrorBlock(s.reason) { tick++ } }
            is LoadState.Ready -> {
                item {
                    SectionHeader(
                        title = "存储后端",
                        trailing = "${s.value.size} 个配置"
                    )
                }
                if (s.value.isEmpty()) {
                    item {
                        OctopCard {
                            Text(
                                text = "当前没有配置任何存储后端。",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = "在网页端管理后台添加对象存储后，会在这里同步显示。",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = TextTertiary
                            )
                        }
                    }
                }
                s.value.forEach { b -> item { BackendCard(b) } }
            }
        }
    }
}

@Composable
private fun BackendCard(b: JSONObject) {
    val kind = b.str("kind", "backend").ifBlank { "backend" }
    val enabled = b.optBoolean("enabled", false)
    val name = b.str("name").ifBlank { kind }

    OctopCard {
        // Header：图标 + 名称/类型 + 启用状态，建立第一层信息重心
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(BgSecondary, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                HubIcon(HubGlyph.STORAGE, tint = TextSecondary, size = 19.dp)
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(text = kind, fontSize = 11.sp, color = TextTertiary)
            }
            Tag(
                text = if (enabled) "已启用" else "已停用",
                color = if (enabled) Green else TextTertiary
            )
        }

        // Body：连接信息按「服务地址 / 认证信息」分区，弱化 key 的展示权重
        Spacer(Modifier.height(13.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(BorderPrimary))
        Spacer(Modifier.height(11.dp))

        val endpoint = b.str("endpoint")
        val bucket = b.str("bucket")
        val region = b.str("region")
        if (endpoint.isNotBlank() || bucket.isNotBlank() || region.isNotBlank()) {
            Text(
                text = "服务地址",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(Modifier.height(7.dp))
            if (endpoint.isNotBlank()) MetaLine(label = "端点", value = endpoint)
            if (bucket.isNotBlank()) MetaLine(label = "Bucket", value = bucket)
            if (region.isNotBlank()) MetaLine(label = "区域", value = region)
        }

        val accessKey = b.str("access_key")
        val note = b.str("note")
        if (accessKey.isNotBlank() || note.isNotBlank()) {
            Spacer(Modifier.height(11.dp))
            Text(
                text = "其他信息",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(Modifier.height(7.dp))
            if (accessKey.isNotBlank()) MetaLine(label = "AccessKey", value = accessKey)
            if (note.isNotBlank()) MetaLine(label = "备注", value = note)
        }

        // Footer：时间戳与能力提示独立成行，不与主信息混排
        val updated = b.optLong("updated_at", 0L)
        val footer = buildString {
            if (updated > 0) append("更新于 ${fmtTime(updated)}")
            if (b.optBoolean("preview", false)) {
                if (isNotEmpty()) append(" · ")
                append("支持预览")
            }
        }
        if (footer.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(BorderPrimary))
            Spacer(Modifier.height(9.dp))
            Text(text = footer, fontSize = 10.sp, color = TextTertiary)
        }
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextTertiary,
            modifier = Modifier.width(58.dp)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
