/*
 * (C) 2015 Yandex LLC (https://yandex.com/)
 *
 * The source code of Java SDK for Yandex.Disk REST API
 * is available to use under terms of Apache License,
 * Version 2.0. See the file LICENSE for the details.
 */

package com.yandex.disk.rest;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class OkHttpClientFactory {

    private static final int CONNECT_TIMEOUT = 30;
    private static final int READ_TIMEOUT = 30;
    private static final int WRITE_TIMEOUT = 30;

    public static OkHttpClient.Builder makeClient() {
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        client.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS);
        client.readTimeout(READ_TIMEOUT, TimeUnit.SECONDS);
        client.writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS);
        client.followSslRedirects(true);
        client.followRedirects(true);
        return client;
    }
}
