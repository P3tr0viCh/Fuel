package ru.p3tr0vich.fuel.observers;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import ru.p3tr0vich.fuel.ContentObserverService;
import ru.p3tr0vich.fuel.helpers.ContentProviderHelper;
import ru.p3tr0vich.fuel.utils.UtilsLog;

import static ru.p3tr0vich.fuel.ApplicationFuel.getContext;

/**
 * Наблюдатель за изменениями в настройках.
 * В случае обнаружения изменений, запускает синхронизацию.
 *
 * @see ContentObserverService
 */
public class PreferencesObserver extends ContentObserverBase {

    private static final String TAG = "PreferencesObserver";

    public void register(@NonNull Context context) {
        register(context, ContentProviderHelper.URI_PREFERENCES, false);
    }

    @Override
    public void onChange(boolean selfChange, Uri changeUri) {
        UtilsLog.d(TAG, "onChange", "selfChange == " + selfChange + ", changeUri == " + changeUri);

        ContentObserverService.requestSync(getContext(),
                ContentObserverService.SYNC_PREFERENCES, true, true, null);
    }
}