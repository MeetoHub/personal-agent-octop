package com.octop.mobile.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octop.mobile.core.model.ScheduleItem
import com.octop.mobile.core.model.ScheduleKind
import com.octop.mobile.ui.AppViewModel
import com.octop.mobile.ui.components.EmptyHint
import com.octop.mobile.ui.components.OctopCard
import com.octop.mobile.ui.components.PageHeader
import com.octop.mobile.ui.components.PillButton
import com.octop.mobile.ui.components.SectionHeader
import com.octop.mobile.ui.components.StatusDot
import com.octop.mobile.ui.components.Tag
import com.octop.mobile.ui.theme.Accent
import com.octop.mobile.ui.theme.BgSecondary
import com.octop.mobile.ui.theme.Green
import com.octop.mobile.ui.theme.OutlineCol
import com.octop.mobile.ui.theme.Rose
import com.octop.mobile.ui.theme.Surface3
import com.octop.mobile.ui.theme.TextPrimary
import com.octop.mobile.ui.theme.TextQuaternary
import com.octop.mobile.ui.theme.TextSecondary
import com.octop.mobile.ui.theme.TextTertiary
import kotlinx.coroutines.launch

/**
 * 定时页。
 *
 * 关键改动：原来那版把 trigger 原样印在卡片上（`cron:0 9 * * *`），开关也只改本地
 * 状态、后端完全不知道。现在：
 *   - trigger 翻成人话（每天 09:00 / 每 30 分钟 / 工作日 08:30）
 *   - 开关走 PATCH /api/agents/{id}/cron/{cron_id}，失败回滚
 *   - 每条可「跑一次」（run-now）和删除
 *
 * 版式：借鉴桌面端 CronJobCard / JobDetailDrawer：列表顶部加任务数小标题，
 * 卡片内按「标题+开关 → 元信息 → 任务内容 → 操作」四层组织，减少卡片密度、拉平留白。
 */
@Composable
fun ScheduleScreen(vm: AppViewModel) {
    val schedules by vm.schedules.collectAsState()
    val enabled = schedules.count { it.enabled }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(com.octop.mobile.ui.theme.Bg),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageHeader(
                title = "定时",
                subtitle = if (schedules.isEmpty()) "到点自己跑，不用你催"
                else "启用中 $enabled 条 · 共 ${schedules.size} 条",
                trailing = {
                    PillButton("新建", filled = true) { showForm = true }
                }
            )
        }

        if (schedules.isEmpty()) {
            item { EmptyHint("还没有定时任务。在网页端建一个，这里就能看和开关。") }
        } else {
            item { SectionHeader("任务列表", "${schedules.size} 条") }
        }

        items(schedules, key = { it.id }) { s ->
            ScheduleCard(
                item = s,
                busy = busy == s.id,
                onToggle = {
                    scope.launch {
                        busy = s.id
                        vm.setScheduleEnabled(s.id, !s.enabled)
                        busy = null
                    }
                },
                onRunNow = {
                    scope.launch {
                        busy = s.id
                        vm.runScheduleNow(s.id)
                        busy = null
                    }
                },
                onDelete = {
                    scope.launch {
                        busy = s.id
                        vm.deleteSchedule(s.id)
                        busy = null
                    }
                }
            )
        }
    }

        if (showForm) {
            NewScheduleSheet(
                onClose = { showForm = false },
                onCreate = { name, trigger, prompt ->
                    vm.createSchedule(name, trigger, prompt) { ok -> if (ok) showForm = false }
                    showForm = false
                }
            )
        }
    }
}

/**
 * 新建定时的最简表单：要做的事 + 什么时候跑。
 * 时间用步进按钮而不是选择器——手机上拨滚轮比点两下慢。
 */
