package ru.p3tr0vich.fuel;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.nfc.FormatException;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
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
                do {
                    result.add(DatabaseHelper.getBoolean(cursor, DatabaseHelper.COLUMN_DELETED_INDEX) ?
                            DELETE + SEPARATOR +
                                    Long.toString(cursor.getLong(DatabaseHelper.COLUMN_ID_INDEX)) :
                            ADD + SEPARATOR +
                                    Long.toString(cursor.getLong(DatabaseHelper.COLUMN_ID_INDEX)) + SEPARATOR +
                                    Long.toString(cursor.getLong(DatabaseHelper.COLUMN_DATETIME_INDEX)) + SEPARATOR +
                                    Float.toString(cursor.getFloat(DatabaseHelper.COLUMN_COST_INDEX)) + SEPARATOR +
                                    Float.toString(cursor.getFloat(DatabaseHelper.COLUMN_VOLUME_INDEX)) + SEPARATOR +
                                    Float.toString(cursor.getFloat(DatabaseHelper.COLUMN_TOTAL_INDEX)));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return result;
    }

    public void syncDeletedRecords() throws RemoteException {
        mProvider.delete(SyncProvider.URI_DATABASE_SYNC, null, null);
    }

    public void syncChangedRecords() throws RemoteException {
        // Нужно указывать new ContentValues(0) иначе будет Null pointer exception в bulkInsert
        mProvider.update(SyncProvider.URI_DATABASE_SYNC, new ContentValues(0), null, null);
    }

    public void updateDatabase(@NonNull List<String> syncRecords) throws FormatException, RemoteException {
        String[] stringValues;

        ContentValues values = new ContentValues();

        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        // TODO: удалить повторяющиеся значения из syncRecords

        for (String syncRecord : syncRecords) {
            if (TextUtils.isEmpty(syncRecord)) continue;

            stringValues = TextUtils.split(syncRecord, SEPARATOR);

//            for (int i = 0; i < stringValues.length; i++)
//                UtilsLog.d(TAG, "updateDatabase", "stringValues[" + i + "] == " + stringValues[i]);

            long id;

            try {
                id = Long.valueOf(stringValues[1]);
            } catch (Exception e) {
                throw new FormatException(TAG +
                        " -- updateDatabase: exception == " + e.toString() +
                        ", record == " + Arrays.toString(stringValues));
            }

            switch (stringValues[0]) {
                case ADD:
                    values.put(DatabaseHelper._ID, id);
                    try {
                        values.put(DatabaseHelper.COLUMN_DATETIME, Long.valueOf(stringValues[2]));
                        values.put(DatabaseHelper.COLUMN_COST, Float.valueOf(stringValues[3]));
                        values.put(DatabaseHelper.COLUMN_VOLUME, Float.valueOf(stringValues[4]));
                        values.put(DatabaseHelper.COLUMN_TOTAL, Float.valueOf(stringValues[5]));
                    } catch (Exception e) {
                        throw new FormatException(TAG +
                                " -- updateDatabase: exception == " + e.toString() +
                                ", record == " + Arrays.toString(stringValues));
                    }
                    values.put(DatabaseHelper.COLUMN_CHANGED, DatabaseHelper.FALSE);
                    values.put(DatabaseHelper.COLUMN_DELETED, DatabaseHelper.FALSE);
                    operations.add(ContentProviderOperation
                            .newInsert(SyncProvider.URI_DATABASE)
                            .withValues(values)
                            .build());

                    break;
                case DELETE:
                    operations.add(ContentProviderOperation
                            .newDelete(ContentUris.withAppendedId(SyncProvider.URI_DATABASE, id))
                            .build());

                    break;
                default:
                    throw new FormatException(TAG +
                            " -- updateDatabase: error stringValues[0] == " + stringValues[0] +
                            ", record == " + Arrays.toString(stringValues));
            }
        }

        try {
            mProvider.applyBatch(operations);
        } catch (OperationApplicationException e) {
            throw new FormatException(TAG + " -- updateDatabase: applyBatch exception == " + e.toString());
        }
    }

    public void clearDatabase() throws RemoteException {
        mProvider.delete(SyncProvider.URI_DATABASE, null, null);
    }
}