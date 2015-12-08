package ru.p3tr0vich.fuel;

import android.app.Application;

public class ApplicationFueling extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Functions.sApplicationContext = getApplicationContext();
        FuelingPreferenceManager.init(Functions.sApplicationContext);
    }
}
