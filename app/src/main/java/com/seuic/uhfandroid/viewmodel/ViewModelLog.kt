package com.seuic.uhfandroid.viewmodel

import aidl.IReadListener
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.seuic.androidreader.bean.TagInfo
import com.seuic.androidreader.sdk.ReaderErrorCode
import com.seuic.androidreader.sdk.UhfReaderSdk
import com.seuic.uhfandroid.base.BaseViewModel
import com.seuic.uhfandroid.database.LogEntry
import com.seuic.uhfandroid.database.TagDataEntry
import com.seuic.uhfandroid.database.UFHDatabase
import com.seuic.uhfandroid.ext.currentAntennaArray
import com.seuic.uhfandroid.ext.totalCounts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import java.util.Vector

class ViewModelLog : BaseViewModel() {

    private var mDataBase : UFHDatabase? = null
    private var mContext : Context? = null





    var logListData = MutableLiveData<MutableList<LogEntry>>()
    var logData = MutableLiveData<MutableList<LogEntry>>()



    fun getLogList(){

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if(mDataBase == null){
                    mDataBase = UFHDatabase.getDatabase(mContext!!)
                }

                mDataBase?.logDao()?.getList()?.let {
                    logListData.postValue(it.toMutableList())
                }

            }catch (e : Exception){

            }
        }
    }


}