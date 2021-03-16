/*
 * (C) 2015 Yandex LLC (https://yandex.com/)
 *
 * The source code of Java SDK for Yandex.Disk REST API
 * is available to use under terms of Apache License,
 * Version 2.0. See the file LICENSE for the details.
 */

package com.yandex.disk.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

public class Credentials {

    @NonNull
    /* package */ static final String AUTHORIZATION_HEADER = "Authorization";

    @NonNull
    /* package */ static final String USER_AGENT_HEADER = "User-Agent";

    @NonNull
    private static final String USER_AGENT = "Cloud API Android Client Example/1.0";

    @NonNull
    protected String user;

    @NonNull
    protected String token;

    public Credentials(@NonNull final String user, @NonNull final String token) {
        this.user = user;
        this.token = token;
    }

    @NonNull
    public String getUser() {
        return user;
    }

    @NonNull
    public String getToken() {
        return token;
    }

    @NonNull
    public List<CustomHeader> getHeaders() {
        final List<CustomHeader> list = new ArrayList<>();
        list.add(new CustomHeader(USER_AGENT_HEADER, USER_AGENT));
        list.add(new CustomHeader(AUTHORIZATION_HEADER, "OAuth " + getToken()));
        return Collections.unmodifiableList(list);
    }
}
