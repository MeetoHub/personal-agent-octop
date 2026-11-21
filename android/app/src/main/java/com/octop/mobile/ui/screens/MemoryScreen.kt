package com.octop.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.core.sync.SyncRepository
import com.octop.mobile.ui.AppViewModel
import com.octop.mobile.ui.components.EmptyHint
import com.octop.mobile.ui.components.PageHeader
import com.octop.mobile.ui.components.SectionHeader
import com.octop.mobile.ui.theme.Accent
import com.octop.mobile.ui.theme.AccentSoft
import com.octop.mobile.ui.theme.Bg
import com.octop.mobile.ui.theme.BgPrimary
import com.octop.mobile.ui.theme.BgSecondary
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.OutlineCol
import com.octop.mobile.ui.theme.SoftGreen
import com.octop.mobile.ui.theme.Surface1
import com.octop.mobile.ui.theme.TextInverse
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * 记忆页 —— App 里最能说明「它真的记得我」的一页。
 *
 * 数据全部走真实接口（agent 维度）：
 *   GET  /api/agents/{id}/memory/stats/counts   概览数字
 *   POST /api/agents/{id}/memory/candidates/list 待确认（它想记住、等你点头）
 *   POST /api/agents/{id}/memory/atoms/list      已记住的原子
 *   POST /api/agents/{id}/memory/episodes/list   事件片段
 *   POST /api/agents/{id}/memory/candidates/{cid}:promote / :reject  一键处理
 *
 * 后端这些接口是 RPC 透传到 octop_memory 服务，返回体是 dict 包一层数组，
 * 键名不固定（atoms / items / candidates 都见过），所以这里用 fetchList 容错取第一个数组。
 *
 * 版式：借鉴桌面端 MemoryPanel 的「概览 → 分栏 → 列表」三级结构：
 * 四个数字合并成单块四等分概览（少卡片、少边框），分段作为带计数的独立导航行，
 * 列表卡片压缩内边距、弱化次要信息，整体更紧、层级更清楚。
 */
@Composable
fun MemoryScreen(vm: AppViewModel) {
    val experts by vm.experts.collectAsState()
    val agentId = remember(experts) { experts.firstOrNull()?.id }

    var counts by remember { mutableStateOf<JSONObject?>(null) }
    var atoms by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var candidates by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var episodes by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var segment by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var acting by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(agentId, tick) {
        val id = agentId
        if (id.isNullOrBlank()) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        counts = SyncRepository.api.statsCounts(id).let { res ->
            if (res.ok) runCatching { JSONObject(res.body) }.getOrNull() else null
        }
        candidates = fetchList(
            SyncRepository.api.listCandidates(id, JSONObject().put("status", "pending")).body
        )
        episodes = fetchList(SyncRepository.api.listEpisodes(id, JSONObject().put("limit", 30)).body)
        loading = false
    }

    LaunchedEffect(agentId, tick, query) {
        val id = agentId
        if (id.isNullOrBlank()) return@LaunchedEffect
        val body = JSONObject().put("limit", 80)
        if (query.isNotBlank()) body.put("query", query.trim())
        atoms = fetchList(SyncRepository.api.listAtoms(id, body).body)
    }

    Column(Modifier.fillMaxSize().background(Bg)) {
        PageHeader(
            title = "记忆",
            subtitle = "它记得你什么",
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 10.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { StatsRow(counts) }

            item {
                Segmented(
                    selected = segment,
                    onSelect = { segment = it },
                    labels = listOf(
                        "待确认 ${candidates.size}".trim(),
                        "记忆 ${counts?.optInt("atoms", atoms.size) ?: atoms.size}".trim(),
                        "片段 ${counts?.optInt("episodes", episodes.size) ?: episodes.size}".trim()
                    )
                )
            }

            when (segment) {
                0 -> {
                    if (candidates.isEmpty()) {
                        item {
                            EmptyHint(
                                if (loading) "正在读它的记忆…"
                                else "没有等你确认的记忆。等它攒够了会来问你。"
                            )
                        }
                    } else {
                        item { SectionHeader("等你点头", "${candidates.size} 条") }
                        items(candidates, key = { it.optString("id") }) { c ->
                            val cid = c.optString("id")
                            CandidateCard(
                                item = c,
                                busy = acting == cid,
                                onPromote = {
                                    val id = agentId
                                    if (id != null) scope.launch {
                                        acting = cid
                                        SyncRepository.api.promoteCandidate(id, cid, JSONObject())
                                        acting = null
                                        tick++
                                    }
                                },
                                onReject = {
                                    val id = agentId
                                    if (id != null) scope.launch {
                                        acting = cid
                                        SyncRepository.api.rejectCandidate(id, cid, JSONObject())
                                        acting = null
                                        tick++
                                    }
                                }
                            )
                        }
                    }
                }

                1 -> {
                    item { MemorySearchField(query = query, onQuery = { query = it }) }
                    if (atoms.isEmpty()) {
                        item {
                            EmptyHint(
                                when {
                                    query.isNotBlank() -> "没有找到包含「${query.trim()}」的记忆。"
                                    loading -> "正在读…"
                                    else -> "还没有落成条目的记忆。"
                                }
                            )
                        }
                    } else {
                        item {
                            SectionHeader(
                                "已记住",
                                "${counts?.optInt("atoms", atoms.size) ?: atoms.size} 条"
                            )
                        }
                        items(atoms, key = { it.optString("id") }) { a -> MemoryCard(a) }
                    }
                }

                else -> {
                    if (episodes.isEmpty()) {
                        item { EmptyHint(if (loading) "正在读…" else "还没有事件片段。") }
                    } else {
                        item {
                            SectionHeader(
                                "事件片段",
                                "${counts?.optInt("episodes", episodes.size) ?: episodes.size} 条"
                            )
                        }
                        items(episodes, key = { it.optString("id") }) { e -> MemoryCard(e) }
                    }
                }
            }
        }
    }
}

