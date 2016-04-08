package ru.p3tr0vich.fuel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

abstract class BroadcastReceiverLoading extends BroadcastReceiver {

    private static final String ACTION = BuildConfig.APPLICATION_ID + ".ACTION_LOADING";
    private static final String EXTRA_LOADING = BuildConfig.APPLICATION_ID + ".EXTRA_LOADING";

    public void register(@NonNull Context context) {
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(ACTION));
    }

    public void unregister(@NonNull Context context) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
    }

    public static void send(@NonNull Context context,
                            boolean loading) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION)
                .putExtra(EXTRA_LOADING, loading));
    }

    @Override
    @Deprecated
    public void onReceive(Context context, Intent intent) {
        onReceive(intent.getBooleanExtra(EXTRA_LOADING, false));
    }

    public abstract void onReceive(boolean loading);
}