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

    private void save(@NonNull File serverFile, @NonNull File localFile) throws IOException, ServerException {
        Link link = mRestClient.getUploadLink(APP_DIR + serverFile.getPath(), true);

        mRestClient.uploadFile(link, false, localFile, null);
    }

    private void load(@NonNull File serverFile, @NonNull File localFile) throws IOException, ServerException {
        UtilsFileIO.deleteFile(localFile);

        mRestClient.downloadFile(APP_DIR + serverFile.getPath(), localFile, null);
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

    private void delete(@NonNull File object) throws IOException, ServerException {
        try {
            Link link = mRestClient.delete(APP_DIR + object.getPath(), true);
            if ("GET".equals(link.getMethod()))
                mRestClient.waitProgress(link, new Runnable() {
                    @Override
                    public void run() {
                        UtilsLog.d("SyncYandexDisk", "wait until deleting object...");
                    }
                });
        } catch (HttpCodeException e) {
            if (e.getCode() != HTTP_CODE_RESOURCE_NOT_FOUND) throw e;
        }
    }

    public void deleteDirDatabase() throws IOException, ServerException {
        delete(mSyncFiles.getServerDirDatabase());
    }

    public void savePreferencesRevision() throws IOException, ServerException {
        save(mSyncFiles.getServerFilePreferencesRevision(),
                mSyncFiles.getLocalFilePreferencesRevision());
    }

    public void saveDatabaseRevision() throws IOException, ServerException {
        save(mSyncFiles.getServerFileDatabaseRevision(),
                mSyncFiles.getLocalFileDatabaseRevision());
    }

    private void loadRevision(@NonNull File serverFile, @NonNull File localFile) throws IOException, ServerException {
        try {
            load(serverFile, localFile);
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

    public void saveDatabase(int revision) throws IOException, ServerException {
        save(mSyncFiles.getServerFileDatabase(revision), mSyncFiles.getLocalFileDatabase());
    }

    public boolean loadDatabase(int revision) throws IOException, ServerException {
        try {
            load(mSyncFiles.getServerFileDatabase(revision), mSyncFiles.getLocalFileDatabase());
            return true;
        } catch (HttpCodeException e) {
            if (e.getCode() != HTTP_CODE_RESOURCE_NOT_FOUND) throw e;
            return false;
        }
    }

    public void savePreferences() throws IOException, ServerException {
        save(mSyncFiles.getServerFilePreferences(), mSyncFiles.getLocalFilePreferences());
    }

    public void loadPreferences() throws IOException, ServerException {
        load(mSyncFiles.getServerFilePreferences(), mSyncFiles.getLocalFilePreferences());
    }
}