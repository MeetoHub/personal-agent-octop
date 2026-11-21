package com.octop.mobile.core.tools

import android.content.Context
import com.octop.mobile.core.model.ChannelState
import com.octop.mobile.core.model.ChannelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 降级实现：不需要 Shizuku，无需 root。
 *
 * 能干什么：跑本进程 uid 权限内的命令（如 `id`、`getprop`、`pm list packages` 的一部分）。
 * 不能干什么：读别的 App 的私有数据、模拟点按、截其他 App 的屏。
 *
 * 它的存在意义是「装上就能跑通链路」，让 UI / 同步层先联调起来，
 * 真机授权 Shizuku 后换 [ShizukuChannel] 即可，上层一行不用改。
 */
class LocalToolChannel(private val context: Context) : PhoneChannel {

    override val id: String = "local"
    override val displayName: String = "本地 shell（降级）"

    override suspend fun status(): ChannelStatus = ChannelStatus(
        name = displayName,
        state = ChannelState.DEGRADED,
        detail = "未接入 Shizuku，仅能执行本应用权限内的命令",
        granted = false
    )

    override suspend fun shell(cmd: String, timeoutMs: Long): ToolResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val process = ProcessBuilder("sh", "-c", cmd)
                    .redirectErrorStream(true)
                    .start()

                val finished = waitWithTimeout(process, timeoutMs)
                if (!finished) {
                    process.destroy()
                    return@runCatching ToolResult.Failure("命令超时（${timeoutMs}ms）：$cmd")
                }

                val out = process.inputStream.bufferedReader().use { it.readText() }
                val code = process.exitValue()
                if (code == 0) ToolResult.Ok(out.ifBlank { "(无输出)" })
                else ToolResult.Failure("退出码 $code：${out.take(500)}")
            }.getOrElse { ToolResult.Failure(it.message ?: "执行失败") }
        }

    override suspend fun screenshot(): ToolResult = ToolResult.DeniedWith(
        "截屏需要 MediaProjection 授权或 Shizuku"
    )

    override suspend fun uiDump(): ToolResult = ToolResult.DeniedWith(
        "读界面树需要无障碍服务或 Shizuku"
    )

    private fun waitWithTimeout(process: Process, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive) return true
            Thread.sleep(40)
        }
        return !process.isAlive
    }
}
