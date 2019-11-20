package com.koso.exohome.exohomelibrarysample.mgr

import androidx.lifecycle.MutableLiveData
import java.util.*

class LoggerManager {

    companion object{
        val instance = LoggerManager()
    }

    val _messageListener = MutableLiveData<Array<String>>()

    interface LoggerListener{
        fun onMessage(msg: String, time: String)
    }



    fun publish(msg: String){


        val cal = Calendar.getInstance()
        val time = "${cal.get(Calendar.HOUR)}:${cal.get(Calendar.MINUTE)}:${cal.get(Calendar.SECOND)}"

        _messageListener.value = arrayOf(time, msg)
    }
}