package com.octop.mobile.core.tools

import com.octop.mobile.core.model.ChannelStatus

/**
 * 万能通道 —— 整个 App 只认这一个接口。
 *
 * 设计依据：不为每个 App 写适配，只暴露原子能力。
 *   shell      → 执行任意命令（读通知、读短信、拉起 App、点按……都由它派生）
 *   screenshot → 截屏
 *   uiDump     → 读当前界面元素树
 *
 * 谁来实现它，上层不关心：
 *   - [ShizukuChannel]   真机推荐，一次 ADB 授权，长期 shell
 *   - [LocalToolChannel] 无 Shizuku 时的降级实现（只能跑本进程权限内的命令）
 *   - 接 droid-mcp 时，再写一个 DroidMcpChannel 实现本接口即可
 */
interface PhoneChannel {

    /** 通道标识，用于日志与回执 */
    val id: String

    /** 给人看的名字，例如 "Shizuku shell" */
    val displayName: String

    /** 当前是否可用、缺什么权限 */
    suspend fun status(): ChannelStatus

    /** 执行一条 shell 命令 */
    suspend fun shell(cmd: String, timeoutMs: Long = 15_000L): ToolResult

    /**
     * 截屏，成功时 result 里带文件绝对路径。
     * 上层可以直接把路径丢给 Octop 做多模态识别。
     */
    suspend fun screenshot(): ToolResult

    /** 读当前界面的元素树（JSON 字符串） */
    suspend fun uiDump(): ToolResult
}

sealed interface ToolResult {
    data class Ok(val output: String) : ToolResult
    data object Denied : ToolResult
    data class DeniedWith(val reason: String) : ToolResult
    data class Failure(val message: String) : ToolResult

    val isOk: Boolean get() = this is Ok

    fun display(): String = when (this) {
        is Ok -> output
        Denied -> "未授权"
        is DeniedWith -> "未授权：$reason"
        is Failure -> "失败：$message"
    }
}
