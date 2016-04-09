package ru.p3tr0vich.fuel;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

abstract class BroadcastReceiverSync extends BroadcastReceiverLocalBase {

    private static final String ACTION = BuildConfig.APPLICATION_ID + ".ACTION_START_SYNC";
    private static final String EXTRA_SYNC_DATABASE = BuildConfig.APPLICATION_ID + ".EXTRA_SYNC_DATABASE";
    private static final String EXTRA_SYNC_PREFERENCES = BuildConfig.APPLICATION_ID + ".EXTRA_SYNC_PREFERENCES";
    private static final String EXTRA_TOKEN_CHANGED = BuildConfig.APPLICATION_ID + ".EXTRA_TOKEN_CHANGED";

    @Override
    protected final String getAction() {
        return ACTION;
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
    public final void onReceive(Context context, Intent intent) {
        onReceive(
                intent.getBooleanExtra(EXTRA_SYNC_DATABASE, true),
                intent.getBooleanExtra(EXTRA_SYNC_PREFERENCES, true),
                intent.getBooleanExtra(EXTRA_TOKEN_CHANGED, true));
    }

    public abstract void onReceive(boolean syncDatabase, boolean syncPreferences, boolean tokenChange);
}