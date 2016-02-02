package ru.p3tr0vich.fuel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.util.Calendar;

public class UtilsFormat {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    public static String dateTimeToString(final long date, final boolean withYear, final boolean abbrevMonth) {
        int flags = DateUtils.FORMAT_SHOW_DATE;
        flags |= withYear ? DateUtils.FORMAT_SHOW_YEAR : DateUtils.FORMAT_NO_YEAR;
        if (abbrevMonth) flags |= DateUtils.FORMAT_ABBREV_MONTH;

        flags |= DateUtils.FORMAT_SHOW_TIME; // TODO: remove

        return DateUtils.formatDateTime(ApplicationFuel.getContext(), date, flags);
    }

    public static String dateToString(final long date, final boolean withYear) {
        return dateTimeToString(date, withYear, false);
    }

    @NonNull
    public static String getRelativeDateTime(@NonNull Context context, final long dateTime) {
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
                final Calendar calendarNow = Calendar.getInstance();
                final Calendar calendarDateTime = Calendar.getInstance();
                calendarDateTime.setTimeInMillis(dateTime);

                final int daysBetween = Math.abs(
                        calendarNow.get(Calendar.DAY_OF_YEAR) -
                                calendarDateTime.get(Calendar.DAY_OF_YEAR));

                if (calendarNow.get(Calendar.YEAR) == calendarDateTime.get(Calendar.YEAR)) {
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

    public static float stringToFloat(final String value) {
        if (TextUtils.isEmpty(value)) return 0;
        try {
            return Float.parseFloat(value);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String floatToString(final float value, final boolean showZero) {
        // TODO: LOCALE?
        final String strValue = DECIMAL_FORMAT.format(value).replace(',', '.');
        return showZero ? strValue : value == 0 ? "" : strValue;
    }

    public static String floatToString(final float value) {
        return floatToString(value, true);
    }

    public static float editTextToFloat(final EditText edit) {
        return stringToFloat(edit.getText().toString());
    }

    public static void floatToEditText(final EditText edit, float value, boolean showZero) {
        edit.setText(floatToString(value, showZero));
    }
}