@Composable
private fun NewScheduleSheet(
    onClose: () -> Unit,
    onCreate: (name: String, trigger: String, prompt: String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var mode by remember { mutableIntStateOf(0) }   // 0 每天 / 1 每隔几小时 / 2 只一次
    var hour by remember { mutableIntStateOf(9) }
    var minute by remember { mutableIntStateOf(0) }
    var everyHours by remember { mutableIntStateOf(2) }

    fun trigger(): String = when (mode) {
        0 -> "cron:$minute $hour * * *"
        1 -> "interval:${everyHours * 3600}"
        else -> {
            val t = java.time.LocalDateTime.now().plusDays(1)
                .withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            "date:${t}"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onClose() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Surface3, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 22.dp)
                .clickable(enabled = false) {}
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("新建定时", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.weight(1f))
                Text(
                    "关闭",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.clickable { onClose() }.padding(vertical = 6.dp, horizontal = 4.dp)
                )
            }

            Spacer(Modifier.height(14.dp))
            FieldLabel("它要做什么")
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgSecondary, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (prompt.isEmpty()) {
                    Text("例如：读一遍今天的日程，把要准备的整理给我", fontSize = 13.sp, color = TextTertiary)
                }
                BasicTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            FieldLabel("名字（可不填）")
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgSecondary, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                if (name.isEmpty()) {
                    Text("晨间简报", fontSize = 13.sp, color = TextTertiary)
                }
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(14.dp))
            FieldLabel("什么时候跑")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("每天", "每隔几小时", "只一次").forEachIndexed { i, label ->
                    val active = mode == i
                    Box(
                        modifier = Modifier
                            .background(
                                if (active) Accent.copy(alpha = 0.14f) else BgSecondary,
                                RoundedCornerShape(11.dp)
                            )
                            .clickable { mode = i }
                            .padding(horizontal = 13.dp, vertical = 8.dp)
                    ) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                            color = if (active) Accent else TextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (mode == 1) {
                    Stepper("$everyHours 小时", onMinus = { if (everyHours > 1) everyHours-- }, onPlus = { if (everyHours < 24) everyHours++ })
                } else {
                    Stepper("%02d".format(hour), onMinus = { hour = (hour + 23) % 24 }, onPlus = { hour = (hour + 1) % 24 })
                    Text(" : ", fontSize = 15.sp, color = TextTertiary)
                    Stepper("%02d".format(minute), onMinus = { minute = (minute + 55) % 60 }, onPlus = { minute = (minute + 5) % 60 })
                    if (mode == 2) {
                        Spacer(Modifier.width(8.dp))
                        Text("明天", fontSize = 11.sp, color = TextTertiary)
                    }
                }
                Spacer(Modifier.weight(1f))
                PillButton(
                    text = "创建",
                    filled = prompt.isNotBlank()
                ) {
                    if (prompt.isNotBlank()) onCreate(name, trigger(), prompt.trim())
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 11.sp, color = TextTertiary)
}

@Composable
private fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        StepperKey("−", onMinus)
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        StepperKey("+", onPlus)
    }
}

@Composable
private fun StepperKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(BgSecondary, RoundedCornerShape(9.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 15.sp, color = TextSecondary)
    }
}

