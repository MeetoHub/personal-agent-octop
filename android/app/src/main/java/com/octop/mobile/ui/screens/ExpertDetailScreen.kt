package com.octop.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.core.model.ExpertDetail
import com.octop.mobile.core.model.LibraryItem
import com.octop.mobile.core.model.MemoryStat
import com.octop.mobile.ui.AppViewModel
import com.octop.mobile.ui.components.EmptyHint
import com.octop.mobile.ui.components.OctopCard
import com.octop.mobile.ui.components.StatusDot
import com.octop.mobile.ui.components.Tag
import com.octop.mobile.ui.theme.Accent
import com.octop.mobile.ui.theme.Bg
import com.octop.mobile.ui.theme.BgPrimary
import com.octop.mobile.ui.theme.BorderPrimary
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.Rose
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary

/**
 * 专家详情。
 * 一个专家 = 人格 + 模型 + 记忆 + 挂载的知识库/技能/工具 + 通道 + 定时。
 * 这些数据分散在 4 个接口里，ViewModel 合并后这里一次画完。
 */
@Composable
fun ExpertDetailScreen(vm: AppViewModel, agentId: String, onBack: () -> Unit) {
    val detail by vm.expertDetail.collectAsState()
    val library by vm.library.collectAsState()

    LaunchedEffect(agentId) { vm.loadExpertDetail(agentId) }

    val d = detail

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary
                )
            }
            Text(
                text = "专家",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }

        if (d == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyHint("加载中…")
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { HeadCard(d) }
            item { MemoryCard(d.memory) }
            item { DocCard("强记忆 · MEMORY.md", "还没有 MEMORY.md。", d.memoryDoc) }
            item { DocCard("用户画像 · USER.md", "还没有 USER.md。", d.userDoc) }
            item { CapabilityCard(d, library) }
            item { ChannelCard(d) }
            item { IntroCard(d) }
        }
    }
}

/* ── 头部：身份 + 状态 + 人格 + 模型 ─────────────────── */

@Composable
private fun DashboardCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgPrimary)
            .border(1.dp, BorderPrimary, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content
    )
}

@Composable
private fun DetailSectionTitle(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextTertiary,
            letterSpacing = 0.6.sp
        )
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(
                text = trailing,
                fontSize = 11.sp,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun HeadCard(d: ExpertDetail) {
    DashboardCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Accent.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = d.name.take(1),
                    style = MaterialTheme.typography.titleLarge,
                    color = Accent
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = d.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val running = d.state == "running"
                    StatusDot(if (running) Green else TextTertiary, size = 7)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (running) "运行中" else d.state.ifBlank { "未知状态" },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (running) Green else TextTertiary
                    )
                    if (d.visibility.isNotBlank()) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = d.visibility,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
            }
        }

        if (d.description.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = d.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (d.persona.isNotBlank()) Tag("人格 ${d.persona}", Accent)
            Tag("${d.tools.size} 个工具", TextTertiary)
        }

        if (d.model.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "模型 · ${d.model}",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }
    }
}

/* ── 记忆账本 ───────────────────────────────────────── */

@Composable
private fun MemoryCard(m: MemoryStat) {
    Column {
        DetailSectionTitle(
            title = "记忆",
            trailing = if (m.atomsDelta7d > 0) "近 7 天 +${m.atomsDelta7d}" else null
        )
        Spacer(Modifier.height(8.dp))
        OctopCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("原子", m.atoms, Modifier.weight(1f))
                StatCell("实体", m.entities, Modifier.weight(1f))
                StatCell("片段", m.episodes, Modifier.weight(1f))
                StatCell("原始", m.rawEvents, Modifier.weight(1f))
            }
            if (m.pending > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${m.pending} 条候选记忆等你确认",
                    style = MaterialTheme.typography.labelSmall,
                    color = Rose
                )
            }
            if (m.total == 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "还没有沉淀记忆。多聊几句、让它记住事，这里就会有数。",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
    }
}

/* ── 能力：知识库 / 技能包 / MCP / 工具 ─────────────── */

@Composable
private fun CapabilityCard(d: ExpertDetail, library: List<LibraryItem>) {
    Column {
        DetailSectionTitle(title = "能力")
        Spacer(Modifier.height(8.dp))
        OctopCard {
            // 知识库
            CapLine(
                label = "知识库",
                value = if (d.knowledgeBaseIds.isEmpty()) "未挂载"
                else "${d.knowledgeBaseIds.size} 个"
            )
            if (d.knowledgeBaseIds.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TagRow(
                    items = d.knowledgeBaseIds.map { id ->
                        library.firstOrNull { it.id == id }?.title ?: id
                    },
                    color = Accent
                )
            }

            Spacer(Modifier.height(14.dp))

            // 技能包
            CapLine(
                label = "技能包",
                value = if (d.skillPackageIds.isEmpty()) "未挂载"
                else "${d.skillPackageIds.size} 个"
            )
            if (d.skillPackageIds.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TagRow(d.skillPackageIds, TextTertiary)
            }

            Spacer(Modifier.height(14.dp))

            // MCP
            CapLine(
                label = "MCP 服务",
                value = if (d.mcpServers.isEmpty()) "未配置"
                else "${d.mcpServers.size} 个"
            )
            if (d.mcpServers.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TagRow(d.mcpServers, TextTertiary)
            }

            Spacer(Modifier.height(14.dp))

            // 工具
            CapLine(
                label = "工具",
                value = if (d.tools.isEmpty()) "无" else "${d.tools.size} 个已启用"
            )
            if (d.tools.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TagRow(d.tools, TextTertiary)
            }
        }
    }
}

@Composable
private fun CapLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}

@Composable
private fun TagRow(items: List<String>, color: Color) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(items) { t -> Tag(text = t, color = color) }
    }
}

/* ── 通道与定时 ─────────────────────────────────────── */

@Composable
private fun ChannelCard(d: ExpertDetail) {
    Column {
        DetailSectionTitle(title = "接入与定时")
        Spacer(Modifier.height(8.dp))
        OctopCard {
            CapLine(
                label = "接入通道",
                value = if (d.channels.isEmpty()) "未接入" else d.channels.size.toString() + " 个"
            )
            if (d.channels.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TagRow(d.channels, Green)
            }

            Spacer(Modifier.height(14.dp))

            CapLine(
                label = "定时任务",
                value = if (d.cronJobs.isEmpty()) "无" else "${d.cronJobs.size} 个"
            )
            if (d.cronJobs.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                d.cronJobs.take(6).forEach { name ->
                    Text(
                        text = "· $name",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/* ── 手写的记忆文档 ─────────────────────────────────── */

@Composable
private fun DocCard(title: String, emptyHint: String, content: String) {
    Column {
        DetailSectionTitle(
            title = title,
            trailing = if (content.isBlank()) "空" else "${content.length} 字"
        )
        Spacer(Modifier.height(8.dp))
        OctopCard {
            if (content.isBlank()) {
                Text(
                    text = emptyHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            } else {
                val limit = 700
                Text(
                    text = if (content.length > limit) content.take(limit) + "\n…" else content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

/* ── 开场白 ─────────────────────────────────────────── */

@Composable
private fun IntroCard(d: ExpertDetail) {
    if (d.welcome.isBlank()) return
    Column {
        DetailSectionTitle(title = "开场白")
        Spacer(Modifier.height(8.dp))
        OctopCard {
            Text(
                text = d.welcome,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
