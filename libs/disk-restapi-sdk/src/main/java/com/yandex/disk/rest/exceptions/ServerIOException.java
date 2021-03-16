/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest.exceptions;

public class ServerIOException extends ServerException {

    public ServerIOException() {
        super();
    }

    public ServerIOException(String message) {
        super(message);
    }

    public ServerIOException(Throwable e) {
        super(e);
    }
}
