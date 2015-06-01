package ru.p3tr0vich.fuel;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.DateUtils;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.pnikosis.materialishprogress.ProgressWheel;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


class Functions {

    public static Context sApplicationContext;

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    private static final String SQLITE_DATE_FORMAT = "yyyy-MM-dd";

    public static Date sqliteToDate(String sqlDate) {
        Date date = null;

        DateFormat format = new SimpleDateFormat(SQLITE_DATE_FORMAT, Locale.getDefault());
        try {
            date = format.parse(sqlDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static String dateToString(Date date, boolean withYear) {
        int flags = DateUtils.FORMAT_SHOW_DATE;
        if (withYear) flags = flags | DateUtils.FORMAT_SHOW_YEAR;
        else flags = flags | DateUtils.FORMAT_NO_YEAR;
        return DateUtils.formatDateTime(sApplicationContext, date.getTime(), flags);
    }

    public static String sqliteToString(String sqlDate, boolean withYear) {
        return dateToString(sqliteToDate(sqlDate), withYear);
    }

    public static String dateToSQLite(Date date) {
        DateFormat dateFormat = new SimpleDateFormat(SQLITE_DATE_FORMAT, Locale.getDefault());
        return dateFormat.format(date);
    }

    public static String checkSQLiteDate(String sqlDate) throws ParseException {
        DateFormat format = new SimpleDateFormat(SQLITE_DATE_FORMAT, Locale.getDefault());
        format.parse(sqlDate);

        return sqlDate;
    }

    public static float textToFloat(String text) {
        if ((text == null) || (text.length() == 0)) return 0;
        try {
            return Float.parseFloat(text);
        } catch (Exception e) {
            return 0;
        }
    }

    public static float editTextToFloat(EditText edit) {
        return textToFloat(edit.getText().toString());
    }

    public static String floatToString(float value) {
        // TODO: LOCALE
        return DECIMAL_FORMAT.format(value).replace(',', '.');
    }

    public static void floatToText(EditText edit, float value, boolean showZero) {
        if (showZero) edit.setText(floatToString(value));
        else if (value == 0) edit.setText("");
        else edit.setText(floatToString(value));
    }

    public static int getCurrentYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return calendar.get(Calendar.YEAR);
    }

    public static void showKeyboard(EditText editText) {
        editText.requestFocus();
        editText.selectAll();
        InputMethodManager imm = (InputMethodManager) sApplicationContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void setProgressWheelVisible(ProgressWheel progressWheel, boolean visible) {
        if (visible) {
            progressWheel.startAnimation(AnimationUtils.loadAnimation(
                    sApplicationContext, android.R.anim.fade_in));
            progressWheel.setVisibility(View.VISIBLE);
        } else {
            progressWheel.startAnimation(AnimationUtils.loadAnimation(
                    sApplicationContext, android.R.anim.fade_out));
            progressWheel.setVisibility(View.GONE);
        }
    }

    public static Const.RecordAction intToRecordAction(int i) {
        if (i == Const.RecordAction.ADD.ordinal()) return Const.RecordAction.ADD;
        else if (i == Const.RecordAction.UPDATE.ordinal()) return Const.RecordAction.UPDATE;
        else return Const.RecordAction.DELETE;
    }

    public static boolean isInternetConnected() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) sApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo[] allNetworkInfo = connectivityManager.getAllNetworkInfo();
            if (allNetworkInfo != null)
                for (NetworkInfo networkInfo : allNetworkInfo)
                    if (networkInfo.getState() == NetworkInfo.State.CONNECTED)
                        return true;
        }
        return false;
    }
}
