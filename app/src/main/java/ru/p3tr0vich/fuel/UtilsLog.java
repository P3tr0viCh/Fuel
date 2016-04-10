package ru.p3tr0vich.fuel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

class UtilsLog {

    private static final String LOG_TAG = "XXX";

    public static void d(@Nullable String aClass, @NonNull String msg, @Nullable String extMsg) {
        if (!TextUtils.isEmpty(aClass)) msg = aClass + " -- " + msg;

        if (!TextUtils.isEmpty(extMsg)) msg = msg + ": " + extMsg;

        Log.d(LOG_TAG, msg);
    }

    public static void d(@NonNull String aClass, @NonNull String msg) {
        d(aClass, msg, null);
    }

    public static void d(@NonNull Object o, @NonNull String msg) {
        d(o.getClass().getSimpleName(), msg, null);
    }
}
