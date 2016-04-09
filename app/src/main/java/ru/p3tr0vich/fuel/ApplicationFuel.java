package ru.p3tr0vich.fuel;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

public class ApplicationFuel extends Application {

    private static Context CONTEXT;

    @Override
    public void onCreate() {
        super.onCreate();
        CONTEXT = getApplicationContext();
        PreferencesHelper.init(CONTEXT);
    }

    @NonNull
    public static Context getContext() {
        if (CONTEXT == null) throw new AssertionError("Application context is null");
        return CONTEXT;
    }
}