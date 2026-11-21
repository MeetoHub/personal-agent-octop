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
import com.octop.mobile.ui.components.StatusDot
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

/** 通道（网页端「通道管理」）。
 *  GET    /api/agents/{agent_id}/channels                  这个 agent 的通道
 *  PATCH  /api/agents/{agent_id}/channels/{channel_id}     {"enabled": bool}  启停（可逆）
 *  POST   /api/agents/{agent_id}/channels/{channel_id}/test 主动探测一次，看 runtime 反馈
 *
 * 扫码授权、建新通道涉及外部账号绑定，这里不做；启停和探测是安全动作。
 */
@Composable
fun ChannelsScreen(agentId: String, onBack: () -> Unit) {
    var state by remember { mutableStateOf<LoadState<List<JSONObject>>>(LoadState.Loading) }
    var tick by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf<String?>(null) }
    var actionMsg by remember { mutableStateOf<String?>(null) }
    var actionErr by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(agentId, tick) {
        if (agentId.isBlank()) {
            state = LoadState.Failed("还没选中 agent，先回专家页选一个")
            return@LaunchedEffect
        }
        state = LoadState.Loading
        val res = SyncRepository.api.listChannels(agentId)
        state = if (!res.ok) LoadState.Failed(reasonOf(res))
        else jsonList(res.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是列表：${res.body.take(80)}")
    }

    SubPage(
        title = "通道",
        subtitle = "从哪里找它说话",
        onBack = onBack,
        onReload = { tick++ },
        note = "开关只启用/停用通道；新增通道要扫码授权，请用网页端。"
    ) {
        item {
            ChannelSummaryCard(
                total = (state as? LoadState.Ready)?.value?.size,
                online = (state as? LoadState.Ready)?.value?.count {
                    it.optBoolean("enabled", false) && it.optJSONObject("runtime")?.optBoolean("connected", false) == true
                },
            )
            Spacer(Modifier.height(10.dp))
        }

        if (actionMsg != null || actionErr != null) {
            item {
                ChannelActionCard(
                    text = actionErr ?: actionMsg!!,
                    isError = actionErr != null,
                    onDismiss = { actionMsg = null; actionErr = null }
                )
            }
        }

        when (val s = state) {
            is LoadState.Loading -> item { ChannelLoadingCard("正在读取通道状态…") }
            is LoadState.Failed -> item { ChannelErrorCard(s.reason) { tick++ } }
            is LoadState.Ready -> {
                if (s.value.isEmpty()) item { ChannelEmptyCard("这个 agent 还没有配置通道") }
                item {
                    ChannelSectionHeader(
                        title = "已配置通道",
                        description = "探测会实际发一次请求；停用只是本地开关，授权关系仍在。",
                        trailing = "${s.value.size} 个"
                    )
                }
                s.value.forEach { ch ->
                    item {
                        ChannelCard(
                            ch,
                            busy == ch.str("id", "channel_id"),
                            onToggle = { id ->
                                val enabled = ch.optBoolean("enabled", false)
                                busy = id
                                actionMsg = null
                                actionErr = null
                                scope.launch {
                                    val res = SyncRepository.api.patchChannel(
                                        agentId, id, JSONObject().put("enabled", !enabled)
                                    )
                                    busy = null
                                    if (res.ok) {
                                        actionMsg = if (!enabled) "已启用" else "已停用"
                                        tick++
                                    } else {
                                        actionErr = reasonOf(res)
                                    }
                                }
                            },
                            onTest = { id ->
                                busy = id
                                actionMsg = null
                                actionErr = null
                                scope.launch {
                                    val res = SyncRepository.api.testChannel(agentId, id, JSONObject())
                                    busy = null
                                    if (res.ok) {
                                        val o = jsonObj(res.body)
                                        actionMsg = "探测结果：" + (o?.str("message", "status", "detail")
                                            ?.takeIf { it.isNotBlank() } ?: "成功")
                                    } else {
                                        actionErr = reasonOf(res)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelSummaryCard(total: Int?, online: Int?) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Surface1, RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Surface2, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) { HubIcon(HubGlyph.CHANNEL, tint = Accent, size = 22.dp) }
            Spacer(Modifier.width(12.dp))
            Text(text = "通道状态", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            Tag("已配置 ${total ?: "…"}", color = Accent)
            Tag("在线 ${online ?: "…"}", color = if (online == null || online == 0) TextTertiary else Green)
        }
    }
}

@Composable
private fun ChannelActionCard(text: String, isError: Boolean, onDismiss: () -> Unit) {
    val tone = if (isError) Rose else Green
    Column(
        modifier = Modifier.fillMaxWidth().background(tone.copy(alpha = 0.08f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(tone, size = 6)
            Spacer(Modifier.width(8.dp))
            Text(text = if (isError) "操作失败：$text" else text, fontSize = 12.sp, lineHeight = 17.sp, color = tone, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        PillButton("知道了", filled = false) { onDismiss() }
    }
}

@Composable
private fun ChannelSectionHeader(title: String, description: String, trailing: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(3.dp))
            Text(text = description, fontSize = 10.sp, lineHeight = 15.sp, color = TextTertiary)
        }
        Spacer(Modifier.width(8.dp))
        Text(text = trailing, fontSize = 10.sp, color = TextTertiary)
    }
}

@Composable
private fun ChannelLoadingCard(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Surface1, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Accent)
        Spacer(Modifier.width(10.dp))
        Text(text = text, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun ChannelErrorCard(reason: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Rose.copy(alpha = 0.08f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(text = "读取失败：$reason", fontSize = 12.sp, lineHeight = 17.sp, color = Rose)
        Spacer(Modifier.height(10.dp))
        PillButton("重试", filled = false) { onRetry() }
    }
}

@Composable
private fun ChannelEmptyCard(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Surface1, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(36.dp).background(Surface2, CircleShape), contentAlignment = Alignment.Center) {
            HubIcon(HubGlyph.CHANNEL, tint = TextTertiary, size = 18.dp)
        }
        Spacer(Modifier.height(8.dp))
        Text(text = text, fontSize = 12.sp, color = TextTertiary)
    }
}

@Composable
private fun ChannelCard(
    ch: JSONObject,
    busy: Boolean,
    onToggle: (String) -> Unit,
    onTest: (String) -> Unit
) {
    val id = ch.str("id", "channel_id")
    val enabled = ch.optBoolean("enabled", false)
    val runtime = ch.optJSONObject("runtime")
    val connected = runtime?.optBoolean("connected", false) ?: false
    val error = runtime?.optString("error").orEmpty().takeIf { it.isNotBlank() && it != "null" }
    val status = ch.str("status", "state")

    Column(
        modifier = Modifier.fillMaxWidth().background(Surface1, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).background(Surface2, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                HubIcon(HubGlyph.CHANNEL, tint = if (enabled && connected) Green else TextTertiary, size = 18.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ch.str("name").ifBlank { ch.str("kind", "channel_type") },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    if (enabled && connected) Tag("在线", color = Green)
                    else if (!enabled) Tag("停用", color = TextTertiary)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(if (enabled && connected) Green else TextTertiary, size = 6)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            !enabled -> "已停用"
                            connected -> "在线"
                            else -> "离线"
                        },
                        fontSize = 10.sp,
                        color = if (enabled && connected) Green else TextTertiary
                    )
                    if (status.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(text = status, fontSize = 10.sp, color = TextTertiary)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().background(Surface2, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "运行状态", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                val updated = if (runtime != null) fmtTime(runtime.optLong("updated_at", 0L)) else ""
                val detail = when {
                    !error.isNullOrBlank() -> error
                    updated.isNotBlank() -> "更新于 $updated"
                    else -> "暂无运行记录"
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = detail,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = if (!error.isNullOrBlank()) Rose else TextTertiary
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = listOfNotNull(ch.str("kind", "channel_type").takeIf { it.isNotBlank() }, id.takeIf { it.isNotBlank() }).joinToString(" · "),
                fontSize = 10.sp,
                color = TextTertiary,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            if (busy) {
                Text("处理中…", fontSize = 10.sp, color = TextTertiary)
            } else {
                PillButton("探测", filled = false) { onTest(id) }
                Spacer(Modifier.width(6.dp))
                PillButton(if (enabled) "停用" else "启用", filled = !enabled) { onToggle(id) }
            }
        }
    }
}
