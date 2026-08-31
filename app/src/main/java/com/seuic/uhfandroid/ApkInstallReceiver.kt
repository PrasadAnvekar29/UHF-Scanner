package com.seuic.uhfandroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ApkInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Intent.ACTION_PACKAGE_REPLACED) {

            val packageName = intent.data?.schemeSpecificPart

            if (packageName != null && packageName == context.packageName) {

                val launchIntent =
                    context.packageManager.getLaunchIntentForPackage(packageName)

                launchIntent?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }

                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            }
        }
    }
}