package com.octop.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 底部 tab 的线性图标，纯 Canvas 手绘。
 *
 * 为什么不直接用 Material Icons：`material-icons-core` 里没有 chat / 记忆 / 定时这类图形，
 * 补 `material-icons-extended` 要多背 1.2MB 包体。五个图标自己画，总代码不到 100 行，
 * 线宽和视觉重心还能统一（Material 那套混着用粗细不齐）。
 *
 * 线宽基准对齐 Octop 本体：`dashboard/src/layouts/sidebarNav.tsx` 用 lucide-react，
 * `iconSize = 16` / `iconStroke = 1.8`，即线宽 = 边长 11.25%；并统一 round cap / round join。
 * 造型也尽量向本体那几个 lucide 图形靠（GraduationCap / Sparkles）。
 */
enum class Glyph { CHAT, PERSON, STAR, CLOCK, MENU }

@Composable
fun TabIcon(
    glyph: Glyph,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 21.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        // 线宽 11.25% 对齐 lucide 1.8/16（本体 sidebarNav.tsx）
        val w = s * 0.1125f
        val st = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (glyph) {
            // 对话气泡 + 左下小尾巴（语义对应本体 MessageSquareText）
            Glyph.CHAT -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(s * 0.10f, s * 0.13f),
                    size = Size(s * 0.80f, s * 0.58f),
                    cornerRadius = CornerRadius(s * 0.26f, s * 0.26f),
                    style = st
                )
                val tail = Path().apply {
                    moveTo(s * 0.30f, s * 0.71f)
                    lineTo(s * 0.30f, s * 0.90f)
                    lineTo(s * 0.50f, s * 0.71f)
                }
                drawPath(tail, tint, style = st)
            }

            // 学士帽：造型参考 lucide-react GraduationCap，本体 sidebarNav.tsx 用它表示「专家」
            // 结构 = 帽板（菱形/平行四边形视角）+ 帽下 U 形带 + 右侧流苏
            Glyph.PERSON -> {
                val cap = Path().apply {
                    moveTo(s * 0.155f, s * 0.42f)   // 帽板左角
                    lineTo(s * 0.50f, s * 0.205f)   // 帽板顶
                    lineTo(s * 0.845f, s * 0.42f)   // 帽板右角
                    lineTo(s * 0.50f, s * 0.635f)   // 帽板底
                    close()
                }
                drawPath(cap, tint, style = st)

                // 帽下的 U 形（头/肩那一圈），lucide 里是 M6 12.5V16a6 3 0 0 0 12 0v-3.5
                val band = Path().apply {
                    moveTo(s * 0.27f, s * 0.545f)
                    lineTo(s * 0.27f, s * 0.66f)
                    cubicTo(s * 0.27f, s * 0.835f, s * 0.73f, s * 0.835f, s * 0.73f, s * 0.66f)
                    lineTo(s * 0.73f, s * 0.545f)
                }
                drawPath(band, tint, style = st)

                // 右侧垂下来的流苏
                drawLine(
                    tint,
                    Offset(s * 0.905f, s * 0.435f),
                    Offset(s * 0.905f, s * 0.655f),
                    strokeWidth = w,
                    cap = StrokeCap.Round
                )
                drawCircle(tint, radius = w * 0.9f, center = Offset(s * 0.905f, s * 0.735f))
            }

            // 星芒组（记忆）：造型参考 lucide-react Sparkles（一大一小星芒 + 一个点），
            // 本体 sidebarNav.tsx 用它表达「个性化 / 智能」，比实心四角星更不像「收藏」
            Glyph.STAR -> {
                // 大星芒：内半径取外半径的 0.40，保证线宽 11.25% 下四个内凹口还能看出来
                sparkle(tint, s * 0.44f, s * 0.585f, s * 0.285f, s * 0.114f, w)
                // 右上小星芒
                sparkle(tint, s * 0.845f, s * 0.185f, s * 0.125f, s * 0.05f, w)
                // 左下一个小点，对应 lucide Sparkles 的碎星
                drawCircle(tint, radius = w * 0.75f, center = Offset(s * 0.175f, s * 0.845f))
            }

            // 钟面 + 时针分针（对应本体 Timer）
            Glyph.CLOCK -> {
                drawCircle(tint, radius = s * 0.36f, center = Offset(s / 2f, s / 2f), style = st)
                drawLine(
                    tint, Offset(s / 2f, s * 0.30f), Offset(s / 2f, s * 0.52f),
                    strokeWidth = w, cap = StrokeCap.Round
                )
                drawLine(
                    tint, Offset(s / 2f, s * 0.52f), Offset(s * 0.66f, s * 0.60f),
                    strokeWidth = w, cap = StrokeCap.Round
                )
            }

            // 三条横线（我的）。线宽提到 11.25% 后原 0.30/0.50/0.70 间距只剩 ~1.8dp，
            // 会糊成一块，故把间距从 0.20 放到 0.235（上下两条各外扩），行长也略收。
            Glyph.MENU -> {
                listOf(0.265f, 0.50f, 0.735f).forEach { y ->
                    drawLine(
                        tint, Offset(s * 0.22f, s * y), Offset(s * 0.78f, s * y),
                        strokeWidth = w, cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/** 四角星芒（Sparkles 单颗）：8 顶点交替半径的闭合折线，锐角朝上下左右 */
private fun DrawScope.sparkle(
    tint: Color,
    cx: Float,
    cy: Float,
    outer: Float,
    inner: Float,
    w: Float
) {
    val p = Path()
    for (i in 0 until 8) {
        val angle = Math.toRadians((i * 45 - 90).toDouble())
        val r = if (i % 2 == 0) outer else inner
        val x = cx + r * cos(angle).toFloat()
        val y = cy + r * sin(angle).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, tint, style = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
