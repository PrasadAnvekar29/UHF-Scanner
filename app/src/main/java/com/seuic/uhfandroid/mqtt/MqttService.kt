package com.seuic.uhfandroid.mqtt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.seuic.uhfandroid.R

/**
 * Keeps a single MQTT connection open for the whole app process lifetime,
 * regardless of which Activity/Fragment is on screen, so
 * uhf.{branchId}.commands is delivered the instant the broker publishes it
 * instead of being polled for. Started once from App.onCreate() so the
 * connection survives while the app is backgrounded.
 *
 * This cannot survive the user force-stopping the app from system
 * settings - Android intentionally blocks any app from resurrecting itself
 * after that, same as it does for FCM.
 */
class MqttService : Service() {

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        MqttManager.connect(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        MqttManager.disconnect()
        super.onDestroy()
    }

    /**
     * Some OEMs (Xiaomi/Huawei/Oppo-style task killers) stop a started
     * service when its app's task is swiped from Recents, even though
     * that's not stock Android behavior for a foreground service. Restart
     * immediately if that happens, so the MQTT connection doesn't silently
     * drop just because the UI was dismissed.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        start(applicationContext)
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MQTT Connection",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VRDDHII")
            .setContentText("MQTT connection running")
            .setSmallIcon(R.drawable.vrddhii_png)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "mqtt_service_channel"
        private const val NOTIFICATION_ID = 4001

        fun start(context: Context) {
            val intent = Intent(context, MqttService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MqttService::class.java))
        }
    }
}
