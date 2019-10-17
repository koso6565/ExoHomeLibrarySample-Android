package com.koso.exohome.exohomelibrarysample.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.koso.exohome.exohomelibrarysample.R
import com.koso.exohome.exohomelibrarysample.api.SessionResponseJsonAdapter
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
        vToken.setText(SharedPrefHandler.getToken(context!!))

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
                            SharedPrefHandler.setToken(context!!, it.token)
                        }
                    }
                }
            }
        }
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
