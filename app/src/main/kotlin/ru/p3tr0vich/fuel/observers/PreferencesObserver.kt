package ru.p3tr0vich.fuel.observers

import android.content.Context
import android.net.Uri

import ru.p3tr0vich.fuel.ApplicationFuel
import ru.p3tr0vich.fuel.ContentObserverService
import ru.p3tr0vich.fuel.helpers.ContentProviderHelper
import ru.p3tr0vich.fuel.utils.UtilsLog

/**
 * Наблюдатель за изменениями в настройках.
 * В случае обнаружения изменений, запускает синхронизацию.
 *
 * @see ContentObserverService
 */
class PreferencesObserver : ContentObserverBase() {

    companion object {
        private const val TAG = "PreferencesObserver"
    }

    fun register(context: Context) {
        register(context, ContentProviderHelper.URI_PREFERENCES, false)
    }

    override fun onChange(selfChange: Boolean, changeUri: Uri?) {
        UtilsLog.d(TAG, "onChange", "selfChange == $selfChange, changeUri == $changeUri")

        ContentObserverService.requestSync(ApplicationFuel.context,
                ContentObserverService.SYNC_PREFERENCES, startIfSyncActive = true, withDelay = true, pendingIntent = null)
    }
}