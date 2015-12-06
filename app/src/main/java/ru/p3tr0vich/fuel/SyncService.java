package ru.p3tr0vich.fuel;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SyncService extends Service {

    public static final int REQUEST_CODE = 3619;

    public static final int STATUS_START = 100;
    public static final int STATUS_FINISH = 200;

    public static final String EXTRA_START = "ru.p3tr0vich.fuel.EXTRA_START";
    public static final String EXTRA_PENDING = "ru.p3tr0vich.fuel.EXTRA_PENDING";

    private PendingIntent mPendingIntent;

    private static boolean mSyncInProcess = false;
    private static boolean mErrorInProcess = false;

    public SyncService() {
    }

    public static boolean isSyncInProcess() {
        return mSyncInProcess;
    }

    public static boolean isErrorInProcess() {
        return mErrorInProcess;
    }

    @Override
    public void onCreate() {
        Functions.logD("SyncService -- onCreate");
    }

    @Override
    public void onDestroy() {
        Functions.logD("SyncService -- onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Functions.logD("SyncService -- onStartCommand: startId == " + startId);

        mPendingIntent = intent.getParcelableExtra(EXTRA_PENDING);

        if (!mSyncInProcess && intent.getBooleanExtra(EXTRA_START, true)) startSync();

        return super.onStartCommand(intent, flags, startId);
    }

    private void startSync() {
        new Thread(new Runnable() {
            public void run() {
                mSyncInProcess = true;
                mErrorInProcess = false;

                try {
                    mPendingIntent.send(STATUS_START);

//                        if (new Random().nextBoolean()) {
//                            Functions.logD("ServiceSync -- doInBackground: error");
//                            mErrorInProcess = true;
//                        }

                    SyncFiles syncFiles = new SyncFiles(SyncService.this);
                    SyncLocal syncLocal = new SyncLocal(syncFiles);

                    // TODO:
                    // Копируем файл с номером ревизии с сервера в папку кэша.
                    // Если нет доступа к серверу mErrorInProcess == true.
                    // Если файла нет, игнорируем.

                    if (!mErrorInProcess) {

                        int serverRevision = syncLocal.getRevision();
                        // serverRevision == -1, если синхронизация не производилась
                        // или файлы синхронизации отсутствуют на сервере

                        int localRevision = FuelingPreferenceManager.getRevision();
                        // localRevision == 0, если программа запускается первый раз

                        Functions.logD("SyncService -- doInBackground: serverRevision == " +
                                serverRevision + ", localRevision == " + localRevision);

                        mErrorInProcess = !save(syncLocal, 0);
//                        mErrorInProcess = !load(syncLocal, 0);

/*                            if (localRevision < serverRevision) {
                                // Синхронизация уже выполнялась на другом устройстве.
                                // Текущие изменения теряются.

                                Functions.logD("ServiceSync -- doInBackground: localRevision < serverRevision");

                                if (load(syncLocal)) {
                                } else
                                    mErrorInProcess = true;
                            } else if (localRevision > serverRevision) {
                                // 1. Сихронизация выполняется в первый раз
                                // (localRevision == 0 > serverRevision == -1).
                                // 2. Файлы синхронизации были удалены
                                // (localRevision > 0 > serverRevision == -1).

                                Functions.logD("ServiceSync -- doInBackground: localRevision > serverRevision");

                                if (save(syncLocal, localRevision))
                                else
                                    mErrorInProcess = true;
                            } else { // localRevision == serverRevision
                                // Синхронизация была выполнена.
                                // Если настройки были изменены, сохранить их на сервер.

                                Functions.logD("ServiceSync -- doInBackground: localRevision == serverRevision");

                                if (FuelingPreferenceManager.isChanged()) {
                                    Functions.logD("ServiceSync -- doInBackground: isChanged == true");

                                    localRevision++;

                                    if (save(syncLocal, localRevision)) {
                                    } else
                                        mErrorInProcess = true;
                                } else
                                    Functions.logD("ServiceSync -- doInBackground: isChanged == false");
                            }*/

/*                        for (int i = 0; i < 10; i++) {

                            TimeUnit.SECONDS.sleep(1);

                            Functions.logD("ServiceSync -- doInBackground: " + String.valueOf(i));
                        }*/
                    }

                    if (!mErrorInProcess) FuelingPreferenceManager.putLastSync(new Date());

                    stopSelf();

                    mSyncInProcess = false;

                    mPendingIntent.send(STATUS_FINISH);
                } catch (Exception e) {
                    stopSelf();

                    mErrorInProcess = true;
                    mSyncInProcess = false;

                    Functions.logD("SyncService -- doInBackground: exception == " + e.toString());
                }
                // TODO: delete local files
            }
        }).start();
    }

    private boolean save(SyncLocal syncLocal, int revision) {
        // Сохранить настройки в файл в папке кэша
        // Сохранить номер ревизии в файл в папке кэша

        // Передать файл настроек из папки кэша на сервер
        // Передать файл с номером ревизии из папки кэша на сервер,

        if (syncLocal.savePreferences(FuelingPreferenceManager.getPreferences()))
            Functions.logD("SyncService -- save: syncLocal.savePreferences() OK");
        else {
            Functions.logD("SyncService -- save: syncLocal.savePreferences() ERROR");
            return false;
        }

        if (syncLocal.saveRevision(revision))
            Functions.logD("SyncService -- save: syncLocal.saveRevision(" + revision + ") OK");
        else {
            Functions.logD("SyncService -- save: syncLocal.saveRevision(" + revision + ") ERROR");
            return false;
        }

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

        return true;
    }

    private boolean load(SyncLocal syncLocal, int revision) {
        // Получить файл настроек с сервера и сохранить в папку кэша

        /*        if (!ServerSyncHelper.loadPreferences()) {
            Functions.logD("ServiceSync -- load: error ServerSyncHelper.loadPreferences");
            return false;
        } */

        List<String> preferences = new ArrayList<>();

        if (syncLocal.loadPreferences(preferences)) {
            Functions.logD("SyncService -- load: syncLocal.loadPreferences() OK");

            FuelingPreferenceManager.setPreferences(preferences);
            FuelingPreferenceManager.putChanged(false);
            FuelingPreferenceManager.putRevision(revision);

            return true;
        } else {
            Functions.logD("SyncService -- load: syncLocal.loadPreferences() ERROR");
            return false;
        }
    }
}
