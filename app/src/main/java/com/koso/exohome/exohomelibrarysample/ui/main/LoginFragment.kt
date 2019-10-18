package com.koso.exohome.exohomelibrarysample.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.koso.exohome.exohomelibrarysample.R
import com.koso.exohome.exohomelibrarysample.api.SessionResponseJsonAdapter
import com.koso.exohome.exohomelibrarysample.api.WebSocketResponseJsonAdapter
import com.koso.exohome.exohomelibrarysample.utils.SharedPrefHandler
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.fragment_login.*
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


/**
 * A simple [Fragment] subclass.
 * To demonstrate the phone side login
 */
class LoginFragment : Fragment() {

    /**
     * Dealing with the HTTP request
     */
    val okHttpClient = OkHttpClient()

    /**
     * Used to handle JSON parsing
     */
    val moshi = Moshi.Builder().build()

    var webSocketClient : WebSocketClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {

        vEmail.setText(SharedPrefHandler.getEmail(context!!))
        vPw.setText(SharedPrefHandler.getPassword(context!!))
        vToken.setText(SharedPrefHandler.getSessionToken(context!!))
        vProvisionToken.setText(SharedPrefHandler.getOwnerProvisionToken(context!!))

        vLogin.setOnClickListener {
            if (validateData()) {
                SharedPrefHandler.setEmail(context!!, vEmail.text.toString())
                SharedPrefHandler.setPassword(context!!, vPw.text.toString())
                val body =
                    """{"password":"${vPw.text}","email": "${vEmail.text}"}"""


                GlobalScope.launch {
                    val response = okHttpClient.newCall(
                        Request.Builder().url("https://koso.apps.exosite.io/api:1/session")
                            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                            .build()
                    ).execute()
                    withContext(Dispatchers.Main){
                        val json = response.body!!.string()
                        val r = SessionResponseJsonAdapter(moshi).fromJson(json)
                        r?.let{
                            vToken.setText(it.token)

                            handleSessionTokenAvailable(it.token)
                        }
                    }
                }
            }
        }
    }

    private fun handleSessionTokenAvailable(token: String) {
        SharedPrefHandler.setSessionToken(context!!, token)
        webSocketClient = object: WebSocketClient(URI.create("wss://koso.apps.exosite.io/api:1/phone")){
            override fun onOpen(handshakedata: ServerHandshake?) {
                val request = """{"id":1, "request":"login", "data":{"token":"$token"}}"""
                webSocketClient?.send(request)
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            override fun onMessage(message: String?) {
                val response = WebSocketResponseJsonAdapter(moshi).fromJson(message)
                response?.let {
                    if(it.response == "login" && it.status == "ok"){
                        handleLogedIn()
                    }else if(it.response == "provision_token" && it.status == "ok"){
                        response.data?.let {
                            handleProvisionTokenAvailable(it.token)
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
        SharedPrefHandler.setOwnerProvisionToken(context!!, token)
        vProvisionToken.setText(token)
        Toast.makeText(context!!, "Gained provision token!", Toast.LENGTH_SHORT).show()
    }

    private fun handleLogedIn() {
        val request = """{"id": 2, "request": "provision_token", "data":{"expires_in": 2592000}}"""
        webSocketClient?.send(request)
    }

    private fun validateData(): Boolean {
        vEmail.setError(null)
        if (vEmail.text.toString().isEmpty()) {
            vEmail.setError("enter email")
            return false
        }

        if (vPw.text.toString().isEmpty()) {
            vPw.setError("enter password")
            return false
        }

        return true
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            LoginFragment()
    }
}
