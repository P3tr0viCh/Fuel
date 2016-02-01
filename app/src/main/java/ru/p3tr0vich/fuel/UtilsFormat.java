package ru.p3tr0vich.fuel;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.widget.EditText;

import java.text.DecimalFormat;

public class UtilsFormat {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    private static String formatDateTime(final long date, final int flags) {
        return DateUtils.formatDateTime(ApplicationFuel.getContext(), date, flags);
    }

    public static String dateTimeToString(final long date, final boolean withYear, final boolean abbrevMonth) {
        int flags = DateUtils.FORMAT_SHOW_DATE;
        flags |= withYear ? DateUtils.FORMAT_SHOW_YEAR : DateUtils.FORMAT_NO_YEAR;
        if (abbrevMonth) flags |= DateUtils.FORMAT_ABBREV_MONTH;

        flags |= DateUtils.FORMAT_SHOW_TIME;

        return formatDateTime(date, flags);
    }

    public static String dateToString(final long date, final boolean withYear) {
        return dateTimeToString(date, withYear, false);
    }

    @NonNull
    public static String getRelativeDateTime(final long dateTime) {
        final long now = System.currentTimeMillis();
        final long elapsed = now - dateTime;
        String result;

        if (elapsed < (10 * DateUtils.SECOND_IN_MILLIS))
            result = ApplicationFuel.getContext().getString(R.string.relative_date_time_now);
        else if (elapsed < DateUtils.MINUTE_IN_MILLIS)
            result = ApplicationFuel.getContext().getString(R.string.relative_date_time_minute);
        else {
            result = DateUtils.getRelativeTimeSpanString(
                    dateTime, now,
                    elapsed < DateUtils.HOUR_IN_MILLIS ?
                            DateUtils.MINUTE_IN_MILLIS :
                            DateUtils.DAY_IN_MILLIS,
                    DateUtils.FORMAT_SHOW_DATE).toString();
            if (elapsed < DateUtils.WEEK_IN_MILLIS)
                result = result + ", " +
                        DateUtils.getRelativeTimeSpanString(
                                ApplicationFuel.getContext(),
                                dateTime,
                                true).toString();
        }

        return result; // TODO locale .toLowerCase();
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
        String strValue = DECIMAL_FORMAT.format(value).replace(',', '.');
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