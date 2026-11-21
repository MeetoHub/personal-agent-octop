package com.octop.mobile.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.core.sync.SyncRepository
import com.octop.mobile.ui.components.HubGlyph
import com.octop.mobile.ui.components.HubIcon
import com.octop.mobile.ui.components.PillButton
import com.octop.mobile.ui.components.Tag
import com.octop.mobile.ui.theme.Accent
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.Rose
import com.octop.mobile.ui.theme.Surface1
import com.octop.mobile.ui.theme.Surface2
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 技能包（网页端「技能包」）。
 *
 * 接口：
 *   GET  /api/agents/{agent_id}/skills        这个 agent 的全部技能（内置 + 工作区 + 包挂载）
 *   GET  /api/skill-packages                  全局技能包列表
 *   GET  /api/agents/{agent_id}/skill-packages 当前 agent 挂载了哪些包
 *   POST /api/agents/{agent_id}/skills/{name}/enable|disable   开/关一个技能
 *
 * 开关是安全的（只写自定义配置，不改技能文件），所以给真按钮；
 * 建包 / 装包要传文件或走 SkillHub 安装，这批不做。
 */
@Composable
fun SkillsScreen(agentId: String, onBack: () -> Unit) {
    var segment by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    var skills by remember { mutableStateOf<LoadState<List<JSONObject>>>(LoadState.Loading) }
    var packages by remember { mutableStateOf<LoadState<List<JSONObject>>>(LoadState.Loading) }
    var mounted by remember { mutableStateOf<List<String>>(emptyList()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tick, agentId) {
        if (agentId.isBlank()) {
            skills = LoadState.Failed("还没选中 agent，先回专家页选一个")
            packages = LoadState.Ready(emptyList())
            return@LaunchedEffect
        }
        skills = LoadState.Loading
        packages = LoadState.Loading

        val s = SyncRepository.api.listSkills(agentId)
        skills = if (!s.ok) LoadState.Failed(reasonOf(s))
        else jsonList(s.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是列表：${s.body.take(80)}")

        val p = SyncRepository.api.listSkillPackages()
        packages = if (!p.ok) LoadState.Failed(reasonOf(p))
        else jsonList(p.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是列表：${p.body.take(80)}")

        val m = SyncRepository.api.listSkillPackageMounts(agentId)
        mounted = if (m.ok) {
            runCatching {
                val o = JSONObject(m.body)
                val arr = o.optJSONArray("package_ids") ?: return@runCatching emptyList()
                (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { v -> v.isNotBlank() } }
            }.getOrDefault(emptyList())
        } else emptyList()
    }

    SubPage(
        title = "技能包",
        subtitle = "它会的本事",
        onBack = onBack,
        onReload = { tick++ },
        note = "开关只影响这个 agent 用不用这项技能，不动技能文件本身。"
    ) {
        // 顶部摘要把「两段业务」与操作反馈先说清，下面是两张独立的白卡列表。
        item {
            SettingsSummaryCard(
                glyph = HubGlyph.SKILL,
                title = "按用途分成两份清单",
                description = "技能是运行时清单，技能包是可复用的分发单元；两段数据独立读取，切换不丢状态。",
                chips = listOfNotNull(
                    (skills as? LoadState.Ready)?.value?.size?.let { "技能 $it" },
                    (packages as? LoadState.Ready)?.value?.size?.let { "包 $it" },
                    mounted.size.takeIf { it > 0 }?.let { "已挂载 $it" }
                )
            )
        }

        if (actionError != null) {
            item {
                SettingsNoticeCard(
                    text = actionError!!,
                    tone = SettingsNoticeTone.DANGER,
                    trailing = "关闭" to { actionError = null }
                )
            }
        }

        item {
            SettingsSegment(
                labels = listOf("技能", "技能包"),
                selected = segment,
                onSelect = { segment = it }
            )
        }

        item {
            SettingsSectionCard(
                icon = HubGlyph.SKILL,
                title = if (segment == 0) "可用技能" else "技能包目录",
                description = if (segment == 0) {
                    "这个 agent 实际能调用的每一项本事；内置与工作区技能也会出现在这里。"
                } else {
                    "全局可复用的分发单元；标记为已挂载的包已出现在当前 agent 的技能清单里。"
                },
                trailing = if (segment == 0) {
                    (skills as? LoadState.Ready)?.value?.size?.let { "$it 项" }
                } else {
                    (packages as? LoadState.Ready)?.value?.size?.let { "$it 个" }
                }
            ) {
                when {
                    segment == 0 && skills is LoadState.Loading -> SettingsLoadingRow("正在读取技能清单…")
                    segment == 1 && packages is LoadState.Loading -> SettingsLoadingRow("正在读取技能包目录…")
                    segment == 0 && skills is LoadState.Failed -> SettingsInlineError((skills as LoadState.Failed).reason) { tick++ }
                    segment == 1 && packages is LoadState.Failed -> SettingsInlineError((packages as LoadState.Failed).reason) { tick++ }
                    segment == 0 && (skills as LoadState.Ready).value.isEmpty() -> SettingsEmptyRow("这个 agent 还没有可用技能")
                    segment == 1 && (packages as LoadState.Ready).value.isEmpty() -> SettingsEmptyRow("技能包目录还是空的")
                    segment == 0 -> (skills as LoadState.Ready).value.forEachIndexed { index, skill ->
                        if (index > 0) SettingsListDivider()
                        SkillCard(skill, busy == skill.str("slug")) { slug ->
                            val enabled = skill.optBoolean("enabled", false)
                            busy = slug
                            scope.launch {
                                val res = if (enabled) SyncRepository.api.disableSkill(agentId, slug)
                                else SyncRepository.api.enableSkill(agentId, slug)
                                busy = null
                                if (res.ok) tick++ else actionError = reasonOf(res)
                            }
                        }
                    }
                    else -> (packages as LoadState.Ready).value.forEachIndexed { index, pack ->
                        if (index > 0) SettingsListDivider()
                        PackageCard(pack, mounted)
                    }
                }
            }
        }

        item {
            SettingsHintCard("技能包目录和挂载关系由网页端维护；这里只展示结果，不会改动包文件。")
        }
    }
}

@Composable
private fun SkillCard(skill: JSONObject, busy: Boolean, onToggle: (String) -> Unit) {
    val name = skill.str("name", "slug", "id")
    val enabled = skill.optBoolean("enabled", false)
    val kind = when (skill.str("kind")) {
        "builtin" -> "内置"
        "package" -> "技能包"
        "workspace" -> "工作区"
        else -> skill.str("kind").ifBlank { "技能" }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).background(Surface2, CircleShape),
            contentAlignment = Alignment.Center
        ) { HubIcon(HubGlyph.SKILL, tint = if (enabled) Green else TextTertiary, size = 18.dp) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = skill.str("description").ifBlank { name },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                if (busy) {
                    Text("处理中…", fontSize = 10.sp, color = TextTertiary)
                } else {
                    Tag(if (enabled) "已启用" else "已停用", color = if (enabled) Green else TextTertiary)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(text = "$name · $kind", fontSize = 10.sp, color = TextTertiary)
        }
        Spacer(Modifier.width(10.dp))
        PillButton(if (enabled) "停用" else "启用", filled = !enabled) { onToggle(name) }
    }
}

@Composable
private fun PackageCard(pack: JSONObject, mounted: List<String>) {
    val id = pack.str("id", "skill_package_id")
    val isMounted = mounted.contains(id)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).background(Surface2, CircleShape),
            contentAlignment = Alignment.Center
        ) { HubIcon(HubGlyph.LIBRARY, tint = if (isMounted) Accent else TextTertiary, size = 18.dp) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pack.str("name").ifBlank { id },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (isMounted) Tag("已挂载", color = Accent)
            }
            Spacer(Modifier.height(4.dp))
            val desc = pack.str("description")
            Text(
                text = desc.ifBlank { "${pack.optInt("skill_count", 0)} 项技能 · 创建者 ${pack.str("created_by").ifBlank { "—" }}" },
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = if (desc.isNotBlank()) TextSecondary else TextTertiary
            )
            if (desc.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${pack.optInt("skill_count", 0)} 项技能 · 创建者 ${pack.str("created_by").ifBlank { "—" }}",
                    fontSize = 10.sp,
                    color = TextTertiary
                )
            }
        }
    }
}

