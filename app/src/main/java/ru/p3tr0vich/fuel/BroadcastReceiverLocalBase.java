package ru.p3tr0vich.fuel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

public abstract class BroadcastReceiverLocalBase extends BroadcastReceiver {

    protected abstract String getAction();

    public final void register(@NonNull Context context) {
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(getAction()));
    }

    public final void unregister(@NonNull Context context) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
    }
}