package ru.p3tr0vich.fuel;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
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

        try {

            try {
                Cursor cursor = provider.query(SyncProvider.URI, null, FuelingPreferenceManager.PREF_CHANGED, null, null);

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            Functions.logD("SyncAdapter -- onPerformSync: cursor == " +
                                    cursor.getString(0));
                        } while (cursor.moveToNext());
                    }
                    cursor.close();
                }
            } catch (RemoteException e) {
                syncResult.databaseError = true;
                e.printStackTrace();
                return;
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

            SyncFiles syncFiles = new SyncFiles(getContext());
            SyncLocal syncLocal = new SyncLocal(syncFiles);

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
            new SyncAccount(getContext()).setLastSync(syncResult.hasError() ? null : new Date());

            Functions.logD("SyncAdapter -- onPerformSync: stop" +
                    (syncResult.hasError() ? ", errors == " + syncResult.toString() : ", all ok"));
        }
    }

    private void save(SyncLocal syncLocal, int revision) throws IOException {
        // Сохранить настройки в файл в папке кэша
        // Сохранить номер ревизии в файл в папке кэша

        // Передать файл настроек из папки кэша на сервер
        // Передать файл с номером ревизии из папки кэша на сервер,

        syncLocal.savePreferences(FuelingPreferenceManager.getPreferences());

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

        FuelingPreferenceManager.putChanged(false);
        FuelingPreferenceManager.putRevision(revision);
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

        FuelingPreferenceManager.setPreferences(preferences);
        FuelingPreferenceManager.putChanged(false);
        FuelingPreferenceManager.putRevision(revision);
    }

    private void providerUpdate(ContentProviderClient provider, ContentValues contentValues) throws RemoteException {
        provider.update(SyncProvider.URI, contentValues, null, null);
    }

    private void providerSetPreferences(ContentProviderClient provider, List<String> preferences) throws RemoteException {
        ContentValues contentValues = new ContentValues();

        providerUpdate(provider, contentValues);
    }

    private void providerPutChanged(ContentProviderClient provider) throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(FuelingPreferenceManager.PREF_CHANGED, false);
        providerUpdate(provider, contentValues);
    }

    private void providerPutRevision(ContentProviderClient provider, int revision) throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(FuelingPreferenceManager.PREF_REVISION, revision);
        providerUpdate(provider, contentValues);
    }

    private List<String> providerGetPreferences(ContentProviderClient provider, String preference) throws RemoteException {
        Cursor cursor = provider.query(SyncProvider.URI, null, preference, null, null);

        List<String> result = new ArrayList<>();

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Functions.logD("SyncAdapter -- onPerformSync: cursor == " +
                            cursor.getString(0) + '=' + cursor.getString(1));
                    result.add(cursor.getString(0) + '=' + cursor.getString(1));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return result;
    }

    private int providerGetRevision(ContentProviderClient provider) throws RemoteException,
            NumberFormatException, IndexOutOfBoundsException {
        List<String> result = providerGetPreferences(provider, FuelingPreferenceManager.PREF_REVISION);
        return Integer.decode(result.get(0));
    }

    private boolean providerIsChanged(ContentProviderClient provider) throws RemoteException,
            NumberFormatException, IndexOutOfBoundsException {
        List<String> result = providerGetPreferences(provider, FuelingPreferenceManager.PREF_CHANGED);
        return Boolean.valueOf(result.get(0));
    }
}