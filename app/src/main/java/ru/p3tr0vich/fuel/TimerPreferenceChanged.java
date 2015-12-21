package ru.p3tr0vich.fuel;

import java.util.Timer;

class TimerPreferenceChanged extends Timer {

    private static final long DELAY = 10000;

    private TimerPreferenceChanged() {
        super();
    }

    public static TimerPreferenceChanged start() {
        Functions.logD("TimerPreferenceChanged -- start");

        TimerPreferenceChanged timerPreferenceChanged = new TimerPreferenceChanged();
        timerPreferenceChanged.schedule(new TimerTaskPreferenceChanged(), DELAY);
        return timerPreferenceChanged;
    }
}
