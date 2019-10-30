package com.koso.exohome.exohomelibrarysample.ui.main

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
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
    private var deviceClient: ExoHomeDeviceClient? = null

    /**
     * The mock MAC Address for demo
     */
    private val mockMacAddr = "0a86dda0514b"

    private val connectState = MutableLiveData<Boolean>().apply { postValue(false) }

    private val mqttConnectListener = object : IMqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken?) {
            showState(true)
            vDeviceProgressBar.visibility = View.INVISIBLE
            vStatus.text = "Connected"
        }

        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            showState(false)
            vDeviceProgressBar.visibility = View.INVISIBLE
            vStatus.text = "Connect fail"
        }

    }

    private val mqttCallback = object : MqttCallback {
        override fun messageArrived(topic: String?, message: MqttMessage?) {
            Log.d(LOG_TAG, "received --- topic:${topic ?: " "}  message: ${message ?: ""}")
            topic?.let {
                if (topic.contains("provision")) {
                    SharedPrefHandler.setDeviceToken(context!!, message.toString())
                    handleProvisionSuccess(message.toString())
                } else if (topic.contains("owner")) {
                    message?.let {
                        val owner = OwnerModel(it.toString())
                        deviceClient?.publish(
                            owner.createResourceCommand(),
                            object : IMqttActionListener {
                                override fun onSuccess(asyncActionToken: IMqttToken?) {
                                    Toast.makeText(context, "owner acked", Toast.LENGTH_SHORT)
                                        .show()
                                    vDeviceProgressBar.visibility = View.INVISIBLE
                                    vStatus.text = "Published owner info"
                                }

                                override fun onFailure(
                                    asyncActionToken: IMqttToken?,
                                    exception: Throwable?
                                ) {
                                    vDeviceProgressBar.visibility = View.INVISIBLE
                                }

                            })
                        vDeviceProgressBar.visibility = View.VISIBLE
                    }
                }
            }
        }

        override fun connectionLost(cause: Throwable?) {
            Log.d(LOG_TAG, "connection lost")
            showState(false)
            vStatus.text = "Disconnected"
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
            Log.d(LOG_TAG, "delivery complete")
        }

    }

    private fun handleProvisionSuccess(token: String) {

        context?.let {
            vStatus.text = "Connenct with token"
            deviceClient?.connect(deviceToken = token, listener = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    publishRestInfo(token)
                    vDeviceProgressBar.visibility = View.INVISIBLE
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    vDeviceProgressBar.visibility = View.INVISIBLE
                    vStatus.text = "Connect fail"
                }
            })
            vDeviceProgressBar.visibility = View.INVISIBLE
        }
    }

    private fun publishRestInfo(token: String) {

        /** ESH product resource
         *0
         **/

        val esh = EshModel("KOSO", "1.00", "KOSO001")
        deviceClient?.publish(esh.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(LOG_TAG, "esh published")
                vStatus.text = "Published esh info"
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(LOG_TAG, "esh failed")
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
        deviceClient?.publish(module.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(LOG_TAG, "module published")
                vStatus.text = "Published module info"
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(LOG_TAG, "module failed")
            }
        })

        // cert
        val cert = CertModel(
            Fingerprint("1dfac17adf3867c9a28acb329de8d16d8b412d8b"),
            Validity("11/10/06", "11/10/31")
        )
        deviceClient?.publish(cert.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(LOG_TAG, "cert published")
                vStatus.text = "Published cert info"
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(LOG_TAG, "cert failed")
            }
        })

        // ota
        val ota = OtaModel("idle")
        deviceClient?.publish(ota.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(LOG_TAG, "ota published")
                vStatus.text = "Published ota info"
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(LOG_TAG, "ota failed")
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

        deviceClient?.publish(schedules.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(LOG_TAG, "schedules published")
                vStatus.text = "Published schedules info"
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(LOG_TAG, "schedules failed")
            }
        })

        // states
        val states = HashMap<String, Any>().apply {
            put("H00", 12345)
            put("H01", 1200)
            put("H02", 3456)
            put("H03", 100)
            put("H04", 54)
            put("H05", 60)
            put("H06", 1)
        }

        val statesModel = StatesModel(states)
        deviceClient?.publish(statesModel.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(LOG_TAG, "states published")
                vStatus.text = "Published states info"
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(LOG_TAG, "states failed")
            }
        })

        // token
        SharedPrefHandler.getOwnerProvisionToken(context!!)?.let {
            val tokenModel = TokenModel(it)
            deviceClient?.publish(tokenModel.createResourceCommand(), object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(LOG_TAG, "token published")
                    vStatus.text = "Published token info"
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(LOG_TAG, "token failed")
                }
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    override fun onDestroyView() {
        super.onDestroyView()
        deviceClient?.close()
    }
    private fun registerConnectState() {
        connectState.observe(
            this,
            Observer<Boolean> { t ->
                vStatus.text = if(t) "Connected" else "Disconnected"
                showState(t) })
    }


    private fun initViews() {
        if (SharedPrefHandler.getDeviceId(context!!).isNotEmpty()) {
            val id = SharedPrefHandler.getDeviceId(context!!)
            vDeviceId.setText(id)
        } else {
            createNewDeviceId()
        }


        vConnect.setOnClickListener {
            context?.let {

                if (deviceClient != null && deviceClient!!.isConnected()) {
                    deviceClient?.disconnect()
                    vStatus.text = "Disconnected"
                } else {
                    SharedPrefHandler.getOwnerProvisionToken(context!!)
                        ?.let { ownerProvisionToken ->
                            if (ownerProvisionToken.isNotEmpty()) {
                                deviceClient = ExoHomeDeviceClient(
                                    it,
                                    vProductId.text.toString(),
                                    mqttCallback
                                )
                                registerConnectState()
                                doConnect()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Provision token is required",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                }
            }
        }

        vProvision.setOnClickListener {
            doProvision()
        }

        vGenId.setOnClickListener {
            SharedPrefHandler.setDeviceToken(context!!, "")
            createNewDeviceId()
        }

        vSendState.setOnClickListener {
            val name = vStateName.text.toString()
            val value = vStateValue.text.toString()
            sendStateValue(name, value as Any)
        }

        showState(false)
    }

    private fun sendStateValue(name: String, value: Any) {
        val map = HashMap<String, Any>()
        map.put(name, value)
        deviceClient?.publish(
            StatesModel(map).createResourceCommand(),
            object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Toast.makeText(context, "data send", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Toast.makeText(context, "failed", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun createNewDeviceId() {
        val deviceId = DeviceIdGenerator.createId(mockMacAddr) // given a default mac address
        vDeviceId.setText(deviceId)
        SharedPrefHandler.setDeviceId(context!!, deviceId)
    }

    private fun doProvision() {

        vStatus.text = "Publish provision info"
        assert(vDeviceId.text.toString()!!.toByteArray().size == 24)
        deviceClient?.publish(
            ProvisionCommand(vDeviceId.text.toString()),
            object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(LOG_TAG, asyncActionToken?.response?.payload?.decodeToString() ?: "null")
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
        vDeviceProgressBar.visibility = View.VISIBLE
        SharedPrefHandler.getDeviceToken(context!!)?.let {
            if (it.isEmpty()) {
                deviceClient?.connect(listener = mqttConnectListener)
                vStatus.text = "Connect without token"
            } else {
                deviceClient?.connect(deviceToken = it, listener = mqttConnectListener)
                vStatus.text = "Connect with token"
            }
        }
    }

    private fun showState(connected: Boolean){
        context?.let{context ->
            SharedPrefHandler.getDeviceToken(context)?.let { token ->
                if (token.isNotEmpty()) {
                    // We don't need provision when provision token is already available
                    vProvision.isEnabled = false

                    if (connected) {
                        vConnect.setText(R.string.disconnect)
                    } else {
                        vConnect.setText(R.string.connect_with_token)
                    }
                } else {
                    vProvision.isEnabled = connected

                    if(connected){
                        vConnect.setText(R.string.disconnect)
                    }else{
                        vConnect.setText(R.string.connect_without_token)
                    }
                }
            }
            vSendState.isEnabled = connected
            vSendState.setBackgroundColor(if (connected) Color.GREEN else Color.RED)
            vConnect.isEnabled = true

        }

    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            if (item.itemId == R.id.rest) {
                deviceClient?.close()
                try {
                    showState(false)
                    vStatus.text = ""
                    createNewDeviceId()
                }catch (e: IllegalStateException){
                    e.printStackTrace()
                }
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
