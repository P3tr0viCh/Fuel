package ru.p3tr0vich.fuel;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.widget.EditText;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UtilsFormat {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    private static final String SQL_DATE_FORMAT = "yyyy-MM-dd";
    private static final String SQL_TIME_FORMAT = "HH:mm:ss.SSS";
    private static final String SQL_DATE_TIME_FORMAT =
            SQL_DATE_FORMAT + ' ' + SQL_TIME_FORMAT;

    public static Date sqlDateToDate(String sqlDate) {
        Date date = null;

        DateFormat format = new SimpleDateFormat(SQL_DATE_FORMAT, Locale.getDefault());
        try {
            date = format.parse(sqlDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static Date sqlDateTimeToDate(String dateTime) {
        Date date;

        DateFormat format = new SimpleDateFormat(SQL_DATE_TIME_FORMAT, Locale.getDefault());
        try {
            date = format.parse(dateTime);
        } catch (ParseException e) {
            return sqlDateToDate(dateTime);
        }
        return date;
    }

    public static String dateToSqlDateTime(long date) {
        return (new SimpleDateFormat(SQL_DATE_TIME_FORMAT, Locale.getDefault())).format(date);
    }

    public static String dateToString(long date, boolean withYear, boolean abbrevMonth) {
        int flags = DateUtils.FORMAT_SHOW_DATE;
        flags = withYear ? flags | DateUtils.FORMAT_SHOW_YEAR : flags | DateUtils.FORMAT_NO_YEAR;
        if (abbrevMonth) flags = flags | DateUtils.FORMAT_ABBREV_MONTH;

        return DateUtils.formatDateTime(ApplicationFuel.getContext(), date, flags);
    }

    public static String dateToString(Date date, boolean withYear, boolean abbrevMonth) {
        return dateToString(date.getTime(), withYear, abbrevMonth);
    }

    @SuppressWarnings("unused") // Used in fueling_listitem
    public static String dateToString(long date, boolean withYear) {
//        return dateToSqlDateTime(date);
        return dateToString(date, withYear, false);
    }

    public static String dateTimeToString(long date) {
        return DateUtils.formatDateTime(ApplicationFuel.getContext(), date,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
    }

    public static String dateTimeToString(Date date) {
        return dateTimeToString(date.getTime());
    }

    public static float stringToFloat(String value) {
        if (TextUtils.isEmpty(value)) return 0;
        try {
            return Float.parseFloat(value);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String floatToString(float value, boolean showZero) {
        // TODO: LOCALE?
        String strValue = DECIMAL_FORMAT.format(value).replace(',', '.');
        return showZero ? strValue : value == 0 ? "" : strValue;
    }

    public static String floatToString(float value) {
        return floatToString(value, true);
    }

    public static float editTextToFloat(EditText edit) {
        return stringToFloat(edit.getText().toString());
    }

    public static void floatToEditText(EditText edit, float value, boolean showZero) {
        edit.setText(floatToString(value, showZero));
    }
}
