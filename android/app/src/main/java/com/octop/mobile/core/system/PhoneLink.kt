package com.octop.mobile.core.system

import android.content.Context
import android.os.Build
import android.util.Log
import com.octop.mobile.core.auth.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** 反向通道的连接状态，给设置页和常驻通知用。 */
enum class PhoneLinkState { IDLE, CONNECTING, ONLINE, OFFLINE }

/**
 * 手机主动连 NAS 的反向 WebSocket（A 方案）。
 *
 * 方向：手机 → NAS。手机永远不需要对外暴露端口，所以 NAT、休眠换网、IP 漂移都不影响；
 * 断线后按 1/2/5/10/30/60 秒退避重连。
 *
 * 协议（NAS 侧 `infra/phone/protocol.py`）：
 *   上：{"type":"hello","device":{...},"capabilities":[...]}
 *       {"type":"tool_result","reqId":"..","ok":true,"data":{...}}
 *       {"type":"pong"}
 *   下：{"type":"hello_ok","protocol":1,"tools":[...]}
 *       {"type":"tool_call","reqId":"..","tool":"contacts.search","args":{}}
 *       {"type":"ping"} {"type":"error","message":".."}
 */
object PhoneLink {

    private const val TAG = "PhoneLink"
    private const val PATH = "/api/phone/ws"
    private val BACKOFF_S = listOf(1, 2, 5, 10, 30, 60)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val _state = MutableStateFlow(PhoneLinkState.IDLE)
    val state: StateFlow<PhoneLinkState> = _state

    private var appCtx: Context? = null
    private var socket: WebSocket? = null
    private var retry: Job? = null
    private var running = false
    private var attempt = 0

    fun start(ctx: Context) {
        appCtx = ctx.applicationContext
        if (running) return
        running = true
        connect()
    }

    fun stop() {
        running = false
        retry?.cancel()
        retry = null
        socket?.close(1000, "bye")
        socket = null
        _state.value = PhoneLinkState.IDLE
    }

    /** 登录态变化后重连：用 App 启动时记下的 Context，ViewModel 里不用再传。 */
    fun restart() {
        val ctx = appCtx ?: return
        stop()
        start(ctx)
    }

    /** 常驻通知里显示的一句话 */
    fun statusText(): String = when (_state.value) {
        PhoneLinkState.ONLINE -> "反向通道已连接"
        PhoneLinkState.CONNECTING -> "反向通道连接中…"
        PhoneLinkState.OFFLINE -> "反向通道重连中…"
        PhoneLinkState.IDLE -> "反向通道未启动"
    }

    // ── 连接 ──────────────────────────────────────────────

    private fun connect() {
        if (!running) return
        val token = TokenStore.token()
        if (token.isNullOrBlank()) {
            // 没登录就没有端点和身份，等下次登录后 restart
            _state.value = PhoneLinkState.IDLE
            return
        }
        _state.value = PhoneLinkState.CONNECTING
        val request = runCatching { Request.Builder().url(wsUrl(TokenStore.endpoint(), token)).build() }
            .getOrElse {
                Log.w(TAG, "WS 地址不合法: ${it.message}")
                scheduleRetry()
                return
            }
        socket = http.newWebSocket(request, listener)
    }

    private fun wsUrl(endpoint: String, token: String): String {
        val base = when {
            endpoint.startsWith("https://") -> "wss://" + endpoint.removePrefix("https://")
            endpoint.startsWith("http://") -> "ws://" + endpoint.removePrefix("http://")
            else -> endpoint
        }.trimEnd('/')
        // 浏览器的 WebSocket 不能自定义 Header，NAS 侧也是从 query 里取 token
        return "$base$PATH?token=${URLEncoder.encode(token, "UTF-8")}"
    }

    private fun scheduleRetry() {
        if (!running) return
        _state.value = PhoneLinkState.OFFLINE
        socket = null
        retry?.cancel()
        val wait = BACKOFF_S[attempt.coerceAtMost(BACKOFF_S.lastIndex)]
        attempt += 1
        retry = scope.launch {
            delay(wait * 1000L)
            if (running) connect()
        }
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            attempt = 0
            _state.value = PhoneLinkState.ONLINE
            Log.i(TAG, "已连上 NAS")
            webSocket.send(hello().toString())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val frame = runCatching { JSONObject(text) }.getOrNull() ?: return
            when (frame.optString("type")) {
                "hello_ok" -> {
                    _state.value = PhoneLinkState.ONLINE
                    Log.i(TAG, "NAS 确认：protocol=${frame.optInt("protocol")}")
                }

                "tool_call" -> handleToolCall(webSocket, frame)

                "ping" -> webSocket.send("""{"type":"pong"}""")

                "error" -> Log.w(TAG, "NAS 报错: ${frame.optString("message")}")

                else -> Unit
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "连接关闭 code=$code $reason")
            scheduleRetry()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "连接失败: ${t.message}")
            scheduleRetry()
        }
    }

    private fun hello(): JSONObject {
        val device = JSONObject()
            .put("model", Build.MODEL)
            .put("brand", Build.BRAND)
            .put("android", Build.VERSION.SDK_INT)
            .put("app", "octop-mobile")
        val caps = JSONArray().apply { PhoneTools.SUPPORTED.forEach { put(it) } }
        return JSONObject()
            .put("type", "hello")
            .put("device", device)
            .put("capabilities", caps)
    }

    // ── 工具调用 ──────────────────────────────────────────

    private fun handleToolCall(webSocket: WebSocket, frame: JSONObject) {
        val reqId = frame.optString("reqId")
        val tool = frame.optString("tool")
        val args = frame.optJSONObject("args") ?: JSONObject()
        val ctx = appCtx
        if (reqId.isBlank()) return

        scope.launch {
            val result = if (ctx == null) {
                JSONObject().put("ok", false).put("error", "no_context")
            } else {
                // ContentResolver 查询会阻塞，别占着 OkHttp 的回调线程
                withContext(Dispatchers.IO) { PhoneTools.call(ctx, tool, args) }
            }
            val reply = JSONObject()
                .put("type", "tool_result")
                .put("reqId", reqId)
            if (result.optBoolean("ok")) {
                reply.put("ok", true).put("data", result.opt("data") ?: JSONObject())
            } else {
                reply.put("ok", false).put("error", result.optString("error", "error"))
            }
            runCatching { webSocket.send(reply.toString()) }
                .onFailure { Log.w(TAG, "回传 $tool 失败: ${it.message}") }
        }
    }
}
