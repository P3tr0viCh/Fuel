package ru.p3tr0vich.fuel.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

public class UtilsLog {

    private static final String LOG_TAG = "XXX";

    public static void d(@Nullable String tag, @NonNull String msg, @Nullable String extMsg) {
        if (!TextUtils.isEmpty(tag)) msg = tag + " -- " + msg;

        if (!TextUtils.isEmpty(extMsg)) msg = msg + ": " + extMsg;

        Log.d(LOG_TAG, msg);
    }

    public static void d(@NonNull String tag, @NonNull String msg) {
        d(tag, msg, null);
    }

    public static void d(@NonNull Object o, @NonNull String msg, @Nullable String extMsg) {
        d(o.getClass(), msg, extMsg);
    }

    public static void d(@NonNull Object o, @NonNull String msg) {
        d(o, msg, null);
    }

    public static void d(@NonNull Class aClass, @NonNull String msg, @Nullable String extMsg) {
        d(aClass.getSimpleName(), msg, extMsg);
    }
}