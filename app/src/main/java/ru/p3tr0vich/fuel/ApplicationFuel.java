package ru.p3tr0vich.fuel;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

public class ApplicationFuel extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
    }

    @NonNull
    public static Context getContext() {
        if (sContext == null) throw new AssertionError("Application context is null");
        return sContext;
    }
}