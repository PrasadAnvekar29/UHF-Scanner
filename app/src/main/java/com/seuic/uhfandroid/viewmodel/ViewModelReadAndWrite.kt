package com.seuic.uhfandroid.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.seuic.androidreader.sdk.ReaderErrorCode
import com.seuic.androidreader.sdk.UhfReaderSdk
import com.seuic.uhfandroid.base.BaseViewModel
import com.seuic.uhfandroid.bean.TagBean
import com.seuic.uhfandroid.util.BaseUtil

class ViewModelReadAndWrite : BaseViewModel() {
    private val TAG = ViewModelReadAndWrite::class.simpleName
    var listTagData = mutableListOf<TagBean>()
    var tagDetailData = MutableLiveData<String>()
    var tagResultWrite = MutableLiveData<Int>()
    var tagResultLock = MutableLiveData<Int>()
    var tagResultKill = MutableLiveData<Int>()
    var tagWriteResult = MutableLiveData<Int>()
    var tagResultFilter = MutableLiveData<Boolean>()
    var currentAntenna = 1

    // 设置过滤
    fun setFilter(sdt: String, bank: Int, data: String, suit: Boolean, backResult: Boolean) {
        val er = UhfReaderSdk.setFilter(sdt.toInt(), bank, data, suit, true)
        if (er == ReaderErrorCode.MT_OK_ERR.value) {
            if (backResult) tagResultFilter.postValue(true)
            Log.i(TAG, "setFilter: 设置成功")
        } else {
            if (backResult) tagResultFilter.postValue(false)
            Log.e(TAG, "setFilter: 设置失败，错误码：${er}")
        }
    }

    // 读操作 设置密码 设置过滤 块数
    fun readTagData(antenna: Int, bank: Int, blockCount: Int, startAddress: Int, accessPassword: String, epc: String) {
        UhfReaderSdk.readTagData(antenna, bank, blockCount, startAddress, accessPassword, epc)?.let { result ->
            if (result.err == ReaderErrorCode.MT_OK_ERR.value) {
                result.data?.let {
                    val out = CharArray(it.size * 2)
                    BaseUtil.Hex2Str(it, it.size, out)
                    tagDetailData.postValue(String(out))
                    Log.i(TAG, "readTagData: 读取成功")
                }
            } else {
                Log.e(TAG, "readTagData: 读取失败，错误码：${result.err}")
                tagDetailData.postValue("")
            }
        }
    }

    // 写操作
    fun writeTagData(antenna: Int, bank: Int, data: String, startAddress: Int, accessPassword: String, epc: String) {
        val er = UhfReaderSdk.writeTagData(antenna, bank, data, startAddress, accessPassword, epc)
        tagResultWrite.postValue(er)
        if (er == ReaderErrorCode.MT_OK_ERR.value) {
            Log.i(TAG, "writeTagData: 写入成功")
        } else {
            Log.e(TAG, "writeTagData: 写入失败，错误码：${er}")
        }
    }

    // 锁操作 0解锁 1暂时锁定 2永久锁定 失败-1
    fun lockTag(antenna: Int, lBank: Int, lType: Int, accessWord: String, epc: String) {
        val result = UhfReaderSdk.lockTag(antenna, lBank, lType, accessWord, epc)
        if (result == null) {
            tagResultLock.postValue(-1)
        } else {
            if (result.err == ReaderErrorCode.MT_OK_ERR.value) {
                tagResultLock.postValue(lType)
            } else {
                tagResultLock.postValue(-1)
            }
        }
    }

    // 销毁操作
    fun destroyTag(antenna: Int, accessPassword: String, epc: String) {
        val er = UhfReaderSdk.killTag(antenna, accessPassword, epc)
        if (er == ReaderErrorCode.MT_OK_ERR.value) {
            tagResultKill.postValue(0)
            Log.i(TAG, "destroyTag: 销毁成功")
        } else {
            tagResultKill.postValue(1)
            Log.e(TAG, "destroyTag: 销毁失败")
        }
    }
}