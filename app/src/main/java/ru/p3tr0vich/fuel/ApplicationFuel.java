package ru.p3tr0vich.fuel;

import android.app.Application;
import android.content.Context;

public class ApplicationFuel extends Application {

    private static Context CONTEXT;

    @Override
    public void onCreate() {
        super.onCreate();
        CONTEXT = getApplicationContext();
        PreferenceManagerFuel.init(CONTEXT);
    }

    public static Context getContext() {
        return CONTEXT;
    }
}
