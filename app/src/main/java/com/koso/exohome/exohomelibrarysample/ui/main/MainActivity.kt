package com.koso.exohome.exohomelibrarysample.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.koso.exohome.exohomelibrarysample.R

class MainActivity : AppCompatActivity() {

    @ExperimentalStdlibApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ConnectFragment.newInstance())
                .commitNow()
        }
    }
}
