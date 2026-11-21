package com.octop.mobile.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.octop.mobile.core.sync.SyncRepository

/**
 * lucide 的每个 `<path>` / `<polyline>` / `<circle>` 是独立元素，表里用空格把它们拼成了一条 `d`。
 * 其中不少子路径以相对命令 `m` 开头（如 `terminal` 的 `m4 17 6-6-6-6`）——原意是从元素原点起算，
 * 但拼成一条 d 后 PathParser 会把它接在上一段的终点上，整段图形飞出 24 视口。
 * 这里按 M/m 切回独立子路径分别解析，等价于还原 SVG「每个元素各自从原点起算」的语义。
 */
private fun parseLucidePath(d: String): Path {
    val out = Path()
    fun append(chunk: String) {
        val t = chunk.trim()
        if (t.isNotEmpty()) out.addPath(PathParser().parsePathString(t).toPath())
    }
    var start = 0
    for (i in d.indices) {
        val c = d[i]
        if ((c == 'M' || c == 'm') && i > start) {
            append(d.substring(start, i))
            start = i
        }
    }
    append(d.substring(start))
    return out
}

/**
 * 通用 lucide 图标渲染器。
 *
 * 坐标与线宽严格照抄 lucide-react：`viewBox="0 0 24 24"`，stroke-width 以 24 单位为基准。
 * 本体 `iconForName.tsx` 那张表用 `strokeWidth={1.8}`，少数几个是 1.75。
 *
 * @param sw lucide 的 strokeWidth 数值（1.8 / 1.75），不是像素。
 */
@Composable
fun LucideIcon(
    name: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    sw: Float = 1.8f
) {
    val d = ExpertIconTable.pathFor(name)
    val path = remember(d) { parseLucidePath(d) }
    Canvas(modifier) {
        val s = size.minDimension
        if (s <= 0f) return@Canvas
        // pivot 必须是原点：默认 pivot 是画布中心，会把 24 单位图形以中心为基准放大并甩出画布。
        translate(left = (size.width - s) / 2f, top = (size.height - s) / 2f) {
            scale(scaleX = s / 24f, scaleY = s / 24f, pivot = Offset.Zero) {
                // 线宽留在 24 单位空间里，由同一个变换缩放到像素，比例才和 lucide 一致。
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

/**
 * 专家头像。逐条复刻本体的头像链，不是「差不多」：
 *
 * 本体 `components/iconForName.tsx` 的 `ExpertIcon({ iconUrl, iconName, size })`：
 *   iconUrl 非空  → 渲染图片
 *   否则 iconName → 查 lucide 表渲染矢量图
 *   查不到        → `<Layers />` 兜底（永不空白）
 *
 * 而 `components/AgentCard.tsx:337-345` 给出容器与尺寸规则：
 *   `.agentCard2Icon` 42×42、border-radius 10px、`color: accent`、`background: accent1a`
 *   `size={iconUrl ?? agent.icon_url ? 42 : 22}` —— 有图时图占满 42，没图时图标 22。
 *
 * 优先序必须是 **图片 > 矢量图标 > layers**，顺序错了会出现「传了图却显示小图标」。
 *
 * @param slot 头像容器边长（本体默认 42dp，列表页更小）
 * @param accent 专家色，同时用于图标线色与容器 10% 底色（本体 `accent` 变量）
 */
@Composable
fun ExpertAvatar(
    iconUrl: String?,
    iconName: String?,
    accent: Color,
    slot: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    val url = iconUrl?.trim()?.takeIf { it.isNotEmpty() }

    if (url != null) {
        var bmp by remember(url) { mutableStateOf<ImageBitmap?>(null) }
        var failed by remember(url) { mutableStateOf(false) }

        LaunchedEffect(url) {
            val bytes = SyncRepository.imageBytes(url)
            if (bytes != null) {
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (decoded != null) bmp = decoded.asImageBitmap() else failed = true
            } else {
                failed = true
            }
        }

        val img = bmp
        if (img != null) {
            Box(
                modifier
                    .size(slot)
                    .clip(RoundedCornerShape(slot * 0.238f)) // 42dp 容器上是 10dp
                    .background2(accent)
            ) {
                Image(
                    bitmap = img,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(slot)
                )
            }
            return
        }
        // 还在下载时先不画东西，避免图标闪一下再被图片替换；失败则继续往下走矢量分支。
        if (!failed) {
            Box(modifier.size(slot).clip(RoundedCornerShape(slot * 0.238f)).background2(accent))
            return
        }
    }

    // 矢量图标分支（本体 `size={... ? 42 : 22}` 的 22 那半支）
    Box(
        modifier
            .size(slot)
            .clip(RoundedCornerShape(slot * 0.238f))
            .background2(accent),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        LucideIcon(
            name = iconName,
            tint = accent,
            modifier = Modifier.size(slot * 0.524f) // 22 / 42
        )
    }
}

/** 本体 `background: ${accent}1a` —— 十六进制 1a = 10.2% 不透明度。 */
private fun Modifier.background2(accent: Color): Modifier =
    this.background(accent.copy(alpha = 0.102f))
