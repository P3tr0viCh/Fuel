package ru.p3tr0vich.fuel;

import java.util.TimerTask;

class TimerTaskPreferenceChanged extends TimerTask {
    @Override
    public void run() {
        ActivityMain.sendStartSyncBroadcast(ActivityMain.START_SYNC_PREFERENCES_CHANGED);
    }
}