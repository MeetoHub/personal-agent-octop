package com.octop.mobile.core.system

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONObject

/**
 * 只读收集最近通知。
 *
 * 通知没法主动查询，只能由系统把 `onNotificationPosted` 推给我们；所以这里做一层
 * 内存缓冲（进程内最近 100 条），工具查询时直接取，不再等待。
 *
 * 不落盘：通知里常有验证码等敏感信息，进程一退就该消失。
 */
object NotificationLog {

    private const val MAX = 100

    private val items = ArrayDeque<JSONObject>()

    @Synchronized
    fun record(packageName: String, title: String, text: String, at: Long, ongoing: Boolean) {
        if (title.isBlank() && text.isBlank()) return
        items.addLast(
            JSONObject()
                .put("package", packageName)
                .put("title", title)
                .put("text", text)
                .put("at", at)
                .put("ongoing", ongoing)
        )
        while (items.size > MAX) items.removeFirst()
    }

    /** 最新的在前。 */
    @Synchronized
    fun recent(limit: Int): List<JSONObject> = items.toList().takeLast(limit).reversed()

    @Synchronized
    fun clear() = items.clear()
}

class NotificationRelayService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        NotificationLog.record(
            packageName = sbn.packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            at = sbn.postTime,
            ongoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0,
        )
    }
}