// ── 概览：四格数字合成单块，减少卡片边框与留白 ──────────

@Composable
private fun StatsRow(counts: JSONObject?) {
    val cells = listOf(
        Triple("原子", counts?.optInt("atoms", 0) ?: 0, Accent),
        Triple("实体", counts?.optInt("entities", 0) ?: 0, Green),
        Triple("片段", counts?.optInt("episodes", 0) ?: 0, TextPrimary),
        Triple("待确认", counts?.optInt("candidates_pending", 0) ?: 0, TextPrimary)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(18.dp))
            .padding(vertical = 14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            cells.forEachIndexed { i, (label, value, color) ->
                if (i > 0) {
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(34.dp)
                            .background(OutlineCol)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$value",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(text = label, fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
        // 七天增量，有就显示，没有就整行不占位
        val delta = counts?.optInt("atoms_delta_7d", 0) ?: 0
        if (delta > 0) {
            Spacer(Modifier.height(9.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlineCol)
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = "最近 7 天新记住 $delta 条",
                fontSize = 11.sp,
                color = TextTertiary,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

// ── 分段控件 ────────────────────────────────────────────

@Composable
private fun Segmented(selected: Int, onSelect: (Int) -> Unit, labels: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSecondary, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val active = selected == i
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (active) BgPrimary else Color.Transparent,
                        RoundedCornerShape(11.dp)
                    )
                    .clickable { onSelect(i) }
                    .padding(vertical = 8.dp),
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

// ── 卡片 ────────────────────────────────────────────────

@Composable
private fun CandidateCard(
    item: JSONObject,
    busy: Boolean,
    onPromote: () -> Unit,
    onReject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KindChip(kindOf(item), AccentSoft, Accent)
            Spacer(Modifier.width(8.dp))
            Text(text = timeOf(item), fontSize = 11.sp, color = TextTertiary)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = textOf(item),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = TextPrimary
        )
        val quote = item.optString("verbatim_quote")
        if (quote.isNotBlank() && quote != textOf(item)) {
            Spacer(Modifier.height(9.dp))
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(
                    Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(OutlineCol, RoundedCornerShape(1.dp))
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = quote,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = TextTertiary
                )
            }
        }
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniButton(
                text = if (busy) "处理中…" else "记住",
                filled = true,
                enabled = !busy,
                onClick = onPromote
            )
            MiniButton(
                text = "忽略",
                filled = false,
                enabled = !busy,
                onClick = onReject
            )
        }
    }
}

@Composable
private fun MemoryCard(item: JSONObject) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KindChip(kindOf(item), SoftGreen, Green)
            Spacer(Modifier.width(8.dp))
            Text(text = timeOf(item), fontSize = 11.sp, color = TextTertiary)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = textOf(item),
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = TextPrimary
        )
    }
}

@Composable
private fun KindChip(text: String, bg: Color, fg: Color) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

@Composable
private fun MiniButton(text: String, filled: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val bg = when {
        filled && enabled -> Accent
        filled -> OutlineCol
        else -> OutlineCol
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (filled && enabled) TextInverse else TextPrimary
        )
    }
}

// ── JSON 容错 ───────────────────────────────────────────

/** 后端返回 dict 里包着数组，键名不固定；顶层是数组也能吃。 */
private fun fetchList(body: String?): List<JSONObject> {
    if (body.isNullOrBlank()) return emptyList()
    runCatching { JSONArray(body) }.getOrNull()?.let { arr ->
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }
    val obj = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
    val keys = obj.keys()
    while (keys.hasNext()) {
        val value = obj.opt(keys.next())
        if (value is JSONArray) {
            return (0 until value.length()).mapNotNull { value.optJSONObject(it) }
        }
    }
    return emptyList()
}

private fun textOf(o: JSONObject): String =
    listOf("assertion", "title", "text", "content", "statement", "summary", "verbatim_quote")
        .firstNotNullOfOrNull { key -> o.optString(key).takeIf { it.isNotBlank() } }
        ?: "（无内容）"

private fun kindOf(o: JSONObject): String =
    listOf("kind", "atom_kind", "candidate_type", "type")
        .firstNotNullOfOrNull { key -> o.optString(key).takeIf { it.isNotBlank() } }
        ?: "记忆"

/** created_at 既可能是 ISO 串也可能是秒/毫秒时间戳，都兜住。 */
private fun timeOf(o: JSONObject): String {
    val raw = o.optString("created_at").ifBlank { o.optString("updated_at") }
    if (raw.length >= 16 && raw[4] == '-') {
        return raw.substring(5, 16).replace('T', ' ')
    }
    val n = raw.toLongOrNull() ?: 0L
    if (n <= 0L) return ""
    val millis = if (n > 1_000_000_000_000L) n else n * 1000L
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    return "%02d-%02d %02d:%02d".format(
        cal.get(java.util.Calendar.MONTH) + 1,
        cal.get(java.util.Calendar.DAY_OF_MONTH),
        cal.get(java.util.Calendar.HOUR_OF_DAY),
        cal.get(java.util.Calendar.MINUTE)
    )
}

/** 记忆搜索框：直接打到 atoms/list 的 query 参数（后端在 assertion/原话/搜索词上做子串匹配）*/
@Composable
private fun MemorySearchField(query: String, onQuery: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(14.dp))
            .padding(horizontal = 13.dp, vertical = 10.dp)
    ) {
        if (query.isEmpty()) {
            Text("搜记忆：关键词、人名、某件事", fontSize = 12.sp, color = TextTertiary)
        }
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
