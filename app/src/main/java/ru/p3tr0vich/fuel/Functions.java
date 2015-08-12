package ru.p3tr0vich.fuel;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;

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

    public static String dateToString(Date date, boolean withYear, boolean abbrevMonth) {
        int flags = DateUtils.FORMAT_SHOW_DATE;
        flags = withYear ? flags | DateUtils.FORMAT_SHOW_YEAR : flags | DateUtils.FORMAT_NO_YEAR;
        if (abbrevMonth) flags = flags | DateUtils.FORMAT_ABBREV_MONTH;
        return DateUtils.formatDateTime(sApplicationContext, date.getTime(), flags);
    }

    public static String dateToString(Date date, boolean withYear) {
        return dateToString(date, withYear, false);
    }

    public static String sqliteToString(String sqlDate, boolean withYear) {
        return dateToString(sqliteToDate(sqlDate), withYear);
    }

    public static String dateToSQLite(Date date) {
        return new SimpleDateFormat(SQLITE_DATE_FORMAT, Locale.getDefault()).format(date);
    }

    public static String checkSQLiteDate(String sqlDate) throws ParseException {
        DateFormat format = new SimpleDateFormat(SQLITE_DATE_FORMAT, Locale.getDefault());
        format.parse(sqlDate);

        return sqlDate;
    }

    public static float textToFloat(String text) {
        if (TextUtils.isEmpty(text)) return 0;
        try {
            return Float.parseFloat(text);
        } catch (Exception e) {
            return 0;
        }
    }

    public static float editTextToFloat(EditText edit) {
        return textToFloat(edit.getText().toString());
    }

    public static String floatToString(float value, boolean showZero) {
        // TODO: LOCALE?
        String strValue = DECIMAL_FORMAT.format(value).replace(',', '.');
        return showZero ? strValue : value == 0 ? "" : strValue;
    }

    public static String floatToString(float value) {
        return floatToString(value, true);
    }

    public static void floatToText(EditText edit, float value, boolean showZero) {
        edit.setText(floatToString(value, showZero));
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

    public static void addSpinnerInToolbar(ActionBar actionBar, Toolbar toolbar, Spinner spinner,
                                           ArrayAdapter adapter, AdapterView.OnItemSelectedListener listener) {
        actionBar.setDisplayShowTitleEnabled(false);

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        spinner.setAdapter(adapter);

        int px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                Const.TOOLBAR_SPINNER_DROPDOWN_OFFSET, sApplicationContext.getResources().getDisplayMetrics()));

        spinner.setDropDownVerticalOffset(-px);

        toolbar.addView(spinner);

        spinner.setOnItemSelectedListener(listener);
    }

    public static void setViewHeight(View view, int height) {
        view.getLayoutParams().height = height;
        view.requestLayout();
    }

    public static void setViewTopMargin(View view, RelativeLayout.LayoutParams layoutParams, int topMargin) {
        layoutParams.setMargins(layoutParams.leftMargin, topMargin, layoutParams.rightMargin, layoutParams.bottomMargin);
        view.setLayoutParams(layoutParams);
    }

    private static int pxToDp(Context context, int px) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, px, context.getResources().getDisplayMetrics()));
    }

    public static void setDialogWidth(Dialog dialog, int width) {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = pxToDp(dialog.getContext(), width);
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(params);
    }

    public static boolean isPhoneInPortrait() {
        return sApplicationContext.getResources().getDimension(R.dimen.is_phone_in_portrait) != 0;
    }

    public static void LogD(String msg) {
        if ((sApplicationContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0)
            Log.d(Const.LOG_TAG, msg);
    }
}
