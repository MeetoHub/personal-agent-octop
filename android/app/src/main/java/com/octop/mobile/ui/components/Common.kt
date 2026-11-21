package com.octop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.core.model.Approval
import com.octop.mobile.core.model.RiskLevel
import com.octop.mobile.ui.theme.Accent
import com.octop.mobile.ui.theme.Amber
import com.octop.mobile.ui.theme.AssistantBubbleBorder
import com.octop.mobile.ui.theme.BorderInput
import com.octop.mobile.ui.theme.BrandLight
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.RadiusFull
import com.octop.mobile.ui.theme.Rose
import com.octop.mobile.ui.theme.AccentSoft
import com.octop.mobile.ui.theme.SkyBlue
import com.octop.mobile.ui.theme.Surface1
import com.octop.mobile.ui.theme.Surface3
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary

@Composable
fun PageHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = TextSecondary
                )
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun SectionHeader(title: String, trailing: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 对齐本体 titleMedium：15sp SemiBold
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(text = trailing, fontSize = 13.sp, color = TextTertiary)
        }
    }
}

@Composable
fun OctopCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var m = modifier
        .fillMaxWidth()
        .background(Surface1, RoundedCornerShape(20.dp))
    if (onClick != null) m = m.clickable { onClick() }
    Column(modifier = m.padding(horizontal = 16.dp, vertical = 15.dp), content = content)
}

@Composable
fun StatusDot(color: Color, size: Int = 7, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(size.dp).background(color, CircleShape))
}

@Composable
fun Tag(text: String, color: Color = Accent, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color, letterSpacing = 0.6.sp)
    }
}

@Composable
fun ThinProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Accent,
    height: Int = 4
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(Surface3, RoundedCornerShape(height.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height.dp)
                .background(color, RoundedCornerShape(height.dp))
        )
    }
}

/**
 * 输入条：对齐本体 `.inputWrapper`（chatThemeOverrides.partial.less L855-871）。
 * 手机断点圆角 20px、描边用 --fn-assistant-bubble-border、底 --fn-bg-primary；
 * 空闲是「发送箭头」，agent 回复中变成「停止方块」；点停止不清草稿。
 */
@Composable
fun Composer(
    running: Boolean,
    hint: String = "让 Octop 做点什么…",
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    prefill: String? = null,
    prefillNonce: Int = 0
) {
    var text by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val canSend = text.isNotBlank()

    // 本体 WelcomeScreen 快捷卡点击后把 prompt 预填进输入框。
    // key 用 nonce 而非字符串：同一 prompt 连点两次 nonce 递增，effect 仍会重触发。
    LaunchedEffect(prefillNonce) {
        if (!prefill.isNullOrEmpty()) text = prefill
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(20.dp))
            .border(1.dp, AssistantBubbleBorder, RoundedCornerShape(20.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(if (menu) SkyBlue else Color.Transparent, CircleShape)
                    .clickable { menu = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Light,
                    color = TextPrimary
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                listOf(
                    "今天有什么任务？",
                    "帮我看看我的记忆里有什么",
                    "最近跑过哪些定时任务？"
                ).forEach { q ->
                    DropdownMenuItem(
                        text = { Text(q, fontSize = 14.sp, color = TextPrimary) },
                        onClick = {
                            text = q
                            menu = false
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            if (text.isEmpty()) {
                Text(text = hint, fontSize = 17.sp, color = TextTertiary)
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = TextStyle(color = TextPrimary, fontSize = 17.sp, lineHeight = 22.sp),
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (canSend || running) AccentSoft else Surface3,
                    CircleShape
                )
                .clickable {
                    if (running) {
                        onStop()
                    } else if (canSend) {
                        val payload = text.trim()
                        text = ""
                        onSend(payload)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (running) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .background(TextPrimary, RoundedCornerShape(2.dp))
                )
            } else {
                Text(
                    text = "↑",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ApprovalCard(
    approval: Approval,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier
) {
    val riskColor = when (approval.risk) {
        RiskLevel.LOW -> Green
        RiskLevel.MEDIUM -> Amber
        RiskLevel.HIGH -> Rose
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(riskColor.copy(alpha = 0.07f), RoundedCornerShape(20.dp))
            .border(1.dp, riskColor.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(riskColor)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "需要你确认 · 风险${approval.risk.label}",
                style = MaterialTheme.typography.labelSmall,
                color = riskColor
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = approval.action,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = approval.reason,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        val canApprove = approval.allowed.contains("approve")
        val canReject = approval.allowed.contains("reject")
        if (canApprove || canReject) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (canApprove) PillButton("允许", filled = true, onClick = onApprove)
                if (canReject) PillButton("拒绝", filled = false, onClick = onDeny)
            }
        } else {
            // 这个暂停只接受 edit / respond 之类的文字决策，手机端做不了，别给假按钮
            Text(
                text = "这一步需要回复文字（${approval.allowed.joinToString(" / ")}），请在网页端处理",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

@Composable
fun PillButton(
    text: String,
    filled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(if (filled) BrandLight else Color.Transparent, RoundedCornerShape(RadiusFull))
            .border(
                width = if (filled) 0.dp else 1.dp,
                color = if (filled) Color.Transparent else BorderInput,
                shape = RoundedCornerShape(RadiusFull)
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary
        )
    }
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
    }
}
