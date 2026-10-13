package com.seuic.uhfandroid


import android.Manifest
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.DataOutputStream
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

    /**
     * When this app is provisioned as Device Owner, explicitly grant itself
     * the REQUEST_INSTALL_PACKAGES permission. Some Android versions still
     * block a PackageInstaller session for a Device Owner app unless the
     * "install unknown apps" permission is granted, so we set it here to make
     * silent installs reliable once provisioning is done.
     *
     * No-op (safely ignored) when the app is not Device Owner.
     */
    private fun ensureInstallPermissionAsDeviceOwner() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        if (!isSilentInstallCapable()) {
            return
        }
        try {
            val dpm = context.getSystemService(
                Context.DEVICE_POLICY_SERVICE
            ) as DevicePolicyManager
            val admin = ComponentName(
                context,
                MyDeviceAdminReceiver::class.java
            )

            // 1) Grant the "install packages" permission to ourselves so the
            //    OS never falls back to the "install unknown apps" prompt.
            dpm.setPermissionGrantState(
                admin,
                context.packageName,
                Manifest.permission.REQUEST_INSTALL_PACKAGES,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
            )
            Log.d(TAG, "Granted REQUEST_INSTALL_PACKAGES as Device Owner")

            // 2) Make sure the "install unknown sources" user restriction is
            //    NOT set, otherwise the session can still stall for user
            //    action even for a Device Owner.
            try {
                dpm.clearUserRestriction(
                    admin,
                    android.os.UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    dpm.clearUserRestriction(
                        admin,
                        android.os.UserManager
                            .DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not clear unknown-sources restriction", e)
            }

            // 3) Disable Play Protect / package verification which can also
            //    inject a confirmation step on some OEM builds.
            try {
                dpm.setSecureSetting(
                    admin,
                    "verifier_verify_adb_installs",
                    "0"
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not disable install verification", e)
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Could not grant install permission as Device Owner",
                e
            )
        }
    }

    /**
     * Attempt to make this app Device Owner at launch so that later updates
     * can install with no user prompt.
     *
     * IMPORTANT — Android security model:
     *  - A normal app CANNOT grant itself Device Owner. `dpm set-device-owner`
     *    requires the shell UID (adb) or root (UID 0).
     *  - It also only works when the device has NO accounts configured.
     *
     * Therefore this method can only succeed on a ROOTED device. On a
     * non-rooted device it is a safe no-op and simply logs guidance for a
     * one-time `adb shell dpm set-device-owner ...` provisioning step.
     *
     * Behaviour:
     *  1. Already Device Owner        -> nothing to do, returns true.
     *  2. Rooted                      -> runs the provisioning command as root.
     *  3. Neither                     -> logs guidance, returns false.
     *
     * @return true if the app is (or just became) Device Owner.
     */
    fun ensureDeviceOwnerOnLaunch(): Boolean {

        // 1) Already provisioned -> done.
        if (isSilentInstallCapable()) {
            Log.d(TAG, "Already Device Owner; silent install ready")
            ensureInstallPermissionAsDeviceOwner()
            return true
        }

        // 2) Try to self-provision via root. This only works on rooted
        //    devices that also have no accounts configured.
        val component =
            "${context.packageName}/${MyDeviceAdminReceiver::class.java.name}"

        val provisioned = runDpmSetDeviceOwnerAsRoot(component)

        if (provisioned) {
            Log.d(TAG, "Device Owner set via root at launch")
            // Give the framework a moment, then lock in install permissions.
            ensureInstallPermissionAsDeviceOwner()
            return isSilentInstallCapable()
        }

        // 3) Cannot self-provision. Log the one-time manual step.
        Log.w(
            TAG,
            "Not Device Owner and no root. Silent install unavailable. " +
                "Provision once from a PC (device must have NO accounts):\n" +
                "adb shell dpm set-device-owner $component"
        )
        return false
    }

    /**
     * Runs `dpm set-device-owner <component>` inside a root shell.
     * Returns true only when the command reports success.
     *
     * Fails (returns false) on non-rooted devices, or when the device
     * already has accounts / an existing owner.
     */
    private fun runDpmSetDeviceOwnerAsRoot(component: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { os ->
                os.writeBytes("dpm set-device-owner $component\n")
                os.writeBytes("exit\n")
                os.flush()
            }
            val exit = process.waitFor()
            val out = process.inputStream.bufferedReader().readText()
            val err = process.errorStream.bufferedReader().readText()
            Log.d(
                TAG,
                "root dpm set-device-owner exit=$exit out=$out err=$err"
            )

            // dpm prints "Success:" on success. Also treat an existing-owner
            // error as effectively provisioned.
            val ok = out.contains("Success", ignoreCase = true)
            val alreadyOwner =
                err.contains("already", ignoreCase = true) &&
                    err.contains("owner", ignoreCase = true)

            (exit == 0 && ok) || alreadyOwner
        } catch (e: Exception) {
            // No root available — expected on most devices.
            Log.d(TAG, "Root device-owner provisioning not available: ${e.message}")
            false
        }
    }

    /**
     * Entry point. Returns true when an install was started (or completed via
     * root). A false return means nothing could be started.
     */
    fun install(apkFile: File): Boolean {

        if (!apkFile.exists()) {
            Log.e(TAG, "APK does not exist: ${apkFile.absolutePath}")
            return false
        }

        if (apkFile.length() <= 0) {
            Log.e(TAG, "APK is empty")
            return false
        }

        // 1) Rooted non-GSM device: install directly via shell, fully silent.
        if (installWithRoot(apkFile)) {
            Log.d(TAG, "Installed via root shell")
            openApplication()
            return true
        }

        // 2) PackageInstaller (Device Owner / platform-signed -> silent).
        return installWithPackageInstaller(apkFile)
    }

    /**
     * Try `pm install` as root. Silent on rooted devices with no dialog.
     * Returns true only if the install command reported success.
     */
    private fun installWithRoot(apkFile: File): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { os ->
                // -r replace existing, -d allow downgrade.
                os.writeBytes(
                    "pm install -r -d \"${apkFile.absolutePath}\"\n"
                )
                os.writeBytes("exit\n")
                os.flush()
            }
            val exit = process.waitFor()
            val out = process.inputStream.bufferedReader().readText()
            val err = process.errorStream.bufferedReader().readText()
            Log.d(TAG, "root pm install exit=$exit out=$out err=$err")

            exit == 0 && out.contains("Success", ignoreCase = true)
        } catch (e: Exception) {
            // No root available. This is expected on most devices.
            Log.d(TAG, "Root install not available: ${e.message}")
            false
        }
    }

    private fun installWithPackageInstaller(apkFile: File): Boolean {

        return try {

            if (!isSilentInstallCapable()) {
                Log.w(
                    TAG,
                    "App is NOT Device Owner / privileged. Android may show " +
                        "the install prompt. To install with no prompt, " +
                        "provision as Device Owner (adb shell dpm " +
                        "set-device-owner " +
                        "${context.packageName}/.MyDeviceAdminReceiver), ship " +
                        "as a platform-signed system app, or root the device."
                )
            } else {
                // Device Owner: make sure we are allowed to install silently.
                ensureInstallPermissionAsDeviceOwner()
            }

            val packageInstaller = context.packageManager.packageInstaller

            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )

            // Mark this as a policy/self update so the OS does not treat it as
            // a user-initiated install that needs confirmation.
            try {
                params.setInstallReason(
                    PackageManager.INSTALL_REASON_POLICY
                )
            } catch (e: Throwable) {
                Log.w(TAG, "setInstallReason not honored", e)
            }

            // Let PackageInstaller read the package name from the APK itself.

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    params.setRequireUserAction(
                        PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
                    )
                } catch (e: Throwable) {
                    Log.w(TAG, "USER_ACTION_NOT_REQUIRED not honored", e)
                }
            }

            val sessionId = packageInstaller.createSession(params)
            Log.d(TAG, "Created session: $sessionId")

            val session = packageInstaller.openSession(sessionId)

            try {
                FileInputStream(apkFile).use { input ->
                    session.openWrite(APK_NAME, 0, apkFile.length())
                        .use { output ->
                            input.copyTo(output, 64 * 1024)
                            session.fsync(output)
                        }
                }

                Log.d(TAG, "APK copied to installation session")

                val intent = Intent(ACTION_INSTALL_RESULT).apply {
                    setPackage(context.packageName)
                }

                val mutabilityFlag =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_MUTABLE
                    } else {
                        0
                    }

                val pendingIntent = PendingIntent.getBroadcast(
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

    private fun openApplication() {
        try {
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName) ?: return
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to launch application", e)
        }
    }
}
