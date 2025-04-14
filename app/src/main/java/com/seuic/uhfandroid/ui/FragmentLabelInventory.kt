package com.seuic.uhfandroid.ui


import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.seuic.androidreader.sdk.Constants
import com.seuic.uhfandroid.MainActivity
import com.seuic.uhfandroid.R
import com.seuic.uhfandroid.adapter.TagInfoAdapter
import com.seuic.uhfandroid.base.BaseFragment
import com.seuic.uhfandroid.bean.TagBean
import com.seuic.uhfandroid.database.TagDataEntry
import com.seuic.uhfandroid.database.UFHDatabase
import com.seuic.uhfandroid.databinding.FragmentLabelInventoryBinding
import com.seuic.uhfandroid.ext.clearTagList
import com.seuic.uhfandroid.ext.currentTag
import com.seuic.uhfandroid.ext.isSearching
import com.seuic.uhfandroid.ext.itemClickable
import com.seuic.uhfandroid.ext.resetCurrentTag
import com.seuic.uhfandroid.service.APIResponse
import com.seuic.uhfandroid.service.ApiClient
import com.seuic.uhfandroid.service.ApiInterface
import com.seuic.uhfandroid.util.DataStoreUtils
import com.seuic.uhfandroid.viewmodel.ViewModelLabelInventory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.seuic.uhfandroid.service.BaseApiResponse


class FragmentLabelInventory :
    BaseFragment<ViewModelLabelInventory, FragmentLabelInventoryBinding>()



