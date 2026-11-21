package com.octop.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.core.model.OctopTask
import com.octop.mobile.core.model.StepStatus
import com.octop.mobile.core.model.TaskStatus
import com.octop.mobile.ui.AppViewModel
import com.octop.mobile.ui.components.ApprovalCard
import com.octop.mobile.ui.components.Composer
import com.octop.mobile.ui.components.OctopCard
import com.octop.mobile.ui.components.PillButton
import com.octop.mobile.ui.components.SectionHeader
import com.octop.mobile.ui.components.StatusDot
import com.octop.mobile.ui.components.Tag
import com.octop.mobile.ui.components.ThinProgress
import com.octop.mobile.ui.theme.Accent
import com.octop.mobile.ui.theme.Amber
import com.octop.mobile.ui.theme.Bg
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.OutlineCol
import com.octop.mobile.ui.theme.Rose
import com.octop.mobile.ui.theme.Surface1
import com.octop.mobile.ui.theme.Surface2
import com.octop.mobile.ui.theme.Surface3
import com.octop.mobile.ui.theme.TextInverse
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
@ReadOnlyComposable
private fun stepColor(s: StepStatus): Color = when (s) {
    StepStatus.DONE -> Green
    StepStatus.RUNNING -> Accent
    StepStatus.FAILED -> Rose
    StepStatus.SKIPPED -> TextTertiary
    StepStatus.PENDING -> Surface3
}

private fun clockOf(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

@Composable
fun TaskDetailScreen(vm: AppViewModel, taskId: String, onBack: () -> Unit) {
    val tasks by vm.tasks.collectAsState()
    val task = tasks.firstOrNull { it.id == taskId }
    // 这条线程的消息：loadHistory 已灌进 chatMessages，页面必须真的画出来
    val messages by vm.chatMessages.collectAsState()
    val chatBusy by vm.chatBusy.collectAsState()

    // 打开会话就把真实历史拉进来
    LaunchedEffect(taskId) { vm.loadHistory(taskId) }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        // 顶栏：返回 + 两行标题（标题 / 任务号）+ 状态，底部一条分隔线
        // 借鉴桌面 JobDetailDrawer 的抽屉头——信息分层而不是把所有东西挤在一行
        Column(modifier = Modifier.fillMaxWidth().background(Surface1)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = TextPrimary, fontSize = 26.sp)
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "对话详情",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = taskId.take(12),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
                if (task != null) {
                    Tag(
                        text = task.status.label,
                        color = when (task.status) {
                            TaskStatus.DONE -> Green
                            TaskStatus.RUNNING -> Accent
                            TaskStatus.WAITING_APPROVAL -> Amber
                            TaskStatus.FAILED -> Rose
                            else -> TextSecondary
                        }
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(OutlineCol))
        }

        if (task == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("任务不存在", color = TextTertiary)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader("任务概况")
                Spacer(Modifier.height(8.dp))
                OctopCard {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = task.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    if (task.progress > 0f) {
                        Spacer(Modifier.height(12.dp))
                        ThinProgress(progress = task.progress)
                    }
                }
            }

            if (messages.isNotEmpty()) {
                item { SectionHeader("对话记录", "${messages.size} 条") }
                items(messages, key = { "msg-${it.id}" }) { m -> Bubble(m) }
            }

            task.approval?.let { ap ->
                item { SectionHeader("需要你拍板") }
                item {
                    ApprovalCard(
                        approval = ap,
                        onApprove = { vm.approve(task.id) },
                        onDeny = { vm.deny(task.id) }
                    )
                }
            }

            if (task.steps.isNotEmpty()) {
                item { SectionHeader("执行步骤", "${task.steps.count { it.status == StepStatus.DONE }}/${task.steps.size}") }
                item {
                    OctopCard {
                        task.steps.forEachIndexed { idx, step ->
                            StepRow(
                                index = idx + 1,
                                title = step.title,
                                detail = step.detail,
                                status = step.status,
                                isLast = idx == task.steps.lastIndex
                            )
                            if (idx != task.steps.lastIndex) {
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }

            if (task.receipts.isNotEmpty()) {
                item { SectionHeader("回执", "可审计") }
                item {
                    OctopCard {
                        task.receipts.reversed().forEachIndexed { i, r ->
                            if (i > 0) {
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(OutlineCol)
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusDot(Accent, size = 6)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = r.tool,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Accent
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = clockOf(r.atMillis),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = r.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(OutlineCol))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (task.status) {
                        TaskStatus.RUNNING -> PillButton("暂停", false) { vm.pause(task.id) }
                        TaskStatus.PAUSED -> PillButton("继续", true) { vm.resume(task.id) }
                        TaskStatus.FAILED -> PillButton("重试", true) { vm.retry(task.id) }
                        else -> Unit
                    }
                }
            }
        }

        // 底部追问
        Composer(
            running = chatBusy,
            hint = "就这件事追问 Octop…",
            onSend = { vm.sendMessage(it) },
            onStop = { vm.stopTurn() },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun StepRow(
    index: Int,
    title: String,
    detail: String?,
    status: StepStatus,
    isLast: Boolean
) {
    val c = stepColor(status)
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        if (status == StepStatus.DONE || status == StepStatus.RUNNING) c
                        else Color.Transparent,
                        CircleShape
                    )
                    .border(1.dp, if (status == StepStatus.PENDING) OutlineCol else c, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val mark = when (status) {
                    StepStatus.DONE -> "✓"
                    StepStatus.FAILED -> "!"
                    StepStatus.SKIPPED -> "–"
                    else -> ""
                }
                Text(
                    text = mark,
                    color = if (status == StepStatus.DONE) TextInverse else c,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(Surface3)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (status == StepStatus.PENDING) TextTertiary else TextPrimary
            )
            if (detail != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = c
        )
    }
}
