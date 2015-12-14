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

public class SyncPreferencesAdapter {

    private final ContentProviderClient mProvider;

    SyncPreferencesAdapter(ContentProviderClient provider) {
        mProvider = provider;
    }

    @NonNull
    private ContentValues query(@Nullable String preference) throws RemoteException, FormatException {

        final Cursor cursor = mProvider.query(SyncProvider.URI_PREFERENCES, null, preference, null, null);

        if (cursor == null)
            throw new FormatException("SyncPreferencesAdapter -- query: cursor == null");
        else if (cursor.getCount() == 0)
            throw new FormatException("SyncPreferencesAdapter -- query: cursor.getCount() == 0");

        ContentValues result = new ContentValues();

        String key;

        if (cursor.moveToFirst())
            do {
                key = cursor.getString(0);

                switch (FuelingPreferenceManager.getPreferenceType(key)) {
                    case FuelingPreferenceManager.PREFERENCE_TYPE_STRING:
                        result.put(key, cursor.getString(1));
                        break;
                    case FuelingPreferenceManager.PREFERENCE_TYPE_INT:
                        result.put(key, cursor.getInt(1));
                        break;
                    case FuelingPreferenceManager.PREFERENCE_TYPE_LONG:
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
            switch (FuelingPreferenceManager.getPreferenceType(key)) {
                case FuelingPreferenceManager.PREFERENCE_TYPE_STRING:
                    result.add(key + '=' + contentValues.getAsString(key));
                    break;
                case FuelingPreferenceManager.PREFERENCE_TYPE_INT:
                    result.add(key + '=' + String.valueOf(contentValues.getAsInteger(key)));
                    break;
                case FuelingPreferenceManager.PREFERENCE_TYPE_LONG:
                    result.add(key + '=' + String.valueOf(contentValues.getAsLong(key)));
                    break;
            }

//        for (String preference : result)
//            Functions.logD("SyncPreferencesAdapter -- getPreferences: preference == " + preference);

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

            switch (FuelingPreferenceManager.getPreferenceType(key)) {
                case FuelingPreferenceManager.PREFERENCE_TYPE_STRING:
                    contentValues.put(key, value);
                    break;
                case FuelingPreferenceManager.PREFERENCE_TYPE_INT:
                    contentValues.put(key, Integer.decode(value));
                    break;
                case FuelingPreferenceManager.PREFERENCE_TYPE_LONG:
                    contentValues.put(key, Long.decode(value));
                    break;
            }
        }

        update(contentValues, null);
    }

    public boolean isChanged() throws RemoteException, FormatException {
        return query(FuelingPreferenceManager.PREF_CHANGED)
                .getAsBoolean(FuelingPreferenceManager.PREF_CHANGED);
    }

    public void putChanged() throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(FuelingPreferenceManager.PREF_CHANGED, false);
        update(contentValues, FuelingPreferenceManager.PREF_CHANGED);
    }

    public int getRevision() throws RemoteException, FormatException {
        return query(FuelingPreferenceManager.PREF_REVISION)
                .getAsInteger(FuelingPreferenceManager.PREF_REVISION);
    }

    public void putRevision(int revision) throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(FuelingPreferenceManager.PREF_REVISION, revision);
        update(contentValues, FuelingPreferenceManager.PREF_REVISION);
    }

    public void putLastSync(@Nullable Date dateTime) throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(FuelingPreferenceManager.PREF_LAST_SYNC,
                dateTime != null ? Functions.dateTimeToString(dateTime) : null);
        update(contentValues, FuelingPreferenceManager.PREF_LAST_SYNC);
    }
}
