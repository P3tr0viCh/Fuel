package ru.p3tr0vich.fuel;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

public abstract class BroadcastReceiverDatabaseChanged extends BroadcastReceiverLocalBase {

    private static final String ACTION = BuildConfig.APPLICATION_ID + ".ACTION_DATABASE_CHANGED";
    private static final String EXTRA_ID = BuildConfig.APPLICATION_ID + ".EXTRA_ID";

    @Override
    protected final String getAction() {
        return ACTION;
    }

    public static void send(@NonNull Context context, long id) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION)
                .putExtra(EXTRA_ID, id));
    }

    @Override
    public final void onReceive(Context context, Intent intent) {
        onReceive(intent.getLongExtra(EXTRA_ID, -1));
    }

    public abstract void onReceive(long id);
}