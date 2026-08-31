package com.seuic.uhfandroid.socketio

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.seuic.uhfandroid.R
import com.seuic.uhfandroid.bean.ApkVersion
import com.seuic.uhfandroid.util.DataStoreUtils
import com.seuic.uhfandroid.util.Utility
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Socket.IO counterpart to MqttManager, kept as close to it as possible so
 * both transports can run side by side: a single connection held open for
 * the whole app process lifetime by SocketIoService, listening on the
 * branchId event for reader START/STOP/UPDATE control and routed through
 * the same Utility.ACTION_APPLICATION_STATUS_UPDATE broadcast, so
 * MainActivity.readerRequestType() handles Socket.IO commands identically
 * to MQTT ones.
 *
 * A Socket.IO "event name" plays the role MQTT's "topic" plays: connect()
 * listens on the branchId event, publish() emits on a caller-supplied
 * event. The client library's own reconnection handling (IO.Options
 * .reconnection, on by default) replaces Paho's automaticReconnect/
 * cleanSession bookkeeping - there's no persisted session to restore, so
 * connect() re-subscribes (re-registers the event listener) every time.
 */
object SocketIoManager {

    private const val TAG = "SocketIoManager"
    private const val CHANNEL_ID = "CHOLA"
    private const val NOTIFICATION_ID_BASE = 4102

    private val notificationIdCounter = AtomicInteger(NOTIFICATION_ID_BASE)

    private var socket: Socket? = null
    private var connectedServerUrl: String? = null
    private var connectedEvent: String? = null

    /**
     * Lets SocketIoService mirror the live connection state in its
     * foreground notification. Set by the service in onCreate, cleared in
     * onDestroy; invoked from the Socket.IO client callbacks below.
     */
    @Volatile
    private var connectionListener: ((connected: Boolean) -> Unit)? = null

    fun setConnectionListener(listener: ((connected: Boolean) -> Unit)?) {
        connectionListener = listener
        listener?.invoke(isSocketConnected())
    }

    fun isSocketConnected(): Boolean = socket?.connected() == true

