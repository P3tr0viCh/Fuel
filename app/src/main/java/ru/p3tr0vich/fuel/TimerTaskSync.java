package ru.p3tr0vich.fuel;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import java.util.TimerTask;

class TimerTaskSync extends TimerTask {

    private static final String ACTION = BuildConfig.APPLICATION_ID + ".ACTION_START_SYNC";
    private static final String EXTRA_SYNC_DATABASE = BuildConfig.APPLICATION_ID + ".EXTRA_SYNC_DATABASE";
    private static final String EXTRA_SYNC_PREFERENCES = BuildConfig.APPLICATION_ID + ".EXTRA_SYNC_PREFERENCES";

    private final boolean mSyncDatabase;
    private final boolean mSyncPreferences;

    @NonNull
    public static IntentFilter getIntentFilter() {
        return new IntentFilter(ACTION);
    }

    public static boolean isSyncDatabase(@NonNull Intent intent) {
        return intent.getBooleanExtra(EXTRA_SYNC_DATABASE, true);
    }

    public static boolean isSyncPreferences(@NonNull Intent intent) {
        return intent.getBooleanExtra(EXTRA_SYNC_PREFERENCES, true);
    }

    public TimerTaskSync(final boolean syncDatabase, final boolean syncPreferences) {
        super();
        mSyncDatabase = syncDatabase;
        mSyncPreferences = syncPreferences;
    }

    @Override
    public void run() {
        LocalBroadcastManager.getInstance(ApplicationFuel.getContext())
                .sendBroadcast(new Intent(ACTION)
                        .putExtra(EXTRA_SYNC_DATABASE, mSyncDatabase)
                        .putExtra(EXTRA_SYNC_PREFERENCES, mSyncPreferences));
    }
}