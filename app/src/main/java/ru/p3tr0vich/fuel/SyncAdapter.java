package ru.p3tr0vich.fuel;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.nfc.FormatException;
import android.os.Bundle;
import android.os.RemoteException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public SyncAdapter(Context context) {
        super(context, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Functions.logD("SyncAdapter -- onPerformSync: start");

        SyncFiles syncFiles = new SyncFiles(getContext());
        SyncLocal syncLocal = new SyncLocal(syncFiles);
        SyncPreferencesAdapter syncPreferencesAdapter = new SyncPreferencesAdapter(provider);

        try {
//            for (int i = 0; i < 10; i++) {
//
//                try {
//                    TimeUnit.SECONDS.sleep(1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                Functions.logD("SyncAdapter -- onPerformSync: " + String.valueOf(i));
//            }

            // TODO:
            // Копируем файл с номером ревизии с сервера в папку кэша.
            // Если нет доступа к серверу syncResult.stats.numAuthExceptions++;.
            // Если файла нет, игнорируем.

            int serverRevision = syncLocal.getRevision();
//            serverRevision == -1, если синхронизация не производилась
//            или файлы синхронизации отсутствуют на сервере

            int localRevision = syncPreferencesAdapter.getRevision();
//            localRevision == 0, если программа запускается первый раз

            boolean isChanged = syncPreferencesAdapter.isChanged();

            Functions.logD("SyncAdapter -- onPerformSync: " +
                    "serverRevision == " + serverRevision + ", localRevision == " + localRevision +
                    ", preference changed == " + isChanged);

            save(syncLocal, syncPreferencesAdapter, 0);
//            load(syncLocal, syncPreferencesAdapter, 0);

        } catch (Exception e) {
            if (e instanceof RemoteException) syncResult.databaseError = true;
            else if (e instanceof IOException) syncResult.stats.numIoExceptions++;
            else if (e instanceof FormatException) syncResult.stats.numParseExceptions++;
//            else if (e instanceof AccountsException) syncResult.stats.numAuthExceptions++;
            else syncResult.databaseError = true;

            Functions.logD("SyncAdapter -- onPerformSync: error  == " + e.toString());
        } finally {
            try {
                syncPreferencesAdapter.putLastSync(syncResult.hasError() ? null : new Date());
            } catch (RemoteException e) {
                syncResult.databaseError = true;
            }

            Functions.logD("SyncAdapter -- onPerformSync: stop" +
                    (syncResult.hasError() ? ", errors == " + syncResult.toString() : ", all ok"));
        }
    }

    private void save(SyncLocal syncLocal, SyncPreferencesAdapter syncPreferencesAdapter, int revision)
            throws IOException, RemoteException, FormatException {
        // Сохранить настройки в файл в папке кэша
        // Сохранить номер ревизии в файл в папке кэша

        // Передать файл настроек из папки кэша на сервер
        // Передать файл с номером ревизии из папки кэша на сервер,

        Functions.logD("SyncAdapter -- save: start");

        List<String> preferences = syncPreferencesAdapter.getPreferences();

        Functions.logD("SyncAdapter -- save: syncPreferencesAdapter.getPreferences() OK");

        syncLocal.savePreferences(preferences);

        Functions.logD("SyncAdapter -- save: syncLocal.savePreferences() OK");

        syncLocal.saveRevision(revision);

        Functions.logD("SyncAdapter -- save: syncLocal.saveRevision() OK");

        // TODO:
/*        if (!ServerSyncHelper.savePreferences()) {
            Functions.logD("ServiceSync -- save: error ServerSyncHelper.savePreferences");
            return false;
        }

        if (!ServerSyncHelper.saveRevision()) {
            Functions.logD("ServiceSync -- save: error ServerSyncHelper.saveRevision");
            return false;
        }*/

        syncPreferencesAdapter.putChanged();
        syncPreferencesAdapter.putRevision(revision);
    }

    private void load(SyncLocal syncLocal, SyncPreferencesAdapter syncPreferencesAdapter, int revision)
            throws IOException, RemoteException {
        // Получить файл настроек с сервера и сохранить в папку кэша

        /*        if (!ServerSyncHelper.loadPreferences()) {
            Functions.logD("ServiceSync -- load: error ServerSyncHelper.loadPreferences");
            return false;
        } */

        List<String> preferences = new ArrayList<>();

        syncLocal.loadPreferences(preferences);

//        for (String s : preferences)
//            Functions.logD("SyncAdapter -- load: " + s);

        Functions.logD("SyncAdapter -- load: syncLocal.loadPreferences() OK");

        syncPreferencesAdapter.setPreferences(preferences);

        Functions.logD("SyncAdapter -- load: syncPreferencesAdapter.setPreferences() OK");

        syncPreferencesAdapter.putChanged();
        syncPreferencesAdapter.putRevision(revision);
    }
}