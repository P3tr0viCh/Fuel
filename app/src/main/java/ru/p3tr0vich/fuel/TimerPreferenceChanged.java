package ru.p3tr0vich.fuel;

import java.util.Timer;

class TimerPreferenceChanged extends Timer {

    private static final String TAG = "TimerPreferenceChanged";

    private static final long DELAY = 10000;

    private TimerPreferenceChanged() {
        super();
    }

    public static TimerPreferenceChanged start() {
        UtilsLog.d(TAG, "start");

        TimerPreferenceChanged timerPreferenceChanged = new TimerPreferenceChanged();
        timerPreferenceChanged.schedule(new TimerTaskPreferenceChanged(), DELAY);
        return timerPreferenceChanged;
    }

    @Override
    public void cancel() {
        UtilsLog.d(TAG, "cancel");
        super.cancel();
    }
}
