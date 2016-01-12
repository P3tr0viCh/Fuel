package ru.p3tr0vich.fuel;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

class SyncAdapterDatabase {

    private final ContentProviderClient mProvider;

    SyncAdapterDatabase(ContentProviderClient provider) {
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
    public List<String> getSyncRecords(boolean fullSave) throws RemoteException {
        List<String> result = new ArrayList<>();

        Cursor cursor = query(fullSave ?
                SyncProvider.DATABASE_GET_ALL_RECORDS : SyncProvider.DATABASE_GET_CHANGED_RECORDS);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String record;
                do {
                    if (FuelingDBHelper.getBoolean(cursor,
                            FuelingDBHelper.TABLE_FUELING_COLUMN_DELETED_INDEX))
                        record = "-" + '\t' +
                                Long.toString(
                                        cursor.getLong(
                                                FuelingDBHelper.TABLE_FUELING_COLUMN_SYNC_ID_INDEX));
                    else
                        record = "+" + '\t' +
                                Long.toString(
                                        cursor.getLong(
                                                FuelingDBHelper.TABLE_FUELING_COLUMN_SYNC_ID_INDEX)) + '\t' +
                                Long.toString(
                                        cursor.getLong(
                                                FuelingDBHelper.TABLE_FUELING_COLUMN_DATETIME_INDEX)) + '\t' +
                                Float.toString(
                                        cursor.getFloat(
                                                FuelingDBHelper.TABLE_FUELING_COLUMN_COST_INDEX)) + '\t' +
                                Float.toString(
                                        cursor.getFloat(
                                                FuelingDBHelper.TABLE_FUELING_COLUMN_VOLUME_INDEX)) + '\t' +
                                Float.toString(
                                        cursor.getFloat(
                                                FuelingDBHelper.TABLE_FUELING_COLUMN_TOTAL_INDEX));

                    result.add(record);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return result;
    }
}