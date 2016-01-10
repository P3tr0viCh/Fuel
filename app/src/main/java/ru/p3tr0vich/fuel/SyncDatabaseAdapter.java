package ru.p3tr0vich.fuel;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.nfc.FormatException;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SyncDatabaseAdapter {

    private final ContentProviderClient mProvider;

    SyncDatabaseAdapter(ContentProviderClient provider) {
        mProvider = provider;
    }

    @Nullable
    private Cursor query(@Nullable String selection) throws RemoteException {
        return mProvider.query(SyncProvider.URI_DATABASE, null, selection, null, null);
    }

    private void update(@NonNull ContentValues contentValues,
                        @Nullable String selection) throws RemoteException {
        mProvider.update(SyncProvider.URI_DATABASE, contentValues, selection, null);
    }

    public boolean isChanged() throws RemoteException, FormatException {
        Cursor cursor = query(SyncProvider.DATABASE_IS_CHANGED);
        return !(cursor == null || cursor.getCount() == 0) && cursor.getInt(0) > 0;
    }

    public void putChanged() throws RemoteException {
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(PreferenceManagerFuel.PREF_CHANGED, false);
//        update(contentValues, PreferenceManagerFuel.PREF_CHANGED);
    }

    public int getRevision() throws RemoteException, FormatException {
        Cursor cursor = query(SyncProvider.DATABASE_GET_REVISION);
        if (cursor == null || cursor.getCount() == 0) return -1;
        return cursor.getInt(0);
    }

    public void putRevision(int revision) throws RemoteException {
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(PreferenceManagerFuel.PREF_REVISION, revision);
//        update(contentValues, PreferenceManagerFuel.PREF_REVISION);
    }

    @NonNull
    public List<String> getSyncRecords(boolean fullSave) throws RemoteException {
        List<String> result = new ArrayList<>();

        Cursor cursor = query(fullSave ?
                SyncProvider.DATABASE_GET_ALL_RECORDS : SyncProvider.DATABASE_GET_CHANGED_RECORDS);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result.add("+");

                do {
                    result.add(Long.toString(cursor.getLong(FuelingDBHelper.COLUMN_DATETIME_INDEX)) + '\t' +
                            Float.toString(cursor.getFloat(FuelingDBHelper.COLUMN_COST_INDEX)) + '\t' +
                            Float.toString(cursor.getFloat(FuelingDBHelper.COLUMN_VOLUME_INDEX)) + '\t' +
                            Float.toString(cursor.getFloat(FuelingDBHelper.COLUMN_TOTAL_INDEX)));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        if (!fullSave) {
            cursor = query(SyncProvider.DATABASE_GET_DELETED_RECORDS);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    result.add("-");

                    do {
                        result.add(Long.toString(cursor.getLong(FuelingDBHelper.COLUMN_DATETIME_INDEX)));
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        }

        return result;
    }
}