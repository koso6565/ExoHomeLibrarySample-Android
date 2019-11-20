package com.koso.exohome.exohomelibrarysample.utils

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.koso.exohome.exohomelibrarysample.App

class SharedPrefHandler {
    companion object{
        /**
         *  To get the default SharedPreferences
         */
        private fun getSharedPrefences(): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(App.instance)
        }

        /**
         *  To get the default Editor
         */
        private fun getEditor(): SharedPreferences.Editor {
            return getSharedPrefences().edit()
        }

        fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener){
            getSharedPrefences().registerOnSharedPreferenceChangeListener(listener)
        }

        fun unregisterListener( listener: SharedPreferences.OnSharedPreferenceChangeListener){
            getSharedPrefences().unregisterOnSharedPreferenceChangeListener(listener)
        }


        fun getDeviceId(): String {
            return try {
                getSharedPrefences().getString("deviceId", "")!!
            }catch (e: Exception){
                ""
            }
        }

        fun setDeviceId( deviceId: String){
            getEditor().putString("deviceId", deviceId).apply()
        }

        fun setEmail( email: String) {
            getEditor().putString("email", email).apply()
        }

        fun getEmail(): String?{
            return getSharedPrefences().getString("email", "")
        }

        fun setPassword( pw: String){
            getEditor().putString("password", pw).apply()

        }
        fun getPassword(): String?{
            return getSharedPrefences().getString("password", "")
        }

        fun setDeviceToken( token: String) {
            getEditor().putString("device_token", token).apply()
        }

        fun getDeviceToken(): String{
            return getSharedPrefences().getString("device_token", "")!!
        }

        // Setter of owner(phone) provision token
        fun setOwnerProvisionToken( token: String) {
            getEditor().putString("owner_provision_token", token).apply()
        }

        // Getter of owner(phone) provision token
        fun getOwnerProvisionToken(): String {
            return getSharedPrefences().getString("owner_provision_token", "")!!
        }

        fun setMockMacAddress(address: String){
            getEditor().putString("pref_mock_address", address).apply()
        }

        fun getMockMackAddress(): String{
            return getSharedPrefences().getString("pref_mock_address", "0a86dda0514b")!!
        }

        fun setProductId(id: String){
            getEditor().putString("pref_product_id", id).apply()
        }

        fun getProductId(): String{
            return getSharedPrefences().getString("pref_product_id", "l55gbjzaaytw00000")!!
        }

        fun clear() {
            getSharedPrefences().edit().clear().apply()
        }
    }
}