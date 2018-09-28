package ru.p3tr0vich.fuel.utils

import android.content.Context
import android.support.annotation.Size
import android.text.TextUtils
import android.text.format.DateUtils
import ru.p3tr0vich.fuel.R
import java.util.*

object UtilsDate {

    @JvmStatic
    val currentYear: Int
        get() = Calendar.getInstance().get(Calendar.YEAR)

    @JvmStatic
    fun utcToLocal(utc: Long): Long {
        return utc + TimeZone.getDefault().getOffset(utc)
    }

    @JvmStatic
    fun localToUtc(local: Long): Long {
        return local - TimeZone.getDefault().getOffset(local)
    }

    @JvmStatic
    fun setStartOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    @JvmStatic
    fun setEndOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
    }

    @JvmStatic
    fun getCalendarInstance(milliseconds: Long): Calendar {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliseconds
        return calendar
    }

    @JvmStatic
    fun getMonthName(context: Context, calendar: Calendar,
                     month: Int, abbrev: Boolean): String {
        calendar.set(Calendar.MONTH, month)
        var flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_NO_MONTH_DAY
        if (abbrev) flags = flags or DateUtils.FORMAT_ABBREV_MONTH

        return DateUtils.formatDateTime(context, calendar.timeInMillis, flags)
    }

    @JvmStatic
    fun getRelativeDateTime(context: Context, dateTime: Long): String {
        val now = System.currentTimeMillis()
        val elapsed = now - dateTime

        var result = ""

        if (elapsed > 0) {
            if (elapsed < 10 * DateUtils.SECOND_IN_MILLIS)
                result = context.getString(R.string.relative_date_time_now)
            else if (elapsed < DateUtils.MINUTE_IN_MILLIS)
                result = context.getString(R.string.relative_date_time_minute)
            else if (elapsed < DateUtils.HOUR_IN_MILLIS)
                result = DateUtils.getRelativeTimeSpanString(
                        dateTime, now,
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_SHOW_DATE).toString()
            else {
                val calendarNow = getCalendarInstance(now)
                val calendarDateTime = getCalendarInstance(dateTime)

                if (calendarNow.get(Calendar.YEAR) == calendarDateTime.get(Calendar.YEAR)) {
                    val daysBetween = Math.abs(
                            calendarNow.get(Calendar.DAY_OF_YEAR) - calendarDateTime.get(Calendar.DAY_OF_YEAR))

                    if (daysBetween == 0)
                        result = context.getString(R.string.relative_date_time_today)
                    else if (daysBetween == 1)
                        result = context.getString(R.string.relative_date_time_yesterday)
                    else if (daysBetween < 7)
                        result = DateUtils.getRelativeTimeSpanString(
                                dateTime, now,
                                DateUtils.DAY_IN_MILLIS,
                                DateUtils.FORMAT_SHOW_DATE).toString()
                }
            }
        }

        if (TextUtils.isEmpty(result)) {
            result = DateUtils.formatDateTime(context, dateTime, DateUtils.FORMAT_SHOW_DATE)
        }

        if (elapsed > DateUtils.MINUTE_IN_MILLIS && elapsed < DateUtils.WEEK_IN_MILLIS) {
            result += ", " + DateUtils.getRelativeTimeSpanString(context, dateTime, true).toString()
        }

        return result
    }

    /**
     * Разбивает секунды на часы, минуты и секунды.
     *
     * @param seconds секунды.
     * @return массив из трёх элементов,
     * где первый элемент -- часы, второй -- минуты и третий -- секунды.
     */
    @JvmStatic
    @Size(3)
    fun splitSeconds(seconds: Int): IntArray {
        val result = IntArray(3)

        result[0] = seconds / 3600
        result[1] = seconds % 3600 / 60
        result[2] = seconds % 60

        return result
    }
}