package com.koso.exohome.exohomelibrarysample.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.koso.exohome.exohomelibrarysample.R
import com.koso.exohome.library.DeviceIdGenerator
import com.koso.exohome.library.ExoHomeClient
import kotlinx.android.synthetic.main.connect_fragment.*

class ConnectFragment : Fragment() {

    companion object {
        fun newInstance() = ConnectFragment()
    }

    private lateinit var viewModel: MainViewModel

    private lateinit var client: ExoHomeClient

    /**
     * ExoHomeClient device id
     */
    private var deviceId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(R.layout.connect_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {

        deviceId = DeviceIdGenerator.createId("0a86dda0514b")
        vDeviceId.setText(deviceId)

        vProvision.setOnClickListener {
            handleProvision()
        }

    }

    private fun handleProvision() {

        context?.let {
            client = ExoHomeClient(it, vProductId.text.toString(), vDeviceId.text.toString())
            client.connect()
        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

    }

}
