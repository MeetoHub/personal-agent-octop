package com.octop.mobile.core.tools

import android.content.Context
import com.octop.mobile.core.model.ChannelState
import com.octop.mobile.core.model.ChannelStatus

/**
 * Shizuku 通道 —— 真机主力。
 *
 * ⚠️ 本文件是**预留骨架**，默认不参与编译失败：它不引用任何 Shizuku 类，
 * 只把接入点标出来。要用时按下面三步打开：
 *
 * 1) app/build.gradle.kts 加依赖：
 *      implementation("dev.rikka.shizuku:api:13.1.5")
 *      implementation("dev.rikka.shizuku:provider:13.1.5")
 *
 * 2) AndroidManifest 里注册 provider（Shizuku 官方文档有原文）：
 *      <provider
 *          android:name="rikka.shizuku.ShizukuProvider"
 *          android:authorities="${applicationId}.shizuku"
 *          android:multiprocess="false"
 *          android:enabled="true"
 *          android:exported="true"
 *          android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" />
 *
 * 3) 把下面 TODO 处换成真实调用：
 *      Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
 *      Shizuku.newProcess(arrayOf("sh","-c",cmd), null, null)
 *      Shizuku.getSystemService(...) / 或用 Shizuku 以 shell uid 跑 screencap、uiautomator dump
 *
 * 授权方式：手机装 Shizuku App → 无线调试配对 → 本 App 请求授权 → 一次授权长期有效。
 *
 * 想省掉这一步的话：直接用 stixez/droid-mcp 的 droid-mcp-shizuku 模块，
 * 它已经把上面的流程封装好了，只需实现本接口转发给它（见 docs/接入-droid-mcp.md）。
 */
class ShizukuChannel(private val context: Context) : PhoneChannel {

    override val id: String = "shizuku"
    override val displayName: String = "Shizuku shell"

    override suspend fun status(): ChannelStatus = ChannelStatus(
        name = displayName,
        state = ChannelState.NOT_GRANTED,
        detail = "Shizuku 未接入（按 ShizukuChannel.kt 顶部注释三步打开）",
        granted = false
    )

    override suspend fun shell(cmd: String, timeoutMs: Long): ToolResult =
        ToolResult.DeniedWith("Shizuku 未接入")

    override suspend fun screenshot(): ToolResult =
        ToolResult.DeniedWith("Shizuku 未接入")

    override suspend fun uiDump(): ToolResult =
        ToolResult.DeniedWith("Shizuku 未接入")
}