/** 本文件内复用的设置页语义骨架，避免五个二级页又回到同一种扁平卡片。 */
@Composable
private fun SettingsSummaryCard(
    glyph: HubGlyph,
    title: String,
    description: String,
    chips: List<String> = emptyList()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Surface2, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) { HubIcon(glyph, tint = Accent, size = 22.dp) }
            Spacer(Modifier.width(12.dp))
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        Spacer(Modifier.height(10.dp))
        Text(text = description, fontSize = 12.sp, lineHeight = 18.sp, color = TextSecondary)
        if (chips.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                chips.forEach { Tag(it, color = Accent) }
            }
        }
    }
}

@Composable
private fun SettingsSegment(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val active = selected == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (active) Surface1 else Color.Transparent, RoundedCornerShape(10.dp))
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) TextPrimary else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon: HubGlyph? = null,
    title: String,
    description: String? = null,
    trailing: String? = null,
    tone: SettingsSectionTone = SettingsSectionTone.NORMAL,
    content: @Composable ColumnScope.() -> Unit
) {
    val accent = when (tone) {
        SettingsSectionTone.NORMAL -> Accent
        SettingsSectionTone.WARNING -> com.octop.mobile.ui.theme.Amber
        SettingsSectionTone.DANGER -> Rose
    }
    val tint = when (tone) {
        SettingsSectionTone.NORMAL -> Surface2
        SettingsSectionTone.WARNING -> com.octop.mobile.ui.theme.SoftOrange
        SettingsSectionTone.DANGER -> Rose.copy(alpha = 0.08f)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(
                    modifier = Modifier.size(34.dp).background(tint, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) { HubIcon(icon, tint = accent, size = 18.dp) }
                Spacer(Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = accent)
                if (!description.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(text = description, fontSize = 11.sp, lineHeight = 16.sp, color = TextSecondary)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                Text(text = trailing, fontSize = 10.sp, color = TextTertiary)
            }
        }

        Spacer(Modifier.height(14.dp))
        Column(content = content)
    }
}

