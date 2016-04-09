package ru.p3tr0vich.fuel;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

abstract class BroadcastReceiverLoading extends BroadcastReceiverLocalBase {

    private static final String ACTION = BuildConfig.APPLICATION_ID + ".ACTION_LOADING";
    private static final String EXTRA_LOADING = BuildConfig.APPLICATION_ID + ".EXTRA_LOADING";

    @Override
    protected final String getAction() {
        return ACTION;
    }

    public static void send(@NonNull Context context,
                            boolean loading) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION)
                .putExtra(EXTRA_LOADING, loading));
    }

    @Override
    public final void onReceive(Context context, Intent intent) {
        onReceive(intent.getBooleanExtra(EXTRA_LOADING, false));
    }

    public abstract void onReceive(boolean loading);
}