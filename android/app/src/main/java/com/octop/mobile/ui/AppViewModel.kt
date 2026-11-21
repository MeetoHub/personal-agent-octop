package com.octop.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octop.mobile.core.auth.TokenStore
import com.octop.mobile.core.chat.ChatEvent
import com.octop.mobile.core.chat.ChatSocket
import com.octop.mobile.core.chat.SocketState
import com.octop.mobile.core.data.ApiParse
import com.octop.mobile.core.model.Expert
import com.octop.mobile.core.model.ExpertDetail
import com.octop.mobile.core.model.LibraryItem
import com.octop.mobile.core.model.OctopTask
import com.octop.mobile.core.model.Receipt
import com.octop.mobile.core.model.ScheduleItem
import com.octop.mobile.core.model.StepStatus
import com.octop.mobile.core.model.TaskStatus
import com.octop.mobile.core.sync.LinkInfo
import com.octop.mobile.core.sync.LinkState
import com.octop.mobile.core.sync.SyncRepository
import com.octop.mobile.core.system.PhoneLink
import com.octop.mobile.core.tools.ToolRegistry
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 单一状态源。所有页面只读它，所有动作只通过它的方法。
 * 等接上 Octop 网关，把 DemoData 换成 SyncRepository 的结果即可，UI 层不动。
 */
class AppViewModel : ViewModel() {

    private val _tasks = MutableStateFlow<List<OctopTask>>(emptyList())
    val tasks: StateFlow<List<OctopTask>> = _tasks.asStateFlow()

    private val _experts = MutableStateFlow<List<Expert>>(emptyList())
    val experts: StateFlow<List<Expert>> = _experts.asStateFlow()

    /** 当前打开的那个专家的全貌（本体 + 记忆 + 通道 + 工具） */
    private val _expertDetail = MutableStateFlow<ExpertDetail?>(null)
    val expertDetail: StateFlow<ExpertDetail?> = _expertDetail.asStateFlow()

    private val _library = MutableStateFlow<List<LibraryItem>>(emptyList())
    val library: StateFlow<List<LibraryItem>> = _library.asStateFlow()

    private val _schedules = MutableStateFlow<List<ScheduleItem>>(emptyList())
    val schedules: StateFlow<List<ScheduleItem>> = _schedules.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    val link: StateFlow<LinkInfo> = SyncRepository.link

    private val _lastToolOutput = MutableStateFlow<String?>(null)
    val lastToolOutput: StateFlow<String?> = _lastToolOutput.asStateFlow()

    fun task(id: String): OctopTask? = _tasks.value.firstOrNull { it.id == id }

    fun approve(taskId: String) {
        val snapshot = _tasks.value.firstOrNull { it.id == taskId }
        updateTask(taskId) { t ->
            t.copy(
                status = TaskStatus.RUNNING,
                progress = (t.progress + 0.15f).coerceAtMost(1f),
                approval = null,
                steps = t.steps.map {
                    if (it.status == StepStatus.RUNNING) it.copy(status = StepStatus.DONE) else it
                },
                receipts = t.receipts + Receipt(
                    tool = "approve",
                    summary = "用户批准：${t.approval?.action ?: ""}",
                    atMillis = System.currentTimeMillis()
                )
            )
        }
        val ap = snapshot?.approval
        if (snapshot != null && ap != null) {
            viewModelScope.launch {
                SyncRepository.decide(snapshot.expertId, taskId, true, null, ap.actionCount)
            }
        }
    }

    fun deny(taskId: String) {
        val snapshot = _tasks.value.firstOrNull { it.id == taskId }
        updateTask(taskId) { t ->
            t.copy(
                status = TaskStatus.PAUSED,
                approval = null,
                receipts = t.receipts + Receipt(
                    tool = "deny",
                    summary = "用户拒绝：${t.approval?.action ?: ""}",
                    atMillis = System.currentTimeMillis()
                )
            )
        }
        val ap = snapshot?.approval
        if (snapshot != null && ap != null) {
            viewModelScope.launch {
                SyncRepository.decide(snapshot.expertId, taskId, false, null, ap.actionCount)
            }
        }
    }

    fun pause(taskId: String) = updateTask(taskId) { it.copy(status = TaskStatus.PAUSED) }

