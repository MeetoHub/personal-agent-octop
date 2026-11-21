package com.octop.mobile.core.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 连接状态：UI 顶部那个小圆点读的就是它 */
enum class LinkState { UNKNOWN, OK, FAIL, SYNCING }

data class LinkInfo(
    val state: LinkState = LinkState.UNKNOWN,
    val endpoint: String = OctopClient.DEFAULT_BASE_URL,
    val message: String = "未连接",
    val lastOkAtMillis: Long = 0L
)

/**
 * 同步仓库。第一版只做「探活 + 拉任务」两件事，
 * 等 NAS 那边端点定了再往里补。UI 不直接碰 OctopClient。
 */
object SyncRepository {

    private val client = OctopClient(
        baseUrl = OctopClient.DEFAULT_BASE_URL,
        token = ""
    )

    /**
     * 全量接口层（375 个方法，覆盖 agents / chat / memory / cron / experts /
     * knowledge-bases / skills / workspace / channels / connectors / settings …）。
     * 页面拿数据一律走这里，不要再往 OctopClient 里加单点方法。
     */
    val api: OctopApi = OctopApi(client)

    /**
     * 「我的」功能中心用到的补充接口（usage / admin / cloud / plugins / security）。
     * 与 api 共用同一个 client，鉴权和 401 处理走同一条路。
     */
    val hub: HubApi = HubApi(client)

    private val _link = MutableStateFlow(LinkInfo())
    val link: StateFlow<LinkInfo> = _link.asStateFlow()

    fun setEndpoint(url: String, token: String = "") {
        client.updateEndpoint(url, token)
        _link.value = _link.value.copy(endpoint = url, state = LinkState.UNKNOWN, message = "待连接")
    }

    suspend fun check(): LinkInfo {
        _link.value = _link.value.copy(state = LinkState.SYNCING, message = "连接中…")
        val res = client.ping()
        val next = if (res.ok) {
            LinkInfo(
                state = LinkState.OK,
                endpoint = _link.value.endpoint,
                message = "已连接",
                lastOkAtMillis = System.currentTimeMillis()
            )
        } else {
            _link.value.copy(state = LinkState.FAIL, message = "连不上：${res.body.take(80)}")
        }
        _link.value = next
        return next
    }

    /** 拉 agent 列表；返回原始 JSON，解析留给上层（第一版先用演示数据兜底） */
    suspend fun pullAgentsRaw(): String? {
        val res = client.agents()
        return if (res.ok) res.body else null
    }

    /** 取一张需要鉴权的图片（专家头像）。失败返回 null，调用方退回矢量图标。 */
    suspend fun imageBytes(url: String): ByteArray? = client.fetchImageBytes(url)

    /** 审批：走本体的人工介入恢复接口（thread 粒度） */
    suspend fun decide(
        agentId: String,
        threadId: String,
        allow: Boolean,
        note: String? = null,
        count: Int = 1
    ): Boolean = client.resumeApproval(agentId, threadId, allow, note, count).ok

    /** 账号密码登录，成功后把 token 装进 client */
    suspend fun login(username: String, password: String): Result<String> {
        val res = client.login(username, password)
        if (!res.ok) {
            val msg = if (res.code == 429) lockoutMessage(res.body)
            else extractError(res.body)
            return Result.failure(IllegalStateException(msg ?: "登录失败（${res.code}）"))
        }
        val t = extractToken(res.body)
        if (t.isNullOrBlank()) return Result.failure(IllegalStateException("登录成功但没拿到 token"))
        client.updateEndpoint(_link.value.endpoint, t)
        return Result.success(t)
    }

    fun setEndpointAndToken(url: String, token: String) = client.updateEndpoint(url, token)

    /**
     * 令牌过期回调（HTTP 401），由 AppViewModel 注册。
     * 触发发生在 client 的 IO 线程上，接的一方必须自己切回主线程。
     */
    var onAuthExpired: (() -> Unit)?
        get() = client.onAuthExpired
        set(value) {
            client.onAuthExpired = value
        }

    fun chatWsUrl(agentId: String): String = client.chatWsUrl(agentId)

    fun notifyWsUrl(): String = client.notifyWsUrl()

