package ru.p3tr0vich.fuel.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.IntegerRes
import android.support.annotation.StringRes
import android.support.v4.view.TintableBackgroundView
import android.text.TextUtils
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import ru.p3tr0vich.fuel.ApplicationFuel
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.helpers.SystemServicesHelper
import java.util.concurrent.TimeUnit

object Utils {

    private const val TAG = "Utils"

    @JvmStatic
    val isPhone: Boolean
        get() = ApplicationFuel.context.resources.getBoolean(R.bool.is_phone)

    @JvmStatic
    private val isPortrait: Boolean
        get() = ApplicationFuel.context.resources.getBoolean(R.bool.is_portrait)

    @JvmStatic
    val isPhoneInPortrait: Boolean
        get() = isPhone && isPortrait

    @JvmStatic
    val isDebuggable: Boolean
        get() = ApplicationFuel.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    @JvmStatic
    fun showKeyboard(editText: EditText?) {
        editText?.let {
            it.requestFocus()
            it.selectAll()

            SystemServicesHelper.getInputMethodManager(it.context)
                    ?.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    @JvmStatic
    fun hideKeyboard(activity: Activity?) {
        activity?.currentFocus?.let {
            SystemServicesHelper.getInputMethodManager(it.context)
                    ?.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    @JvmStatic
    fun setViewVisibleAnimate(view: View?, visible: Boolean) {
        if (view == null) return

        if (view.visibility == View.VISIBLE && visible) return

        if (visible) {
            view.startAnimation(AnimationUtils.loadAnimation(view.context, android.R.anim.fade_in))
            view.visibility = View.VISIBLE
        } else {
            view.startAnimation(AnimationUtils.loadAnimation(view.context, android.R.anim.fade_out))
            view.visibility = View.GONE
        }
    }

    @JvmStatic
    fun setViewHeight(view: View?, height: Int) {
        if (view == null) return

        view.layoutParams.height = height
        view.requestLayout()
    }

    @JvmStatic
    fun setViewTopMargin(view: View?, topMargin: Int) {
        if (view == null) return

        val params = view.layoutParams as RelativeLayout.LayoutParams
        params.setMargins(params.leftMargin, topMargin, params.rightMargin, params.bottomMargin)
        view.layoutParams = params
    }

    @JvmStatic
    fun getInteger(@IntegerRes id: Int): Int {
        return ApplicationFuel.context.resources.getInteger(id)
    }

    @JvmStatic
    @ColorInt
    fun getColor(@ColorRes id: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ApplicationFuel.context.resources.getColor(id, null)
        } else {
            @Suppress("DEPRECATION")
            ApplicationFuel.context.resources.getColor(id)
        }
    }

    @JvmStatic
    fun getSupportActionBarSize(context: Context): Int {
        val a = context.theme.obtainStyledAttributes(
                intArrayOf(android.R.attr.actionBarSize))
        try {
            return a.getDimensionPixelSize(0, 0)
        } finally {
            a.recycle()
        }
    }

    @JvmStatic
    fun toast(@StringRes resId: Int) {
        Toast.makeText(ApplicationFuel.context, resId, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun toast(text: String) {
        Toast.makeText(ApplicationFuel.context, text, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun openUrl(context: Context, url: String, onErrorMessage: String?) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            UtilsLog.d(TAG, "openUrl", "exception == " + e.toString())

            if (!TextUtils.isEmpty(onErrorMessage)) {
                toast(onErrorMessage!!)
            }
        }

    }

    @JvmStatic
    fun setBackgroundTint(view: View?, @ColorRes defaultColor: Int, @ColorRes pressedColor: Int) {
        if (view is TintableBackgroundView) {
            (view as TintableBackgroundView).supportBackgroundTintList = ColorStateList(
                    arrayOf(intArrayOf(-android.R.attr.state_pressed, -android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(getColor(defaultColor), getColor(pressedColor)))
        }
    }

    @JvmStatic
    fun wait(seconds: Int) {
        for (i in 0 until seconds) {
            try {
                TimeUnit.SECONDS.sleep(1)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            UtilsLog.d(TAG, "query", "wait... " + (seconds - i))
        }
    }
}