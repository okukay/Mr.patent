package com.ssafy.mr_patent_android.util

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.util.Log
import android.widget.Toast
import kotlin.math.log

private const val TAG = "ConnectionStateMonitor"
class ConnectionStateMonitor(val activity: Activity, val onConnected: () -> Unit, val onDisconnected: () -> Unit) : ConnectivityManager.NetworkCallback() {

    private val networkRequest: NetworkRequest = NetworkRequest.Builder().build()

    init {
        registerConnectionMonitor(activity)
    }

    private fun registerConnectionMonitor(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerNetworkCallback(networkRequest, this)
    }

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        activity.runOnUiThread {
            onConnected()
        }
    }
    override fun onLost(network: Network) {
        super.onLost(network)
        activity.runOnUiThread {
            onDisconnected()
        }
    }
}