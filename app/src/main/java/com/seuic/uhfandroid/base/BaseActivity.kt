package com.seuic.uhfandroid.base

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.scottyab.rootbeer.RootBeer
import com.seuic.uhfandroid.BuildConfig
import com.seuic.uhfandroid.R
import com.seuic.uhfandroid.bean.ApkVersion
import com.seuic.uhfandroid.database.UFHDatabase
import com.seuic.uhfandroid.util.DataStoreUtils
import com.seuic.uhfandroid.util.DeveloperOptionsObserver
import com.seuic.uhfandroid.util.Utility
import java.lang.reflect.ParameterizedType


abstract class BaseActivity<VM : BaseViewModel, VB : ViewBinding> : AppCompatActivity(), DeveloperOptionsObserver.OnDeveloperOptionsChangedListener {


    private val REQUEST_CODE_ASK_PERMISSIONS = 1
    private val REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSIONS = 2
    lateinit var mContext: FragmentActivity
    lateinit var vm: VM
    lateinit var v: VB

    private var loadingDialog: ProgressDialog? = null
    private val TAG = BaseActivity::class.simpleName
    var ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE: Int = 2323
    private var developerOptionsObserver: DeveloperOptionsObserver? = null


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
      /*  val version = ApkVersion("")
        version.apkVersion = "2.0.0"
      //  version.apkUrl = "https://readerprocess-uat.chola.murugappa.com/download/apk/21e01fd6-042b-44a7-9f3f-ae78938afcc2"
        version.apkUrl = "https://esthenos-save-dev.s3.amazonaws.com/apk_uploads/save_gravity_040820261754.apk?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAZDSVGKVZIQQOTO5Q%2F20260901%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Date=20260901T062508Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=cf436428fde0a5148f6511183f0ec9313eac75ca8e45450be35c46edf191be6f"
        DataStoreUtils.setApkVersion(version, this)*/

        initView()
        initData()
        initVM()
        initClick()
        requestPermissions()


        val rootBeer = RootBeer(this)
        // if device is rooted , won't allow to use this application
        // if device is rooted , won't allow to use this application
        if (rootBeer.isRooted || rootBeer.isRootedWithBusyBoxCheck
            || Utility.isDeviceRooted()
            || Utility.checkBuildConfigIsEmulator(this)
            || Utility.checkRunningProcesses(this)
            || Utility.searchForMagisk(this)
        ) {
            if (!BuildConfig.DEBUG) {
                try {
                    Toast.makeText(applicationContext,"This is a rooted device, we don't allow the use of our mobile app on rooted devices",
                        Toast.LENGTH_LONG).show()
                    finish()
                } catch (e: Exception) {

                }
            }
        }


        //        }
        checkDeveloperMode()

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }

    private fun checkDeveloperMode() {
        val developerOptionsEnabled = Settings.Global.getInt(
            getContentResolver(),
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
        ) == 1

        onDeveloperOptionsChanged(developerOptionsEnabled)

        developerOptionsObserver = DeveloperOptionsObserver(this, Handler(), this)
        getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED),
            true, developerOptionsObserver!!
        )
    }

    public override fun onDeveloperOptionsChanged(enabled: Boolean) {
        // Handle developer options state change here
        if (!BuildConfig.DEBUG && enabled) {
            try {
                Toast.makeText(getApplicationContext(), "Developer mode detected. Turn it off to continue using the application.", Toast.LENGTH_LONG).show()
            //    finish()
            } catch (e: java.lang.Exception) {
            }
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
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.LOCATION_HARDWARE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_SETTINGS,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_CONTACTS
                ), REQUEST_CODE_ASK_PERMISSIONS
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    ActivityCompat.startActivityForResult(this,
                        intent,
                        REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSIONS, null
                    )
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!getPackageManager().canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:" + getPackageName()))
                    startActivityForResult(intent, REQUEST_CODE_ASK_PERMISSIONS)
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

    protected fun showAlertDialog(alertTitle: String?, alertMessage: String?,  positiveButtonTitle: String?,
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