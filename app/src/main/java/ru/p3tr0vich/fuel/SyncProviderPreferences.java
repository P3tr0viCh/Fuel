package ru.p3tr0vich.fuel;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.FormatException;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import ru.p3tr0vich.fuel.helpers.ContentProviderHelper;
import ru.p3tr0vich.fuel.helpers.PreferencesHelper;

class SyncProviderPreferences {

    private static final String TAG = "SyncProviderPreferences";

    private static final String SEPARATOR = "=";

    private final ContentProviderClient mProvider;

    SyncProviderPreferences(ContentProviderClient provider) {
        mProvider = provider;
    }

    @NonNull
    private ContentValues query(@Nullable String preference) throws RemoteException, FormatException {
        final Cursor cursor = mProvider.query(
                TextUtils.isEmpty(preference) ?
                        ContentProviderHelper.URI_PREFERENCES :
                        Uri.withAppendedPath(ContentProviderHelper.URI_PREFERENCES, preference),
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

                    switch (PreferencesHelper.getPreferenceType(key)) {
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
                        ContentProviderHelper.URI_PREFERENCES :
                        Uri.withAppendedPath(ContentProviderHelper.URI_PREFERENCES, preference),
                contentValues, null, null);
    }

    private String encode(String s) throws FormatException {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new FormatException(e.toString());
        }
    }

    private String decode(String s) throws FormatException {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new FormatException(e.toString());
        }
    }

    @NonNull
    public List<String> getPreferences() throws RemoteException, FormatException {
        ContentValues contentValues = query(null);

        List<String> result = new ArrayList<>();

        for (String key : contentValues.keySet())
            switch (PreferencesHelper.getPreferenceType(key)) {
                case PreferencesHelper.PREFERENCE_TYPE_STRING:
                    result.add(key + SEPARATOR + encode(contentValues.getAsString(key)));
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
            throws RemoteException, NumberFormatException, FormatException {
        ContentValues contentValues = new ContentValues();

        int index;
        String key, value;

        for (String preference : preferences) {
            index = preference.indexOf(SEPARATOR);

            if (index == -1) continue;

            key = preference.substring(0, index);

            if (key.isEmpty()) continue;

            value = preference.substring(index + 1);

            switch (PreferencesHelper.getPreferenceType(key)) {
                case PreferencesHelper.PREFERENCE_TYPE_STRING:
                    contentValues.put(key, decode(value));
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
        return query(PreferencesHelper.PREF_CHANGED)
                .getAsBoolean(PreferencesHelper.PREF_CHANGED);
    }

    public void putChangedFalse() throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PreferencesHelper.PREF_CHANGED, false);

        update(contentValues, PreferencesHelper.PREF_CHANGED);
    }

    private int getRevision(String keyRevision) throws RemoteException, FormatException {
        return query(keyRevision).getAsInteger(keyRevision);
    }

    public int getDatabaseRevision() throws RemoteException, FormatException {
        return getRevision(PreferencesHelper.PREF_DATABASE_REVISION);
    }

    public int getPreferencesRevision() throws RemoteException, FormatException {
        return getRevision(PreferencesHelper.PREF_PREFERENCES_REVISION);
    }

    private void putRevision(String keyRevision, int revision) throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(keyRevision, revision);

        update(contentValues, keyRevision);
    }

    public void putDatabaseRevision(int revision) throws RemoteException {
        putRevision(PreferencesHelper.PREF_DATABASE_REVISION, revision);
    }

    public void putPreferencesRevision(int revision) throws RemoteException {
        putRevision(PreferencesHelper.PREF_PREFERENCES_REVISION, revision);
    }

    public void putLastSync(final long dateTime, final boolean hasError) throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PreferencesHelper.PREF_LAST_SYNC_DATE_TIME, dateTime);
        contentValues.put(PreferencesHelper.PREF_LAST_SYNC_HAS_ERROR, hasError);

        update(contentValues, PreferencesHelper.PREF_LAST_SYNC_DATE_TIME);
        update(contentValues, PreferencesHelper.PREF_LAST_SYNC_HAS_ERROR);
    }

    public boolean isDatabaseFullSync() throws RemoteException, FormatException {
        return query(PreferencesHelper.PREF_DATABASE_FULL_SYNC)
                .getAsBoolean(PreferencesHelper.PREF_DATABASE_FULL_SYNC);
    }

    public void putDatabaseFullSyncFalse() throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PreferencesHelper.PREF_DATABASE_FULL_SYNC, false);

        update(contentValues, PreferencesHelper.PREF_DATABASE_FULL_SYNC);
    }
}