package com.seuic.uhfandroid.viewmodel

import android.content.Context
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.MutableLiveData
import com.seuic.androidreader.bean.AntPowerData
import com.seuic.androidreader.sdk.Constants
import com.seuic.androidreader.sdk.ReaderErrorCode
import com.seuic.androidreader.sdk.UhfReaderSdk
import com.seuic.uhfandroid.base.BaseViewModel
import com.seuic.uhfandroid.ext.currentAntennaArray
import com.seuic.uhfandroid.util.BaseUtils
import com.seuic.uhfandroid.util.DataStoreUtils
import com.seuic.uhfandroid.util.SharedPreferencesUtils

class ViewModelParameterSetting : BaseViewModel() {
  //  var power = MutableLiveData<Int>()
  var ant1Power = MutableLiveData<Int>()
  var ant2Power = MutableLiveData<Int>()
  var ant3Power = MutableLiveData<Int>()
  var ant4Power = MutableLiveData<Int>()
  var ant5Power = MutableLiveData<Int>()
  var ant6Power = MutableLiveData<Int>()
  var ant7Power = MutableLiveData<Int>()
  var ant8Power = MutableLiveData<Int>()

    var region = MutableLiveData<Int>()
    var session = MutableLiveData<Int>()

    // profile下标
    var profile = MutableLiveData<Int>()
    var target = MutableLiveData<Int>()
    var qValue = MutableLiveData<Int>()
    var hardversion = MutableLiveData<String>()
    var firmverion = MutableLiveData<String>()
    var readerGpi = MutableLiveData<ArrayList<Int>>()
    var checkAnt = MutableLiveData<Boolean>()
    var connAnt = MutableLiveData<Int>()
    var additionalDataPassword = MutableLiveData<String>()//附加数据访问密码
    var additionalDataStartAddress = MutableLiveData<String>()//附加数据起始地址
    var additionalDataByteCount = MutableLiveData<String>()//附加数据字节
    var additionalDataBank = MutableLiveData<String>()//附加数据区域
    var additionalDataEnable = MutableLiveData<Boolean>()//附加数据是否启用
    var tempture = MutableLiveData<Int>()//温度
    var whetherCheckAnt = MutableLiveData<Int>()//是否检测结果
    var dialogHint = MutableLiveData<Int>()//是否检测结果
    var buzzerEnable = MutableLiveData<Boolean>()
    private val TAG = ViewModelParameterSetting::class.simpleName

    // 获取启用附加数据
    fun getEmbeddedData() {
        UhfReaderSdk.getEmbeddedData()?.let { result ->
            if (result.err == ReaderErrorCode.MT_OK_ERR.value) {
                val res = result.data
                if (res != null) {
                    if (res.enable) {
                        val password = BaseUtils.bytesToHex(res.accesspwd)
                        additionalDataPassword.postValue(password)
                        additionalDataStartAddress.postValue(res.startaddr.toString())
                        additionalDataByteCount.postValue(res.bytecnt.toString())
                        additionalDataBank.postValue(res.bank.toString())
                        additionalDataEnable.postValue(true)
                    } else {
                        additionalDataEnable.postValue(false)
                    }
                }
            }
        }
    }

    // 设置附加数据
    fun setEmbeddedData(st: String, ct: String, bank: Int, pwd: String, enable: Boolean): Boolean {
        val er = UhfReaderSdk.setEmbeddedData(st.toInt(), ct.toInt(), bank, pwd, enable)
        return if (er == ReaderErrorCode.MT_OK_ERR.value) {
            Log.i(TAG, "setEmbeddedData: 设置成功")
            true
        } else {
            Log.e(TAG, "setEmbeddedData: 设置失败，返回码：$er")
            false
        }
    }

    // 设置不启用附加数据
    fun setEmbeddedDataNull(): Boolean {
        val er = UhfReaderSdk.setEmbeddedData(0, 0, 0, "", false)
        return if (er == ReaderErrorCode.MT_OK_ERR.value) {
            Log.i(TAG, "setEmbeddedDataNull: 设置成功")
            true
        } else {
            Log.e(TAG, "setEmbeddedDataNull: 设置失败，返回码：$er")
            false
        }
    }

