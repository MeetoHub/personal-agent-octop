package com.octop.mobile.core.data

import com.octop.mobile.core.model.Approval
import com.octop.mobile.core.model.Expert
import com.octop.mobile.core.model.LibraryItem
import com.octop.mobile.core.model.LibraryKind
import com.octop.mobile.core.model.OctopTask
import com.octop.mobile.core.model.Receipt
import com.octop.mobile.core.model.RiskLevel
import com.octop.mobile.core.model.ScheduleItem
import com.octop.mobile.core.model.ScheduleKind
import com.octop.mobile.core.model.StepStatus
import com.octop.mobile.core.model.TaskStatus
import com.octop.mobile.core.model.TaskStep

/**
 * 演示数据。等 SyncRepository 能拉到真数据，这里整个文件删掉。
 * 刻意用你真实场景的文案，方便直接判断 UI 信息密度够不够。
 */
object DemoData {

    private val now = System.currentTimeMillis()

    val experts = listOf(
        Expert(
            id = "lifebot",
            name = "小乐",
            role = "生活助理",
            description = "管日程、待办、提醒。手机上的杂事都丢给它。",
            enabledTools = listOf("phone.shell", "screenshot", "calendar.read", "alarm.set"),
            color = 0xFF7C6CFF
        ),
        Expert(
            id = "inbox",
            name = "信使",
            role = "消息整理",
            description = "短信、通知、通话记录归拢成一条时间线，重要的才叫你。",
            enabledTools = listOf("sms.read", "notifications.read", "calllog.read"),
            color = 0xFF4FD1C5
        ),
        Expert(
            id = "researcher",
            name = "查证",
            role = "信息核查",
            description = "要查政策、办事流程、价格的时候找它。会真的打开页面看。",
            enabledTools = listOf("web.open", "screenshot", "ui_dump"),
            color = 0xFFFFB74D
        ),
        Expert(
            id = "writer",
            name = "笔杆",
            role = "写作",
            description = "简历、规划书、课件。你口述，它成稿。",
            enabledTools = listOf("files.write", "tts"),
            color = 0xFF5FD68A
        )
    )

    val tasks = listOf(
        OctopTask(
            id = "t1",
            title = "把今天的短信整理成待办",
            summary = "26 条新短信，其中 3 条含时间点",
            status = TaskStatus.WAITING_APPROVAL,
            progress = 0.7f,
            expertId = "inbox",
            steps = listOf(
                TaskStep("读取 26 条短信", StepStatus.DONE),
                TaskStep("识别含时间/金额的条目", StepStatus.DONE),
                TaskStep("生成 3 条待办草稿", StepStatus.DONE),
                TaskStep("等你确认后写入日历", StepStatus.RUNNING)
            ),
            approval = Approval(
                id = "a1",
                action = "把「周五 14:00 社区体检」写入日历",
                reason = "涉及日程写入，按你的规则需要确认",
                risk = RiskLevel.LOW
            ),
            receipts = listOf(
                Receipt("sms.read", "读取 26 条，耗时 0.4s", now - 60_000),
                Receipt("phone.shell", "am start 日历预览", now - 40_000)
            ),
            updatedAtMillis = now - 30_000
        ),
        OctopTask(
            id = "t2",
            title = "查一下医保报销要带什么材料",
            summary = "已打开政务页面，正在摘录",
            status = TaskStatus.RUNNING,
            progress = 0.45f,
            expertId = "researcher",
            steps = listOf(
                TaskStep("搜索官方办事指南", StepStatus.DONE),
                TaskStep("打开市医保局页面", StepStatus.DONE),
                TaskStep("摘录材料清单", StepStatus.RUNNING),
                TaskStep("整理成一页纸", StepStatus.PENDING)
            ),
            receipts = listOf(
                Receipt("screenshot", "截取页面 1 张", now - 120_000),
                Receipt("ui_dump", "解析出 42 个元素", now - 110_000)
            ),
            updatedAtMillis = now - 120_000
        ),
        OctopTask(
            id = "t3",
            title = "明早 7:30 的闹钟",
            summary = "已设置，明早提醒你带身份证",
            status = TaskStatus.DONE,
            progress = 1f,
            expertId = "lifebot",
            steps = listOf(
                TaskStep("设置闹钟 07:30", StepStatus.DONE),
                TaskStep("附加提醒文案", StepStatus.DONE)
            ),
            receipts = listOf(Receipt("alarm.set", "07:30 工作日重复", now - 3_600_000)),
            updatedAtMillis = now - 3_600_000
        )
    )

    val library = listOf(
        LibraryItem(
            id = "l1",
            title = "魏方乐 · 身份与近况",
            snippet = "汉语言文学专业，正在转 AI 产品方向。长期目标：做一件能被别人用上的东西。",
            kind = LibraryKind.MEMORY,
            tags = listOf("身份", "长期")
        ),
        LibraryItem(
            id = "l2",
            title = "手机端智能体的技术选型结论",
            snippet = "手：Shizuku 万能通道；脑：本地 DSH + PRoot；脸：声明式渲染。不要一上来就设计 UI。",
            kind = LibraryKind.NOTE,
            tags = listOf("项目", "选型")
        ),
        LibraryItem(
            id = "l3",
            title = "铜匠实验室 · 3D 科学课件",
            snippet = "给中小学生做的可交互分子/天体模型，含 12 个课程包。",
            kind = LibraryKind.KNOWLEDGE,
            tags = listOf("作品")
        ),
        LibraryItem(
            id = "l4",
            title = "妈妈",
            snippet = "每周日晚上打电话。她关心的是吃饭和睡觉，不是项目进度。",
            kind = LibraryKind.PERSON,
            tags = listOf("家人")
        )
    )

    val schedules = listOf(
        ScheduleItem(
            id = "s1",
            name = "每早 7:00 今日简报",
            cron = "每天 07:00",
            kind = ScheduleKind.CRON,
            enabled = true,
            nextRunLabel = "明天 07:00",
            taskPrompt = "汇总今天日程、未读重要短信、待办，生成一段 200 字以内的话。"
        ),
        ScheduleItem(
            id = "s2",
            name = "每周日 21:00 一周回顾",
            cron = "每周日 21:00",
            kind = ScheduleKind.CRON,
            enabled = true,
            nextRunLabel = "本周日 21:00",
            taskPrompt = "回顾这一周完成的任务、新增记忆、未解决的事。"
        ),
        ScheduleItem(
            id = "s3",
            name = "收到银行短信时提醒我",
            cron = "短信触发",
            kind = ScheduleKind.TRIGGER,
            enabled = false,
            nextRunLabel = "已停用",
            taskPrompt = "识别到银行/扣款类短信时，提取金额和商户，记一笔账。"
        )
    )
}
