package ru.p3tr0vich.fuel;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.FormatException;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import ru.p3tr0vich.fuel.helpers.ContentProviderHelper;
import ru.p3tr0vich.fuel.helpers.PreferencesHelper;
import ru.p3tr0vich.fuel.utils.UtilsString;

class SyncProviderPreferences {

    private static final String TAG = "SyncProviderPreferences";

    private static final String SEPARATOR = "=";

    private final PreferencesHelper mPreferencesHelper;

    private final ContentProviderClient mProvider;

    SyncProviderPreferences(Context context, ContentProviderClient provider) {
        mPreferencesHelper = PreferencesHelper.getInstance(context);
        mProvider = provider;
    }

    @NonNull
    private ContentValues query(@Nullable String preference) throws RemoteException, FormatException {
        final Cursor cursor = mProvider.query(
                TextUtils.isEmpty(preference) ?
                        ContentProviderHelper.Companion.getURI_PREFERENCES() :
                        Uri.withAppendedPath(ContentProviderHelper.Companion.getURI_PREFERENCES(), preference),
                null, null, null, null);

        if (cursor == null)
            throw new FormatException(TAG + " -- query: cursor == null");
        else if (cursor.getCount() == 0)
            throw new FormatException(TAG + " -- query: cursor.getCount() == 0");

        ContentValues result = new ContentValues();

        String key;

        try {
            if (cursor.moveToFirst())
                do {
                    key = cursor.getString(0);

                    switch (mPreferencesHelper.getPreferenceType(key)) {
                        case PreferencesHelper.PREFERENCE_TYPE_STRING:
                            result.put(key, cursor.getString(1));
                            break;
                        case PreferencesHelper.PREFERENCE_TYPE_INT:
                            result.put(key, cursor.getInt(1));
                            break;
                        case PreferencesHelper.PREFERENCE_TYPE_LONG:
                            result.put(key, cursor.getLong(1));
                            break;
                    }
                } while (cursor.moveToNext());
        } finally {
            cursor.close();
        }

        return result;
    }

    private void update(@NonNull ContentValues contentValues,
                        @Nullable String preference) throws RemoteException {
        mProvider.update(TextUtils.isEmpty(preference) ?
                        ContentProviderHelper.Companion.getURI_PREFERENCES() :
                        Uri.withAppendedPath(ContentProviderHelper.Companion.getURI_PREFERENCES(), preference),
                contentValues, null, null);
    }

    @NonNull
    public List<String> getPreferences() throws RemoteException, FormatException {
        ContentValues contentValues = query(null);

        List<String> result = new ArrayList<>();

        for (String key : contentValues.keySet())
            switch (mPreferencesHelper.getPreferenceType(key)) {
                case PreferencesHelper.PREFERENCE_TYPE_STRING:
                    result.add(key + SEPARATOR + UtilsString.encodeLineBreaks(contentValues.getAsString(key)));
                    break;
                case PreferencesHelper.PREFERENCE_TYPE_INT:
                    result.add(key + SEPARATOR + String.valueOf(contentValues.getAsInteger(key)));
                    break;
                case PreferencesHelper.PREFERENCE_TYPE_LONG:
                    result.add(key + SEPARATOR + String.valueOf(contentValues.getAsLong(key)));
                    break;
            }

        return result;
    }

    public void setPreferences(@NonNull List<String> preferences)
            throws RemoteException {
        ContentValues contentValues = new ContentValues();

        int index;
        String key, value;

        for (String preference : preferences) {
            index = preference.indexOf(SEPARATOR);

            if (index == -1) continue;

            key = preference.substring(0, index);

            if (key.isEmpty()) continue;

            value = preference.substring(index + 1);

            switch (mPreferencesHelper.getPreferenceType(key)) {
                case PreferencesHelper.PREFERENCE_TYPE_STRING:
                    contentValues.put(key, UtilsString.decodeLineBreaks(value));
                    break;
                case PreferencesHelper.PREFERENCE_TYPE_INT:
                    contentValues.put(key, Integer.decode(value));
                    break;
                case PreferencesHelper.PREFERENCE_TYPE_LONG:
                    contentValues.put(key, Long.decode(value));
                    break;
            }
        }

        update(contentValues, null);
    }

    public boolean isChanged() throws RemoteException, FormatException {
        return query(mPreferencesHelper.getKeys().getChanged()).getAsBoolean(mPreferencesHelper.getKeys().getChanged());
    }

    public void putChangedFalse() throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(mPreferencesHelper.getKeys().getChanged(), false);

        update(contentValues, mPreferencesHelper.getKeys().getChanged());
    }

    private int getRevision(String keyRevision) throws RemoteException, FormatException {
        return query(keyRevision).getAsInteger(keyRevision);
    }

    public int getDatabaseRevision() throws RemoteException, FormatException {
        return getRevision(mPreferencesHelper.getKeys().getDatabaseRevision());
    }

    public int getPreferencesRevision() throws RemoteException, FormatException {
        return getRevision(mPreferencesHelper.getKeys().getPreferencesRevision());
    }

    private void putRevision(String keyRevision, int revision) throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(keyRevision, revision);

        update(contentValues, keyRevision);
    }

    public void putDatabaseRevision(int revision) throws RemoteException {
        putRevision(mPreferencesHelper.getKeys().getDatabaseRevision(), revision);
    }

    public void putPreferencesRevision(int revision) throws RemoteException {
        putRevision(mPreferencesHelper.getKeys().getPreferencesRevision(), revision);
    }

    public void putLastSync(long dateTime, boolean hasError) throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(mPreferencesHelper.getKeys().getLastSyncDateTime(), dateTime);
        contentValues.put(mPreferencesHelper.getKeys().getLastSyncHasError(), hasError);

        update(contentValues, mPreferencesHelper.getKeys().getLastSyncDateTime());
        update(contentValues, mPreferencesHelper.getKeys().getLastSyncHasError());
    }

    public boolean isDatabaseFullSync() throws RemoteException, FormatException {
        return query(mPreferencesHelper.getKeys().getDatabaseFullSync()).getAsBoolean(mPreferencesHelper.getKeys().getDatabaseFullSync());
    }

    public void putDatabaseFullSyncFalse() throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(mPreferencesHelper.getKeys().getDatabaseFullSync(), false);

        update(contentValues, mPreferencesHelper.getKeys().getDatabaseFullSync());
    }
}