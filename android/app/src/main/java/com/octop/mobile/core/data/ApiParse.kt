package com.octop.mobile.core.data

import com.octop.mobile.core.model.Approval
import com.octop.mobile.core.model.Expert
import com.octop.mobile.core.model.ExpertDetail
import com.octop.mobile.core.model.LibraryItem
import com.octop.mobile.core.model.LibraryKind
import com.octop.mobile.core.model.MemoryStat
import com.octop.mobile.core.model.OctopTask
import com.octop.mobile.core.model.RiskLevel
import com.octop.mobile.core.model.ScheduleItem
import com.octop.mobile.core.model.ScheduleKind
import com.octop.mobile.core.model.TaskStatus
import org.json.JSONArray
import org.json.JSONObject

/**
 * 本体 JSON → App 模型的翻译层。
 *
 * 字段名全部来自实际源码：
 *   GET /api/agents                        agents.py::_row_dict
 *   GET /api/agents/{id}/threads           chat/history.py::list_threads
 *   GET /api/agents/{id}/threads/{t}/history  chat/history.py::get_thread_history
 *
 * 原则：字段缺失一律给安全默认值，绝不抛异常——宁可少显示也不要白屏。
 */
object ApiParse {

    /**
     * org.json 的 `optString` 遇到 JSON `null` 会返回字符串 `"null"`（不是空串）。
     * 后端 description / icon_name / icon_url 都可能是 null，直接显示就会出现
     * 界面上写着 null 的尴尬。统一在入口处折成空串。
     */
    private fun String.realText(): String = if (this == "null") "" else this

    // ── agents → Expert ──────────────────────────────────

