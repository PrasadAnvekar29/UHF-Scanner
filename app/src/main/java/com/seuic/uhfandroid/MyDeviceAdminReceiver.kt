package com.seuic.uhfandroid


import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "DeviceAdminReceiver"
    }

    override fun onEnabled(
        context: Context,
        intent: Intent
    ) {
        super.onEnabled(context, intent)

        Log.d(
            TAG,
            "Device Admin enabled"
        )
    }

    override fun onDisabled(
        context: Context,
        intent: Intent
    ) {
        super.onDisabled(context, intent)

        Log.d(
            TAG,
            "Device Admin disabled"
        )
    }

    override fun onProfileProvisioningComplete(
        context: Context,
        intent: Intent
    ) {
        super.onProfileProvisioningComplete(
            context,
            intent
        )

        Log.d(
            TAG,
            "Device provisioning completed"
        )
    }

    override fun onLockTaskModeEntering(
        context: Context,
        intent: Intent,
        pkg: String
    ) {
        super.onLockTaskModeEntering(
            context,
            intent,
            pkg
        )

        Log.d(
            TAG,
            "Lock task mode entered: $pkg"
        )
    }

    override fun onLockTaskModeExiting(
        context: Context,
        intent: Intent
    ) {
        super.onLockTaskModeExiting(
            context,
            intent
        )

        Log.d(
            TAG,
            "Lock task mode exited"
        )
    }
}