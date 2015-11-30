package ru.p3tr0vich.fuel;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ServiceSync extends Service {

    public static final int REQUEST_CODE = 3619;

    public static final int STATUS_START = 100;
    public static final int STATUS_FINISH = 200;

    public static final String EXTRA_PENDING = "ru.p3tr0vich.fuel.EXTRA_PENDING";

    PendingIntent mPendingIntent;

    private static boolean mSyncInProcess = false;

    public ServiceSync() {
    }

    public static boolean isSyncInProcess() {
        return mSyncInProcess;
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

        if (startId == 1) sync();

        return super.onStartCommand(intent, flags, startId);
    }

    private void sync() {
        new Thread(new Runnable() {
            public void run() {
                mSyncInProcess = true;

                try {
                    mPendingIntent.send(STATUS_START);

                    for (int i = 0; i < 10; i++) {

                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        Functions.logD("ServiceSync -- doInBackground: " + String.valueOf(i));
                    }


                    FuelingPreferenceManager.putLastSync(new Date());

                    stopSelf();

                    mPendingIntent.send(STATUS_FINISH);

                    mSyncInProcess = false;
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
