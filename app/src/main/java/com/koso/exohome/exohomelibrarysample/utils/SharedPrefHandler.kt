package com.koso.exohome.exohomelibrarysample.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class SharedPrefHandler {
    companion object{
        /**
         *  To get the default SharedPreferences
         */
        private fun getSharedPrefences(context: Context): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(context)
        }

        /**
         *  To get the default Editor
         */
        private fun getEditor(context: Context): SharedPreferences.Editor {
            return getSharedPrefences(
                context
            ).edit()
        }

        fun getToken(context: Context): String{
            return try {
                getSharedPrefences(
                    context
                ).getString("token", "")!!
            }catch (e: Exception){
                ""
            }
        }

        fun setToken(context: Context, token: String){
            getEditor(
                context
            ).putString("token", token).apply()
        }

        fun getDeviceId(context: Context): String {
            return try {
                getSharedPrefences(context).getString("deviceId", "")!!
            }catch (e: Exception){
                ""
            }
        }

        fun setDeviceId(context: Context, deviceId: String){
            getEditor(context).putString("deviceId", deviceId).apply()
        }

        fun setEmail(context:Context, email: String) {
            getEditor(context).putString("email", email).apply()
        }

        fun getEmail(context: Context): String?{
            return getSharedPrefences(context).getString("email", "")
        }

        fun setPassword(context: Context, pw: String){
            getEditor(context).putString("password", pw).apply()

        }
        fun getPassword(context: Context): String?{
            return getSharedPrefences(context).getString("password", "")
        }
    }
}