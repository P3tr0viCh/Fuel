package ru.p3tr0vich.fuel;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActivityTokenReceiver extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null && getIntent().getData() != null) {
            Uri data = getIntent().getData();

            setIntent(null);

            Pattern pattern = Pattern.compile(SyncYandexDisk.PATTERN_ACCESS_TOKEN);
            Matcher matcher = pattern.matcher(data.toString());

            if (matcher.find()) {
                String token = matcher.group(1);

                if (!TextUtils.isEmpty(token)) {
                    Functions.logD("ActivityTokenReceiver -- onCreate: token == " + token);

                    new SyncAccount(this).setYandexDiskToken(token);
                    // TODO: updateSyncStatus
                } else
                    Functions.logD("ActivityTokenReceiver -- onCreate: empty token");
            } else
                Functions.logD("ActivityTokenReceiver -- onCreate: token not found in return url");
        }

        finish();
    }
}
