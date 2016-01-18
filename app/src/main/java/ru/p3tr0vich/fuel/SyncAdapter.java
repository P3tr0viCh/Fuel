package ru.p3tr0vich.fuel;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
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

    public static final String SYNC_DATABASE = "SYNC_DATABASE";
    public static final String SYNC_PREFERENCES = "SYNC_PREFERENCES";

    public SyncAdapter(Context context) {
        super(context, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        UtilsLog.d(TAG, "onPerformSync", "start");

        SyncProviderDatabase syncProviderDatabase = new SyncProviderDatabase(provider);
        SyncProviderPreferences syncProviderPreferences = new SyncProviderPreferences(provider);

        SyncFiles syncFiles = new SyncFiles(getContext());
        SyncLocal syncLocal = new SyncLocal(syncFiles);
        SyncAccount syncAccount = new SyncAccount(getContext());

        String yandexDiskToken = syncAccount.getYandexDiskToken();

        SyncYandexDisk syncYandexDisk = new SyncYandexDisk(syncFiles, yandexDiskToken);

        try {
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

            if (extras.getBoolean(SYNC_DATABASE, true)) {
                UtilsLog.d(TAG, "onPerformSync", "******************");
                syncDatabase(syncProviderDatabase, syncProviderPreferences, syncLocal, syncYandexDisk);
            }

            if (extras.getBoolean(SYNC_PREFERENCES, true)) {
                UtilsLog.d(TAG, "onPerformSync", "******************");
                syncPreferences(syncProviderPreferences, syncLocal, syncYandexDisk);
            }

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

            UtilsLog.d(TAG, "onPerformSync", "******************");
            UtilsLog.d(TAG, "onPerformSync", "error  == " + e.toString());
        } finally {
            UtilsLog.d(TAG, "onPerformSync", "******************");
            try {
                syncProviderPreferences.putLastSync(syncResult.hasError() ? null : new Date());
            } catch (RemoteException e) {
                syncResult.databaseError = true;
            }

            UtilsLog.d(TAG, "onPerformSync", "stop" +
                    (syncResult.hasError() ? ", errors == " + syncResult.toString() : ", all ok"));

            if (extras.getBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false)) syncResult.clear();
        }
    }

    private void syncPreferences(SyncProviderPreferences syncProviderPreferences,
                                 SyncLocal syncLocal,
                                 SyncYandexDisk syncYandexDisk) throws
            IOException, ServerException, RemoteException, FormatException {
        UtilsLog.d(TAG, "syncPreferences", "start");

        // Копируем файл с номером ревизии с сервера в папку кэша.
        // Если файла нет, игнорируем.

        syncYandexDisk.loadPreferencesRevision();

        UtilsLog.d(TAG, "syncPreferences", "syncYandexDisk.loadPreferencesRevision() OK");

        int serverRevision = syncLocal.getPreferencesRevision();
        // serverRevision == -1, если синхронизация не производилась
        // или файлы синхронизации отсутствуют на сервере

        int localRevision = syncProviderPreferences.getPreferencesRevision();
        // localRevision == -1, если программа запускается первый раз

        boolean isChanged = syncProviderPreferences.isChanged();

        UtilsLog.d(TAG, "syncPreferences",
                "serverRevision == " + serverRevision + ", localRevision == " + localRevision +
                        ", preference changed == " + isChanged);

        if (localRevision < serverRevision) {
            // Синхронизация уже выполнялась на другом устройстве.
            // Текущие изменения теряются.

            UtilsLog.d(TAG, "syncPreferences", "localRevision < serverRevision");

            syncPreferencesLoad(syncLocal, syncYandexDisk, syncProviderPreferences, serverRevision);
        } else if (localRevision > serverRevision) {
            // Файлы синхронизации были удалены
            // (localRevision > -1 > serverRevision == -1).

            UtilsLog.d(TAG, "syncPreferences", "localRevision > serverRevision");

            syncPreferencesSave(syncLocal, syncYandexDisk, syncProviderPreferences, localRevision);
        } else /* localRevision == serverRevision */ {
            // 1. Сихронизация выполняется в первый раз
            // (localRevision == -1, serverRevision == -1, changed == true).
            // 2. Настройки синхронизированы.
            // Если настройки были изменены, сохранить их на сервер.

            UtilsLog.d(TAG, "syncPreferences", "localRevision == serverRevision");

            if (isChanged) {
                localRevision++;
                syncPreferencesSave(syncLocal, syncYandexDisk, syncProviderPreferences, localRevision);
            }
        }

        UtilsLog.d(TAG, "syncPreferences", "finish");
    }

    private void syncPreferencesSave(SyncLocal syncLocal, SyncYandexDisk syncYandexDisk,
                                     SyncProviderPreferences syncProviderPreferences, int revision)
            throws IOException, RemoteException, FormatException, ServerException {
        // 1) Сохранить настройки в файл в папке кэша.
        // 2) Сохранить номер ревизии в файл в папке кэша.
        // 3) Передать файл настроек из папки кэша на сервер.
        // 4) Передать файл с номером ревизии из папки кэша на сервер.
        // 5) Сохранить флаг изменения настроек и номер ревизии в настройках.

        UtilsLog.d(TAG, "syncPreferencesSave", "start");

        List<String> preferences = syncProviderPreferences.getPreferences();
        UtilsLog.d(TAG, "syncPreferencesSave", "syncProviderPreferences.getPreferences() OK");

        syncLocal.savePreferences(preferences);
        UtilsLog.d(TAG, "syncPreferencesSave", "syncLocal.savePreferences() OK");

        syncLocal.savePreferencesRevision(revision);
        UtilsLog.d(TAG, "syncPreferencesSave", "syncLocal.savePreferencesRevision() OK");

        syncYandexDisk.makeDirs();
        UtilsLog.d(TAG, "syncPreferencesSave", "syncYandexDisk.makeDirs() OK");

        syncYandexDisk.savePreferences();
        UtilsLog.d(TAG, "syncPreferencesSave", "syncYandexDisk.savePreferences() OK");

        syncYandexDisk.savePreferencesRevision();
        UtilsLog.d(TAG, "syncPreferencesSave", "syncYandexDisk.savePreferencesRevision() OK");

        syncProviderPreferences.putChangedFalse();
        syncProviderPreferences.putPreferencesRevision(revision);

        UtilsLog.d(TAG, "syncPreferencesSave", "finish");
    }

    private void syncPreferencesLoad(SyncLocal syncLocal, SyncYandexDisk syncYandexDisk,
                                     SyncProviderPreferences syncProviderPreferences, int revision)
            throws IOException, RemoteException, ServerException {
        // 1) Получить файл настроек с сервера и сохранить в папку кэша.
        // 2) Прочитать значения из файла в папке кэша.
        // 3) Сохранить полученные значения в настройках.
        // 4) Сохранить флаг изменения настроек и номер ревизии в настройках.

        UtilsLog.d(TAG, "syncPreferencesLoad", "start");

        syncYandexDisk.loadPreferences();
        UtilsLog.d(TAG, "syncPreferencesLoad", "syncYandexDisk.loadPreferences() OK");

        List<String> preferences = new ArrayList<>();

        syncLocal.loadPreferences(preferences);
        UtilsLog.d(TAG, "syncPreferencesLoad", "syncLocal.loadPreferences() OK");

        syncProviderPreferences.setPreferences(preferences);
        UtilsLog.d(TAG, "syncPreferencesLoad", "syncProviderPreferences.setPreferences() OK");

        syncProviderPreferences.putChangedFalse();
        syncProviderPreferences.putPreferencesRevision(revision);

        UtilsLog.d(TAG, "syncPreferencesLoad", "finish");
    }

    private void syncDatabase(SyncProviderDatabase syncProviderDatabase,
                              SyncProviderPreferences syncProviderPreferences,
                              SyncLocal syncLocal,
                              SyncYandexDisk syncYandexDisk) throws
            IOException, ServerException, RemoteException, FormatException {
        UtilsLog.d(TAG, "syncDatabase", "start");

        // Данные были загружены из резервной копии
        if (syncProviderPreferences.isFullSync()) {
            syncDatabaseFullSave(syncLocal, syncYandexDisk, syncProviderDatabase, syncProviderPreferences);
            return;
        }

        // Копируем файл с номером ревизии с сервера в папку кэша.
        // Если файла нет, игнорируем.

        syncYandexDisk.loadDatabaseRevision();

        UtilsLog.d(TAG, "syncDatabase", "syncYandexDisk.loadDatabaseRevision() OK");

        int serverRevision = syncLocal.getDatabaseRevision();
        // serverRevision == -1, если синхронизация не производилась
        // или файлы синхронизации отсутствуют на сервере

        int localRevision = syncProviderPreferences.getDatabaseRevision();
        // localRevision == -1, если программа запускается первый раз

        UtilsLog.d(TAG, "syncDatabase",
                "serverRevision == " + serverRevision + ", localRevision == " + localRevision);

//        syncDatabaseSave(syncLocal, syncYandexDisk, syncProviderDatabase, syncProviderPreferences, 1, false);
//        syncDatabaseLoad(syncLocal, syncYandexDisk, syncProviderDatabase, syncProviderPreferences, -1, 1);

        if (localRevision < serverRevision) {
            // Синхронизация уже выполнялась на другом устройстве.
            // Загрузить записи с сервера.
            // TODO:  Если есть изменённые или удалённые записи, сохранить их на сервер.

            UtilsLog.d(TAG, "syncDatabase", "localRevision < serverRevision");

//            syncPreferencesLoad(syncLocal, syncYandexDisk, syncProviderPreferences, serverRevision);
        } else if (localRevision > serverRevision) {
            // Файлы синхронизации были удалены
            // (localRevision > -1 > serverRevision == -1).
            // Сохранить все записи на сервер.

            UtilsLog.d(TAG, "syncDatabase", "localRevision > serverRevision");

//            syncDatabaseSave(syncLocal, syncYandexDisk, syncProviderDatabase, syncProviderPreferences,
//                    localRevision, true);
        } else /* localRevision == serverRevision */ {
            // 1. Сихронизация выполняется в первый раз
            // (localRevision == -1, serverRevision == -1).
            // Сохранить все записи на сервер.
            // 2. БД синхронизирована.
            // Если есть изменённые или удалённые записи, сохранить их на сервер.

            UtilsLog.d(TAG, "syncDatabase", "localRevision == serverRevision");

//            if (localRevision == -1) localRevision = 0;

//            syncDatabaseSave(syncLocal, syncYandexDisk, syncProviderDatabase, syncProviderPreferences,
//                    localRevision, serverRevision == -1);
        }

        UtilsLog.d(TAG, "syncDatabase", "finish");
    }

    private void syncDatabaseFullSave(SyncLocal syncLocal, SyncYandexDisk syncYandexDisk,
                                      SyncProviderDatabase syncProviderDatabase,
                                      SyncProviderPreferences syncProviderPreferences)
            throws IOException, ServerException, RemoteException {
        // 1) Удаляем все файлы с сервера.
        // 2) Сохраняем файл-признак полного обновления  в папке кэша.
        // 3) Передать файл-признак из папки кэша на сервер.
        // 3) Сохраняем все записи на сервер.
        // 5) Удалить признак полного обновления из настроек.
        // 4) Изменить номер ревизии БД на 0 и сохранить в настройках.

        UtilsLog.d(TAG, "syncDatabaseFullSave", "start");

        syncYandexDisk.deleteDirDatabase();
        UtilsLog.d(TAG, "syncDatabaseFullSave", "syncYandexDisk.deleteDirDatabase() OK");

        syncLocal.saveDatabaseFullSync();
        UtilsLog.d(TAG, "syncDatabaseFullSave", "syncLocal.saveDatabaseFullSync() OK");

        syncYandexDisk.makeDirs();
        UtilsLog.d(TAG, "syncDatabaseFullSave", "syncYandexDisk.makeDirs() OK");

        syncYandexDisk.saveDatabaseFullSync();
        UtilsLog.d(TAG, "syncDatabaseFullSave", "syncYandexDisk.saveDatabaseFullSync() OK");

        syncDatabaseSave(syncLocal, syncYandexDisk, syncProviderDatabase, syncProviderPreferences,
                0, true);

        syncProviderPreferences.putFullSyncFalse();
        syncProviderPreferences.putDatabaseRevision(0);

        UtilsLog.d(TAG, "syncDatabaseFullSave", "finish");
    }

    private void syncDatabaseSave(SyncLocal syncLocal, SyncYandexDisk syncYandexDisk,
                                  SyncProviderDatabase syncProviderDatabase,
                                  SyncProviderPreferences syncProviderPreferences,
                                  int revision, boolean fullSave)
            throws IOException, RemoteException, ServerException {
        // 1) Сохранить БД в файл в папке кэша.
        // 1.1) Выбрать изменённые и удалённые записи или, если fullSave, выбрать все записи.
        // 2) Сохранить номер ревизии в файл в папке кэша.
        // 3) Передать файл БД из папки кэша на сервер.
        // 4) Передать файл с номером ревизии из папки кэша на сервер.
        // 5) Удалить записи, отмеченные как удалённые.
        // 6) Отметить изменённые записи как не изменённые.
        // 7) Сохранить номер ревизии БД в настройках.

        UtilsLog.d(TAG, "syncDatabaseSave", "start");

        List<String> syncRecords = syncProviderDatabase.getSyncRecords(fullSave);
        UtilsLog.d(TAG, "syncDatabaseSave", "syncProviderDatabase.getSyncRecords(fullSave == " + fullSave + ") OK");
        UtilsLog.d(TAG, "syncDatabaseSave", "records count == " + syncRecords.size());

//        for (String record : syncRecords) UtilsLog.d(TAG, record);

        if (!syncRecords.isEmpty()) {
            syncLocal.saveDatabase(syncRecords);
            UtilsLog.d(TAG, "syncDatabaseSave", "syncLocal.saveDatabase() OK");

            syncLocal.saveDatabaseRevision(revision);
            UtilsLog.d(TAG, "syncDatabaseSave", "syncLocal.saveDatabaseRevision() OK");

            syncYandexDisk.makeDirs();
            UtilsLog.d(TAG, "syncDatabaseSave", "syncYandexDisk.makeDirs() OK");

            syncYandexDisk.saveDatabase(revision);
            UtilsLog.d(TAG, "syncDatabaseSave", "syncYandexDisk.saveDatabase() OK");

            syncYandexDisk.saveDatabaseRevision();
            UtilsLog.d(TAG, "syncDatabaseSave", "syncYandexDisk.saveDatabaseRevision() OK");

            syncProviderDatabase.syncDeletedRecords();
            UtilsLog.d(TAG, "syncDatabaseSave", "syncProviderDatabase.syncDeletedRecords() OK");

            syncProviderDatabase.syncChangedRecords();
            UtilsLog.d(TAG, "syncDatabaseSave", "syncProviderDatabase.syncChangedRecords() OK");

            syncProviderPreferences.putDatabaseRevision(revision);
        } else
            UtilsLog.d(TAG, "syncDatabaseSave", "syncRecords.isEmpty() == true");

        UtilsLog.d(TAG, "syncDatabaseSave", "finish");
    }

    private void syncDatabaseLoad(SyncLocal syncLocal, SyncYandexDisk syncYandexDisk,
                                  SyncProviderDatabase syncProviderDatabase,
                                  SyncProviderPreferences syncProviderPreferences,
                                  int localRevision, int serverRevision)
            throws IOException, ServerException, RemoteException, FormatException {
        // 1) Получить файл БД с сервера и сохранить в папку кэша.
        // 2) Прочитать записи из файла в папке кэша.
        // 3) Сохранить полученные значения в БД.
        // 4) Сохранить флаг изменения настроек.
        // 5) Сохранить номер ревизии БД в настройках.

        UtilsLog.d(TAG, "syncDatabaseLoad", "start");

        List<String> syncRecords = new ArrayList<>();

        for (int revision = localRevision + 1; revision <= serverRevision; revision++) {

            syncYandexDisk.loadDatabase(revision);
            UtilsLog.d(TAG, "syncDatabaseLoad", "syncYandexDisk.loadDatabase(revision == " + revision + ") OK");

            syncLocal.loadDatabase(syncRecords);
            UtilsLog.d(TAG, "syncDatabaseLoad", "syncLocal.loadDatabase() OK");

            // Если не удалять, будет HttpCodeException{code=416, response=null}
            // в syncYandexDisk.loadDatabase(revision)
            syncLocal.deleteLocalFileDatabase();
        }

//        for (String record : syncRecords) UtilsLog.d(TAG, record);

        syncProviderDatabase.updateDatabase(syncRecords);
        UtilsLog.d(TAG, "syncDatabaseLoad", "syncProviderDatabase.updateDatabase() OK");

//        syncProviderPreferences.putChanged();
//        syncProviderPreferences.putPreferencesRevision(revision);

        UtilsLog.d(TAG, "syncDatabaseLoad", "finish");
    }
}