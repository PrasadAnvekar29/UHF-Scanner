package com.seuic.uhfandroid.base

import android.Manifest
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.seuic.uhfandroid.R
import java.lang.reflect.ParameterizedType


abstract class BaseActivity<VM : BaseViewModel, VB : ViewBinding> : AppCompatActivity() {

    lateinit var mContext: FragmentActivity
    lateinit var vm: VM
    lateinit var v: VB

    private var loadingDialog: ProgressDialog? = null
    private val TAG = BaseActivity::class.simpleName

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_UHF_AndroidReader)
        super.onCreate(savedInstanceState)
        //注意 type.actualTypeArguments[0]=BaseViewModel，type.actualTypeArguments[1]=ViewBinding
        val type = javaClass.genericSuperclass as ParameterizedType
        val clazz1 = type.actualTypeArguments[0] as Class<VM>
        vm = ViewModelProvider(this).get(clazz1)

        val clazz2 = type.actualTypeArguments[1] as Class<VB>
        val method = clazz2.getMethod("inflate", LayoutInflater::class.java)
        v = method.invoke(null, layoutInflater) as VB
        setContentView(v.root)
        mContext = this
        initView()
        initData()
        initVM()
        initClick()
        requestPermissions()

    }

    fun resetViewBinding(VB: ViewBinding) {
        val type = javaClass.genericSuperclass as ParameterizedType
        val clazz2 = type.actualTypeArguments[1] as Class<VB>
        val method = clazz2.getMethod("inflate", LayoutInflater::class.java)
        v = method.invoke(null, layoutInflater) as VB
        setContentView(v.root)
    }


    private fun requestPermissions() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val permission = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                )
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.LOCATION_HARDWARE, Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.WRITE_SETTINGS, Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS
                        ), 0x0010
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    abstract fun initView()

    abstract fun initClick()

    abstract fun initData()

    abstract fun initVM()
}