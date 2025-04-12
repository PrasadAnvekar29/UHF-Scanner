package com.seuic.uhfandroid


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.seuic.androidreader.sdk.Constants
import com.seuic.androidreader.sdk.ReaderErrorCode
import com.seuic.androidreader.sdk.UhfReaderSdk
import com.seuic.uhfandroid.base.BaseActivity
import com.seuic.uhfandroid.base.BaseViewModel
import com.seuic.uhfandroid.databinding.ActivityMainBinding
import com.seuic.uhfandroid.ext.connectResult
import com.seuic.uhfandroid.ext.isSearching
import com.seuic.uhfandroid.ui.FragmentLabelInventory
import com.seuic.uhfandroid.ui.FragmentParameterSetting
import com.seuic.uhfandroid.ui.FragmentReadAndWrite
import com.seuic.uhfandroid.util.DataStoreUtils
import com.seuic.util.common.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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


        v.llBranchId.setOnClickListener {
            val brachId = DataStoreUtils.getBranchId(this)
            showAlertDialog(null, brachId, null, null, false, false ,null)
        }

        v.llSetBranchId.setOnClickListener {
            showSetBranchIdDialog()

        }

        val brachId = DataStoreUtils.getBranchId(this)
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

        showAlertDialog(   null, brachId ,
            getString(R.string.yes), getString(R.string.no), false, true ,object : AlertDialogActionListener {
                override fun action(isPositive: Boolean) {
                    try {
                        val brachId = DataStoreUtils.getBranchId(this@MainActivity)
                        v.branchId.text = "Branch Id : " + brachId

                    }catch (e : Exception){

                    }

                }
            })
    }

    fun showDialog(){
        showSetBranchIdDialog()
    }


}