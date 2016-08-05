package ru.p3tr0vich.fuel.observers;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import ru.p3tr0vich.fuel.BroadcastReceiverDatabaseChanged;
import ru.p3tr0vich.fuel.ContentObserverService;
import ru.p3tr0vich.fuel.helpers.ContentProviderHelper;
import ru.p3tr0vich.fuel.utils.UtilsLog;

import static ru.p3tr0vich.fuel.ApplicationFuel.getContext;

/**
 * Наблюдатель за изменениями в базе данных.
 * В случае обнаружения изменений, запускает синхронизацию,
 * кроме изменений в результате синхронизации.
 * Также, отправляет сообщение ({@link BroadcastReceiver}) об изменениях.
 *
 * @see ContentObserverService
 * @see BroadcastReceiverDatabaseChanged
 */
public class DatabaseObserver extends ContentObserverBase {

    private static final String TAG = "DatabaseObserver";

    public void register(@NonNull Context context) {
        register(context, ContentProviderHelper.URI_DATABASE, true);
    }

    @Override
    public void onChange(boolean selfChange, Uri changeUri) {
        UtilsLog.d(TAG, "onChange", "changeUri == " + changeUri);

        long id = -1;

        if (changeUri != null)
            switch (ContentProviderHelper.uriMatch(changeUri)) {
                case ContentProviderHelper.DATABASE_ITEM:
                    id = ContentUris.parseId(changeUri);
                case ContentProviderHelper.DATABASE:
                    ContentObserverService.requestSync(getContext(),
                            ContentObserverService.SYNC_DATABASE, true, true, null);
                    break;
                case ContentProviderHelper.DATABASE_SYNC:
                    // Изменения произошли после синхронизации.
                    break;
                default:
                    return;
            }

        BroadcastReceiverDatabaseChanged.send(getContext(), id);
    }
}