package ru.p3tr0vich.fuel.utils

import android.text.TextUtils
import android.util.Log

object UtilsLog {

    private const val LOG_TAG = "XXX"

    @JvmStatic
    @JvmOverloads
    fun d(tag: String?, msg: String, extMsg: String? = null) {
        var msgLog = msg

        if (!TextUtils.isEmpty(tag)) {
            msgLog = "$tag: $msgLog"
        }

        if (!TextUtils.isEmpty(extMsg)) {
            msgLog = "$msgLog. $extMsg"
        }

        Log.d(LOG_TAG, msgLog)
    }

    @JvmStatic
    fun d(aClass: Class<*>, msg: String, extMsg: String?) {
        d(aClass.simpleName, msg, extMsg)
    }
}