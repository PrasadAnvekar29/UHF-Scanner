package com.seuic.uhfandroid



import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log

class InstallResultReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "APK_INSTALL"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        Log.d(TAG, "================================")
        Log.d(TAG, "InstallResultReceiver TRIGGERED")
        Log.d(TAG, "action = ${intent.action}")

        /*
         * Print all extras.
         */
        intent.extras?.let { extras ->

            for (key in extras.keySet()) {

                Log.d(
                    TAG,
                    "EXTRA: $key = ${extras.get(key)}"
                )
            }

        } ?: run {

            Log.d(
                TAG,
                "NO EXTRAS"
            )
        }

        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            -999
        )

        val message =
            intent.getStringExtra(
                PackageInstaller.EXTRA_STATUS_MESSAGE
            )

        val packageName =
            intent.getStringExtra(
                PackageInstaller.EXTRA_PACKAGE_NAME
            )

        Log.d(TAG, "status = $status")
        Log.d(TAG, "message = $message")
        Log.d(TAG, "package = $packageName")

        when (status) {

            PackageInstaller.STATUS_SUCCESS -> {

                Log.d(
                    TAG,
                    "APK INSTALL SUCCESS"
                )

                openApplication(context)
            }

            PackageInstaller.STATUS_PENDING_USER_ACTION -> {

                Log.e(
                    TAG,
                    "USER ACTION REQUIRED (device cannot install silently)"
                )

                val confirmIntent =
                    if (Build.VERSION.SDK_INT >= 33) {

                        intent.getParcelableExtra(
                            Intent.EXTRA_INTENT,
                            Intent::class.java
                        )

                    } else {

                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<Intent>(
                            Intent.EXTRA_INTENT
                        )
                    }

                Log.e(
                    TAG,
                    "confirmIntent = $confirmIntent"
                )

                /*
                 * Fallback: if the OS will not install silently (this app is
                 * not Device Owner / privileged / platform-signed), launch
                 * the system confirmation dialog so the update still
                 * proceeds instead of stalling.
                 */
                if (confirmIntent != null) {
                    try {
                        confirmIntent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                        context.startActivity(confirmIntent)
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Unable to launch confirm intent",
                            e
                        )
                    }
                }
            }

            else -> {

                Log.e(
                    TAG,
                    "APK INSTALL FAILED"
                )

                Log.e(
                    TAG,
                    "status = $status"
                )

                Log.e(
                    TAG,
                    "message = $message"
                )
            }
        }

        Log.d(TAG, "================================")
    }

    private fun openApplication(
        context: Context
    ) {

        try {

            val launchIntent =
                context.packageManager
                    .getLaunchIntentForPackage(
                        context.packageName
                    )

            if (launchIntent == null) {

                Log.e(
                    TAG,
                    "Launch intent not found"
                )

                return
            }

            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )

            context.startActivity(
                launchIntent
            )

            Log.d(
                TAG,
                "Application launched"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Unable to launch application",
                e
            )
        }
    }
}