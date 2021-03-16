/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest;

import com.yandex.disk.rest.exceptions.CancelledUploadingException;
import com.yandex.disk.rest.util.Logger;
import com.yandex.disk.rest.util.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/* package */ abstract class RequestBodyProgress {

    private static final Logger logger = LoggerFactory.getLogger(RequestBodyProgress.class);

    private static final int SIZE = 2048;

    /**
     * Returns a new request body that transmits the content of {@code file}.
     * <br/>
     * Based on {@link RequestBody#create(MediaType, File)}
     *
     * @see RequestBody#create(MediaType, File)
     */
    /* package */ static RequestBody create(@NonNull final MediaType contentType, @Nullable final File file,
                                            final long startOffset, @Nullable final ProgressListener listener) {
        if (file == null) {
            throw new NullPointerException("content == null");
        }

        if (listener == null && startOffset == 0) {
            return RequestBody.create(contentType, file);
        }

        return new RequestBody() {

            private void updateProgress(long loaded)
                    throws CancelledUploadingException {
                if (listener != null) {
                    if (listener.hasCancelled()) {
                        throw new CancelledUploadingException();
                    }
                    listener.updateProgress(loaded + startOffset, file.length());
                }
            }

            @Override
            public MediaType contentType() {
                return contentType;
            }

            @Override
            public long contentLength() {
                return file.length() - startOffset;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                Source source = null;
                InputStream inputStream = new FileInputStream(file);
                try {
                    if (startOffset > 0) {
                        long skipped = inputStream.skip(startOffset);
                        if (skipped != startOffset) {
                            throw new IOException("RequestBodyProgress: inputStream.skip() failed");
                        }
                    }
                    long loaded = 0;
                    updateProgress(loaded);
                    source = Okio.source(inputStream);
                    Buffer buffer = new Buffer();
                    for (long readCount; (readCount = source.read(buffer, SIZE)) != -1; ) {
                        sink.write(buffer, readCount);
                        loaded += readCount;
                        updateProgress(loaded);
                    }
                    logger.debug("loaded: " + loaded);
                } finally {
                    Util.closeQuietly(source);
                    Util.closeQuietly(inputStream);
                }
            }
        };
    }
}