    // 获取输出功率
    fun getPower(context: Context) {

        val sp = DataStoreUtils.getINSTANCE(context)


        UhfReaderSdk.getPower()?.let { result ->
            if (result.err == ReaderErrorCode.MT_OK_ERR.value) {
                result.data?.filterNotNull()?.forEach {
                    Log.i(
                        TAG, "getPower: antId:" + it.antid.toString()
                                + " readPower:" + it.readPower.toString()
                                + " writePower:" + it.writePower.toString()
                    )

                 //   power.postValue(it.readPower.toInt())

                    if(it.antid == 1){
                        if(sp.getPower(sp.ant1PowerKey, context) == 0){
                            ant1Power.postValue(it.readPower.toInt())
                        } else {
                            ant1Power.postValue(sp.getPower(sp.ant1PowerKey, context))
                        }
                        sp.setPower(sp.ant1PowerKey, ant1Power.value , context)

                    } else if(it.antid == 2){
                        if(sp.getPower(sp.ant2PowerKey, context) == 0){
                            ant2Power.postValue(it.readPower.toInt())
                        } else {
                            ant2Power.postValue(sp.getPower(sp.ant2PowerKey, context))
                        }
                        sp.setPower(sp.ant2PowerKey, ant2Power.value, context )
                    } else if(it.antid == 3){
                        if(sp.getPower(sp.ant3PowerKey, context) == 0){
                            ant3Power.postValue(it.readPower.toInt())
                        } else {
                            ant3Power.postValue(sp.getPower(sp.ant3PowerKey, context))
                        }
                        sp.setPower(sp.ant3PowerKey, ant3Power.value, context )
                    } else if(it.antid == 4){
                        if(sp.getPower(sp.ant4PowerKey, context) == 0){
                            ant4Power.postValue(it.readPower.toInt())
                        } else {
                            ant4Power.postValue(sp.getPower(sp.ant4PowerKey, context))
                        }
                        sp.setPower(sp.ant4PowerKey, ant4Power.value, context )
                    } else if(it.antid == 5){
                        if(sp.getPower(sp.ant5PowerKey, context) == 0){
                            ant5Power.postValue(it.readPower.toInt())
                        } else {
                            ant5Power.postValue(sp.getPower(sp.ant5PowerKey, context))
                        }
                        sp.setPower(sp.ant5PowerKey, ant5Power.value, context )
                    } else if(it.antid == 6){
                        if(sp.getPower(sp.ant6PowerKey, context) == 0){
                            ant6Power.postValue(it.readPower.toInt())
                        } else {
                            ant6Power.postValue(sp.getPower(sp.ant6PowerKey, context))
                        }
                        sp.setPower(sp.ant6PowerKey, ant6Power.value, context )
                    } else if(it.antid == 7){
                        if(sp.getPower(sp.ant7PowerKey, context) == 0){
                            ant7Power.postValue(it.readPower.toInt())
                        } else {
                            ant7Power.postValue(sp.getPower(sp.ant7PowerKey, context))
                        }
                        sp.setPower(sp.ant7PowerKey, ant7Power.value, context )
                    } else if(it.antid == 8){
                        if(sp.getPower(sp.ant8PowerKey, context) == 0){
                            ant8Power.postValue(it.readPower.toInt())
                        } else {
                            ant8Power.postValue(sp.getPower(sp.ant8PowerKey, context))
                        }
                        sp.setPower(sp.ant8PowerKey, ant8Power.value, context )
                    }

                    /*if(it.readPower.toInt() < 20){
                        power.postValue(it.readPower.toInt())
                    } else {
                        power.postValue(20)
                    }*/

                }
            } else {
                Log.e(TAG, "getPower: 获取失败")
            }
        }
    }

    // 获取频段
    fun getRegion() {
        UhfReaderSdk.getRegion()?.let { result ->
            if (result.err == ReaderErrorCode.MT_OK_ERR.value) {
                when (result.data) {
                    "FCC" -> {
                        // 北美
                        region.postValue(0)
                    }
                    "China1" -> {
                        // 中国
                        region.postValue(1)
                    }
                    "ETSI" -> {
                        // 欧洲
                        region.postValue(2)
                    }
                    else -> region.postValue(1)
                }
            } else {
                Log.e(TAG, "getRegion: 获取失败")
            }
        }
    }

    // 设置频段
    fun setRegion(region: String): Boolean {
        val er = UhfReaderSdk.setRegion(region)
        return if (er == ReaderErrorCode.MT_OK_ERR.value) {
            Log.i(TAG, "setRegion: 设置成功$region")
            true
        } else {
            Log.e(TAG, "setRegion: 设置失败，返回码：$er")
            false
        }
    }

    // 获取session
    fun getSession() {
        UhfReaderSdk.getSession()?.let { result ->
            if (result.err == ReaderErrorCode.MT_OK_ERR.value) {
                session.postValue(result.data)
                Log.i(TAG, "getSession: " + result.data)
            } else {
                Log.e(TAG, "getSession: 获取失败")
            }
        }
    }

