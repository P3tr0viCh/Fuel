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
import java.util.List;

class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "SyncAdapter";

    public static final String SYNC_DATABASE = "SYNC_DATABASE";
    public static final String SYNC_PREFERENCES = "SYNC_PREFERENCES";

    private SyncProviderDatabase mSyncProviderDatabase;
    private SyncProviderPreferences mSyncProviderPreferences;

    private SyncLocal mSyncLocal;
    private SyncYandexDisk mSyncYandexDisk;

    public SyncAdapter(Context context) {
        super(context, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        final String TAG2 = "onPerformSync";

        UtilsLog.d(TAG, TAG2, "start");

        try { // finally

            mSyncProviderDatabase = new SyncProviderDatabase(provider);
            mSyncProviderPreferences = new SyncProviderPreferences(provider);

            SyncAccount mSyncAccount = new SyncAccount(getContext());

            String yandexDiskToken = mSyncAccount.getYandexDiskToken();

            if (TextUtils.isEmpty(yandexDiskToken)) {
                syncResult.stats.numAuthExceptions++;

                UtilsLog.d(TAG, TAG2, "error  == empty Yandex.Disk token");

                return;
            }

            SyncFiles mSyncFiles = new SyncFiles(getContext());

            mSyncLocal = new SyncLocal(mSyncFiles);

            mSyncYandexDisk = new SyncYandexDisk(mSyncFiles, yandexDiskToken);

            try { // catch
                mSyncLocal.makeDirs();

                try {
                    if (extras.getBoolean(SYNC_DATABASE, true))
                        syncDatabase();

                    if (extras.getBoolean(SYNC_PREFERENCES, true))
                        syncPreferences();
                } finally {
                    mSyncLocal.deleteFiles();
                }
            } catch (Exception e) {
                if (e instanceof RemoteException) syncResult.databaseError = true;
                else if (e instanceof IOException) syncResult.stats.numIoExceptions++;
                else if (e instanceof FormatException) syncResult.stats.numParseExceptions++;
                else if (e instanceof HttpCodeException) {
                    if (((HttpCodeException) e).getCode() == SyncYandexDisk.HTTP_CODE_UNAUTHORIZED) {
                        syncResult.stats.numAuthExceptions++;
                        mSyncAccount.setYandexDiskToken(null);
                    } else
                        syncResult.stats.numIoExceptions++;
                } else if (e instanceof ServerIOException) syncResult.stats.numIoExceptions++;
                else if (e instanceof ServerException) syncResult.stats.numIoExceptions++;
                else syncResult.databaseError = true;

                UtilsLog.d(TAG, TAG2, "error  == " + e.toString());
            }
        } finally {
            try {
                mSyncProviderPreferences.putLastSync(System.currentTimeMillis(), syncResult.hasError());
            } catch (RemoteException e) {
                syncResult.databaseError = true;
            }

            UtilsLog.d(TAG, TAG2, "finish" +
                    (syncResult.hasError() ? ", errors == " + syncResult.toString() : ", all ok"));

            if (extras.getBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false)) syncResult.clear();

            mSyncYandexDisk = null;
            mSyncLocal = null;
            mSyncProviderPreferences = null;
            mSyncProviderDatabase = null;
        }
    }

    private void syncPreferences() throws
            IOException, ServerException, RemoteException, FormatException {
        final String TAG2 = "syncPreferences";

        UtilsLog.d(TAG, TAG2, "start");

        try {
            // Получить файл с номером ревизии с сервера и сохранить в папку кэша.
            // Если файла нет, игнорировать.

            mSyncYandexDisk.loadPreferencesRevision();

            UtilsLog.d(TAG, TAG2, "mSyncYandexDisk.loadPreferencesRevision() OK");

            int serverRevision = mSyncLocal.getPreferencesRevision();
            // serverRevision == -1, если синхронизация не производилась
            // или файлы синхронизации отсутствуют на сервере.

            int localRevision = mSyncProviderPreferences.getPreferencesRevision();
            // localRevision == -1, если программа запускается первый раз.

            boolean isChanged = mSyncProviderPreferences.isChanged();

            UtilsLog.d(TAG, TAG2,
                    "serverRevision == " + serverRevision + ", localRevision == " + localRevision +
                            ", preference changed == " + isChanged);

            if (localRevision < serverRevision) {
                // Синхронизация уже выполнялась на другом устройстве.
                // Текущие изменения теряются.

                UtilsLog.d(TAG, TAG2, "localRevision < serverRevision");

                syncPreferencesLoad(serverRevision);
            } else if (localRevision > serverRevision) {
                // Файлы синхронизации были удалены
                // (localRevision > -1 > serverRevision == -1).

                UtilsLog.d(TAG, TAG2, "localRevision > serverRevision");

                syncPreferencesSave(localRevision);
            } else /* localRevision == serverRevision */ {
                // 1. Сихронизация выполняется в первый раз
                // (localRevision == -1, serverRevision == -1, changed == true).
                // 2. Настройки синхронизированы.
                // Если настройки были изменены, сохранить их на сервер.

                UtilsLog.d(TAG, TAG2, "localRevision == serverRevision");

                if (isChanged) {
                    localRevision++;
                    syncPreferencesSave(localRevision);
                }
            }
        } finally {
            UtilsLog.d(TAG, TAG2, "finish");
        }
    }

    private void syncPreferencesSave(int revision)
            throws IOException, RemoteException, FormatException, ServerException {
        // 1) Сохранить настройки в файл в папке кэша.
        // 2) Сохранить номер ревизии в файл в папке кэша.
        // 3) Передать файл настроек из папки кэша на сервер.
        // 4) Передать файл с номером ревизии из папки кэша на сервер.
        // 5) Сохранить флаг изменения настроек и номер ревизии в настройках.

        final String TAG2 = "syncPreferencesSave";

        UtilsLog.d(TAG, TAG2, "start");

        List<String> preferences = mSyncProviderPreferences.getPreferences();
        UtilsLog.d(TAG, TAG2, "mSyncProviderPreferences.getPreferences() OK");

        mSyncLocal.savePreferences(preferences);
        UtilsLog.d(TAG, TAG2, "mSyncLocal.savePreferences() OK");

        mSyncLocal.savePreferencesRevision(revision);
        UtilsLog.d(TAG, "syncPreferencesSave", "mSyncLocal.savePreferencesRevision() OK");

        mSyncYandexDisk.makeDirs();
        UtilsLog.d(TAG, TAG2, "mSyncYandexDisk.makeDirs() OK");

        mSyncYandexDisk.savePreferences();
        UtilsLog.d(TAG, TAG2, "mSyncYandexDisk.savePreferences() OK");

        mSyncYandexDisk.savePreferencesRevision();
        UtilsLog.d(TAG, TAG2, "mSyncYandexDisk.savePreferencesRevision() OK");

        mSyncProviderPreferences.putChangedFalse();
        mSyncProviderPreferences.putPreferencesRevision(revision);

        UtilsLog.d(TAG, TAG2, "finish");
    }

    private void syncPreferencesLoad(int revision)
            throws IOException, RemoteException, ServerException {
        // 1) Получить файл настроек с сервера и сохранить в папку кэша.
        // 2) Прочитать значения из файла в папке кэша.
        // 3) Сохранить полученные значения в настройках.
        // 4) Сохранить флаг изменения настроек и номер ревизии в настройках.

        final String TAG2 = "syncPreferencesLoad";

        UtilsLog.d(TAG, TAG2, "start");

        mSyncYandexDisk.loadPreferences();
        UtilsLog.d(TAG, TAG2, "mSyncYandexDisk.loadPreferences() OK");

        List<String> preferences = new ArrayList<>();

        mSyncLocal.loadPreferences(preferences);
        UtilsLog.d(TAG, TAG2, "mSyncLocal.loadPreferences() OK");

        mSyncProviderPreferences.setPreferences(preferences);
        UtilsLog.d(TAG, TAG2, "mSyncProviderPreferences.setPreferences() OK");

        mSyncProviderPreferences.putChangedFalse();
        mSyncProviderPreferences.putPreferencesRevision(revision);

        UtilsLog.d(TAG, TAG2, "finish");
    }

    private void syncDatabase() throws
            IOException, ServerException, RemoteException, FormatException {
        final String TAG2 = "syncDatabase";

        UtilsLog.d(TAG, TAG2, "start");

        try {
            int localRevision = mSyncProviderPreferences.getDatabaseRevision();
            // localRevision == -1, если программа запускается первый раз.

            // Получить файл с номером ревизии с сервера и сохранить в папку кэша.
            // Если файла нет, игнорировать.
            mSyncYandexDisk.loadDatabaseRevision();
            UtilsLog.d(TAG, TAG2, "mSyncYandexDisk.loadDatabaseRevision() OK");

            int serverRevision = mSyncLocal.getDatabaseRevision();
            // serverRevision == -1, если синхронизация не производилась
            // или файлы синхронизации отсутствуют на сервере.

            UtilsLog.d(TAG, TAG2,
                    "serverRevision == " + serverRevision + ", localRevision == " + localRevision);

            // Проверить, что данные на этом устройстве были загружены из резервной копии.
            final boolean isFullSyncToServer = mSyncProviderPreferences.isDatabaseFullSync();

//            syncDatabaseSave(localRevision, false);
//            syncDatabaseLoad(-1, serverRevision);

            if (isFullSyncToServer) {
                UtilsLog.d(TAG, TAG2, "isFullSyncToServer == true");

                syncDatabaseFullSave(serverRevision);
            } else if (localRevision < serverRevision) {
                // Синхронизация уже выполнялась на другом устройстве.
                // Загрузить записи с сервера.
                // Если есть изменённые или удалённые записи, сохранить их на сервер.

                UtilsLog.d(TAG, TAG2, "localRevision < serverRevision");

                syncDatabaseLoad(localRevision, serverRevision);

                syncDatabaseSave(serverRevision, false, false);
            } else if (localRevision > serverRevision) {
                // Файлы синхронизации были удалены (localRevision > -1 > serverRevision == -1).
                // Сохранить все записи на сервер.

                UtilsLog.d(TAG, TAG2, "localRevision > serverRevision");

                syncDatabaseSave(localRevision, true, false);
            } else /* localRevision == serverRevision */ {
                // 1. Сихронизация выполняется в первый раз
                // (localRevision == -1, serverRevision == -1).
                // Сохранить все записи на сервер.
                // 2. БД синхронизирована.
                // Если есть изменённые или удалённые записи, сохранить их на сервер.

                UtilsLog.d(TAG, TAG2, "localRevision == serverRevision");

                syncDatabaseSave(localRevision, serverRevision == -1, false);
            }
        } finally {
            UtilsLog.d(TAG, TAG2, "finish");
        }
    }

    private void syncDatabaseFullSave(int revision)
            throws IOException, ServerException, RemoteException {
        // 1) Удалить все файлы с сервера.
        // 2) Сохранить полную копию БД с признаком полного обновления БД на сервер.
        // 3) Удалить признак загрузки из настроек.

        final String TAG2 = "syncDatabaseFullSave";

        UtilsLog.d(TAG, TAG2, "start");

        mSyncYandexDisk.deleteDirDatabase();
        UtilsLog.d(TAG, TAG2, "mSyncYandexDisk.deleteDirDatabase() OK");

        syncDatabaseSave(revision, true, true);

        mSyncProviderPreferences.putDatabaseFullSyncFalse();

        UtilsLog.d(TAG, TAG2, "finish");
    }

    private void syncDatabaseSave(int revision, boolean saveAllRecords, boolean addDeleteAll)
            throws IOException, RemoteException, ServerException {
        // 1) Сохранить БД в файл в папке кэша.
        // 1.1) Если "getAllRecords", выбрать все записи, иначе выбрать изменённые и удалённые записи.
        // 1.2) Если "addDeleteAll", добавить признак очистки БД перед добавлением записей с сервера.
        // 1.3) Если записи есть, увеличить номер ревизии на единицу и сохранить БД на сервер.
        // 2) Сохранить номер ревизии в файл в папке кэша.
        // 3) Передать файл БД из папки кэша на сервер.
        // 4) Передать файл с номером ревизии из папки кэша на сервер.
        // 5) Удалить записи, отмеченные как удалённые.
        // 6) Отметить изменённые записи как не изменённые.
        // 7) Сохранить номер ревизии БД в настройках.

        final String TAG2 = "syncDatabaseSave";

        UtilsLog.d(TAG, TAG2, "start");

        List<String> syncRecords = mSyncProviderDatabase.getSyncRecords(saveAllRecords, addDeleteAll);
        UtilsLog.d(TAG, TAG2, "mSyncProviderDatabase.getSyncRecords(saveAllRecords == " + saveAllRecords +
                ", addDeleteAll == " + addDeleteAll + ") OK");

        final int size = syncRecords.size();

        UtilsLog.d(TAG, TAG2, "records count == " + size);

//        for (String record : syncRecords) UtilsLog.d(TAG, record);

        if (size != 0) {

            revision++;

            mSyncLocal.saveDatabase(syncRecords);
            UtilsLog.d(TAG, TAG2, "mSyncLocal.saveDatabase() OK");

            mSyncLocal.saveDatabaseRevision(revision);
            UtilsLog.d(TAG, TAG2, "mSyncLocal.saveDatabaseRevision() OK");

            mSyncYandexDisk.makeDirs();
            UtilsLog.d(TAG, TAG2, "mSyncYandexDisk.makeDirs() OK");

            mSyncYandexDisk.saveDatabase(revision);
            UtilsLog.d(TAG, TAG2, "mSyncYandexDisk.saveDatabase() OK");

            mSyncYandexDisk.saveDatabaseRevision();
            UtilsLog.d(TAG, TAG2, "mSyncYandexDisk.saveDatabaseRevision() OK");

            mSyncProviderDatabase.syncDeletedRecords();
            UtilsLog.d(TAG, TAG2, "mSyncProviderDatabase.syncDeletedRecords() OK");

            mSyncProviderDatabase.syncChangedRecords();
            UtilsLog.d(TAG, TAG2, "mSyncProviderDatabase.syncChangedRecords() OK");

            mSyncProviderPreferences.putDatabaseRevision(revision);
        }

        UtilsLog.d(TAG, TAG2, "finish");
    }

    private void syncDatabaseLoad(int localRevision, int serverRevision)
            throws IOException, ServerException, RemoteException, FormatException {
        // 1) Получить файлы БД с сервера и сохранить в папку кэша.
        // 2) Прочитать записи из файлов в папке кэша.
        // 3) Сохранить полученные значения в БД.
        // 4) Сохранить номер ревизии БД в настройках.

        final String TAG2 = "syncDatabaseLoad";

        UtilsLog.d(TAG, TAG2, "start");

        List<String> syncRecords = new ArrayList<>();

        boolean loadResult;

        for (int revision = localRevision + 1; revision <= serverRevision; revision++) {

            loadResult = mSyncYandexDisk.loadDatabase(revision);
            UtilsLog.d(TAG, TAG2, "mSyncYandexDisk.loadDatabase(revision == " + revision +
                    ", loadResult == " + loadResult + ") OK");

            if (loadResult) {
                mSyncLocal.loadDatabase(syncRecords);
                UtilsLog.d(TAG, TAG2, "mSyncLocal.loadDatabase() OK");
            }
        }

//        for (String record : syncRecords) UtilsLog.d(TAG, record);

        mSyncProviderDatabase.updateDatabase(syncRecords);
        UtilsLog.d(TAG, TAG2, "mSyncProviderDatabase.updateDatabase() OK");

        mSyncProviderPreferences.putDatabaseRevision(serverRevision);

        ContentProviderHelper.notifyChangeAfterSync(getContext());

        UtilsLog.d(TAG, TAG2, "finish");
    }
}