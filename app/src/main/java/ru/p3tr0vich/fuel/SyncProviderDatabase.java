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

import ru.p3tr0vich.fuel.helpers.ContentProviderHelper;
import ru.p3tr0vich.fuel.helpers.DatabaseHelper;
import ru.p3tr0vich.fuel.models.FuelingRecord;
import ru.p3tr0vich.fuel.utils.UtilsLog;

class SyncProviderDatabase {

    private static final String TAG = "SyncProviderDatabase";

    private static final String INSERT = "+";
    private static final String DELETE = "-";
    private static final String CLEAR = "~";

    private static final String SEPARATOR = "\t";

    private final ContentProviderClient mProvider;

    SyncProviderDatabase(ContentProviderClient provider) {
        mProvider = provider;
    }

    @NonNull
    public List<String> getSyncRecords(boolean getAllRecords, boolean addDeleteAll) throws RemoteException {
        List<String> result = new ArrayList<>();

        Cursor cursor = mProvider.query(getAllRecords ?
                ContentProviderHelper.Companion.getURI_DATABASE_SYNC_ALL() :
                ContentProviderHelper.Companion.getURI_DATABASE_SYNC_CHANGED(), null, null, null, null);

        if (addDeleteAll) result.add(CLEAR);

        if (cursor != null) {
            FuelingRecord fuelingRecord;

            if (cursor.moveToFirst()) {
                do {
                    fuelingRecord = DatabaseHelper.getFuelingRecordForSync(cursor);

                    result.add(DatabaseHelper.getBoolean(cursor, DatabaseHelper.TableFueling.Columns.DELETED_INDEX) ?
                            DELETE + SEPARATOR +
                                    Long.toString(fuelingRecord.getId()) :
                            INSERT + SEPARATOR +
                                    Long.toString(fuelingRecord.getId()) + SEPARATOR +
                                    Long.toString(fuelingRecord.getDateTime()) + SEPARATOR +
                                    Float.toString(fuelingRecord.getCost()) + SEPARATOR +
                                    Float.toString(fuelingRecord.getVolume()) + SEPARATOR +
                                    Float.toString(fuelingRecord.getTotal()));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return result;
    }

    public void syncDeletedRecords() throws RemoteException {
        // Полностью удалить записи, отмеченные как удалённые
        mProvider.delete(ContentProviderHelper.Companion.getURI_DATABASE_SYNC(), null, null);
    }

    public void syncChangedRecords() throws RemoteException {
        // Изменить записи, помеченные как изменённые, как не изменённые
        mProvider.update(ContentProviderHelper.Companion.getURI_DATABASE_SYNC(), new ContentValues(0), null, null);
    }

    public void updateDatabase(@NonNull List<String> syncRecords)
            throws FormatException, RemoteException {

        int size = syncRecords.size();

        UtilsLog.d(TAG, "updateDatabase", "syncRecords.size() == " + size);

        if (size == 0) return;

        long id;

        boolean clearDatabase = false;

        String[] stringValues;

        ContentValues values;

        LongSparseArray<ContentValues> records = new LongSparseArray<>();

        for (String syncRecord : syncRecords) {
            if (TextUtils.isEmpty(syncRecord)) continue;

            stringValues = TextUtils.split(syncRecord, SEPARATOR);

//            for (int i = 0; i < stringValues.length; i++)
//                UtilsLog.d(TAG, "updateDatabase", "stringValues[" + i + "] == " + stringValues[i]);

            if (CLEAR.equals(stringValues[0]))
                clearDatabase = true;
            else {
                try {
                    id = Long.valueOf(stringValues[1]);
                } catch (Exception e) {
                    throw new FormatException(TAG +
                            " -- updateDatabase: error id, syncRecord == " + Arrays.toString(stringValues));
                }

                switch (stringValues[0]) {
                    case INSERT:
                        records.put(id,
                                DatabaseHelper.getValuesForSync(
                                        id,
                                        Long.valueOf(stringValues[2]),
                                        Float.valueOf(stringValues[3]),
                                        Float.valueOf(stringValues[4]),
                                        Float.valueOf(stringValues[5])));

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
        }

        ContentProviderOperation operation;
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        if (clearDatabase)
            operations.add(ContentProviderOperation
                    .newDelete(ContentProviderHelper.Companion.getURI_DATABASE())
                    .build());

        size = records.size();

        for (int i = 0; i < size; i++) {
            values = records.valueAt(i);

            if (values != null)
                operation = ContentProviderOperation
                        .newInsert(ContentProviderHelper.Companion.getURI_DATABASE())
                        .withValues(values)
                        .build();
            else
                operation = ContentProviderOperation
                        .newDelete(ContentUris.withAppendedId(ContentProviderHelper.Companion.getURI_DATABASE(),
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
}