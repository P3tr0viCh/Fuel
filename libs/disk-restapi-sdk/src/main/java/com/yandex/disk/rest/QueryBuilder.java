/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/* package */ class QueryBuilder {

    @NonNull
    private static final String UTF8 = "UTF-8";

    @NonNull
    private final Map<String, Object> queryMap;

    @NonNull
    private final String url;

    /* package */ QueryBuilder(@NonNull final String url) {
        this.url = url;
        this.queryMap = new LinkedHashMap<>();
    }

    @NonNull
    /* package */ String build() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : queryMap.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(encode(entry.getKey()))
                        .append("=")
                        .append(encode(value.toString()));
            }
        }
        return url + "?" + sb.toString();
    }

    @NonNull
    private static String encode(@NonNull final String key) {
        try {
            return URLEncoder.encode(key, UTF8);
        } catch (UnsupportedEncodingException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    @NonNull
    /* package */ QueryBuilder add(@NonNull final String key, @Nullable final String value) {
        queryMap.put(key, value);
        return this;
    }

    @NonNull
    /* package */ QueryBuilder add(@NonNull final String key, @Nullable final Boolean value) {
        queryMap.put(key, value);
        return this;
    }

    @NonNull
    /* package */ QueryBuilder add(@NonNull final String key, @Nullable final Integer value) {
        queryMap.put(key, value);
        return this;
    }
}