    suspend fun agentsJson(): String? = client.agents().takeIf { it.ok }?.body

    /**
     * 令牌自检：返回 /api/agents 的 HTTP 状态码。
     * 网页登录抓到的 JWT 必须先过这一关，否则用户会被丢进一个空壳主界面
     * ——之前就是 401 被 takeIf{it.ok} 静默吃成空列表，看起来像「没数据」。
     */
    suspend fun probeAgents(): Int = client.agents().code

    suspend fun threadsJson(agentId: String): String? =
        client.threads(agentId).takeIf { it.ok }?.body

    suspend fun historyJson(agentId: String, threadId: String): String? =
        client.history(agentId, threadId).takeIf { it.ok }?.body

    suspend fun knowledgeJson(): String? = client.knowledgeBases().takeIf { it.ok }?.body

    suspend fun cronJson(agentId: String): String? =
        client.cronJobs(agentId).takeIf { it.ok }?.body

    suspend fun agentJson(agentId: String): String? =
        client.agentDetail(agentId).takeIf { it.ok }?.body

    suspend fun agentStatusJson(agentId: String): String? =
        client.agentStatus(agentId).takeIf { it.ok }?.body

    suspend fun memoryStatsJson(agentId: String): String? =
        client.memoryStats(agentId).takeIf { it.ok }?.body

    suspend fun toolSettingsJson(agentId: String): String? =
        client.toolSettings(agentId).takeIf { it.ok }?.body

    suspend fun strongMemoryJson(agentId: String): String? =
        client.strongMemory(agentId).takeIf { it.ok }?.body

    suspend fun userProfileJson(agentId: String): String? =
        client.userProfile(agentId).takeIf { it.ok }?.body

    /**
     * 登录被锁（服务端 ErrorCode.LOGIN_LOCKED，HTTP 429）。
     *
     * 服务端默认 login_max_attempts=5、login_lockout_seconds=900：连续错 5 次锁 15 分钟。
     * 错误体形如
     *   {"error":{"code":"LOGIN_LOCKED","message":"account temporarily locked",
     *             "details":{"retry_after_seconds":884,"minutes":15}}}
     * details 里的 minutes 是「剩余分钟」（向上取整），不是总时长；有就按它说，
     * 没有就退回 seconds 自己换算，别把英文原句甩给用户。
     */
    private fun lockoutMessage(body: String): String? = runCatching {
        val err = org.json.JSONObject(body).optJSONObject("error")
        val details = err?.optJSONObject("details")
        val minutes = details?.optInt("minutes", 0) ?: 0
        val seconds = details?.optInt("retry_after_seconds", 0) ?: 0
        val wait = when {
            minutes > 0 -> minutes
            seconds > 0 -> ((seconds + 59) / 60).coerceAtLeast(1)
            else -> 0
        }
        if (wait > 0) "密码错太多次，请 $wait 分钟后再试" else "密码错太多次，请稍后再试"
    }.getOrNull()

    /** 从 {"error":{"message":...}} 或 {"detail":...} 里抠出人话 */
    private fun extractError(body: String): String? = runCatching {
        val o = org.json.JSONObject(body)
        o.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
            ?: o.optString("detail").takeIf { it.isNotBlank() && it != "null" }
    }.getOrNull()

    /** 兼容 {"token":..} / {"access_token":..} / {"data":{"token":..}} */
    private fun extractToken(body: String): String? = runCatching {
        val o = org.json.JSONObject(body)
        sequenceOf("access_token", "token")
            .mapNotNull { k -> o.optString(k).takeIf { it.isNotBlank() } }
            .firstOrNull()
            ?: o.optJSONObject("data")?.let { d ->
                sequenceOf("access_token", "token")
                    .mapNotNull { k -> d.optString(k).takeIf { it.isNotBlank() } }
                    .firstOrNull()
            }
    }.getOrNull()

    // 注：第一版 NAS 侧没有上报工具调用的端点，OctopClient 里也没有对应方法，
    // 原来这里的 report() 是死代码（引用了不存在的 reportToolCall）。
    // 等端点定了再补，届时从 OctopClient 加一个公开方法。
}
