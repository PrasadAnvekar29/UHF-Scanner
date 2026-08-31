package com.seuic.uhfandroid.socketio

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
 * Socket.IO counterpart to MqttService: keeps a single Socket.IO connection
 * open for the whole app process lifetime, regardless of which Activity/
 * Fragment is on screen, so the branchId event is delivered the instant the
 * server emits it instead of being polled for. Started once from
 * App.onCreate() so the connection survives while the app is backgrounded.
 *
 * This cannot survive the user force-stopping the app from system
 * settings - Android intentionally blocks any app from resurrecting itself
 * after that, same as it does for MQTT/FCM.
 */
class SocketIoService : Service() {

    override fun onCreate() {

        try {
            super.onCreate()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, buildNotification(isConnected = false), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, buildNotification(isConnected = false))
            }
            SocketIoManager.setConnectionListener { connected -> updateNotification(connected) }
            SocketIoManager.connect(applicationContext)
        } catch (e: Exception) {

        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            SocketIoManager.setConnectionListener(null)
            SocketIoManager.disconnect()
        } catch (e: Exception) {

        }
        super.onDestroy()
    }

    /**
     * Some OEMs (Xiaomi/Huawei/Oppo-style task killers) stop a started
     * service when its app's task is swiped from Recents, even though
     * that's not stock Android behavior for a foreground service. Restart
     * immediately if that happens, so the Socket.IO connection doesn't
     * silently drop just because the UI was dismissed.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        start(applicationContext)
    }

    private fun buildNotification(isConnected: Boolean): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Socket.IO Connection",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val statusText = if (isConnected) "Socket Connected" else "Socket Not connected"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VRDDHII")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.vrddhii_png)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    /**
     * Called on SocketIoManager's connect/disconnect callbacks so the
     * persistent notification always reflects the live socket state
     * instead of a static "running" label.
     */
    private fun updateNotification(isConnected: Boolean) {
        try {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            manager.notify(NOTIFICATION_ID, buildNotification(isConnected))
        } catch (e: Exception) {

        }
    }

    companion object {
        private const val CHANNEL_ID = "socketio_service_channel"
        private const val NOTIFICATION_ID = 4101

        fun start(context: Context) {
            val intent = Intent(context, SocketIoService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SocketIoService::class.java))
        }
    }
}
