package com.octop.mobile.core.tools

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 通道注册表：App 里所有"动手"的动作都必须经过它。
 * 好处是换实现只改一行，UI 与同步层完全解耦。
 */
object ToolRegistry {

    private lateinit var appContext: Context

    private val _active = MutableStateFlow<PhoneChannel?>(null)
    val active: StateFlow<PhoneChannel?> = _active.asStateFlow()

    private val _channels = MutableStateFlow<List<PhoneChannel>>(emptyList())
    val channels: StateFlow<List<PhoneChannel>> = _channels.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext

        val local = LocalToolChannel(appContext)
        val shizuku = ShizukuChannel(appContext)
        _channels.value = listOf(shizuku, local)
        // 暂时用降级通道；Shizuku 打通后把这里换成 shizuku
        _active.value = local
    }

    fun switchTo(channelId: String) {
        _channels.value.firstOrNull { it.id == channelId }?.let { _active.value = it }
    }

    /** 高频快捷封装：只在这里写，别散落到各处 */
    suspend fun exec(cmd: String): ToolResult =
        _active.value?.shell(cmd) ?: ToolResult.Failure("通道未初始化")

    suspend fun shot(): ToolResult =
        _active.value?.screenshot() ?: ToolResult.Failure("通道未初始化")

    suspend fun dumpUi(): ToolResult =
        _active.value?.uiDump() ?: ToolResult.Failure("通道未初始化")
}
