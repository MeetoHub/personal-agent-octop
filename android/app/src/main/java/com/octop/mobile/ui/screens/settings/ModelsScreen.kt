package com.octop.mobile.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import org.json.JSONArray
import org.json.JSONObject

/**
 * 模型配置（网页端「模型配置」）。
 *
 * 接口：
 *   GET /api/admin/providers        服务商 + 全部模型（含 enabled / context_window / visibility）
 *   GET /api/providers/active-model 全局默认模型（没设过就是两个空串）
 *   PUT /api/providers/active-model 设全局默认 {provider_name, model}
 *
 * 只做「切换全局默认模型」这一种写操作：可逆、语义明确、不涉及密钥。
 * 加服务商、改 base_url、填 API Key 全部留给网页端——手机上填密钥容易输错还不好核对。
 * api_key 服务端是明文返回的，这里只显示前 4 位，不整串打在屏幕上。
 */
@Composable
fun ModelsScreen(onBack: () -> Unit) {
    var providers by remember { mutableStateOf<LoadState<List<JSONObject>>>(LoadState.Loading) }
    var active by remember { mutableStateOf<Pair<String, String>>("" to "") }
    var tick by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tick) {
        providers = LoadState.Loading
        val p = SyncRepository.hub.adminProviders()
        providers = if (!p.ok) LoadState.Failed(reasonOf(p))
        else jsonList(p.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是列表：${p.body.take(80)}")
        active = SyncRepository.hub.activeModel().takeIf { it.ok }?.body?.let { b ->
            jsonObj(b)?.let { (it.str("provider_name") to it.str("model")) }
        } ?: ("" to "")
    }

    SubPage(
        title = "模型配置",
        subtitle = "它用哪个脑子",
        onBack = onBack,
        onReload = { tick++ },
        note = "点某个模型右边的「设为默认」，就是全局默认模型；单个专家自己的模型在专家详情页里看。"
    ) {
        if (err != null) item { ErrorBlock(err!!) { err = null } }

        item {
            ModelSummaryCard(active = active, providerCount = (providers as? LoadState.Ready)?.value?.size)
            Spacer(Modifier.height(10.dp))
            SettingsReadonlyCard(
                title = "服务商与密钥由网页端维护",
                description = "手机端只读取结果并切换全局默认；添加服务商、改 base_url、填 API Key 都请到网页端完成。"
            )
        }

        when (val s = providers) {
            is LoadState.Loading -> item { SettingsLoadingCard("正在读取服务商和模型…") }
            is LoadState.Failed -> item { SettingsErrorCard(s.reason) { tick++ } }
            is LoadState.Ready -> {
                if (s.value.isEmpty()) item { SettingsEmptyCard("还没有服务商。去网页端加一个。") }
                item {
                    SettingsSectionHeader(
                        title = "服务商与可用模型",
                        description = "同一个服务商下只有已启用的模型能设为默认；密钥只显示前 4 位。",
                        trailing = "${s.value.size} 个"
                    )
                }
                s.value.forEach { p -> item { ProviderCard(p, active, busy) { name, model ->
                    val key = "$name/$model"
                    busy = key
                    err = null
                    scope.launch {
                        val res = SyncRepository.hub.setActiveModel(name, model)
                        busy = null
                        if (res.ok) tick++ else err = reasonOf(res)
                    }
                } } }
            }
        }
    }
}

