package com.octop.mobile.core.sync

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 与 NAS 上 Octop 本体通信的客户端。端点全部来自 octop/api/routers 的实际挂载：
 *
 *   POST /api/auth/login                 账号密码登录 → access token
 *   GET  /api/health                     探活（无需 token）
 *   GET  /api/agents                     agent 列表
 *   GET  /api/agents/{id}/threads        线程列表
 *   GET  /api/agents/{id}/threads/{tid}/history
 *   GET  /api/agents/{id}/chat/welcome   开场快捷卡
 *   POST /api/agents/{id}/chat/hitl/resume  人工审批后继续（SSE）
 *   WS   /api/agents/{id}/chat/ws        对话流
 *   WS   /api/notifications/ws           全局通知
 *
 * 刻意只用 HttpURLConnection + org.json：不引 OkHttp / Retrofit，少两个依赖少两个坑。
 */
class OctopClient(
    private var baseUrl: String = DEFAULT_BASE_URL,
    private var token: String = ""
) {

    companion object {
        private const val TAG = "OctopClient"

        /** 局域网直连；换成 NAS 地址或 Tailscale 主机名。端口就是本体的 8089。 */
        const val DEFAULT_BASE_URL = "http://192.168.1.100:8089"
    }

    data class Response(val code: Int, val body: String) {
        val ok: Boolean get() = code in 200..299
    }

    fun updateEndpoint(url: String, token: String) {
        this.baseUrl = Endpoints.normalize(url)
        this.token = token
    }

    /**
     * access token 过期（HTTP 401）时回调。
     *
     * 为什么要这个：token 只有 24h，过期后所有接口都 401，而调用方多半用
     * `takeIf { it.ok }` 把失败吞成空列表 —— 用户看到的是「页面空了」，
     * 以为是数据丢了，其实只是该重新登录。这里统一兜住，交给 ViewModel 登出。
     *
     * 注意：request() 跑在 Dispatchers.IO，回调也在 IO 线程上触发，
     * 接的一方不能直接改 Compose 状态，必须切回主线程。
     */
    var onAuthExpired: (() -> Unit)? = null

    // ── 鉴权 ──────────────────────────────────────────────

    /** 账号密码登录。body: {"username":..,"password":..} */
    suspend fun login(username: String, password: String): Response =
        request(
            "POST", "/api/auth/login",
            """{"username":${q(username)},"password":${q(password)}}"""
        )

    // ── 探活（无需 token，返回 {"ok":true,...}）────────────

    suspend fun ping(): Response = request("GET", "/api/health")

    // ── agent / 会话 ──────────────────────────────────────

    suspend fun agents(): Response = request("GET", "/api/agents")

    suspend fun threads(agentId: String): Response =
        request("GET", "/api/agents/$agentId/threads")

    suspend fun newThread(agentId: String, title: String = "新对话"): Response =
        request("POST", "/api/agents/$agentId/threads", """{"title":${q(title)}}""")

    /**
     * 历史消息。服务端 HISTORY_DEFAULT_LIMIT=25 且硬上限 200（传 500 也只回 200）。
     * 会话尾部常常全是 tool_result / thinking，这些在 App 侧会被过滤掉，
     * 所以默认直接拉满 200，否则页面上什么都剩不下。
     */
    suspend fun history(
        agentId: String,
        threadId: String,
        limit: Int = 200,
        offset: Int = 0,
    ): Response =
        request("GET", "/api/agents/$agentId/threads/$threadId/history?limit=$limit&offset=$offset")

    suspend fun welcome(agentId: String): Response =
        request("GET", "/api/agents/$agentId/chat/welcome")

    /** 资料库：knowledge_bases.py::list_bases，返回 list[dict] */
    suspend fun knowledgeBases(): Response = request("GET", "/api/knowledge-bases")

    /** 定时任务：cron.py::list_cron，按 agent 维度 */
    suspend fun cronJobs(agentId: String): Response =
        request("GET", "/api/agents/$agentId/cron")

    /** 专家详情：agent 本体（人格 / 模型 / 挂载的知识库与技能包） */
    suspend fun agentDetail(agentId: String): Response = request("GET", "/api/agents/$agentId")

    /** 运行时状态：通道 + 定时任务 */
    suspend fun agentStatus(agentId: String): Response =
        request("GET", "/api/agents/$agentId/status")

    /** 记忆账本：原子 / 实体 / 片段 / 待确认 */
    suspend fun memoryStats(agentId: String): Response =
        request("GET", "/api/agents/$agentId/memory/stats/counts")

    /** 工具清单，含每个工具的启用状态 */
    suspend fun toolSettings(agentId: String): Response =
        request("GET", "/api/agents/$agentId/tool-settings")

    /** 强记忆：主人手写的 MEMORY.md */
    suspend fun strongMemory(agentId: String): Response =
        request("GET", "/api/agents/$agentId/memory/strong")

    /** 用户画像：主人手写的 USER.md */
    suspend fun userProfile(agentId: String): Response =
        request("GET", "/api/agents/$agentId/memory/user")

    /**
     * 审批后继续。本体的入参是 {thread_id, decisions:[{type:"approve"|"reject"}]}，
     * 这里按 thread 粒度走——审批天然是「这个线程里那一步要不要放行」。
     */
    suspend fun resumeApproval(
        agentId: String,
        threadId: String,
        allow: Boolean,
        note: String? = null,
        count: Int = 1
    ): Response {
        val one = if (allow) {
            """{"type":"approve"}"""
        } else if (note.isNullOrBlank()) {
            """{"type":"reject"}"""
        } else {
            """{"type":"reject","message":${q(note)}}"""
        }
        // decisions 条数必须等于这次暂停的 action_requests 条数，少一条服务端直接 400
        val decisions = (1..count.coerceAtLeast(1)).joinToString(",") { one }
        return request(
            "POST", "/api/agents/$agentId/chat/hitl/resume",
            """{"thread_id":${q(threadId)},"decisions":[$decisions]}"""
        )
    }

    // ── WS 地址 ───────────────────────────────────────────

    /** 对话流。token 走 query——浏览器 WebSocket 不能自定义 Header，本体也是这么收的。 */
    fun chatWsUrl(agentId: String): String =
        httpToWs(baseUrl) + "/api/agents/$agentId/chat/ws" + tokenQuery()

    /** 全局通知（审批请求、任务完成都从这里推） */
    fun notifyWsUrl(): String =
        httpToWs(baseUrl) + "/api/notifications/ws" + tokenQuery()

    private fun tokenQuery(): String = if (token.isBlank()) "" else "?token=${enc(token)}"

    private fun httpToWs(u: String): String = when {
        u.startsWith("https://") -> "wss://" + u.removePrefix("https://")
        u.startsWith("http://") -> "ws://" + u.removePrefix("http://")
        else -> u
    }

    // ── 图片 ──────────────────────────────────────────────

    /**
     * 取一张需要鉴权的图片（专家头像）。
     *
     * 为什么不能直接把 icon_url 丢给图片库：本体 `experts/avatar.py:78`
     * `display_agent_icon_url()` 返回的是**相对路径**（`/api/agents/{id}/avatar?...`），
     * 且该接口要求 Bearer 令牌。Compose 自带的东西不会替我们补 baseUrl 和 Header，
     * 所以这里复用同一套连接与鉴权，吐字节流。
     *
     * @param raw icon_url 原值，绝对 http(s) 地址或站内相对路径都吃。
     * @return 图片字节；失败返回 null（调用方退回矢量图标）。
     */
    suspend fun fetchImageBytes(raw: String): ByteArray? = withContext(Dispatchers.IO) {
        val path = raw.trim()
        if (path.isEmpty()) return@withContext null
        val full = when {
            path.startsWith("http://") || path.startsWith("https://") -> path
            path.startsWith("/") -> baseUrl + path
            else -> baseUrl + "/" + path
        }
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(full).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 20_000
                setRequestProperty("Accept", "image/*")
                if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
            }
            if (conn.responseCode !in 200..299) {
                Log.w(TAG, "取图失败 ${conn.responseCode} $full")
                return@withContext null
            }
            conn.inputStream.use { it.readBytes() }
        } catch (e: Exception) {
            Log.w(TAG, "取图异常 $full: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    // ── 底层 ──────────────────────────────────────────────

    /**
     * 底层请求。internal 是为了让 OctopApi（全量接口层）复用同一套连接与鉴权，
     * 不重复造 HttpURLConnection。
     */
    internal suspend fun request(method: String, path: String, body: String? = null): Response =
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL(baseUrl + path)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 8_000
                    readTimeout = 20_000
                    setRequestProperty("Accept", "application/json")
                    if (token.isNotBlank()) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    }
                }
                if (body != null) {
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                // 401 = 令牌过期或被撤。登录和探活自己就是 401 的合法来源，必须排除，
                // 否则输错一次密码就会被判成「会话过期」。没带 token 时也不触发：
                // 那时本来就没登录，没必要再走一遍登出。
                if (code == 401 && token.isNotBlank() && !isAuthExpiryExempt(path)) {
                    onAuthExpired?.invoke()
                }
                Response(code, text)
            } catch (e: Exception) {
                Log.w(TAG, "请求失败 $method $path: ${e.message}")
                Response(-1, e.message ?: "网络错误")
            } finally {
                conn?.disconnect()
            }
        }

    /**
     * 这些路径上的 401 不算「会话过期」：
     * 登录失败返回 401（AUTH_FAILED），探活本来就不需要令牌。
     * 用前缀匹配，因为探活可能带 query 或落在 /api/health/ 子路径上。
     */
    private fun isAuthExpiryExempt(path: String): Boolean {
        val p = path.substringBefore('?')
        return p == "/api/auth/login" || p.startsWith("/api/health")
    }

    /** 极简 JSON 字符串转义 */    private fun q(s: String): String {
        val sb = StringBuilder("\"")
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch < ' ') sb.append("\\u%04x".format(ch.code)) else sb.append(ch)
            }
        }
        return sb.append('"').toString()
    }

    private fun enc(s: String): String =
        java.net.URLEncoder.encode(s, "UTF-8")
}
