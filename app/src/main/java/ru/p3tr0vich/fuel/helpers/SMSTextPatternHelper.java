package ru.p3tr0vich.fuel.helpers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ru.p3tr0vich.fuel.utils.UtilsLog;

public class SMSTextPatternHelper {

    private static final boolean LOG_ENABLED = false;

    private static final String PATTERN_FLOAT = "([-]?\\\\d*[.,]?\\\\d+)";

    private SMSTextPatternHelper() {
    }

    @NonNull
    private static String convert(@NonNull String pattern) {
        if (LOG_ENABLED)
            UtilsLog.d(SMSTextPatternHelper.class, "convert", "pattern == " + pattern);

        // Экранирование всех символов, кроме буквенно-цифровых,
        // разделителей (пробелов, переводов строки и т.п.)
        // символов '(', '|', ')', '@' и символа экранирования '\'.
        // Пример: "яяя (xxx[zzz]111\|/) qqq" -> "яяя (xxx\[zzz\]111\|\/) qqq".
        // RegEx: "([^\w\s@\(\|\)\\])". Replace: "\$1".
        //noinspection RegExpRedundantEscape
        pattern = pattern.replaceAll("([^\\w\\s@\\(\\|\\)\\\\])", "\\\\$1");

        // Замена групп символов-разделителей.
        // Пример: "яяя xxx      zzz" -> "яяя.*?xxx.*?zzz".
        // RegEx: "[\s]+". Replace: ".*?".
        pattern = pattern.replaceAll("[\\s]+", ".*?");

        // Замена двойных символов '\' на символ с кодом 0 (��).
        // Пример: "@ \@ \\@ \\\@ \\\\@" -> "@ \@ ��@ ��\@ ����@".
        // RegEx: "[\\][\\]". Replace: "\00".
        pattern = pattern.replaceAll("[\\\\][\\\\]", "\00");

        // Установка всех групп в незахватывающие, то есть
        // добавление к символу '(' (открывающаяся скобка, начало группы) строки "?:",
        // если символ '(' не экранирован, то есть перед ним не стоит символ '\'.
        // В итоговом выражении должна быть только одна захватывающая группа,
        // в которой будет содержаться вещественное число.
        // Пример: "(xxx\(yyy(zzz\(\(qqq" -> "(?:xxx\(yyy(?:zzz\(\(qqq".
        // RegEx: "(?<![\\])[(]". Replace: "(?:".
        pattern = pattern.replaceAll("(?<![\\\\])[(]", "(?:");

        // Замена символа '@' (собака) на регулярное выражение поиска вещественного числа,
        // если символ '@' не экранирован, то есть перед ним не стоит символ '\'.
        // Пример: "@xxx\@yyy@zzz\@\@qqq" -> "PATTERN_FLOATxxx\@yyyPATTERN_FLOATzzz\@\@qqq",
        // где PATTERN_FLOAT -- регулярное выражение поиска вещественного числа.
        // RegEx: "(?<![\\])[@]". Replace: PATTERN_FLOAT == "([-]?\d*[.,]?\d+)".
        pattern = pattern.replaceAll("(?<![\\\\])[@]", PATTERN_FLOAT);

        // Обратная замена символов с кодом 0 на двойные символы '\'.
        // Пример: "@ \@ ��@ ��\@ ����@" -> "@ \@ \\@ \\\@ \\\\@".
        // RegEx: "[\00]". Replace: "\\\\".
        pattern = pattern.replaceAll("[\\00]", "\\\\\\\\");

        if (LOG_ENABLED)
            UtilsLog.d(SMSTextPatternHelper.class, "convert", "pattern == " + pattern);

        return pattern;
    }

    @Nullable
    public static Float getValue(@Nullable String regularExpression, @Nullable String message) throws PatternSyntaxException {
        if (TextUtils.isEmpty(regularExpression) || TextUtils.isEmpty(message))
            return null;

        @SuppressWarnings("ConstantConditions")
        final Pattern pattern = Pattern.compile(convert(regularExpression), Pattern.CASE_INSENSITIVE);

        @SuppressWarnings("ConstantConditions")
        final Matcher matcher = pattern.matcher(message.replace('\n', ' '));

        if (matcher.find() && (matcher.groupCount() > 0))
            try {
                return Float.valueOf(matcher.group(1).replace(',', '.'));
            } catch (Exception e) {
                return null;
            }
        else
            return null;
    }
}