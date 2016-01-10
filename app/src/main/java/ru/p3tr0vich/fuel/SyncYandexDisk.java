package ru.p3tr0vich.fuel;

import android.support.annotation.NonNull;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.exceptions.http.HttpCodeException;
import com.yandex.disk.rest.json.Link;

import java.io.File;
import java.io.IOException;

class SyncYandexDisk {
    public static final String WWW_URL = "https://disk.yandex.ru/client/disk";

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

    private void makeDir(@NonNull File dir) throws IOException, ServerIOException {
        try {
            mRestClient.makeFolder(APP_DIR + dir.getName());
        } catch (HttpCodeException e) {
            if (e.getCode() != HTTP_CODE_DIR_EXISTS) throw e;
        }
    }

    public void makeDirs() throws IOException, ServerIOException {
        makeDir(mSyncFiles.getServerDirDatabase());
        makeDir(mSyncFiles.getServerDirPreferences());
    }

    private void saveRevision(@NonNull File serverFile, @NonNull File localFile) throws IOException, ServerException {
        Link link = mRestClient.getUploadLink(APP_DIR + serverFile.getPath(), true);

        mRestClient.uploadFile(link, false, localFile, null);
    }

    public void savePreferencesRevision() throws IOException, ServerException {
        saveRevision(mSyncFiles.getServerFilePreferencesRevision(),
                mSyncFiles.getLocalFilePreferencesRevision());
    }

    private void loadRevision(@NonNull File serverFile, @NonNull File localFile) throws IOException, ServerException {
        try {
            mRestClient.downloadFile(APP_DIR + serverFile.getPath(), localFile, null);
        } catch (HttpCodeException e) {
            if (e.getCode() != HTTP_CODE_RESOURCE_NOT_FOUND) throw e;
        }
    }

    public void loadPreferencesRevision() throws IOException, ServerException {
        loadRevision(mSyncFiles.getServerFilePreferencesRevision(),
                mSyncFiles.getLocalFilePreferencesRevision());
    }

    public void loadDatabaseRevision() throws IOException, ServerException {
        loadRevision(mSyncFiles.getServerFileDatabaseRevision(),
                mSyncFiles.getLocalFileDatabaseRevision());
    }

    public void savePreferences() throws IOException, ServerException {
        Link link = mRestClient.getUploadLink(APP_DIR +
                mSyncFiles.getServerFilePreferences().getPath(), true);

        mRestClient.uploadFile(link, false, mSyncFiles.getLocalFilePreferences(), null);
    }

    public void loadPreferences() throws IOException, ServerException {
        mRestClient.downloadFile(APP_DIR + mSyncFiles.getServerFilePreferences().getPath(),
                mSyncFiles.getLocalFilePreferences(), null);
    }
}