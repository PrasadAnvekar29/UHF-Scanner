package com.seuic.uhfandroid

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.esthenos.presentation.ui.attendance.AttendanceListAdapter
import com.seuic.androidreader.sdk.Constants
import com.seuic.androidreader.sdk.ReaderErrorCode
import com.seuic.androidreader.sdk.UhfReaderSdk
import com.seuic.uhfandroid.base.BaseActivity
import com.seuic.uhfandroid.base.BaseActivity.AlertDialogActionListener
import com.seuic.uhfandroid.base.BaseViewModel
import com.seuic.uhfandroid.database.LogEntry
import com.seuic.uhfandroid.database.UFHDatabase
import com.seuic.uhfandroid.databinding.ActivityMainBinding
import com.seuic.uhfandroid.databinding.FragmentLogsBinding
import com.seuic.uhfandroid.ext.connectResult
import com.seuic.uhfandroid.ext.isSearching
import com.seuic.uhfandroid.ui.FragmentLabelInventory
import com.seuic.uhfandroid.ui.FragmentParameterSetting
import com.seuic.uhfandroid.ui.FragmentReadAndWrite
import com.seuic.uhfandroid.util.DataStoreUtils
import com.seuic.uhfandroid.viewmodel.ViewModelLog
import com.seuic.util.common.ToastUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.properties.Delegates
import kotlin.system.exitProcess

class LogsActivity: BaseActivity <ViewModelLog, FragmentLogsBinding>() {

    private var adapter : AttendanceListAdapter? = null ;
    private val TAG = MainActivity::class.simpleName
    private var list : MutableList<LogEntry> = ArrayList()

    override fun initView() {



    }

    override fun initClick() {


    }

    override fun initData() {

    }

    override fun initVM() {

    }
    private var mDataBase : UFHDatabase? = null
    override fun onResume() {
        super.onResume()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if(mDataBase == null){
                    mDataBase = UFHDatabase.getDatabase(mContext!!)
                }

                var listData =  mDataBase?.logDao()?.getList()

                list.clear()
                list.addAll(listData!!)

                withContext(Dispatchers.Main){
                    adapter = AttendanceListAdapter(list, this@LogsActivity)
                    v.rvLogs.adapter = adapter
                    v.rvLogs.layoutManager = LinearLayoutManager(this@LogsActivity)
                    adapter?.notifyDataSetChanged()
                    Toast.makeText(this@LogsActivity, "Count "+list?.size, Toast.LENGTH_SHORT).show()
                }

            }catch (e : Exception){
                e.printStackTrace()
            }
        }

    }



}