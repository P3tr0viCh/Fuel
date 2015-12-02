package ru.p3tr0vich.fuel;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.Date;

public class ServiceSync extends Service {

    public static final int REQUEST_CODE = 3619;

    public static final int STATUS_START = 100;
    public static final int STATUS_FINISH = 200;

    public static final String EXTRA_START = "ru.p3tr0vich.fuel.EXTRA_START";
    public static final String EXTRA_PENDING = "ru.p3tr0vich.fuel.EXTRA_PENDING";

    private PendingIntent mPendingIntent;

    private static boolean mSyncInProcess = false;
    private static boolean mErrorInProcess = false;

    public ServiceSync() {
    }

    public static boolean isSyncInProcess() {
        return mSyncInProcess;
    }

    public static boolean isErrorInProcess() {
        return mErrorInProcess;
    }

    @Override
    public void onCreate() {
        Functions.logD("ServiceSync -- onCreate");
    }

    @Override
    public void onDestroy() {
        Functions.logD("ServiceSync -- onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Functions.logD("ServiceSync -- onStartCommand: startId == " + startId);

        mPendingIntent = intent.getParcelableExtra(EXTRA_PENDING);

        if (!mSyncInProcess && intent.getBooleanExtra(EXTRA_START, true)) startSync();

        return super.onStartCommand(intent, flags, startId);
    }

    private void startSync() {
        new Thread(new Runnable() {
            public void run() {
                mSyncInProcess = true;
                mErrorInProcess = false;

//                Random rand = new Random();

                try {
                    mPendingIntent.send(STATUS_START);
                    try {

//                    if (rand.nextBoolean()) {
//                        Functions.logD("ServiceSync -- doInBackground: error");
//                        mErrorInProcess = true;
//                    }

                        CacheSyncHelper cacheSyncHelper = new CacheSyncHelper(ServiceSync.this);

                        // TODO:
                        // Копируем файл с номером ревизии с сервера в папку кэша.
                        // Если нет доступа к серверу mErrorInProcess == true.
                        // Если файла нет, игнорируем.

                        if (!mErrorInProcess) {

                            int serverRevision = cacheSyncHelper.getRevision();
                            // serverRevision == -1, если синхронизация не производилась
                            // или файлы синхронизации отсутствуют на сервере

                            int localRevision = FuelingPreferenceManager.getRevision();
                            // localRevision == 0, если программа запускается первый раз

                            Functions.logD("ServiceSync -- doInBackground: serverRevision == " +
                                    serverRevision + ", localRevision == " + localRevision);

                            if (localRevision < serverRevision) {
                                // Синхронизация уже выполнялась на другом устройстве.
                                // Текущие изменения теряются.

                                Functions.logD("ServiceSync -- doInBackground: localRevision < serverRevision");

                                if (load(cacheSyncHelper)) {
                                    FuelingPreferenceManager.putChanged(false);
                                    FuelingPreferenceManager.putRevision(serverRevision);
                                } else
                                    mErrorInProcess = true;
                            } else if (localRevision > serverRevision) {
                                // 1. Сихронизация выполняется в первый раз
                                // (localRevision == 0 > serverRevision == -1).
                                // 2. Файлы синхронизации были удалены
                                // (localRevision > 0 > serverRevision == -1).

                                Functions.logD("ServiceSync -- doInBackground: localRevision > serverRevision");

                                if (save(cacheSyncHelper, localRevision))
                                    FuelingPreferenceManager.putChanged(false);
                                else
                                    mErrorInProcess = true;
                            } else { // localRevision == serverRevision
                                // Синхронизация была выполнена.
                                // Если настройки были изменены, сохранить их на сервер.

                                Functions.logD("ServiceSync -- doInBackground: localRevision == serverRevision");

                                if (FuelingPreferenceManager.isChanged()) {
                                    Functions.logD("ServiceSync -- doInBackground: isChanged == true");

                                    localRevision++;

                                    if (save(cacheSyncHelper, localRevision)) {
                                        FuelingPreferenceManager.putChanged(false);
                                        FuelingPreferenceManager.putRevision(localRevision);
                                    } else
                                        mErrorInProcess = true;
                                } else
                                    Functions.logD("ServiceSync -- doInBackground: isChanged == false");
                            }

/*                        for (int i = 0; i < 10; i++) {

                            TimeUnit.SECONDS.sleep(1);

                            Functions.logD("ServiceSync -- doInBackground: " + String.valueOf(i));
                        }*/
                        }

                        if (!mErrorInProcess) FuelingPreferenceManager.putLastSync(new Date());

                        stopSelf();

                        mSyncInProcess = false;
                    } catch (Exception e) {
                        stopSelf();

                        mErrorInProcess = true;
                        mSyncInProcess = false;

                        Functions.logD("ServiceSync -- doInBackground: catch (Exception e): " + e.toString());
                    }

                    mPendingIntent.send(STATUS_FINISH);
                } catch (PendingIntent.CanceledException e) {
                    stopSelf();

                    mErrorInProcess = true;
                    mSyncInProcess = false;
                }
            }
        }).start();
    }

    private boolean save(CacheSyncHelper cacheSyncHelper, int revision) {
        // Сохранить настройки в файл в папке кэша
        // Сохранить номер ревизии в файл в папке кэша

        // Передать файл настроек из папки кэша на сервер
        // Передать файл с номером ревизии из папки кэша на сервер,

        if (cacheSyncHelper.savePreferences())
            Functions.logD("ServiceSync -- save: cacheSyncHelper.savePreferences() OK");
        else {
            Functions.logD("ServiceSync -- save: cacheSyncHelper.savePreferences() ERROR");
            return false;
        }

        if (cacheSyncHelper.saveRevision(revision))
            Functions.logD("ServiceSync -- save: cacheSyncHelper.saveRevision(" + revision + ") OK");
        else {
            Functions.logD("ServiceSync -- save: cacheSyncHelper.saveRevision(" + revision + ") ERROR");
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

        return true;
    }

    private boolean load(CacheSyncHelper cacheSyncHelper) {
        // Получить файл настроек с сервера и сохранить в папку кэша

        /*        if (!ServerSyncHelper.loadPreferences()) {
            Functions.logD("ServiceSync -- load: error ServerSyncHelper.loadPreferences");
            return false;
        } */

        if (cacheSyncHelper.loadPreferences())
            Functions.logD("ServiceSync -- load: cacheSyncHelper.loadPreferences() OK");
        else {
            Functions.logD("ServiceSync -- load: cacheSyncHelper.loadPreferences() ERROR");
            return false;
        }

        return true;
    }
}
