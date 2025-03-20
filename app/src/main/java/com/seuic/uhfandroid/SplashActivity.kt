package com.seuic.uhfandroid

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.seuic.uhfandroid.databinding.ActivitySplashBinding

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SplashActivity : AppCompatActivity()  {

    private val DELAY = 2000L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView( R.layout.activity_splash)



        handler3.post(runnable3)

    }

    private var handler3 = Handler()
    private var runnable3: Runnable = object : Runnable {
        override fun run() {
            // Call your API here
            openTourScreen()

            // Schedule the runnable to run again after 10 seconds
            handler3.postDelayed(this, 2000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler3.removeCallbacks(runnable3)

    }


    private fun openTourScreen() {

        MainActivity().launch(this)
        finish()
    }


}