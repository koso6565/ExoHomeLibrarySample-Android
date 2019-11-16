package com.koso.exohome.exohomelibrarysample.mgr

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.koso.exohome.ExoHomeDeviceClient
import com.koso.exohome.command.ProvisionCommand
import com.koso.exohome.exohomelibrarysample.App
import com.koso.exohome.exohomelibrarysample.utils.SharedPrefHandler
import com.koso.exohome.prodresources.*
import com.squareup.moshi.Moshi
import org.eclipse.paho.client.mqttv3.*

class ExoHomeConnectionManager {
    companion object{
        val instance = ExoHomeConnectionManager()
    }

    /**
     * To handle the connection and communication with ExoHome server
     */
    private var deviceClient: ExoHomeDeviceClient? = null

    /**
     * The private state value for notifying the connection progress
     */
    private val _connectStateLiveData =
        MutableLiveData<ConnectState>(if (deviceClient != null && deviceClient!!.isConnected()) ConnectState.Connected else ConnectState.Disconnect)

    /**
     * The outer [_connectStateLiveData] property for access
     */
    val connectStateLiveData = _connectStateLiveData

    /**
     * The Product id that gathered when connect action is performed
     */
    var productId: String? = null

    /**
     * The Device id that gathered when connect action is performed
     */
    var deviceId: String? = null

    /**
     * The incoming message for inner access, with a String array parameter, which index 0 is topic and 1 is message
     */
    private val _messageArriveListener = MutableLiveData<Array<String>>()

    /**
     * The incoming message with a String array parameter, which index 0 is topic and 1 is message
     */
    val messageArriveLiveData: LiveData<Array<String>> = _messageArriveListener

    /**
     * Mqtt message listener
     */
    private val mqttCallback = object : MqttCallback {
        override fun messageArrived(topic: String?, message: MqttMessage?) {

            topic?.let {
                _messageArriveListener.value = arrayOf(topic, message.toString())


                when{
                    topic.contains("provision") -> {
                        SharedPrefHandler.setDeviceToken(message.toString())
                        handleProvisionSuccess(message.toString())
                    }
                    topic.contains("owner") -> {
                        message?.let {
                            val owner = OwnerModel(it.toString())
                            deviceClient?.publish(
                                owner.createResourceCommand(),
                                object : IMqttActionListener {
                                    override fun onSuccess(asyncActionToken: IMqttToken?) {

                                    }

                                    override fun onFailure(
                                        asyncActionToken: IMqttToken?,
                                        exception: Throwable?
                                    ) {
                                    }

                                })
                        }
                    }
                    topic.contains("action") -> {
                        message?.let { msg ->
                            val moshi = Moshi.Builder().build()
                            val adapter = ActionModelJsonAdapter(moshi)
                            val model  = adapter.fromJson(msg.toString())
                            model?.let { model ->
                                if(model.request == "set"){
                                    val sadapter = ActionSetModelJsonAdapter(moshi)
                                    val data = sadapter.fromJson(msg.toString())
                                    if(data != null) {

                                        val command = ActionResponseModel(model.id, message = "", code = "").createResourceCommand()

                                        deviceClient?.publish(command, object: IMqttActionListener{
                                            override fun onSuccess(asyncActionToken: IMqttToken?) {
                                                Log.d("exohome",asyncActionToken.toString())
                                            }

                                            override fun onFailure(
                                                asyncActionToken: IMqttToken?,
                                                exception: Throwable?
                                            ) {
                                                Log.d("exohome",asyncActionToken.toString())
                                            }

                                        })
                                    }
                                }
                            }

                        }
                    }
                    else -> {

                    }
                }


            }

        }

        override fun connectionLost(cause: Throwable?) {
            _connectStateLiveData.value = ConnectState.Disconnect
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {

        }

    }

    /**
     * Connection state listener
     */
    private val mqttConnectListener = object : IMqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken?) {
            _connectStateLiveData.value = ConnectState.Connected
            if (SharedPrefHandler.getDeviceToken().isEmpty()) {
                doProvision()
            }
        }

        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            _connectStateLiveData.value = ConnectState.Disconnect
        }
    }

    enum class ConnectState {
        Disconnect,
        Connecting,
        Connected,
        Provisioning,
        SendingRestInfo,
        ProvisionComplete
    }

    fun isConnected(): Boolean{
        if(deviceClient == null){
            return false
        }else{
            return deviceClient!!.isConnected()
        }
    }

    fun connect(productId: String, deviceId: String) {

        if (deviceClient != null && deviceClient!!.isConnected()) {
            deviceClient?.disconnect()
            _connectStateLiveData.value = ConnectState.Disconnect
        } else {
            this.productId = productId
            this.deviceId = deviceId
            deviceClient = ExoHomeDeviceClient(
                App.instance!!.applicationContext,
                productId,
                mqttCallback
            )
            doConnect()
        }
    }


    fun sendStateValue(name: String, value: Any, listener: IMqttActionListener) {
        if(deviceClient != null && deviceClient!!.isConnected()) {
            val map = HashMap<String, Any>()
            map.put(name, value)
            deviceClient?.publish(
                StatesModel(map).createResourceCommand(), listener
            )
        }
    }

    fun disconnect(){
        deviceClient?.disconnect()
    }

    private fun doConnect() {
        _connectStateLiveData.value = ConnectState.Connecting
        if (SharedPrefHandler.getDeviceToken().isEmpty()) {
            deviceClient?.connect(listener = mqttConnectListener)
        } else {
            deviceClient?.connect(
                deviceToken = SharedPrefHandler.getDeviceToken(),
                listener = mqttConnectListener
            )
        }
    }

    private fun doProvision() {
        deviceId?.let {
            _connectStateLiveData.value = ConnectState.Provisioning
            deviceClient?.publish(
                ProvisionCommand(it),
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {

                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {

                    }
                })
        }
    }

    private fun handleProvisionSuccess(token: String) {
        _connectStateLiveData.value = ConnectState.Connecting
        deviceClient?.connect(deviceToken = token, listener = object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                publishRestInfo(token)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {

            }
        })
    }


    private fun publishRestInfo(token: String) {

        _connectStateLiveData.value = ConnectState.SendingRestInfo

        /** ESH product resource
         *0
         **/

        val esh = EshModel("KOSO", "1.00", "KOSO001")
        deviceClient?.publish(esh.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            }
        })

        // module

        deviceId?.substring(12)?.let {
            val module = ModuleModel(
                "1.00",
                it,
                "192.168.0.13",
                it,
                "KOSO-${it.substring(9)}"
            )
            deviceClient?.publish(module.createResourceCommand(), object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                }
            })
        }


        // cert
        val cert = CertModel(
            Fingerprint("1dfac17adf3867c9a28acb329de8d16d8b412d8b"),
            Validity("11/10/06", "11/10/31")
        )
        deviceClient?.publish(cert.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            }
        })

        // ota
        val ota = OtaModel("idle")
        deviceClient?.publish(ota.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
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
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
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
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            }
        })

        // token
        SharedPrefHandler.getOwnerProvisionToken()?.let {
            val tokenModel = TokenModel(it)
            deviceClient?.publish(tokenModel.createResourceCommand(), object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    _connectStateLiveData.value = ConnectState.ProvisionComplete
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {

                }
            })
        }
    }
}