    fun resume(taskId: String) = updateTask(taskId) { it.copy(status = TaskStatus.RUNNING) }

    fun retry(taskId: String) = updateTask(taskId) {
        it.copy(status = TaskStatus.RUNNING, progress = 0f, steps = it.steps.map { s ->
            if (s.status == StepStatus.FAILED) s.copy(status = StepStatus.PENDING) else s
        })
    }

    fun toggleSchedule(id: String) {
        val cur = _schedules.value.firstOrNull { it.id == id } ?: return
        setScheduleEnabled(id, !cur.enabled)
    }

    fun refreshLink() {
        viewModelScope.launch { SyncRepository.check() }
    }

    fun setEndpoint(url: String) {
        SyncRepository.setEndpoint(url)
    }

    /** 跑一次真实通道命令，用来验证「手」通不通 */
    fun testChannel(cmd: String = "id") {
        viewModelScope.launch {
            val r = ToolRegistry.exec(cmd)
            _lastToolOutput.value = "[$cmd]\n${r.display()}"
        }
    }

    // ── 登录 ───────────────────────────────────────────────

    enum class AuthState { CHECKING, LOGGED_OUT, LOGGED_IN }

    private val _auth = MutableStateFlow(AuthState.CHECKING)
    val auth: StateFlow<AuthState> = _auth.asStateFlow()

    private val _loginBusy = MutableStateFlow(false)
    val loginBusy: StateFlow<Boolean> = _loginBusy.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _loginHint = MutableStateFlow<String?>(null)
    val loginHint: StateFlow<String?> = _loginHint.asStateFlow()

    /**
     * 401 统一出口。access token 只有 24h，过期后接口全是 401，
     * 调用方又习惯把失败吞成空列表，用户只会看到「页面空了」。
     * 这里挂到 client 上，收到就退回登录页，并说清为什么。
     */
    init {
        SyncRepository.onAuthExpired = { onTokenExpired() }
    }

    override fun onCleared() {
        // 回调挂在单例上，不留着旧 ViewModel 的引用
        SyncRepository.onAuthExpired = null
        closeChat()
        super.onCleared()
    }

    /**
     * 回调来自 OctopClient 的 IO 线程，这里必须切回主线程再动 StateFlow。
     * viewModelScope 默认就是 Main，正好当那条边界。
     * 同时只处理一次：并发请求可能一起 401，不能连弹多次。
     */
    private fun onTokenExpired() {
        viewModelScope.launch {
            // 只处理「已登录却在用过期令牌」这一种情况：
            // 网页登录自检（loginWithToken）也会收到 401，但那条路径自己有更准的提示，
            // 不能被这里覆盖成「登录已过期」。并发请求一起 401 时也只处理第一条。
            if (_auth.value != AuthState.LOGGED_IN) return@launch
            closeChat()
            // 只清内存里的 token 与落盘的 token，endpoint / username 保留
            // （TokenStore.clearToken 本来就只删 token 这一项），下次进来不用重填地址
            SyncRepository.setEndpointAndToken(TokenStore.endpoint(), "")
            TokenStore.clearToken()
            _chatMessages.value = emptyList()
            _chatBusy.value = false
            _tasks.value = emptyList()
            _schedules.value = emptyList()
            _library.value = emptyList()
            _loginBusy.value = false
            _loginError.value = null
            _loginHint.value = "登录已过期（令牌 24 小时有效），请重新输入密码。"
            _auth.value = AuthState.LOGGED_OUT
        }
    }

    val savedEndpoint: String get() = TokenStore.endpoint()
    val savedUsername: String get() = TokenStore.username()

    /** 冷启动：有 token 就直接进主界面，顺手探一下活 */
    fun restoreSession() {
        if (_auth.value != AuthState.CHECKING) return
        val token = TokenStore.token()
        val ep = TokenStore.endpoint()
        SyncRepository.setEndpointAndToken(ep, token.orEmpty())
        if (token.isNullOrBlank()) {
            _auth.value = AuthState.LOGGED_OUT
            return
        }
        viewModelScope.launch {
            val info = SyncRepository.check()
            if (info.state == LinkState.FAIL) {
                _loginHint.value = "没连上：${info.message}"
            }
            _auth.value = AuthState.LOGGED_IN
            connectChat()
            loadWorkspace()
        }
    }

