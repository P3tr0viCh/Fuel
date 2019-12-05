package ru.p3tr0vich.fuel.sync

import android.content.*
import android.nfc.FormatException
import android.os.RemoteException
import android.text.TextUtils
import androidx.collection.LongSparseArray
import ru.p3tr0vich.fuel.helpers.ContentProviderHelper
import ru.p3tr0vich.fuel.helpers.DatabaseHelper
import ru.p3tr0vich.fuel.models.DatabaseModel
import ru.p3tr0vich.fuel.models.FuelingRecord
import ru.p3tr0vich.fuel.utils.UtilsLog
import java.util.*

internal class SyncProviderDatabase(private val provider: ContentProviderClient) {

    @Throws(RemoteException::class)
    fun getSyncRecords(getAllRecords: Boolean, addDeleteAll: Boolean): List<String> {
        val result = ArrayList<String>()

        val cursor = provider.query(
                if (getAllRecords) {
                    ContentProviderHelper.URI_DATABASE_SYNC_ALL
                } else {
                    ContentProviderHelper.URI_DATABASE_SYNC_CHANGED
                }, null, null, null, null)

        if (addDeleteAll) {
            result.add(CLEAR)
        }

        if (cursor != null) {
            var fuelingRecord: FuelingRecord

            if (cursor.moveToFirst()) {
                do {
                    fuelingRecord = DatabaseHelper.getFuelingRecordForSync(cursor)

                    result.add(
                            if (DatabaseHelper.getBoolean(cursor, DatabaseModel.TableFueling.Columns.DELETED_INDEX)) {
                                DELETE + SEPARATOR + fuelingRecord.id.toString()
                            } else {
                                INSERT + SEPARATOR +
                                        fuelingRecord.id.toString() + SEPARATOR +
                                        fuelingRecord.dateTime.toString() + SEPARATOR +
                                        fuelingRecord.cost.toString() + SEPARATOR +
                                        fuelingRecord.volume.toString() + SEPARATOR +
                                        fuelingRecord.total.toString()
                            })
                } while (cursor.moveToNext())
            }
            cursor.close()
        }

        return result
    }

    @Throws(RemoteException::class)
    fun syncDeletedRecords() {
        // Полностью удалить записи, отмеченные как удалённые
        provider.delete(ContentProviderHelper.URI_DATABASE_SYNC, null, null)
    }

    @Throws(RemoteException::class)
    fun syncChangedRecords() {
        // Изменить записи, помеченные как изменённые, как не изменённые
        provider.update(ContentProviderHelper.URI_DATABASE_SYNC, ContentValues(0), null, null)
    }

    @Throws(FormatException::class, RemoteException::class)
    fun updateDatabase(syncRecords: List<String>) {

        var size = syncRecords.size

        UtilsLog.d(TAG, "updateDatabase", "syncRecords.size() == $size")

        if (size == 0) return

        var id: Long

        var clearDatabase = false

        var stringValues: Array<String>

        var values: ContentValues?

        val records = LongSparseArray<ContentValues>()

        for (syncRecord in syncRecords) {
            if (TextUtils.isEmpty(syncRecord)) continue

            stringValues = TextUtils.split(syncRecord, SEPARATOR)

            //            for (int i = 0; i < stringValues.length; i++)
            //                UtilsLog.d(TAG, "updateDatabase", "stringValues[" + i + "] == " + stringValues[i]);

            if (CLEAR == stringValues[0]) {
                clearDatabase = true
            } else {
                try {
                    id = java.lang.Long.valueOf(stringValues[1])
                } catch (e: Exception) {
                    throw FormatException("$TAG -- updateDatabase: error id, syncRecord == ${stringValues.contentToString()}")
                }

                when (stringValues[0]) {
                    INSERT -> records.put(id,
                            DatabaseHelper.getValuesForSync(
                                    id,
                                    java.lang.Long.valueOf(stringValues[2]),
                                    java.lang.Float.valueOf(stringValues[3]),
                                    java.lang.Float.valueOf(stringValues[4]),
                                    java.lang.Float.valueOf(stringValues[5])))
                    DELETE -> records.put(id, null)
                    else -> throw FormatException("$TAG -- updateDatabase: error stringValues[0] == ${stringValues[0]}, syncRecord == ${stringValues.contentToString()}")
                }
            }
        }

        var operation: ContentProviderOperation
        val operations = ArrayList<ContentProviderOperation>()

        if (clearDatabase) {
            operations.add(ContentProviderOperation
                    .newDelete(ContentProviderHelper.URI_DATABASE)
                    .build())
        }

        size = records.size()

        for (i in 0 until size) {
            values = records.valueAt(i)

            operation = if (values != null) {
                ContentProviderOperation
                        .newInsert(ContentProviderHelper.URI_DATABASE)
                        .withValues(values)
                        .build()
            } else {
                ContentProviderOperation
                        .newDelete(ContentUris.withAppendedId(ContentProviderHelper.URI_DATABASE,
                                records.keyAt(i)))
                        .build()
            }

            operations.add(operation)
        }

        size = operations.size

        UtilsLog.d(TAG, "updateDatabase", "operations.size() == $size")

        if (size == 0) return

        try {
            provider.applyBatch(operations)
        } catch (e: OperationApplicationException) {
            throw FormatException("$TAG -- updateDatabase: applyBatch exception == $e")
        }

    }

    companion object {
        private const val TAG = "SyncProviderDatabase"

        private const val INSERT = "+"
        private const val DELETE = "-"
        private const val CLEAR = "~"

        private const val SEPARATOR = "\t"
    }
}