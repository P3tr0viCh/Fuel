package ru.p3tr0vich.fuel;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class ConnectivityHelper {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DISCONNECTED, CONNECTED, CONNECTED_WIFI, CONNECTED_ROAMING})
    public @interface ConnectedState {
    }

    public static final int DISCONNECTED = 0;
    public static final int CONNECTED = 1;
    public static final int CONNECTED_WIFI = 2;
    public static final int CONNECTED_ROAMING = 3;

    @ConnectedState
    public static int getConnectedState(@NonNull Context context) {
        ConnectivityManager connectivityManager =
                SystemServicesHelper.getConnectivityManager(context);

        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                if (activeNetworkInfo.isRoaming())
                    return CONNECTED_ROAMING;

                if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI)
                    return CONNECTED_WIFI;

                return CONNECTED;
            }
        }

        return DISCONNECTED;
    }
}