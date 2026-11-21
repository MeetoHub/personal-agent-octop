package com.octop.mobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.octop.mobile.R
import com.octop.mobile.core.chat.SocketState
import com.octop.mobile.core.model.Expert
import com.octop.mobile.ui.AppViewModel
import com.octop.mobile.ui.CtxUsage
import com.octop.mobile.ui.TraceMetrics
import com.octop.mobile.ui.TraceStep
import com.octop.mobile.ui.components.Composer
import com.octop.mobile.ui.components.ExpertAvatar
import com.octop.mobile.ui.theme.AccentSoft
import com.octop.mobile.ui.theme.Amber
import com.octop.mobile.ui.theme.AssistantBubbleBg
import com.octop.mobile.ui.theme.AssistantBubbleBorder
import com.octop.mobile.ui.theme.Bg
import com.octop.mobile.ui.theme.BgPrimary
import com.octop.mobile.ui.theme.BgTertiary
import com.octop.mobile.ui.theme.Brand
import com.octop.mobile.ui.theme.BrandLight
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.LocalOctopColors
import com.octop.mobile.ui.theme.RadiusXl
import com.octop.mobile.ui.theme.Rose
import com.octop.mobile.ui.theme.Surface3
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import kotlinx.coroutines.launch

/** 头像固定槽位，本体手机断点 36px（chatMessages.partial.less botAvatar/userAvatar）。 */
private val AvatarSlot = 36.dp

/** 本体欢迎快捷卡（chatWelcome zh.json quickPrompt1~6），无专家数据时的默认卡。 */
private data class WelcomeCard(val glyph: String, val title: String, val desc: String, val prompt: String)

// desc 是两列卡片上的副标题，半宽下只能放约 6 个字：超过就会被省略号截成半句，
// 反而比没有更糟。所以这里一律压到 6 字以内，完整意思靠 prompt（点下去真正发出去的内容）承载。
private val WelcomeCards = listOf(
    WelcomeCard("文", "总结长文", "提炼核心要点", "请帮我总结以下内容，提炼出核心要点和结论：\n\n"),
    WelcomeCard("邮", "写一封邮件", "起草正式邮件", "请帮我写一封邮件。收件人：，目的：，语气要求：正式/友好。"),
    WelcomeCard("码", "解释代码", "逐行解读逻辑", "请逐步解释以下代码的逻辑和功能：\n\n"),
    WelcomeCard("划", "制定计划", "拆成任务清单", "请帮我制定一个详细的执行计划，目标是："),
    WelcomeCard("译", "翻译润色", "中英互译改写", "请将以下内容翻译成英文，并确保表达自然流畅：\n\n"),
    WelcomeCard("想", "头脑风暴", "发散创意思路", "请围绕以下主题进行头脑风暴，给出 5～8 个有创意的方向或想法："),
)

