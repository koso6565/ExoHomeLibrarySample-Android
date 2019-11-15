package com.koso.kosoproxy.ui.exoconnect

import androidx.lifecycle.ViewModel
import com.koso.exohome.exohomelibrarysample.mgr.ExoHomeConnectionManager

class DeviceRegisterViewModel : ViewModel() {

    val connectStateLiveData = ExoHomeConnectionManager.instance.connectStateLiveData

    fun connect(productId: String, deviceId: String){
        ExoHomeConnectionManager.instance.connect(productId, deviceId)
    }


}


