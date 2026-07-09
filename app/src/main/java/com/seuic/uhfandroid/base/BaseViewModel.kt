package com.seuic.uhfandroid.base

import android.app.AlertDialog
import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.ProgressBar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL


open class BaseViewModel: ViewModel() {


    private var progressDialog: AlertDialog? = null

    fun showLoading(message: String?, context: Context) {
        try {
            if (progressDialog == null) {

                val progressBar = ProgressBar(context).apply {
                    isIndeterminate = true
                }

                progressDialog = AlertDialog.Builder(context)
                    .setView(progressBar)
                    .setCancelable(false)
                    .create()

            }

            progressDialog!!.setMessage(message)
            progressDialog!!.setCancelable(false)
            progressDialog!!.setCanceledOnTouchOutside(false)
            progressDialog!!.show()
        } catch (e: Exception) {
        }
    }

    fun hideLoading() {
        try {
            if (progressDialog != null) {
                progressDialog!!.cancel()
            }
        } catch (e: Exception) {
        }
    }



    fun downloadApk(apkUrl: String, context: Context): LiveData<String> {
        val localPath = MutableLiveData<String>()

        CoroutineScope(IO).launch {

            try {
                val path: String = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    .toString() + "/esaf_gravity.apk"
                try {
                    val url = URL(apkUrl)
                    val connection = url.openConnection()
                    connection.connect()
                    connection.contentLength

                    // download the file
                    val input: InputStream = BufferedInputStream(url.openStream())
                    val output: OutputStream = FileOutputStream(path)
                    val data = ByteArray(1024)
                    var total: Long = 0
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count.toLong()
                        // publishProgress((int) (total * 100 / fileLength));
                        output.write(data, 0, count)
                    }
                    output.flush()
                    output.close()
                    input.close()
                } catch (e: Exception) {
                    Log.e("YourApp", "Well that didn't work out so well...")
                }
                localPath.postValue(path)

            }catch (e: Exception){
                e.printStackTrace()
            }
        }
        return localPath
    }
}

