package ru.p3tr0vich.fuel;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.nfc.FormatException;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.exceptions.http.HttpCodeException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "SyncAdapter";

    public SyncAdapter(Context context) {
        super(context, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        UtilsLog.d(TAG, "onPerformSync", "start");

        SyncPreferencesAdapter syncPreferencesAdapter = new SyncPreferencesAdapter(provider);
        SyncFiles syncFiles = new SyncFiles(getContext());
        SyncLocal syncLocal = new SyncLocal(syncFiles);
        SyncAccount syncAccount = new SyncAccount(getContext());

        String yandexDiskToken = syncAccount.getYandexDiskToken();

        SyncYandexDisk syncYandexDisk = new SyncYandexDisk(syncFiles, yandexDiskToken);

        try {
            // TODO // FIXME: 23.12.2015

//            for (int i = 0; i < 10; i++) {
//
//                TimeUnit.SECONDS.sleep(1);
//
//                UtilsLog.d(TAG, "onPerformSync", String.valueOf(i));
//            }
//
//            if (true) return;

            if (TextUtils.isEmpty(yandexDiskToken)) {
                syncResult.stats.numAuthExceptions++;
                UtilsLog.d(TAG, "onPerformSync", "error  == empty Yandex.Disk token");
                return;
            }

            syncLocal.makeDirs();
            syncLocal.deleteFiles();

            // Копируем файл с номером ревизии с сервера в папку кэша.
            // Если файла нет, игнорируем.

            syncYandexDisk.loadRevision();

            UtilsLog.d(TAG, "onPerformSync", "syncYandexDisk.loadRevision() OK");

            int serverRevision = syncLocal.getRevision();
//            serverRevision == -1, если синхронизация не производилась
//            или файлы синхронизации отсутствуют на сервере

            int localRevision = syncPreferencesAdapter.getRevision();
//            localRevision == -1, если программа запускается первый раз

            boolean isChanged = syncPreferencesAdapter.isChanged();

            UtilsLog.d(TAG, "onPerformSync",
                    "serverRevision == " + serverRevision + ", localRevision == " + localRevision +
                            ", preference changed == " + isChanged);

            if (localRevision < serverRevision) {
                // Синхронизация уже выполнялась на другом устройстве.
                // Текущие изменения теряются.

                UtilsLog.d(TAG, "onPerformSync", "localRevision < serverRevision");

                load(syncLocal, syncYandexDisk, syncPreferencesAdapter);

                localRevision = serverRevision;
            } else if (localRevision > serverRevision) {
                // Файлы синхронизации были удалены
                // (localRevision > -1 > serverRevision == -1).

                UtilsLog.d(TAG, "onPerformSync", "localRevision > serverRevision");

                save(syncLocal, syncYandexDisk, syncPreferencesAdapter, localRevision);
            } else /* localRevision == serverRevision */ {
                // 1. Сихронизация выполняется в первый раз
                // (localRevision == -1, serverRevision == -1, changed == true).
                // 2. Настройки синхронизированы.
                // Если настройки были изменены, сохранить их на сервер.

                UtilsLog.d(TAG, "onPerformSync", "localRevision == serverRevision");

                if (isChanged) {
                    localRevision++;
                    save(syncLocal, syncYandexDisk, syncPreferencesAdapter, localRevision);
                }
            }

            syncPreferencesAdapter.putChanged();
            syncPreferencesAdapter.putRevision(localRevision);

            syncLocal.deleteFiles();
        } catch (Exception e) {
            if (e instanceof RemoteException) syncResult.databaseError = true;
            else if (e instanceof IOException) syncResult.stats.numIoExceptions++;
            else if (e instanceof FormatException) syncResult.stats.numParseExceptions++;
            else if (e instanceof HttpCodeException) {
                if (((HttpCodeException) e).getCode() == SyncYandexDisk.HTTP_CODE_UNAUTHORIZED) {
                    syncResult.stats.numAuthExceptions++;
                    syncAccount.setYandexDiskToken(null);
                } else
                    syncResult.stats.numIoExceptions++;
            } else if (e instanceof ServerIOException) syncResult.stats.numIoExceptions++;
            else if (e instanceof ServerException) syncResult.stats.numIoExceptions++;
            else syncResult.databaseError = true;

            UtilsLog.d(TAG, "onPerformSync", "error  == " + e.toString());
        } finally {
            try {
                syncPreferencesAdapter.putLastSync(syncResult.hasError() ? null : new Date());
            } catch (RemoteException e) {
                syncResult.databaseError = true;
            }

            UtilsLog.d(TAG, "onPerformSync", "stop" +
                    (syncResult.hasError() ? ", errors == " + syncResult.toString() : ", all ok"));
        }
    }

    private void save(SyncLocal syncLocal, SyncYandexDisk syncYandexDisk,
                      SyncPreferencesAdapter syncPreferencesAdapter, int revision)
            throws IOException, RemoteException, FormatException, ServerException {
        // 1) Сохранить настройки в файл в папке кэша.
        // 2) Сохранить номер ревизии в файл в папке кэша.
        // 3) Передать файл настроек из папки кэша на сервер.
        // 4) Передать файл с номером ревизии из папки кэша на сервер.
        // 5) Сохранить флаг изменения настроек и номер ревизии в настройках.

        UtilsLog.d(TAG, "save", "start");

        List<String> preferences = syncPreferencesAdapter.getPreferences();
        UtilsLog.d(TAG, "save", "syncPreferencesAdapter.getPreferences() OK");

        syncLocal.savePreferences(preferences);
        UtilsLog.d(TAG, "save", "syncLocal.savePreferences() OK");

        syncLocal.saveRevision(revision);
        UtilsLog.d(TAG, "save", "syncLocal.saveRevision() OK");

        syncYandexDisk.makeDirs();
        UtilsLog.d(TAG, "save", "syncYandexDisk.makeDirs() OK");

        syncYandexDisk.savePreferences();
        UtilsLog.d(TAG, "save", "syncYandexDisk.savePreferences() OK");

        syncYandexDisk.saveRevision();
        UtilsLog.d(TAG, "save", "syncYandexDisk.saveRevision() OK");
    }

    private void load(SyncLocal syncLocal, SyncYandexDisk syncYandexDisk,
                      SyncPreferencesAdapter syncPreferencesAdapter)
            throws IOException, RemoteException, ServerException {
        // 1) Получить файл настроек с сервера и сохранить в папку кэша.
        // 2) Прочитать настройки из файла в папке кэша.
        // 3) Сохранить полученные значения в настройках.
        // 4) Сохранить флаг изменения настроек и номер ревизии в настройках.

        syncYandexDisk.loadPreferences();
        UtilsLog.d(TAG, "load", "syncYandexDisk.loadPreferences() OK");

        List<String> preferences = new ArrayList<>();

        syncLocal.loadPreferences(preferences);
//        for (String s : preferences) Functions.logD("SyncAdapter -- load: " + s);
        UtilsLog.d(TAG, "load", "syncLocal.loadPreferences() OK");

        syncPreferencesAdapter.setPreferences(preferences);
        UtilsLog.d(TAG, "load", "syncPreferencesAdapter.setPreferences() OK");
    }
}