    @Synchronized
    fun connect(context: Context) {
        if (!DataStoreUtils.getSocketIoEnabled(context)) return
        val serverUrl = DataStoreUtils.getSocketIoServerUrl(context)
        val branchId = DataStoreUtils.getBranchId(context)
        if (serverUrl.isNullOrEmpty() || branchId.isNullOrEmpty()) {
            Log.w(TAG, "Socket.IO enabled but server URL or branch id missing, not connecting")
            return
        }

        val event = eventFor(branchId)
        val existing = socket
        if (existing != null && existing.connected() && connectedServerUrl == serverUrl && connectedEvent == event) {
            return
        }
        disconnectInternal()

        try {
            val options = IO.Options().apply {
                reconnection = true
                timeout = 30000

                val username = DataStoreUtils.getSocketIoUsername(context)
                if (!username.isNullOrEmpty()) {
                    val password = DataStoreUtils.getSocketIoPassword(context).orEmpty()
                    query = "username=$username&password=$password"
                }
            }

            val ioSocket = IO.socket(serverUrl, options)

            ioSocket.on(Socket.EVENT_CONNECT) {
                Log.i(TAG, "Socket.IO connected to $serverUrl")
                connectionListener?.invoke(true)
            }
            ioSocket.on(Socket.EVENT_DISCONNECT) {
                Log.w(TAG, "Socket.IO connection lost, client will retry")
                connectionListener?.invoke(false)
            }
            ioSocket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Socket.IO connect error: ${args.firstOrNull()}")
                connectionListener?.invoke(false)
            }
            ioSocket.on(event) { args ->
                val payload = args.firstOrNull()?.toString().orEmpty()
                try {
                    handleCommandMessage(context.applicationContext, JsonParser.parseString(payload))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to handle Socket.IO message on $event: $payload", e)
                }
            }

            ioSocket.connect()

            socket = ioSocket
            connectedServerUrl = serverUrl
            connectedEvent = event
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid Socket.IO server URL: $serverUrl", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Socket.IO client", e)
        }
    }

    private fun eventFor(branchId: String): String {
        return branchId
    }

    fun isConnected(context: Context) {
        val ioSocket = socket
        if (ioSocket == null || !ioSocket.connected()) {
            connect(context)
        }
    }

    /**
     * Emits [payload] on [publishEvent]. Mirrors a REST API call so the
     * server sees the same data the backend receives; kept best-effort (no
     * throw) since Socket.IO is a secondary channel and must never block/
     * fail the caller's HTTP flow. Unlike MQTT's QoS-1 publish, a plain
     * emit has no delivery acknowledgement from the server, so success here
     * only means the emit was handed to the client - not that it arrived.
     */
    fun publish(
        context: Context, publishEvent: String, payload: String,
        onResult: ((success: Boolean, error: Throwable?) -> Unit)? = null
    ) {
        try {
            if (!DataStoreUtils.getSocketIoEnabled(context)) {
                onResult?.invoke(false, IllegalStateException("Socket.IO disabled"))
                return
            }
            val branchId = DataStoreUtils.getBranchId(context)
            if (branchId.isNullOrEmpty()) {
                onResult?.invoke(false, IllegalStateException("Branch id missing"))
                return
            }

            val ioSocket = socket
            if (ioSocket == null || !ioSocket.connected()) {
             //   Toast.makeText(context, "Socket.IO not connected, dropping publish to $publishEvent", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Socket.IO not connected, dropping publish to $publishEvent")
                onResult?.invoke(false, IllegalStateException("Socket.IO not connected"))
                return
            }

            ioSocket.emit(publishEvent, payload)
            Log.d(TAG, "Emitted $publishEvent successfully")
            onResult?.invoke(true, null)
        } catch (e: Exception) {
         //   Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Failed to emit $publishEvent", e)
            onResult?.invoke(false, e)
        }
    }

    private fun handleCommandMessage(appContext: Context, value: JsonElement) {
        try {
            var requestType: String
            var apkVersion: String? = null
            var apkUrl: String? = null

            if (value.isJsonObject) {
                val json = value.asJsonObject
                requestType = json.get("reader_request_type")?.asString ?: return
                apkVersion = json.get("apk_version")?.asString
                apkUrl = json.get("apk_url")?.asString
            } else {
                requestType = value.asString.trim()
            }

            if (requestType.isEmpty()) return



            when (requestType.uppercase()) {
                "START", "STOP" -> dispatchRequestType(appContext, requestType.uppercase())
                "UPDATE" -> {
                    val version = ApkVersion("")
                    version.apkVersion = apkVersion
                    version.apkUrl = apkUrl
                    DataStoreUtils.setApkVersion(version, appContext)
                    dispatchRequestType(appContext, "UPDATE")
                }
                else -> Log.w(TAG, "Unknown Socket.IO command payload: $value")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle Socket.IO command payload: $value", e)
        }
    }

    private fun dispatchRequestType(appContext: Context, type: String) {
        DataStoreUtils.setRequestType(type, appContext)

        val broadcastIntent = Intent(Utility.ACTION_APPLICATION_STATUS_UPDATE).apply {
            setPackage(appContext.packageName)
            putExtra("reader_request_type", type)
        }
        appContext.sendBroadcast(broadcastIntent)

        notifyUser(appContext, "VRDDHII", "Socket.IO request $type")
    }

    private fun isAppInForeground(appContext: Context): Boolean {
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return false
        return runningProcesses.any {
            it.processName == appContext.packageName &&
                it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }

    private fun launchApplication(appContext: Context) {
        try {
            val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            launchIntent?.let { appContext.startActivity(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch application from Socket.IO command", e)
        }
    }

    private fun notifyUser(appContext: Context, title: String, body: String) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.vrddhii_png)
                .setContentTitle(title)
                .setContentText(body)
                .setSound(uri)
                .setColor(ContextCompat.getColor(appContext, R.color.colorPrimary))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)

            val notificationManager = appContext.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }
            notificationManager.notify(notificationIdCounter.incrementAndGet(), builder.build())
        } catch (e: Exception) {

        }
    }

    fun disconnect() {
        disconnectInternal()
    }

    @Synchronized
    private fun disconnectInternal() {
        try {
            socket?.let {
                try {
                    it.disconnect()
                    it.off()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing Socket.IO client", e)
                }
            }
            socket = null
            connectedServerUrl = null
            connectedEvent = null
            connectionListener?.invoke(false)
        } catch (e: Exception) {

        }
    }
}
