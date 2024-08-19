package ru.p3tr0vich.fuel.helpers

import android.text.TextUtils
import ru.p3tr0vich.fuel.utils.UtilsLog
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

object SMSTextPatternHelper {

    private var LOG_ENABLED = false

    private const val PATTERN_FLOAT = "([-]?\\\\d*[.,]?\\\\d+)"

    @JvmStatic
    private fun convert(pattern: String): String {
        var s = pattern

        if (LOG_ENABLED) {
            UtilsLog.d(SMSTextPatternHelper::class.java, "convert", "pattern == $s")
        }

        // Экранирование всех символов, кроме буквенно-цифровых,
        // разделителей (пробелов, переводов строки и т.п.)
        // символов '(', '|', ')', '@' и символа экранирования '\'.
        // Пример: "яяя (xxx[zzz]111\|/) qqq" -> "яяя (xxx\[zzz\]111\|\/) qqq".
        // RegEx: "([^\w\s@(|)\\])". Replace: "\$1".

        s = s.replace("([^\\w\\s@(|)\\\\])".toRegex(), "\\\\$1")

        // Замена групп символов-разделителей.
        // Пример: "яяя xxx      zzz" -> "яяя.*?xxx.*?zzz".
        // RegEx: "[\s]+". Replace: ".*?".
        s = s.replace("[\\s]+".toRegex(), ".*?")

        // Замена двойных символов '\' на символ с кодом 0 (��).
        // Пример: "@ \@ \\@ \\\@ \\\\@" -> "@ \@ ��@ ��\@ ����@".
        // RegEx: "[\\][\\]". Replace: "\00".
        s = s.replace("[\\\\][\\\\]".toRegex(), "\u0000")

        // Установка всех групп в незахватывающие, то есть
        // добавление к символу '(' (открывающаяся скобка, начало группы) строки "?:",
        // если символ '(' не экранирован, то есть перед ним не стоит символ '\'.
        // В итоговом выражении должна быть только одна захватывающая группа,
        // в которой будет содержаться вещественное число.
        // Пример: "(xxx\(yyy(zzz\(\(qqq" -> "(?:xxx\(yyy(?:zzz\(\(qqq".
        // RegEx: "(?<![\\])[(]". Replace: "(?:".
        s = s.replace("(?<![\\\\])[(]".toRegex(), "(?:")

        // Замена символа '@' (собака) на регулярное выражение поиска вещественного числа,
        // если символ '@' не экранирован, то есть перед ним не стоит символ '\'.
        // Пример: "@xxx\@yyy@zzz\@\@qqq" -> "PATTERN_FLOATxxx\@yyyPATTERN_FLOATzzz\@\@qqq",
        // где PATTERN_FLOAT -- регулярное выражение поиска вещественного числа.
        // RegEx: "(?<![\\])[@]". Replace: PATTERN_FLOAT == "([-]?\d*[.,]?\d+)".
        s = s.replace("(?<![\\\\])[@]".toRegex(), PATTERN_FLOAT)

        // Обратная замена символов с кодом 0 на двойные символы '\'.
        // Пример: "@ \@ ��@ ��\@ ����@" -> "@ \@ \\@ \\\@ \\\\@".
        // RegEx: "[\00]". Replace: "\\\\".
        s = s.replace("[\\00]".toRegex(), "\\\\\\\\")

        if (LOG_ENABLED) {
            UtilsLog.d(SMSTextPatternHelper::class.java, "convert", "pattern == $s")
        }

        return s
    }

    @JvmStatic
    @Throws(PatternSyntaxException::class)
    fun getValue(regularExpression: String?, message: String?): Float? {
        if (TextUtils.isEmpty(regularExpression) || TextUtils.isEmpty(message)) {
            return null
        }

        val pattern = Pattern.compile(convert(regularExpression!!), Pattern.CASE_INSENSITIVE)

        val matcher = pattern.matcher(message!!.replace('\n', ' '))

        return if (matcher.find() && matcher.groupCount() > 0)
            try {
                java.lang.Float.valueOf(matcher.group(1)?.replace(',', '.')!!)
            } catch (e: Exception) {
                null
            }
        else
            null
    }
}