package ru.p3tr0vich.fuel.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

public class UtilsString {

    private UtilsString() {
    }

    @NonNull
    public static String encodeLineBreaks(@Nullable String s) {
        if (TextUtils.isEmpty(s)) return "";

        return s.replaceAll("\\\\n", "\\\\\\\\n").replaceAll("[\\n]", "\\\\n");
    }

    @NonNull
    public static String decodeLineBreaks(@Nullable String s) {
        if (TextUtils.isEmpty(s)) return "";

        return s.replaceAll("(?<![\\\\])\\\\n", "\n").replaceAll("\\\\\\\\n", "\\\\n");
    }
}