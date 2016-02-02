package ru.p3tr0vich.fuel;

import android.support.annotation.Nullable;

import java.util.Timer;

class TimerSync extends Timer {

    private static final String TAG = "TimerSync";

    private static final long DELAY = 10000;

    private TimerSync() {
        super();
    }

    public static TimerSync start(final boolean syncDatabase, final boolean syncPreferences) {
        UtilsLog.d(TAG, "start",
                "syncDatabase == " + syncDatabase + ", syncPreferences == " + syncPreferences);

        TimerSync timerSync = new TimerSync();
        timerSync.schedule(new TimerTaskSync(syncDatabase, syncPreferences), DELAY);

        return timerSync;
    }

    public static void stop(@Nullable TimerSync timerSync) {
        if (timerSync != null) timerSync.cancel();
    }

    @Override
    public void cancel() {
        UtilsLog.d(TAG, "cancel");
        super.cancel();
    }
}