    fun login(endpoint: String, username: String, password: String) {
        if (_loginBusy.value) return
        _loginBusy.value = true
        _loginError.value = null
        _loginHint.value = null
        viewModelScope.launch {
            SyncRepository.setEndpointAndToken(endpoint, "")
            SyncRepository.login(username, password)
                .onSuccess { token ->
                    TokenStore.save(token, endpoint, username)
                    _loginBusy.value = false
                    _auth.value = AuthState.LOGGED_IN
                    connectChat()
                    loadWorkspace()
                    refreshLink()
                    // 有了 token 才能连反向通道（手机 → NAS）
                    PhoneLink.restart()
                }
                .onFailure { e ->
                    _loginBusy.value = false
                    _loginError.value = e.message ?: "登录失败"
                }
        }
    }

    /**
     * 网页登录成功后走这里：不再经 /api/auth/login，直接拿 JWT 进主界面。
     * 这样验证码、MFA、LDAP 这些校验全交给网页版，客户端不用重复实现。
     */
    fun loginWithToken(endpoint: String, token: String) {
        viewModelScope.launch {
            SyncRepository.setEndpointAndToken(endpoint, token)
            // 先自检：令牌真能取到数据才放人进主界面
            val code = runCatching { SyncRepository.probeAgents() }.getOrElse { -1 }
            if (code != 200) {
                _loginError.value = when (code) {
                    401, 403 -> "登录成功，但这个令牌没有权限（$code）。请重新用网页登录一次。"
                    -1 -> "连不上本体（$endpoint），检查地址和节点小宝。"
                    else -> "本体返回 $code，数据没取到。"
                }
                _auth.value = AuthState.LOGGED_OUT
                return@launch
            }
            TokenStore.save(token, endpoint, TokenStore.username())
            _loginBusy.value = false
            _loginError.value = null
            _loginHint.value = null
            _auth.value = AuthState.LOGGED_IN
            connectChat()
            loadWorkspace()
            refreshLink()
            PhoneLink.restart()
        }
    }

    fun logout() {
        closeChat()
        PhoneLink.stop()
        TokenStore.clearToken()
        _chatMessages.value = emptyList()
        _auth.value = AuthState.LOGGED_OUT
    }

    // ── 对话 ───────────────────────────────────────────────

    data class ChatMessage(
        val id: String,
        val fromUser: Boolean,
        val text: String,
        val atMillis: Long = System.currentTimeMillis()
    )

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatBusy = MutableStateFlow(false)
    val chatBusy: StateFlow<Boolean> = _chatBusy.asStateFlow()

    private val _socketState = MutableStateFlow(SocketState.IDLE)
    val socketState: StateFlow<SocketState> = _socketState.asStateFlow()

    private val _hasThread = MutableStateFlow(false)
    val hasThread: StateFlow<Boolean> = _hasThread.asStateFlow()

    private val _ctxUsage = MutableStateFlow<CtxUsage?>(null)
    val ctxUsage: StateFlow<CtxUsage?> = _ctxUsage.asStateFlow()

    private val _trajectory = MutableStateFlow<List<TraceStep>>(emptyList())
    val trajectory: StateFlow<List<TraceStep>> = _trajectory.asStateFlow()

    private val _traceMetrics = MutableStateFlow(TraceMetrics())
    val traceMetrics: StateFlow<TraceMetrics> = _traceMetrics.asStateFlow()

    /** 默认 agent；多 agent 时用它切换 */
    var activeAgentId: String = "lifebot"

    private var chatSocket: ChatSocket? = null
    private var currentThreadId: String? = null
        set(value) {
            field = value
            _hasThread.value = value != null
            if (value == null) { _ctxUsage.value = null; _trajectory.value = emptyList() }
        }

    // ── 从本体拉真实数据 ───────────────────────────────────