@Composable
private fun ScheduleCard(
    item: ScheduleItem,
    busy: Boolean,
    onToggle: () -> Unit,
    onRunNow: () -> Unit,
    onDelete: () -> Unit
) {
    // 删除不可撤销，第一次点只把按钮换成「确认」，避免手滑把定时任务删了
    var confirmDelete by remember { mutableStateOf(false) }
    OctopCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(if (item.enabled) Green else TextTertiary)
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = humanSchedule(item.cron),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.enabled) TextPrimary else TextSecondary
                )
                Spacer(Modifier.height(2.dp))
                Text(text = item.name, fontSize = 12.sp, color = TextTertiary)
            }
            Switch(
                checked = item.enabled,
                enabled = !busy,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Accent,
                    uncheckedThumbColor = TextQuaternary,
                    uncheckedTrackColor = Surface3
                )
            )
        }

        // 元信息行（对应桌面 CronJobCard 的 channel/类型/下次运行标签）：紧随标题
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tag(
                text = if (item.kind == ScheduleKind.ONCE) "一次" else "周期",
                color = if (item.enabled) Accent else TextTertiary
            )
            if (item.nextRunLabel.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(text = item.nextRunLabel, fontSize = 11.sp, color = TextTertiary)
            }
        }

        // 任务内容：用一条分隔线把它和标题/元信息拉开层级
        if (item.taskPrompt.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(OutlineCol))
            Spacer(Modifier.height(9.dp))
            Text(
                text = item.taskPrompt,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = TextSecondary,
                maxLines = 2
            )
        }

        // 操作区单独一行，避免和元信息挤在一起
        Spacer(Modifier.height(11.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            if (confirmDelete) {
                TextAction("取消", TextSecondary, enabled = !busy) { confirmDelete = false }
                Spacer(Modifier.width(14.dp))
                TextAction("确认删除", Rose, enabled = !busy, onClick = onDelete)
            } else {
                TextAction(if (busy) "…" else "跑一次", Accent, enabled = !busy, onClick = onRunNow)
                Spacer(Modifier.width(14.dp))
                TextAction("删除", Rose, enabled = !busy) { confirmDelete = true }
            }
        }
    }
}

@Composable
private fun TextAction(text: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = if (enabled) color else TextTertiary,
        modifier = Modifier
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    )
}

// ── trigger → 人话 ─────────────────────────────────────

private fun humanSchedule(trigger: String): String {
    if (trigger.isBlank()) return "未设置"
    if (trigger.startsWith("interval:")) {
        val sec = trigger.removePrefix("interval:").trim().toLongOrNull() ?: return trigger
        return when {
            sec >= 86400 && sec % 86400 == 0L -> "每 ${sec / 86400} 天"
            sec >= 3600 && sec % 3600 == 0L -> "每 ${sec / 3600} 小时"
            sec >= 60 && sec % 60 == 0L -> "每 ${sec / 60} 分钟"
            else -> "每 $sec 秒"
        }
    }
    if (trigger.startsWith("date:")) {
        val raw = trigger.removePrefix("date:").trim()
        val d = if (raw.length >= 16) raw.substring(0, 16).replace('T', ' ') else raw
        return "一次 · $d"
    }
    val expr = trigger.removePrefix("cron:").trim()
    val f = expr.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (f.size < 5) return expr
    val mi = f[0]; val ho = f[1]; val dom = f[2]; val mon = f[3]; val dow = f[4]
    fun time(): String {
        val h = ho.toIntOrNull() ?: return "%02d".format(0)
        val m = mi.toIntOrNull() ?: 0
        return "%02d:%02d".format(h, m)
    }
    return when {
        expr == "* * * * *" -> "每分钟"
        mi.startsWith("*/") && ho == "*" -> "每 ${mi.removePrefix("*/")} 分钟"
        mi.startsWith("*/") && ho == "*" && dom == "!" -> "每 ${mi.removePrefix("*/")} 分钟"
        ho == "*" && mi != "*" -> "每小时第 $mi 分"
        ho != "*" && mi != "*" && dow == "1-5" -> "工作日 ${time()}"
        ho != "*" && mi != "*" && dow != "*" && dow != "?" -> "每${dowName(dow)} ${time()}"
        ho != "*" && mi != "*" && dom != "*" && dom != "?" -> "每月 $dom 日 ${time()}"
        ho != "*" && mi != "*" -> "每天 ${time()}"
        else -> expr
    }
}

private fun dowName(s: String): String = when (s) {
    "0", "7" -> "周日"
    "1" -> "周一"
    "2" -> "周二"
    "3" -> "周三"
    "4" -> "周四"
    "5" -> "周五"
    "6" -> "周六"
    else -> "周$s"
}
