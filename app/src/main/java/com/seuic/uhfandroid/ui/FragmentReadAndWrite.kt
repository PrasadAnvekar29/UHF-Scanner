package com.seuic.uhfandroid.ui

import android.app.AlertDialog
import android.text.TextUtils
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.seuic.androidreader.sdk.ReaderErrorCode
import com.seuic.androidreader.sdk.UhfReaderSdk
import com.seuic.uhfandroid.R
import com.seuic.uhfandroid.adapter.TagInfoAdapter
import com.seuic.uhfandroid.base.BaseFragment
import com.seuic.uhfandroid.bean.TagBean
import com.seuic.uhfandroid.databinding.FragmentReadAndWriteBinding
import com.seuic.uhfandroid.ext.*
import com.seuic.uhfandroid.util.DataStoreUtils
import com.seuic.uhfandroid.viewmodel.ViewModelReadAndWrite
import com.seuic.util.common.LanguageUtils
import java.util.*

class FragmentReadAndWrite :
    BaseFragment<ViewModelReadAndWrite, FragmentReadAndWriteBinding>() {
    private val mAdapter = TagInfoAdapter(R.layout.layout_tag)
    private val TAG = FragmentReadAndWrite::class.simpleName

    override fun initView() {
        if (LanguageUtils.getAppliedLanguage() == Locale.ENGLISH) {
            var temp = resources.getStringArray(R.array.sp_suit_English)
            setDefaultSpData(v.spSuit, temp.toList() as ArrayList<String>)
            temp = resources.getStringArray(R.array.sp_enable_English)
            setDefaultSpData(v.spEnable, temp.toList() as ArrayList<String>)
            temp = resources.getStringArray(R.array.sp_lock_type_English)
            setDefaultSpData(v.spLockType, temp.toList() as ArrayList<String>)
        }
    }

    override fun initClick() {
        // 标签列表点击事件
        mAdapter.setOnItemClickListener { adapter, _, position ->
            mAdapter.setPosition(position)
            mAdapter.notifyDataSetChanged()
            val tag = adapter.data[position] as TagBean
            currentTag = tag.epcId
            vm.currentAntenna = tag.antenna.toInt()
        }

        // 过滤
        v.btnFilter.setOnClickListener {
            when {
                // 起始地址必填
                TextUtils.isEmpty(v.etStartAddress.text.toString()) -> showDialogTip(getString(R.string.start_address_is_empty))
                v.rbPassword.isChecked -> showDialogTip("不支持对保留区进行过滤")
                else -> {
                    val suit = v.spSuit.selectedItemPosition == 0
                    var bank = 1
                    when {
                        v.rbEpc.isChecked -> bank = 1
                        v.rbTid.isChecked -> bank = 2
                        v.rbUser.isChecked -> bank = 3
                    }
                    if (v.spEnable.selectedItemPosition == 0) {
                        if ("" == v.etData.text.toString()) {
                            showDialogTip(getString(R.string.data_content_is_empty))
                        } else {
                            vm.setFilter(v.etStartAddress.text.toString(), bank, v.etData.text.toString(), suit, true)
                            // 保存到sp中
                            val sp = DataStoreUtils.getINSTANCE(requireContext())
                            sp.setFilterStartAddress(v.etStartAddress.text.toString())
                                .setFilterBank(1)
                                .setFilterData(currentTag)
                                .filterSuit = true
                        }
                    } else {
                        val er = UhfReaderSdk.setFilter(0, 0, "", isInvert = false, enable = false)
                        vm.tagResultFilter.postValue(er == ReaderErrorCode.MT_OK_ERR.value)
                    }
                }
            }
        }

        // 读卡
        v.btnReadCard.setOnClickListener {
            when {
                TextUtils.isEmpty(v.etReadWriteStartAddress.text.toString()) -> showDialogTip(getString(R.string.start_address_is_empty))
                TextUtils.isEmpty(v.etReadWriteLength.text.toString()) -> showDialogTip(getString(R.string.length_is_empty))
                TextUtils.isEmpty(v.etVisitPassword.text.toString()) -> showDialogTip(getString(R.string.password_is_empty))
                TextUtils.isEmpty(currentTag) -> showDialogTip(getString(R.string.please_select_the_label))
                else -> {
                    var bank = 0
                    when {
                        /*
                            bank0又称为保留区，存放着访问密码和销毁密码，每个密码都有32bit。
                            bank1又称为EPC区，其中又含有CRC字段（16bit），PC字段（16bit），EPC码（最长可到达496bit，一般为96bit），
                            bank2又称为TID区，含有全球唯一的序列号，共64bit。
                            bank3又称为USER区，容量是变长，不同的标签品牌容量不同，也有很多标签没有bank3。
                        */
                        v.rbPassword.isChecked -> bank = 0
                        v.rbEpc.isChecked -> bank = 1
                        v.rbTid.isChecked -> bank = 2
                        v.rbUser.isChecked -> bank = 3
                    }
                    vm.readTagData(
                        vm.currentAntenna,
                        bank,
                        v.etReadWriteLength.text.toString().toInt(),
                        v.etReadWriteStartAddress.text.toString().toInt(),
                        v.etVisitPassword.text.toString(),
                        currentTag
                    )
                }
            }
        }

        // 写卡
        v.btnWriteCard.setOnClickListener {
            val address = v.etReadWriteStartAddress.text.toString()
            val length = v.etReadWriteLength.text.toString()
            val content = v.etDataContent.text.toString()
            when {
                TextUtils.isEmpty(address) -> showDialogTip(getString(R.string.start_address_is_empty))
                TextUtils.isEmpty(length) -> showDialogTip(getString(R.string.length_is_empty))
                TextUtils.isEmpty(content) -> showDialogTip(getString(R.string.data_content_is_empty))
                TextUtils.isEmpty(currentTag) -> showDialogTip(getString(R.string.please_select_the_label))
                content.length != length.toInt() * 4 -> showDialogTip(getString(R.string.data_length_is_inconsistent_with_the_setting))
                else -> {
                    var bank = 1
                    when {
                        v.rbPassword.isChecked -> bank = 0
                        v.rbEpc.isChecked -> bank = 1
                        v.rbTid.isChecked -> bank = 2
                        v.rbUser.isChecked -> bank = 3
                    }
                    vm.writeTagData(
                        vm.currentAntenna, bank,
                        v.etDataContent.text.toString(),
                        v.etReadWriteStartAddress.text.toString().toInt(),
                        v.etVisitPassword.text.toString(),
                        currentTag
                    )
                }
            }
        }

        // 清除
        v.btnRemove.setOnClickListener {
            v.etDataContent.setText("")
            v.etData.setText("")
        }

        // 重置当前选中标签和标签列表的下标
        resetCurrentTag.observe(this) {
            if (it) {
                currentTag = ""
                mAdapter.setPosition(-1)
                mAdapter.notifyDataSetChanged()
            }
        }

        // 锁卡
        v.btnLockCard.setOnClickListener {
            // 锁卡
            when {
                TextUtils.isEmpty(currentTag) -> showDialogTip(getString(R.string.please_select_the_label))
                TextUtils.isEmpty(v.etLockVisitPassword.text.toString()) -> showDialogTip(
                    getString(
                        R.string.password_is_empty
                    )
                )
                v.spLockType.selectedItemPosition == 2 -> {
                    val builder = AlertDialog.Builder(activity)
                    builder.setTitle(getString(R.string.lock_hint))
                    builder.setPositiveButton(
                        getString(R.string.sure)
                    ) { _, _ ->
                        builder.create().dismiss()
                        var bank = 1
                        when {
                            v.rbPassword.isChecked -> bank = 0
                            v.rbEpc.isChecked -> bank = 1
                            v.rbTid.isChecked -> bank = 2
                            v.rbUser.isChecked -> bank = 3
                        }
                        vm.lockTag(
                            vm.currentAntenna,
                            bank,
                            v.spLockType.selectedItemPosition,
                            v.etLockVisitPassword.text.toString(),
                            currentTag
                        )
                    }
                    builder.setNegativeButton(
                        getString(R.string.cancel)
                    ) { _, _ -> builder.create().dismiss() }
                    // 显示
                    builder.show();
                }
                else -> {
                    var bank = 1
                    when {
                        v.rbPassword.isChecked -> bank = 0
                        v.rbEpc.isChecked -> bank = 1
                        v.rbTid.isChecked -> bank = 2
                        v.rbUser.isChecked -> bank = 3
                    }
                    vm.lockTag(
                        vm.currentAntenna,
                        bank,
                        v.spLockType.selectedItemPosition,
                        v.etLockVisitPassword.text.toString(),
                        currentTag
                    )
                }
            }
        }

        // 销毁卡
        v.btnDestroyTag.setOnClickListener {
            when {
                TextUtils.isEmpty(currentTag) -> showDialogTip(getString(R.string.please_select_the_label))
                TextUtils.isEmpty(v.etDestoryPassword.text.toString()) -> showDialogTip(getString(R.string.destroy_password_is_empty))
                else -> {
                    val builder = AlertDialog.Builder(activity)
                    builder.setTitle(getString(R.string.destroy_hint))
                    builder.setPositiveButton(
                        getString(R.string.sure)
                    ) { _, _ ->
                        vm.destroyTag(vm.currentAntenna, v.etDestoryPassword.text.toString(), currentTag)
                        builder.create().dismiss()
                    }
                    builder.setNegativeButton(
                        getString(R.string.cancel)
                    ) { _, _ -> builder.create().dismiss() };
                    // 显示
                    builder.show();
                }
            }
        }
    }

    override fun initData() {
        v.rlvEpc.adapter = mAdapter
        v.rlvEpc.layoutManager = LinearLayoutManager(activity)
    }

    override fun initVM() {
        // 单次寻卡的livedata监听
        inventoryDatas.observe(this) {
            vm.listTagData.clear()
            vm.listTagData.add(it)
            mAdapter.setList(vm.listTagData)
            mAdapter.notifyDataSetChanged()
        }

        // 连续寻卡的livedata监听
        inventoryListDatas.observe(this) {
            mAdapter.setList(it)
            mAdapter.notifyDataSetChanged()
        }

        // 清空标签列表livedata监听
        clearTagList.observe(this) {
            if (it) {
                vm.listTagData.clear()
                mAdapter.setList(vm.listTagData)
                mAdapter.notifyDataSetChanged()
                // 当前没有选择标签
                currentTag = ""
                mAdapter.setPosition(-1)
            }
        }

        // 读卡返回的结果
        vm.tagDetailData.observe(this) {
            if (it.isNotEmpty()) {
                v.etDataContent.setText(it)
                Toast.makeText(requireContext(), getString(R.string.read_card_success), Toast.LENGTH_SHORT).show()
            } else {
                showDialogTip(getString(R.string.read_card_failed))
            }
        }

        vm.tagResultWrite.observe(this) {
            when (it) {
                ReaderErrorCode.MT_OK_ERR.value -> Toast.makeText(requireContext(), getString(R.string.write_card_success), Toast.LENGTH_SHORT).show()
                else -> showDialogTip(getString(R.string.write_card_failed))
            }
        }

        vm.tagResultFilter.observe(this) {
            if (it) {
                Toast.makeText(requireContext(), getString(R.string.set_success), Toast.LENGTH_SHORT).show()
            } else {
                showDialogTip(getString(R.string.set_failed))
            }
        }

        vm.tagResultLock.observe(this) {
            when (it) {
                -1 -> showDialogTip(getString(R.string.set_failed))
                0 -> Toast.makeText(requireContext(), getString(R.string.unlock_success), Toast.LENGTH_SHORT).show()
                1 -> Toast.makeText(requireContext(), getString(R.string.temp_lock_success), Toast.LENGTH_SHORT).show()
                2 -> Toast.makeText(requireContext(), getString(R.string.foreveer_lock_success), Toast.LENGTH_SHORT).show()
            }
        }

        vm.tagResultKill.observe(this) {
            when (it) {
                0 -> Toast.makeText(requireContext(), getString(R.string.destry_success), Toast.LENGTH_SHORT).show()
                1 -> Toast.makeText(requireContext(), getString(R.string.destry_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun initStartSendingAPI() {
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
        builder.show();
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
}