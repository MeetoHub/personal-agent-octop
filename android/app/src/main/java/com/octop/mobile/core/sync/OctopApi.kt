package com.octop.mobile.core.sync

import org.json.JSONObject

/**
 * Octop 全量 HTTP 接口层 —— 375 个方法，覆盖 38 个业务模块。
 *
 * 生成方式：直接解析 NAS 上 octop 1.0.3b6 的 `octop/api/routers` 下全部 .py，
 * 路径与处理函数名一一对应，非手抄。全量清单见 docs/octop-api-inventory.md。
 *
 * 刻意不接：mobile（云手机通道，本 App 走本机 Android 权限）、desktop、browser、
 * admin/cloud/teams/MFA/LDAP/OIDC/TLS/backup/update/users/usage 等纯运维模块。
 *
 * 调用约定：路径变量展开为参数（agent_id → agentId）；GET/DELETE 用 query 映射；
 * POST/PUT/PATCH 用 JSONObject body，字段名与后端 pydantic 模型一致。
 */
class OctopApi(private val c: OctopClient) {

    // ══════════ acp.py ══════════

    /** GET /api/acp — get_global_acp_runners */
    suspend fun getGlobalAcpRunners(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/acp", query), null)

    /** PUT /api/acp — put_global_acp_runners */
    suspend fun putGlobalAcpRunners(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/acp", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/acp/ask — ask_acp_runner */
    suspend fun askAcpRunner(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/acp/ask", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/acp/install — install_acp_runner */
    suspend fun installAcpRunner(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/acp/install", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/acp/probe — probe_acp_runner */
    suspend fun probeAcpRunner(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/acp/probe", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/acp/{runner_name} — get_global_acp_runner */
    suspend fun getGlobalAcpRunner(runnerName: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/acp/${runnerName}", query), null)

    /** PUT /api/acp/{runner_name} — put_global_acp_runner */
    suspend fun putGlobalAcpRunner(runnerName: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/acp/${runnerName}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/acp/{runner_name} — delete_global_acp_runner */
    suspend fun deleteGlobalAcpRunner(runnerName: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/acp/${runnerName}", query), null)

    /** GET /api/agents/{agent_id}/acp — get_acp_config */
    suspend fun getAcpConfig(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/acp", query), null)

    /** PUT /api/agents/{agent_id}/acp — put_acp_config */
    suspend fun putAcpConfig(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/agents/${agentId}/acp", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** PUT /api/agents/{agent_id}/acp/tool — put_acp_tool_toggle */
    suspend fun putAcpToolToggle(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/agents/${agentId}/acp/tool", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/acp/{runner_name} — get_acp_runner */
    suspend fun getAcpRunner(agentId: String, runnerName: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/acp/${runnerName}", query), null)

    /** PUT /api/agents/{agent_id}/acp/{runner_name} — put_acp_runner */
    suspend fun putAcpRunner(agentId: String, runnerName: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/agents/${agentId}/acp/${runnerName}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/agents/{agent_id}/acp/{runner_name} — delete_acp_runner */
    suspend fun deleteAcpRunner(agentId: String, runnerName: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/agents/${agentId}/acp/${runnerName}", query), null)

    // ══════════ agent_files.py ══════════

    /** GET /api/agents/{agent_id}/heartbeat-config — get_heartbeat_config */
    suspend fun getHeartbeatConfig(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/heartbeat-config", query), null)

    /** PUT /api/agents/{agent_id}/heartbeat-config — put_heartbeat_config */
    suspend fun putHeartbeatConfig(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/agents/${agentId}/heartbeat-config", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/memory/daily — list_daily_memory */
    suspend fun listDailyMemory(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/daily", query), null)

    /** GET /api/agents/{agent_id}/memory/daily/{filename} — read_daily_memory */
    suspend fun readDailyMemory(agentId: String, filename: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/daily/${filename}", query), null)

    /** DELETE /api/agents/{agent_id}/memory/daily/{filename} — delete_daily_memory */
    suspend fun deleteDailyMemory(agentId: String, filename: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/agents/${agentId}/memory/daily/${filename}", query), null)

    /** POST /api/agents/{agent_id}/memory/manual-review — call */
    suspend fun agentsAgentIdMemoryManualReview(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/manual-review", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/memory/manual-review — get_manual_memory_review */
    suspend fun getManualMemoryReview(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/manual-review", query), null)

    /** GET /api/agents/{agent_id}/memory/strong — get_strong_memory */
    suspend fun getStrongMemory(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/strong", query), null)

    /** PUT /api/agents/{agent_id}/memory/strong — put_strong_memory */
    suspend fun putStrongMemory(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/agents/${agentId}/memory/strong", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/memory/user — get_user_profile */
    suspend fun getUserProfile(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/user", query), null)

    /** PUT /api/agents/{agent_id}/memory/user — put_user_profile */
    suspend fun putUserProfile(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/agents/${agentId}/memory/user", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ agent_mcp.py ══════════

    /** POST /api/mcp/agents — agent_mcp_post */
    suspend fun agentMcpPost(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/mcp/agents", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/mcp/agents — agent_mcp_get */
    suspend fun agentMcpGet(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/mcp/agents", query), null)

    /** DELETE /api/mcp/agents — agent_mcp_delete */
    suspend fun agentMcpDelete(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/mcp/agents", query), null)

    // ══════════ agent_tools.py ══════════

    /** GET /api/agents/{agent_id}/tool-settings — get_tool_settings */
    suspend fun getToolSettings(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/tool-settings", query), null)

    /** PUT /api/agents/{agent_id}/tool-settings — put_tool_settings */
    suspend fun putToolSettings(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/agents/${agentId}/tool-settings", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** PATCH /api/agents/{agent_id}/tool-settings/{tool_name} — patch_tool_setting */
    suspend fun patchToolSetting(agentId: String, toolName: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/agents/${agentId}/tool-settings/${toolName}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ agents.py ══════════

    /** GET /api/agents — list_agents */
    suspend fun listAgents(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents", query), null)

    /** POST /api/agents — create_agent */
    suspend fun createAgent(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id} — get_agent */
    suspend fun getAgent(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}", query), null)

    /** PATCH /api/agents/{agent_id} — patch_agent */
    suspend fun patchAgent(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/agents/${agentId}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/agents/{agent_id} — delete_agent */
    suspend fun deleteAgent(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/agents/${agentId}", query), null)

    /** POST /api/agents/{agent_id}/avatar — upload_agent_avatar */
    suspend fun uploadAgentAvatar(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/avatar", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/avatar — get_agent_avatar */
    suspend fun getAgentAvatar(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/avatar", query), null)

    /** DELETE /api/agents/{agent_id}/avatar — delete_agent_avatar */
    suspend fun deleteAgentAvatar(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/agents/${agentId}/avatar", query), null)

    /** POST /api/agents/{agent_id}/read — mark_agent_read */
    suspend fun markAgentRead(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/read", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/reload — reload_agent */
    suspend fun reloadAgent(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/reload", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/start — start_agent */
    suspend fun startAgent(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/start", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/status — agent_status */
    suspend fun agentStatus(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/status", query), null)

    /** POST /api/agents/{agent_id}/stop — stop_agent */
    suspend fun stopAgent(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/stop", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ auth.py ══════════

    /** GET /api/captcha — get_captcha */
    suspend fun getCaptcha(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/captcha", query), null)

    /** POST /api/change-password — change_password */
    suspend fun changePassword(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/change-password", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/login — login */
    suspend fun login(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/login", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/logout — logout */
    suspend fun logout(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/logout", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/me — me */
    suspend fun me(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/me", query), null)

    /** PATCH /api/me — update_me */
    suspend fun updateMe(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/me", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/me/avatar — upload_my_avatar */
    suspend fun uploadMyAvatar(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/me/avatar", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/me/avatar — delete_my_avatar */
    suspend fun deleteMyAvatar(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/me/avatar", query), null)

    // auth_device.py 未收录：/api/auth/device/{register,token,revoke} 在服务端
    // 只对桌面 shell（OCTOP_DESKTOP_SHELL=1）且回环地址开放，手机从局域网调用
    // 必然 403，留着就是永远发不出去的请求。

    // ══════════ bridge.py ══════════

    /** GET /api/bridge/connections — list_connections */
    suspend fun listConnections(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/bridge/connections", query), null)

    /** POST /api/bridge/connections — create_connection */
    suspend fun createConnection(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/bridge/connections", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/bridge/connections/{connection_id} — get_connection */
    suspend fun getConnection(connectionId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/bridge/connections/${connectionId}", query), null)

    /** PATCH /api/bridge/connections/{connection_id} — patch_connection */
    suspend fun patchConnection(connectionId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/bridge/connections/${connectionId}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/bridge/connections/{connection_id} — delete_connection */
    suspend fun deleteConnection(connectionId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/bridge/connections/${connectionId}", query), null)

    /** GET /api/bridge/connections/{connection_id}/agents — list_remote_agents */
    suspend fun listRemoteAgents(connectionId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/bridge/connections/${connectionId}/agents", query), null)

    /** POST /api/bridge/connections/{connection_id}/avatar — upload_bridge_avatar */
    suspend fun uploadBridgeAvatar(connectionId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/bridge/connections/${connectionId}/avatar", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/bridge/connections/{connection_id}/avatar — get_bridge_avatar */
    suspend fun getBridgeAvatar(connectionId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/bridge/connections/${connectionId}/avatar", query), null)

    /** DELETE /api/bridge/connections/{connection_id}/avatar — delete_bridge_avatar */
    suspend fun deleteBridgeAvatar(connectionId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/bridge/connections/${connectionId}/avatar", query), null)

    /** GET /api/bridge/connections/{connection_id}/browser/env-status — get_remote_browser_env_status */
    suspend fun getRemoteBrowserEnvStatus(connectionId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/bridge/connections/${connectionId}/browser/env-status", query), null)

    /** GET /api/bridge/connections/{connection_id}/browser/harness-sessions — get_remote_browser_sessions */
    suspend fun getRemoteBrowserSessions(connectionId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/bridge/connections/${connectionId}/browser/harness-sessions", query), null)

    /** POST /api/bridge/connections/{connection_id}/browser/sessions/{session_id}/handoff — post_remote_browser_handoff */
    suspend fun postRemoteBrowserHandoff(connectionId: String, sessionId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/bridge/connections/${connectionId}/browser/sessions/${sessionId}/handoff", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/bridge/connections/{connection_id}/connect — connect_connection */
    suspend fun connectConnection(connectionId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/bridge/connections/${connectionId}/connect", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/bridge/connections/{connection_id}/disconnect — disconnect_connection */
    suspend fun disconnectConnection(connectionId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/bridge/connections/${connectionId}/disconnect", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/bridge/connections/{connection_id}/knowledge-bases — list_remote_knowledge_bases */
    suspend fun listRemoteKnowledgeBases(connectionId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/bridge/connections/${connectionId}/knowledge-bases", query), null)

    /** GET /api/bridge/connections/{connection_id}/knowledge-bases/capability — get_remote_knowledge_capability */
    suspend fun getRemoteKnowledgeCapability(connectionId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/bridge/connections/${connectionId}/knowledge-bases/capability", query), null)

    /** POST /api/bridge/connections/{connection_id}/probe — probe_connection */
    suspend fun probeConnection(connectionId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/bridge/connections/${connectionId}/probe", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/bridge/connections/{connection_id}/providers/active-model — get_remote_active_model */
    suspend fun getRemoteActiveModel(connectionId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/bridge/connections/${connectionId}/providers/active-model", query), null)

    /** GET /api/bridge/connections/{connection_id}/providers/resolved — list_remote_resolved_models */
    suspend fun listRemoteResolvedModels(connectionId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/bridge/connections/${connectionId}/providers/resolved", query), null)

    /** POST /api/bridge/probe — probe_peer */
    suspend fun probePeer(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/bridge/probe", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ channels.py ══════════

    /** GET /api/agents/{agent_id}/channels — list_channels */
    suspend fun listChannels(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/channels", query), null)

    /** POST /api/agents/{agent_id}/channels — create_channel */
    suspend fun createChannel(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/channels/dingtalk/qrcode/generate — dingtalk_qrcode_generate */
    suspend fun dingtalkQrcodeGenerate(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/dingtalk/qrcode/generate", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/channels/dingtalk/qrcode/poll — dingtalk_qrcode_poll */
    suspend fun dingtalkQrcodePoll(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/dingtalk/qrcode/poll", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/channels/feishu/bot-creator/poll — feishu_bot_creator_poll */
    suspend fun feishuBotCreatorPoll(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/feishu/bot-creator/poll", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/channels/feishu/bot-creator/start — feishu_bot_creator_start */
    suspend fun feishuBotCreatorStart(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/feishu/bot-creator/start", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/channels/feishu/bot-creator/stop — feishu_bot_creator_stop */
    suspend fun feishuBotCreatorStop(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/feishu/bot-creator/stop", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/channels/probe — probe_channel_config */
    suspend fun probeChannelConfig(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/probe", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/channels/qq/qrcode/generate — qq_qrcode_generate */
    suspend fun qqQrcodeGenerate(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/qq/qrcode/generate", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/channels/qq/qrcode/poll — qq_qrcode_poll */
    suspend fun qqQrcodePoll(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/qq/qrcode/poll", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/channels/wecom/qrcode/generate — wecom_qrcode_generate */
    suspend fun wecomQrcodeGenerate(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/wecom/qrcode/generate", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/channels/wecom/qrcode/poll — wecom_qrcode_poll */
    suspend fun wecomQrcodePoll(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/wecom/qrcode/poll", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/channels/weixin/qrcode/generate — weixin_qrcode_generate */
    suspend fun weixinQrcodeGenerate(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/weixin/qrcode/generate", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/channels/weixin/qrcode/poll — weixin_qrcode_poll */
    suspend fun weixinQrcodePoll(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/weixin/qrcode/poll", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/channels/yuanbao/bot-creator/poll — yuanbao_bot_creator_poll */
    suspend fun yuanbaoBotCreatorPoll(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/yuanbao/bot-creator/poll", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/channels/yuanbao/bot-creator/start — yuanbao_bot_creator_start */
    suspend fun yuanbaoBotCreatorStart(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/yuanbao/bot-creator/start", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/channels/yuanbao/bot-creator/stop — yuanbao_bot_creator_stop */
    suspend fun yuanbaoBotCreatorStop(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/yuanbao/bot-creator/stop", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/channels/{channel_id} — get_channel */
    suspend fun getChannel(agentId: String, channelId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/channels/${channelId}", query), null)

    /** PATCH /api/agents/{agent_id}/channels/{channel_id} — patch_channel */
    suspend fun patchChannel(agentId: String, channelId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/agents/${agentId}/channels/${channelId}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/agents/{agent_id}/channels/{channel_id} — delete_channel */
    suspend fun deleteChannel(agentId: String, channelId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/agents/${agentId}/channels/${channelId}", query), null)

    /** POST /api/agents/{agent_id}/channels/{channel_id}/test — test_channel */
    suspend fun testChannel(agentId: String, channelId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/channels/${channelId}/test", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/channels/plugin-kinds — list_plugin_channel_kinds */
    suspend fun listPluginChannelKinds(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/channels/plugin-kinds", query), null)

    // ══════════ chat/history.py ══════════

    /** POST /api/agents/{agent_id}/history-migration/start — start_history_migration */
    suspend fun startHistoryMigration(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/history-migration/start", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/history-migration/status — get_history_migration_status */
    suspend fun getHistoryMigrationStatus(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/history-migration/status", query), null)

    /** PATCH /api/agents/{agent_id}/session — rebind_session */
    suspend fun rebindSession(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/agents/${agentId}/session", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/threads — list_threads */
    suspend fun listThreads(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/threads", query), null)

    /** POST /api/agents/{agent_id}/threads — create_thread */
    suspend fun createThread(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/threads", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** PATCH /api/agents/{agent_id}/threads/{thread_id} — patch_thread */
    suspend fun patchThread(agentId: String, threadId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/agents/${agentId}/threads/${threadId}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/agents/{agent_id}/threads/{thread_id} — delete_thread */
    suspend fun deleteThread(agentId: String, threadId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/agents/${agentId}/threads/${threadId}", query), null)

    /** POST /api/agents/{agent_id}/threads/{thread_id}/cancel — cancel_thread_turn */
    suspend fun cancelThreadTurn(agentId: String, threadId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/threads/${threadId}/cancel", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/threads/{thread_id}/context-usage — get_thread_context_usage */
    suspend fun getThreadContextUsage(agentId: String, threadId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/threads/${threadId}/context-usage", query), null)

    /** POST /api/agents/{agent_id}/threads/{thread_id}/fork — fork_thread */
    suspend fun forkThread(agentId: String, threadId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/threads/${threadId}/fork", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/threads/{thread_id}/history — get_thread_history */
    suspend fun getThreadHistory(agentId: String, threadId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/threads/${threadId}/history", query), null)

    /** POST /api/agents/{agent_id}/threads/{thread_id}/read — mark_thread_read */
    suspend fun markThreadRead(agentId: String, threadId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/threads/${threadId}/read", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ chat/routes.py ══════════

    /** POST /api/agents/{agent_id}/chat/hitl/resume — resume_hitl */
    suspend fun resumeHitl(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/chat/hitl/resume", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/chat/polish — polish_prompt */
    suspend fun polishPrompt(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/chat/polish", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/chat/welcome — get_chat_welcome */
    suspend fun getChatWelcome(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/chat/welcome", query), null)

    // ══════════ chat/trajectory.py ══════════

    /** GET /api/agents/{agent_id}/threads/{thread_id}/trajectory — get_thread_trajectory */
    suspend fun getThreadTrajectory(agentId: String, threadId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/threads/${threadId}/trajectory", query), null)

    /** GET /api/agents/{agent_id}/threads/{thread_id}/trajectory/events/{event_id} — get_trajectory_event */
    suspend fun getTrajectoryEvent(agentId: String, threadId: String, eventId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/threads/${threadId}/trajectory/events/${eventId}", query), null)

    /** GET /api/agents/{agent_id}/threads/{thread_id}/trajectory/export — export_thread_trajectory */
    suspend fun exportThreadTrajectory(agentId: String, threadId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/threads/${threadId}/trajectory/export", query), null)

    /** GET /api/agents/{agent_id}/threads/{thread_id}/trajectory/metrics — get_trajectory_metrics */
    suspend fun getTrajectoryMetrics(agentId: String, threadId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/threads/${threadId}/trajectory/metrics", query), null)

    /** GET /api/agents/{agent_id}/threads/{thread_id}/trajectory/stream — stream_thread_trajectory */
    suspend fun streamThreadTrajectory(agentId: String, threadId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/threads/${threadId}/trajectory/stream", query), null)

    // ══════════ connectors.py ══════════

    /** GET /api/connector-instances — list_instances */
    suspend fun listInstances(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/connector-instances", query), null)

    /** POST /api/connector-instances — create_instance */
    suspend fun createInstance(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connector-instances", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/connector-instances/{instance_id} — get_instance */
    suspend fun getInstance(instanceId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/connector-instances/${instanceId}", query), null)

    /** PATCH /api/connector-instances/{instance_id} — patch_instance */
    suspend fun patchInstance(instanceId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/connector-instances/${instanceId}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/connector-instances/{instance_id} — delete_instance */
    suspend fun deleteInstance(instanceId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/connector-instances/${instanceId}", query), null)

    /** POST /api/connector-instances/{instance_id}/agently-auth/logout — agently_auth_logout */
    suspend fun agentlyAuthLogout(instanceId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connector-instances/${instanceId}/agently-auth/logout", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/connector-instances/{instance_id}/agently-auth/refresh — agently_auth_refresh */
    suspend fun agentlyAuthRefresh(instanceId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connector-instances/${instanceId}/agently-auth/refresh", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/connector-instances/{instance_id}/agently-auth/start — agently_auth_start */
    suspend fun agentlyAuthStart(instanceId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connector-instances/${instanceId}/agently-auth/start", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/connector-instances/{instance_id}/agently-auth/status — agently_auth_status */
    suspend fun agentlyAuthStatus(instanceId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/connector-instances/${instanceId}/agently-auth/status", query), null)

    /** POST /api/connector-instances/{instance_id}/feishu-user-auth/complete — feishu_cli_user_auth_complete_instance */
    suspend fun feishuCliUserAuthCompleteInstance(instanceId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connector-instances/${instanceId}/feishu-user-auth/complete", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/connector-instances/{instance_id}/feishu-user-auth/start — feishu_cli_user_auth_start_instance */
    suspend fun feishuCliUserAuthStartInstance(instanceId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connector-instances/${instanceId}/feishu-user-auth/start", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/connector-instances/{instance_id}/refresh — refresh_instance */
    suspend fun refreshInstance(instanceId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connector-instances/${instanceId}/refresh", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/connector-instances/{instance_id}/test — test_instance */
    suspend fun testInstance(instanceId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connector-instances/${instanceId}/test", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/connectors/auth/{kind}/authorize-url — auth_authorize_url */
    suspend fun authAuthorizeUrl(kind: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/connectors/auth/${kind}/authorize-url", query), null)

    /** POST /api/connectors/auth/{kind}/exchange-code — auth_exchange_code */
    suspend fun authExchangeCode(kind: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connectors/auth/${kind}/exchange-code", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/connectors/auth/{kind}/info — auth_info */
    suspend fun authInfo(kind: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/connectors/auth/${kind}/info", query), null)

    /** GET /api/connectors/catalog — get_catalog */
    suspend fun getCatalog(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/connectors/catalog", query), null)

    /** GET /api/connectors/custom-mcp — get_custom_mcp */
    suspend fun getCustomMcp(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/connectors/custom-mcp", query), null)

    /** PUT /api/connectors/custom-mcp — put_custom_mcp */
    suspend fun putCustomMcp(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/connectors/custom-mcp", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** PATCH /api/connectors/custom-mcp/servers/{server_name} — patch_custom_mcp_server */
    suspend fun patchCustomMcpServer(serverName: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/connectors/custom-mcp/servers/${serverName}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/connectors/custom-mcp/test — test_custom_mcp */
    suspend fun testCustomMcp(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connectors/custom-mcp/test", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/connectors/feishu-cli/user-auth/complete — feishu_cli_user_auth_complete */
    suspend fun feishuCliUserAuthComplete(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connectors/feishu-cli/user-auth/complete", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/connectors/feishu-cli/user-auth/start — feishu_cli_user_auth_start */
    suspend fun feishuCliUserAuthStart(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connectors/feishu-cli/user-auth/start", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/connectors/oauth/callback — oauth_callback */
    suspend fun oauthCallback(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/connectors/oauth/callback", query), null)

    /** GET /api/connectors/oauth/pending/{state_id} — oauth_pending */
    suspend fun oauthPending(stateId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/connectors/oauth/pending/${stateId}", query), null)

    /** POST /api/connectors/oauth/start — oauth_start_unified */
    suspend fun oauthStartUnified(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connectors/oauth/start", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/connectors/oauth/{kind}/start — oauth_start_legacy */
    suspend fun oauthStartLegacy(kind: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connectors/oauth/${kind}/start", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/connectors/test-credentials — test_credentials */
    suspend fun testCredentials(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connectors/test-credentials", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/connectors/weknora/detect-local — detect_weknora_on_octop_host */
    suspend fun detectWeknoraOnOctopHost(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/connectors/weknora/detect-local", query), null)

    /** GET /api/connectors/{kind}/cli-status — connector_cli_status */
    suspend fun connectorCliStatus(kind: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/connectors/${kind}/cli-status", query), null)

    /** POST /api/connectors/{kind}/install-cli — connector_install_cli */
    suspend fun connectorInstallCli(kind: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/connectors/${kind}/install-cli", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ cron.py ══════════

    /** GET /api/agents/{agent_id}/cron — list_cron */
    suspend fun listCron(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/cron", query), null)

    /** POST /api/agents/{agent_id}/cron — create_cron */
    suspend fun createCron(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/cron", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/cron/examples — cron_examples */
    suspend fun cronExamples(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/cron/examples", query), null)

    /** GET /api/agents/{agent_id}/cron/{cron_id} — get_cron */
    suspend fun getCron(agentId: String, cronId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/cron/${cronId}", query), null)

    /** PATCH /api/agents/{agent_id}/cron/{cron_id} — patch_cron */
    suspend fun patchCron(agentId: String, cronId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/agents/${agentId}/cron/${cronId}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/agents/{agent_id}/cron/{cron_id} — delete_cron */
    suspend fun deleteCron(agentId: String, cronId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/agents/${agentId}/cron/${cronId}", query), null)

    /** POST /api/agents/{agent_id}/cron/{cron_id}/run-now — run_now */
    suspend fun runNow(agentId: String, cronId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/cron/${cronId}/run-now", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/cron/settings — cron_settings */
    suspend fun cronSettings(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/cron/settings", query), null)

    // ══════════ decision.py ══════════

    /** GET /api/settings/decision — get_decision_settings */
    suspend fun getDecisionSettings(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/settings/decision", query), null)

    /** PUT /api/settings/decision — put_decision_settings */
    suspend fun putDecisionSettings(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/settings/decision", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/settings/decision/laya — get_laya_status */
    suspend fun getLayaStatus(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/settings/decision/laya", query), null)

    /** POST /api/settings/decision/laya/install — install_laya */
    suspend fun installLaya(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/settings/decision/laya/install", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/settings/decision/laya/start — start_laya */
    suspend fun startLaya(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/settings/decision/laya/start", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/settings/decision/laya/stop — stop_laya */
    suspend fun stopLaya(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/settings/decision/laya/stop", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/settings/decision/test — test_decision_settings */
    suspend fun testDecisionSettings(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/settings/decision/test", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ experts.py ══════════

    /** POST /api/octop-published-/agents/from-expert/{expert_id} — create_agent_from_expert */
    suspend fun createAgentFromExpert(expertId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/octop-published-/agents/from-expert/${expertId}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/octop-published-/agents/{agent_id}/publish-expert — publish_agent_expert */
    suspend fun publishAgentExpert(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/octop-published-/agents/${agentId}/publish-expert", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/octop-published-/experts — list_experts */
    suspend fun listExperts(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/octop-published-/experts", query), null)

    /** GET /api/octop-published-/experts/hub — list_expert_hub */
    suspend fun listExpertHub(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/octop-published-/experts/hub", query), null)

    /** GET /api/octop-published-/experts/hub/{slug} — get_expert_hub_item */
    suspend fun getExpertHubItem(slug: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/octop-published-/experts/hub/${slug}", query), null)

    /** POST /api/octop-published-/experts/hub/{slug}/install — install_expert_hub_item */
    suspend fun installExpertHubItem(slug: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/octop-published-/experts/hub/${slug}/install", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/octop-published-/experts/published — list_published_experts */
    suspend fun listPublishedExperts(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/octop-published-/experts/published", query), null)

    /** GET /api/octop-published-/experts/published/{expert_id} — get_published_expert */
    suspend fun getPublishedExpert(expertId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/octop-published-/experts/published/${expertId}", query), null)

    /** DELETE /api/octop-published-/experts/published/{expert_id} — unpublish_expert */
    suspend fun unpublishExpert(expertId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/octop-published-/experts/published/${expertId}", query), null)

    /** GET /api/octop-published-/experts/published/{expert_id}/avatar — get_published_expert_avatar */
    suspend fun getPublishedExpertAvatar(expertId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/octop-published-/experts/published/${expertId}/avatar", query), null)

    /** POST /api/octop-published-/experts/published/{expert_id}/install — install_published_expert */
    suspend fun installPublishedExpert(expertId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/octop-published-/experts/published/${expertId}/install", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/octop-published-/experts/published/{expert_id}/refresh — refresh_published_expert */
    suspend fun refreshPublishedExpert(expertId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/octop-published-/experts/published/${expertId}/refresh", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/octop-published-/experts/{expert_id} — get_expert */
    suspend fun getExpert(expertId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/octop-published-/experts/${expertId}", query), null)

    // ══════════ filesystem.py ══════════

    /** GET /api/defaults — filesystem_defaults */
    suspend fun filesystemDefaults(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/defaults", query), null)

    /** GET /api/dirs — list_host_dirs */
    suspend fun listHostDirs(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/dirs", query), null)

    /** GET /api/docker-status — get_docker_status */
    suspend fun getDockerStatus(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/docker-status", query), null)

    /** POST /api/ensure-bwrap — ensure_bwrap */
    suspend fun ensureBwrap(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/ensure-bwrap", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/ensure-docker — post_ensure_docker */
    suspend fun postEnsureDocker(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/ensure-docker", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/mkdir — mkdir_host_dir */
    suspend fun mkdirHostDir(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/mkdir", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/probe — probe_host_dir */
    suspend fun probeHostDir(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/probe", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/rename — rename_host_directory */
    suspend fun renameHostDirectory(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/rename", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/roots — filesystem_roots */
    suspend fun filesystemRoots(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/roots", query), null)

    // ══════════ health.py ══════════

    /** GET /api/ — health */
    suspend fun health(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/", query), null)

    // ══════════ knowledge_bases.py ══════════

    /** GET /api/knowledge-bases — list_bases */
    suspend fun listBases(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases", query), null)

    /** POST /api/knowledge-bases — create_base */
    suspend fun createBase(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/knowledge-bases", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/knowledge-bases/capability — capability */
    suspend fun capability(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/capability", query), null)

    /** GET /api/knowledge-bases/default-open — default_open_bases */
    suspend fun defaultOpenBases(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/default-open", query), null)

    /** GET /api/knowledge-bases/embedding-options — embedding_options */
    suspend fun embeddingOptions(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/embedding-options", query), null)

    /** PUT /api/knowledge-bases/feature — put_feature */
    suspend fun putFeature(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/knowledge-bases/feature", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/knowledge-bases/graph-options — graph_options */
    suspend fun graphOptions(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/graph-options", query), null)

    /** GET /api/knowledge-bases/ocr-options — ocr_options */
    suspend fun ocrOptions(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/ocr-options", query), null)

    /** POST /api/knowledge-bases/onnx-activate — activate_onnx_service */
    suspend fun activateOnnxService(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/knowledge-bases/onnx-activate", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/knowledge-bases/onnx-download — start_onnx_download */
    suspend fun startOnnxDownload(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/knowledge-bases/onnx-download", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/knowledge-bases/onnx-download-status — onnx_download_status */
    suspend fun onnxDownloadStatus(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/onnx-download-status", query), null)

    /** POST /api/knowledge-bases/onnx-test — test_onnx_model */
    suspend fun testOnnxModel(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/knowledge-bases/onnx-test", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/knowledge-bases/rerank-options — rerank_options */
    suspend fun rerankOptions(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/rerank-options", query), null)

    /** GET /api/knowledge-bases/{kb_id} — get_base */
    suspend fun getBase(kbId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/${kbId}", query), null)

    /** PATCH /api/knowledge-bases/{kb_id} — update_base */
    suspend fun updateBase(kbId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/knowledge-bases/${kbId}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/knowledge-bases/{kb_id} — delete_base */
    suspend fun deleteBase(kbId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/knowledge-bases/${kbId}", query), null)

    /** POST /api/knowledge-bases/{kb_id}/avatar — upload_kb_avatar */
    suspend fun uploadKbAvatar(kbId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/knowledge-bases/${kbId}/avatar", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/knowledge-bases/{kb_id}/avatar — get_kb_avatar */
    suspend fun getKbAvatar(kbId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/${kbId}/avatar", query), null)

    /** DELETE /api/knowledge-bases/{kb_id}/avatar — delete_kb_avatar */
    suspend fun deleteKbAvatar(kbId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/knowledge-bases/${kbId}/avatar", query), null)

    /** GET /api/knowledge-bases/{kb_id}/documents — list_documents */
    suspend fun listDocuments(kbId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/${kbId}/documents", query), null)

    /** POST /api/knowledge-bases/{kb_id}/documents — upload_document */
    suspend fun uploadDocument(kbId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/knowledge-bases/${kbId}/documents", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/knowledge-bases/{kb_id}/documents/text — create_text_document */
    suspend fun createTextDocument(kbId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/knowledge-bases/${kbId}/documents/text", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/knowledge-bases/{kb_id}/documents/{doc_id} — delete_document */
    suspend fun deleteDocument(kbId: String, docId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/knowledge-bases/${kbId}/documents/${docId}", query), null)

    /** GET /api/knowledge-bases/{kb_id}/documents/{doc_id}/chunks — list_document_chunks */
    suspend fun listDocumentChunks(kbId: String, docId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/${kbId}/documents/${docId}/chunks", query), null)

    /** GET /api/knowledge-bases/{kb_id}/documents/{doc_id}/content — get_text_document */
    suspend fun getTextDocument(kbId: String, docId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/${kbId}/documents/${docId}/content", query), null)

    /** PUT /api/knowledge-bases/{kb_id}/documents/{doc_id}/content — update_text_document */
    suspend fun updateTextDocument(kbId: String, docId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/knowledge-bases/${kbId}/documents/${docId}/content", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/knowledge-bases/{kb_id}/documents/{doc_id}/file — download_document_file */
    suspend fun downloadDocumentFile(kbId: String, docId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/${kbId}/documents/${docId}/file", query), null)

    /** POST /api/knowledge-bases/{kb_id}/documents/{doc_id}/move — move_document */
    suspend fun moveDocument(kbId: String, docId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/knowledge-bases/${kbId}/documents/${docId}/move", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/knowledge-bases/{kb_id}/documents/{doc_id}/preview — preview_document */
    suspend fun previewDocument(kbId: String, docId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/${kbId}/documents/${docId}/preview", query), null)

    /** POST /api/knowledge-bases/{kb_id}/documents/{doc_id}/reindex — reindex_document */
    suspend fun reindexDocument(kbId: String, docId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/knowledge-bases/${kbId}/documents/${docId}/reindex", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/knowledge-bases/{kb_id}/documents/{doc_id}/rename — rename_document */
    suspend fun renameDocument(kbId: String, docId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/knowledge-bases/${kbId}/documents/${docId}/rename", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/knowledge-bases/{kb_id}/folders — create_folder */
    suspend fun createFolder(kbId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/knowledge-bases/${kbId}/folders", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/knowledge-bases/{kb_id}/graph — get_graph */
    suspend fun getGraph(kbId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/${kbId}/graph", query), null)

    /** DELETE /api/knowledge-bases/{kb_id}/graph — delete_graph */
    suspend fun deleteGraph(kbId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/knowledge-bases/${kbId}/graph", query), null)

    /** POST /api/knowledge-bases/{kb_id}/graph/build — call */
    suspend fun knowledgeBasesKbIdGraphBuild(kbId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/knowledge-bases/${kbId}/graph/build", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/knowledge-bases/{kb_id}/graph/entity — get_graph_entity */
    suspend fun getGraphEntity(kbId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/knowledge-bases/${kbId}/graph/entity", query), null)

    /** POST /api/knowledge-bases/{kb_id}/reindex — reindex_base */
    suspend fun reindexBase(kbId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/knowledge-bases/${kbId}/reindex", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ mbti.py ══════════

    /** POST /api/mbti/apply — apply_type */
    suspend fun applyType(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/mbti/apply", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/mbti/current — get_current_mbti */
    suspend fun getCurrentMbti(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/mbti/current", query), null)

    /** GET /api/mbti/preview/{code} — get_persona_preview */
    suspend fun getPersonaPreview(code: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/mbti/preview/${code}", query), null)

    /** GET /api/mbti/test/questions — get_test_questions */
    suspend fun getTestQuestions(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/mbti/test/questions", query), null)

    /** POST /api/mbti/test/submit — submit_test */
    suspend fun submitTest(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/mbti/test/submit", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/mbti/types — list_types */
    suspend fun listTypes(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/mbti/types", query), null)

    /** GET /api/mbti/types/{code} — get_type */
    suspend fun getType(code: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/mbti/types/${code}", query), null)

    // ══════════ media_generation.py ══════════

    /** GET /api/ — get_media_generation_settings */
    suspend fun getMediaGenerationSettings(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/", query), null)

    /** PUT /api/ — put_media_generation_settings */
    suspend fun putMediaGenerationSettings(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/presets — list_media_provider_presets */
    suspend fun listMediaProviderPresets(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/presets", query), null)

    /** POST /api/test — test_media_generation_provider */
    suspend fun testMediaGenerationProvider(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/test", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ memory.py ══════════

    /** POST /api/agents/{agent_id}/memory/atoms — create_atom */
    suspend fun createAtom(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/atoms", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/memory/atoms/list — list_atoms */
    suspend fun listAtoms(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/atoms/list", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/memory/atoms/{atom_id} — get_atom */
    suspend fun getAtom(agentId: String, atomId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/atoms/${atomId}", query), null)

    /** POST /api/agents/{agent_id}/memory/atoms/{atom_id}:deprecate — deprecate_atom */
    suspend fun deprecateAtom(agentId: String, atomId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/atoms/${atomId}:deprecate", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/memory/atoms/{atom_id}:replace — replace_atom */
    suspend fun replaceAtom(agentId: String, atomId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/atoms/${atomId}:replace", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/memory/candidates/list — list_candidates */
    suspend fun listCandidates(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/candidates/list", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/memory/candidates/{candidate_id} — get_candidate */
    suspend fun getCandidate(agentId: String, candidateId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/candidates/${candidateId}", query), null)

    /** POST /api/agents/{agent_id}/memory/candidates/{candidate_id}:promote — promote_candidate */
    suspend fun promoteCandidate(agentId: String, candidateId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/candidates/${candidateId}:promote", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/memory/candidates/{candidate_id}:reject — reject_candidate */
    suspend fun rejectCandidate(agentId: String, candidateId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/candidates/${candidateId}:reject", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/memory/entities/list — list_entities */
    suspend fun listEntities(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/entities/list", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/memory/entities/{entity_id} — get_entity */
    suspend fun getEntity(agentId: String, entityId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/entities/${entityId}", query), null)

    /** POST /api/agents/{agent_id}/memory/episodes/list — list_episodes */
    suspend fun listEpisodes(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/episodes/list", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/memory/episodes/{episode_id} — get_episode */
    suspend fun getEpisode(agentId: String, episodeId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/episodes/${episodeId}", query), null)

    /** GET /api/agents/{agent_id}/memory/extract-config — get_extract_config */
    suspend fun getExtractConfig(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/extract-config", query), null)

    /** PUT /api/agents/{agent_id}/memory/extract-config — put_extract_config */
    suspend fun putExtractConfig(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/agents/${agentId}/memory/extract-config", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/memory/journal/list — list_journal */
    suspend fun listJournal(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/journal/list", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/memory/journal/recent — recent_journal */
    suspend fun recentJournal(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/journal/recent", query), null)

    /** POST /api/agents/{agent_id}/memory/raw_events/list — list_raw_events */
    suspend fun listRawEvents(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/raw_events/list", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/memory/raw_events/{event_id} — get_raw_event */
    suspend fun getRawEvent(agentId: String, eventId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/raw_events/${eventId}", query), null)

    /** GET /api/agents/{agent_id}/memory/stats/atom_kinds — stats_atom_kinds */
    suspend fun statsAtomKinds(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/stats/atom_kinds", query), null)

    /** GET /api/agents/{agent_id}/memory/stats/counts — stats_counts */
    suspend fun statsCounts(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/stats/counts", query), null)

    /** GET /api/agents/{agent_id}/memory/stats/growth — stats_growth */
    suspend fun statsGrowth(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/stats/growth", query), null)

    // ══════════ memory_portable.py ══════════

    /** POST /api/agents/{agent_id}/memory/portable/adopt — adopt_agent_memory */
    suspend fun adoptAgentMemory(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/portable/adopt", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/memory/portable/doctor — doctor_agent_memory */
    suspend fun doctorAgentMemory(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/portable/doctor", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/memory/portable/pack — pack_agent_memory */
    suspend fun packAgentMemory(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/portable/pack", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/memory/portable/sources — list_portable_sources */
    suspend fun listPortableSources(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/memory/portable/sources", query), null)

    // ══════════ memory_storage.py ══════════

    /** GET /api/agents/{agent_id}/memory/storage/backups — backups */
    suspend fun backups(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/storage/backups", query), null)

    /** DELETE /api/agents/{agent_id}/memory/storage/backups — delete_backup */
    suspend fun deleteBackup(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/agents/${agentId}/memory/storage/backups", query), null)

    /** GET /api/agents/{agent_id}/memory/storage/check — check_storage */
    suspend fun checkStorage(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/storage/check", query), null)

    /** GET /api/agents/{agent_id}/memory/storage/maintenance — maintenance_status */
    suspend fun maintenanceStatus(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/memory/storage/maintenance", query), null)

    /** POST /api/agents/{agent_id}/memory/storage/maintenance — start_maintenance */
    suspend fun startMaintenance(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/storage/maintenance", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/memory/storage/restore — restore */
    suspend fun restore(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/memory/storage/restore", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ plugins.py ══════════

    /** GET /api/plugins — list_plugins */
    suspend fun listPlugins(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/plugins", query), null)

    /** GET /api/plugins/agents/{agent_id} — list_agent_plugins */
    suspend fun listAgentPlugins(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/plugins/agents/${agentId}", query), null)

    /** PATCH /api/plugins/agents/{agent_id} — patch_agent_plugins */
    suspend fun patchAgentPlugins(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/plugins/agents/${agentId}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/plugins/agents/{agent_id}/tools — list_agent_plugin_tools */
    suspend fun listAgentPluginTools(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/plugins/agents/${agentId}/tools", query), null)

    /** PATCH /api/plugins/agents/{agent_id}/tools — patch_agent_plugin_tools */
    suspend fun patchAgentPluginTools(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/plugins/agents/${agentId}/tools", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/plugins/install — install_plugin */
    suspend fun installPlugin(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/plugins/install", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/plugins/market — list_market_plugins */
    suspend fun listMarketPlugins(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/plugins/market", query), null)

    /** POST /api/plugins/market/{plugin_id}/install — install_market_plugin */
    suspend fun installMarketPlugin(pluginId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/plugins/market/${pluginId}/install", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/plugins/market/{plugin_id}/ui/{file_path:path} — get_market_plugin_ui_asset */
    suspend fun getMarketPluginUiAsset(pluginId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/plugins/market/${pluginId}/ui/{file_path:path}", query), null)

    /** POST /api/plugins/reload — reload_plugins */
    suspend fun reloadPlugins(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/plugins/reload", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/plugins/upload — upload_plugin */
    suspend fun uploadPlugin(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/plugins/upload", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** PATCH /api/plugins/{plugin_id} — patch_plugin */
    suspend fun patchPlugin(pluginId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/plugins/${pluginId}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/plugins/{plugin_id} — uninstall_plugin */
    suspend fun uninstallPlugin(pluginId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/plugins/${pluginId}", query), null)

    /** GET /api/plugins/{plugin_id}/ui/{file_path:path} — get_plugin_ui_asset */
    suspend fun getPluginUiAsset(pluginId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/plugins/${pluginId}/ui/{file_path:path}", query), null)

    // ══════════ preferences.py ══════════

    /** GET /api/preferences — get_preferences */
    suspend fun getPreferences(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/preferences", query), null)

    /** PATCH /api/preferences — patch_preferences */
    suspend fun patchPreferences(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/preferences", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ proactive_care.py ══════════

    /** GET /api/agents/{agent_id}/proactive-care — get_proactive_care_config */
    suspend fun getProactiveCareConfig(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/proactive-care", query), null)

    /** PUT /api/agents/{agent_id}/proactive-care — put_proactive_care_config */
    suspend fun putProactiveCareConfig(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/agents/${agentId}/proactive-care", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ search.py ══════════

    /** POST /api/search/{provider_id}/test — call */
    suspend fun searchProviderIdTest(providerId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/search/${providerId}/test", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ settings.py ══════════

    /** POST /api/downloads/link — create_download_link */
    suspend fun createDownloadLink(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/downloads/link", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/downloads/link/{token} — open_download_link */
    suspend fun openDownloadLink(token: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/downloads/link/${token}", query), null)

    /** POST /api/downloads/local — save_local_download */
    suspend fun saveLocalDownload(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/downloads/local", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/settings/capabilities — get_capabilities */
    suspend fun getCapabilities(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/settings/capabilities", query), null)

    /** GET /api/settings/captcha — get_captcha_settings */
    suspend fun getCaptchaSettings(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/settings/captcha", query), null)

    /** PUT /api/settings/captcha — put_captcha_settings */
    suspend fun putCaptchaSettings(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/settings/captcha", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/settings/hitl — get_hitl_settings */
    suspend fun getHitlSettings(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/settings/hitl", query), null)

    /** GET /api/settings/timezone — get_timezone_settings */
    suspend fun getTimezoneSettings(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/settings/timezone", query), null)

    /** GET /api/settings/upload — get_upload_settings */
    suspend fun getUploadSettings(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/settings/upload", query), null)

    /** GET /api/settings/webauthn — get_webauthn_settings */
    suspend fun getWebauthnSettings(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/settings/webauthn", query), null)

    /** PUT /api/settings/webauthn — put_webauthn_settings */
    suspend fun putWebauthnSettings(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/settings/webauthn", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ skill_packages.py ══════════

    /** GET /api/skill-packages — list_skill_packages */
    suspend fun listSkillPackages(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/skill-packages", query), null)

    /** POST /api/skill-packages — create_skill_package */
    suspend fun createSkillPackage(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/skill-packages", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/skill-packages/from-skillhub — create_from_skillhub */
    suspend fun createFromSkillhub(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/skill-packages/from-skillhub", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/skill-packages/hub/rankings — package_hub_rankings */
    suspend fun packageHubRankings(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/skill-packages/hub/rankings", query), null)

    /** GET /api/skill-packages/hub/search — package_hub_search */
    suspend fun packageHubSearch(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/skill-packages/hub/search", query), null)

    /** GET /api/skill-packages/{package_id} — get_skill_package */
    suspend fun getSkillPackage(packageId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/skill-packages/${packageId}", query), null)

    /** PATCH /api/skill-packages/{package_id} — update_skill_package */
    suspend fun updateSkillPackage(packageId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/skill-packages/${packageId}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/skill-packages/{package_id} — delete_skill_package */
    suspend fun deleteSkillPackage(packageId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/skill-packages/${packageId}", query), null)

    /** POST /api/skill-packages/{package_id}/avatar — upload_skill_package_avatar */
    suspend fun uploadSkillPackageAvatar(packageId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/skill-packages/${packageId}/avatar", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/skill-packages/{package_id}/avatar — get_skill_package_avatar */
    suspend fun getSkillPackageAvatar(packageId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/skill-packages/${packageId}/avatar", query), null)

    /** DELETE /api/skill-packages/{package_id}/avatar — delete_skill_package_avatar */
    suspend fun deleteSkillPackageAvatar(packageId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/skill-packages/${packageId}/avatar", query), null)

    /** GET /api/skill-packages/{package_id}/export — export_skill_package */
    suspend fun exportSkillPackage(packageId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/skill-packages/${packageId}/export", query), null)

    /** GET /api/skill-packages/{package_id}/skills — list_package_skills */
    suspend fun listPackageSkills(packageId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/skill-packages/${packageId}/skills", query), null)

    /** POST /api/skill-packages/{package_id}/skills — create_package_skill */
    suspend fun createPackageSkill(packageId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/skill-packages/${packageId}/skills", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/skill-packages/{package_id}/skills/hub/install — hub_install_package_skill */
    suspend fun hubInstallPackageSkill(packageId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/skill-packages/${packageId}/skills/hub/install", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/skill-packages/{package_id}/skills/import — import_package_skill */
    suspend fun importPackageSkill(packageId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/skill-packages/${packageId}/skills/import", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/skill-packages/{package_id}/skills/{slug} — get_package_skill */
    suspend fun getPackageSkill(packageId: String, slug: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/skill-packages/${packageId}/skills/${slug}", query), null)

    /** PUT /api/skill-packages/{package_id}/skills/{slug} — update_package_skill */
    suspend fun updatePackageSkill(packageId: String, slug: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/skill-packages/${packageId}/skills/${slug}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/skill-packages/{package_id}/skills/{slug} — delete_package_skill */
    suspend fun deletePackageSkill(packageId: String, slug: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/skill-packages/${packageId}/skills/${slug}", query), null)

    /** GET /api/skill-packages/{package_id}/skills/{slug}/download — download_package_skill_file */
    suspend fun downloadPackageSkillFile(packageId: String, slug: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/skill-packages/${packageId}/skills/${slug}/download", query), null)

    /** GET /api/skill-packages/{package_id}/skills/{slug}/file — read_package_skill_file */
    suspend fun readPackageSkillFile(packageId: String, slug: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/skill-packages/${packageId}/skills/${slug}/file", query), null)

    /** GET /api/skill-packages/{package_id}/skills/{slug}/tree — list_package_skill_tree */
    suspend fun listPackageSkillTree(packageId: String, slug: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/skill-packages/${packageId}/skills/${slug}/tree", query), null)

    // ══════════ skills.py ══════════

    /** GET /api/agents/{agent_id}/skill-packages — list_skill_package_mounts */
    suspend fun listSkillPackageMounts(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/skill-packages", query), null)

    /** PUT /api/agents/{agent_id}/skill-packages — replace_skill_package_mounts */
    suspend fun replaceSkillPackageMounts(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/agents/${agentId}/skill-packages", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/skill-packages/{package_id}/copy — copy_skill_package_to_workspace */
    suspend fun copySkillPackageToWorkspace(agentId: String, packageId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/skill-packages/${packageId}/copy", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/skills — list_skills */
    suspend fun listSkills(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/skills", query), null)

    /** POST /api/agents/{agent_id}/skills — create_skill */
    suspend fun createSkill(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/skills", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/skills/copy — copy_skill_from_agent */
    suspend fun copySkillFromAgent(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/skills/copy", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/skills/export — export_skills_zip */
    suspend fun exportSkillsZip(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/skills/export", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/skills/hub/install — hub_install_skill */
    suspend fun hubInstallSkill(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/skills/hub/install", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/skills/hub/rankings — hub_rankings */
    suspend fun hubRankings(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/skills/hub/rankings", query), null)

    /** GET /api/agents/{agent_id}/skills/hub/search — hub_search_skills */
    suspend fun hubSearchSkills(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/skills/hub/search", query), null)

    /** POST /api/agents/{agent_id}/skills/import — import_skill_from_url */
    suspend fun importSkillFromUrl(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/skills/import", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/skills/{name} — get_skill */
    suspend fun getSkill(agentId: String, name: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/skills/${name}", query), null)

    /** PUT /api/agents/{agent_id}/skills/{name} — update_skill */
    suspend fun updateSkill(agentId: String, name: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/agents/${agentId}/skills/${name}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/agents/{agent_id}/skills/{name} — delete_skill */
    suspend fun deleteSkill(agentId: String, name: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/agents/${agentId}/skills/${name}", query), null)

    /** POST /api/agents/{agent_id}/skills/{name}/disable — disable_skill */
    suspend fun disableSkill(agentId: String, name: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/skills/${name}/disable", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/skills/{name}/enable — enable_skill */
    suspend fun enableSkill(agentId: String, name: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/skills/${name}/enable", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/skills/{name}/export — call */
    suspend fun agentsAgentIdSkillsNameExport(agentId: String, name: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/skills/${name}/export", query), null)

    /** POST /api/agents/{agent_id}/skills/{name}/push-to-package — push_workspace_skill_to_package */
    suspend fun pushWorkspaceSkillToPackage(agentId: String, name: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/skills/${name}/push-to-package", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/skills/hub/rankings — hub_rankings_global */
    suspend fun hubRankingsGlobal(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/skills/hub/rankings", query), null)

    /** GET /api/skills/hub/search — hub_search_skills_global */
    suspend fun hubSearchSkillsGlobal(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/skills/hub/search", query), null)

    // ══════════ slash.py ══════════

    /** GET /api/slash/commands — list_slash_commands */
    suspend fun listSlashCommands(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/slash/commands", query), null)

    // ══════════ subagents.py ══════════

    /** GET /api/agents/{agent_id}/subagents — list_subagents */
    suspend fun listSubagents(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/subagents", query), null)

    /** POST /api/agents/{agent_id}/subagents/install — install_subagent */
    suspend fun installSubagent(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/subagents/install", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/subagent-catalog — list_subagent_catalog */
    suspend fun listSubagentCatalog(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/subagent-catalog", query), null)

    /** GET /api/subagent-catalog/divisions — list_subagent_divisions */
    suspend fun listSubagentDivisions(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/subagent-catalog/divisions", query), null)

    /** GET /api/subagent-catalog/{slug} — get_subagent_catalog_item */
    suspend fun getSubagentCatalogItem(slug: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/subagent-catalog/${slug}", query), null)

    // ══════════ terminal.py ══════════

    /** GET /api/agents/{agent_id}/terminal/context — terminal_context */
    suspend fun terminalContext(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/terminal/context", query), null)

    // ══════════ text_assist.py ══════════

    /** POST /api/text/assist — text_assist */
    suspend fun textAssist(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/text/assist", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ uploads.py ══════════

    /** POST /api/agents/{agent_id}/upload — upload_attachment */
    suspend fun uploadAttachment(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/upload", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ voice.py ══════════

    /** GET /api/ — admin_list_voice_providers */
    suspend fun adminListVoiceProviders(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/", query), null)

    /** POST /api/ — admin_create_voice_provider */
    suspend fun adminCreateVoiceProvider(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/active — get_active_voice */
    suspend fun getActiveVoice(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/active", query), null)

    /** PUT /api/active — set_active_voice */
    suspend fun setActiveVoice(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/active", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/presets — list_voice_presets */
    suspend fun listVoicePresets(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/presets", query), null)

    /** GET /api/providers — list_voice_providers */
    suspend fun listVoiceProviders(query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/providers", query), null)

    /** POST /api/stt — transcribe_audio */
    suspend fun transcribeAudio(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/stt", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/test-configuration — admin_test_voice_configuration */
    suspend fun adminTestVoiceConfiguration(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/test-configuration", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/tts — synthesize_speech */
    suspend fun synthesizeSpeech(body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/tts", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** PATCH /api/{provider_id} — admin_patch_voice_provider */
    suspend fun adminPatchVoiceProvider(providerId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PATCH", url("/api/${providerId}", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/{provider_id} — admin_delete_voice_provider */
    suspend fun adminDeleteVoiceProvider(providerId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/${providerId}", query), null)

    /** POST /api/{provider_id}/test — admin_test_voice_provider */
    suspend fun adminTestVoiceProvider(providerId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/${providerId}/test", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ══════════ workspace.py ══════════

    /** GET /api/agents/{agent_id}/media/preview — preview_media */
    suspend fun previewMedia(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/media/preview", query), null)

    /** GET /api/agents/{agent_id}/workspace/archive — export_workspace_archive */
    suspend fun exportWorkspaceArchive(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/workspace/archive", query), null)

    /** POST /api/agents/{agent_id}/workspace/archive — import_workspace_archive */
    suspend fun importWorkspaceArchive(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/workspace/archive", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/workspace/doc — read_doc */
    suspend fun readDoc(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/workspace/doc", query), null)

    /** PUT /api/agents/{agent_id}/workspace/doc — write_doc */
    suspend fun writeDoc(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/agents/${agentId}/workspace/doc", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/workspace/download — download_file */
    suspend fun downloadFile(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/workspace/download", query), null)

    /** GET /api/agents/{agent_id}/workspace/file — read_file */
    suspend fun readFile(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/workspace/file", query), null)

    /** PUT /api/agents/{agent_id}/workspace/file — write_file */
    suspend fun writeFile(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("PUT", url("/api/agents/${agentId}/workspace/file", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** DELETE /api/agents/{agent_id}/workspace/file — delete_workspace_file */
    suspend fun deleteWorkspaceFile(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("DELETE", url("/api/agents/${agentId}/workspace/file", query), null)

    /** GET /api/agents/{agent_id}/workspace/glob — glob_files */
    suspend fun globFiles(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/workspace/glob", query), null)

    /** GET /api/agents/{agent_id}/workspace/grep — grep_files */
    suspend fun grepFiles(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/workspace/grep", query), null)

    /** GET /api/agents/{agent_id}/workspace/mention-index — call */
    suspend fun agentsAgentIdWorkspaceMentionIndex(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/workspace/mention-index", query), null)

    /** POST /api/agents/{agent_id}/workspace/mkdir — mkdir_workspace_dir */
    suspend fun mkdirWorkspaceDir(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/workspace/mkdir", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** POST /api/agents/{agent_id}/workspace/move — move_workspace_file */
    suspend fun moveWorkspaceFile(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/workspace/move", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    /** GET /api/agents/{agent_id}/workspace/tree — list_tree */
    suspend fun listTree(agentId: String, query: Map<String, String> = emptyMap()): OctopClient.Response =
        c.request("GET", url("/api/agents/${agentId}/workspace/tree", query), null)

    /** POST /api/agents/{agent_id}/workspace/upload — upload_file */
    suspend fun uploadFile(agentId: String, body: JSONObject = JSONObject()): OctopClient.Response =
        c.request("POST", url("/api/agents/${agentId}/workspace/upload", emptyMap()), body.takeIf { it.length() > 0 }?.toString())

    // ── 内部辅助 ──────────────────────────────────────

    private fun url(path: String, query: Map<String, String>): String {
        if (query.isEmpty()) return path
        return path + "?" + query.entries.joinToString("&") { "${it.key}=${enc(it.value)}" }
    }

    private fun enc(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")
}
