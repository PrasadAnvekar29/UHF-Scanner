package com.seuic.uhfandroid.viewmodel


import aidl.IReadListener
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.seuic.androidreader.bean.TagInfo
import com.seuic.androidreader.sdk.ReaderErrorCode
import com.seuic.androidreader.sdk.UhfReaderSdk
import com.seuic.uhfandroid.base.BaseViewModel
import com.seuic.uhfandroid.bean.TagBean
import com.seuic.uhfandroid.ext.currentAntennaArray
import com.seuic.uhfandroid.ext.inventoryDatas
import com.seuic.uhfandroid.ext.inventoryListDatas
import com.seuic.uhfandroid.ext.totalCounts
import java.util.*


class ViewModelLabelInventory : BaseViewModel() {
    // 0:不工作，1:单次，2:连续
    private var workMode = 0
    private val TAG = ViewModelLabelInventory::class.simpleName
    var listTagData = Vector<TagBean>()

    // 单次寻卡得到的数据
    var tagData = MutableLiveData<TagBean>()
    var errData = MutableLiveData<ReaderErrorCode>()
    var tagListData = MutableLiveData<MutableList<TagBean>>()
    var statenvtick = 0L
    private var startSearching = true
    private val readListener by lazy {
        object : IReadListener.Stub() {
            override fun tagRead(tags: List<TagInfo>) {
                for (bean in tags) {
                    when (workMode) {
                        0 -> Log.i(TAG, "已停止寻卡")
                        1 -> {
                            // Single card search
                            val tagBean = TagBean(
                                bean.getEpcStr(),
                                bean.RSSI,
                                1,
                                bean.getAntennaIDStr(),
                                bean.getEmbeddedStr()
                            )
                            // Notification label inventory interface
                            tagData.postValue(tagBean)
                            // Notification tag reading and writing interface
                            inventoryDatas.postValue(tagBean)
                        }
                        2 -> {
                            // Continuous card search
                            val tagBean = TagBean(
                                bean.getEpcStr(),
                                bean.RSSI,
                                1,
                                bean.AntennaID.toString(),
                                bean.getEmbeddedStr()
                            )
//                        Log.i(TAG, "EPC: " + tagBean.epcId)
                            // Number of times found+1
                            Log.i(TAG, "EPC count: " + totalCounts.incrementAndGet().toString())
                            setListData(tagBean)
                            // Notification label inventory interface
                            tagListData.postValue(listTagData)
                            // Notification tag reading and writing interface
                            inventoryListDatas.postValue(listTagData)
                        }
                    }
                }
            }

            override fun tagReadException(errorCode: Int) {
                errData.postValue(ReaderErrorCode.valueOf(errorCode))
            }

            override fun getTag(): String {
                return "" + this.hashCode()
            }
        }
    }


    fun registerListener() {
        UhfReaderSdk.registerReadListener(readListener)
    }

    fun unregisterListener() {
        UhfReaderSdk.unregisterReadListener(readListener)
    }

    // 单次寻卡
    fun singleCard() {
        workMode = 1
        val timeOut = 50
        UhfReaderSdk.inventoryOnce(currentAntennaArray, timeOut)
    }

    // 连续寻卡
    fun searchForCard() {
        totalCounts.set(0)
        workMode = 2
        UhfReaderSdk.inventoryStart(currentAntennaArray)
        statenvtick = System.currentTimeMillis()
        UhfReaderSdk.startBZForSearching()
    }

    // Stop continuous card search
    fun stopSearchForCard() {
        workMode = 0
        startSearching = false
        UhfReaderSdk.stopBZForSearching()
        UhfReaderSdk.inventoryStop()
    }

    private fun setListData(bean: TagBean?) {
        if (bean != null) {
            val judgeExist = judgeExist(bean.epcId, bean.antenna)
            if (judgeExist != -1) {// Add if repeated count
                val tagEpc = listTagData[judgeExist]
                tagEpc.times += 1
                tagEpc.rssi = bean.rssi
                tagEpc.antenna = bean.antenna
                tagEpc.additionalData = bean.additionalData
                listTagData[judgeExist] = tagEpc
            } else {
                listTagData.add(bean)
            }
        }
    }

    private fun judgeExist(id: String, antenna: String): Int {
        var result = -1
        // TODO: 2022/2/18 java.util.ConcurrentModificationException
        for ((index, bean) in listTagData.withIndex()) {
            if (bean.epcId == id && bean.antenna == antenna  ) {
                result = index
            }
        }
        return result
    }
}