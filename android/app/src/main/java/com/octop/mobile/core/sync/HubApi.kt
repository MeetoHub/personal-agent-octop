package com.octop.mobile.core.sync

import org.json.JSONObject

/**
 * 「我的 → 功能中心」那批二级页要用的只读接口。
 *
 * 为什么另起一个类而不往 OctopApi 里塞：OctopApi 是按服务端文件逐个生成的，
 * 加进去下次重新生成就被覆盖。这里只收本批次用到的、genuerated 层没有的方法
 * （usage / admin / cloud），路径和参数全部对照 routers 源码 + curl 实测：
 *
 *   GET /api/usage/summary?window=&granularity=   usage.py::user_summary
 *   GET /api/admin/usage/summary                  usage.py::admin_summary（admin）
 *   GET /api/admin/providers                      providers.py::admin_list_providers
 *   GET /api/providers/active-model               providers.py::get_active_model
 *   GET /api/plugins                               plugins.py::list_plugins
 *   GET /api/plugins/market                        plugins.py::list_market_plugins
 *   GET /api/admin/security                        security.py::get_security_policy
 *   GET /api/admin/security/tool-guard/rules       security.py::list_rules
 *   GET /api/admin/app-config                      app_config.py::get_app_config
 *   GET /api/users                                 users.py::list_users（需 users 权限）
 *   GET /api/admin/storage-backends                storage_backends.py::list_storage_backends
 *   GET /api/cloud/status                          cloud.py::status
 *   GET /api/cloud/instances                       cloud.py::instances（未授权时 409）
 */
class HubApi(private val c: OctopClient) {

    // ── 用量 ──────────────────────────────────────────────

    /** window: today|yesterday|last_7d|last_30d|all；granularity: total|by_day|by_agent|by_model|by_user */
    suspend fun usageSummary(
        window: String = "last_30d",
        granularity: String = "by_day",
    ): OctopClient.Response = c.request(
        "GET",
        "/api/usage/summary?window=$window&granularity=$granularity",
        null
    )

    suspend fun adminUsageSummary(
        window: String = "last_30d",
        granularity: String = "by_day",
    ): OctopClient.Response = c.request(
        "GET",
        "/api/admin/usage/summary?window=$window&granularity=$granularity",
        null
    )

    // ── 模型 ──────────────────────────────────────────────

    /** admin 视角：每个模型带 context_window / visibility，普通视角缺这些字段 */
    suspend fun adminProviders(): OctopClient.Response =
        c.request("GET", "/api/admin/providers", null)

    suspend fun activeModel(): OctopClient.Response =
        c.request("GET", "/api/providers/active-model", null)

    suspend fun resolvedModels(): OctopClient.Response =
        c.request("GET", "/api/providers/resolved", null)

    /** 只改全局默认模型。服务端要 providers 权限，失败会原样回错误。 */
    suspend fun setActiveModel(providerName: String, model: String): OctopClient.Response =
        c.request(
            "PUT", "/api/providers/active-model",
            JSONObject().put("provider_name", providerName).put("model", model).toString()
        )

    // ── 插件 ──────────────────────────────────────────────

    suspend fun plugins(): OctopClient.Response = c.request("GET", "/api/plugins", null)

    suspend fun pluginMarket(): OctopClient.Response =
        c.request("GET", "/api/plugins/market", null)

    /** 全局启用/停用。停用会把插件的工具/技能从所有 agent 摘掉，但可逆。 */
    suspend fun setPluginEnabled(pluginId: String, enabled: Boolean): OctopClient.Response =
        c.request(
            "PATCH", "/api/plugins/$pluginId",
            JSONObject().put("enabled", enabled).toString()
        )

    // ── 安全 ──────────────────────────────────────────────

    suspend fun securityPolicy(): OctopClient.Response =
        c.request("GET", "/api/admin/security", null)

    suspend fun toolGuardRules(): OctopClient.Response =
        c.request("GET", "/api/admin/security/tool-guard/rules", null)

    // ── 应用设置 ──────────────────────────────────────────

    suspend fun appConfig(): OctopClient.Response =
        c.request("GET", "/api/admin/app-config", null)

    // ── 用户 ──────────────────────────────────────────────

    suspend fun users(): OctopClient.Response = c.request("GET", "/api/users", null)

    // ── 存储 ──────────────────────────────────────────────

    suspend fun adminStorageBackends(): OctopClient.Response =
        c.request("GET", "/api/admin/storage-backends", null)

    // ── 云端协同 ──────────────────────────────────────────

    suspend fun cloudStatus(): OctopClient.Response = c.request("GET", "/api/cloud/status", null)

    suspend fun cloudInstances(): OctopClient.Response =
        c.request("GET", "/api/cloud/instances", null)
}
