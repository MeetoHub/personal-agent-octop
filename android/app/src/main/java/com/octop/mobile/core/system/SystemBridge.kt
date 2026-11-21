package com.octop.mobile.core.system

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.content.ContextCompat
import java.util.Calendar

/**
 * 手机本地能力的唯一入口。
 *
 * 这一层只碰 Android 系统 API，不发网络请求、不依赖 Octop——
 * 这样它既能被 App 内部用，也能被 [HttpBridgeServer] 暴露给 NAS 上的 agent。
 *
 * 所有查询都会在权限缺失时返回空列表而不是抛异常：
 * 用户拒过一次权限是很常见的事，不该让整个页面崩掉。
 */
object SystemBridge {

    // ── 权限 ──────────────────────────────────────────────

    val REQUIRED: List<Pair<String, String>> = listOf(
        Manifest.permission.READ_CONTACTS to "读取联系人",
        Manifest.permission.READ_SMS to "读取短信",
        Manifest.permission.READ_CALENDAR to "读取日历"
    )

    fun has(ctx: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

    fun grantedCount(ctx: Context): Int = REQUIRED.count { has(ctx, it.first) }

    // ── 联系人 ────────────────────────────────────────────

    data class Contact(val name: String, val phone: String)

    fun contacts(ctx: Context, query: String? = null, limit: Int = 50): List<Contact> {
        if (!has(ctx, Manifest.permission.READ_CONTACTS)) return emptyList()
        val out = LinkedHashMap<String, Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = if (query.isNullOrBlank()) null
        else "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = if (query.isNullOrBlank()) null else arrayOf("%$query%")

        return try {
            ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, args,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { c ->
                val nameIdx = c.getColumnIndex(projection[0])
                val numIdx = c.getColumnIndex(projection[1])
                while (c.moveToNext() && out.size < limit) {
                    val name = c.getString(nameIdx)?.trim().orEmpty()
                    val number = c.getString(numIdx)?.trim().orEmpty()
                    if (name.isBlank() && number.isBlank()) continue
                    // 同一个人可能有多个号，用「姓名|号码」去重
                    out.getOrPut("$name|$number") { Contact(name, number) }
                }
                out.values.toList()
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── 短信 ──────────────────────────────────────────────

    data class Sms(
        val address: String,
        val body: String,
        val atMillis: Long,
        val incoming: Boolean
    )

    fun recentSms(ctx: Context, limit: Int = 30): List<Sms> {
        if (!has(ctx, Manifest.permission.READ_SMS)) return emptyList()
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        return try {
            ctx.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection, null, null,
                "${Telephony.Sms.DATE} DESC"
            )?.use { c ->
                val addrIdx = c.getColumnIndex(projection[0])
                val bodyIdx = c.getColumnIndex(projection[1])
                val dateIdx = c.getColumnIndex(projection[2])
                val typeIdx = c.getColumnIndex(projection[3])
                val list = ArrayList<Sms>(limit.coerceAtMost(100))
                while (c.moveToNext() && list.size < limit) {
                    val type = c.getInt(typeIdx)
                    list += Sms(
                        address = c.getString(addrIdx).orEmpty(),
                        body = c.getString(bodyIdx).orEmpty(),
                        atMillis = c.getLong(dateIdx),
                        incoming = type == Telephony.Sms.MESSAGE_TYPE_INBOX
                    )
                }
                list
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── 日历 ──────────────────────────────────────────────

    data class Event(
        val title: String,
        val startMillis: Long,
        val endMillis: Long,
        val location: String?
    )

    fun upcomingEvents(ctx: Context, days: Int = 7, limit: Int = 30): List<Event> {
        if (!has(ctx, Manifest.permission.READ_CALENDAR)) return emptyList()
        val now = System.currentTimeMillis()
        val until = now + days * 24L * 60 * 60 * 1000
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION
        )
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND " +
            "${CalendarContract.Events.DTSTART} <= ? AND " +
            "${CalendarContract.Events.DELETED} = 0"
        return try {
            ctx.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection, selection, arrayOf(now.toString(), until.toString()),
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { c ->
                val titleIdx = c.getColumnIndex(projection[0])
                val startIdx = c.getColumnIndex(projection[1])
                val endIdx = c.getColumnIndex(projection[2])
                val locIdx = c.getColumnIndex(projection[3])
                val list = ArrayList<Event>(limit.coerceAtMost(100))
                while (c.moveToNext() && list.size < limit) {
                    list += Event(
                        title = c.getString(titleIdx).orEmpty(),
                        startMillis = c.getLong(startIdx),
                        endMillis = c.getLong(endIdx),
                        location = c.getString(locIdx)
                    )
                }
                list
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── 闹钟 ──────────────────────────────────────────────

    /**
     * 跳系统闹钟界面建闹钟。必须走 UI——直接写 AlarmManager 需要精确闹钟权限，
     * 而且各厂商 ROM 的后台限制不同，用系统 Intent 是最不容易失败的做法。
     */
    fun setAlarm(ctx: Context, hour: Int, minute: Int, label: String? = null): Boolean {
        if (hour !in 0..23 || minute !in 0..59) return false
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                if (!label.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 下一个整点的相对时间辅助——agent 说「20 分钟后提醒我」时用得上 */
    fun inMinutes(minutes: Int): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, minutes) }
        return cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
    }

    // ── 给 agent 看的纯文本快照 ────────────────────────────

    fun contactsDigest(ctx: Context, limit: Int = 30): String =
        contacts(ctx, limit = limit)
            .joinToString("\n") { "- ${it.name.ifBlank { "(无名)" }} ${it.phone}" }
            .ifBlank { "(没有联系人数据，或未授权读取联系人)" }

    fun smsDigest(ctx: Context, limit: Int = 20): String =
        recentSms(ctx, limit)
            .joinToString("\n") { m ->
                val dir = if (m.incoming) "收" else "发"
                "[$dir] ${m.address}: ${m.body.take(80)}"
            }
            .ifBlank { "(没有短信数据，或未授权读取短信)" }

    fun calendarDigest(ctx: Context, days: Int = 7): String =
        upcomingEvents(ctx, days)
            .joinToString("\n") { e ->
                val fmt = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA)
                "${fmt.format(java.util.Date(e.startMillis))} ${e.title}" +
                    (e.location?.takeIf { it.isNotBlank() }?.let { " @$it" } ?: "")
            }
            .ifBlank { "(未来 $days 天没有日程，或未授权读取日历)" }
}
