package ru.p3tr0vich.fuel.observers

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import ru.p3tr0vich.fuel.ApplicationFuel
import ru.p3tr0vich.fuel.ContentObserverService
import ru.p3tr0vich.fuel.helpers.ContentProviderHelper
import ru.p3tr0vich.fuel.receivers.BroadcastReceiverDatabaseChanged
import ru.p3tr0vich.fuel.utils.UtilsLog

/**
 * Наблюдатель за изменениями в базе данных.
 * В случае обнаружения изменений, запускает синхронизацию,
 * кроме изменений в результате синхронизации.
 * Также, отправляет сообщение ([BroadcastReceiver]) об изменениях.
 *
 * @see ContentObserverService
 *
 * @see BroadcastReceiverDatabaseChanged
 */
class DatabaseObserver : ContentObserverBase() {

    companion object {
        private const val TAG = "DatabaseObserver"
    }

    fun register(context: Context) {
        register(context, ContentProviderHelper.URI_DATABASE, true)
    }

    override fun onChange(selfChange: Boolean, changeUri: Uri?) {
        UtilsLog.d(TAG, "onChange", "changeUri == $changeUri")

        var id: Long = -1

        if (changeUri != null) {
            when (ContentProviderHelper.uriMatch(changeUri)) {
                ContentProviderHelper.DATABASE_ITEM -> {
                    id = ContentUris.parseId(changeUri)

                    ContentObserverService.requestSync(ApplicationFuel.context,
                            ContentObserverService.SYNC_DATABASE, startIfSyncActive = true, withDelay = true, pendingIntent = null)
                }
                ContentProviderHelper.DATABASE -> {
                    ContentObserverService.requestSync(ApplicationFuel.context,
                            ContentObserverService.SYNC_DATABASE, startIfSyncActive = true, withDelay = true, pendingIntent = null)
                }
                ContentProviderHelper.DATABASE_SYNC -> {
                    // Изменения произошли после синхронизации.
                }
                else -> return
            }
        }

        BroadcastReceiverDatabaseChanged.send(ApplicationFuel.context, id)
    }
}