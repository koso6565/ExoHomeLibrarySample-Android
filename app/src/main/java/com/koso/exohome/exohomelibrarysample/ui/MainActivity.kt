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
import com.koso.exohome.exohomelibrarysample.ui.login.LoginActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import java.util.*

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
        registerIncomingMessage()
    }

    private fun registerIncomingMessage() {
        ExoHomeConnectionManager.instance.messageArriveLiveData.observe(this, Observer {
            appendLogs("Received topic ${it[0]} - ${it[1]}")
        })
    }

    private fun registerConnectState() {
        ExoHomeConnectionManager.instance.connectStateLiveData.observe(this, Observer {
            appendLogs(it.name)
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
            ExoHomeConnectionManager.instance.sendStateValue(name, value, listener)
            appendLogs("Send $name : $value")
        }
    }

    private fun showLogger(show: Boolean){

        TransitionManager.beginDelayedTransition(vConstraintLayout)
        if (show) {
            constraintSet2.applyTo(vConstraintLayout)
        } else {
            constraintSet1.applyTo(vConstraintLayout)
            vLog.text = ""
        }
    }

    private fun appendLogs(msg: String){
        Calendar.getInstance().let {
            var text = vLog.text.toString()
            text = "${it.get(Calendar.HOUR)}:${it.get(Calendar.MINUTE)}:${it.get(Calendar.SECOND)}  $msg \n$text"
            if (text.length > 10000) {
                text.substring(0, 10000)
            }
            vLog.text = text
        }

    }

}
