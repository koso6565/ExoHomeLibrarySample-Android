package com.koso.exohome.exohomelibrarysample.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.koso.exohome.exohomelibrarysample.R
import com.koso.exohome.exohomelibrarysample.utils.SharedPrefHandler
import com.koso.exohome.library.DeviceIdGenerator
import com.koso.exohome.library.ExoHomeDeviceClient
import com.koso.exohome.library.command.ProvisionCommand
import com.koso.exohome.library.prodresources.*
import kotlinx.android.synthetic.main.fragment_connect.*
import org.eclipse.paho.client.mqttv3.*

@ExperimentalStdlibApi
class ConnectFragment : Fragment() {

    companion object {
        fun newInstance() = ConnectFragment()
    }

    /**
     * The constant for logcat message handling
     */
    private val LOG_TAG = "EXOHOME"

    /**
     * To handle the connection and communication with ExoHome server
     */
    private lateinit var deviceClient: ExoHomeDeviceClient

    /**
     * The mock MAC Address for demo
     */
    private val mockMacAddr = "0a86dda0514b"

    private val connectState = MutableLiveData<Boolean>().apply { postValue(false) }

    private val mqttConnectListener = object : IMqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken?) {
            showState(true)
        }

        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            showState(false)
        }

    }

    private val mqttCallback = object : MqttCallback {
        override fun messageArrived(topic: String?, message: MqttMessage?) {
            Log.d("EXOHOME", "received --- topic:${topic ?: " "}  message: ${message ?: ""}")
            topic?.let {
                if (topic.contains("provision")) {
                    SharedPrefHandler.setToken(context!!, message.toString())
                    vToken.setText(message.toString())
                    handleProvisionSuccess(message.toString())
                }
            }
        }

        override fun connectionLost(cause: Throwable?) {
            Log.d("EXOHOME", "connection lost")
            showState(false)
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
            Log.d("EXOHOME", "delivery complete")
        }

    }

    private fun handleProvisionSuccess(token: String) {

        context?.let {
            deviceClient.connect(deviceToken = token, listener = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    publishRestInfo(token)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                }
            })
        }
    }

    private fun publishRestInfo(token: String) {

        // esh
        val esh = EshModel("KOSO", "1.00", "KOSO001")
        deviceClient.publish(esh.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("EXOHOME", "esh published")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d("EXOHOME", "esh failed")
            }
        })

        // module
        val module = ModuleModel(
            "1.00",
            mockMacAddr,
            "192.168.0.13",
            mockMacAddr,
            "KOSO-${mockMacAddr.substring(9)}"
        )
        deviceClient.publish(module.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("EXOHOME", "module published")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d("EXOHOME", "module failed")
            }
        })

        // cert
        val cert = CertModel(
            Fingerprint("1dfac17adf3867c9a28acb329de8d16d8b412d8b"),
            Validity("11/10/06", "11/10/31")
        )
        deviceClient.publish(cert.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("EXOHOME", "cert published")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d("EXOHOME", "cert failed")
            }
        })

        // ota
        val ota = OtaModel("idle")
        deviceClient.publish(ota.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("EXOHOME", "ota published")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d("EXOHOME", "ota failed")
            }
        })

        // schedules
        val eshMap = HashMap<String, Any>()
        eshMap.put("H000", 1)
        eshMap.put("H001", 2)

        val state = HashMap<String, Any>()
        state.put("H000", 1)
        state.put("H001", 2)


        val schedules = SchedulesModel(
            listOf(
                Schedule(
                    1, 1477377969,
                    listOf(1, 2, 3, 4, 5, 6, 7), "12:24", "13:24", eshMap, state
                )
            )
        )

        deviceClient.publish(schedules.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("EXOHOME", "schedules published")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d("EXOHOME", "schedules failed")
            }
        })

        // states
        val states = HashMap<String, Any>().apply {
            put("H00", 0)
            put("H01", 0)
            put("H02", 0)
            put("H03", 0)
        }

        val statesModel = StatesModel(states)
        deviceClient.publish(statesModel.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("EXOHOME", "states published")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d("EXOHOME", "states failed")
            }
        })

        // token
        val tokenModel =
            TokenModel("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJpMjVsbWxuczVlbmhjMDAwMCIsImV4cCI6MTU3MzgwMzQ1NCwiaWF0IjoxNTcxMjExNDU0LCJpc3MiOiJsNTVnYmp6YWF5dHcwMDAwMCIsInN1YiI6Nn0.MDM2OWNiMWZhOTA1NTczMzYzZWZjZTM3MDExNmU3ZTJlMDk2YmNmMjgxZTZjMzIyOTVhODAzOWZhMTU4YWVjMA")
        deviceClient.publish(tokenModel.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("EXOHOME", "token published")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d("EXOHOME", "token failed")
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(R.layout.fragment_connect, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun registerConnectState() {
        connectState.observe(
            this,
            Observer<Boolean> { t -> showState(t) })
    }


    private fun initViews() {

        if (SharedPrefHandler.getDeviceId(context!!).isNotEmpty()) {
            val id = SharedPrefHandler.getDeviceId(context!!)
            vDeviceId.setText(id)
        } else {
            createNewDeviceId()
        }

        vToken.setText(SharedPrefHandler.getToken(context!!))


        vConnect.setOnClickListener {
            context?.let {
                deviceClient = ExoHomeDeviceClient(it, vProductId.text.toString(), mqttCallback)
                registerConnectState()
                doConnect()
            }
        }

        vProvision.setOnClickListener {
            doProvision()
        }

        vGenId.setOnClickListener {
            SharedPrefHandler.setToken(context!!, "")
            createNewDeviceId()
        }


    }

    private fun createNewDeviceId() {

        val deviceId = DeviceIdGenerator.createId(mockMacAddr) // given a default mac address
        vDeviceId.setText(deviceId)
        SharedPrefHandler.setDeviceId(context!!, deviceId)
        vToken.setText("")
    }

    private fun doProvision() {
        assert(vDeviceId.text.toString()!!.toByteArray().size == 24)
        deviceClient.publish(
            ProvisionCommand(vDeviceId.text.toString()),
            object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(LOG_TAG, asyncActionToken?.response?.payload?.decodeToString() ?: "null")
                    Toast.makeText(context, "provision success", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(
                        LOG_TAG,
                        asyncActionToken?.exception?.message ?: "" + "  ..." + exception?.message
                        ?: ""
                    )
                    Toast.makeText(context, "failed", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun doConnect() {
        if (vToken.text.toString().isEmpty()) {
            deviceClient.connect(listener = mqttConnectListener)
        } else {
            deviceClient.connect(deviceToken = vToken.text.toString(), listener = mqttConnectListener)
        }
    }

    private fun showState(connected: Boolean) {
        vConnect.isEnabled = true
        vProvision.isEnabled = connected
    }
}
