package com.seuic.uhfandroid.util

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Locale
import kotlin.collections.indices
import kotlin.text.contains

class Utility {

    companion object{

        const val ACTION_APPLICATION_STATUS_UPDATE = "com.ukcorp.vrddhii.firebase.READER_REQUEST"



        fun isDeviceRooted(): Boolean {
            return checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
        }

        fun searchForMagisk(context: Context): Boolean {
            var returnValue = false
            val pm = context.packageManager
            @SuppressLint("QueryPermissionsNeeded") val installedPackages = pm.getInstalledPackages(0)
            for (i in installedPackages.indices) {
                val info = installedPackages[i]
                val appInfo = info.applicationInfo
                val nativeLibraryDir = appInfo.nativeLibraryDir
                val packageName = appInfo.packageName
                Log.i("Magisk Detection", "Checking App: $nativeLibraryDir")
                val f = File("$nativeLibraryDir/libstub.so")
                if (f.exists()) {
                    returnValue = true
                }
            }
            return returnValue
        }

        fun checkRunningProcesses(context: Context): Boolean {
            var returnValue = false
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            // Get currently running application processes
            val list = manager.getRunningServices(300)
            if (list != null) {
                var tempName: String
                for (i in list.indices) {
                    tempName = list[i].process
                    if (tempName.contains("fridaserver") || tempName.contains("frida")) {
                        returnValue = true
                    }
                }
            }
            return returnValue
        }

        private fun checkRootMethod1(): Boolean {
            val buildTags = Build.TAGS
            return buildTags != null && buildTags.contains("test-keys")
        }

        private fun checkRootMethod2(): Boolean {
            val paths = arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
            )
            for (path in paths) {
                if (File(path).exists()) return true
            }
            return false
        }

        private fun checkRootMethod3(): Boolean {
            var process: Process? = null
            return try {
                process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
                val `in` = BufferedReader(InputStreamReader(process.inputStream))
                if (`in`.readLine() != null) true else false
            } catch (t: Throwable) {
                false
            } finally {
                process?.destroy()
            }
        }

        fun checkBuildConfigIsEmulator(context: Context): Boolean {
            val androidId =
                Settings.Secure.getString(context.contentResolver, "android_id")
            return (Build.MANUFACTURER.contains("Genymotion")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.lowercase(Locale.getDefault()).contains("droid4x")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.HARDWARE === "goldfish" || Build.HARDWARE === "vbox86" || Build.HARDWARE.lowercase(
                Locale.getDefault()
            ).contains("nox")
                    || Build.FINGERPRINT.startsWith("generic")
                    || Build.PRODUCT === "sdk" || Build.PRODUCT === "google_sdk" || Build.PRODUCT === "sdk_x86" || Build.PRODUCT === "vbox86p" || Build.PRODUCT.lowercase(
                Locale.getDefault()
            ).contains("nox")
                    || Build.BOARD.lowercase(Locale.getDefault()).contains("nox")
                    || Build.BRAND.startsWith("generic")
                    || Build.HARDWARE.contains("ranchu")
                    || androidId == null)
        }


        fun getVersion(context: Context): String {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                return packageInfo!!.versionName!!
            } catch (e: PackageManager.NameNotFoundException) {

            }
            return "version-na"
        }

    }
}