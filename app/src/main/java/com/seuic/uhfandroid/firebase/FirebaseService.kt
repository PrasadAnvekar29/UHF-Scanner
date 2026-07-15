package com.seuic.uhfandroid.firebase

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.seuic.uhfandroid.R
import com.seuic.uhfandroid.bean.ApkVersion
import com.seuic.uhfandroid.util.DataStoreUtils


class FirebaseService() : FirebaseMessagingService() {

    private var NOTIFICATION_ID = 0

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        Log.e("RemmoteMessageTo", remoteMessage.data.toString());

        handleFirebaseDataNotification(remoteMessage)
    }

    private fun handleFirebaseDataNotification(remoteMessage: RemoteMessage) {
        try {

            val notificationType = remoteMessage.data["reader_request_type"]
          //  val branchID = remoteMessage.data["branch_id"]
            Log.i("Firebase:", remoteMessage.toString())

            if (!notificationType.isNullOrEmpty()) {
                when (notificationType) {
                    "START" -> handleReaderNotification(notificationType, "")
                    "STOP" ->handleReaderNotification(notificationType, "")
                    "UPDATE" -> handleUpdateNotification(notificationType, remoteMessage)
                    else -> {
                    }
                }
            }

        } catch (e: Exception) {

        }
    }

    private fun handleReaderNotification(type: String, branchId : String) {

        val header = String.format("VRDDHII")
        val body = String.format("Reqeust %s", type)

        DataStoreUtils.setRequestType( type, applicationContext)

        notifyUser(header, body)

        if (!isAppInForeground()) {
            launchApplication()
        }

        val broadcastIntent = Intent(com.seuic.uhfandroid.util.Utility.ACTION_APPLICATION_STATUS_UPDATE).apply {
            setPackage(packageName)
            putExtra("reader_request_type", type)
        }
        sendBroadcast(broadcastIntent)

    }

    private fun handleUpdateNotification(type: String, remoteMessage: RemoteMessage) {

        val header = String.format("VRDDHII")
        val body = String.format("Reqeust %s", type)

        var apkVerson : ApkVersion = ApkVersion("")

        apkVerson.apkVersion = remoteMessage.data["apk_version"]
        apkVerson.apkUrl = remoteMessage.data["apk_url"]

        DataStoreUtils.setApkVersion( apkVerson, applicationContext)

        notifyUser(header, body)

        if (!isAppInForeground()) {
            launchApplication()
        }

        val broadcastIntent = Intent(com.seuic.uhfandroid.util.Utility.ACTION_APPLICATION_STATUS_UPDATE).apply {
            setPackage(packageName)
            putExtra("reader_request_type", type)
        }
        sendBroadcast(broadcastIntent)

    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return false
        return runningProcesses.any {
            it.processName == packageName &&
                it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }

    private fun launchApplication() {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            launchIntent?.let { startActivity(it) }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to launch application", e)
        }
    }


    private fun notifyUser(header: String, body: String) {
  //      Toast.makeText(applicationContext, "1", Toast.LENGTH_SHORT).show()
        NOTIFICATION_ID++

        showNotification(applicationContext, header, body )

    }


    val CHANNEL_ID = "CHOLA"
    val CHANNEL_NAME = "CHOLA"
    val CHANNEL_DESC = "CHOLA"


    fun showNotification(context: Context, title: String?, message: String?) {

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.vrddhii_png)
                .setContentTitle(title)
                .setContentText(message)
                .setSound(uri)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)


        val notificationManager = getSystemService(
            NotificationManager::class.java
        )
        createChannelIfRequired(notificationManager)

        notificationManager.notify(
            NOTIFICATION_ID,
            builder.build()
        )


    }


    private fun createChannelIfRequired(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = CHANNEL_DESC
            notificationManager.createNotificationChannel(channel)
        }
    }

}