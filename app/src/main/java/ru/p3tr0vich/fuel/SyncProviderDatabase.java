package ru.p3tr0vich.fuel;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

class SyncProviderDatabase {

    private static final String ADD = "+";
    private static final String DELETE = "-";
    private static final String SEPARATOR = "\t";

    private final ContentProviderClient mProvider;

    SyncProviderDatabase(ContentProviderClient provider) {
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

    @NonNull
    private String cursorToString(@NonNull Cursor cursor, boolean delete) {
        return delete ?
                DELETE + SEPARATOR +
                        Long.toString(cursor.getLong(
                                FuelingDBHelper.TABLE_FUELING_COLUMN_SYNC_ID_INDEX)) :
                ADD + SEPARATOR +
                        Long.toString(cursor.getLong(
                                FuelingDBHelper.TABLE_FUELING_COLUMN_SYNC_ID_INDEX)) + SEPARATOR +
                        Long.toString(cursor.getLong(
                                FuelingDBHelper.TABLE_FUELING_COLUMN_DATETIME_INDEX)) + SEPARATOR +
                        Float.toString(cursor.getFloat(
                                FuelingDBHelper.TABLE_FUELING_COLUMN_COST_INDEX)) + SEPARATOR +
                        Float.toString(cursor.getFloat(
                                FuelingDBHelper.TABLE_FUELING_COLUMN_VOLUME_INDEX)) + SEPARATOR +
                        Float.toString(cursor.getFloat(
                                FuelingDBHelper.TABLE_FUELING_COLUMN_TOTAL_INDEX));
    }

    @NonNull
    public List<String> getSyncRecords(boolean fullSave) throws RemoteException {
        List<String> result = new ArrayList<>();

        Cursor cursor = query(fullSave ?
                SyncProvider.DATABASE_GET_ALL_RECORDS : SyncProvider.DATABASE_GET_CHANGED_RECORDS);

        if (cursor != null) {
            if (cursor.moveToFirst())
                do {
                    result.add(
                            cursorToString(cursor,
                                    FuelingDBHelper.getBoolean(cursor,
                                            FuelingDBHelper.TABLE_FUELING_COLUMN_DELETED_INDEX)));
                } while (cursor.moveToNext());
            cursor.close();
        }

        return result;
    }

    public void clearSyncRecords() throws RemoteException {
        update(new ContentValues(), SyncProvider.DATABASE_CLEAR_SYNC_RECORDS);
    }
}