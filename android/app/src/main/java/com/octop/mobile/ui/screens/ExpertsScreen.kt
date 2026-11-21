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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.core.model.Expert
import com.octop.mobile.ui.AppViewModel
import com.octop.mobile.ui.components.EmptyHint
import com.octop.mobile.ui.components.ExpertAvatar
import com.octop.mobile.ui.components.PageHeader
import com.octop.mobile.ui.components.PillButton
import com.octop.mobile.ui.components.StatusDot
import com.octop.mobile.ui.components.Tag
import com.octop.mobile.ui.theme.Bg
import com.octop.mobile.ui.theme.BgPrimary
import com.octop.mobile.ui.theme.BorderPrimary
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary

/**
 * 专家列表。
 *
 * 数据是真接口（GET /api/agents）。卡片只留三样能帮用户做决定的信息：
 * 它是谁（名字/角色）、它现在在不在（在线点）、它能动什么（工具数 + 前三个工具）。
 * 工具原来一屏铺十几个标签，反而看不出重点，现在收成 3 个 + 余数。
 */
@Composable
fun ExpertsScreen(vm: AppViewModel, onOpen: (String) -> Unit = {}) {
    val experts by vm.experts.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageHeader(
                title = "专家",
                subtitle = if (experts.isEmpty()) "每个专家 = 一套人格 + 工具白名单"
                else "${experts.size} 位 · 点开看它用什么模型、记得什么",
                trailing = { PillButton("刷新", filled = false) { vm.loadWorkspace() } }
            )
        }

        if (experts.isEmpty()) {
            item { EmptyHint("没取到专家。点右上角刷新试试。") }
        }

        items(experts, key = { it.id }) { e ->
            ExpertCard(e) { onOpen(e.id) }
        }
    }
}

@Composable
private fun ExpertCard(e: Expert, onClick: () -> Unit) {
    val tint = Color(e.color)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgPrimary)
            .border(1.dp, BorderPrimary, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            // 顶部 2px 强调条：对应 .agentCard2Accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(tint)
            )

            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 头像：复刻本体 AgentCard.tsx:337-346 —— 42dp 容器、10dp 圆角、
                    // accent 10% 底，内容按 icon_url > icon_name > layers 依次回退。
                    ExpertAvatar(
                        iconUrl = e.iconUrl,
                        iconName = e.iconName,
                        accent = tint,
                        slot = 42.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = e.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = e.role.ifBlank { "专家" },
                            fontSize = 12.sp,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // 状态胶囊：对应 .agentCard2State / .stateDot2
                        Spacer(Modifier.height(5.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    (if (e.online) Green else TextTertiary).copy(alpha = 0.12f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            StatusDot(if (e.online) Green else TextTertiary, size = 6)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (e.online) "在线" else "离线",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (e.online) Green else TextTertiary
                            )
                        }
                    }
                }

                if (e.description.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = e.description,
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (e.enabledTools.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        e.enabledTools.take(3).forEach { t ->
                            Tag(text = t, color = tint)
                        }
                        val rest = e.enabledTools.size - 3
                        if (rest > 0) {
                            Text(
                                text = "+$rest",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextTertiary,
                                modifier = Modifier.padding(start = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