    // 设置session
    fun setSession(session: Int): Boolean {
        val er = UhfReaderSdk.setSession(session)
        return if (er == ReaderErrorCode.MT_OK_ERR.value) {
            Log.i(TAG, "setSession: 设置成功$session")
            true
        } else {
            Log.e(TAG, "setSession: 设置失败，返回码：$er")
            false
        }
    }

    // 获取profile
    fun getProfile() {
        UhfReaderSdk.getProfile()?.let { result ->
            if (result.err == ReaderErrorCode.MT_OK_ERR.value) {
                result.data?.let {
                    if (it in 1..4) {
                        // 实际返回1，2，3，4对应下标0到3
                        profile.postValue(it - 1)
                        Log.i(TAG, "getProfile: profile$it")
                    } else {
                        profile.postValue(0)
                        Log.i(TAG, "getProfile: profile1")
                    }
                }
            } else {
                Log.e(TAG, "getProfile: 获取失败")
            }
        }
    }

    fun setProfile(profile: Int): Boolean {
        // 传0，1，2，3对应profile1到4
        val er = UhfReaderSdk.setProfile(profile + 1)
        return if (er == ReaderErrorCode.MT_OK_ERR.value) {
            Log.i(TAG, "setProfile: 设置成功$profile")
            true
        } else {
            Log.e(TAG, "setProfile: 设置失败，返回码：$er")
            false
        }
    }

    // 获取target
    fun getTarget() {
        UhfReaderSdk.getTarget()?.let { result ->
            if (result.err == ReaderErrorCode.MT_OK_ERR.value) {
                target.postValue(result.data)
                Log.i(TAG, "getTarget: ${result.data}")
            } else {
                Log.e(TAG, "getTarget: 获取失败")
            }
        }
    }

    // 设置target
    fun setTarget(target: Int): Boolean {
        val er = UhfReaderSdk.setTarget(target)
        return if (er == ReaderErrorCode.MT_OK_ERR.value) {
            Log.i(TAG, "setTarget: 设置成功$target")
            true
        } else {
            Log.e(TAG, "setTarget: 设置失败，返回码：$er")
            false
        }
    }

    // 获取qValue
    fun getQValue() {
        UhfReaderSdk.getQValue()?.let { result ->
            if (result.err == ReaderErrorCode.MT_OK_ERR.value) {
                qValue.postValue(result.data)
            } else {
                Log.e(TAG, "getQValue: 获取失败")
            }
        }
    }

    // 设置q
    fun setQValue(qValue: Int): Boolean {
        val er = UhfReaderSdk.setQValue(qValue)
        return if (er == ReaderErrorCode.MT_OK_ERR.value) {
            Log.i(TAG, "setQValue: 设置成功$qValue")
            true
        } else {
            Log.e(TAG, "setQValue: 设置失败，返回码：$er")
            false
        }
    }

    // 获取版本号
    fun getVersion() {
        UhfReaderSdk.getVersion()?.let { result ->
            if (result.err == ReaderErrorCode.MT_OK_ERR.value) {
                hardversion.postValue(result.data?.hardver)
                firmverion.postValue(result.data?.firmver)
            } else {
                Log.e(TAG, "getVersion: 获取失败")
            }
        }
    }

    // 获取gpi
    fun getGpi() {
        val gpi = Array(4) { IntArray(1) }
        val list: ArrayList<Int> = arrayListOf(0, 0, 0, 0)
        for ((i, _) in gpi.withIndex()) {
            UhfReaderSdk.getGpi(i + 1)?.let { result ->
                if (result.err == ReaderErrorCode.MT_OK_ERR.value && result.data != null) {
                    val gpiValue = result.data.toString().toInt()
                    Log.i(TAG, "getGpio" + (i + 1) + ": $gpiValue")
                    list[i] = gpiValue
                } else {
                    Log.e(TAG, "getGpio: 获取失败")
                }
            }
        }
        readerGpi.postValue(list)
    }

    // 设置gpo
    fun setGpo(gpoId: Int, gpo: Int): Boolean {
        val er = UhfReaderSdk.setGpo(gpoId, gpo)
        return if (er == ReaderErrorCode.MT_OK_ERR.value) {
            Log.i(TAG, "setGpo: 设置gpoId:${gpoId} gpo:${gpo}成功")
            true
        } else {
            Log.e(TAG, "setGpo: 设置gpoId:${gpoId} gpo:${gpo}失败")
            false
        }
    }

