package com.seuic.uhfandroid


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.seuic.androidreader.sdk.Constants
import com.seuic.androidreader.sdk.ReaderErrorCode
import com.seuic.androidreader.sdk.UhfReaderSdk
import com.seuic.uhfandroid.base.BaseActivity
import com.seuic.uhfandroid.base.BaseViewModel
import com.seuic.uhfandroid.bean.ApkVersion
import com.seuic.uhfandroid.databinding.ActivityMainBinding
import com.seuic.uhfandroid.ext.connectResult
import com.seuic.uhfandroid.ext.isSearching
import com.seuic.uhfandroid.ui.FragmentLabelInventory
import com.seuic.uhfandroid.ui.FragmentParameterSetting
import com.seuic.uhfandroid.ui.FragmentReadAndWrite
import com.seuic.uhfandroid.util.DataStoreUtils
import com.seuic.uhfandroid.util.Utility
import com.seuic.util.common.ToastUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.properties.Delegates
import kotlin.system.exitProcess

class MainActivity : BaseActivity<BaseViewModel, ActivityMainBinding>() {

    private val fragmentWriteReadDeviceConnect by lazy { FragmentLabelInventory() }
    private val fragmentParameterSetting by lazy { FragmentParameterSetting() }
    private val fragmentReadAndWrite by lazy { FragmentReadAndWrite() }
    private val TAG = MainActivity::class.simpleName

