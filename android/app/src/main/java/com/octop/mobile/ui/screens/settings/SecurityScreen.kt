package com.octop.mobile.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.octop.mobile.ui.theme.Amber
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.Rose
import com.octop.mobile.ui.theme.Surface1
import com.octop.mobile.ui.theme.Surface2
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import org.json.JSONObject

/**
 * 安全防护（网页端「安全防护」）。
 *
 * GET /api/admin/security                   全局安全策略（hitl / filesystem / pii / skill_scan / tool_guard）
 * GET /api/admin/security/tool-guard/rules  命令拦截规则清单（rules / path / rule_count）
 *
 * 只读。任何一条策略关错都可能让 agent 越权或直接停摆，手机上不做编辑。
 */
@Composable
fun SecurityScreen(onBack: () -> Unit) {
    var policy by remember { mutableStateOf<LoadState<JSONObject>>(LoadState.Loading) }
    var rules by remember { mutableStateOf<LoadState<JSONObject>>(LoadState.Loading) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick) {
        policy = LoadState.Loading
        rules = LoadState.Loading
        val p = SyncRepository.hub.securityPolicy()
        policy = if (!p.ok) LoadState.Failed(reasonOf(p))
        else jsonObj(p.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是对象：${p.body.take(80)}")

        val r = SyncRepository.hub.toolGuardRules()
        rules = if (!r.ok) LoadState.Failed(reasonOf(r))
        else jsonObj(r.body)?.let { LoadState.Ready(it) }
            ?: LoadState.Failed("返回的不是对象：${r.body.take(80)}")
    }

    SubPage(
        title = "安全防护",
        subtitle = "它被允许做什么",
        onBack = onBack,
        onReload = { tick++ },
        note = "这里只读。策略在网页端改——关掉任何一条都可能让 agent 越权或直接卡住。"
    ) {
        item { SecurityReadonlyNotice() }

        when (val s = policy) {
            is LoadState.Loading -> item { SecurityLoadingCard("正在读取安全策略…") }
            is LoadState.Failed -> item { SecurityErrorCard(s.reason) { tick++ } }
            is LoadState.Ready -> {
                val o = s.value
                item {
                    SecuritySummaryCard(
                        enabledCount = listOf("hitl", "filesystem", "pii", "tool_guard")
                            .count { o.optJSONObject(it)?.optBoolean("enabled", false) == true },
                        ruleCount = (rules as? LoadState.Ready)?.value?.optInt("rule_count", 0)
                    )
                }
                item { Spacer(Modifier.height(10.dp)) }

                item {
                    SecurityGroupCard(
                        glyph = HubGlyph.SECURITY,
                        title = "运行边界",
                        description = "文件系统、隐私脱敏与技能扫描决定 agent 能读到什么、能看到什么。",
                        tone = SecurityTone.WARNING,
                        trailing = "3 项"
                    ) {
                        PolicyCard("文件系统管控", o.optJSONObject("filesystem"), HubGlyph.STORAGE, SecurityTone.WARNING) {
                            val fs = o.optJSONObject("filesystem")
                            KvRow("状态", if (fs?.optBoolean("enabled", false) == true) "开启" else "关闭")
                            val ruleList = fs?.optJSONArray("rules")
                            KvRow("规则数", ruleList?.length()?.toString() ?: "0")
                            if (ruleList != null && ruleList.length() > 0) {
                                val first = ruleList.optJSONObject(0)
                                KvRow("模式", first?.str("mode") ?: "—")
                                KvRow("操作", first?.optJSONArray("operations")?.let { a ->
                                    (0 until a.length()).joinToString("/") { a.optString(it) }
                                } ?: "—")
                                val paths = first?.optJSONArray("paths")
                                if (paths != null) {
                                    KvRow("保护路径", (0 until paths.length()).joinToString("\n") { paths.optString(it) })
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        PolicyCard("隐私脱敏 PII", o.optJSONObject("pii"), HubGlyph.USER, SecurityTone.WARNING) {
                            val pii = o.optJSONObject("pii")
                            KvRow("状态", if (pii?.optBoolean("enabled", false) == true) "开启" else "关闭")
                            KvRow("策略", pii?.str("strategy") ?: "—")
                            KvRow("作用面", pii?.optJSONArray("surfaces")?.let { a ->
                                (0 until a.length()).joinToString(" / ") { a.optString(it) }
                            } ?: "—")
                        }
                        Spacer(Modifier.height(10.dp))
                        PolicyCard("技能扫描", o.optJSONObject("skill_scan"), HubGlyph.SKILL, SecurityTone.NORMAL) {
                            KvRow("模式", o.optJSONObject("skill_scan")?.str("mode") ?: "—")
                        }
                    }
                }
                item { Spacer(Modifier.height(10.dp)) }

                item {
                    SecurityGroupCard(
                        glyph = HubGlyph.USER,
                        title = "人工确认",
                        description = "HITL 让高风险工具在执行前停下来等真人点头。",
                        tone = SecurityTone.WARNING,
                        trailing = "1 项"
                    ) {
                        PolicyCard("工具人工确认 HITL", o.optJSONObject("hitl"), HubGlyph.USER, SecurityTone.WARNING) {
                            val h = o.optJSONObject("hitl")
                            KvRow("状态", if (h?.optBoolean("enabled", false) == true) "开启" else "关闭")
                            KvRow("适用工具", h?.str("tools") ?: "—")
                            KvRow("允许决策", h?.optJSONArray("allowed_decisions")?.let { a ->
                                (0 until a.length()).joinToString(" / ") { a.optString(it) }
                            } ?: "—")
                        }
                    }
                }
                item { Spacer(Modifier.height(10.dp)) }

                item {
                    SecurityGroupCard(
                        glyph = HubGlyph.SECURITY,
                        title = "危险操作拦截",
                        description = "命令拦截是最后一道闸；关掉它，危险命令会直接落到本地环境。",
                        tone = SecurityTone.DANGER,
                        trailing = "1 项"
                    ) {
                        PolicyCard("命令拦截 Tool Guard", o.optJSONObject("tool_guard"), HubGlyph.SECURITY, SecurityTone.DANGER) {
                            val tg = o.optJSONObject("tool_guard")
                            KvRow("状态", if (tg?.optBoolean("enabled", false) == true) "开启" else "关闭")
                            KvRow("模式", tg?.str("mode") ?: "—")
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }

        item {
            SecurityGroupCard(
                glyph = HubGlyph.SECURITY,
                title = "拦截清单",
                description = "这些规则来自服务端规则文件；严重度越高，误放行代价越大。",
                tone = SecurityTone.DANGER,
                trailing = (rules as? LoadState.Ready)?.value?.optInt("rule_count", 0)?.let { "$it 条" }
            ) {
                when (val r = rules) {
                    is LoadState.Loading -> SecurityLoadingRow("正在读取拦截规则…")
                    is LoadState.Failed -> SecurityErrorRow(r.reason) { tick++ }
                    is LoadState.Ready -> {
                        val o = r.value
                        val arr = o.optJSONArray("rules")
                        val count = o.optInt("rule_count", arr?.length() ?: 0)
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Surface2, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "规则文件", fontSize = 11.sp, color = TextTertiary)
                                Spacer(Modifier.height(2.dp))
                                Text(text = o.str("path").ifBlank { "—" }, fontSize = 11.sp, color = TextPrimary)
                            }
                            Spacer(Modifier.width(8.dp))
                            Tag("$count 条", color = if (count == 0) Rose else Amber)
                        }
                        if (arr == null || arr.length() == 0) {
                            Spacer(Modifier.height(12.dp))
                            SecurityEmptyRow("没有生效的拦截规则")
                        } else {
                            val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
                            list.forEachIndexed { index, rule ->
                                Spacer(Modifier.height(10.dp))
                                RuleCard(rule)
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class SecurityTone { NORMAL, WARNING, DANGER }

@Composable
private fun SecurityTone.tint(textSecondary: Color, textTertiary: Color): Color = when (this) {
    SecurityTone.NORMAL -> textSecondary
    SecurityTone.WARNING -> Amber
    SecurityTone.DANGER -> Rose
}

@Composable
private fun SecurityReadonlyNotice() {
    Column(
        modifier = Modifier.fillMaxWidth().background(Rose.copy(alpha = 0.10f), RoundedCornerShape(20.dp)).padding(horizontal = 15.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).background(Surface1, CircleShape),
                contentAlignment = Alignment.Center
            ) { HubIcon(HubGlyph.SECURITY, tint = Rose, size = 19.dp) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "只读 · 改动请回网页端", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Rose)
                Spacer(Modifier.height(3.dp))
                Text(text = "这里只是核对现状；关闭任意一道防线都可能让 agent 越权或直接卡住。", fontSize = 11.sp, lineHeight = 16.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun SecuritySummaryCard(enabledCount: Int, ruleCount: Int?) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Surface1, RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Surface2, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) { HubIcon(HubGlyph.SECURITY, tint = Rose, size = 22.dp) }
            Spacer(Modifier.width(12.dp))
            Text(text = "安全防线概览", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag("已开启 ${enabledCount}/4", color = if (enabledCount >= 3) Green else Rose)
            Tag("拦截 ${ruleCount ?: "…"} 条", color = if (ruleCount == null || ruleCount == 0) Rose else Amber)
        }
    }
}

@Composable
private fun SecurityGroupCard(
    glyph: HubGlyph,
    title: String,
    description: String,
    tone: SecurityTone,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val accent = tone.tint(TextSecondary, TextTertiary)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (tone == SecurityTone.DANGER) Rose.copy(alpha = 0.06f) else Surface1, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).background(Surface2, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) { HubIcon(glyph, tint = accent, size = 18.dp) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = accent)
                Spacer(Modifier.height(3.dp))
                Text(text = description, fontSize = 11.sp, lineHeight = 16.sp, color = TextSecondary)
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

@Composable
private fun PolicyCard(
    title: String,
    node: JSONObject?,
    glyph: HubGlyph,
    tone: SecurityTone,
    body: @Composable () -> Unit
) {
    val on = node?.optBoolean("enabled", false) == true
    val accent = tone.tint(TextSecondary, TextTertiary)
    Column(
        modifier = Modifier.fillMaxWidth().background(Surface2, RoundedCornerShape(16.dp)).padding(horizontal = 13.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HubIcon(glyph, tint = accent, size = 17.dp)
            Spacer(Modifier.width(8.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
            Tag(if (on) "开启" else "关闭", color = if (on) Green else Rose)
        }
        Spacer(Modifier.height(8.dp))
        body()
    }
}

@Composable
private fun SecurityLoadingCard(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Surface1, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Accent)
        Spacer(Modifier.width(10.dp))
        Text(text = text, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun SecurityLoadingRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Accent)
        Spacer(Modifier.width(10.dp))
        Text(text = text, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun SecurityErrorCard(reason: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Rose.copy(alpha = 0.08f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(text = "读取失败：$reason", fontSize = 12.sp, lineHeight = 17.sp, color = Rose)
        Spacer(Modifier.height(10.dp))
        PillButton("重试", filled = false) { onRetry() }
    }
}

@Composable
private fun SecurityErrorRow(reason: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Rose.copy(alpha = 0.08f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(text = "读取失败：$reason", fontSize = 12.sp, lineHeight = 17.sp, color = Rose)
        Spacer(Modifier.height(8.dp))
        PillButton("重试", filled = false) { onRetry() }
    }
}

@Composable
private fun SecurityEmptyRow(text: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(36.dp).background(Surface1, CircleShape), contentAlignment = Alignment.Center) {
            HubIcon(HubGlyph.SECURITY, tint = TextTertiary, size = 18.dp)
        }
        Spacer(Modifier.height(8.dp))
        Text(text = text, fontSize = 12.sp, color = TextTertiary)
    }
}

@Composable
private fun RuleCard(rule: JSONObject) {
    val severity = rule.str("severity")
    val color = when (severity) {
        "HIGH", "CRITICAL" -> Rose
        "MEDIUM" -> Amber
        "LOW" -> TextTertiary
        else -> Accent
    }
    Column(
        modifier = Modifier.fillMaxWidth().background(Surface2, RoundedCornerShape(14.dp)).padding(horizontal = 13.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = rule.str("id"), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(6.dp))
            Tag(severity.ifBlank { "?" }, color = color)
        }
        Spacer(Modifier.height(5.dp))
        Text(text = rule.str("description"), fontSize = 11.sp, lineHeight = 16.sp, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "工具 " + rule.optJSONArray("tools")?.let { a ->
                (0 until a.length()).joinToString("/") { a.optString(it) }
            }.orEmpty(),
            fontSize = 10.sp,
            color = TextTertiary
        )
        val rem = rule.str("remediation")
        if (rem.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(text = rem, fontSize = 10.sp, lineHeight = 15.sp, color = TextTertiary)
        }
    }
}
