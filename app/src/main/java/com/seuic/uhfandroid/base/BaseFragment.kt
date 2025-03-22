package com.seuic.uhfandroid.base


import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment

import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding


import java.lang.reflect.ParameterizedType

abstract class BaseFragment<VM : BaseViewModel, VB : ViewBinding> : Fragment() {

    lateinit var vm: VM
    lateinit var v: VB

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val type = javaClass.genericSuperclass as ParameterizedType
        val clazz1 = type.actualTypeArguments[0] as Class<VM>
        vm = ViewModelProvider(this).get(clazz1)

        val clazz2 = type.actualTypeArguments[1] as Class<VB>
        val method = clazz2.getMethod("inflate", LayoutInflater::class.java)
        v = method.invoke(null, layoutInflater) as VB
        initView()
        initData()
        initVM()
        initClick()
        initStartSendingAPI()
        return v.root
    }

    abstract fun initView()

    abstract fun initClick()

    abstract fun initData()

    abstract fun initVM()

    abstract fun initStartSendingAPI()
}