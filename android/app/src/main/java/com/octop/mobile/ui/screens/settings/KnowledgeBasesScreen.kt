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
import com.octop.mobile.ui.components.OctopCard
import com.octop.mobile.ui.components.Tag
import com.octop.mobile.ui.components.ThinProgress
import com.octop.mobile.ui.theme.Accent
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.Surface2
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import org.json.JSONObject

/**
 * 知识库。对应网页端 /knowledge-bases。
 *
 * 接口（只读）：
 *   GET /api/knowledge-bases                        列表（knowledge_bases.py::list_bases）
 *   GET /api/knowledge-bases/capability             功能开关 + 向量模型 + 上限
 *   GET /api/knowledge-bases/{kb_id}/documents      某个库的文档树（文件夹 is_dir=true）
 *
 * 写操作（建库 / 上传 / 删除）这一批不做：上传要走 multipart，客户端只有 JSON 通道，
 * 与其放个点了没反应的按钮，不如不做。
 */
@Composable
fun KnowledgeBasesScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf<LoadState<List<JSONObject>>>(LoadState.Loading) }
    var cap by remember { mutableStateOf<JSONObject?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    var opened by remember { mutableStateOf<String?>(null) }
    var docState by remember { mutableStateOf<LoadState<List<JSONObject>>>(LoadState.Loading) }
    // 单独的 docTick：失败后重试时必须让 LaunchedEffect 重新跑一次。
    // 直接写 opened = null; opened = kbId 在同一帧里会塌成同一个值，键没变就不重跑。
    var docTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick) {
        state = LoadState.Loading
        val res = SyncRepository.api.listBases()
        cap = SyncRepository.api.capability().takeIf { it.ok }?.body?.let(::jsonObj)
        state = if (!res.ok) {
            LoadState.Failed(reasonOf(res))
        } else {
            val list = jsonList(res.body)
            if (list == null) LoadState.Failed("返回的不是列表：${res.body.take(80)}")
            else LoadState.Ready(list)
        }
    }

    // 展开某个库时才去拉文档，避免一进页面就 N 个请求
    LaunchedEffect(opened, docTick) {
        val id = opened ?: return@LaunchedEffect
        docState = LoadState.Loading
        val res = SyncRepository.api.listDocuments(id)
        docState = if (!res.ok) LoadState.Failed(reasonOf(res))
        else jsonList(res.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是列表：${res.body.take(80)}")
    }

    SubPage(
        title = "知识库",
        subtitle = "它查资料的地方",
        onBack = onBack,
        onReload = { tick++; opened = null },
        note = "点开一个库看里面的文档。建库、上传文件请在网页端操作，手机端只读。"
    ) {
        // 能力卡片要等接口真的回来才画，否则会先闪一下「未启用」再跳成「已启用」
        val capNow = cap
        if (capNow != null) item { CapabilityRow(capNow) }

        when (val s = state) {
            is LoadState.Loading -> item { LoadingBlock() }
            is LoadState.Failed -> item { ErrorBlock(s.reason) { tick++ } }
            is LoadState.Ready -> {
                if (s.value.isEmpty()) {
                    item { com.octop.mobile.ui.components.EmptyHint("还没有知识库。去网页端建一个。") }
                }
                s.value.forEach { kb ->
                    val kbId = kb.str("id", "knowledge_base_id")
                    val isOpen = opened == kbId
                    item { KbCard(kb) { opened = if (isOpen) null else kbId } }
                    if (isOpen) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Surface2, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                when (val d = docState) {
                                    is LoadState.Loading -> LoadingBlock("读文档列表…")
                                    is LoadState.Failed -> ErrorBlock(d.reason) { docTick++ }
                                    is LoadState.Ready -> {
                                        if (d.value.isEmpty()) {
                                            Text("这个库还是空的", fontSize = 12.sp, color = TextTertiary)
                                        } else {
                                            Text(
                                                "共 ${d.value.size} 项（含文件夹）",
                                                fontSize = 11.sp,
                                                color = TextTertiary
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            d.value.take(60).forEachIndexed { idx, doc ->
                                                if (idx > 0) Spacer(Modifier.height(7.dp))
                                                DocRow(doc)
                                            }
                                            if (d.value.size > 60) {
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    "只显示前 60 项，共 ${d.value.size} 项",
                                                    fontSize = 10.sp,
                                                    color = TextTertiary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityRow(cap: JSONObject?) {
    OctopCard {
        val enabled = cap?.optBoolean("feature_enabled", false) ?: false
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (enabled) "知识库已启用" else "知识库未启用",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(Modifier.weight(1f))
            Tag(if (cap?.optBoolean("usable", false) == true) "可用" else "不可用",
                color = if (cap?.optBoolean("usable", false) == true) Green else TextTertiary)
        }
        Spacer(Modifier.height(8.dp))
        KvRow("向量模型", cap?.str("selected_model") ?: "—")
        KvRow("后端", (cap?.str("backend") ?: "").ifBlank { "—" })
        val limits = cap?.optJSONObject("limits")
        if (limits != null) {
            KvRow(
                "上限",
                "每库 ${limits.optInt("max_docs_per_kb")} 篇 / ${limits.optInt("max_bases_per_owner")} 个库"
            )
        }
    }
}

@Composable
private fun KbCard(kb: JSONObject, onClick: () -> Unit) {
    val docCount = kb.optInt("doc_count", 0)
    val maxDocs = kb.optInt("max_documents", 0)
    val ratio = if (maxDocs > 0) docCount.toFloat() / maxDocs else 0f
    OctopCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = kb.str("name").ifBlank { "未命名" },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(Modifier.weight(1f))
            Tag("$docCount 篇", color = Accent)
            Spacer(Modifier.width(6.dp))
            Text("›", fontSize = 15.sp, color = TextTertiary)
        }
        val desc = kb.str("description")
        if (desc.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(text = desc, fontSize = 11.sp, lineHeight = 16.sp, color = TextSecondary)
        }
        if (maxDocs > 0) {
            Spacer(Modifier.height(10.dp))
            ThinProgress(progress = ratio, color = Accent)
            Spacer(Modifier.height(5.dp))
            Text(
                text = "$docCount / $maxDocs",
                fontSize = 10.sp,
                color = TextTertiary
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = listOf(
                kb.str("embedding_model"),
                kb.str("visibility").let { if (it == "shared") "共享" else "仅自己" },
                fmtTime(kb.optLong("updated_at", 0L))
            ).filter { it.isNotBlank() && it != "—" }.joinToString(" · "),
            fontSize = 10.sp,
            color = TextTertiary
        )
    }
}

@Composable
private fun DocRow(doc: JSONObject) {
    val isDir = doc.optBoolean("is_dir", false)
    val status = doc.str("status")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isDir) "▸" else "·",
            fontSize = 12.sp,
            color = TextTertiary,
            modifier = Modifier.width(14.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.str("path", "filename"),
                fontSize = 12.sp,
                color = TextPrimary
            )
            val sub = listOf(
                if (isDir) "文件夹" else if (doc.optInt("chunk_count", 0) > 0) "${doc.optInt("chunk_count")} 块" else "",
                if (!isDir) doc.str("status") else "",
                doc.str("error_message")
            ).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(text = sub, fontSize = 10.sp, color = if (status == "failed") com.octop.mobile.ui.theme.Rose else TextTertiary)
            }
        }
    }
}
