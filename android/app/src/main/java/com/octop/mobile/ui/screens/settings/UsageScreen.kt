package com.octop.mobile.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.core.sync.SyncRepository
import com.octop.mobile.ui.components.OctopCard
import com.octop.mobile.ui.components.SectionHeader
import com.octop.mobile.ui.components.ThinProgress
import com.octop.mobile.ui.theme.Accent
import com.octop.mobile.ui.theme.Amber
import com.octop.mobile.ui.theme.BgPrimary
import com.octop.mobile.ui.theme.Surface1
import com.octop.mobile.ui.theme.Surface2
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import org.json.JSONObject

/**
 * Token 统计（网页端「用量统计」）。
 *
 * 接口：GET /api/usage/summary?window=&granularity=（usage.py::user_summary）
 *   window      today | yesterday | last_7d | last_30d | all
 *   granularity total | by_day | by_agent | by_model | by_user
 *
 * 返回体字段是实测的：input_tokens / cache_read_tokens / output_tokens /
 * reasoning_tokens / total_tokens / model_calls / turns / cache_hit_percent，
 * 分桶在 buckets[]（key + label + 同样的字段）。不要照猜的字段名写。
 */
@Composable
fun UsageScreen(onBack: () -> Unit) {
    var window by remember { mutableIntStateOf(2) }
    var granularity by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<LoadState<JSONObject>>(LoadState.Loading) }

    val windows = listOf("today" to "今天", "last_7d" to "7 天", "last_30d" to "30 天", "all" to "全部")
    val grans = listOf("by_day" to "按天", "by_model" to "按模型", "by_agent" to "按专家")

    LaunchedEffect(window, granularity, tick) {
        state = LoadState.Loading
        val res = SyncRepository.hub.usageSummary(
            windows[window].first,
            grans[granularity].first
        )
        state = if (!res.ok) LoadState.Failed(reasonOf(res))
        else jsonObj(res.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是对象：${res.body.take(80)}")
    }

    SubPage(
        title = "Token 统计",
        subtitle = "它花了多少",
        onBack = onBack,
        onReload = { tick++ },
        note = "数字来自服务端 usage_log 台账，含缓存读写。缓存命中率高说明上下文被复用了，不等于浪费。"
    ) {
        item {
            SegmentedRow(
                labels = windows.map { it.second },
                selected = window,
                onSelect = { window = it }
            )
        }
        item {
            SegmentedRow(
                labels = grans.map { it.second },
                selected = granularity,
                onSelect = { granularity = it }
            )
        }

        when (val s = state) {
            is LoadState.Loading -> item { LoadingBlock() }
            is LoadState.Failed -> item { ErrorBlock(s.reason) { tick++ } }
            is LoadState.Ready -> {
                item { SummaryCard(s.value) }
                item { CacheCard(s.value) }
                val buckets = s.value.optJSONArray("buckets")?.let { a ->
                    (0 until a.length()).mapNotNull { a.optJSONObject(it) }
                } ?: emptyList()
                item { SectionHeader(grans[granularity].second + "明细", "${buckets.size} 组") }
                if (buckets.isEmpty()) {
                    item { com.octop.mobile.ui.components.EmptyHint("这个区间没有调用记录") }
                }
                val maxTotal = buckets.maxOfOrNull { it.optLong("total_tokens", 0L) } ?: 0L
                buckets.forEach { b -> item { BucketRow(b, maxTotal) } }
            }
        }
    }
}

@Composable
private fun SegmentedRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val active = selected == i
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (active) BgPrimary else Color.Transparent, RoundedCornerShape(11.dp))
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

@Composable
private fun SummaryCard(o: JSONObject) {
    OctopCard {
        Text(
            text = fmtCount(o.optLong("total_tokens", 0L)),
            fontSize = 27.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text(text = "总 token", fontSize = 11.sp, color = TextTertiary)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell("输入", fmtCount(o.optLong("input_tokens", 0L)), Modifier.weight(1f))
            StatCell("输出", fmtCount(o.optLong("output_tokens", 0L)), Modifier.weight(1f))
            StatCell("推理", fmtCount(o.optLong("reasoning_tokens", 0L)), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell("调用", o.optLong("model_calls", 0L).toString(), Modifier.weight(1f))
            StatCell("轮次", o.optLong("turns", 0L).toString(), Modifier.weight(1f))
            StatCell("每轮均", fmtCount(o.optLong("avg_per_turn", 0L)), Modifier.weight(1f))
        }
        val start = o.optLong("range_start", 0L)
        val end = o.optLong("range_end", 0L)
        if (start > 0 && end > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "区间 ${fmtTime(start)} ~ ${fmtTime(end)}",
                fontSize = 10.sp,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        Text(text = label, fontSize = 10.sp, color = TextTertiary)
    }
}

@Composable
private fun CacheCard(o: JSONObject) {
    val rate = o.optInt("cache_hit_percent", 0)
    OctopCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "缓存命中", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.weight(1f))
            Text(text = "$rate%", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Accent)
        }
        Spacer(Modifier.height(9.dp))
        ThinProgress(progress = rate / 100f, color = Accent)
        Spacer(Modifier.height(9.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell("读缓存", fmtCount(o.optLong("cache_read_tokens", 0L)), Modifier.weight(1f))
            StatCell("写缓存", fmtCount(o.optLong("cache_write_tokens", 0L)), Modifier.weight(1f))
            StatCell("未命中", fmtCount(o.optLong("uncached_input_tokens", 0L)), Modifier.weight(1f))
        }
    }
}

@Composable
private fun BucketRow(b: JSONObject, maxTotal: Long) {
    val total = b.optLong("total_tokens", 0L)
    val ratio = if (maxTotal > 0) total.toFloat() / maxTotal else 0f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = b.str("label", "key").ifBlank { "—" },
                fontSize = 12.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(text = fmtCount(total), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
        Spacer(Modifier.height(7.dp))
        ThinProgress(progress = ratio, color = Amber, height = 3)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "调 ${b.optLong("model_calls", 0L)} 次 · ${b.optLong("turns", 0L)} 轮 · 输出 " +
                fmtCount(b.optLong("output_tokens", 0L)),
            fontSize = 10.sp,
            color = TextTertiary
        )
    }
}
