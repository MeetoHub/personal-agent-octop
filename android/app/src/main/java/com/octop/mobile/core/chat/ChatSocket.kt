package com.octop.mobile.core.chat

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** 对话流里可能出现的事件。结构对不上的一律降级成 [Raw]，保证不丢数据。 */
sealed interface ChatEvent {
    data class Text(val text: String) : ChatEvent
    data class Thinking(val text: String) : ChatEvent
    data class Tool(val name: String, val detail: String?) : ChatEvent
    data class Approval(val action: String, val risk: String) : ChatEvent
    /** 服务端给这次对话落了 thread_id —— 新会话发第一句之前客户端并不知道自己的 id */
    data class Bound(val threadId: String) : ChatEvent
    data object Done : ChatEvent
    data class Failure(val message: String) : ChatEvent
    data class Raw(val json: String) : ChatEvent
}

enum class SocketState { IDLE, CONNECTING, OPEN, CLOSED, FAILED }

/**
 * 一条对话 WS。协议来自 octop/api/routers/chat/ws.py：
 *
 *   发：{"type":"user_turn","text":"..","thread_id":".."}
 *       {"type":"ping"} {"type":"cancel","thread_id":".."} {"type":"subscribe","thread_id":".."}
 *   收：{"type":"pong"} / {"type":"error",..} / {"type":"done"} / {"type":"turn_status",..}
 *       + harness stream chunks（text / thinking / tool_use / tool_result）
 */
class ChatSocket(
    private val urlProvider: () -> String,
    private val onEvent: (ChatEvent) -> Unit,
    private val onState: (SocketState) -> Unit
) {

    companion object {
        private const val TAG = "ChatSocket"
        private const val NORMAL = 1000
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var socket: WebSocket? = null
    private var heartbeat: Job? = null
    private var retryJob: Job? = null
    private var attempt = 0
    private var closedByUs = false

    val isOpen: Boolean get() = socket != null

    fun connect() {
        if (socket != null) return
        closedByUs = false
        onState(SocketState.CONNECTING)

        val req = Request.Builder().url(urlProvider()).build()
        socket = http.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                attempt = 0
                onState(SocketState.OPEN)
                heartbeat?.cancel()
                heartbeat = appScope.launch {
                    while (true) {
                        delay(25_000)
                        runCatching { webSocket.send("""{"type":"ping"}""") }
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                onEvent(parse(text))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "ws 断开: ${t.message}")
                teardown()
                onState(SocketState.FAILED)
                if (!closedByUs) scheduleReconnect()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(NORMAL, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                teardown()
                onState(SocketState.CLOSED)
                // 4001 = token 失效，不重连，交给 UI 跳登录
                if (!closedByUs && code != 4001) scheduleReconnect()
            }
        })
    }

    /** 发一句话。threadId 为空时本体自己开新线程。 */
    fun sendTurn(text: String, threadId: String?, model: String? = null) {
        val payload = JSONObject().apply {
            put("type", "user_turn")
            put("text", text)
            if (!threadId.isNullOrBlank()) put("thread_id", threadId)
            if (!model.isNullOrBlank()) put("model", model)
        }
        val ok = socket?.send(payload.toString()) ?: false
        if (!ok) {
            onEvent(ChatEvent.Failure("连接已断开，消息没发出去"))
        }
    }

    fun cancel(threadId: String) {
        socket?.send("""{"type":"cancel","thread_id":"$threadId"}""")
    }

    fun subscribe(threadId: String) {
        socket?.send("""{"type":"subscribe","thread_id":"$threadId"}""")
    }

    fun close() {
        closedByUs = true
        retryJob?.cancel()
        teardown()
        onState(SocketState.CLOSED)
    }

    private fun teardown() {
        heartbeat?.cancel()
        heartbeat = null
        runCatching { socket?.close(NORMAL, null) }
        socket = null
    }

    private fun scheduleReconnect() {
        retryJob?.cancel()
        retryJob = appScope.launch {
            attempt++
            val wait = (1_000L shl attempt.coerceAtMost(4)).coerceAtMost(30_000L)
            delay(wait)
            if (!closedByUs) connect()
        }
    }

    // ── 帧解析：字段名对不上就退回 Raw，不猜 ────────────────────

    private fun parse(raw: String): ChatEvent {
        if (raw.isBlank()) return ChatEvent.Raw(raw)
        val o = runCatching { JSONObject(raw) }.getOrNull() ?: return ChatEvent.Raw(raw)

        // 任何一帧只要带了 thread_id 就记住，不依赖具体 type：
        // 新会话第一句发出去之前客户端不知道自己的 thread_id，只能等服务端回传。
        val tid = o.optString("thread_id").takeIf { it.isNotBlank() && it != "null" }
            ?: o.optJSONObject("turn")?.optString("thread_id")
                ?.takeIf { it.isNotBlank() && it != "null" }
        if (tid != null) onEvent(ChatEvent.Bound(tid))

        val type = o.optString("type")

        return when (type) {
            "pong", "" -> ChatEvent.Raw(raw)

            "done" -> ChatEvent.Done

            "error" -> ChatEvent.Failure(
                o.optString("message").ifBlank { "出错了" }
            )

            "turn_status", "turn.busy", "steer.discarded" -> ChatEvent.Raw(raw)

            "thinking", "reasoning" -> ChatEvent.Thinking(
                firstNonBlank(o, "thinking", "text", "delta", "content")
            )

            "tool_use", "tool_call" -> ChatEvent.Tool(
                name = firstNonBlank(o, "name", "tool", "tool_name"),
                detail = o.optJSONObject("input")?.toString() ?: o.optString("input").takeIf { it.isNotBlank() }
            )

            "tool_result" -> ChatEvent.Tool(
                name = firstNonBlank(o, "name", "tool", "tool_name"),
                detail = firstNonBlank(o, "output", "content", "result").takeIf { it.isNotBlank() }
            )

            "approval", "hitl", "hitl_request" -> ChatEvent.Approval(
                action = firstNonBlank(o, "action", "title", "message", "text"),
                risk = o.optString("risk").ifBlank { "medium" }
            )

            "text", "delta", "text_delta", "chunk", "content_block_delta" -> {
                val t = firstNonBlank(o, "text", "delta", "content")
                if (t.isBlank()) ChatEvent.Raw(raw) else ChatEvent.Text(t)
            }

            else -> {
                // 未知 type，但里面可能有能显示的文字
                val t = firstNonBlank(o, "text", "delta", "content")
                if (t.isNotBlank()) ChatEvent.Text(t) else ChatEvent.Raw(raw)
            }
        }
    }

    private fun firstNonBlank(o: JSONObject, vararg keys: String): String {
        for (k in keys) {
            val v = o.optString(k)
            if (v.isNotBlank() && v != "null") return v
        }
        return ""
    }
}
