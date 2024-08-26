package ru.p3tr0vich.fuel.helpers

import android.content.Context
import android.net.NetworkCapabilities
import androidx.annotation.IntDef

object ConnectivityHelper {

    const val DISCONNECTED = 0
    const val CONNECTED = 1
    const val CONNECTED_WIFI = 2
    const val CONNECTED_ROAMING = 3

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(DISCONNECTED, CONNECTED, CONNECTED_WIFI, CONNECTED_ROAMING)
    annotation class ConnectedState

    @JvmStatic
    @ConnectedState
    fun getConnectedState(context: Context): Int {
        val connectivityManager = SystemServicesHelper.getConnectivityManager(context)
                ?: return DISCONNECTED

        val activeNetwork = connectivityManager.activeNetwork ?: return DISCONNECTED

        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                ?: return DISCONNECTED

        return when {
            !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING) -> CONNECTED_ROAMING
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> CONNECTED_WIFI
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> CONNECTED
            else -> DISCONNECTED
        }
    }
}