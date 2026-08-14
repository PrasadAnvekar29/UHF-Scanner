package com.seuic.uhfandroid.mqtt

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
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Single MQTT connection kept open for the whole app process lifetime by
 * MqttService: subscribes to uhf.{branchId}.commands (reader START/STOP/
 * UPDATE control) and relies on the broker pushing messages the instant
 * they're published - no polling involved, unlike the FCM/Kafka
 * alternatives this replaces. Routed through the same
 * Utility.ACTION_APPLICATION_STATUS_UPDATE broadcast FirebaseService uses,
 * so MainActivity.readerRequestType() handles MQTT commands identically.
 *
 * automaticReconnect + cleanSession=false keep the broker session (and its
 * subscription) alive across brief network drops without re-subscribing
 * from scratch every time; connectComplete() still re-subscribes
 * explicitly since that's cheap and covers brokers that don't persist
 * sessions.
 */
object MqttManager {

    private const val TAG = "MqttManager"
    private const val CHANNEL_ID = "CHOLA"
    private const val NOTIFICATION_ID_BASE = 4002
    private const val QOS = 1

    private val notificationIdCounter = AtomicInteger(NOTIFICATION_ID_BASE)

    private var client: MqttAsyncClient? = null
    private var connectedBrokerUrl: String? = null
    private var connectedTopic: String? = null

    @Synchronized
    fun connect(context: Context) {
        if (!DataStoreUtils.getMqttEnabled(context)) return
        val brokerUrl = DataStoreUtils.getMqttBrokerUrl(context)
        val branchId = DataStoreUtils.getBranchId(context)
        if (brokerUrl.isNullOrEmpty() || branchId.isNullOrEmpty()) {
            Log.w(TAG, "MQTT enabled but broker URL or branch id missing, not connecting")
            return
        }

        val topic = topicFor(branchId)
        val existing = client
        if (existing != null && existing.isConnected && connectedBrokerUrl == brokerUrl && connectedTopic == topic) {
            return
        }
        disconnectInternal()

        try {
            val clientId = getOrCreateClientId(context)
            val mqttClient = MqttAsyncClient(brokerUrl, clientId, MemoryPersistence())
            mqttClient.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String) {
                    Log.i(TAG, "MQTT connected (reconnect=$reconnect) to $serverURI")
                    try {
                        mqttClient.subscribe(topic, QOS)
                    } catch (e: MqttException) {
                        Log.e(TAG, "Failed to subscribe to $topic", e)
                    }
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.w(TAG, "MQTT connection lost, automatic reconnect will retry", cause)
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    val payload = String(message.payload)
                    try {
                        handleCommandMessage(context.applicationContext, JsonParser.parseString(payload))
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to handle MQTT message on $topic: $payload", e)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = false
                connectionTimeout = 30
                keepAliveInterval = 60

                val username = DataStoreUtils.getMqttUsername(context)
                if (!username.isNullOrEmpty()) {
                    userName = username
                    password = DataStoreUtils.getMqttPassword(context).orEmpty().toCharArray()
                }
            }

            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.i(TAG, "MQTT connect requested successfully")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "MQTT connect failed, automatic reconnect will retry", exception)
                }
            })

            client = mqttClient
            connectedBrokerUrl = brokerUrl
            connectedTopic = topic
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create MQTT client", e)
        }
    }

    private fun topicFor(branchId: String): String {
       // val sanitized = branchId.replace(Regex("[^a-zA-Z0-9._-]"), "-")
        return branchId
    }

    /**
     * Publishes [payload] to "{branchId}/{topicSuffix}". Mirrors a REST API call so the
     * broker sees the same data the backend receives; kept best-effort (no throw) since
     * MQTT is a secondary channel and must never block/fail the caller's HTTP flow.
     */

    fun isConnected(context: Context){
        val mqttClient = client
        if (mqttClient == null || !mqttClient.isConnected) {
            connect(context)
        }
    }


    fun publish(context: Context, publishTopic: String, payload: String
                , onResult: ((success: Boolean, error: Throwable?) -> Unit)? = null
    ) {
        try {
            if (!DataStoreUtils.getMqttEnabled(context)) {
                onResult?.invoke(false, IllegalStateException("MQTT disabled"))
                return
            }
            val branchId = DataStoreUtils.getBranchId(context)
            if (branchId.isNullOrEmpty()) {
                onResult?.invoke(false, IllegalStateException("Branch id missing"))
                return
            }

            val mqttClient = client
            if (mqttClient == null || !mqttClient.isConnected) {
                Toast.makeText(context,"MQTT not connected, dropping publish to $publishTopic", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "MQTT not connected, dropping publish to $publishTopic")
                onResult?.invoke(false, IllegalStateException("MQTT not connected"))
                return
            }

            val topic = "$publishTopic"
            val message = MqttMessage(payload.toByteArray()).apply { qos = QOS }
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Published to $topic successfully")
                    onResult?.invoke(true, null)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Failed to publish to $topic", exception)
                    onResult?.invoke(false, exception)
                }
            })
        } catch (e: MqttException) {
            Toast.makeText(context,e.message, Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Failed to publish to $publishTopic", e)
            onResult?.invoke(false, e)
        }
    }

    private fun getOrCreateClientId(context: Context): String {
        var clientId = DataStoreUtils.getMqttClientId(context)
        if (clientId.isNullOrEmpty()) {
            clientId = "chola-" + UUID.randomUUID().toString().substring(0, 8)
            DataStoreUtils.setMqttClientId(clientId, context)
        }
        return clientId
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
                else -> Log.w(TAG, "Unknown MQTT command payload: $value")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle MQTT command payload: $value", e)
        }
    }

    private fun dispatchRequestType(appContext: Context, type: String) {
        DataStoreUtils.setRequestType(type, appContext)

        val broadcastIntent = Intent(Utility.ACTION_APPLICATION_STATUS_UPDATE).apply {
            setPackage(appContext.packageName)
            putExtra("reader_request_type", type)
        }
        appContext.sendBroadcast(broadcastIntent)

        notifyUser(appContext, "VRDDHII", "MQTT request $type")

        /*if (!isAppInForeground(appContext)) {
            launchApplication(appContext)
        }*/
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
            Log.e(TAG, "Failed to launch application from MQTT command", e)
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
        }catch (e: Exception){

        }

    }

    fun disconnect() {
        disconnectInternal()
    }

    @Synchronized
    private fun disconnectInternal() {

        try{
            client?.let {
                try {
                    if (it.isConnected) {
                        it.disconnect()
                    }
                    it.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing MQTT client", e)
                }
            }
            client = null
            connectedBrokerUrl = null
            connectedTopic = null
        }catch (e : Exception){

        }

    }
}
