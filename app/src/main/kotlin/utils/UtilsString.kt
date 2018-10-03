package ru.p3tr0vich.fuel.utils

import android.text.TextUtils

object UtilsString {

    @JvmStatic
    fun encodeLineBreaks(s: String?): String {
        return if (TextUtils.isEmpty(s)) {
            ""
        } else {
            s!!.replace("\\\\n".toRegex(), "\\\\\\\\n").replace("[\\n]".toRegex(), "\\\\n")
        }
    }

    @JvmStatic
    fun decodeLineBreaks(s: String?): String {
        return if (TextUtils.isEmpty(s)) {
            ""
        } else {
            s!!.replace("(?<![\\\\])\\\\n".toRegex(), "\n").replace("\\\\\\\\n".toRegex(), "\\\\n")
        }
    }
}