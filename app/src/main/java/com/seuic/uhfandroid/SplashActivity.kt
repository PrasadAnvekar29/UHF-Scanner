package com.seuic.uhfandroid

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Environment
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

        lifecycleScope.launch {
            delay(DELAY)
            openTourScreen()
        }

    }



    private fun openTourScreen() {

        MainActivity().launch(this)
        finish()
    }


}