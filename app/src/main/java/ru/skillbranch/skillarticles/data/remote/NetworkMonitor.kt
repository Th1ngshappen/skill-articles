package ru.skillbranch.skillarticles.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData

// 14: 01:48:43 важный момент: мы не можем инжектить в приватное поле
// (только если это приватное поле конструктора, тогда можем),
// inject может происходить только в публичные открытые свойства и,
// соответственно только в публичные методы
// если вы хотите, чтобы провайды были видны только в каком-то одном модуле,
// просто не наследуйтесь от этого модуля либо использовать dependency подход

// позволяет определять, есть сеть или нет,
// а также определять тип сети и наличие соединения в виде LiveData
class NetworkMonitor(val context: Context) {
    var isConnected: Boolean = false
    val isConnectedLive = MutableLiveData(false)
    val networkTypeLive = MutableLiveData(NetworkType.NONE)

    private val cm: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun registerNetworkMonitor() {

        obtainNetworkType(cm.activeNetwork?.let { cm.getNetworkCapabilities(it) })
            // именно postValue, потому что это будет не главный поток
            .also { networkTypeLive.postValue(it) }

        cm.registerNetworkCallback(
            NetworkRequest.Builder().build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    networkTypeLive.postValue(obtainNetworkType(networkCapabilities))
                }

                override fun onLost(network: Network) {
                    isConnected = false
                    isConnectedLive.postValue(false)
                    networkTypeLive.postValue(NetworkType.NONE)
                }

                override fun onAvailable(network: Network) {
                    isConnected = true
                    isConnectedLive.postValue(true)
                }
            }
        )
    }

    private fun obtainNetworkType(networkCapabilities: NetworkCapabilities?): NetworkType = when {
        networkCapabilities == null -> NetworkType.NONE
        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
        else -> NetworkType.UNKNOWN
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setNetworkIsConnected(isConnected: Boolean = true) {
        this.isConnected = isConnected
    }
}

enum class NetworkType {
    NONE, UNKNOWN, WIFI, CELLULAR
}