@Composable
private fun ModelSummaryCard(active: Pair<String, String>, providerCount: Int?) {
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
            ) { HubIcon(HubGlyph.MODEL, tint = Accent, size = 22.dp) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "全局默认模型", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = "所有未单独指定的专家都走这条配置", fontSize = 10.sp, color = TextTertiary)
            }
            if (providerCount != null) Tag("$providerCount 个服务商", color = Accent)
        }
        Spacer(Modifier.height(14.dp))
        if (active.second.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Amber.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(text = "还没设全局默认模型", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Amber)
                Spacer(Modifier.height(4.dp))
                Text(text = "各专家会用自己的配置；在下方模型行点「设为默认」即可。", fontSize = 11.sp, color = TextSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface2, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(text = active.second, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(3.dp))
                Text(text = "来自 ${active.first}", fontSize = 11.sp, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun SettingsReadonlyCard(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HubIcon(HubGlyph.STORAGE, tint = TextTertiary, size = 18.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(text = description, fontSize = 10.sp, lineHeight = 15.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, description: String, trailing: String? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(3.dp))
            Text(text = description, fontSize = 10.sp, lineHeight = 15.sp, color = TextTertiary)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Text(text = trailing, fontSize = 10.sp, color = TextTertiary)
        }
    }
}

@Composable
private fun SettingsLoadingCard(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Accent)
        Spacer(Modifier.width(10.dp))
        Text(text = text, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun SettingsErrorCard(reason: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Rose.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(text = "读取失败：$reason", fontSize = 12.sp, lineHeight = 17.sp, color = Rose)
        Spacer(Modifier.height(10.dp))
        PillButton("重试", filled = false) { onRetry() }
    }
}

@Composable
private fun SettingsEmptyCard(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(Surface2, CircleShape),
            contentAlignment = Alignment.Center
        ) { HubIcon(HubGlyph.CLOUD, tint = TextTertiary, size = 18.dp) }
        Spacer(Modifier.height(8.dp))
        Text(text = text, fontSize = 12.sp, color = TextTertiary)
    }
}

@Composable
private fun ProviderCard(
    p: JSONObject,
    active: Pair<String, String>,
    busy: String?,
    onPick: (String, String) -> Unit
) {
    val name = p.str("name")
    val modelsArr = p.optJSONArray("models") ?: JSONArray()
    val models = (0 until modelsArr.length()).mapNotNull { modelsArr.optJSONObject(it) }
    val enabled = models.filter { it.optBoolean("enabled", false) }
    val key = p.str("api_key")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).background(Surface2, CircleShape),
                contentAlignment = Alignment.Center
            ) { HubIcon(HubGlyph.CLOUD, tint = Accent, size = 18.dp) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${p.str("kind")} · ${p.str("base_url")}",
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    color = TextTertiary
                )
            }
            Tag("启用 ${enabled.size}/${models.size}", color = if (enabled.isEmpty()) Rose else Green)
        }
        if (key.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "密钥 ${key.take(4)}${"*".repeat(8)}",
                fontSize = 10.sp,
                color = TextTertiary
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "可用模型",
            fontSize = 11.sp,
            color = TextTertiary
        )
        Spacer(Modifier.height(6.dp))
        if (enabled.isEmpty()) {
            Text(text = "这个服务商没有一个模型被启用", fontSize = 11.sp, color = TextSecondary)
        }
        enabled.forEachIndexed { i, m ->
            if (i > 0) Spacer(Modifier.height(8.dp))
            val modelId = m.str("id", "name")
            val isActive = active.first == name && active.second == modelId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface2, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = modelId,
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = TextPrimary
                    )
                    val ctx = m.optInt("context_window", 0)
                    val inputs = m.optJSONArray("input")?.let { a ->
                        (0 until a.length()).joinToString("/") { a.optString(it) }
                    }.orEmpty()
                    Text(
                        text = listOf(
                            if (ctx > 0) "上下文 ${ctx / 1000}k" else "",
                            inputs
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        fontSize = 10.sp,
                        color = TextTertiary
                    )
                }
                Spacer(Modifier.width(8.dp))
                when {
                    isActive -> Tag("默认", color = Accent)
                    busy == "$name/$modelId" -> Text("…", fontSize = 12.sp, color = TextTertiary)
                    else -> PillButton("设为默认", filled = false) { onPick(name, modelId) }
                }
            }
        }
    }
}
