package com.seuic.uhfandroid

import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.esthenos.presentation.ui.attendance.LogsAdapter
import com.seuic.uhfandroid.base.BaseActivity
import com.seuic.uhfandroid.database.LogEntry
import com.seuic.uhfandroid.database.UFHDatabase
import com.seuic.uhfandroid.databinding.FragmentLogsBinding
import com.seuic.uhfandroid.viewmodel.ViewModelLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogsActivity: BaseActivity <ViewModelLog, FragmentLogsBinding>() {

    private var adapter : LogsAdapter? = null ;
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
                    adapter = LogsAdapter(list, this@LogsActivity)
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