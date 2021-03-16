/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface Logger {

    boolean isDebugEnabled();

    void debug(@NonNull String message);

    void debug(@NonNull String message, @Nullable Throwable th);

    void info(@NonNull String message);

    void info(@NonNull String message, @Nullable Throwable th);

    void warn(@NonNull String message);

    void warn(@NonNull String message, @Nullable Throwable th);

    void error(@NonNull String message);

    void error(@NonNull String message, @Nullable Throwable th);
}
