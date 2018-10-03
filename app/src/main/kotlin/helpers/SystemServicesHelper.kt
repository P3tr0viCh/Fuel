package ru.p3tr0vich.fuel.helpers

import android.accounts.AccountManager
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.view.inputmethod.InputMethodManager

object SystemServicesHelper {

    @JvmStatic
    fun getNotificationManager(context: Context?): NotificationManager? {
        return context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
    }

    @JvmStatic
    fun getConnectivityManager(context: Context?): ConnectivityManager? {
        return context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    }

    @JvmStatic
    fun getAccountManager(context: Context?): AccountManager? {
        return context?.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager?
    }

    @JvmStatic
    fun getInputMethodManager(context: Context?): InputMethodManager? {
        return context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    }
}