@Composable
fun ChatScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onTapAgent: () -> Unit = {},
    showBack: Boolean = true
) {
    val messages by vm.chatMessages.collectAsState()
    val busy by vm.chatBusy.collectAsState()
    val socket by vm.socketState.collectAsState()
    val listState = rememberLazyListState()
    val usage by vm.ctxUsage.collectAsState()
    val hasThread by vm.hasThread.collectAsState()
    val trace by vm.trajectory.collectAsState()
    val traceMetrics by vm.traceMetrics.collectAsState()
    val agents by vm.experts.collectAsState()
    var showTrace by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // 本体 WelcomeScreen 快捷卡点击后预填输入框（Composer.prefill）。
    var prefill by remember { mutableStateOf<String?>(null) }
    var prefillNonce by remember { mutableStateOf(0) }
    val onQuickCardClick: (String) -> Unit = { prompt ->
        prefill = prompt
        prefillNonce++
    }
    // 本体 scrollToBottomBtn：用户上滑离开底部一段距离后显示，点击回到最新。
    val showScrollBottom by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = layout.totalItemsCount
            total > 1 && lastVisible < total - 1
        }
    }

    // 每轮结束顺手看一下窗口吃满了没；失败就算了，不该打扰用户
    LaunchedEffect(messages.size, busy) { vm.refreshContextUsage() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val statusText = when (socket) {
        SocketState.OPEN -> "已连上本体"
        SocketState.CONNECTING -> "连接中…"
        SocketState.FAILED -> "断线，正在重试"
        SocketState.CLOSED -> "已断开"
        SocketState.IDLE -> "未连接"
    }
    val statusColor = when (socket) {
        SocketState.OPEN -> Green
        SocketState.CONNECTING -> Amber
        SocketState.FAILED -> Rose
        else -> TextTertiary
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Bg)) {
            val activeAgent = agents.firstOrNull { it.id == vm.activeAgentId }
            ChatTopBar(
                agent = activeAgent,
                subtitle = statusText,
                subtitleColor = statusColor,
                onBack = onBack,
                onNewChat = { vm.newChat() },
                onTapAgent = onTapAgent,
                showBack = showBack
            )

            usage?.let { u ->
                UsageStrip(
                    usage = u,
                    showTraceEntry = hasThread,
                    onOpenTrace = {
                        vm.refreshTrajectory()
                        showTrace = true
                    }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty()) {
                    // 空会话不走 LazyColumn：欢迎区高度固定且常常不满一屏，塞进 LazyColumn
                    // 会死死贴顶，把省下的空间全甩到输入框上方，形成「上面满、下面空一大截」。
                    //
                    // 这里用 verticalScroll + Arrangement.Center：内容不满时居中，内容超高时
                    // （大字体/小屏）可以滚，而不是被 Box 上下同时裁掉。
                    // 注意这个 verticalScroll 是挂在 Box 下的 Column 上，不是挂在 LazyColumn 的
                    // item 里——之前闪退的成因是「可滚动组件作为 Lazy item 拿到无限高约束」，
                    // 这里父级是 fillMaxSize 的有界 Box，不会触发。
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WelcomeView(
                            agentName = agents.firstOrNull { it.id == vm.activeAgentId }?.name,
                            onPromptClick = onQuickCardClick
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        // 横向留白交给 MessageRow 的 14dp，这里不再叠加（本体 padding-inline:14px）。
                        contentPadding = PaddingValues(top = 15.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        items(messages, key = { it.id }) { m -> Bubble(m) }
                    }
                }

                // 本体 scrollToBottomBtn（chatMessages.partial.less L53）：底部 20dp 居中、
                // pill、半透明 elevated 底、1dp 边框、12sp/500、带阴影。
                if (showScrollBottom) {
                    ScrollToBottomButton(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                        onClick = {
                            if (messages.isNotEmpty()) {
                                scope.launch { listState.animateScrollToItem(messages.lastIndex) }
                            }
                        }
                    )
                }
            }

            // 键盘 inset 的唯一 owner 是 OctopRoot 的 Scaffold（safeDrawing + 80px 阈值），
            // 这里再加 imePadding/navigationBarsPadding 会变成双份留白。
            Composer(
                running = busy,
                onSend = { prefill = null; vm.sendMessage(it) },
                onStop = { vm.stopTurn() },
                prefill = prefill,
                prefillNonce = prefillNonce,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp)
            )
        }

        if (showTrace) {
            TraceOverlay(steps = trace, metrics = traceMetrics, onClose = { showTrace = false })
        }
    }
}

/** 用量条：细到只有 3dp，但它回答「还能聊多久」这个问题。 */
@Composable
private fun UsageStrip(usage: CtxUsage, showTraceEntry: Boolean, onOpenTrace: () -> Unit) {
    val color = when {
        usage.ratio >= 0.9f -> Rose
        usage.ratio >= 0.7f -> Amber
        else -> TextTertiary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 12.dp, top = 2.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .background(Surface3, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(usage.ratio.coerceAtLeast(0.02f))
                    .height(3.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(
            text = "${fmtTokens(usage.used)} / ${fmtTokens(usage.limit)}",
            fontSize = 10.sp,
            color = TextTertiary
        )
        if (showTraceEntry) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = "轨迹",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier
                    .clickable { onOpenTrace() }
                    .padding(vertical = 3.dp, horizontal = 2.dp)
            )
        }
    }
}

