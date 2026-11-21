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

/**
 * 「我的」功能中心那一排入口用的线性图标。
 *
 * 为什么不引 material-icons-extended：本体这套图标（对话 / 记忆 / 定时）是纯 Canvas 画的，
 * 再混进来的官方图标线宽、圆角、视觉重心都不一样，一排看过去会参差。
 * 这里沿用同一套画法，线宽 = 边长 11.25%（对齐本体 lucide 1.8/16），13 个图形不到 200 行。
 */
enum class HubGlyph {
    LIBRARY, SKILL, CONNECTOR, CHANNEL, CLOUD, USAGE,
    MODEL, USER, APP, SECURITY, PLUGIN, STORAGE, DEVICE
}

@Composable
fun HubIcon(glyph: HubGlyph, tint: Color, modifier: Modifier = Modifier, size: Dp = 19.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        // 线宽 11.25% 对齐 lucide 1.8/16（本体 sidebarNav.tsx）
        val st = Stroke(width = s * 0.1125f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (glyph) {
            // 三本叠起来的书
            HubGlyph.LIBRARY -> {
                drawRoundRect(
                    tint,
                    topLeft = Offset(s * 0.14f, s * 0.18f),
                    size = Size(s * 0.60f, s * 0.64f),
                    cornerRadius = CornerRadius(s * 0.08f, s * 0.08f),
                    style = st
                )
                drawLine(tint, Offset(s * 0.74f, s * 0.20f), Offset(s * 0.86f, s * 0.82f), st.width, StrokeCap.Round)
                // 两行文字间距 0.20（原 0.16）：线宽提到 11.25% 后原来的间距只剩 0.05 的空隙，会粘在一起
                drawLine(tint, Offset(s * 0.28f, s * 0.38f), Offset(s * 0.62f, s * 0.38f), st.width, StrokeCap.Round)
                drawLine(tint, Offset(s * 0.28f, s * 0.58f), Offset(s * 0.62f, s * 0.58f), st.width, StrokeCap.Round)
            }
            // 四角星（技能 / 本事）
            HubGlyph.SKILL -> star(tint, s, st)
            // 两个扣环 = 连接。半径 0.165 / 圆心 0.33 与 0.67，正好相切而不糊成一团
            HubGlyph.CONNECTOR -> {
                drawCircle(tint, s * 0.165f, Offset(s * 0.33f, s * 0.50f), style = st)
                drawCircle(tint, s * 0.165f, Offset(s * 0.67f, s * 0.50f), style = st)
            }
            // 聊天气泡 + 一个点 = 通道
            HubGlyph.CHANNEL -> {
                drawRoundRect(
                    tint,
                    topLeft = Offset(s * 0.12f, s * 0.16f),
                    size = Size(s * 0.76f, s * 0.56f),
                    cornerRadius = CornerRadius(s * 0.22f, s * 0.22f),
                    style = st
                )
                val tail = Path().apply {
                    moveTo(s * 0.32f, s * 0.70f)
                    lineTo(s * 0.30f, s * 0.88f)
                    lineTo(s * 0.48f, s * 0.70f)
                }
                drawPath(tail, tint, style = st)
            }
            // 云
            HubGlyph.CLOUD -> {
                val p = Path().apply {
                    moveTo(s * 0.26f, s * 0.72f)
                    cubicTo(s * 0.08f, s * 0.72f, s * 0.08f, s * 0.46f, s * 0.28f, s * 0.46f)
                    cubicTo(s * 0.28f, s * 0.24f, s * 0.62f, s * 0.20f, s * 0.66f, s * 0.44f)
                    cubicTo(s * 0.90f, s * 0.42f, s * 0.94f, s * 0.72f, s * 0.72f, s * 0.72f)
                    lineTo(s * 0.26f, s * 0.72f)
                }
                drawPath(p, tint, style = st)
            }
            // 三根柱 = 统计。柱宽收到 0.12、间距放到 0.295，
            // 因为线宽翻到 11.25% 后每根柱的外框会左右各胀 0.056，再按原来的 0.16 宽会连成一片
            HubGlyph.USAGE -> {
                drawRoundRect(tint, Offset(s * 0.145f, s * 0.52f), Size(s * 0.12f, s * 0.30f), CornerRadius(s * 0.04f, s * 0.04f), style = st)
                drawRoundRect(tint, Offset(s * 0.440f, s * 0.30f), Size(s * 0.12f, s * 0.52f), CornerRadius(s * 0.04f, s * 0.04f), style = st)
                drawRoundRect(tint, Offset(s * 0.735f, s * 0.42f), Size(s * 0.12f, s * 0.40f), CornerRadius(s * 0.04f, s * 0.04f), style = st)
            }
            // 芯片：方框 + 四根引脚
            HubGlyph.MODEL -> {
                drawRoundRect(
                    tint,
                    topLeft = Offset(s * 0.24f, s * 0.24f),
                    size = Size(s * 0.52f, s * 0.52f),
                    cornerRadius = CornerRadius(s * 0.10f, s * 0.10f),
                    style = st
                )
                drawCircle(tint, s * 0.09f, Offset(s * 0.50f, s * 0.50f), style = st)
                // 引脚加长到 0.08/0.92：线宽翻倍后太短的引脚会被正方形边框的描边吃掉
                listOf(
                    Offset(s * 0.50f, s * 0.08f) to Offset(s * 0.50f, s * 0.24f),
                    Offset(s * 0.50f, s * 0.76f) to Offset(s * 0.50f, s * 0.92f),
                    Offset(s * 0.08f, s * 0.50f) to Offset(s * 0.24f, s * 0.50f),
                    Offset(s * 0.76f, s * 0.50f) to Offset(s * 0.92f, s * 0.50f)
                ).forEach { (a, b) -> drawLine(tint, a, b, st.width, StrokeCap.Round) }
            }
            // 头 + 肩
            HubGlyph.USER -> {
                drawCircle(tint, s * 0.16f, Offset(s * 0.50f, s * 0.34f), style = st)
                val p = Path().apply {
                    moveTo(s * 0.20f, s * 0.84f)
                    cubicTo(s * 0.22f, s * 0.60f, s * 0.78f, s * 0.60f, s * 0.80f, s * 0.84f)
                }
                drawPath(p, tint, style = st)
            }
            // 滑块 = 应用设置
            HubGlyph.APP -> {
                drawLine(tint, Offset(s * 0.12f, s * 0.32f), Offset(s * 0.88f, s * 0.32f), st.width, StrokeCap.Round)
                drawLine(tint, Offset(s * 0.12f, s * 0.68f), Offset(s * 0.88f, s * 0.68f), st.width, StrokeCap.Round)
                drawCircle(tint, s * 0.11f, Offset(s * 0.36f, s * 0.32f), style = Stroke(st.width * 1.1f, cap = StrokeCap.Round))
                drawCircle(tint, s * 0.11f, Offset(s * 0.64f, s * 0.68f), style = Stroke(st.width * 1.1f, cap = StrokeCap.Round))
            }
            // 盾牌
            HubGlyph.SECURITY -> {
                val p = Path().apply {
                    moveTo(s * 0.50f, s * 0.12f)
                    lineTo(s * 0.82f, s * 0.26f)
                    lineTo(s * 0.82f, s * 0.54f)
                    cubicTo(s * 0.82f, s * 0.76f, s * 0.64f, s * 0.86f, s * 0.50f, s * 0.90f)
                    cubicTo(s * 0.36f, s * 0.86f, s * 0.18f, s * 0.76f, s * 0.18f, s * 0.54f)
                    lineTo(s * 0.18f, s * 0.26f)
                    close()
                }
                drawPath(p, tint, style = st)
            }
            // 拼图：带缺口的方块
            HubGlyph.PLUGIN -> {
                val p = Path().apply {
                    moveTo(s * 0.14f, s * 0.20f)
                    lineTo(s * 0.44f, s * 0.20f)
                    lineTo(s * 0.44f, s * 0.30f)
                    cubicTo(s * 0.56f, s * 0.24f, s * 0.56f, s * 0.46f, s * 0.44f, s * 0.40f)
                    lineTo(s * 0.44f, s * 0.52f)
                    lineTo(s * 0.86f, s * 0.52f)
                    lineTo(s * 0.86f, s * 0.86f)
                    lineTo(s * 0.14f, s * 0.86f)
                    close()
                }
                drawPath(p, tint, style = st)
            }
            // 三层圆盘 = 存储
            HubGlyph.STORAGE -> {
                drawOval(
                    tint,
                    topLeft = Offset(s * 0.18f, s * 0.14f),
                    size = Size(s * 0.64f, s * 0.24f),
                    style = st
                )
                drawLine(tint, Offset(s * 0.18f, s * 0.26f), Offset(s * 0.18f, s * 0.74f), st.width)
                drawLine(tint, Offset(s * 0.82f, s * 0.26f), Offset(s * 0.82f, s * 0.74f), st.width)
                arcBottom(tint, s, st)
                drawOval(
                    tint,
                    topLeft = Offset(s * 0.18f, s * 0.62f),
                    size = Size(s * 0.64f, s * 0.24f),
                    style = st
                )
            }
            // 手机
            HubGlyph.DEVICE -> {
                drawRoundRect(
                    tint,
                    topLeft = Offset(s * 0.28f, s * 0.10f),
                    size = Size(s * 0.44f, s * 0.80f),
                    cornerRadius = CornerRadius(s * 0.12f, s * 0.12f),
                    style = st
                )
                drawLine(tint, Offset(s * 0.42f, s * 0.78f), Offset(s * 0.58f, s * 0.78f), st.width, StrokeCap.Round)
            }
        }
    }
}

private fun DrawScope.star(tint: Color, s: Float, st: Stroke) {
    val cx = s / 2f
    val cy = s / 2f
    val outer = s * 0.38f
    val inner = s * 0.13f
    val p = Path()
    for (i in 0 until 8) {
        val angle = Math.toRadians((i * 45 - 90).toDouble())
        val r = if (i % 2 == 0) outer else inner
        val x = cx + r * kotlin.math.cos(angle).toFloat()
        val y = cy + r * kotlin.math.sin(angle).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, tint, style = st)
}

private fun DrawScope.arcBottom(tint: Color, s: Float, st: Stroke) {
    val p = Path().apply {
        moveTo(s * 0.18f, s * 0.44f)
        cubicTo(s * 0.18f, s * 0.60f, s * 0.82f, s * 0.60f, s * 0.82f, s * 0.44f)
    }
    drawPath(p, tint, style = st)
}
