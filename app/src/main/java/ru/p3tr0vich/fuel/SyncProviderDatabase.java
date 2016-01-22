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
import android.support.v4.util.LongSparseArray;
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
                ContentProviderFuel.URI_DATABASE_SYNC_ALL :
                ContentProviderFuel.URI_DATABASE_SYNC_CHANGED, null, null, null, null);

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
        mProvider.delete(ContentProviderFuel.URI_DATABASE_SYNC, null, null);
    }

    public void syncChangedRecords() throws RemoteException {
        mProvider.update(ContentProviderFuel.URI_DATABASE_SYNC, new ContentValues(0), null, null);
    }

    public void updateDatabase(@NonNull List<String> syncRecords)
            throws FormatException, RemoteException {

        int size = syncRecords.size();

        UtilsLog.d(TAG, "updateDatabase", "syncRecords.size() == " + size);

        if (size == 0) return;

        long id;

        String[] stringValues;

        ContentValues values;

        LongSparseArray<ContentValues> records = new LongSparseArray<>();

        for (String syncRecord : syncRecords) {
            if (TextUtils.isEmpty(syncRecord)) continue;

            stringValues = TextUtils.split(syncRecord, SEPARATOR);

//            for (int i = 0; i < stringValues.length; i++)
//                UtilsLog.d(TAG, "updateDatabase", "stringValues[" + i + "] == " + stringValues[i]);

            try {
                id = Long.valueOf(stringValues[1]);
            } catch (Exception e) {
                throw new FormatException(TAG +
                        " -- updateDatabase: exception == " + e.toString() +
                        ", syncRecord == " + Arrays.toString(stringValues));
            }

            switch (stringValues[0]) {
                case ADD:
                    records.put(id,
                            DatabaseHelper.getValues(
                                    id,
                                    Long.valueOf(stringValues[2]),
                                    Float.valueOf(stringValues[3]),
                                    Float.valueOf(stringValues[4]),
                                    Float.valueOf(stringValues[5]),
                                    false,
                                    false));

                    break;
                case DELETE:
                    records.put(id, null);

                    break;
                default:
                    throw new FormatException(TAG +
                            " -- updateDatabase: error stringValues[0] == " + stringValues[0] +
                            ", syncRecord == " + Arrays.toString(stringValues));
            }
        }

        size = records.size();

        if (size == 0) return;

        ContentProviderOperation operation;
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            values = records.valueAt(i);

            if (values != null)
                operation = ContentProviderOperation
                        .newInsert(ContentProviderFuel.URI_DATABASE)
                        .withValues(values)
                        .build();
            else
                operation = ContentProviderOperation
                        .newDelete(ContentUris.withAppendedId(ContentProviderFuel.URI_DATABASE,
                                records.keyAt(i)))
                        .build();

            operations.add(operation);
        }

        size = operations.size();

        UtilsLog.d(TAG, "updateDatabase", "operations.size() == " + size);

        if (size == 0) return;

        try {
            mProvider.applyBatch(operations);
        } catch (OperationApplicationException e) {
            throw new FormatException(TAG + " -- updateDatabase: applyBatch exception == " + e.toString());
        }
    }

    public void clearDatabase() throws RemoteException {
        mProvider.delete(ContentProviderFuel.URI_DATABASE, null, null);
    }
}