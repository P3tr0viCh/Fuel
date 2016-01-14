package ru.p3tr0vich.fuel;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.nfc.FormatException;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class SyncProviderPreferences {

    private static final String TAG = "SyncProviderPreferences";

    private final ContentProviderClient mProvider;

    SyncProviderPreferences(ContentProviderClient provider) {
        mProvider = provider;
    }

    @NonNull
    private ContentValues query(@Nullable String preference) throws RemoteException, FormatException {

        final Cursor cursor = mProvider.query(SyncProvider.URI_PREFERENCES, null, preference, null, null);

        if (cursor == null)
            throw new FormatException(TAG + " -- query: cursor == null");
        else if (cursor.getCount() == 0)
            throw new FormatException(TAG + " -- query: cursor.getCount() == 0");

        ContentValues result = new ContentValues();

        String key;

        if (cursor.moveToFirst())
            do {
                key = cursor.getString(0);

                switch (PreferenceManagerFuel.getPreferenceType(key)) {
                    case PreferenceManagerFuel.PREFERENCE_TYPE_STRING:
                        result.put(key, cursor.getString(1));
                        break;
                    case PreferenceManagerFuel.PREFERENCE_TYPE_INT:
                        result.put(key, cursor.getInt(1));
                        break;
                    case PreferenceManagerFuel.PREFERENCE_TYPE_LONG:
                        result.put(key, cursor.getLong(1));
                        break;
                }
            } while (cursor.moveToNext());
        cursor.close();

        return result;
    }

    private void update(@NonNull ContentValues contentValues,
                        @Nullable String preference) throws RemoteException {
        mProvider.update(SyncProvider.URI_PREFERENCES, contentValues, preference, null);
    }

    @NonNull
    public List<String> getPreferences() throws RemoteException, FormatException {

        ContentValues contentValues = query(null);

        List<String> result = new ArrayList<>();

        for (String key : contentValues.keySet())
            switch (PreferenceManagerFuel.getPreferenceType(key)) {
                case PreferenceManagerFuel.PREFERENCE_TYPE_STRING:
                    result.add(key + '=' + contentValues.getAsString(key));
                    break;
                case PreferenceManagerFuel.PREFERENCE_TYPE_INT:
                    result.add(key + '=' + String.valueOf(contentValues.getAsInteger(key)));
                    break;
                case PreferenceManagerFuel.PREFERENCE_TYPE_LONG:
                    result.add(key + '=' + String.valueOf(contentValues.getAsLong(key)));
                    break;
            }

        return result;
    }

    public void setPreferences(@NonNull List<String> preferences) throws RemoteException, NumberFormatException {
        ContentValues contentValues = new ContentValues();

        int index;
        String key, value;

        for (String preference : preferences) {
            index = preference.indexOf('=');

            if (index == -1) continue;

            key = preference.substring(0, index);

            value = preference.substring(index + 1);

            if (key.isEmpty()) continue;

            switch (PreferenceManagerFuel.getPreferenceType(key)) {
                case PreferenceManagerFuel.PREFERENCE_TYPE_STRING:
                    contentValues.put(key, value);
                    break;
                case PreferenceManagerFuel.PREFERENCE_TYPE_INT:
                    contentValues.put(key, Integer.decode(value));
                    break;
                case PreferenceManagerFuel.PREFERENCE_TYPE_LONG:
                    contentValues.put(key, Long.decode(value));
                    break;
            }
        }

        update(contentValues, null);
    }

    public boolean isChanged() throws RemoteException, FormatException {
        return query(PreferenceManagerFuel.PREF_CHANGED)
                .getAsBoolean(PreferenceManagerFuel.PREF_CHANGED);
    }

    public void putChanged() throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PreferenceManagerFuel.PREF_CHANGED, false);
        update(contentValues, PreferenceManagerFuel.PREF_CHANGED);
    }

    private int getRevision(String keyRevision) throws RemoteException, FormatException {
        return query(keyRevision).getAsInteger(keyRevision);
    }

    public int getDatabaseRevision() throws RemoteException, FormatException {
        return getRevision(PreferenceManagerFuel.PREF_DATABASE_REVISION);
    }

    public int getPreferencesRevision() throws RemoteException, FormatException {
        return getRevision(PreferenceManagerFuel.PREF_PREFERENCES_REVISION);
    }

    private void putRevision(String keyRevision, int revision) throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(keyRevision, revision);
        update(contentValues, keyRevision);
    }

    public void putDatabaseRevision(int revision) throws RemoteException {
        putRevision(PreferenceManagerFuel.PREF_DATABASE_REVISION, revision);
    }

    public void putPreferencesRevision(int revision) throws RemoteException {
        putRevision(PreferenceManagerFuel.PREF_PREFERENCES_REVISION, revision);
    }

    public void putLastSync(@Nullable Date dateTime) throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PreferenceManagerFuel.PREF_LAST_SYNC,
                dateTime != null ? UtilsFormat.dateTimeToString(dateTime) : null);
        update(contentValues, PreferenceManagerFuel.PREF_LAST_SYNC);
    }
}
