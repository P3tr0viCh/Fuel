package ru.p3tr0vich.fuel;

import android.support.annotation.NonNull;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.exceptions.http.HttpCodeException;
import com.yandex.disk.rest.json.Link;

import java.io.IOException;

class SyncYandexDisk {
    public static final String AUTH_URL = "https://oauth.yandex.ru/authorize?response_type=token&client_id=" +
            SyncYandexDiskSecret.CLIENT_ID;

    public static final String PATTERN_ACCESS_TOKEN = "access_token=(.*?)(&|$)";

    private static final String APP_DIR = "app:/";

    public static final int HTTP_CODE_UNAUTHORIZED = 401;
    private static final int HTTP_CODE_RESOURCE_NOT_FOUND = 404;
    private static final int HTTP_CODE_DIR_EXISTS = 409;

    private final SyncFiles mSyncFiles;

    private final RestClient mRestClient;

    SyncYandexDisk(@NonNull SyncFiles syncFiles, String token) {
        mSyncFiles = syncFiles;
        mRestClient = new RestClient(new Credentials("", token));
    }

    public void makeDirs() throws IOException, ServerIOException {
        try {
            mRestClient.makeFolder(APP_DIR + mSyncFiles.getServerDirPreferences().getName());
        } catch (HttpCodeException e) {
            if (e.getCode() != HTTP_CODE_DIR_EXISTS) throw e;
        }
    }

    public void savePreferences() throws IOException, ServerException {
        Link link = mRestClient.getUploadLink(APP_DIR +
                mSyncFiles.getServerFilePreferences().getPath(), true);

        mRestClient.uploadFile(link, false, mSyncFiles.getLocalFilePreferences(), null);
    }

    public void saveRevision() throws IOException, ServerException {
        Link link = mRestClient.getUploadLink(APP_DIR +
                mSyncFiles.getServerFilePreferencesRevision().getPath(), true);

        mRestClient.uploadFile(link, false, mSyncFiles.getLocalFilePreferencesRevision(), null);
    }

    public void loadPreferences() throws IOException, ServerException {
        mRestClient.downloadFile(APP_DIR + mSyncFiles.getServerFilePreferences().getPath(),
                mSyncFiles.getLocalFilePreferences(), null);
    }

    public void loadRevision() throws IOException, ServerException {
        try {
            mRestClient.downloadFile(APP_DIR + mSyncFiles.getServerFilePreferencesRevision().getPath(),
                    mSyncFiles.getLocalFilePreferencesRevision(), null);
        } catch (HttpCodeException e) {
            if (e.getCode() != HTTP_CODE_RESOURCE_NOT_FOUND) throw e;
        }
    }
}
