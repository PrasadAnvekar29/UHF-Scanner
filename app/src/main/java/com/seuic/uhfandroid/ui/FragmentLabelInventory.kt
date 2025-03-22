package com.seuic.uhfandroid.ui


import android.app.AlertDialog
import android.os.Handler
import androidx.recyclerview.widget.LinearLayoutManager
import com.seuic.androidreader.sdk.Constants
import com.seuic.uhfandroid.R
import com.seuic.uhfandroid.adapter.TagInfoAdapter
import com.seuic.uhfandroid.base.BaseFragment
import com.seuic.uhfandroid.databinding.FragmentLabelInventoryBinding
import com.seuic.uhfandroid.ext.*
import com.seuic.uhfandroid.viewmodel.ViewModelLabelInventory


class FragmentLabelInventory :
    BaseFragment<ViewModelLabelInventory, FragmentLabelInventoryBinding>() {
    private val TAG = FragmentLabelInventory::class.simpleName
    val handler2 = Handler()
    private val adapter = TagInfoAdapter(R.layout.layout_tag)
    private val runnable = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis() - vm.statenvtick
            v.tvInventoryTime.text = currentTime.toString()
            handler2.postDelayed(this, 10)
        }
    }

    override fun initView() {
    }

    override fun initClick() {
        // 单次寻卡
        v.btnSingleCard.setOnClickListener {
            vm.stopSearchForCard()
            v.btnSearchForCard.isEnabled = true
            handler2.removeCallbacks(runnable)
            v.tvInventoryTime.text = "0"
            v.tvTagNumber.text = "0"
            v.tvRecognizeTimes.text = "0"
            isSearching = false
            resetCurrentTag.postValue(true)
            itemClickable.postValue(true)
            vm.listTagData.clear()
            adapter.data.clear()
            adapter.notifyDataSetChanged()

            vm.singleCard()
        }

        // 连续寻卡
        v.btnSearchForCard.setOnClickListener {
            isSearching = true
            v.tvTagNumber.text = "0"
            v.tvRecognizeTimes.text = "0"
            vm.listTagData.clear()
            adapter.data.clear()
            adapter.notifyDataSetChanged()
            resetCurrentTag.postValue(true)
            itemClickable.postValue(false)

            vm.searchForCard()

            v.btnSearchForCard.isEnabled = false
            handler2.postDelayed(runnable, 0)
        }

        // 停止连续寻卡
        v.btnStopSearchForCard.setOnClickListener {
            vm.stopSearchForCard()
            v.btnSearchForCard.isEnabled = true
            handler2.removeCallbacks(runnable)
            isSearching = false
            resetCurrentTag.postValue(true)
            itemClickable.postValue(true)
        }

        // 清空
        v.btnClear.setOnClickListener {
            // 总次数置0
            totalCounts.set(0)
            v.tvTagNumber.text = "0"
            v.tvRecognizeTimes.text = "0"
            v.tvInventoryTime.text = "0"
            // 清空标签盘存和标签读写两个界面的标签列表
            clearTagList.postValue(true)
            resetCurrentTag.postValue(true)
        }
    }

    override fun initData() {
        v.rlvEpc.adapter = adapter
        v.rlvEpc.layoutManager = LinearLayoutManager(activity)
        Constants.connectState.observe(requireActivity()) {
            if (it.state == Constants.CONNECTED) {
                vm.registerListener()
            }
        }
    }

    override fun initVM() {
        activity?.runOnUiThread {
            itemClickable.observe(this) {
                v.btnSingleCard.isEnabled = it
                v.btnSingleCard.isClickable = it
            }

            // 单次寻卡的livedata监听
            vm.tagData.observe(this) {
                // 清空list，加入单次寻卡结果
                vm.listTagData.clear()
                vm.listTagData.add(it)
                adapter.setList(vm.listTagData)
                adapter.notifyDataSetChanged()
                v.tvTagNumber.text = vm.listTagData.size.toString()
                v.tvRecognizeTimes.text = "1"
            }

            // 连续寻卡的livedata监听
            vm.tagListData.observe(this) {
                adapter.setList(it)
                adapter.notifyDataSetChanged()
                v.tvTagNumber.text = it.size.toString()
                v.tvRecognizeTimes.text = totalCounts.get().toString()
            }

            // 清除标签列表的livedata监听
            clearTagList.observe(this) {
                if (it) {
                    vm.listTagData.clear()
                    adapter.data.clear()
                    adapter.notifyDataSetChanged()
                }
            }

            // 重置当前选中标签和标签列表的下标
            resetCurrentTag.observe(this) {
                if (it) {
                    currentTag = ""
                    adapter.setPosition(-1)
                    adapter.notifyDataSetChanged()
                }
            }

            vm.errData.observe(this) {
                showDialogTip(getString(R.string.err_infomation) + ": errCode=$it")
            }
        }
    }

    private fun showDialogTip(msg: String) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(msg)
        builder.setPositiveButton(
            getString(R.string.sure)
        ) { _, _ ->
            builder.create().dismiss()
        };
        // 显示
        builder.show();
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.stopSearchForCard()
        handler2.removeCallbacks(runnable)
        vm.unregisterListener()
    }
}