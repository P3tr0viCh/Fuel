package ru.p3tr0vich.fuel;

import android.support.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

import ru.p3tr0vich.fuel.utils.UtilsLog;

class TimerSync extends Timer {

    private static final String TAG = "TimerSync";

    private static final long DELAY = 10000;

    private TimerSync() {
        super();
    }

    public static TimerSync start(final boolean databaseChanged, final boolean preferencesChanged) {
        UtilsLog.d(TAG, "start",
                "databaseChanged == " + databaseChanged + ", preferencesChanged == " + preferencesChanged);

        TimerSync timerSync = new TimerSync();
        timerSync.schedule(new TimerTaskSync(databaseChanged, preferencesChanged), DELAY);

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

    static class TimerTaskSync extends TimerTask {

        private final boolean mDatabaseChanged;
        private final boolean mPreferencesChanged;

        public TimerTaskSync(final boolean databaseChanged, final boolean preferencesChanged) {
            super();
            mDatabaseChanged = databaseChanged;
            mPreferencesChanged = preferencesChanged;
        }

        @Override
        public void run() {
            BroadcastReceiverSync.send(ApplicationFuel.getContext(),
                    mDatabaseChanged, mPreferencesChanged, false);
        }
    }
}