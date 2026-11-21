package com.octop.mobile.core.system

import android.Manifest
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 反向通道的工具实现：NAS 下发 tool_call，这里翻成对本机系统 API 的只读查询。
 *
 * 第一批只读，刻意不含任何写操作（发短信、闹钟、点按都不在这里）。
 * 权限缺失时返回 `permission_denied` 而不是空数据——空数据会让 agent 误以为"真的没有"，
 * 明确报错它才知道该让用户去授权。
 */
object PhoneTools {

    const val CONTACTS_SEARCH = "contacts.search"
    const val SMS_RECENT = "sms.recent"
    const val CALENDAR_TODAY = "calendar.today"

    /** 本机已实现的能力。hello 时上报给 NAS，没实现的不报，NAS 侧会据此判断可用性。 */
    val SUPPORTED: List<String> = listOf(CONTACTS_SEARCH, SMS_RECENT, CALENDAR_TODAY)

    fun call(ctx: Context, tool: String, args: JSONObject): JSONObject = try {
        when (tool) {
            CONTACTS_SEARCH -> contacts(ctx, args)
            SMS_RECENT -> sms(ctx, args)
            CALENDAR_TODAY -> calendar(ctx, args)
            else -> fail("unsupported")
        }
    } catch (e: Exception) {
        fail(e.message ?: "internal_error")
    }

    private fun contacts(ctx: Context, args: JSONObject): JSONObject {
        if (!SystemBridge.has(ctx, Manifest.permission.READ_CONTACTS)) {
            return fail("permission_denied")
        }
        val query = args.optString("query", "")
        val limit = args.optInt("limit", 20).coerceIn(1, 50)
        val arr = JSONArray()
        SystemBridge.contacts(ctx, query, limit).forEach {
            arr.put(JSONObject().put("name", it.name).put("phone", it.phone))
        }
        return ok(JSONObject().put("contacts", arr))
    }

    private fun sms(ctx: Context, args: JSONObject): JSONObject {
        if (!SystemBridge.has(ctx, Manifest.permission.READ_SMS)) {
            return fail("permission_denied")
        }
        val limit = args.optInt("limit", 20).coerceIn(1, 50)
        val hours = args.optInt("hours", 72).coerceIn(1, 720)
        val since = System.currentTimeMillis() - hours * 3_600_000L
        val arr = JSONArray()
        SystemBridge.recentSms(ctx, limit)
            .filter { it.atMillis >= since }
            .forEach {
                arr.put(
                    JSONObject()
                        .put("address", it.address)
                        .put("body", it.body)
                        .put("at", it.atMillis)
                        .put("incoming", it.incoming)
                )
            }
        return ok(JSONObject().put("messages", arr))
    }

    private fun calendar(ctx: Context, args: JSONObject): JSONObject {
        if (!SystemBridge.has(ctx, Manifest.permission.READ_CALENDAR)) {
            return fail("permission_denied")
        }
        // 只看今天：NAS 那边的工具叫 calendar.today，跨天的日程不归它管
        val arr = JSONArray()
        SystemBridge.upcomingEvents(ctx, days = 1, limit = 20).forEach {
            arr.put(
                JSONObject()
                    .put("title", it.title)
                    .put("start", it.startMillis)
                    .put("end", it.endMillis)
                    .put("location", it.location ?: "")
            )
        }
        return ok(JSONObject().put("events", arr))
    }

    private fun ok(data: JSONObject): JSONObject = JSONObject().put("ok", true).put("data", data)

    private fun fail(code: String): JSONObject = JSONObject().put("ok", false).put("error", code)
}
