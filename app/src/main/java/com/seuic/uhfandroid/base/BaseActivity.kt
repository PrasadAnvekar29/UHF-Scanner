package com.seuic.uhfandroid.base

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.seuic.uhfandroid.R
import com.seuic.uhfandroid.database.UFHDatabase
import com.seuic.uhfandroid.util.DataStoreUtils
import java.lang.reflect.ParameterizedType


abstract class BaseActivity<VM : BaseViewModel, VB : ViewBinding> : AppCompatActivity() {

    lateinit var mContext: FragmentActivity
    lateinit var vm: VM
    lateinit var v: VB

    private var loadingDialog: ProgressDialog? = null
    private val TAG = BaseActivity::class.simpleName
    var ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE: Int = 2323


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

        UFHDatabase.getDatabase(this)


        initView()
        initData()
        initVM()
        initClick()
        requestPermissions()


        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                // You have permission
            }
        }
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



    interface AlertDialogActionListener {
        fun action(isPositive: Boolean)
    }
    private var alertDialogBuilder: AlertDialog.Builder? = null

    protected fun showAlertDialog(alertTitle: String?, alertMessage: String?, positiveButtonTitle: String?,
                                  negativeButtonTitle: String?,  isCancelable : Boolean?, isEditable : Boolean, actionListener: AlertDialogActionListener?) {
        try {

            val inflater = getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val promptView: View = inflater.inflate(R.layout.dialog_alert, null)
            alertDialogBuilder = AlertDialog.Builder(this)
            val mTitle = promptView.findViewById<View>(R.id.title) as AppCompatTextView
            val mBranchIdText = promptView.findViewById<View>(R.id.branch_id_text) as AppCompatTextView
            var mBranchId = promptView.findViewById<View>(R.id.branch_id) as AppCompatEditText
            val mPositive = promptView.findViewById<View>(R.id.positive) as AppCompatButton
            val mNegative = promptView.findViewById<View>(R.id.negative) as AppCompatButton

            mTitle.text = alertTitle ?: "Branch Id"
            mBranchId.setText(alertMessage ?: "Message")
            mPositive.text = positiveButtonTitle ?: "Ok"
            mNegative.text = negativeButtonTitle ?: "Cancel"


            alertDialogBuilder!!.setView(promptView)
            val alertDialog = alertDialogBuilder!!.create()

            if (negativeButtonTitle == null) {
                mNegative.visibility = View.GONE
            }


            if(isEditable ){
                mBranchId.isEnabled = true
                mBranchIdText.visibility = View.VISIBLE
            } else {
                mBranchId.isEnabled = false
                mBranchIdText.visibility = View.INVISIBLE
            }




            mPositive.setOnClickListener(View.OnClickListener {

                if(!mBranchId.text.toString().isNullOrEmpty()){
                    val branchId = mBranchId.text.toString().trim()
                    DataStoreUtils.setBranchId(branchId, this)
                }
                actionListener?.action(true)

                alertDialog.dismiss()
            })
            mNegative.setOnClickListener {
                actionListener?.action(false)
                alertDialog.dismiss()
            }

            if(isCancelable != null){
                alertDialog.setCanceledOnTouchOutside(isCancelable)
                alertDialog.setCancelable(isCancelable)
            } else {
                alertDialog.setCanceledOnTouchOutside(true)
                alertDialog.setCancelable(true)
            }


            if(isCancelable != null){
                alertDialog.setCanceledOnTouchOutside(isCancelable)
                alertDialog.setCancelable(isCancelable)
            } else {
                alertDialog.setCanceledOnTouchOutside(true)
                alertDialog.setCancelable(true)
            }

            alertDialog.show()
            alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        } catch (ignored: Exception) {
        }
    }

}