    fun getBZEnable() {
        UhfReaderSdk.getBZEnable()?.let { result ->
            buzzerEnable.postValue(result)
        }
    }

    // 获取指定盘点天线配置
    fun getInvantAnt() {
        UhfReaderSdk.getInvantAnt()?.let { result ->
            if (result.err == ReaderErrorCode.MT_OK_ERR.value) {
                result.data?.let {
                    try {
                        val ltp = arrayListOf<Int>()
                        for (element in 0 until it.antcnt) {
                            when (it.connectedants[element]) {
                                1 -> ltp.add(1)
                                2 -> ltp.add(2)
                                3 -> ltp.add(3)
                                4 -> ltp.add(4)
                                5 -> ltp.add(5)
                                6 -> ltp.add(6)
                                7 -> ltp.add(7)
                                8 -> ltp.add(8)
                            }
                        }
                        val ants = ltp.toTypedArray()
                        currentAntennaArray = IntArray(ants.size)
                        for ((index, _) in ants.withIndex()) {
                            currentAntennaArray[index] = ants[index]
                        }
                        connAnt.postValue(1)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                Log.e(TAG, "getInvantAnt: 获取失败")
            }
        }
    }

    // 获取温度
    fun getTemperature(hint: Boolean) {
        UhfReaderSdk.getTemperature()?.let { result ->
            if (result.err == ReaderErrorCode.MT_OK_ERR.value) {

                tempture.postValue(result.data)
                if (hint) dialogHint.postValue(0)
                Log.i(TAG, "getTemperature: ${result.data}")
            } else {
                dialogHint.postValue(1)
                Log.e(TAG, "getTemperature: 获取失败")
            }
        }
    }

    fun setPower(ant1Power: Int, ant2Power: Int, ant3Power: Int, ant4Power: Int, ant5Power: Int, ant6Power: Int, ant7Power: Int, ant8Power: Int): Boolean {
        /*val cPower = power
        val rp = IntArray(8)
        for ((index, _) in rp.withIndex()) {
            rp[index] = cPower
        }
        val array = mutableListOf<AntPowerData>()
        for (i in 0..7) {
            val antPowerData = AntPowerData().apply {
                antid = i + 1
                readPower = ((rp[i]).toShort())
                writePower = ((rp[i]).toShort())
            }
            array.add(antPowerData)
        }*/

        val array = mutableListOf<AntPowerData>()

        if(ant1Power != 0){
            val ant1PowerData = AntPowerData().apply {
                antid =  1
                readPower = (ant1Power.toShort())
                writePower = (ant1Power.toShort())
            }
            array.add(ant1PowerData)
        }

        if(ant2Power != 0){
            val ant2PowerData = AntPowerData().apply {
                antid =  2
                readPower = (ant2Power.toShort())
                writePower = (ant2Power.toShort())
            }
            array.add(ant2PowerData)
        }

        if(ant3Power != 0){
            val ant3PowerData = AntPowerData().apply {
                antid =  3
                readPower = (ant3Power.toShort())
                writePower = (ant3Power.toShort())
            }
            array.add(ant3PowerData)
        }

        if(ant4Power != 0){
            val ant4PowerData = AntPowerData().apply {
                antid =  4
                readPower = (ant4Power.toShort())
                writePower = (ant4Power.toShort())
            }
            array.add(ant4PowerData)
        }


        if(ant5Power != 0){
            val ant5PowerData = AntPowerData().apply {
                antid =  5
                readPower = (ant5Power.toShort())
                writePower = (ant5Power.toShort())
            }
            array.add(ant5PowerData)
        }

        if(ant6Power != 0){
            val ant6PowerData = AntPowerData().apply {
                antid =  6
                readPower = (ant6Power.toShort())
                writePower = (ant6Power.toShort())
            }
            array.add(ant6PowerData)
        }

        if(ant7Power != 0){
            val ant7PowerData = AntPowerData().apply {
                antid =  7
                readPower = (ant7Power.toShort())
                writePower = (ant7Power.toShort())
            }
            array.add(ant7PowerData)
        }

        if(ant8Power != 0){
            val ant8PowerData = AntPowerData().apply {
                antid =  8
                readPower = (ant8Power.toShort())
                writePower = (ant8Power.toShort())
            }
            array.add(ant8PowerData)
        }




        val er = UhfReaderSdk.setPower(array.toTypedArray())
        return if (er == ReaderErrorCode.MT_OK_ERR.value) {
          //  Log.i(TAG, "setPower: 设置成功$power")
            true
        } else {
            Log.e(TAG, "setPower: 设置失败，返回码：$er")
            false
        }
    }
}