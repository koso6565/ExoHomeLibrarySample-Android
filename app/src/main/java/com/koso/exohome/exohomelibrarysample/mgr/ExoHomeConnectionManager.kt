package com.koso.exohome.exohomelibrarysample.mgr

import android.os.CountDownTimer
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
     * The Mqtt message listener. This is important to handle some critical incoming message and provide the
     * suitable response message to make sure that the communication flow continuously
     */
    private val mqttCallback = object : MqttCallback {
        override fun messageArrived(topic: String?, message: MqttMessage?) {

            topic?.let {
                _messageArriveListener.value = arrayOf(topic, message.toString())
                LoggerManager.instance.publish("Receive topic $topic - ${message.toString()}")

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
                                    handleActionSet(moshi, msg)
                                } else if (model.request == "config") {
                                    handleActionConfig(moshi, msg)
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
     * Handle the message from ExoHome cloud that the topic is "$resource/action" and the request is equal
     * to "config", then, we have to reply with topic "$fields" accordingly
     */
    private fun handleActionConfig(moshi: Moshi, msg: MqttMessage) {
        val sadapter = ActionConfigModelJsonAdapter(moshi)
        val data = sadapter.fromJson(msg.toString())
        if (data != null) {
            val command = FieldsModel(data.data.fields).createResourceCommand()
            deviceClient?.publish(command, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    LoggerManager.instance.publish("Send topic $${command.name} - ${command.message}")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    LoggerManager.instance.publish("Send topic $${command.name} failed")
                }

            })
        }
    }

    /**
     * Handle the message from ExoHome cloud that the topic is "$resource/action" and the request is equal
     * to "set", then, we have to reply accordingly as follows
     * 1. publish to "$resource/action" with empty string
     * 2. execute action
     * 3. publish to "$resource/result" with result payload
     */
    private fun handleActionSet(moshi: Moshi, msg: MqttMessage) {
        val sadapter = ActionSetModelJsonAdapter(moshi)
        val data = sadapter.fromJson(msg.toString())
        if (data != null) {

            // simulate the executing time
            object :CountDownTimer(2000, 2000){
                override fun onFinish() {
                    // change the state after executing actions
                    sendStateValue(data.data)



                }

                override fun onTick(millisUntilFinished: Long) {

                }

            }.start()


            object :CountDownTimer(3000, 3000){
                override fun onFinish() {
                    // send success response
                    val command =
                        ActionSuccessResponseModel(id = data.id).createResourceCommand()

                    deviceClient?.publish(command, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            LoggerManager.instance.publish("Send topic $${command.name} - ${command.message}")
                        }

                        override fun onFailure(
                            asyncActionToken: IMqttToken?,
                            exception: Throwable?
                        ) {
                            LoggerManager.instance.publish("Send topic $${command.name} failed")
                        }

                    })
                }

                override fun onTick(millisUntilFinished: Long) {
                }

            }.start()

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

    fun sendStateValue(name: String, value: Any) {
        if(deviceClient != null && deviceClient!!.isConnected()) {
            val map = HashMap<String, Any>()
            map[name] = value
            val command = StatesModel(map).createResourceCommand()
            deviceClient?.publish(
                command, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        LoggerManager.instance.publish("Send topic $${command.name} - ${command.message}")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    }

                }
            )
        }
    }

    fun sendStateValue(map: Map<String, Any>) {
        val command = StatesModel(map).createResourceCommand()
        deviceClient?.publish(
            command, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    LoggerManager.instance.publish("Send topic $${command.name} - ${command.message}")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                }
            }
        )
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
            val command = ProvisionCommand(it)
            deviceClient?.publish(
                command,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        LoggerManager.instance.publish("Send topic $${command.topic()} - ${command.message()} failed")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        LoggerManager.instance.publish("Send topic $${command.topic()} failed")
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
                LoggerManager.instance.publish("Send topic $${esh.name()} - ${esh.message()}")
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
                    LoggerManager.instance.publish("Send topic $${module.name()} - ${module.message()}")
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
                LoggerManager.instance.publish("Send topic $${cert.name()} - ${cert.message()}")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            }
        })

        // ota
        val ota = OtaModel("idle")
        deviceClient?.publish(ota.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                LoggerManager.instance.publish("Send topic $${ota.name()} - ${ota.message()}")
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
                LoggerManager.instance.publish("Send topic $${schedules.name()} - ${schedules.message()}")
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
            put("H06", false)
        }

        val statesModel = StatesModel(states)
        deviceClient?.publish(statesModel.createResourceCommand(), object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                LoggerManager.instance.publish("Send topic $${statesModel.name()} - ${statesModel.message()}")
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
                    LoggerManager.instance.publish("Send topic $${tokenModel.name()} - ${tokenModel.message()}")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {

                }
            })
        }
    }
}