package com.octop.mobile.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.core.auth.TokenStore
import com.octop.mobile.core.model.Expert
import com.octop.mobile.core.system.BridgeManager
import com.octop.mobile.core.system.SystemBridge
import com.octop.mobile.core.model.LibraryItem
import com.octop.mobile.core.model.LibraryKind
import com.octop.mobile.core.model.OctopTask
import com.octop.mobile.core.model.ScheduleItem
import com.octop.mobile.core.model.TaskStatus
import com.octop.mobile.core.sync.LinkState
import com.octop.mobile.ui.AppViewModel
import com.octop.mobile.ui.components.EmptyHint
import com.octop.mobile.ui.components.PillButton
import com.octop.mobile.ui.components.ThinProgress
import com.octop.mobile.ui.theme.Accent
import com.octop.mobile.ui.theme.Amber
import com.octop.mobile.ui.theme.Bg
import com.octop.mobile.ui.theme.Brand
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.RadiusLg
import com.octop.mobile.ui.theme.RadiusMd
import com.octop.mobile.ui.theme.Rose
import com.octop.mobile.ui.theme.SoftOrange
import com.octop.mobile.ui.theme.Surface1
import com.octop.mobile.ui.theme.Surface2
import com.octop.mobile.ui.theme.Surface3
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary

/** 相对时间。列表里显示时间戳比显示 UUID 有用。 */
private fun ago(millis: Long, now: Long = System.currentTimeMillis()): String {
    val d = (now - millis) / 1000L
    return when {
        d < 60L -> "刚刚"
        d < 3600L -> "${d / 60} 分钟前"
        d < 86400L -> "${d / 3600} 小时前"
        d < 172800L -> "昨天"
        d < 2592000L -> "${d / 86400} 天前"
        else -> "${d / 2592000} 个月前"
    }
}

@Composable
@ReadOnlyComposable
private fun statusColor(s: TaskStatus): Color = when (s) {
    TaskStatus.DONE -> Green
    TaskStatus.RUNNING -> Accent
    TaskStatus.WAITING_APPROVAL -> Amber
    TaskStatus.FAILED -> Rose
    TaskStatus.PAUSED -> TextTertiary
    else -> TextSecondary
}

/* ───────────────────────── 任务 ─────────────────────────
 * 布局参照本体 dashboard/src/pages/Chat：
 *  - chatSidebar.partial.less L87-99：会话头「标题 15/700 品牌色 + 说明」两段式，留白 16/16/12。
 *  - chatSidebar.partial.less L133-160：会话行 10/12 padding + radius-md + active 浅底/品牌文字。
 *  - HitlApprovalCard.module.less L1-53：语义色卡 radius-lg + 16% 图标底 + 7% 卡底 + 标题/说明分层。
 *  - theme-vars.css L164-205：radius-lg=12px、spacing 3/4(12/16px)、warning/success/danger 语义。
 * 这里只重排结构与样式，ago()/statusColor()/回调签名/vm 用法全部原样。
 */

@Composable
private fun SectionTitle(title: String, count: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun HeroIcon(tint: Color, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val s = size.toPx()
        val st = Stroke(width = s * 0.1125f, cap = StrokeCap.Round) // HubIcons.kt 同款 11.25% 线宽
        // 对话气泡：圆角矩形 + 尾巴 + 两点，对齐本体 sidebar 的 MessageSquare 语义
        drawRoundRect(
            color = tint,
            topLeft = Offset(s * 0.12f, s * 0.16f),
            size = Size(s * 0.76f, s * 0.56f),
            cornerRadius = CornerRadius(s * 0.16f, s * 0.16f),
            style = st
        )
        drawLine(tint, Offset(s * 0.32f, s * 0.70f), Offset(s * 0.28f, s * 0.88f), st.width, StrokeCap.Round)
        drawCircle(tint, radius = s * 0.04f, center = Offset(s * 0.38f, s * 0.44f))
        drawCircle(tint, radius = s * 0.04f, center = Offset(s * 0.62f, s * 0.44f))
    }
}

@Composable
fun TasksScreen(vm: AppViewModel, onOpen: (String) -> Unit, onOpenChat: () -> Unit) {
    val tasks by vm.tasks.collectAsState()
    val waiting = tasks.filter { it.status == TaskStatus.WAITING_APPROVAL }

    LazyColumn(
        // 本体 .sessionHeader: padding 16px 16px 12px；.sessionItem 左右内距 12px，
        // 所以外层只留 4dp，让行内容起点落在 16dp，和上游对齐。
        modifier = Modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp) // .sessionItem 无间距，靠行内距分隔
    ) {
        // ── 头部：复刻本体 .sessionHeader（15px/700 品牌色标题 + 说明 + 32px 加号按钮）。
        //    上游这里是扁平的列表头，不是卡片，所以去掉浅品牌底和描边。
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "对话",
                        fontSize = 15.sp,               // .sessionTitle: font-size 15px
                        fontWeight = FontWeight.Bold,   // .sessionTitle: font-weight 700
                        color = Brand
                    )
                    Spacer(Modifier.weight(1f))
                    // .sessionAddBtn: 32x32, radius-md
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Accent.copy(alpha = 0.10f), RoundedCornerShape(RadiusMd))
                            .clickable { onOpenChat() },
                        contentAlignment = Alignment.Center
                    ) {
                        HeroIcon(tint = Accent, size = 17.dp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (waiting.isEmpty()) "Octop 在替你办事" else "${waiting.size} 件事等你点头",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // ── 等待确认：本体 HitlApprovalCard 风格，语义色 amber/warning。
        if (waiting.isNotEmpty()) {
            item { SectionTitle("等你确认") }
            items(waiting, key = { "w-${it.id}" }) { task ->
                val ap = task.approval
                if (ap != null) {
                    WaitingApprovalCard(
                        approval = ap,
                        onApprove = { vm.approve(task.id) },
                        onDeny = { vm.deny(task.id) }
                    )
                } else {
                    // 真实数据的审批详情只在 history 里，列表页先给个能点进去的入口
                    TaskRow(task = task, onOpen = { onOpen(task.id) })
                }
            }
        }

        item { SectionTitle("全部任务", "${tasks.size}") }
        if (tasks.isEmpty()) {
            item { EmptyHint("还没有会话。点上方加号开第一句。") }
        }
        items(tasks, key = { it.id }) { task ->
            TaskRow(
                task = task,
                onOpen = { onOpen(task.id) },
                onDelete = { vm.deleteThread(task.id) }
            )
        }
    }
}

