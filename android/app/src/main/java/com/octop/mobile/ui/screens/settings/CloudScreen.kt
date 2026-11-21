package com.octop.mobile.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.octop.mobile.ui.components.Tag
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.Rose
import com.octop.mobile.ui.theme.Surface1
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import org.json.JSONObject

/**
 * 云端协同（网页端「云端协同」）。对应腾讯云账号授权 + 云主机列表。
 *
 * 接口：
 *   GET /api/cloud/status    {authorized, state, expires_at?, remaining_seconds?, can_refresh}
 *   GET /api/cloud/instances {instances[], regions[], fetched_at, cached}；未授权时 HTTP 409 + detail
 *
 * 注意 409 不是「出错」，是「还没授权」——这是正常业务态，必须单独说清楚，
 * 不能跟网络失败混成同一条红条，否则用户会以为服务坏了。
 */
@Composable
fun CloudScreen(onBack: () -> Unit) {
    var status by remember { mutableStateOf<LoadState<JSONObject>>(LoadState.Loading) }
    var instances by remember { mutableStateOf<LoadState<List<JSONObject>>>(LoadState.Loading) }
    var notAuthorized by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick) {
        status = LoadState.Loading
        instances = LoadState.Loading
        notAuthorized = null

        val st = SyncRepository.hub.cloudStatus()
        status = if (!st.ok) LoadState.Failed(reasonOf(st))
        else jsonObj(st.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是对象：${st.body.take(80)}")

        val inst = SyncRepository.hub.cloudInstances()
        when {
            inst.code == 409 -> {
                // 未授权：服务端明确说「尚未授权腾讯云」，这里不当作失败
                notAuthorized = runCatching {
                    JSONObject(inst.body).optString("detail")
                }.getOrNull().takeUnless { it.isNullOrBlank() } ?: "尚未授权腾讯云"
                instances = LoadState.Ready(emptyList())
            }
            !inst.ok -> instances = LoadState.Failed(reasonOf(inst))
            else -> instances = jsonObj(inst.body)?.let { o ->
                val arr = o.optJSONArray("instances")
                val list = if (arr == null) emptyList() else (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
                LoadState.Ready(list)
            } ?: LoadState.Failed("返回的不是对象：${inst.body.take(80)}")
        }
    }

    SubPage(
        title = "云端协同",
        subtitle = "腾讯云主机概况",
        onBack = onBack,
        onReload = { tick++ },
        note = "授权要走扫码登录换取临时凭据，手机端只展示状态。授权请用网页端。"
    ) {
        when (val s = status) {
            is LoadState.Loading -> item { LoadingBlock() }
            is LoadState.Failed -> item { ErrorBlock(s.reason) { tick++ } }
            is LoadState.Ready -> {
                val o = s.value
                val authorized = o.optBoolean("authorized", false)
                item {
                    KvCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (authorized) "已授权" else "未授权",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (authorized) TextPrimary else TextSecondary
                            )
                            Spacer(Modifier.weight(1f))
                            Tag(
                                text = when (o.str("state")) {
                                    "valid" -> "有效"
                                    "expired" -> "已过期"
                                    "missing" -> "无凭据"
                                    else -> o.str("state").ifBlank { "未知" }
                                },
                                color = if (authorized) Green else Rose
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        KvRow("状态码", o.str("state"))
                        val exp = o.optLong("expires_at", 0L)
                        if (exp > 0) KvRow("过期于", fmtTime(exp))
                        val remain = o.optLong("remaining_seconds", 0L)
                        if (remain > 0) {
                            KvRow("剩余", "${remain / 3600} 小时 ${(remain % 3600) / 60} 分")
                        }
                        KvRow("可自动续期", if (o.optBoolean("can_refresh", false)) "是" else "否")
                    }
                }
            }
        }

        item { SectionTitle("云主机") }
        val na = notAuthorized
        when {
            na != null -> item {
                KvCard {
                    Text(text = na, fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "在网页端「云端协同」里扫码授权后，这里会列出你的云服务器。",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = TextTertiary
                    )
                }
            }
            else -> when (val s = instances) {
                is LoadState.Loading -> item { LoadingBlock() }
                is LoadState.Failed -> item { ErrorBlock(s.reason) { tick++ } }
                is LoadState.Ready -> {
                    if (s.value.isEmpty()) {
                        item { com.octop.mobile.ui.components.EmptyHint("这个账号下没有云主机") }
                    }
                    s.value.forEach { i -> item { InstanceCard(i) } }
                }
            }
        }
    }
}

@Composable
private fun InstanceCard(i: JSONObject) {
    val state = i.str("state")
    val running = state.equals("RUNNING", ignoreCase = true)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = i.str("name").ifBlank { i.str("instance_id") },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${i.str("region")} · ${i.str("zone")}",
                    fontSize = 10.sp,
                    color = TextTertiary
                )
            }
            Spacer(Modifier.width(8.dp))
            Tag(state.ifBlank { "未知" }, color = if (running) Green else TextTertiary)
        }
        Spacer(Modifier.height(8.dp))
        KvRow("配置", "${i.optInt("cpu", 0)} 核 / ${i.optInt("memory_gb", 0)} GB")
        val pub = i.optJSONArray("public_ips")?.let { a ->
            (0 until a.length()).joinToString(", ") { a.optString(it) }
        }.orEmpty()
        val pri = i.optJSONArray("private_ips")?.let { a ->
            (0 until a.length()).joinToString(", ") { a.optString(it) }
        }.orEmpty()
        if (pub.isNotBlank()) KvRow("公网 IP", pub)
        if (pri.isNotBlank()) KvRow("内网 IP", pri)
        val os = i.str("os_name")
        if (os.isNotBlank()) KvRow("系统", os)
    }
}
