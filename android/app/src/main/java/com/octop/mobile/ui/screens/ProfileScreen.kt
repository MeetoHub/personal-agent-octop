package com.octop.mobile.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.core.auth.TokenStore
import com.octop.mobile.core.sync.LinkState
import com.octop.mobile.core.system.BridgeManager
import com.octop.mobile.core.system.PhoneLink
import com.octop.mobile.core.system.PhoneLinkState
import com.octop.mobile.core.system.PhoneTools
import com.octop.mobile.core.system.SystemBridge
import com.octop.mobile.ui.AppViewModel
import com.octop.mobile.ui.components.HubGlyph
import com.octop.mobile.ui.components.HubIcon
import com.octop.mobile.ui.components.OctopCard
import com.octop.mobile.ui.components.PageHeader
import com.octop.mobile.ui.components.PillButton
import com.octop.mobile.ui.components.SectionHeader
import com.octop.mobile.ui.components.StatusDot
import com.octop.mobile.ui.components.Tag
import com.octop.mobile.ui.theme.Accent
import com.octop.mobile.ui.theme.Amber
import com.octop.mobile.ui.theme.Bg
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.OutlineCol
import com.octop.mobile.ui.theme.Rose
import com.octop.mobile.ui.theme.Surface1
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary

/**
 * 「我的」——不再是开关清单，而是按网页端信息架构分组的**功能中心**。
 *
 * 为什么改：网页端主区有 11 项、管理区有 6 项能力，之前手机端只有本机权限和局域网桥，
 * 用户在手机上看不到知识库、Token 用量、模型配置这些，只能开浏览器。
 * 现在这一页就是入口目录：分组标题 + 图标 + 名称 + 数量/状态 + 右箭头，点进二级页。
 *
 * 分组与网页端一致：
 *   主区  —— 知识库 / 技能包 / 通道 / 连接器 / Token 统计 / 云端协同
 *   管理  —— 模型配置 / 插件管理 / 安全防护 / 应用设置 / 用户管理 / 存储管理
 * （「对话 / 专家 / 自动化」已经在底部 tab 里，不重复放。
 *   「工作台 / 远程桌面 / ACP 协作」是桌面专属，用户明确说不搬。）
 *
 * 本机的手机能力与局域网桥仍然保留在下面——它们是这台手机上独有的，
 * 网页端反而没有。
 */