/**
 * 等待确认卡：对齐本体 HitlApprovalCard.module.less 与 theme-vars.css 语义色。
 * 与 Common.kt ApprovalCard 的差别：圆角走 radius-lg(12) 而非写死 20；
 * 等待态固定用 warning/amber 语义（Low/Medium/High 风险只作为摘要信息展示，不改语义色）；
 * 主操作（允许）实心、次操作（拒绝）描边，按钮层级清晰。
 * 回调签名与业务判断（allowed）原样保留。
 */
@Composable
private fun WaitingApprovalCard(
    approval: com.octop.mobile.core.model.Approval,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    val shape = RoundedCornerShape(RadiusLg)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SoftOrange, shape)                    // warning 7% 语义底
            .border(1.dp, Amber.copy(alpha = 0.42f), shape)   // warning 42% 描边
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Amber.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Amber
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "等你确认",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "风险${approval.risk.label} · 需要你点头",
                    style = MaterialTheme.typography.labelSmall,
                    color = Amber
                )
            }
        }
        Spacer(Modifier.height(12.dp))
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canApprove) {
                    PillButton(
                        text = "允许",
                        filled = true,
                        modifier = Modifier.weight(1f),
                        onClick = onApprove
                    )
                }
                if (canReject) {
                    PillButton(
                        text = "拒绝",
                        filled = false,
                        modifier = Modifier.weight(1f),
                        onClick = onDeny
                    )
                }
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
private fun TaskRow(task: OctopTask, onOpen: () -> Unit, onDelete: (() -> Unit)? = null) {
    val c = statusColor(task.status)
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(RadiusMd) // 本体 .sessionItem: --fn-radius-md = 8px
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 本体 .sessionItem 是「无描边、无阴影」的扁平行，靠 hover 底色区分；
            // 手机端没有 hover，用极淡的填充模拟同样的视觉层级，不再套 1px 边框卡片。
            .background(Surface1, shape)
            .clickable { onOpen() }
            .padding(horizontal = 12.dp, vertical = 10.dp), // .sessionItem: padding 10px 12px
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp) // .sessionItem: gap 10px
    ) {
        // 会话图标位：本体 .sessionIcon 是 16px 的 MessageSquare，opacity .6
        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
            ConversationIcon(tint = TextTertiary, size = 16.dp)
        }
        // 名称 + 元信息：本体只有单行 13px 名称，手机端把「状态 / 时间」压成同一行右侧，
        // 不新增视觉层级，保持一行的紧凑感。
        Text(
            text = task.title,
            fontSize = 13.sp,               // .sessionName: font-size 13px
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (task.status == TaskStatus.RUNNING) {
            // 运行中才占用一格，静态行保持和上游一样干净
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(c, CircleShape)
            )
            Spacer(Modifier.width(2.dp))
        }
        Text(
            text = ago(task.updatedAtMillis),
            fontSize = 11.sp,
            color = TextTertiary,
            maxLines = 1
        )
        if (onDelete != null) {
            Box {
                Text(
                    text = "⋯",
                    fontSize = 15.sp,
                    color = TextTertiary,
                    modifier = Modifier
                        .clickable { menu = true }
                        .padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("删除这个会话", fontSize = 13.sp) },
                        onClick = {
                            menu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/** 会话行图标：本体 sidebar 用的 MessageSquare（圆角矩形 + 左下尾巴）。 */
@Composable
private fun ConversationIcon(tint: Color, size: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.minDimension
        val r = s * 0.16f
        val stroke = Stroke(width = s * 0.083f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            // 气泡主体（上边略宽、下边收，右下角带尾巴）
            moveTo(r, 0f)
            lineTo(s - r, 0f)
            quadraticTo(s, 0f, s, r)
            lineTo(s, s * 0.78f)
            quadraticTo(s, s * 0.78f + r, s - r, s * 0.78f + r)
            lineTo(s * 0.34f, s * 0.78f + r)
            lineTo(s * 0.14f, s)
            lineTo(s * 0.14f, s * 0.78f + r)
            lineTo(r, s * 0.78f + r)
            quadraticTo(0f, s * 0.78f + r, 0f, s * 0.78f)
            lineTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            close()
        }
        drawPath(path, tint.copy(alpha = 0.6f), style = stroke) // .sessionIcon: opacity .6
    }
}
