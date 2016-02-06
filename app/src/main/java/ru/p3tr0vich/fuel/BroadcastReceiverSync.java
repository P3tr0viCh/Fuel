package ru.p3tr0vich.fuel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

abstract class BroadcastReceiverSync extends BroadcastReceiver {

    private static final String ACTION = BuildConfig.APPLICATION_ID + ".ACTION_START_SYNC";
    private static final String EXTRA_SYNC_DATABASE = BuildConfig.APPLICATION_ID + ".EXTRA_SYNC_DATABASE";
    private static final String EXTRA_SYNC_PREFERENCES = BuildConfig.APPLICATION_ID + ".EXTRA_SYNC_PREFERENCES";
    private static final String EXTRA_TOKEN_CHANGED = BuildConfig.APPLICATION_ID + ".EXTRA_TOKEN_CHANGED";

    public static void register(@NonNull Context context, @NonNull BroadcastReceiverSync broadcastReceiverSync) {
        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiverSync, new IntentFilter(ACTION));
    }

    public static void send(@NonNull Context context,
                            boolean syncDatabase,
                            boolean syncPreferences,
                            boolean tokenChanged) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION)
                        .putExtra(EXTRA_SYNC_DATABASE, syncDatabase)
                        .putExtra(EXTRA_SYNC_PREFERENCES, syncPreferences)
                        .putExtra(EXTRA_TOKEN_CHANGED, tokenChanged));
    }

    @Override
    @Deprecated
    public void onReceive(Context context, Intent intent) {
        onReceive(
                intent.getBooleanExtra(EXTRA_SYNC_DATABASE, true),
                intent.getBooleanExtra(EXTRA_SYNC_PREFERENCES, true),
                intent.getBooleanExtra(EXTRA_TOKEN_CHANGED, true));
    }

    public abstract void onReceive(boolean syncDatabase, boolean syncPreferences, boolean tokenChange);
}