{


    private var mDataBase : UFHDatabase? = null
    private var mBranchId : String? = null


    private var handler3 = Handler()
    private var runnable3: Runnable = object : Runnable {
        override fun run() {


            mBranchId = DataStoreUtils.getBranchId(requireActivity())

            if(mBranchId.isNullOrEmpty()){
                Toast.makeText(requireContext(), "Please set Branch id.", Toast.LENGTH_SHORT).show()
            } else {
                // Call your API here
                callNetworkAPI()
            }



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
                TimeUnit.MILLISECONDS.toMinutes(currentTime) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(currentTime)),
                TimeUnit.MILLISECONDS.toSeconds(currentTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(currentTime))
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
           /* totalCounts.set(0)
            v.tvTagNumber.text = "0"
            v.tvRecognizeTimes.text = "0"
            v.tvInventoryTime.text = "0"
            // 清空标签盘存和标签读写两个界面的标签列表
            clearTagList.postValue(true)
            resetCurrentTag.postValue(true)*/

            CoroutineScope(Dispatchers.IO).launch {
                mDataBase?.tagDataDao()?.deleteAll()
                mDataBase?.tagDataDao()?.truncateAll()
            }


        }
    }

    override fun initData() {
        v.rlvEpc.adapter = adapter
        v.rlvEpc.layoutManager = LinearLayoutManager(activity)
        Constants.connectState.observe(requireActivity()) {
            if (it.state == Constants.CONNECTED) {
                vm.registerListener(requireContext())
            }
        }
    }

    override fun initVM() {
        activity?.runOnUiThread {
            itemClickable.observe(this) {
                v.btnSingleCard.isEnabled = it
                v.btnSingleCard.isClickable = it
            }

            // Single card search livedata
            vm.tagData.observe(this) {
                // 清空list，加入单次寻卡结果
                vm.listTagData.clear()
                vm.listTagData.add(it)
                adapter.setList(vm.listTagData)
                adapter.notifyDataSetChanged()
                v.tvTagNumber.text = vm.listTagData.size.toString()
                v.tvRecognizeTimes.text = "1"
            }

            // Continuous card search livedata
            vm.tagListData.observe(this) {
                adapter.setList(it)
                adapter.notifyDataSetChanged()
             //   v.tvTagNumber.text = it.size.toString()
             //   v.tvRecognizeTimes.text = totalCounts.get().toString()

            }

            // Clear the tag list livedata
            clearTagList.observe(this) {
                if (it) {
                    vm.listTagData.clear()
                    adapter.data.clear()
                    adapter.notifyDataSetChanged()
                }
            }

            // Reset the index of the currently selected tag and tag list
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

            mBranchId = DataStoreUtils.getBranchId(requireContext())

            if(mBranchId.isNullOrEmpty()){
                (getActivity() as MainActivity).showDialog()
            } else {
                v.btnSearchForCard.performClick()
            }


            handler3.post(runnable3)

            if(mDataBase == null){
                mDataBase = UFHDatabase.getDatabase(requireContext())
            }

            mDataBase?.tagDataDao()?.getCount()
            ?.observe(this, Observer { count: List<TagDataEntry> -> this.getCount(count) })

            mDataBase?.tagDataDao()?.getListLiveData()
            ?.observe(this, Observer { listData: MutableList<TagDataEntry> -> this.getListLiveData(listData) })


         //   addToDatabase(null)
        }, 3000)

    }

    private fun getCount(list: List<TagDataEntry>) {
        v.tvTagNumber.text = list.size.toString()
    }
    private fun getListLiveData(listData: MutableList<TagDataEntry>) {
        vm.tagListData.postValue(listData)
    }


    fun callNetworkAPI() {

        if(mDataBase == null){
            mDataBase = UFHDatabase.getDatabase(requireContext())
        }



        CoroutineScope(IO).launch {

         //   addToDatabase(vm.listTagData)


            var listNeedtoUpload : List<TagDataEntry>? =  mDataBase?.tagDataDao()!!.getList()

        //    listNeedtoUpload.addAll(vm.listTagData!!)


            Log.v("Prasad", "1")
            if(!listNeedtoUpload.isNullOrEmpty()){
                Log.v("Prasad","2")
         //     if(listNeedtoUpload!!.isNotEmpty()){
                val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body: RequestBody = RequestBody.create(JSON, DataStoreUtils.getGson().toJson(listNeedtoUpload).toString())


                val apiService: ApiInterface = ApiClient.getClient()
                    .create(ApiInterface::class.java)

                val call: Call<APIResponse.Response> = apiService.postData(mBranchId, body)

                call.enqueue(object : Callback<APIResponse.Response?> {
                    override fun onResponse(call: Call<APIResponse.Response?>, response: Response<APIResponse.Response?>) {
                        try {
                            Log.v("Prasad","3")

                            if(response.body() != null  && response.body()!!.isSuccess){
                                Toast.makeText(requireContext(),response.body()?.message, Toast.LENGTH_SHORT).show()
                                if(response.body()!!.data != null ){
                                 //   vm.listTagData.clear()
                                 //   adapter.data.clear()
                                //    adapter.notifyDataSetChanged()


                                    removerFromDatabase(response.body()!!.data)

                                }

                                /*if(!response.body()!!.data.isNullOrEmpty()){
                                    removerFromDatabase(response.body()!!.data!!)
                                }*/

                               /* vm.listTagData.clear()
                                adapter.data.clear()
                                adapter.notifyDataSetChanged()*/
                            } else {
                                val b = BaseApiResponse()
                                val  msg: String = b.safeErrorResponse(response)
                                Toast.makeText(requireContext(), msg , Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                        }
                    }

                    override fun onFailure(call: Call<APIResponse.Response?>, t: Throwable) {
                        try {
                            Log.v("Prasad","4")

                            val b = BaseApiResponse()
                            val  msg: String = b.safeErrorResponse(t)
                            Toast.makeText(requireContext(), msg , Toast.LENGTH_SHORT).show()

                        } catch (e: Exception) {
                        }
                    }
                })

            }
        }



      //  var dataList : List<TagDataEntry>? = mDataBase?.tagDataDao()!!.getList()

      //  listNeedtoUpload.addAll(dataList!!)

      //  var listNeedtoUpload : List<TagDataEntry>?  = mDataBase?.tagDataDao()!!.getList()
        //   var listNeedtoUpload : List<TagBean>  = vm.listTagData

      //  listNeedtoUpload.addAll(list)

      //  if(vm.listTagData.size > 0){
        /*if(listNeedtoUpload.size > 0){
            //   if(listNeedtoUpload != null && listNeedtoUpload.isNotEmpty()){
            val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body: RequestBody = RequestBody.create(JSON, DataStoreUtils.getGson().toJson(listNeedtoUpload).toString())


            val apiService: ApiInterface = ApiClient.getClient()
                .create(ApiInterface::class.java)
            val call: Call<APIResponse> = apiService.postData(body)
            call.enqueue(this)

        }*/



    }


     fun addToDatabase(list : MutableList<TagBean>?){

    //    mDataBase?.tagDataDao()?.insert(map(list))

        CoroutineScope(IO).launch {
            mDataBase?.tagDataDao()?.insert1(TagDataEntry("1","1"))
            mDataBase?.tagDataDao()?.insert1(TagDataEntry("1","2"))
            mDataBase?.tagDataDao()?.insert1(TagDataEntry("2","1"))
            mDataBase?.tagDataDao()?.insert1(TagDataEntry("2","2"))
            mDataBase?.tagDataDao()?.insert1(TagDataEntry("3","1"))
        }

    }

    fun removerFromDatabase(list : List<APIResponse.Tag>){
        CoroutineScope(IO).launch {
            for(i in list){
                   mDataBase?.tagDataDao()?.deleteData(i.epcId, i.antenna)
            }
        }
    }


    fun map(list : MutableList<TagBean>) : List<TagDataEntry>{
        var tagDataEntry : MutableList<TagDataEntry> = ArrayList()

        for(i in list){
            tagDataEntry.add(TagDataEntry(i.epcId, i.antenna))
        }

        return tagDataEntry
    }

}