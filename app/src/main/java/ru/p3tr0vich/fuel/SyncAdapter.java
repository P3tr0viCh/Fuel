package ru.p3tr0vich.fuel;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public SyncAdapter(Context context) {
        super(context, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {

        boolean isNoErrors = true;

        Functions.logD("SyncAdapter -- onPerformSync: start");

        try {
            if (new Random().nextBoolean()) {
                Functions.logD("SyncAdapter -- onPerformSync: error");

                isNoErrors = false;

                syncResult.stats.numAuthExceptions++;

                return;
            }

            for (int i = 0; i < 10; i++) {

                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Functions.logD("SyncAdapter -- onPerformSync: " + String.valueOf(i));
            }
        } finally {
            if (isNoErrors) FuelingPreferenceManager.putLastSync(new Date());
            else FuelingPreferenceManager.putLastSync(FuelingPreferenceManager.SYNC_ERROR);

            new SyncAccount(getContext()).setUserData(SyncAccount.KEY_LAST_SYNC,
                    isNoErrors ? Functions.dateTimeToString(new Date()) : FuelingPreferenceManager.SYNC_ERROR);

            Functions.logD("SyncAdapter -- onPerformSync: stop, isNoErrors == " + isNoErrors);
        }
    }
}