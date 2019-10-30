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

        fun registerListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener){
            getSharedPrefences(context).registerOnSharedPreferenceChangeListener(listener)
        }

        fun unregisterListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener){
            getSharedPrefences(context).unregisterOnSharedPreferenceChangeListener(listener)
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

        fun setDeviceToken(context: Context, token: String) {
            getEditor(context).putString("device_token", token).apply()
        }

        fun getDeviceToken(context: Context): String{
            return getSharedPrefences(context).getString("device_token", "")!!
        }

        // Setter of owner(phone) provision token
        fun setOwnerProvisionToken(context: Context, token: String) {
            getEditor(context).putString("owner_provision_token", token).apply()
        }

        // Getter of owner(phone) provision token
        fun getOwnerProvisionToken(context: Context): String {
            return getSharedPrefences(context).getString("owner_provision_token", "")!!
        }
    }
}