@Composable
fun SettingsScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    val link by vm.link.collectAsState()
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { tick++ }

    val dotColor = when (link.state) {
        LinkState.OK -> Green
        LinkState.FAIL -> Rose
        LinkState.SYNCING -> Amber
        LinkState.UNKNOWN -> TextTertiary
    }

    // 分组、组内顺序、条目名称全部照抄本体 sidebarNav.tsx:150-335 的 IA，
    // 不再自己重排。本体只有三组：顶部无分组、settings、admin
    // （另有 control 组，其中的工作台/远程桌面/智能体协作手机端尚无路由，故不列）。
    // 命名取本体 zh.json 的 nav.* 原文，不自造词。
    //
    // 本体顶部无分组区（sidebarNav.tsx:154-186）是 对话/专家/定时/Token统计；
    // 前三个在手机端是底部 tab，只有 Token 统计落到这里，归入 nav.common「常用」。
    val commonEntries = listOf(
        Entry("Token 统计", "它花了多少", HubGlyph.USAGE, "settings/usage")
    )
    // settings 组（sidebarNav.tsx:188-243）本体顺序：
    // 个性化 → 通道 → 连接器 → 技能包 → 知识库 → 桥接。
    // 「个性化」手机端无独立路由，已由「专家」页承担，故不列。
    val settingsEntries = listOf(
        Entry("通道", "从哪里找它说话", HubGlyph.CHANNEL, "settings/channels"),
        Entry("连接器", "外部服务接入", HubGlyph.CONNECTOR, "settings/connectors"),
        Entry("技能包", "它会的本事", HubGlyph.SKILL, "settings/skills"),
        Entry("知识库", "它查资料的地方", HubGlyph.LIBRARY, "settings/knowledge"),
        Entry("云端协同", "腾讯云主机概况", HubGlyph.CLOUD, "settings/cloud")
    )
    // admin 组（sidebarNav.tsx:283-334）本体顺序：
    // 用户管理 → 模型配置 → 存储管理 → 插件管理 → 安全防护 → 应用设置。
    val adminEntries = listOf(
        Entry("用户管理", "谁能用这个本体", HubGlyph.USER, "settings/users"),
        Entry("模型配置", "它用哪个脑子", HubGlyph.MODEL, "settings/models"),
        Entry("存储管理", "文件存在哪", HubGlyph.STORAGE, "settings/storage"),
        Entry("插件管理", "给本体装的本事", HubGlyph.PLUGIN, "settings/plugins"),
        Entry("安全防护", "它被允许做什么", HubGlyph.SECURITY, "settings/security"),
        Entry("应用设置", "本体启动配置", HubGlyph.APP, "settings/app")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageHeader("我的", "本体跑在 NAS 上，这里只是遥控器") }

        // 连接状态做成一块「主控面板」：左侧状态灯 + 大字状态 + 地址，
        // 右侧整块可点的重连按钮。原来是一行小字，信息密度不够，看着像设置项。
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface1, RoundedCornerShape(20.dp))
                    .border(1.dp, OutlineCol, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 15.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(dotColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) { StatusDot(dotColor, size = 10) }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = link.message,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = link.endpoint,
                            fontSize = 11.sp,
                            color = TextTertiary,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    PillButton("重连", filled = false) { vm.refreshLink() }
                }
            }
        }

        // 分组标题用本体 zh.json 的 nav.common / nav.settings / nav.admin 原文：
        // 「常用」「设置」「管理」。副标题只写手机端的事实，不写营销话术。
        item { SectionHeader("常用", "${commonEntries.size} 项") }
        item { EntryGroup(commonEntries, onOpen) }

        item { SectionHeader("设置", "${settingsEntries.size} 项") }
        item { EntryGroup(settingsEntries, onOpen) }

        item { SectionHeader("管理", "管理员可见 · ${adminEntries.size} 项") }
        item { EntryGroup(adminEntries, onOpen) }

        item { SectionHeader("本机能力", "这台手机上独有，网页端没有") }
        item {
            OctopCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HubIcon(HubGlyph.DEVICE, TextSecondary, size = 18.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("手机能力", fontSize = 14.sp, color = TextPrimary)
                        Text(
                            "通讯录 / 短信 / 日历 / 通知",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                    Text(
                        text = "${SystemBridge.REQUIRED.count { SystemBridge.has(ctx, it.first) }}/${SystemBridge.REQUIRED.size}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.height(12.dp))
                val rows = remember(tick) {
                    listOf(
                        Capability("通讯录", "读联系人，供 agent 查人", SystemBridge.has(ctx, android.Manifest.permission.READ_CONTACTS), false),
                        Capability("短信", "读收发记录；发送必须你确认", SystemBridge.has(ctx, android.Manifest.permission.READ_SMS), false),
                        Capability("日历 / 闹钟", "看日程，替你设提醒", SystemBridge.has(ctx, android.Manifest.permission.READ_CALENDAR), false),
                        Capability("通知", "读取最近通知，做摘要", SystemBridge.has(ctx, "android.permission.POST_NOTIFICATIONS"), false),
                        Capability("屏幕", "截屏 + 元素树，可视化操作", false, true)
                    )
                }
                rows.forEachIndexed { i, cap ->
                    if (i > 0) Spacer(Modifier.height(11.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = cap.name, fontSize = 13.sp, color = TextPrimary)
                            Spacer(Modifier.height(1.dp))
                            Text(text = cap.desc, fontSize = 10.sp, color = TextTertiary, lineHeight = 14.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        when {
                            cap.onDemand -> Tag(text = "用时授权", color = TextTertiary)
                            cap.granted -> Tag(text = "已授权", color = Green)
                            else -> Tag(text = "未授权", color = TextTertiary)
                        }
                    }
                }
                val needGrant = rows.count { !it.granted && !it.onDemand }
                if (needGrant > 0) {
                    Spacer(Modifier.height(13.dp))
                    PillButton(text = "申请权限", filled = true) {
                        permLauncher.launch(SystemBridge.REQUIRED.map { it.first }.toTypedArray())
                    }
                }
            }
        }

        item { SectionHeader("反向通道", "手机主动连 NAS，不在同一网络也能用") }
        item {
            val linkState by PhoneLink.state.collectAsState()
            val online = linkState == PhoneLinkState.ONLINE
            OctopCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(if (online) Green else TextTertiary, size = 7)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (linkState) {
                            PhoneLinkState.ONLINE -> "已连接"
                            PhoneLinkState.CONNECTING -> "连接中…"
                            PhoneLinkState.OFFLINE -> "重连中…"
                            PhoneLinkState.IDLE -> "未启动"
                        },
                        fontSize = 14.sp,
                        color = if (online) Green else TextSecondary
                    )
                    Spacer(Modifier.weight(1f))
                    PillButton(text = if (online) "断开" else "连接", filled = !online) {
                        if (online) PhoneLink.stop() else PhoneLink.restart()
                        tick++
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "能力：" + PhoneTools.SUPPORTED.joinToString("、"),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = TextSecondary
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface1, RoundedCornerShape(20.dp))
                    .border(1.dp, OutlineCol, RoundedCornerShape(20.dp))
                    .clickable { vm.logout() }
                    .padding(horizontal = 16.dp, vertical = 15.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "退出登录", fontSize = 15.sp, color = Rose)
                    Spacer(Modifier.weight(1f))
                    Text(text = "›", fontSize = 16.sp, color = TextTertiary)
                }
            }
        }

        item {
            Text(
                text = "Octop 手机端 · 原生客户端\n推理、记忆、工具全在 NAS 本体执行，客户端不落地任何状态。",
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = TextTertiary,
                modifier = Modifier.padding(start = 2.dp, top = 6.dp)
            )
        }
    }
}

private data class Entry(
    val title: String,
    val subtitle: String,
    val glyph: HubGlyph,
    val route: String
)

/**
 * 分组入口列表：一张白卡片里堆若干行，行间用极浅的分隔线，
 * 和网页端「设置分组」的视觉一致——靠留白和一条发丝线分层，不用重边框。
 */
@Composable
private fun EntryGroup(entries: List<Entry>, onOpen: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(20.dp))
            .padding(vertical = 4.dp)
    ) {
        entries.forEachIndexed { i, entry ->
            if (i > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp, end = 16.dp)
                        .height(1.dp)
                        .background(OutlineCol)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(entry.route) }
                    .padding(start = 14.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标砖统一用品牌色低透明度底 + 品牌色描边，比原来的灰底灰图标
                // 更能形成「功能入口」的视觉节奏，也和对话页的快捷卡片呼应。
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Accent.copy(alpha = 0.10f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    HubIcon(entry.glyph, Accent, size = 18.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(text = entry.subtitle, fontSize = 11.sp, color = TextTertiary)
                }
                Spacer(Modifier.width(8.dp))
                Text(text = "›", fontSize = 18.sp, color = TextTertiary)
            }
        }
    }
}

private data class Capability(
    val name: String,
    val desc: String,
    val granted: Boolean,
    val onDemand: Boolean
)
