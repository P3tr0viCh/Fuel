package ru.p3tr0vich.fuel;

import android.support.annotation.NonNull;

import java.util.Calendar;
import java.util.TimeZone;

class UtilsDate {

    public static int getCurrentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    public static long utcToLocal(long utc) {
        return utc + TimeZone.getDefault().getOffset(utc);
    }

    public static long localToUtc(long local) {
        return local - TimeZone.getDefault().getOffset(local);
    }

    public static void setStartOfDay(@NonNull Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    public static void setEndOfDay(@NonNull Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
    }
}