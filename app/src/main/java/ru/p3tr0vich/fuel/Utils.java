package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.view.TintableBackgroundView;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

class Utils {

    private static final String TAG = "Utils";

    public static void showKeyboard(EditText editText) {
        editText.requestFocus();
        editText.selectAll();
        ((InputMethodManager)
                ApplicationFuel.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null)
            ((InputMethodManager)
                    ApplicationFuel.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void setViewVisibleAnimate(View view, boolean visible) {
        if (view.getVisibility() == View.VISIBLE && visible) return;

        if (visible) {
            view.startAnimation(AnimationUtils.loadAnimation(
                    ApplicationFuel.getContext(), android.R.anim.fade_in));
            view.setVisibility(View.VISIBLE);
        } else {
            view.startAnimation(AnimationUtils.loadAnimation(
                    ApplicationFuel.getContext(), android.R.anim.fade_out));
            view.setVisibility(View.GONE);
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

    public static int getInteger(@IntegerRes int id) {
        return ApplicationFuel.getContext().getResources().getInteger(id);
    }

    public static void toast(@StringRes int resId) {
        Toast.makeText(ApplicationFuel.getContext(), resId, Toast.LENGTH_SHORT).show();
    }

    public static void toast(@NonNull String text) {
        Toast.makeText(ApplicationFuel.getContext(), text, Toast.LENGTH_SHORT).show();
    }

    public static void openUrl(@NonNull Context context, @NonNull String url,
                               @Nullable String onErrorMessage) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            UtilsLog.d(TAG, "openUrl", "exception == " + e.toString());

            if (!TextUtils.isEmpty(onErrorMessage)) toast(onErrorMessage);
        }
    }

    public static void setBackgroundTint(@NonNull View view,
                                         @ColorRes int defaultColor, @ColorRes int pressedColor) {
        if (view instanceof TintableBackgroundView)
            //noinspection deprecation
            ((TintableBackgroundView) view).setSupportBackgroundTintList(
                    new ColorStateList(
                            new int[][]{
                                    new int[]{-android.R.attr.state_pressed, -android.R.attr.state_checked},
                                    new int[]{}},
                            new int[]{
                                    view.getResources().getColor(defaultColor),
                                    view.getResources().getColor(pressedColor)}));
    }
}