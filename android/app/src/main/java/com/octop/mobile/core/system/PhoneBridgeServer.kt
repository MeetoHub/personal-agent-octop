package com.octop.mobile.core.system

import android.content.Context
import android.os.Build
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject

/**
 * 把手机能力暴露成局域网 HTTP 接口，给 NAS 上的 agent 用。
 *
 * 为什么是 HTTP 而不是 MCP：Octop 的 MCP 客户端只支持 stdio（在 NAS 上 spawn 子进程），
 * 连不到手机。所以这里退一步，手机当 server，NAS 上的 MCP 脚本当 client 转发过去。
 *
 * 安全：
 *  - 必须带 token（?token= 或 X-Bridge-Token 头），否则 401。同 WiFi 下别人也能扫到端口，
 *    短信是敏感数据，不能裸奔。
 *  - 只读接口（contacts / sms / calendar）默认开；写操作（alarm）也走同一个 token。
 */
class PhoneBridgeServer(
    private val ctx: Context,
    private val tokenProvider: () -> String,
    port: Int = DEFAULT_PORT
) : NanoHTTPD(port) {

    companion object {
        const val DEFAULT_PORT = 8765
        private const val TAG = "PhoneBridge"
    }

    val port: Int get() = listeningPort

    fun startServer() {
        if (isAlive) return
        runCatching { start(SOCKET_READ_TIMEOUT, false) }
            .onSuccess { Log.i(TAG, "bridge 已启动 :$listeningPort") }
            .onFailure { Log.w(TAG, "bridge 启动失败: ${it.message}") }
    }

    fun stopServer() {
        runCatching { stop() }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri.trimEnd('/').ifBlank { "/" }
        val params = session.parameters

        if (!tokenOk(params["token"]?.firstOrNull() ?: session.headers["x-bridge-token"])) {
            return json(Response.Status.UNAUTHORIZED, error("bad token"))
        }

        return try {
            when (uri) {
                "/", "/health" -> json(Response.Status.OK, health())

                "/contacts" -> {
                    val q = params["q"]?.firstOrNull()
                    val limit = params["limit"]?.firstOrNull()?.toIntOrNull()?.coerceIn(1, 200) ?: 50
                    val arr = JSONArray()
                    SystemBridge.contacts(ctx, q, limit).forEach {
                        arr.put(JSONObject().put("name", it.name).put("phone", it.phone))
                    }
                    json(Response.Status.OK, JSONObject().put("contacts", arr))
                }

                "/sms" -> {
                    val limit = params["limit"]?.firstOrNull()?.toIntOrNull()?.coerceIn(1, 200) ?: 30
                    val arr = JSONArray()
                    SystemBridge.recentSms(ctx, limit).forEach {
                        arr.put(
                            JSONObject()
                                .put("address", it.address)
                                .put("body", it.body)
                                .put("at", it.atMillis)
                                .put("incoming", it.incoming)
                        )
                    }
                    json(Response.Status.OK, JSONObject().put("messages", arr))
                }

                "/calendar" -> {
                    val days = params["days"]?.firstOrNull()?.toIntOrNull()?.coerceIn(1, 365) ?: 7
                    val arr = JSONArray()
                    SystemBridge.upcomingEvents(ctx, days).forEach {
                        arr.put(
                            JSONObject()
                                .put("title", it.title)
                                .put("start", it.startMillis)
                                .put("end", it.endMillis)
                                .put("location", it.location ?: "")
                        )
                    }
                    json(Response.Status.OK, JSONObject().put("events", arr))
                }

                // 给 agent 一眼读完的纯文本，省得它自己拼
                "/digest" -> {
                    val text = buildString {
                        appendLine("## 联系人")
                        appendLine(SystemBridge.contactsDigest(ctx))
                        appendLine()
                        appendLine("## 最近短信")
                        appendLine(SystemBridge.smsDigest(ctx))
                        appendLine()
                        appendLine("## 未来 7 天日程")
                        appendLine(SystemBridge.calendarDigest(ctx))
                    }
                    json(Response.Status.OK, JSONObject().put("text", text))
                }

                "/alarm" -> {
                    val h = params["h"]?.firstOrNull()?.toIntOrNull()
                    val m = params["m"]?.firstOrNull()?.toIntOrNull()
                    val label = params["label"]?.firstOrNull()
                    val inMin = params["in_minutes"]?.firstOrNull()?.toIntOrNull()

                    // 支持「20 分钟后」这种相对说法，agent 更容易用
                    val (hh, mm) = when {
                        inMin != null -> SystemBridge.inMinutes(inMin)
                        h != null && m != null -> h to m
                        else -> return json(
                            Response.Status.BAD_REQUEST,
                            error("需要 h&m 或 in_minutes")
                        )
                    }
                    val ok = SystemBridge.setAlarm(ctx, hh, mm, label)
                    json(
                        if (ok) Response.Status.OK else Response.Status.INTERNAL_ERROR,
                        JSONObject().put("ok", ok).put("hour", hh).put("minute", mm)
                    )
                }

                else -> json(Response.Status.NOT_FOUND, error("unknown path $uri"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "处理 $uri 失败: ${e.message}")
            json(Response.Status.INTERNAL_ERROR, error(e.message ?: "internal error"))
        }
    }

    private fun health(): JSONObject = JSONObject()
        .put("ok", true)
        .put("app", "octop-mobile")
        .put("device", Build.MODEL)
        .put("android", Build.VERSION.SDK_INT)
        .put("can_read_contacts", SystemBridge.has(ctx, SystemBridge.REQUIRED[0].first))
        .put("can_read_sms", SystemBridge.has(ctx, SystemBridge.REQUIRED[1].first))
        .put("can_read_calendar", SystemBridge.has(ctx, SystemBridge.REQUIRED[2].first))

    private fun tokenOk(given: String?): Boolean {
        val expected = tokenProvider()
        if (expected.isBlank()) return false
        if (given.isNullOrBlank()) return false
        // 长度不同直接失败，长度相同再逐字节比，避免一眼看出前缀对不对
        if (given.length != expected.length) return false
        var diff = 0
        for (i in expected.indices) diff = diff or (expected[i].code xor given[i].code)
        return diff == 0
    }

    private fun json(status: Response.Status, body: JSONObject): Response =
        newFixedLengthResponse(status, "application/json; charset=utf-8", body.toString())

    private fun error(msg: String): JSONObject =
        JSONObject().put("ok", false).put("error", msg)
}