    fun agents(json: String?): List<Expert> {
        val arr = asArray(json) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { agentToExpert(it) }
        }
    }

    private fun agentToExpert(o: JSONObject): Expert {
        val state = o.optString("state").ifBlank { "unknown" }
        return Expert(
            id = o.optString("agent_id").ifBlank { o.optString("id") },
            name = o.optString("name").ifBlank { "Agent" },
            role = when (state) {
                "running" -> "在跑"
                "stopped" -> "已停止"
                "error", "failed" -> "出错"
                else -> "待命"
            },
            description = o.optString("description").realText().ifBlank {
                o.optString("welcome_message").realText().take(80)
            },
            enabledTools = idList(o, "skill_package_ids"),
            online = state == "running",
            color = parseColor(o.optString("color")),
            // agents.py:224 返回 icon_name；agents.py:225 返回 icon_url。
            // 这两个字段决定专家卡片的头像，本体 Experts 页就是靠它们渲染。
            iconName = o.optString("icon_name").realText().ifBlank { null },
            iconUrl = o.optString("icon_url").realText().ifBlank { null }
        )
    }

    // ── threads → OctopTask ──────────────────────────────

    fun threads(json: String?, agentId: String): List<OctopTask> {
        val arr = asArray(json) ?: return emptyList()
        return (0 until arr.length())
            .mapNotNull { i -> arr.optJSONObject(i)?.let { threadToTask(it, agentId) } }
            .sortedWith(compareByDescending<OctopTask> { it.status == TaskStatus.WAITING_APPROVAL }
                .thenByDescending { it.updatedAtMillis })
    }

    private fun threadToTask(o: JSONObject, agentId: String): OctopTask {
        val awaiting = o.optBoolean("awaiting_user")
        val turnActive = o.optBoolean("turn_active")
        val hasMessages = o.optBoolean("has_messages")
        val unread = o.optInt("unread_count")

        val status = when {
            awaiting -> TaskStatus.WAITING_APPROVAL
            turnActive -> TaskStatus.RUNNING
            hasMessages -> TaskStatus.DONE
            else -> TaskStatus.QUEUED
        }

        val bits = mutableListOf<String>()
        if (awaiting) bits += "等你确认"
        if (turnActive) bits += "生成中"
        if (unread > 0) bits += "$unread 条未读"
        if (o.optBoolean("pinned")) bits += "已置顶"

        return OctopTask(
            id = o.optString("thread_id"),
            title = o.optString("title").ifBlank { "未命名会话" },
            summary = bits.joinToString(" · "),
            status = status,
            progress = if (turnActive) 0.5f else 0f,
            expertId = agentId,
            updatedAtMillis = epochMillis(o.optLong("last_active"))
                ?: epochMillis(o.optLong("created_at"))
                ?: System.currentTimeMillis()
        )
    }

    // ── history → 对话消息 ───────────────────────────────

    data class RemoteMessage(val fromUser: Boolean, val text: String)

    fun history(json: String?): List<RemoteMessage> {
        val root = runCatching { JSONObject(json ?: "") }.getOrNull() ?: return emptyList()
        val messages = root.optJSONArray("messages") ?: return emptyList()
        val out = ArrayList<RemoteMessage>(messages.length())
        for (i in 0 until messages.length()) {
            val m = messages.optJSONObject(i) ?: continue
            val role = m.optString("role")
            val text = flattenContent(m.opt("content"))
            if (text.isBlank()) continue
            out += RemoteMessage(fromUser = role == "user", text = text.trim())
        }
        return out
    }

    /** history 里是否还在流式 / 还在等审批 */
    fun historyFlags(json: String?): Pair<Boolean, Boolean> {
        val root = runCatching { JSONObject(json ?: "") }.getOrNull() ?: return false to false
        return root.optBoolean("turn_active") to (root.opt("hitl_pending") != null &&
            root.opt("hitl_pending").toString() != "null")
    }

    /**
     * content 可能是纯字符串，也可能是 content blocks 数组
     * （type: text / thinking / tool_use / tool_result / image_url）。
     * 只取能给人看的文字。
     */
    private fun flattenContent(content: Any?): String = when (content) {
        null -> ""
        is String -> content
        is JSONArray -> buildString {
            for (i in 0 until content.length()) {
                val block = content.optJSONObject(i) ?: continue
                when (block.optString("type")) {
                    "text" -> append(block.optString("text"))
                    "tool_use" -> append("· ${block.optString("name")}\n")
                    "tool_result" -> Unit
                    else -> Unit
                }
            }
        }
        is JSONObject -> content.optString("text")
        else -> ""
    }

    // ── hitl_pending → 审批卡 ────────────────────────────

    /**
     * history 里的 hitl_pending 结构（infra/gateway/hitl/coordinator.py::pending_hitl_payload）：
     *   {pending_id, action_requests:[{name, args, description}], review_configs:[...]}
     * 一次暂停可能压着多个 action_request，列表页只展示第一个，其余折叠成一句话。
     */
    fun hitlPending(json: String?): Approval? {
        val root = runCatching { JSONObject(json ?: "") }.getOrNull() ?: return null
        val pending = root.optJSONObject("hitl_pending") ?: return null
        val actions = pending.optJSONArray("action_requests") ?: return null
        if (actions.length() == 0) return null

        val first = actions.optJSONObject(0) ?: return null
        val toolName = first.optString("name")
        val desc = first.optString("description")
        val argsText = first.opt("args")?.toString()?.trim().orEmpty()

        val action = desc.ifBlank {
            if (argsText.isNotBlank() && argsText != "{}") "$toolName $argsText" else toolName
        }.ifBlank { "需要你确认一步操作" }

        val more = if (actions.length() > 1) "还有 ${actions.length() - 1} 项排队" else ""
        val reason = listOfNotNull(
            toolName.takeIf { it.isNotBlank() }?.let { "工具 $it" },
            more.takeIf { it.isNotBlank() }
        ).joinToString(" · ")

        // review_configs[].allowed_decisions 决定了按钮能有哪些，猜错会被服务端 400 挡回
        val allowed = mutableSetOf<String>()
        pending.optJSONArray("review_configs")?.let { cfgs ->
            for (i in 0 until cfgs.length()) {
                val cfg = cfgs.optJSONObject(i) ?: continue
                val arr = cfg.optJSONArray("allowed_decisions") ?: continue
                for (j in 0 until arr.length()) {
                    arr.optString(j).takeIf { it.isNotBlank() }?.let { allowed += it }
                }
            }
        }

        return Approval(
            id = pending.optString("pending_id"),
            action = action,
            reason = reason,
            risk = riskOf(toolName),
            allowed = if (allowed.isEmpty()) listOf("approve", "reject") else allowed.toList(),
            actionCount = actions.length()
        )
    }

    /** 工具名 → 风险等级。宁可报高，不报低。 */
    private fun riskOf(tool: String): RiskLevel = when {
        tool.contains("sms") || tool.contains("call") || tool.contains("phone") -> RiskLevel.HIGH
        tool.contains("delete") || tool.contains("write") || tool.contains("shell") -> RiskLevel.HIGH
        tool.contains("read") || tool.contains("search") || tool.contains("list") -> RiskLevel.LOW
        else -> RiskLevel.MEDIUM
    }

    // ── knowledge bases → LibraryItem ────────────────────

    /**
     * GET /api/knowledge-bases → knowledge_bases.py::_base_payload
     * 字段：id / knowledge_base_id / name / description / doc_count / updated_at
     */
    fun library(json: String?): List<LibraryItem> {
        val arr = asArray(json) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("name").ifBlank { "未命名知识库" }
            val desc = o.optString("description").trim()
            val docs = o.optInt("doc_count")
            LibraryItem(
                id = o.optString("id").ifBlank { o.optString("knowledge_base_id") },
                title = name,
                snippet = if (desc.isBlank()) "$docs 篇文档" else "$desc（$docs 篇）",
                kind = LibraryKind.KNOWLEDGE,
                updatedAtMillis = epochMillis(o.optLong("updated_at")) ?: 0L
            )
        }.sortedByDescending { it.updatedAtMillis }
    }

    // ── cron jobs → ScheduleItem ─────────────────────────

    /**
     * GET /api/agents/{id}/cron → infra/db/repos/cron.py::to_public_dict
     * 字段：id / name / trigger / prompt / enabled / last_status / last_run_at
     */
    fun schedules(json: String?): List<ScheduleItem> {
        val arr = asArray(json) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val trigger = o.optString("trigger")
            val status = o.optString("last_status")
            ScheduleItem(
                id = o.optString("id"),
                name = o.optString("name").ifBlank { "未命名任务" },
                cron = trigger.ifBlank { "未设置" },
                kind = if (trigger.startsWith("cron:") || trigger.startsWith("interval:"))
                    ScheduleKind.CRON else ScheduleKind.ONCE,
                enabled = o.optBoolean("enabled"),
                nextRunLabel = if (status.isBlank() || status == "null") "" else "上次 $status",
                taskPrompt = o.optString("prompt")
            )
        }
    }

    // ── 专家详情：agent 本体 ──────────────────────────────

    /** GET /api/agents/{agent_id} */
    fun expertDetail(json: String?): ExpertDetail? {
        val o = runCatching { JSONObject(json ?: "") }.getOrNull() ?: return null
        if (o.optString("agent_id").isBlank() && o.optString("name").isBlank()) return null
        return ExpertDetail(
            id = o.optString("agent_id"),
            name = o.optString("name").ifBlank { o.optString("agent_id") },
            description = o.optString("description"),
            persona = o.optString("persona_mbti"),
            model = o.optString("default_model"),
            state = o.optString("state"),
            visibility = o.optString("visibility"),
            welcome = o.optString("welcome_message"),
            iconUrl = o.optString("icon_url"),
            knowledgeBaseIds = idList(o, "knowledge_base_ids"),
            skillPackageIds = idList(o, "skill_package_ids"),
            mcpServers = idList(o, "mcp_servers")
        )
    }

    /** GET /api/agents/{id}/memory/stats/counts */
    fun memoryStat(json: String?): MemoryStat {
        val o = runCatching { JSONObject(json ?: "") }.getOrNull() ?: return MemoryStat()
        return MemoryStat(
            rawEvents = o.optInt("raw_events"),
            atoms = o.optInt("atoms"),
            entities = o.optInt("entities"),
            episodes = o.optInt("episodes"),
            pending = o.optInt("candidates_pending"),
            atomsDelta7d = o.optInt("atoms_delta_7d")
        )
    }

    /** GET /api/agents/{id}/status → (state, 通道名, 定时任务名) */
    fun agentRuntime(json: String?): Triple<String, List<String>, List<String>> {
        val o = runCatching { JSONObject(json ?: "") }.getOrNull()
            ?: return Triple("", emptyList(), emptyList())
        val channels = mutableListOf<String>()
        o.optJSONArray("channels")?.let { arr ->
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                val n = c.optString("name").ifBlank { c.optString("kind") }
                if (n.isNotBlank()) channels += n
            }
        }
        val crons = mutableListOf<String>()
        o.optJSONArray("cron_jobs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                val n = c.optString("name").ifBlank { c.optString("prompt").take(18) }
                if (n.isNotBlank()) crons += n
            }
        }
        return Triple(o.optString("state"), channels, crons)
    }

    /** GET /api/agents/{id}/tool-settings → 已启用工具名 */
    fun enabledTools(json: String?): List<String> {
        val root = runCatching { JSONObject(json ?: "") }.getOrNull() ?: return emptyList()
        val arr = root.optJSONArray("tools") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val t = arr.optJSONObject(i) ?: return@mapNotNull null
            if (!t.optBoolean("enabled")) null
            else t.optString("label").ifBlank { t.optString("name") }.takeIf { it.isNotBlank() }
        }
    }

    /** GET /api/agents/{id}/memory/strong 或 /memory/user → {exists, content} */
    fun docContent(json: String?): String {
        val o = runCatching { JSONObject(json ?: "") }.getOrNull() ?: return ""
        return if (o.optBoolean("exists")) o.optString("content") else ""
    }

    // ── 小工具 ───────────────────────────────────────────

    private fun asArray(json: String?): JSONArray? =
        runCatching { JSONArray(json ?: "") }.getOrNull()

    private fun idList(o: JSONObject, key: String): List<String> {
        val arr = o.optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
    }

    /** last_active / created_at 可能是秒、毫秒或 ISO 串；统一成毫秒 */
    private fun epochMillis(value: Long): Long? {
        if (value <= 0L) return null
        return if (value < 100_000_000_000L) value * 1000 else value
    }

    private fun parseColor(raw: String): Long {
        val s = raw.trim()
        if (s.startsWith("#")) {
            val hex = s.removePrefix("#")
            return runCatching {
                when (hex.length) {
                    6 -> 0xFF000000L or hex.toLong(16)
                    8 -> hex.toLong(16)
                    else -> DEFAULT_COLOR
                }
            }.getOrDefault(DEFAULT_COLOR)
        }
        return runCatching { s.toLong() }.getOrDefault(DEFAULT_COLOR)
    }

    private const val DEFAULT_COLOR = 0xFF7C6CFF
}