    private fun showFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val beginTransaction = fragmentManager.beginTransaction()
        if (!fragment.isAdded) {
            beginTransaction.add(R.id.rl_layout, fragmentWriteReadDeviceConnect)
            beginTransaction.add(R.id.rl_layout, fragmentParameterSetting)
            beginTransaction.add(R.id.rl_layout, fragmentReadAndWrite)
        }
        when (fragment) {
            fragmentWriteReadDeviceConnect -> {
                beginTransaction.hide(fragmentParameterSetting)
                beginTransaction.hide(fragmentReadAndWrite)
                changeBackground(v.llLabelInventory)
            }
            fragmentParameterSetting -> {
                beginTransaction.hide(fragmentWriteReadDeviceConnect)
                beginTransaction.hide(fragmentReadAndWrite)
                changeBackground(v.llParameterSet)
            }
            fragmentReadAndWrite -> {
                beginTransaction.hide(fragmentParameterSetting)
                beginTransaction.hide(fragmentWriteReadDeviceConnect)
                changeBackground(v.llEpcWriteRead)
            }
        }
        beginTransaction.show(fragment)
        beginTransaction.commitAllowingStateLoss()
    }

    override fun initView() {
        lifecycleScope.launchWhenResumed {
            showFragment(fragmentWriteReadDeviceConnect)
        }
        
    }

    override fun initClick() {
        // 标签盘存
        v.llLabelInventory.setOnClickListener {
            showFragment(fragmentWriteReadDeviceConnect)
        }
        // 读写
        v.llEpcWriteRead.setOnClickListener {
            if (isSearching) {
                ToastUtils.showShort(getString(R.string.please_stop_searching))
            } else {
                showFragment(fragmentReadAndWrite)
            }
        }
        // 参数设置
        v.llParameterSet.setOnClickListener {
            if (isSearching) {
                ToastUtils.showShort(getString(R.string.please_stop_searching))
            } else {
                showFragment(fragmentParameterSetting)
            }
        }

        v.llLogs.setOnClickListener {
            if (isSearching) {
                ToastUtils.showShort(getString(R.string.please_stop_searching))
            } else {
              //  showFragment(fragmentLogs)
                val intent = Intent(this, LogsActivity::class.java)
                startActivity(intent )

            }
        }


        v.llBranchId.setOnClickListener {
            val brachId = DataStoreUtils.getBranchId(this)
            val fbToken = DataStoreUtils.getFireBaseToken(this)
            showAlertDialog(null, brachId, fbToken, null, null, false, false ,null)
        }

        v.llSetBranchId.setOnClickListener {
            showSetBranchIdDialog()

        }

        val brachId = DataStoreUtils.getBranchId(this)
        val fbToken = DataStoreUtils.getFireBaseToken(this)
        v.branchId.text = "Branch Id : " + brachId
    }

    private fun reboot() {
        val packageManager: PackageManager = packageManager
        val intent: Intent? = packageManager.getLaunchIntentForPackage(packageName)
        val componentName: ComponentName? = intent?.component
        val mainIntent: Intent = Intent.makeRestartActivityTask(componentName)
        startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }

    override fun initData() {
        //绿灯常亮亮灯且蜂鸣器响一声
        Constants.connectState.observeForever { it ->
            if (it.state == 1) {
                MainScope().launch(Dispatchers.IO) {
                    val isOpen = UhfReaderSdk.isReaderOpen()
                    if (isOpen == null || !isOpen) {
                        UhfReaderSdk.connectReader().let {
                            when (it) {
                                ReaderErrorCode.MT_OK_ERR.value -> {
                                    Log.i(TAG, "连接读写器成功")
                                    connectResult.postValue(0)
                                    UhfReaderSdk.setLed(2, true)
                                    UhfReaderSdk.setLed(3, false)
                                }
                                else -> withContext(Dispatchers.Main) {
                                    Log.i(TAG, "连接读写器错误")
                                    showDialogTip("连接读写器错误${it.toString()}")
                                    UhfReaderSdk.setLed(2, false)
                                    UhfReaderSdk.setLed(3, true)
                                }
                            }
                        }
                    } else {
                        UhfReaderSdk.setLed(2, true)
                        UhfReaderSdk.setLed(3, false)
                    }
                }
            }
            when (it.state) {
                Constants.CONNECTED -> {
                    Log.i(TAG, "内置应用已连接AIDL")
                    UhfReaderSdk.setLed(1, true)
                    UhfReaderSdk.setLed(2, true)
                    UhfReaderSdk.setLed(3, false)
                }
                Constants.DISCONNECT -> {
                    Log.e(TAG, "内置应用未连接AIDL")
                    UhfReaderSdk.setLed(2, false)
                    UhfReaderSdk.setLed(3, false)
                }
                Constants.CONNECTING -> {
                    Log.e(TAG, "内置应用连接AIDL中")
                    UhfReaderSdk.setLed(2, false)
                    UhfReaderSdk.setLed(3, false)
                }
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun changeBackground(view: View) {
        when (view) {
            v.llLabelInventory -> {
                v.llLabelInventory.background = applicationContext.getDrawable(R.drawable.rectangle_line_top)
                v.llParameterSet.background =
                    applicationContext.getDrawable(R.drawable.rectangle_line_top_nopressed)
                v.llEpcWriteRead.background =
                    applicationContext.getDrawable(R.drawable.rectangle_line_top_nopressed)
            }
            v.llParameterSet -> {
                v.llLabelInventory.background =
                    applicationContext.getDrawable(R.drawable.rectangle_line_top_nopressed)
                v.llParameterSet.background = applicationContext.getDrawable(R.drawable.rectangle_line_top)
                v.llEpcWriteRead.background =
                    applicationContext.getDrawable(R.drawable.rectangle_line_top_nopressed)
            }
            v.llEpcWriteRead -> {
                v.llLabelInventory.background =
                    applicationContext.getDrawable(R.drawable.rectangle_line_top_nopressed)
                v.llParameterSet.background =
                    applicationContext.getDrawable(R.drawable.rectangle_line_top_nopressed)
                v.llEpcWriteRead.background = applicationContext.getDrawable(R.drawable.rectangle_line_top)
            }
        }
    }

    override fun initVM() {

    }

    fun showDialogTip(msg: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(msg)
        builder.setPositiveButton(
            "确定"
        ) { _, _ ->
            builder.create().dismiss()
        }
        // 显示
        builder.show()
    }

    private var backPressedTime by Delegates.observable(0L) { _, old, new ->
        /**
         * 2 次的时间间隔小于2秒就退出了
         */
        if (new - old < 2000) {
            UhfReaderSdk.stopBZForSearching()
            UhfReaderSdk.inventoryStop()
            exitProcess(0)
        } else {
            ToastUtils.showShort(getString(R.string.press_back_again))
        }
    }

    /**
     * 从新写back方法
     */
    override fun onBackPressed() {
        backPressedTime = System.currentTimeMillis()
    }

    fun showSetBranchIdDialog(){
        val brachId = DataStoreUtils.getBranchId(this)
        val fbToken = DataStoreUtils.getFireBaseToken(this)

        showAlertDialog(   null, brachId , fbToken,
            getString(R.string.yes), getString(R.string.no), false, true ,object : AlertDialogActionListener {
                override fun action(isPositive: Boolean) {
                    try {
                        val brachId = DataStoreUtils.getBranchId(this@MainActivity)
                        val fbToken = DataStoreUtils.getFireBaseToken(this@MainActivity)
                        v.branchId.text = "Branch Id : " + brachId

                    }catch (e : Exception){

                    }

                }
            })
    }

    fun showDialog(){
        showSetBranchIdDialog()
    }


    private val firebaseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra("reader_request_type")
            Log.d("firebase 1", status.toString())
         //   fragmentWriteReadDeviceConnect.readerRequestType(status!!)
            readerRequestType(status!!)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(
                firebaseReceiver,
                IntentFilter(Utility.ACTION_APPLICATION_STATUS_UPDATE),
                RECEIVER_NOT_EXPORTED
            )
        }

        readerRequestType("")
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(firebaseReceiver)
    }


    fun readerRequestType(notificationType: String){
        when (notificationType) {
            "START" -> {
                fragmentWriteReadDeviceConnect.startReader()
            }
            "STOP" -> {
                fragmentWriteReadDeviceConnect.stopReader()
            }
            "UPDATE" ->{
                val apkVersion = DataStoreUtils.getApkVersion(applicationContext)
                if(apkVersion != null && !TextUtils.isEmpty(apkVersion.apkVersion) && !apkVersion.apkVersion.equals(Utility.getVersion(applicationContext)))    {
                    updateAPk(apkVersion)
                }
            }
            else -> {

                val apkVersion = DataStoreUtils.getApkVersion(applicationContext)
                if(apkVersion != null && !TextUtils.isEmpty(apkVersion.apkVersion) && !apkVersion.apkVersion.equals(Utility.getVersion(applicationContext)))    {
                    updateAPk(apkVersion)
                } else {
                    if(DataStoreUtils.getRequestType(applicationContext).equals("START", true)){
                        fragmentWriteReadDeviceConnect.startReader()
                    } else {
                        fragmentWriteReadDeviceConnect.stopReader()
                    }
                }
            }
        }
    }

    fun updateAPk(apkVersion : ApkVersion){
        AlertDialog.Builder(this)
            .setTitle("Warning")
            .setMessage("Updated Version of App is available. Update your apk first")
            .setCancelable(false)
            .setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
                vm.showLoading("Please wait...", this)
                vm.downloadApk(apkVersion.apkUrl!!, this)
                    .observe(this, { resultState: String ->
                        newApkPath(resultState)
                    })
            }
            .show()
    }




    private fun newApkPath(apkPath: String) {
        try {
            if (!TextUtils.isEmpty(apkPath)) {
                vm.hideLoading()
                val toInstall = File(apkPath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val apkUri = FileProvider.getUriForFile(
                        applicationContext,
                        applicationContext.packageName + ".fileprovider",
                        toInstall
                    )
                    val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
                    intent.setData(apkUri)
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } else {
                    val apkUri = Uri.fromFile(toInstall)
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
        } catch (e: java.lang.Exception) {

        }
    }





}