    /** 拉 agent 列表 + 当前 agent 的会话列表 */
    fun loadWorkspace() {
        if (_auth.value != AuthState.LOGGED_IN) return
        viewModelScope.launch {
            _refreshing.value = true
            runCatching {
                val agents = ApiParse.agents(SyncRepository.agentsJson())
                if (agents.isNotEmpty()) {
                    _experts.value = agents
                    if (agents.none { it.id == activeAgentId }) {
                        activeAgentId = agents.first().id
                    }
                }
                _tasks.value = ApiParse.threads(
                    SyncRepository.threadsJson(activeAgentId),
                    activeAgentId
                )
                // 资料库 + 定时：这两页之前从来没接过数据，一直是空白
                _library.value = ApiParse.library(SyncRepository.knowledgeJson())
                _schedules.value = ApiParse.schedules(SyncRepository.cronJson(activeAgentId))
            }
            _refreshing.value = false
        }
    }

    /**
     * 专家详情：本体 + 运行时状态 + 记忆账本 + 工具清单，四路合并成一张视图。
     * 任一接口失败都不影响其它区块——能显示多少显示多少。
     */
    fun loadExpertDetail(agentId: String) {
        viewModelScope.launch {
            _expertDetail.value = null
            val base = ApiParse.expertDetail(SyncRepository.agentJson(agentId))
            if (base == null) {
                _expertDetail.value = ExpertDetail(
                    id = agentId,
                    name = agentId,
                    description = "详情没取到：/api/agents/$agentId 没有返回 200。"
                )
                return@launch
            }
            val (state, channels, crons) =
                ApiParse.agentRuntime(SyncRepository.agentStatusJson(agentId))
            _expertDetail.value = base.copy(
                state = state.ifBlank { base.state },
                channels = channels,
                cronJobs = crons,
                memory = ApiParse.memoryStat(SyncRepository.memoryStatsJson(agentId)),
                tools = ApiParse.enabledTools(SyncRepository.toolSettingsJson(agentId)),
                memoryDoc = ApiParse.docContent(SyncRepository.strongMemoryJson(agentId)),
                userDoc = ApiParse.docContent(SyncRepository.userProfileJson(agentId))
            )
        }
    }

    /** 打开某个会话，把历史灌进对话流 */
    fun loadHistory(threadId: String) {
        viewModelScope.launch {
            currentThreadId = threadId
            val json = SyncRepository.historyJson(activeAgentId, threadId)
            if (json == null) {
                // 接口没返回 200：别让页面假装空白，直接说清楚
                _chatMessages.value = listOf(
                    ChatMessage(
                        id = "err-${System.currentTimeMillis()}",
                        fromUser = false,
                        text = "历史没取到：接口没有返回 200。可能是令牌过期，或这条线程不属于「$activeAgentId」。"
                    )
                )
                return@launch
            }
            val msgs = ApiParse.history(json)
            _chatMessages.value = if (msgs.isEmpty()) {
                listOf(
                    ChatMessage(
                        id = "empty-${System.currentTimeMillis()}",
                        fromUser = false,
                        text = "接口返回了空消息（后端投影可能还在生成，过一会儿再进来看看）。"
                    )
                )
            } else {
                msgs.mapIndexed { i, m -> ChatMessage(id = "h-$i", fromUser = m.fromUser, text = m.text) }
            }
            // 这条会话是不是正卡在审批上，只有 history 说了算
            ApiParse.hitlPending(json)?.let { ap ->
                updateTask(threadId) { it.copy(approval = ap) }
            }
        }
    }

    fun selectAgent(id: String) {
        if (id == activeAgentId) return
        closeChat()
        activeAgentId = id
        loadWorkspace()
    }

    fun connectChat() {
        if (chatSocket != null) return
        val s = ChatSocket(
            urlProvider = { SyncRepository.chatWsUrl(activeAgentId) },
            onEvent = { ev -> handleEvent(ev) },
            onState = { st -> _socketState.value = st }
        )
        chatSocket = s
        s.connect()
    }

    fun closeChat() {
        chatSocket?.close()
        chatSocket = null
        _socketState.value = SocketState.IDLE
    }

    /** 开一段新对话：清空气泡、解绑 thread。第一句发出去后服务端才给新 id。 */
    fun newChat() {
        currentThreadId = null
        _chatMessages.value = emptyList()
        _chatBusy.value = false
        connectChat()
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val trimmed = text.trim()
        _chatMessages.value = _chatMessages.value + ChatMessage(
            id = "u-${System.currentTimeMillis()}",
            fromUser = true,
            text = trimmed
        )
        _chatBusy.value = true
        connectChat()
        chatSocket?.sendTurn(trimmed, currentThreadId)
    }

