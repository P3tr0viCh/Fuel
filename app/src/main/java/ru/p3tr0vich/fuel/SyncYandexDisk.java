package ru.p3tr0vich.fuel;

import android.support.annotation.NonNull;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.exceptions.http.HttpCodeException;

import java.io.IOException;

class SyncYandexDisk {
    private static final String CLIENT_ID = "5c1778f5bd58490a9ea471a150f25550";
//    private static final String CLIENT_SECRET = "b7771c64e8a54741bf71d8b679ffd44a";

    public static final String AUTH_URL = "https://oauth.yandex.ru/authorize?response_type=token&client_id=" + CLIENT_ID;

    public static final String PATTERN_ACCESS_TOKEN = "access_token=(.*?)(&|$)";

    private static final String APP_DIR = "app:/";

    private final SyncFiles mSyncFiles;

    private final RestClient mRestClient;

    SyncYandexDisk(@NonNull SyncFiles syncFiles, String token) {
        mSyncFiles = syncFiles;
        mRestClient = new RestClient(new Credentials("", token));
    }

    public void makeDirPreferences() throws IOException, ServerIOException {
        try {
            mRestClient.makeFolder(APP_DIR + mSyncFiles.getServerDirPreferences().getName());
        } catch (HttpCodeException e) {
            if (e.getCode() != 409) // 409 -- dir exists
                throw e;
        }
    }

    public void savePreferences() {

    }

    public void saveRevision() {

    }
}