private enum class SettingsSectionTone { NORMAL, WARNING, DANGER }

@Composable
private fun SettingsListDivider() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(vertical = 1.dp)
            .background(Surface2)
    )
}

@Composable
private fun SettingsLoadingRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = Accent
        )
        Spacer(Modifier.width(10.dp))
        Text(text = text, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun SettingsEmptyRow(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(Surface2, CircleShape),
            contentAlignment = Alignment.Center
        ) { HubIcon(HubGlyph.STORAGE, tint = TextTertiary, size = 18.dp) }
        Spacer(Modifier.height(8.dp))
        Text(text = text, fontSize = 12.sp, color = TextTertiary)
    }
}

@Composable
private fun SettingsInlineError(reason: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Rose.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(text = "读取失败：$reason", fontSize = 12.sp, lineHeight = 17.sp, color = Rose)
        Spacer(Modifier.height(8.dp))
        PillButton("重试", filled = false) { onRetry() }
    }
}

private enum class SettingsNoticeTone { DANGER, WARNING }

@Composable
private fun SettingsNoticeCard(
    text: String,
    tone: SettingsNoticeTone,
    trailing: Pair<String, () -> Unit>? = null
) {
    val color = if (tone == SettingsNoticeTone.DANGER) Rose else com.octop.mobile.ui.theme.Amber
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(text = text, fontSize = 12.sp, lineHeight = 17.sp, color = color)
        if (trailing != null) {
            Spacer(Modifier.height(6.dp))
            PillButton(trailing.first, filled = false) { trailing.second() }
        }
    }
}

@Composable
private fun SettingsHintCard(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(text = text, fontSize = 10.sp, lineHeight = 16.sp, color = TextSecondary)
    }
}
