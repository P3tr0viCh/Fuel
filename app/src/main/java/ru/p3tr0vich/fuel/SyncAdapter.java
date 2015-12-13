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

            try {
                Functions.logD("SyncAdapter -- onPerformSync: preferences is changed == " +
                        syncPreferencesAdapter.isChanged());

                Functions.logD("SyncAdapter -- onPerformSync: preferences put changed");

                syncPreferencesAdapter.putChanged();

                Functions.logD("SyncAdapter -- onPerformSync: preferences is changed == " +
                        syncPreferencesAdapter.isChanged());

                syncPreferencesAdapter.getPreferences();

            } catch (RemoteException | FormatException e) {
                syncResult.databaseError = true;
            }

//            Random random = new Random();
//
//            if (random.nextBoolean()) {
//                Functions.logD("SyncAdapter -- onPerformSync: error auth");
//
//                syncResult.stats.numAuthExceptions++;
//
//                return;
//            }
//
//            if (random.nextBoolean()) {
//                Functions.logD("SyncAdapter -- onPerformSync: error IO");
//
//                syncResult.stats.numIoExceptions++;
//
//                return;
//            }
//
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

//            int serverRevision = syncLocal.getRevision();
            // serverRevision == -1, если синхронизация не производилась
            // или файлы синхронизации отсутствуют на сервере

//            int localRevision = FuelingPreferenceManager.getRevision();
            // localRevision == 0, если программа запускается первый раз

//            Functions.logD("SyncAdapter -- onPerformSync: serverRevision == " +
//                    serverRevision + ", localRevision == " + localRevision);
//
//            Functions.logD("SyncAdapter -- onPerformSync: FuelingPreferenceManager.isChanged == " +
//                    FuelingPreferenceManager.isChanged());

//            try {
//                save(syncLocal, 0);
//                load(syncLocal, 0);
//            } catch (IOException e) {
//                syncResult.stats.numIoExceptions++;
//            }
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

    private void save(SyncLocal syncLocal, int revision) throws IOException {
        // Сохранить настройки в файл в папке кэша
        // Сохранить номер ревизии в файл в папке кэша

        // Передать файл настроек из папки кэша на сервер
        // Передать файл с номером ревизии из папки кэша на сервер,

//        syncLocal.savePreferences(FuelingPreferenceManager.getPreferences());

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

//        FuelingPreferenceManager.putChanged(false);
//        FuelingPreferenceManager.putRevision(revision);
    }

    private void load(SyncLocal syncLocal, int revision) throws IOException {
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

//        FuelingPreferenceManager.setPreferences(preferences);
//        FuelingPreferenceManager.putChanged(false);
//        FuelingPreferenceManager.putRevision(revision);
    }
}