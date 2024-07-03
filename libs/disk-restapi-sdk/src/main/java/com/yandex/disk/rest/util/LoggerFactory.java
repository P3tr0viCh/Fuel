/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest.util;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LoggerFactory {

    @NonNull
    public static Logger getLogger(@NonNull final Class clazz) {
        final String tag = clazz.getSimpleName();
        return new Logger() {

            @Override
            public boolean isDebugEnabled() {
                return false;
            }

            @Override
            public void debug(@NonNull String message) {
                Log.d(tag, message);
            }

            @Override
            public void debug(@NonNull String message, @Nullable Throwable throwable) {
                Log.d(tag, message, throwable);
            }

            @Override
            public void info(@NonNull String message) {
                Log.d(tag, message);
            }

            @Override
            public void info(@NonNull String message, @Nullable Throwable throwable) {
                Log.i(tag, message, throwable);
            }

            @Override
            public void warn(@NonNull String message) {
                Log.w(tag, message);
            }

            @Override
            public void warn(@NonNull String message, @Nullable Throwable throwable) {
                Log.w(tag, message, throwable);
            }

            @Override
            public void error(@NonNull String message) {
                Log.e(tag, message);
            }

            @Override
            public void error(@NonNull String message, @Nullable Throwable throwable) {
                Log.e(tag, message, throwable);
            }
        };
    }
}
