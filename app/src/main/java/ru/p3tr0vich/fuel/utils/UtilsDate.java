package ru.p3tr0vich.fuel.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.util.Calendar;
import java.util.TimeZone;

import ru.p3tr0vich.fuel.R;

public class UtilsDate {

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

    @NonNull
    public static Calendar getCalendarInstance(long milliseconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        return calendar;
    }

    @NonNull
    public static String getMonthName(@NonNull Context context, @NonNull Calendar calendar,
                                int month, boolean abbrev) {
        calendar.set(Calendar.MONTH, month);
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR | DateUtils.FORMAT_NO_MONTH_DAY;
        if (abbrev) flags |= DateUtils.FORMAT_ABBREV_MONTH;

        return DateUtils.formatDateTime(context, calendar.getTimeInMillis(), flags);
    }

    @NonNull
    public static String getRelativeDateTime(@NonNull Context context, long dateTime) {
        final long now = System.currentTimeMillis();
        final long elapsed = now - dateTime;

        String result = null;

        if (elapsed > 0) {
            if (elapsed < 10 * DateUtils.SECOND_IN_MILLIS)
                result = context.getString(R.string.relative_date_time_now);
            else if (elapsed < DateUtils.MINUTE_IN_MILLIS)
                result = context.getString(R.string.relative_date_time_minute);
            else if (elapsed < DateUtils.HOUR_IN_MILLIS)
                result = DateUtils.getRelativeTimeSpanString(
                        dateTime, now,
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_SHOW_DATE).toString();
            else {
                final Calendar calendarNow = getCalendarInstance(now);
                final Calendar calendarDateTime = getCalendarInstance(dateTime);

                if (calendarNow.get(Calendar.YEAR) == calendarDateTime.get(Calendar.YEAR)) {
                    final int daysBetween = Math.abs(
                            calendarNow.get(Calendar.DAY_OF_YEAR) -
                                    calendarDateTime.get(Calendar.DAY_OF_YEAR));

                    if (daysBetween == 0)
                        result = context.getString(R.string.relative_date_time_today);
                    else if (daysBetween == 1)
                        result = context.getString(R.string.relative_date_time_yesterday);
                    else if (daysBetween < 7)
                        result = DateUtils.getRelativeTimeSpanString(
                                dateTime, now,
                                DateUtils.DAY_IN_MILLIS,
                                DateUtils.FORMAT_SHOW_DATE).toString();
                }
            }
        }

        if (TextUtils.isEmpty(result))
            result = DateUtils.formatDateTime(context, dateTime, DateUtils.FORMAT_SHOW_DATE);

        if (elapsed > DateUtils.MINUTE_IN_MILLIS && elapsed < DateUtils.WEEK_IN_MILLIS)
            result += ", " +
                    DateUtils.getRelativeTimeSpanString(context, dateTime, true).toString();

        return result;
    }
}