    fun stopTurn() {
        currentThreadId?.let { chatSocket?.cancel(it) }
        _chatBusy.value = false
    }

    // ── 上下文用量 / 轨迹：给聊天页的「它现在有多满、刚才做了什么」 ──

    /** GET /api/agents/{id}/chat/threads/{tid}/context-usage */
    fun refreshContextUsage() {
        val tid = currentThreadId ?: return
        viewModelScope.launch {
            val body = SyncRepository.api.getThreadContextUsage(activeAgentId, tid)
                .takeIf { it.ok }?.body
            _ctxUsage.value = parseUsage(body)
        }
    }

    /** GET /api/agents/{id}/chat/threads/{tid}/trajectory */
    fun refreshTrajectory() {
        val tid = currentThreadId ?: return
        viewModelScope.launch {
            val body = SyncRepository.api.getThreadTrajectory(activeAgentId, tid)
                .takeIf { it.ok }?.body
            _trajectory.value = parseTrace(body)
            val metrics = SyncRepository.api.getTrajectoryMetrics(activeAgentId, tid)
            _traceMetrics.value = parseMetrics(metrics.takeIf { it.ok }?.body)
        }
    }

    // ── 定时：改成真调接口，不再是本地翻开关 ──

    fun setScheduleEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            val res = SyncRepository.api.patchCron(
                activeAgentId, id, JSONObject().put("enabled", enabled)
            )
            if (res.ok) {
                _schedules.value = _schedules.value.map {
                    if (it.id == id) it.copy(enabled = enabled) else it
                }
            }
        }
    }

    fun runScheduleNow(id: String) {
        viewModelScope.launch {
            SyncRepository.api.runNow(activeAgentId, id, JSONObject())
        }
    }

    fun deleteSchedule(id: String) {
        viewModelScope.launch {
            val res = SyncRepository.api.deleteCron(activeAgentId, id)
            if (res.ok) _schedules.value = _schedules.value.filterNot { it.id == id }
        }
    }

    /** 新建定时任务。trigger 形如 cron:0 9 * * * / interval:3600 / date:2026-01-01T08:00 */
    fun createSchedule(
        name: String,
        trigger: String,
        prompt: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (prompt.isBlank()) { onDone(false); return }
        viewModelScope.launch {
            val body = JSONObject()
                .put("name", name.ifBlank { "定时任务" })
                .put("trigger", trigger)
                .put("prompt", prompt)
                .put("task_type", "agent")
                .put("enabled", true)
            val res = SyncRepository.api.createCron(activeAgentId, body)
            if (res.ok) loadWorkspace()
            onDone(res.ok)
        }
    }

    /** 删一条会话线程 */
    fun deleteThread(threadId: String) {
        viewModelScope.launch {
            val res = SyncRepository.api.deleteThread(activeAgentId, threadId)
            if (res.ok) _tasks.value = _tasks.value.filterNot { it.id == threadId }
        }
    }

    private fun handleEvent(ev: ChatEvent) {
        when (ev) {
            is ChatEvent.Text -> appendAssistant(ev.text)
            is ChatEvent.Thinking -> Unit
            is ChatEvent.Tool -> appendAssistant("· ${ev.name}")
            is ChatEvent.Approval -> {
                appendAssistant("需要你确认一步操作，去「对话」里点开处理。")
                _chatBusy.value = false
                // 只刷列表，不动当前气泡——history 投影没 ready 时重拉会把对话清空
                loadWorkspace()
            }
            is ChatEvent.Bound -> {
                // 新会话拿到 id 了：记住，并立刻把它带进任务列表
                currentThreadId = ev.threadId
                if (_chatMessages.value.isNotEmpty()) loadWorkspace()
            }
            is ChatEvent.Done -> {
                _chatBusy.value = false
                loadWorkspace()
            }
            is ChatEvent.Failure -> {
                appendAssistant("⚠ ${ev.message}")
                _chatBusy.value = false
            }
            is ChatEvent.Raw -> Unit
        }
    }

    /** 流式追加：上一条是 assistant 就续写，否则新开一条 */
    private fun appendAssistant(delta: String) {
        if (delta.isEmpty()) return
        val list = _chatMessages.value
        val last = list.lastOrNull()
        _chatMessages.value = if (last != null && !last.fromUser) {
            list.dropLast(1) + last.copy(text = last.text + delta)
        } else {
            list + ChatMessage(
                id = "a-${System.currentTimeMillis()}",
                fromUser = false,
                text = delta
            )
        }
    }

    private fun updateTask(id: String, block: (OctopTask) -> OctopTask) {
        _tasks.value = _tasks.value.map {
            if (it.id == id) block(it).copy(updatedAtMillis = System.currentTimeMillis()) else it
        }
    }
}

