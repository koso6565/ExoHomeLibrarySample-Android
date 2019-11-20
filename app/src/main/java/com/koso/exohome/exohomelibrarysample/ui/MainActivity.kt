package com.koso.exohome.exohomelibrarysample.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionManager
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Observer
import com.koso.exohome.exohomelibrarysample.R
import com.koso.exohome.exohomelibrarysample.mgr.ExoHomeConnectionManager
import com.koso.exohome.exohomelibrarysample.mgr.LoggerManager
import com.koso.exohome.exohomelibrarysample.ui.login.LoginActivity
import com.koso.exohome.exohomelibrarysample.utils.SharedPrefHandler
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken

class MainActivity : AppCompatActivity() {


    companion object {
        fun launch(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        }
    }

    private val constraintSet1 = ConstraintSet()
    private val constraintSet2 = ConstraintSet()

    private var isExoHomeReady = false
    private val listener = object: IMqttActionListener{
        override fun onSuccess(asyncActionToken: IMqttToken?) {

        }

        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        registerConnectState()
        LoggerManager.instance._messageListener.observe(this, Observer {
            vLog.text = "[${it[0]}]  \n${it[1]}\n${vLog.text}"
        })
    }

    private fun registerConnectState() {
        ExoHomeConnectionManager.instance.connectStateLiveData.observe(this, Observer {
            LoggerManager.instance.publish(it.name)
            when(it){
                ExoHomeConnectionManager.ConnectState.Connected -> {
                    vConnectExohome.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        getDrawable(R.drawable.ic_check_circle_black_24dp),
                        null,
                        null,
                        null
                    )
                    isExoHomeReady = true
                }
                ExoHomeConnectionManager.ConnectState.ProvisionComplete -> {
                    vConnectExohome.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        getDrawable(R.drawable.ic_check_circle_black_24dp),
                        null,
                        null,
                        null
                    )
                    isExoHomeReady = true
                }
                else -> {
                    vConnectExohome.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        getDrawable(R.drawable.ic_remove_circle_black_24dp),
                        null,
                        null,
                        null
                    )
                    isExoHomeReady = false
                }
            }
            showLogger(isExoHomeReady)

        })
    }


    private fun initViews() {
        constraintSet1.clone(vConstraintLayout)
        constraintSet2.clone(this, R.layout.activity_main_1)

        vConnectExohome.setOnClickListener {
            LoginActivity.launch(this)
        }

        vSend.setOnClickListener{
            val name = vTopic.text.toString()
            val value = vValue.text.toString()
            ExoHomeConnectionManager.instance.sendStateValue(name, value)

        }
    }

    private fun showLogger(show: Boolean){

        TransitionManager.beginDelayedTransition(vConstraintLayout)
        if (show) {
            constraintSet2.applyTo(vConstraintLayout)
            vLog.text =
                "------ device token -----\n${SharedPrefHandler.getDeviceToken()}\n------ device id -----\n${SharedPrefHandler.getDeviceId()}"
        } else {
            constraintSet1.applyTo(vConstraintLayout)
            vLog.text = ""
        }
    }



}
