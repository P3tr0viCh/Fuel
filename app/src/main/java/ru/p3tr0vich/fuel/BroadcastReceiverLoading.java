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

    public static void register(@NonNull Context context, @NonNull BroadcastReceiverLoading broadcastReceiverSync) {
        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiverSync, new IntentFilter(ACTION));
    }

    public static void send(@NonNull Context context,
                            boolean loading) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION)
                .putExtra(EXTRA_LOADING, loading));
    }

    @Override
    @Deprecated
    public void onReceive(Context context, Intent intent) {
        onReceive(context, intent.getBooleanExtra(EXTRA_LOADING, false));
    }

    public abstract void onReceive(Context context, boolean loading);
}