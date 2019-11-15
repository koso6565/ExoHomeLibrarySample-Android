package com.koso.kosoproxy.ui.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.koso.exohome.exohomelibrarysample.api.SessionResponseJsonAdapter
import com.koso.exohome.exohomelibrarysample.api.WebSocketResponseJsonAdapter
import com.koso.exohome.exohomelibrarysample.utils.SharedPrefHandler
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class LoginViewModel : ViewModel() {

    /**
     * URL for gaining session token
     */
    val SESSION_URL = "https://koso.apps.exosite.io/api:1/session"

    /**
     * Dealing with the HTTP request
     */
    val okHttpClient = OkHttpClient()

    /**
     * Used to handle JSON parsing
     */
    val moshi = Moshi.Builder().build()

    /**
     * For querying the owner token
     */
    var webSocketClient: WebSocketClient? = null

    /**
     * The inner login state live data which is mutable
     */
    private val _loginStateLiveData = MutableLiveData<LoginState>(
        if(SharedPrefHandler.getOwnerProvisionToken().isEmpty())
            LoginState.Unlogin
        else
            LoginState.GotOwnerProvisionToken
    )

    /**
     * The outer accessible login state liveData
     */
    val loginStateLiveData = _loginStateLiveData

    enum class LoginState {
        Unlogin, GainingSessionToken, LoggingIn, GettingOwnerProvissionToken, GotOwnerProvisionToken
    }

    fun login(account: String, password: String) {
        SharedPrefHandler.setEmail(account)
        SharedPrefHandler.setPassword(password)
        val body = """{"password":"$password","email": "$account"}"""
        GlobalScope.launch {
            _loginStateLiveData.postValue(LoginState.GainingSessionToken)
            val response = okHttpClient.newCall(
                Request.Builder().url(SESSION_URL)
                    .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                    .build()
            ).execute()

            withContext(Dispatchers.Main) {
                val json = response.body!!.string()
                val r = SessionResponseJsonAdapter(moshi).fromJson(json)
                r?.let {
                    handleSessionTokenAvailable(it.token)
                }
            }
        }
    }

    private fun handleSessionTokenAvailable(token: String) {
        webSocketClient =
            object : WebSocketClient(URI.create("wss://koso.apps.exosite.io/api:1/phone")) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    _loginStateLiveData.postValue(LoginState.LoggingIn)
                    val request = """{"id":1, "request":"login", "data":{"token":"$token"}}"""
                    webSocketClient?.send(request)
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {

                }

                override fun onMessage(message: String?) {
                    val response = WebSocketResponseJsonAdapter(moshi).fromJson(message)
                    response?.let {
                        if (it.response == "login" && it.status == "ok") {
                            handleLogedIn()
                        } else if (it.response == "provision_token" && it.status == "ok") {
                            response.data?.let { data ->
                                handleProvisionTokenAvailable(data.token)
                            }
                        }
                    }
                }

                override fun onError(ex: Exception?) {

                }
            }
        webSocketClient!!.connect()
    }


    private fun handleProvisionTokenAvailable(token: String) {

        _loginStateLiveData.postValue(LoginState.GotOwnerProvisionToken)
        SharedPrefHandler.setOwnerProvisionToken(token)
    }

    private fun handleLogedIn() {
        _loginStateLiveData.postValue(LoginState.GettingOwnerProvissionToken)
        //todo fix the expires_in parameter

        val request = """{"id": 2, "request": "provision_token", "data":{"expires_in": 2592000}}"""
        webSocketClient?.send(request)
    }

}