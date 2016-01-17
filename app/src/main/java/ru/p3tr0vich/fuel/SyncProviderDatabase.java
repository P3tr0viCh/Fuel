package ru.p3tr0vich.fuel;

import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.nfc.FormatException;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

class SyncProviderDatabase {

    private static final String TAG = "SyncProviderDatabase";

    private static final String ADD = "+";
    private static final String DELETE = "-";
    private static final String SEPARATOR = "\t";

    private final ContentProviderClient mProvider;

    SyncProviderDatabase(ContentProviderClient provider) {
        mProvider = provider;
    }

    @NonNull
    public List<String> getSyncRecords(boolean fullSave) throws RemoteException {
        List<String> result = new ArrayList<>();

        Cursor cursor = mProvider.query(fullSave ?
                SyncProvider.URI_DATABASE :
                SyncProvider.URI_DATABASE_SYNC, null, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIdIndex = DatabaseHelper.getColumnIdIndex(cursor);
                int columnDateTimeIndex = DatabaseHelper.getColumnDateTimeIndex(cursor);
                int columnCostIndex = DatabaseHelper.getColumnCostIndex(cursor);
                int columnVolumeIndex = DatabaseHelper.getColumnVolumeIndex(cursor);
                int columnTotalIndex = DatabaseHelper.getColumnTotalIndex(cursor);
                int columnDeletedIndex = DatabaseHelper.getColumnDeletedIndex(cursor);

                do {
                    result.add(DatabaseHelper.getBoolean(cursor, columnDeletedIndex) ?
                            DELETE + SEPARATOR +
                                    Long.toString(cursor.getLong(columnIdIndex)) :
                            ADD + SEPARATOR +
                                    Long.toString(cursor.getLong(columnIdIndex)) + SEPARATOR +
                                    Long.toString(cursor.getLong(columnDateTimeIndex)) + SEPARATOR +
                                    Float.toString(cursor.getFloat(columnCostIndex)) + SEPARATOR +
                                    Float.toString(cursor.getFloat(columnVolumeIndex)) + SEPARATOR +
                                    Float.toString(cursor.getFloat(columnTotalIndex)));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return result;
    }

    public void clearSyncRecords() throws RemoteException {
        mProvider.delete(SyncProvider.URI_DATABASE_SYNC, null, null);
        mProvider.update(SyncProvider.URI_DATABASE_SYNC, null, null, null);
    }

    public void updateDatabase(@NonNull List<String> syncRecords) throws FormatException, RemoteException {
        ContentValues values = new ContentValues();

        String[] strings;

        for (String syncRecord : syncRecords) {
            if (TextUtils.isEmpty(syncRecord)) continue;

            try {
                strings = TextUtils.split(syncRecord, SEPARATOR);

//                for (int i = 0; i < strings.length; i++)
//                    UtilsLog.d(TAG, "updateDatabase", "strings[" + i + "] == " + strings[i]);

                switch (strings[0]) {
                    case ADD:
                        values.put(DatabaseHelper._ID, Long.valueOf(strings[1]));
                        values.put(DatabaseHelper.COLUMN_DATETIME, Long.valueOf(strings[2]));
                        values.put(DatabaseHelper.COLUMN_COST, Float.valueOf(strings[3]));
                        values.put(DatabaseHelper.COLUMN_VOLUME, Float.valueOf(strings[4]));
                        values.put(DatabaseHelper.COLUMN_TOTAL, Float.valueOf(strings[5]));
                        values.put(DatabaseHelper.COLUMN_CHANGED, DatabaseHelper.FALSE);
                        values.put(DatabaseHelper.COLUMN_DELETED, DatabaseHelper.FALSE);

                        mProvider.insert(SyncProvider.URI_DATABASE, values);

                        break;
                    case DELETE:
                        mProvider.delete(ContentUris.withAppendedId(SyncProvider.URI_DATABASE,
                                Long.valueOf(strings[1])), null, null);

                        break;
                    default:
                        throw new FormatException(TAG +
                                " -- updateDatabase: error strings[0] == " + strings[0]);
                }
            } catch (RemoteException e) {
                throw new RemoteException(e.getMessage());
            } catch (Exception e) {
                throw new FormatException(TAG + " -- updateDatabase: exception == " + e.toString());
            }
        }
    }
}