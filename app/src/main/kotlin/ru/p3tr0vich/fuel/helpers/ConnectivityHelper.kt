package ru.p3tr0vich.fuel.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.support.annotation.IntDef

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

        val activeNetworkInfo = connectivityManager.activeNetworkInfo ?: return DISCONNECTED

        if (!activeNetworkInfo.isConnected) {
            return DISCONNECTED
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork ?: return DISCONNECTED

            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                    ?: return DISCONNECTED

            if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)) {
                return CONNECTED_ROAMING
            }

            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return CONNECTED_WIFI
            }

            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return CONNECTED
            }

            return DISCONNECTED
        } else {
            @Suppress("DEPRECATION")
            if (activeNetworkInfo.isRoaming) {
                return CONNECTED_ROAMING
            }

            @Suppress("DEPRECATION")
            if (activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI) {
                return CONNECTED_WIFI
            }

            return CONNECTED
        }
    }
}