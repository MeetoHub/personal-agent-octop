package com.octop.mobile.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.octop.mobile.core.system.BridgeManager

/**
 * 常驻服务：让 agent 即使 App 退到后台也能被唤醒干活。
 *
 * 为什么必须有它：手机桥（8765）跑在 App 进程里，进程被系统回收就断了。
 * 前台服务能把进程钉住，锁屏后 agent 仍然调得通手机。
 */
class OctopForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "octop_agent"
        private const val NOTI_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, OctopForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OctopForegroundService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 服务活着 = 进程不被回收 = 手机桥还能被 NAS 调到
        BridgeManager.start(this)
        val where = BridgeManager.localUrl()
        startForeground(NOTI_ID, buildNotification("手机能力在线 · $where"))
        return START_STICKY
    }

    override fun onDestroy() {
        BridgeManager.stop()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Octop 运行状态",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "agent 常驻通知" }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Octop")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()
    }
}
