package ru.p3tr0vich.fuel.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import ru.p3tr0vich.fuel.ContentObserverService
import ru.p3tr0vich.fuel.ContentObserverService.Companion.SYNC_ALL
import ru.p3tr0vich.fuel.SyncAccount
import ru.p3tr0vich.fuel.SyncYandexDisk
import ru.p3tr0vich.fuel.utils.UtilsLog
import java.util.regex.Pattern

class ActivityTokenReceiver : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent != null && intent.data != null) {
            val data: Uri = intent.data!!

            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "onCreate", "data == $data")
            }

            intent = null

            val matcher = Pattern
                    .compile(SyncYandexDisk.PATTERN_ACCESS_TOKEN)
                    .matcher(data.toString())

            if (matcher.find()) {
                val token = matcher.group(1)

                if (!TextUtils.isEmpty(token)) {
                    if (LOG_ENABLED) {
                        UtilsLog.d(TAG, "onCreate", "token == $token")
                    }

                    SyncAccount(this).yandexDiskToken = token
                } else {
                    UtilsLog.d(TAG, "onCreate", "empty token")
                }
            } else {
                UtilsLog.d(TAG, "onCreate", "token not found in return url")
            }
        } else {
            UtilsLog.d(TAG, "onCreate", "intent == null || intent.data == null")
        }

        finish()

        startActivity(Intent(applicationContext, ActivityMain::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))

        ContentObserverService.requestSync(this, SYNC_ALL, true, false, null)
    }

    companion object {
        private const val TAG = "ActivityTokenReceiver"

        private var LOG_ENABLED = false
    }
}