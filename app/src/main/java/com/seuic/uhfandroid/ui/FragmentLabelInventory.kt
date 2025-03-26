package com.seuic.uhfandroid.ui


import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.seuic.androidreader.sdk.Constants
import com.seuic.uhfandroid.R
import com.seuic.uhfandroid.adapter.TagInfoAdapter
import com.seuic.uhfandroid.base.BaseFragment
import com.seuic.uhfandroid.bean.TagBean
import com.seuic.uhfandroid.databinding.FragmentLabelInventoryBinding
import com.seuic.uhfandroid.ext.clearTagList
import com.seuic.uhfandroid.ext.currentTag
import com.seuic.uhfandroid.ext.isSearching
import com.seuic.uhfandroid.ext.itemClickable
import com.seuic.uhfandroid.ext.resetCurrentTag
import com.seuic.uhfandroid.ext.totalCounts
import com.seuic.uhfandroid.service.APIResponse
import com.seuic.uhfandroid.service.ApiClient
import com.seuic.uhfandroid.service.ApiInterface
import com.seuic.uhfandroid.util.DataStoreUtils
import com.seuic.uhfandroid.viewmodel.ViewModelLabelInventory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException


class FragmentLabelInventory :
    BaseFragment<ViewModelLabelInventory, FragmentLabelInventoryBinding>(),
    Callback<APIResponse>



{


    private var handler3 = Handler()
    private var runnable3: Runnable = object : Runnable {
        override fun run() {
            // Call your API here
            //    callNetworkAPI()

            // Schedule the runnable to run again after 10 seconds
            handler3.postDelayed(this, 10000)
        }
    }



    val handler2 = Handler()
    private val adapter = TagInfoAdapter(R.layout.layout_tag)
    private val runnable = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis() - vm.statenvtick

            val hms = java.lang.String.format(
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(currentTime),
                TimeUnit.MILLISECONDS.toMinutes(currentTime) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(
                        currentTime
                    )
                ),
                TimeUnit.MILLISECONDS.toSeconds(currentTime) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(
                        currentTime
                    )
                )
            )
            //   println(hms)

            //    v.tvInventoryTime.text = currentTime.toString()
            v.tvInventoryTime.text = hms
            handler2.postDelayed(this, 10)

            //    val currentTime = System.currentTimeMillis() - vm.statenvtick
            //     v.tvInventoryTime.text = currentTime.toString()
            //     handler2.postDelayed(this, 10)
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



    override fun initStartSendingAPI() {
        // Start the periodic task


        Handler(Looper.myLooper()!!).postDelayed({
            handler3.post(runnable3)
            v.btnSearchForCard.performClick()
        }, 3000)

    }


    private fun callNetworkAPI() {

        /*if(mDataBase == null){
            mDataBase = UFHDatabase.getDatabase(requireContext())
        }



     //   var listNeedtoUpload : List<TagDataEntry>  = mDataBase?.tagDataDao()!!.getList()*/
        //   var listNeedtoUpload : List<TagBean>  = vm.listTagData


        if(vm.listTagData.size > 0){
            val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body: RequestBody = RequestBody.create(JSON, DataStoreUtils.getGson().toJson(vm.listTagData).toString())


            val apiService: ApiInterface = ApiClient.getClient()
                .create(ApiInterface::class.java)
            val call: Call<APIResponse> = apiService.postData(body)
            call.enqueue(this)
        }

    }


    override fun onResponse(call: Call<APIResponse?>?, response: Response<APIResponse?>) {

        Toast.makeText(requireContext(),response.body()?.message, Toast.LENGTH_SHORT).show()
        if(response.body() != null  && response.body()!!.isSuccess){
            /*if(!response.body()!!.data.isNullOrEmpty()){
                removerFromDatabase(response.body()!!.data!!)
            }*/

            vm.listTagData.clear()
            adapter.data.clear()
            adapter.notifyDataSetChanged()
        }


    }

    override fun onFailure(call: Call<APIResponse?>?, t: Throwable) {

        if (t is SSLHandshakeException) {
            Toast.makeText(requireContext(),t.message, Toast.LENGTH_SHORT).show()
        } else if (t is UnknownHostException || t is IllegalStateException) {
            Toast.makeText(requireContext(),"Please check your internet connection", Toast.LENGTH_SHORT).show()
        } else if (t is SocketTimeoutException) {
            Toast.makeText(requireContext(), "Request timeout, please try again.", Toast.LENGTH_SHORT).show()
            return
        } else {
            Toast.makeText(requireContext(),t.message, Toast.LENGTH_SHORT).show()
        }
    }


    fun addToDatabase(list : MutableList<TagBean>){
        CoroutineScope(IO).launch {
            //  mDataBase?.tagDataDao()!!.insert(map(list))
        }
    }

    fun removerFromDatabase(list : List<TagBean>){
        CoroutineScope(IO).launch {
            for(i in list){
                //   mDataBase?.tagDataDao()!!.deleteData(i.epcId, i.rssi, i.times, i.antenna, i.additionalData)
            }
        }
    }


    /*fun map(list : List<TagBean>) : List<TagDataEntry>{
        var tagDataEntry : MutableList<TagDataEntry> = ArrayList()

        for(i in list){
            tagDataEntry.add(TagDataEntry(i.epcId, i.rssi, i.times, i.antenna, i.additionalData))
        }

        return tagDataEntry
    }*/

}