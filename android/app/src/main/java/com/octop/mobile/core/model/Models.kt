package com.octop.mobile.core.model

/** 任务状态机：一个任务的完整生命周期 —— 对应 OpenMuse 的 Activity 语义 */
enum class TaskStatus(val label: String) {
    QUEUED("排队"),
    PLANNING("规划中"),
    RUNNING("执行中"),
    WAITING_APPROVAL("等你确认"),
    DONE("已完成"),
    FAILED("失败"),
    PAUSED("已暂停")
}

/** 任务里的一个步骤（plan） */
data class TaskStep(
    val title: String,
    val status: StepStatus = StepStatus.PENDING,
    val detail: String? = null
)

enum class StepStatus(val label: String) {
    PENDING("待执行"),
    RUNNING("进行中"),
    DONE("完成"),
    FAILED("失败"),
    SKIPPED("跳过")
}

/** 需要人拍板才能继续的操作 */
data class Approval(
    val id: String,
    val action: String,          // 例如 "拨打电话给 张伟"
    val reason: String,          // 为什么需要这次
    val risk: RiskLevel = RiskLevel.MEDIUM,
    // 服务端 review_configs 允许的决策：approve / reject / edit / respond
    val allowed: List<String> = listOf("approve", "reject"),
    // 一次暂停可能同时压着多个 action_request，resume 时必须给等量的 decision
    val actionCount: Int = 1
)

enum class RiskLevel(val label: String) {
    LOW("低"), MEDIUM("中"), HIGH("高")
}

/** 回执：这次到底做了什么（可审计） */
data class Receipt(
    val tool: String,            // phone.shell / screenshot / ui_dump / sms.read ...
    val summary: String,
    val atMillis: Long
)

data class OctopTask(
    val id: String,
    val title: String,
    val summary: String,
    val status: TaskStatus,
    val progress: Float = 0f,
    val expertId: String,
    val steps: List<TaskStep> = emptyList(),
    val approval: Approval? = null,
    val receipts: List<Receipt> = emptyList(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

/** 专家 = 一个被配置好的 agent 人格 + 工具白名单 */
data class Expert(
    val id: String,
    val name: String,
    val role: String,
    val description: String,
    val enabledTools: List<String> = emptyList(),
    val online: Boolean = true,
    val color: Long = 0xFF7C6CFF,
    /**
     * 本体 §pages/Experts/components/iconForName.tsx 的图标名（如 "cpu" / "sparkles"）。
     * 后端 /api/agents 的 `icon_name` 字段；查不到就画 layers，和本体兜底一致。
     */
    val iconName: String? = null,
    /** 后端返回的远程头像地址（市场/上传图）；非空时优先于 iconName 显示成图片。 */
    val iconUrl: String? = null
)

/** 资料库条目：记忆 / 笔记 / 知识 */
enum class LibraryKind(val label: String) {
    MEMORY("记忆"),
    NOTE("笔记"),
    KNOWLEDGE("知识"),
    PERSON("人物")
}

data class LibraryItem(
    val id: String,
    val title: String,
    val snippet: String,
    val kind: LibraryKind,
    val tags: List<String> = emptyList(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

/** 定时任务 */
enum class ScheduleKind(val label: String) {
    CRON("周期"),
    ONCE("一次"),
    TRIGGER("触发")
}

data class ScheduleItem(
    val id: String,
    val name: String,
    val cron: String,            // 人类可读的周期描述
    val kind: ScheduleKind,
    val enabled: Boolean = true,
    val nextRunLabel: String = "",
    val taskPrompt: String = ""
)

/** 记忆账本：agent 到底记住了多少东西 */
data class MemoryStat(
    val rawEvents: Int = 0,      // 原始事件
    val atoms: Int = 0,          // 提炼出的记忆原子
    val entities: Int = 0,       // 实体（人物/项目/概念）
    val episodes: Int = 0,       // 事件片段
    val pending: Int = 0,        // 待确认候选
    val atomsDelta7d: Int = 0    // 近 7 天新增
) {
    val total: Int get() = atoms + entities + episodes
}

/**
 * 专家详情：一个 agent 身上挂着的一切。
 * 对应 GET /api/agents/{id} + /status + /memory/stats/counts + /tool-settings 的合并视图。
 */
data class ExpertDetail(
    val id: String,
    val name: String,
    val description: String,
    val persona: String = "",           // MBTI
    val model: String = "",
    val state: String = "",             // running / stopped
    val visibility: String = "",
    val welcome: String = "",
    val iconUrl: String = "",
    val knowledgeBaseIds: List<String> = emptyList(),
    val skillPackageIds: List<String> = emptyList(),
    val mcpServers: List<String> = emptyList(),
    val memory: MemoryStat = MemoryStat(),
    val channels: List<String> = emptyList(),
    val cronJobs: List<String> = emptyList(),
    val tools: List<String> = emptyList(),  // 已启用的工具名
    val memoryDoc: String = "",             // MEMORY.md 全文
    val userDoc: String = ""                // USER.md 全文
)

/** 手机能力通道的连通状态 */
enum class ChannelState(val label: String) {
    NOT_GRANTED("未授权"),
    READY("已就绪"),
    DEGRADED("受限"),
    ERROR("异常")
}

data class ChannelStatus(
    val name: String,
    val state: ChannelState,
    val detail: String,
    val granted: Boolean
)
