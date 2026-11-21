package com.octop.mobile.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.core.sync.OctopClient
import com.octop.mobile.ui.components.EmptyHint
import com.octop.mobile.ui.components.PillButton
import com.octop.mobile.ui.theme.Accent
import com.octop.mobile.ui.theme.Bg
import com.octop.mobile.ui.theme.OutlineCol
import com.octop.mobile.ui.theme.Rose
import com.octop.mobile.ui.theme.Surface1
import com.octop.mobile.ui.theme.TextInverse
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import org.json.JSONObject
import java.util.Locale

/**
 * 「我的」下面那 12 个二级页共用的骨架。
 *
 * 为什么单独抽出来：每页都必须交代清楚「在加载 / 没数据 / 请求挂了」三种状态，
 * 各写一遍必然有人漏掉失败态，最后又是一片静默空白。这里统一收口，
 * 页面只负责「拿到数据怎么画」，不负责「拿不到怎么解释」。
 */

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Failed(val reason: String) : LoadState<Nothing>
    data class Ready<T>(val value: T) : LoadState<T>
}

/**
 * 把失败的 HTTP 响应翻译成一句人话。
 *
 * 服务端错误体有两种形状：OctopError → {"error":{"message":..}}，
 * FastAPI 原生 → {"detail":..}（校验错误时 detail 是数组，这里直接取文本）。
 * 401 单独说清楚，因为最常见的原因就是令牌 24 小时到期。
 */
fun reasonOf(res: OctopClient.Response): String {
    if (res.code == -1) return "连不上本体：${res.body.take(70)}"
    val o = runCatching { JSONObject(res.body) }.getOrNull()
    val msg = o?.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
        ?: o?.optString("detail")?.takeIf { it.isNotBlank() && it != "null" }
    val codeText = when (res.code) {
        401 -> "令牌已过期，请重新登录"
        403 -> "这个账号没有该权限"
        404 -> "服务端没有这个接口"
        409 -> "服务端拒绝：" + (msg ?: "").ifBlank { "状态不允许" }
        else -> "HTTP ${res.code}"
    }
    return when {
        msg == null -> codeText
        res.code == 401 || res.code == 403 || res.code == 404 -> msg
        else -> "$msg（$codeText）"
    }.take(140)
}

/** 列表接口的响应 → JSONObject 列表。不是数组就返回 null（交给失败态），别假装是空列表。 */
fun jsonList(body: String): List<JSONObject>? =
    runCatching { org.json.JSONArray(body) }.getOrNull()?.let { a ->
        (0 until a.length()).mapNotNull { a.optJSONObject(it) }
    }

/** 单个对象接口的响应。 */
fun jsonObj(body: String): JSONObject? = runCatching { JSONObject(body) }.getOrNull()

/** 请求体是 JSON 时，把顶层对象字段读成字符串；缺字段就给空串。 */
fun JSONObject.str(vararg keys: String): String {
    for (k in keys) {
        val v = opt(k) ?: continue
        val s = if (v is String) v else v.toString()
        if (s.isNotBlank() && s != "null") return s
    }
    return ""
}

/**
 * 二级页骨架：返回 + 标题 + 刷新 + 内容列表。
 *
 * content 收的是 LazyListScope 而不是 Composable，页面里直接写 item { } 即可，
 * 不用每页再套一层 LazyColumn（套错了就是滚动嵌套，滑不动）。
 */
@Composable
fun SubPage(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    onReload: () -> Unit,
    note: String? = null,
    content: LazyListScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        // 顶栏改成网页端 Settings 的「操作行 + 大标题」两段式：
        //   第一行只有返回箭头和刷新（轻量、贴边）；
        //   第二行是 26sp 大标题 + 说明，作为内容区的视觉地标。
        // 之前标题、副标题、刷新按钮挤在同一行，14 个子页看起来全是同一个
        // 小工具条，层级扁平；改成大标题后每个页面的身份一眼可辨。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 10.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onReload) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "刷新",
                    tint = TextTertiary
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 14.dp)) {
            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
                color = TextPrimary
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(text = subtitle, fontSize = 12.sp, lineHeight = 17.sp, color = TextTertiary)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 0.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
            if (note != null) {
                item {
                    Text(
                        text = note,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        color = TextTertiary,
                        modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingBlock(text: String = "加载中…") {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 13.sp, color = TextTertiary)
    }
}

/** 失败态：把原因写出来 + 给一个重试按钮，别让用户面对空白猜。 */
@Composable
fun ErrorBlock(reason: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Rose.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(Rose, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = "失败", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextInverse)
            }
            Spacer(Modifier.width(8.dp))
            Text(text = "没取到数据", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Rose)
        }
        Spacer(Modifier.height(8.dp))
        Text(text = reason, fontSize = 12.sp, lineHeight = 18.sp, color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        PillButton("重试", filled = true) { onRetry() }
    }
}

/** 卡片里的「标签 → 值」行。值太长就换行，不截断，省得看不出是什么。 */
@Composable
fun KvRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = label, fontSize = 12.sp, color = TextTertiary, modifier = Modifier.width(96.dp))
        Text(
            text = value.ifBlank { "—" },
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = valueColor ?: TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

/** 一个只有分割线的白卡片，用来装 KvRow。 */
@Composable
fun KvCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content
    )
}

/**
 * 分节小标题。左边一条 2dp 品牌色竖条作视觉锚，右侧可挂计数/范围。
 * 原来只有一行灰字，14 个二级页里分节边界几乎看不出来；加上竖条后
 * 每个区块有明确的起点，长列表扫读成本明显下降。
 */
@Composable
fun SectionTitle(text: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(12.dp)
                .background(Accent, RoundedCornerShape(1.dp))
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            letterSpacing = 0.3.sp
        )
        Spacer(Modifier.weight(1f))
        if (trailing != null) Text(text = trailing, fontSize = 11.sp, color = TextTertiary)
    }
}

/** 32 位小数？不用。只是统一「大数字」的读法：万 / 亿。 */
fun fmtCount(n: Long): String = when {
    n >= 100_000_000L -> String.format(Locale.CHINA, "%.2f 亿", n / 100_000_000.0)
    n >= 10_000L -> String.format(Locale.CHINA, "%.1f 万", n / 10_000.0)
    else -> n.toString()
}

/** epoch 秒 → MM-dd HH:mm；0 显示破折号，别显示 1970。 */
fun fmtTime(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return "—"
    return runCatching {
        java.time.Instant.ofEpochSecond(epochSeconds)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))
    }.getOrDefault("—")
}

/** 一般页面顶部「当前范围」的说明卡片，例如用了哪个 agent。 */
@Composable
fun HintCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Accent.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = text, fontSize = 11.sp, lineHeight = 16.sp, color = TextSecondary)
    }
}