/** 上下文用量：used 已吃进去的 token，limit 窗口上限 */
data class CtxUsage(val used: Int, val limit: Int) {
    val ratio: Float get() = if (limit <= 0) 0f else (used.toFloat() / limit).coerceIn(0f, 1f)
}

/** 轨迹的一步：它想了什么、调了什么、拿到什么。字段对应服务端 TrajectoryEventOut。 */
data class TraceStep(
    val kind: String,
    val title: String,
    val detail: String,
    val isError: Boolean = false,
    val ts: Double = 0.0
)

/** 这轮花了多少时间。对应 GET .../trajectory/metrics */
data class TraceMetrics(
    val turns: Int = 0,
    val steps: Int = 0,
    val llmMs: Long = 0,
    val toolMs: Long = 0,
    val ttftMs: Long = 0
) {
    val hasData: Boolean get() = turns > 0 || steps > 0
}

private fun parseUsage(body: String?): CtxUsage? {
    if (body.isNullOrBlank()) return null
    val o = runCatching { JSONObject(body) }.getOrNull() ?: return null
    fun num(keys: List<String>): Int = keys.firstNotNullOfOrNull { k ->
        o.optInt(k, -1).takeIf { it >= 0 }
    } ?: 0
    val used = num(listOf("used_tokens", "used", "tokens_used", "total_tokens", "input_tokens", "prompt_tokens"))
    val limit = num(listOf("max_tokens", "limit", "context_window", "max_input_tokens", "window", "capacity"))
    if (used == 0 && limit == 0) return null
    return CtxUsage(used, limit)
}

private fun parseTrace(body: String?): List<TraceStep> {
    if (body.isNullOrBlank()) return emptyList()
    val root = runCatching { JSONObject(body) }.getOrNull()
    // 服务端返回 {thread_id, events:[...], has_more}；也兼容裸数组
    val arr = root?.optJSONArray("events")
        ?: runCatching { JSONArray(body) }.getOrNull()
        ?: return emptyList()
    return (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        val kind = o.optString("kind")
        val summary = o.optString("summary").ifBlank { kind }
        val isError = o.optBoolean("is_error")
        val ts = o.optDouble("ts", 0.0)
        // 详情在 payload 里：工具调用看 args/result，文本看 content/text
        val payload = o.optJSONObject("payload")
        val detail = payload?.let { py ->
            listOf("tool_name", "result", "output", "content", "text", "error", "args", "arguments")
                .firstNotNullOfOrNull { k ->
                    val v = py.opt(k) ?: return@firstNotNullOfOrNull null
                    val str = if (v is String) v else v.toString()
                    str.takeIf { it.isNotBlank() && it != "{}" && it != "[]" }
                }
        } ?: ""
        TraceStep(kind, summary, detail.take(500), isError, ts)
    }
}

private fun parseMetrics(body: String?): TraceMetrics {
    if (body.isNullOrBlank()) return TraceMetrics()
    val o = runCatching { JSONObject(body) }.getOrNull() ?: return TraceMetrics()
    return TraceMetrics(
        turns = o.optInt("turns", 0),
        steps = o.optInt("steps", 0),
        llmMs = o.optDouble("llm_duration_ms", 0.0).toLong(),
        toolMs = o.optDouble("tool_duration_ms", 0.0).toLong(),
        ttftMs = o.optDouble("ttft_avg_ms", 0.0).toLong()
    )
}