@Composable
private fun TraceOverlay(steps: List<TraceStep>, metrics: TraceMetrics, onClose: () -> Unit) {
    // 遮罩与面板都用语义令牌：暗色下遮罩更重，面板跟随 bgPrimary。
    val scrim = Color.Black.copy(alpha = if (LocalOctopColors.current.isDark) 0.55f else 0.33f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrim)
            .clickable { onClose() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    BgPrimary,
                    RoundedCornerShape(topStart = RadiusXl, topEnd = RadiusXl)
                )
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "轨迹",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "关闭",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .clickable { onClose() }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                )
            }
            if (metrics.hasData) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = buildString {
                        append("${metrics.steps} 步")
                        if (metrics.turns > 0) append(" · ${metrics.turns} 轮")
                        if (metrics.llmMs > 0) append(" · 模型 ${metrics.llmMs / 1000.0}s".let { " · 模型 %.1fs".format(metrics.llmMs / 1000.0) })
                        if (metrics.toolMs > 0) append(" · 工具 %.1fs".format(metrics.toolMs / 1000.0))
                        if (metrics.ttftMs > 0) append(" · 首字 %.1fs".format(metrics.ttftMs / 1000.0))
                    },
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
            Spacer(Modifier.height(10.dp))
            if (steps.isEmpty()) {
                Text(
                    text = "这轮还没有轨迹：要么还没开始跑，要么接口没返回。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = TextTertiary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 430.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(steps) { st ->
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(if (st.isError) Rose else AccentSoft, CircleShape)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = st.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (st.isError) Rose else TextPrimary,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (st.kind.isNotBlank()) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(text = st.kind, fontSize = 10.sp, color = TextTertiary)
                                }
                                val clock = clockOf(st.ts)
                                if (clock.isNotEmpty()) {
                                    Spacer(Modifier.weight(1f))
                                    Text(text = clock, fontSize = 10.sp, color = TextTertiary)
                                }
                            }
                            if (st.detail.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = st.detail,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = if (st.isError) Rose.copy(alpha = 0.85f) else TextSecondary,
                                    maxLines = 4
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun fmtTokens(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1fk".format(n / 1_000.0)
    else -> "$n"
}

/**
 * 顶栏：左侧返回，中间专家头像 + 名称 + 状态行，右侧「新对话」。
 * 名称 16sp/600、状态 11sp，点击头像或名称进专家页。
 */
@Composable
private fun ChatTopBar(
    agent: Expert?,
    subtitle: String,
    subtitleColor: Color,
    onBack: () -> Unit,
    onNewChat: () -> Unit,
    onTapAgent: () -> Unit,
    showBack: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            // 不画返回箭头时（首页），头像直接贴 padding 左边界会太靠边，
            // 这里补到 14dp；有箭头时箭头自带 44dp 点击区，4dp 就够。
            .padding(start = if (showBack) 4.dp else 14.dp, end = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(44.dp)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Light)
                }
            }

            ExpertAvatar(
                iconUrl = agent?.iconUrl,
                iconName = agent?.iconName,
                accent = agent?.let { Color(it.color) } ?: Brand,
                slot = 36.dp,
                modifier = Modifier.clickable { onTapAgent() }
            )
            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTapAgent() },
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = agent?.name ?: "Octop",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(subtitleColor, CircleShape)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = "新对话",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier
                    .clickable { onNewChat() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * 本体 WelcomeScreen 的手机端精确复刻（dashboard/src/pages/Chat/components/WelcomeScreen.tsx
 * + chatWelcome.partial.less）。结构：吉祥物 → 标题 → 副标题（含 @agent 高亮）→ 快速开始卡片网格。
 */
@Composable
private fun WelcomeView(agentName: String?, onPromptClick: (String) -> Unit) {
    val c = LocalOctopColors.current
    var mascotTapped by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 不能在这里加 verticalScroll：本组件是作为 LazyColumn 的 item 渲染的，
            // 再套一层可滚动容器会拿到无限高约束并直接崩溃。滚动统一交给外层 LazyColumn。
            // vertical 收到 14dp：吉祥物 PNG 自带大片透明边距，再加 24dp 会在标题上方
            // 堆出近 90dp 空白，首屏重心被拉到「启动引导页」而不是对话。12dp 保持左右卡片对齐。
            .padding(horizontal = 12.dp).padding(top = 14.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 吉祥物：本体写的是 110dp 宽，但这张 PNG 自带大量透明边距，110×100 的框里
        // 实际墨迹只有约 60dp。容器取 115×105（墨迹约 68dp）——再小就压不住标题、
        // 看着像一枚贴纸；再大又会把首屏重心拉回「启动引导页」。
        Image(
            painter = painterResource(id = R.drawable.octop_mascot),
            contentDescription = "Octop 吉祥物",
            modifier = Modifier
                .width(115.dp)
                .height(105.dp)
                .offset(y = 8.dp)
                .graphicsLayer {
                    val s = if (mascotTapped) 0.96f else 1f
                    scaleX = s; scaleY = s
                }
                .clickable { mascotTapped = !mascotTapped }
        )
        // 标题 20sp/600/1.4（本体写 23sp，但 23sp 粗体在手机上成了全页最重的元素，
        // 而它不承载任何功能，反而把真正有用的卡片压成次要信息。降到 20sp 把主次调回来）。
        // maxLines=2 是给系统字体放大留的余量。
        Text(
            text = "嗨！你专属的智能伙伴来啦～",
            fontSize = 20.sp,
            lineHeight = (20 * 1.4f).sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        // 副标题 13sp/1.55，@agent 用品牌色衬线强调（本体 welcomeAgentMention）
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (agentName != null) {
                Text(
                    text = "@$agentName",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.brand
                )
            }
            Text(
                text = if (agentName != null) "无论工作还是生活，说出你的想法"
                       else "无论工作还是生活，说出你的想法，我来搞定",
                fontSize = 13.sp,
                lineHeight = (13 * 1.55f).sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        // 快速开始区：marginTop 14dp（移动断点）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "快速开始",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                letterSpacing = 0.06.em,
                modifier = Modifier.padding(start = 2.dp)
            )
            // 卡片网格：两列，奇数个时末行补等宽空位，避免最后一张被拉伸成整行。
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WelcomeCards.chunked(2).forEach { rowCards ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCards.forEach { card ->
                            Box(Modifier.weight(1f)) {
                                WelcomeQuickCard(card = card, onClick = { onPromptClick(card.prompt) })
                            }
                        }
                        // 奇数个时补一个等宽空位，避免最后一张卡被拉伸成整行
                        if (rowCards.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/** 本体 quickCard：row + 36dp 圆角图标 + 标题/描述，padding12/14、圆角12、1dp 边框。 */
@Composable
private fun WelcomeQuickCard(card: WelcomeCard, onClick: () -> Unit) {
    val c = LocalOctopColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.bgPrimary)
            .border(1.dp, c.borderPrimary, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(c.brandLight),
            contentAlignment = Alignment.Center
        ) {
            Text(text = card.glyph, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.brand)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = card.title,
                fontSize = 13.sp,
                lineHeight = (13 * 1.3f).sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = card.desc,
                fontSize = 11.5.sp,
                lineHeight = (11.5f * 1.45f).sp,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 气泡栅格照抄本体手机断点（chatMessages.partial.less L898-958）：
 * 左侧固定 44dp 头像槽 + 8dp 沟槽 + 文字轨道；用户气泡镜像，头像在右。
 */
@Composable
private fun MessageRow(isAssistant: Boolean, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = if (isAssistant) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        if (isAssistant) {
            AgentAvatar()
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = if (isAssistant) Modifier.weight(1f) else Modifier.widthIn(max = 300.dp)
        ) {
            content()
        }
        if (!isAssistant) {
            Spacer(Modifier.width(8.dp))
            UserAvatar()
        }
    }
}

/** 助手头像占位：品牌浅底 + 品牌字，没有图片资源。 */
@Composable
private fun AgentAvatar() {
    Box(
        modifier = Modifier
            .size(AvatarSlot)
            .background(BrandLight, CircleShape)
            .border(1.dp, AssistantBubbleBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "O", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Brand)
    }
}

/** 用户头像占位：中性底 + 次级字。 */
@Composable
private fun UserAvatar() {
    Box(
        modifier = Modifier
            .size(AvatarSlot)
            .background(BgTertiary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "我", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
    }
}

/**
 * 气泡对齐本体内核：助手磨砂渐变卡片（AssistantBubbleBg + AssistantBubbleBorder，
 * 16sp/1.7）；用户中性灰底 + 右下收角（16/16/6/16，15sp/1.55）。
 */
@Composable
internal fun Bubble(m: AppViewModel.ChatMessage) {
    val isUser = m.fromUser
    MessageRow(isAssistant = !isUser) {
        if (isUser) {
            Text(
                text = m.text,
                modifier = Modifier
                    .background(
                        BgTertiary,
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomEnd = 6.dp,
                            bottomStart = 16.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                fontSize = 15.sp,
                lineHeight = 23.sp,
                color = TextPrimary
            )
        } else {
            // 本体三层：底 AssistantBubbleBg + 135° 品牌浅色渐变 + 1dp AssistantBubbleBorder。
            // 渐变两端用 BrandLight、中间透出底色，明暗两套色板各自都对。
            val shape = RoundedCornerShape(14.dp)
            Text(
                text = m.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AssistantBubbleBg, shape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(BrandLight, Color.Transparent, BrandLight)
                        ),
                        shape = shape
                    )
                    .border(1.dp, AssistantBubbleBorder, shape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                fontSize = 16.sp,
                lineHeight = 27.sp,
                color = TextPrimary
            )
        }
    }
}

/** 本体 scrollToBottomBtn 的 Compose 复刻：半透明 pill + 1dp 边框 + 轻阴影。 */
@Composable
private fun ScrollToBottomButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = LocalOctopColors.current
    val pill = RoundedCornerShape(9999.dp)
    val bg = if (c.isDark) c.bgElevated.copy(alpha = 0.9f) else c.bgElevated.copy(alpha = 0.88f)
    Row(
        modifier = modifier
            .shadow(8.dp, pill)
            .clip(pill)
            .background(bg)
            .border(1.dp, c.borderSecondary, pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("↓", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.textSecondary)
        Text("回到最新", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = c.textSecondary)
    }
}

private fun clockOf(ts: Double): String {
    if (ts <= 0.0) return ""
    val millis = if (ts > 1e11) ts.toLong() else (ts * 1000).toLong()
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    return "%02d:%02d:%02d".format(
        cal.get(java.util.Calendar.HOUR_OF_DAY),
        cal.get(java.util.Calendar.MINUTE),
        cal.get(java.util.Calendar.SECOND)
    )
}
