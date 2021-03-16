/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest;

import com.google.gson.Gson;
import com.yandex.disk.rest.exceptions.CancelledDownloadException;
import com.yandex.disk.rest.exceptions.DownloadNoSpaceAvailableException;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.exceptions.http.ConflictException;
import com.yandex.disk.rest.exceptions.http.FileNotModifiedException;
import com.yandex.disk.rest.exceptions.http.FileTooBigException;
import com.yandex.disk.rest.exceptions.http.HttpCodeException;
import com.yandex.disk.rest.exceptions.http.InsufficientStorageException;
import com.yandex.disk.rest.exceptions.http.NotFoundException;
import com.yandex.disk.rest.exceptions.http.PreconditionFailedException;
import com.yandex.disk.rest.exceptions.http.RangeNotSatisfiableException;
import com.yandex.disk.rest.exceptions.http.ServiceUnavailableException;
import com.yandex.disk.rest.json.Link;
import com.yandex.disk.rest.json.Operation;
import com.yandex.disk.rest.retrofit.ErrorHandler;
import com.yandex.disk.rest.util.Hash;
import com.yandex.disk.rest.util.Logger;
import com.yandex.disk.rest.util.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpMethod;

/* package */ class RestClientIO {

    @NonNull
    private static final Logger logger = LoggerFactory.getLogger(RestClientIO.class);

    @NonNull private static final String ETAG_HEADER = "Etag";
    @NonNull private static final String SHA256_HEADER = "Sha256";
    @NonNull private static final String SIZE_HEADER = "Size";
    @NonNull private static final String CONTENT_LENGTH_HEADER = "Content-Length";
    @NonNull private static final String CONTENT_RANGE_HEADER = "Content-Range";

    @NonNull private static final String METHOD_GET = "GET";
    @NonNull private static final String METHOD_DELETE = "DELETE";
    @NonNull private static final String METHOD_PUT = "PUT";

    @NonNull
    private static final Pattern CONTENT_RANGE_HEADER_PATTERN = Pattern.compile("bytes\\D+(\\d+)-\\d+/(\\d+)");

    @NonNull
    private final OkHttpClient client;

    /* package */ RestClientIO(@NonNull final OkHttpClient client) {
        this.client = client;
    }

    /* package */ void downloadUrl(@NonNull final String url, @NonNull final DownloadListener downloadListener)
            throws IOException, CancelledDownloadException, DownloadNoSpaceAvailableException,
            HttpCodeException {

        final Request.Builder req = new Request.Builder()
                .url(url);

        final long length = downloadListener.getLocalLength();
        String ifTag = "If-None-Match";
        if (length >= 0) {
            ifTag = "If-Range";
            final StringBuilder contentRange = new StringBuilder();
            contentRange.append("bytes=").append(length).append("-");
            logger.debug("Range: " + contentRange);
            req.addHeader("Range", contentRange.toString());
        }

        final String etag = downloadListener.getETag();
        if (etag != null) {
            logger.debug(ifTag + ": " + etag);
            req.addHeader(ifTag, etag);
        }

        final Request request = req.build();
        final Response response = client
                .newCall(request)
                .execute();

        boolean partialContent = false;
        final int code = response.code();
        switch (code) {
            case 200:
                // OK
                break;
            case 206:
                partialContent = true;
                break;
            case 304:
                throw new FileNotModifiedException(code);
            case 404:
                throw new NotFoundException(code);
            case 416:
                throw new RangeNotSatisfiableException(code);
            default:
                throw new HttpCodeException(code);
        }

        final ResponseBody responseBody = response.body();
        long contentLength = responseBody.contentLength();
        logger.debug("download: contentLength=" + contentLength);

        long loaded;
        if (partialContent) {
            final ContentRangeResponse contentRangeResponse = parseContentRangeHeader(response.header("Content-Range"));
            logger.debug("download: contentRangeResponse=" + contentRangeResponse);
            if (contentRangeResponse != null) {
                loaded = contentRangeResponse.getStart();
                contentLength = contentRangeResponse.getSize();
            } else {
                loaded = length;
            }
        } else {
            loaded = 0;
            if (contentLength < 0) {
                contentLength = 0;
            }
        }

        OutputStream os = null;
        try {
            downloadListener.setStartPosition(loaded);
            final MediaType contentTypeHeader = responseBody.contentType();
            if (contentTypeHeader != null) {
                downloadListener.setContentType(contentTypeHeader.toString());
            }
            downloadListener.setContentLength(contentLength);

            int count;
            final InputStream content = responseBody.byteStream();
            os = downloadListener.getOutputStream(partialContent);
            final byte[] downloadBuffer = new byte[1024];
            while ((count = content.read(downloadBuffer)) != -1) {
                if (downloadListener.hasCancelled()) {
                    logger.info("Downloading " + url + " canceled");
                    cancel(request.tag());
                    throw new CancelledDownloadException();
                }
                os.write(downloadBuffer, 0, count);
                loaded += count;
                downloadListener.updateProgress(loaded, contentLength);
            }
        } catch (CancelledDownloadException ex) {
            throw ex;
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
           	throw e;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException ex) {
                // nothing
            }
            try {
                response.body().close();
            } catch (NullPointerException ex) {
                logger.warn(ex.getMessage(), ex);
            }
        }
    }

    @Nullable
    private ContentRangeResponse parseContentRangeHeader(@Nullable final String header) {
        if (header == null) {
            return null;
        }
        final Matcher matcher = CONTENT_RANGE_HEADER_PATTERN.matcher(header);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new ContentRangeResponse(Long.parseLong(matcher.group(1)), Long.parseLong(matcher.group(2)));
        } catch (IllegalStateException ex) {
            logger.error("parseContentRangeHeader: " + header, ex);
            return null;
        } catch (NumberFormatException ex) {
            logger.error("parseContentRangeHeader: " + header, ex);
            return null;
        }
    }

    /* package */ void uploadFile(@NonNull final String url, @NonNull final File file, final long startOffset,
                           @Nullable final ProgressListener progressListener)
            throws IOException, HttpCodeException {
        logger.debug("uploadFile: put to url: "+url);
        final MediaType mediaType = MediaType.parse("application/octet-stream");
        final RequestBody requestBody = RequestBodyProgress.create(mediaType, file, startOffset,
                progressListener);
        final Request.Builder requestBuilder = new Request.Builder()
                .removeHeader(Credentials.AUTHORIZATION_HEADER)
                .url(url)
                .put(requestBody);
        if (startOffset > 0) {
            final StringBuilder contentRange = new StringBuilder();
            contentRange.append("bytes ").append(startOffset).append("-").append(file.length() - 1)
                    .append("/").append(file.length());
            logger.debug(CONTENT_RANGE_HEADER + ": " + contentRange);
            requestBuilder.addHeader(CONTENT_RANGE_HEADER, contentRange.toString());
        }
        final Request request = requestBuilder.build();

        final Response response = client
                .newCall(request)
                .execute();

        final String statusLine = response.message();
        logger.debug("headUrl: " + statusLine + " for url " + url);

        final int code = response.code();

        final ResponseBody responseBody = response.body();
        responseBody.close();

        switch (code) {
            case 201:
            case 202:
                logger.debug("uploadFile: file uploaded successfully: "+file);
                break;
            case 404:
                throw new NotFoundException(code, null);
            case 409:
                throw new ConflictException(code, null);
            case 412:
                throw new PreconditionFailedException(code, null);
            case 413:
                throw new FileTooBigException(code, null);
            case 503:
                throw new ServiceUnavailableException(code, null);
            case 507:
                throw new InsufficientStorageException(code, null);
            default:
                throw new HttpCodeException(code);
        }
    }

    /* package */ long getUploadedSize(@NonNull final String url, @NonNull final Hash hash)
            throws IOException {

        final Request request = new Request.Builder()
                .removeHeader(Credentials.AUTHORIZATION_HEADER)
                .url(url)
                .head()
                .addHeader(ETAG_HEADER, hash.getMd5())
                .addHeader(SHA256_HEADER, hash.getSha256())
                .addHeader(SIZE_HEADER, String.valueOf(hash.getSize()))
                .build();

        final Response response = client
                .newCall(request)
                .execute();

        final int code = response.code();
        final ResponseBody responseBody = response.body();
        responseBody.close();
        switch (code) {
            case 200:
                return Long.parseLong(response.header(CONTENT_LENGTH_HEADER, "0"));
            default:
                return 0;
        }
    }

    @NonNull
    /* package */ Operation getOperation(@NonNull final String url)
            throws IOException, HttpCodeException {
        final Response response = call(METHOD_GET, url);
        final int code = response.code();
        if (!response.isSuccessful()) {
            throw new HttpCodeException(code);
        }
        return parseJson(response, Operation.class);
    }

    @NonNull
    /* package */ Link delete(@NonNull final String url)
            throws IOException, ServerIOException {
        Response response = null;
        try {
            response = call(METHOD_DELETE, url);
            switch (response.code()) {
                case 202:
                    final Link result = parseJson(response, Link.class);
                    result.setHttpStatus(Link.HttpStatus.inProgress);
                    return result;
                case 204:
                    close(response);
                    return Link.DONE;
                default:
                    throw ErrorHandler.createHttpCodeException(response.code(),
                            response.body().byteStream());
            }
        } finally {
            close(response);
        }
    }

    @NonNull
    /* package */ Link put(@NonNull final String url)
            throws IOException, ServerIOException {
        Response response = null;
        try {
            response = call(METHOD_PUT, url);
            switch (response.code()) {
                case 201:
                    final Link done = parseJson(response, Link.class);
                    done.setHttpStatus(Link.HttpStatus.done);
                    return done;
                case 202:
                    final Link inProgress = parseJson(response, Link.class);
                    inProgress.setHttpStatus(Link.HttpStatus.inProgress);
                    return inProgress;
                default:
                    throw ErrorHandler.createHttpCodeException(response.code(),
                            response.body().byteStream());
            }
        } finally {
            close(response);
        }
    }

    private void close(@Nullable final Response response) {
        if (response == null) {
            return;
        }
        final ResponseBody responseBody = response.body();
        if (responseBody == null) {
            return;
        }
        responseBody.close();
    }

    @NonNull
    private Response call(@NonNull final String method, @NonNull final String url)
            throws IOException {
        final RequestBody body = HttpMethod.requiresRequestBody(method)
                ? RequestBody.create(MediaType.parse("text/plain"), "")
                : null;
        final Request request = new Request.Builder()
                .method(method, body)
                .url(url)
                .build();
        return client.newCall(request)
                .execute();
    }

    @NonNull
    private <T> T parseJson(@NonNull final Response response, @NonNull final Class<T> classOfT)
            throws IOException {
        ResponseBody responseBody = null;
        try {
            responseBody = response.body();
            final Gson gson = new Gson();
            return gson.fromJson(responseBody.charStream(), classOfT);
        } finally {
            if (responseBody != null) {
                responseBody.close();
            }
        }
    }

    private void cancel(Object tag) {
		for (Call call : client.dispatcher().runningCalls()) {
			if (tag.equals(call.request().tag())) call.cancel();
		}
	}

    private static class ContentRangeResponse {

        private final long start, size;

        ContentRangeResponse(long start, long size) {
            this.start = start;
            this.size = size;
        }

        long getStart() {
            return start;
        }

        long getSize() {
            return size;
        }
    }
}
