package com.koso.exohome.exohomelibrarysample.ui.exoconnect

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.koso.exohome.DeviceIdGenerator
import com.koso.exohome.exohomelibrarysample.R
import com.koso.exohome.exohomelibrarysample.mgr.ExoHomeConnectionManager
import com.koso.exohome.exohomelibrarysample.ui.login.LoginActivity
import com.koso.exohome.exohomelibrarysample.utils.SharedPrefHandler
import com.koso.kosoproxy.ui.exoconnect.DeviceRegisterViewModel
import kotlinx.android.synthetic.main.activity_device_registration.*
import java.util.*
import kotlin.concurrent.schedule

class DeviceRegisterActivity : AppCompatActivity() {

    /**
     * The mock MAC Address for demo
     */
    private val mockMacAddr = "0a86dda0514b"

    private var model: DeviceRegisterViewModel? = null

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, DeviceRegisterActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_registration)
        model = ViewModelProviders.of(this)[DeviceRegisterViewModel::class.java]
        initViews()
        registerConnectState()

        if (SharedPrefHandler.getOwnerProvisionToken().isEmpty()) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    private fun registerConnectState() {

        model?.connectStateLiveData?.observe(this, Observer {
            when (it) {
                ExoHomeConnectionManager.ConnectState.Disconnect -> {
                    vProgressBar.visibility = View.INVISIBLE
                    vState.setText(R.string.disconnect)
                    vConnect.setText(R.string.connect)
                }
                ExoHomeConnectionManager.ConnectState.Connected -> {
                    vProgressBar.visibility = View.INVISIBLE
                    vState.setText(R.string.connected)
                    vConnect.setText(R.string.disconnect)
                    vImage.setImageDrawable(getDrawable(R.drawable.avd_checked_anim))
                    if (vImage.drawable is AnimatedVectorDrawable) {
                        (vImage.drawable as AnimatedVectorDrawable).start()
                    }

                    Timer("close", false).schedule(1500) {
                        finish()
                    }
                    if(SharedPrefHandler.getDeviceToken().isNotEmpty()){

                    }

                }
                ExoHomeConnectionManager.ConnectState.ProvisionComplete -> {
                    vProgressBar.visibility = View.INVISIBLE
                    vState.setText(R.string.provision_completed)
                }
                ExoHomeConnectionManager.ConnectState.Provisioning -> {
                    vProgressBar.visibility = View.VISIBLE
                    vState.setText(R.string.gain_device_provision_token)
                }
                ExoHomeConnectionManager.ConnectState.Connecting -> {
                    vProgressBar.visibility = View.VISIBLE
                    vState.setText(R.string.connecting)
                }
                ExoHomeConnectionManager.ConnectState.SendingRestInfo -> {
                    vProgressBar.visibility = View.VISIBLE
                    vState.setText(R.string.sending_info)
                }

            }
        })
    }

    private fun initViews() {
        if (SharedPrefHandler.getDeviceId().isEmpty()) {
            createNewDeviceId()
        }
        vDeviceId.setText(SharedPrefHandler.getDeviceId())

        vConnect.setOnClickListener {
            if (dataValidate()) {
                model?.connect(vProductId.text.toString(), vDeviceId.text.toString())
            }
        }
    }

    private fun dataValidate(): Boolean {
        vProductId.error = null
        vDeviceId.error = null
        return when {
            vProductId.text.isEmpty() -> {
                vProductId.error = getString(R.string.invalidate_data)
                false
            }
            vDeviceId.text.isEmpty() -> {
                vDeviceId.error = getString(R.string.invalidate_data)
                false
            }
            else -> true
        }
    }

    private fun createNewDeviceId() {
        val deviceId = DeviceIdGenerator.createId(mockMacAddr) // given a default mac address
        SharedPrefHandler.setDeviceId(deviceId)
    }
}
