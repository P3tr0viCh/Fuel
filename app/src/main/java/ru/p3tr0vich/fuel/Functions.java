package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.DimenRes;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
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

public class Functions {

    private static final String LOG_TAG = "XXX";

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    private static final String SQLITE_DATE_FORMAT = "yyyy-MM-dd";

    public static Date sqlDateToDate(String sqlDate) {
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
        return DateUtils.formatDateTime(ApplicationFuel.getContext(), date.getTime(), flags);
    }

    public static String dateToString(Date date, boolean withYear) {
        return dateToString(date, withYear, false);
    }

    @SuppressWarnings("unused")
    public static String sqlDateToString(String sqlDate, boolean withYear) {
        // Used in fueling_listitem
        return dateToString(sqlDateToDate(sqlDate), withYear);
    }

    public static String dateToSQLite(Date date) {
        return (new SimpleDateFormat(SQLITE_DATE_FORMAT, Locale.getDefault())).format(date);
    }

    public static String dateTimeToString(Date date) {
        return DateUtils.formatDateTime(ApplicationFuel.getContext(), date.getTime(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
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
        InputMethodManager inputMethodManager = (InputMethodManager)
                ApplicationFuel.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)
                ApplicationFuel.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view != null)
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void setProgressWheelVisible(ProgressWheel progressWheel, boolean visible) {
        if (visible) {
            progressWheel.startAnimation(AnimationUtils.loadAnimation(
                    ApplicationFuel.getContext(), android.R.anim.fade_in));
            progressWheel.setVisibility(View.VISIBLE);
        } else {
            progressWheel.startAnimation(AnimationUtils.loadAnimation(
                    ApplicationFuel.getContext(), android.R.anim.fade_out));
            progressWheel.setVisibility(View.GONE);
        }
    }

    @Const.RecordAction
    public static int intToRecordAction(int i) {
        return i;
    }

    public static boolean isInternetConnected() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) ApplicationFuel.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                @SuppressWarnings("deprecation")
                NetworkInfo[] networkInfos = connectivityManager.getAllNetworkInfo();
                if (networkInfos != null)
                    for (NetworkInfo networkInfo : networkInfos)
                        if (networkInfo.getState() == NetworkInfo.State.CONNECTED)
                            return true;
            } else {
                Network[] networks = connectivityManager.getAllNetworks();
                if (networks != null)
                    for (Network network : networks) {
                        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                        if (networkInfo != null && networkInfo.isConnected()) return true;
                    }
            }
        }
        return false;
    }

    public static void addSpinnerInToolbar(ActionBar actionBar, Toolbar toolbar, Spinner spinner,
                                           ArrayAdapter adapter, AdapterView.OnItemSelectedListener listener) {
        actionBar.setDisplayShowTitleEnabled(false);

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        spinner.setAdapter(adapter);

        int px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                Const.TOOLBAR_SPINNER_DROPDOWN_OFFSET,
                ApplicationFuel.getContext().getResources().getDisplayMetrics()));

        spinner.setDropDownVerticalOffset(-px);

        toolbar.addView(spinner);

        spinner.setOnItemSelectedListener(listener);
    }

    public static void setViewHeight(View view, int height) {
        view.getLayoutParams().height = height;
        view.requestLayout();
    }

    public static void setViewTopMargin(View view, int topMargin) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
        params.setMargins(params.leftMargin, topMargin, params.rightMargin, params.bottomMargin);
        view.setLayoutParams(params);
    }

    public static boolean isPhone() {
        return ApplicationFuel.getContext().getResources().getDimension(R.dimen.is_phone) != 0;
    }

    private static boolean isPortrait() {
        return ApplicationFuel.getContext().getResources().getDimension(R.dimen.is_portrait) != 0;
    }

    public static boolean isPhoneInPortrait() {
        return isPhone() && isPortrait();
    }

    public static boolean isDebuggable() {
        return (ApplicationFuel.getContext().getApplicationInfo().flags &
                ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    public static void logD(String msg) {
        if (isDebuggable()) Log.d(LOG_TAG, msg);
    }

    public static int getDimensionPixelSize(@DimenRes int id) {
        return ApplicationFuel.getContext().getResources().getDimensionPixelSize(id);
    }
}
