package ru.p3tr0vich.fuel.utils

import android.text.TextUtils
import android.text.format.DateUtils
import android.widget.EditText
import android.widget.TextView
import ru.p3tr0vich.fuel.ApplicationFuel
import java.text.DecimalFormat

object UtilsFormat {

    private val DECIMAL_FORMAT = DecimalFormat("#.##")

    @JvmStatic
    fun dateTimeToString(date: Long, withYear: Boolean, abbrevMonth: Boolean): String {
        var flags = DateUtils.FORMAT_SHOW_DATE

        flags = flags or if (withYear) DateUtils.FORMAT_SHOW_YEAR else DateUtils.FORMAT_NO_YEAR

        if (abbrevMonth) {
            flags = flags or DateUtils.FORMAT_ABBREV_MONTH
        }

        //        flags |= DateUtils.FORMAT_SHOW_TIME;

        return DateUtils.formatDateTime(ApplicationFuel.context, date, flags)
    }

    @JvmStatic
    fun dateToString(date: Long, withYear: Boolean): String {
        return dateTimeToString(date, withYear, false)
    }

    @JvmStatic
    fun stringToFloat(value: String): Float {
        if (TextUtils.isEmpty(value)) return 0f

        return try {
            java.lang.Float.parseFloat(value)
        } catch (e: Exception) {
            0f
        }
    }

    @JvmStatic
    @JvmOverloads
    fun floatToString(value: Float, showZero: Boolean = true): String {
        val strValue = DECIMAL_FORMAT.format(value).replace(',', '.')

        return when {
            showZero -> strValue
            value == 0f -> ""
            else -> strValue
        }
    }

    @JvmStatic
    fun editTextToFloat(edit: EditText?): Float {
        return stringToFloat(edit?.text.toString())
    }

    @JvmStatic
    fun floatToEditText(edit: EditText?, value: Float, showZero: Boolean) {
        edit?.setText(floatToString(value, showZero))
    }

    @JvmStatic
    fun floatToTextView(text: TextView?, value: Float, showZero: Boolean) {
        text?.text = floatToString(value, showZero)
    }
}