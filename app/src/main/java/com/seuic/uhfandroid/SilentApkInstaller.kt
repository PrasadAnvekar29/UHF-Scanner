package com.seuic.uhfandroid


import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileInputStream

class SilentApkInstaller(
    private val context: Context
) {

    companion object {
        private const val TAG = "SilentApkInstaller"

        /**
         * Action for the install-result broadcast.
         *
         * IMPORTANT: this must match the action that [InstallResultReceiver]
         * listens for. We build it from the real application id so it is
         * always an explicit, app-scoped broadcast.
         */
        const val ACTION_INSTALL_RESULT =
            BuildConfig.APPLICATION_ID + ".ACTION_INSTALL_RESULT"

        private const val APK_NAME = "base.apk"
    }

    /**
     * True when this app is provisioned as Device Owner. Only in this state
     * (or as a platform-signed / privileged system app) can Android install
     * an APK with NO "Do you want to install this app?" prompt.
     */
    fun isSilentInstallCapable(): Boolean {
        return try {
            val dpm = context.getSystemService(
                Context.DEVICE_POLICY_SERVICE
            ) as DevicePolicyManager
            val owner = dpm.isDeviceOwnerApp(context.packageName)
            Log.d(TAG, "isDeviceOwnerApp=$owner")
            owner
        } catch (e: Exception) {
            Log.e(TAG, "Could not query device owner state", e)
            false
        }
    }

    fun install(apkFile: File): Boolean {

        if (!apkFile.exists()) {
            Log.e(TAG, "APK does not exist: ${apkFile.absolutePath}")
            return false
        }

        if (apkFile.length() <= 0) {
            Log.e(TAG, "APK is empty")
            return false
        }

        return try {

            val silentCapable = isSilentInstallCapable()
            if (!silentCapable) {
                Log.w(
                    TAG,
                    "App is NOT Device Owner / privileged. Android will show " +
                        "the 'Do you want to install this app?' prompt. To " +
                        "install with no prompt, provision this app as Device " +
                        "Owner (adb shell dpm set-device-owner " +
                        "${context.packageName}/.MyDeviceAdminReceiver) or ship " +
                        "it as a platform-signed system app."
                )
            }

            val packageInstaller =
                context.packageManager.packageInstaller

            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )

            /*
             * Do NOT force setAppPackageName() to our own package name.
             * The APK being installed already declares its package name.
             * Forcing it can cause INSTALL_FAILED_* on a mismatch. Letting
             * PackageInstaller read it from the APK is the safe default.
             */

            /*
             * On devices where this app is Device Owner / privileged, this
             * lets the update replace the existing app without a downgrade
             * or reinstall prompt.
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    params.setRequireUserAction(
                        PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
                    )
                } catch (e: Throwable) {
                    Log.w(TAG, "USER_ACTION_NOT_REQUIRED not honored", e)
                }
            }

            val sessionId =
                packageInstaller.createSession(params)

            Log.d(TAG, "Created session: $sessionId")

            val session =
                packageInstaller.openSession(sessionId)

            try {

                FileInputStream(apkFile).use { input ->

                    session.openWrite(
                        APK_NAME,
                        0,
                        apkFile.length()
                    ).use { output ->

                        input.copyTo(output, 64 * 1024)

                        session.fsync(output)
                    }
                }

                Log.d(TAG, "APK copied to installation session")

                /*
                 * Explicit broadcast intent targeting our own package so
                 * InstallResultReceiver receives the result / pending-user-
                 * action callback.
                 */
                val intent = Intent(ACTION_INSTALL_RESULT).apply {
                    setPackage(context.packageName)
                }

                /*
                 * Use FLAG_MUTABLE: the system fills in EXTRA_STATUS and,
                 * when needed, EXTRA_INTENT for the confirmation dialog.
                 * FLAG_ONE_SHOT would invalidate the sender before we can
                 * use the follow-up confirm intent.
                 */
                val mutabilityFlag =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_MUTABLE
                    } else {
                        0
                    }

                val pendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        sessionId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or mutabilityFlag
                    )

                Log.d(TAG, "Committing installation...")

                session.commit(pendingIntent.intentSender)

                Log.d(TAG, "session.commit() completed")

                true

            } catch (e: Exception) {

                Log.e(TAG, "Installation session error", e)

                try {
                    session.abandon()
                } catch (ignored: Exception) {
                }

                false

            } finally {

                session.close()
            }

        } catch (e: Exception) {

            Log.e(TAG, "PackageInstaller error", e)

            false
        }
    }
}
