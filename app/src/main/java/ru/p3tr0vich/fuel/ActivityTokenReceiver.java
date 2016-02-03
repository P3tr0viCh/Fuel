package ru.p3tr0vich.fuel;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActivityTokenReceiver extends AppCompatActivity {

    private static final String TAG = "ActivityTokenReceiver";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null && getIntent().getData() != null) {
            final Uri data = getIntent().getData();

            UtilsLog.d(TAG, "onCreate", "data == " + data.toString());

            setIntent(null);

            final Matcher matcher = Pattern
                    .compile(SyncYandexDisk.PATTERN_ACCESS_TOKEN)
                    .matcher(data.toString());

            if (matcher.find()) {
                final String token = matcher.group(1);

                if (!TextUtils.isEmpty(token)) {
                    UtilsLog.d(TAG, "onCreate", "token == " + token);

                    new SyncAccount(this).setYandexDiskToken(token);
                } else
                    UtilsLog.d(TAG, "onCreate", "empty token");
            } else
                UtilsLog.d(TAG, "onCreate", "token not found in return url");
        } else
            UtilsLog.d(TAG, "onCreate", "getIntent() == null || getIntent().getData() == null");

        finish();

        startActivity(new Intent(getApplicationContext(), ActivityMain.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));

        BroadcastReceiverSync.send(getApplicationContext(), false, false, true);
    }
}