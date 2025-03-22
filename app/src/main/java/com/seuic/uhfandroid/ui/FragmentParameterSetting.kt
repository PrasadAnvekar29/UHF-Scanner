package com.seuic.uhfandroid.ui

import android.app.AlertDialog
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.seuic.androidreader.sdk.Constants
import com.seuic.androidreader.sdk.UhfReaderSdk
import com.seuic.uhfandroid.R
import com.seuic.uhfandroid.base.BaseFragment
import com.seuic.uhfandroid.databinding.FragmentParameterSettingBinding
import com.seuic.uhfandroid.ext.currentAntennaArray
import com.seuic.uhfandroid.ext.isSearching
import com.seuic.uhfandroid.util.DataStoreUtils
import com.seuic.uhfandroid.viewmodel.ViewModelParameterSetting
import com.seuic.util.common.LanguageUtils
import java.util.*


class FragmentParameterSetting :
    BaseFragment<ViewModelParameterSetting, FragmentParameterSettingBinding>() {
    private val TAG = FragmentParameterSetting::class.simpleName

    /**
     * 获取上一次保存的参数，更新界面
     * gpo和是否启用附加数据是保存在本地的，aidl服务没有提供接口查询
     */
    private fun getSettings() {
        val sp = DataStoreUtils.getINSTANCE(requireContext())
        // 附加数据
        vm.getEmbeddedData()
        // 功率
        vm.getPower()
        // 工作频段
        vm.getRegion()
        // session
        vm.getSession()
        // profile
        vm.getProfile()
        // target
        vm.getTarget()
        // q
        vm.getQValue()
        // gpi
        vm.getGpi()
        // gpo
        //v.rbOut1.isChecked = sp.gpo1
        //v.rbOut2.isChecked = sp.gpo2
        //v.rbOut3.isChecked = sp.gpo3
        //v.rbOut4.isChecked = sp.gpo4
        // 设置蜂鸣器
        vm.getBZEnable()
        // 获取连接中的天线
        vm.getInvantAnt()
    }

    override fun initView() {
        if (LanguageUtils.getAppliedLanguage() == Locale.ENGLISH) {
            val temp = resources.getStringArray(R.array.sp_buzzer_English)
            setDefaultSpData(
                v.spBuzzer,
                temp.toList() as ArrayList<String>
            )
        }
        if (LanguageUtils.getAppliedLanguage() == Locale.ENGLISH) {
            val temp = resources.getStringArray(R.array.sp_out_dir_English)
            setDefaultSpData(
                v.spOutDir,
                temp.toList() as ArrayList<String>
            )
        }
        // 初始化功率、工作频段、q值的三个下拉列表
        val list = ArrayList<String>()
        for (index in 1..33) {
            list.add(index.toString())
        }
        setDefaultSpData(v.spOutputPower, list)
        var workFrequency = resources.getStringArray(R.array.sp_work_frequency_band)
        if (LanguageUtils.getAppliedLanguage() == Locale.ENGLISH) {
            workFrequency = resources.getStringArray(R.array.sp_work_frequency_band_English)
        }
        setDefaultSpData(
            v.spWorkFrequencyBand,
            workFrequency.toList() as ArrayList<String>
        )
        v.spWorkFrequencyBand.setSelection(0)
        val qList = ArrayList<String>()
        qList.add(getString(R.string.auto))
        for (index in 0..15) {
            qList.add(index.toString())
        }
        setDefaultSpData(v.spQValue, qList)
        // 启用附加数据未勾选时，不可选中编辑框
        v.cbEnableadd.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                v.etStartAddress.isEnabled = true
                v.etLength.isEnabled = true
                v.etVisitPassword.isEnabled = true
                v.spStorageArea.isEnabled = true
            } else {
                v.etStartAddress.isEnabled = false
                v.etLength.isEnabled = false
                v.etVisitPassword.isEnabled = false
                v.spStorageArea.isEnabled = false
            }
        }

        // 显示上次保存的设置
        Constants.connectState.observe(requireActivity()) {
            if (it.state == 1) {
                getSettings()
            }
        }
    }

    override fun initClick() {
//        val sp = SharedPreferencesUtils(requireContext())
        val sp = DataStoreUtils.getINSTANCE(requireContext())

        // 获取参数
        v.btnParamerGet.setOnClickListener {
            if (isSearching) {
                showDialogTip(getString(R.string.please_stop_searching))
            } else {
                // 显示上次保存的设置
                getSettings()
                // 并且获取版本信息和温度
                vm.getVersion()
                vm.getTemperature(false)
            }
        }

        // 设置参数
        v.btnParamerSet.setOnClickListener {
            if (isSearching) {
                showDialogTip(getString(R.string.please_stop_searching))
            } else {
                // 设置天线
                val ltp = arrayListOf<Int>()
                if (v.rbAnt1.isChecked) ltp.add(1)
                if (v.rbAnt2.isChecked) ltp.add(2)
                if (v.rbAnt3.isChecked) ltp.add(3)
                if (v.rbAnt4.isChecked) ltp.add(4)
                if (v.rbAnt5.isChecked) ltp.add(5)
                if (v.rbAnt6.isChecked) ltp.add(6)
                if (v.rbAnt7.isChecked) ltp.add(7)
                if (v.rbAnt8.isChecked) ltp.add(8)
                // 设置附加数据
                val st = v.etStartAddress.text.toString()
                val ct = v.etLength.text.toString()
                val pwd = v.etVisitPassword.text.toString()
                val bank = v.spStorageArea.selectedItemPosition
                if (ltp.size < 1) {
                    // 天线配置不为空才能设置参数
                    showDialogTip(getString(R.string.please_select_antenna))
                } else if (v.cbEnableadd.isChecked && "" == v.etStartAddress.text.toString()) {
                    // 勾选启用附加数据时，起始地址不为空才能设置参数
                    showDialogTip(getString(R.string.please_fill_additional_data_start_address))
                } else if (v.cbEnableadd.isChecked && "" == v.etVisitPassword.text.toString()) {
                    // 勾选启用附加数据时，访问密码不为空才能设置参数
                    showDialogTip(getString(R.string.please_fill_additional_data_password))
                } else if (v.cbEnableadd.isChecked && "" == v.etLength.text.toString()) {
                    // 勾选启用附加数据时，附加数据长度不为空才能设置参数
                    showDialogTip(getString(R.string.please_fill_additional_data_length))
                } else {
                    val res = StringBuilder()
                    // 设置天线
                    val ants = ltp.toTypedArray()
                    currentAntennaArray = IntArray(ants.size)
                    for ((index, _) in ants.withIndex()) {
                        currentAntennaArray[index] = ants[index]
                    }
                    // 设置附加数据
                    // tid2 user3
                    if (!vm.setEmbeddedData(st, ct, v.spStorageArea.selectedItemPosition + 2, pwd, v.cbEnableadd.isChecked)) res.append("附加数据 ")
                    // 设置功率
                    val power = v.spOutputPower.selectedItemPosition + 1
                    if (!vm.setPower(power)) {
                        res.append("功率 ")
                    } else {
                        sp.power = v.spOutputPower.selectedItemPosition + 1
                    }
                    // 工作频段
                    val tempValue: String = when (v.spWorkFrequencyBand.selectedItemPosition) {
                        0 -> "FCC"
                        1 -> "China1"
                        //2 -> "ETSI"
                        else -> "FCC"
                    }
                    if (!vm.setRegion(tempValue))
                        res.append("工作频段 ")
                    else
                        sp.region = v.spWorkFrequencyBand.selectedItemPosition
                    // session,profile,target,q
                    if (!vm.setSession(v.spSession.selectedItemPosition)) {
                        res.append("session ")
                    } else {
                        sp.session = v.spSession.selectedItemPosition
                    }
                    if (!vm.setProfile(v.spProfile.selectedItemPosition)) {
                        res.append("profile ")
                    } else {
                        sp.profile = v.spProfile.selectedItemPosition
                    }
                    if (!vm.setTarget(v.spTarget.selectedItemPosition)) {
                        res.append("target ")
                    } else {
                        sp.target = v.spTarget.selectedItemPosition
                    }
                    if (!vm.setQValue(v.spQValue.selectedItemPosition - 1)) {
                        res.append("q ")
                    } else {
                        sp.q = v.spQValue.selectedItemPosition
                    }
                    // gpo
                    /*if (v.rbOut1.isChecked) {
                        if (!vm.setGpo(1, 1)) {
                            res.append("gpo1 ")
                        } else {
                            sp.gpo1 = v.rbOut1.isChecked
                        }
                    } else {
                        if (!vm.setGpo(1, 0)) {
                            res.append("gpo1 ")
                        } else {
                            sp.gpo1 = v.rbOut1.isChecked
                        }
                    }*/

                    if (!vm.setGpo(v.spOut.selectedItemPosition+1, v.spOutDir.selectedItemPosition)) {
                        res.append("gpo"+(v.spOut.selectedItemPosition+1)+" ")
                    } else {
                        if(v.spOut.selectedItemPosition+1 == 1)
                            sp.gpo1 = v.spOutDir.selectedItemPosition == 1
                        if(v.spOut.selectedItemPosition+1 == 2)
                            sp.gpo2 = v.spOutDir.selectedItemPosition == 1
                        if(v.spOut.selectedItemPosition+1 == 3)
                            sp.gpo3 = v.spOutDir.selectedItemPosition == 1
                        if(v.spOut.selectedItemPosition+1 == 4)
                            sp.gpo4 = v.spOutDir.selectedItemPosition == 1
                    }

                    // 设置蜂鸣器
                    when (v.spBuzzer.selectedItemPosition) {
                        0 -> UhfReaderSdk.setBZEnable(true)
                        1 -> UhfReaderSdk.setBZEnable(false)
                    }
                    sp.enableBuzzer = v.spBuzzer.selectedItemPosition == 0
                    if (res.isEmpty()) {
                        showDialogTip(getString(R.string.set_success))
                    } else {
                        res.append(getString(R.string.set_failed))
                        showDialogTip(res.toString())
                    }
                }
            }
        }
        // 恢复出厂
        v.btnFactorySet.setOnClickListener {
            if (isSearching) {
                showDialogTip(getString(R.string.please_stop_searching))
            } else {
                // 提示是否需要出厂
                val builder = AlertDialog.Builder(activity)
                builder.setTitle(R.string.confirm_factory_settings)
                builder.setPositiveButton(
                    getString(R.string.sure)
                ) { _, _ ->
                    // 设置功能
                    currentAntennaArray = intArrayOf()
                    vm.setPower(33)
                    vm.setRegion("FCC")
                    vm.setSession(0)
                    vm.setProfile(0)
                    vm.setTarget(0)
                    vm.setQValue(-1)
                    vm.setEmbeddedData("0", "12", 1, "00000000", false)
                    vm.setGpo(1, 1)
                    vm.setGpo(2, 0)
                    vm.setGpo(3, 0)
                    vm.setGpo(4, 0)
                    UhfReaderSdk.setFilter(0, 0, "", isInvert = false, enable = false)

                    // 更新界面
                    v.spOutputPower.setSelection(32)
                    v.spWorkFrequencyBand.setSelection(0)
                    v.cbCheckant.isChecked = true
                    v.spSession.setSelection(0)
                    v.spProfile.setSelection(0)
                    v.spTarget.setSelection(0)
                    v.spQValue.setSelection(0)
                    //v.rbOut1.isChecked = true
                    //v.rbOut2.isChecked = false
                    //v.rbOut3.isChecked = false
                    //v.rbOut4.isChecked = false
                    v.cbEnableadd.isChecked = false
                    v.etStartAddress.setText(getString(R.string.default_etStartAddress))
                    v.etLength.setText(getString(R.string.default_etLength))
                    v.etVisitPassword.setText(getString(R.string.default_etVisitPassword))
                    v.tvTemperature.text = ""
                    v.tvFirmwareVersion.text = ""
                    v.tvHardwareVersion.text = ""
                    v.spBuzzer.setSelection(0)
                    // 更新sharedPreferences
                    sp.setPower(33)
                        .setRegion(0)
                        .setSession(0)
                        .setProfile(0)
                        .setTarget(0)
                        .setQ(5)
                        .setGpo1(true)
                        .setGpo2(false)
                        .setGpo3(false)
                        .setGpo4(false)
                        .enableBuzzer = true

                    vm.getPower()
                    vm.getRegion()
                    vm.getSession()
                    vm.getProfile()
                    vm.getTarget()
                    vm.getQValue()
                    vm.getGpi()
                    vm.getInvantAnt()
                    UhfReaderSdk.setBZEnable(true)
                    builder.create().dismiss()
                }
                builder.setNegativeButton(
                    getString(R.string.cancel)
                ) { _, _ -> builder.create().dismiss() }
                // 显示
                builder.show()
            }
        }
    }

    override fun initData() {
    }

    override fun initVM() {
/*        connectResult.observe(this, {
        })*/
        vm.power.observe(this) {
            v.spOutputPower.setSelection(it - 1)
        }
        vm.region.observe(this) {
            v.spWorkFrequencyBand.setSelection(it)
        }
        vm.session.observe(this) {
            v.spSession.setSelection(it)
        }
        vm.profile.observe(this) {
            v.spProfile.setSelection(it)
        }
        vm.target.observe(this) {
            v.spTarget.setSelection(it)
        }
        vm.qValue.observe(this) {
            v.spQValue.setSelection(it + 1)
        }
        vm.hardversion.observe(this) {
            v.tvHardwareVersion.text = it
        }
        vm.firmverion.observe(this) {
            v.tvFirmwareVersion.text = it
        }
        vm.readerGpi.observe(this) {
            v.rbIn1.isEnabled = false
            v.rbIn2.isEnabled = false
            v.rbIn3.isEnabled = false
            v.rbIn4.isEnabled = false
            v.rbIn1.isChecked = it[0] == 1
            v.rbIn2.isChecked = it[1] == 1
            v.rbIn3.isChecked = it[2] == 1
            v.rbIn4.isChecked = it[3] == 1
        }
        vm.checkAnt.observe(this) {
            v.cbCheckant.isChecked = it
        }
        vm.buzzerEnable.observe(this) {
            if (it) {
                v.spBuzzer.setSelection(0)
            } else {
                v.spBuzzer.setSelection(1)
            }
        }
        vm.dialogHint.observe(this) {
            when (it) {
                0 -> showDialogTip(getString(R.string.get_setting_success))
                else -> showDialogTip(getString(R.string.get_setting_failed))
            }
        }
        vm.connAnt.observe(this) {
            v.rbAnt1.isChecked = false
            v.rbAnt2.isChecked = false
            v.rbAnt3.isChecked = false
            v.rbAnt4.isChecked = false
            v.rbAnt5.isChecked = false
            v.rbAnt6.isChecked = false
            v.rbAnt7.isChecked = false
            v.rbAnt8.isChecked = false
            v.rbAnt1.isEnabled = true//false
            v.rbAnt2.isEnabled = true//false
            v.rbAnt3.isEnabled = true//false
            v.rbAnt4.isEnabled = true//false
            v.rbAnt5.isEnabled = true//false
            v.rbAnt6.isEnabled = true//false
            v.rbAnt7.isEnabled = true//false
            v.rbAnt8.isEnabled = true//false
            for (bean in currentAntennaArray) {
                when (bean) {
                    1 -> {
                        v.rbAnt1.isChecked = true
                        v.rbAnt1.isEnabled = true
                    }
                    2 -> {
                        v.rbAnt2.isChecked = true
                        v.rbAnt2.isEnabled = true
                    }
                    3 -> {
                        v.rbAnt3.isChecked = true
                        v.rbAnt3.isEnabled = true
                    }
                    4 -> {
                        v.rbAnt4.isChecked = true
                        v.rbAnt4.isEnabled = true
                    }
                    5 -> {
                        v.rbAnt5.isChecked = true
                        v.rbAnt5.isEnabled = true
                    }
                    6 -> {
                        v.rbAnt6.isChecked = true
                        v.rbAnt6.isEnabled = true
                    }
                    7 -> {
                        v.rbAnt7.isChecked = true
                        v.rbAnt7.isEnabled = true
                    }
                    8 -> {
                        v.rbAnt8.isChecked = true
                        v.rbAnt8.isEnabled = true
                    }
                }
            }
        }
        vm.additionalDataByteCount.observe(this) {
            v.cbEnableadd.isChecked = it.isNotEmpty()
            v.etLength.setText(it)
        }
        vm.additionalDataStartAddress.observe(this) {
            v.etStartAddress.setText(it)
        }
        vm.additionalDataPassword.observe(this) {
            v.etVisitPassword.setText(it)
        }
        vm.additionalDataBank.observe(this) {
            if (it.isNotEmpty()) {
                // 2tid 3user
                v.spStorageArea.setSelection(it.toInt() - 2)
            }
        }
        vm.additionalDataEnable.observe(this) {
            v.cbEnableadd.isChecked = it
            v.etStartAddress.isEnabled = it
            v.etLength.isEnabled = it
            v.etVisitPassword.isEnabled = it
            v.spStorageArea.isEnabled = it
        }
        vm.tempture.observe(this) {
            v.tvTemperature.text = it.toString()
        }
        vm.whetherCheckAnt.observe(this) {
            // 0 代表设置天线检测的这一个操作成功而不是是否天线检测，也不代表所有的设置操作成功
        }
    }

    private fun setDefaultSpData(spinner: Spinner, data: ArrayList<String>) {
        val dropDownAdapter = activity?.let {
            ArrayAdapter(
                it,
                R.layout.my_simple_spinner_dropdown_item, data
            )

        }
        spinner.adapter = dropDownAdapter
    }

    private fun showDialogTip(msg: String) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(msg)
        builder.setPositiveButton(
            getString(R.string.sure)
        ) { _, _ ->
            builder.create().dismiss()
        }
        // 显示
        builder.show()
    }

    override fun initStartSendingAPI() {
    }
}