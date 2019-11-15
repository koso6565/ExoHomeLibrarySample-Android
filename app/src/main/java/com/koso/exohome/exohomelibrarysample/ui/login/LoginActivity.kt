package com.koso.exohome.exohomelibrarysample.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.koso.exohome.exohomelibrarysample.R
import com.koso.exohome.exohomelibrarysample.ui.exoconnect.DeviceRegisterActivity
import com.koso.exohome.exohomelibrarysample.utils.SharedPrefHandler
import com.koso.kosoproxy.ui.login.LoginViewModel
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {


    var model: LoginViewModel? = null

    companion object{
        fun launch(context: Context){
            context.startActivity(Intent(context, LoginActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        model = ViewModelProviders.of(this)[LoginViewModel::class.java]

        initViews()
        registerLoginState()
    }


    private fun registerLoginState() {
        model?.loginStateLiveData?.observe(this,
            Observer<LoginViewModel.LoginState> {
                when (it) {
                    LoginViewModel.LoginState.Unlogin -> {
                        vState.setText(R.string.unlogin)
                        vProgressBar.visibility = View.INVISIBLE
                    }
                    LoginViewModel.LoginState.GainingSessionToken -> {
                        vState.setText(R.string.gain_session_token)
                        vProgressBar.visibility = View.VISIBLE
                    }
                    LoginViewModel.LoginState.GettingOwnerProvissionToken -> {
                        vState.setText(R.string.gain_owner_provision_token)
                        vProgressBar.visibility = View.VISIBLE
                    }
                    LoginViewModel.LoginState.LoggingIn -> {
                        vState.setText(R.string.logging_in)
                        vProgressBar.visibility = View.VISIBLE
                    }
                    LoginViewModel.LoginState.GotOwnerProvisionToken -> {
                        vState.setText(R.string.got_owner_provision_token)
                        vProgressBar.visibility = View.INVISIBLE
                        DeviceRegisterActivity.launch(this)
                        finish()
                    }

                }
            })
    }


    private fun initViews() {
        vAccount.setText(SharedPrefHandler.getEmail())
        vPassword.setText(SharedPrefHandler.getPassword())
        vLogin.setOnClickListener {
            if (validateData()) {
                model?.login(vAccount.text.toString(), vPassword.text.toString())
            }
        }
    }

    private fun validateData(): Boolean {
        vAccount.setError(null)
        if (vAccount.text.toString().isEmpty()) {
            vAccount.error = getString(R.string.invalidate_data)
            return false
        }

        if (vPassword.text.toString().isEmpty()) {
            vPassword.setError("enter password")
            return false
        }

        return true
    }
}
