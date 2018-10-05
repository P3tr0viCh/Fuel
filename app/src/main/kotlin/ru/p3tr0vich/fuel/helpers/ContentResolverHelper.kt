package ru.p3tr0vich.fuel.helpers

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import ru.p3tr0vich.fuel.helpers.ContentProviderHelper.Companion.URI_DATABASE
import ru.p3tr0vich.fuel.helpers.ContentProviderHelper.Companion.URI_DATABASE_SUM_BY_MONTHS
import ru.p3tr0vich.fuel.helpers.ContentProviderHelper.Companion.URI_DATABASE_TWO_LAST_RECORDS
import ru.p3tr0vich.fuel.helpers.ContentProviderHelper.Companion.URI_DATABASE_YEARS
import ru.p3tr0vich.fuel.models.DatabaseModel
import ru.p3tr0vich.fuel.models.FuelingRecord
import java.util.*

object ContentResolverHelper {

    @JvmStatic
    fun getAll(context: Context, filter: DatabaseHelper.Filter): Cursor? {
        return context.contentResolver.query(URI_DATABASE, null,
                filter.selection, null, null, null)
    }

    @JvmStatic
    fun getYears(context: Context): Cursor? {
        return context.contentResolver.query(URI_DATABASE_YEARS, null, null, null, null)
    }

    @JvmStatic
    fun getSumByMonthsForYear(context: Context, year: Int): Cursor? {
        return context.contentResolver.query(
                ContentUris.withAppendedId(URI_DATABASE_SUM_BY_MONTHS, year.toLong()), null, null, null, null)
    }

    @JvmStatic
    fun getTwoLastRecords(context: Context): Cursor? {
        return context.contentResolver.query(URI_DATABASE_TWO_LAST_RECORDS, null, null, null, null, null)
    }

    @JvmStatic
    fun swapRecords(context: Context, fuelingRecordList: List<FuelingRecord>) {
        context.contentResolver.delete(URI_DATABASE, null, null)

        val size = fuelingRecordList.size
        if (size == 0) return

        var fuelingRecord: FuelingRecord

        val values = arrayOfNulls<ContentValues>(size)

        for (i in 0 until size) {
            fuelingRecord = fuelingRecordList[i]

            values[i] = DatabaseHelper.getValues(
                    fuelingRecord.dateTime,
                    fuelingRecord.dateTime,
                    fuelingRecord.cost,
                    fuelingRecord.volume,
                    fuelingRecord.total)
        }

        context.contentResolver.bulkInsert(URI_DATABASE, values)

        context.contentResolver.notifyChange(URI_DATABASE, null, false)
    }

    @JvmStatic
    fun getFuelingRecord(context: Context, id: Long): FuelingRecord? {
        context.contentResolver.query(
                ContentUris.withAppendedId(URI_DATABASE, id), null, null, null, null, null)
                ?.use {
                    if (it.moveToFirst()) {
                        return DatabaseHelper.getFuelingRecord(it)
                    }
                }

        return null
    }

    @JvmStatic
    fun getAllRecordsList(context: Context): List<FuelingRecord> {
        val fuelingRecords = ArrayList<FuelingRecord>()

        context.contentResolver.query(URI_DATABASE, null,
                DatabaseModel.Where.RECORD_NOT_DELETED, null, null, null)
                ?.use {
                    if (it.moveToFirst()) {
                        do
                            fuelingRecords.add(DatabaseHelper.getFuelingRecord(it))
                        while (it.moveToNext())
                    }
                }

        return fuelingRecords
    }

    @JvmStatic
    fun insertRecord(context: Context, fuelingRecord: FuelingRecord): Boolean {
        val result = context.contentResolver.insert(
                URI_DATABASE,
                DatabaseHelper.getValues(
                        fuelingRecord.dateTime,
                        fuelingRecord.dateTime,
                        fuelingRecord.cost,
                        fuelingRecord.volume,
                        fuelingRecord.total)) ?: return false

        context.contentResolver.notifyChange(result, null, false)

        return true
    }

    @JvmStatic
    fun updateRecord(context: Context, fuelingRecord: FuelingRecord): Boolean {
        val uri = ContentUris.withAppendedId(URI_DATABASE, fuelingRecord.id)

        val result = context.contentResolver.update(uri,
                DatabaseHelper.getValues(null,
                        fuelingRecord.dateTime,
                        fuelingRecord.cost,
                        fuelingRecord.volume,
                        fuelingRecord.total), null, null)

        if (result == -1) return false

        context.contentResolver.notifyChange(uri, null, false)

        return true
    }

    @JvmStatic
    fun markRecordAsDeleted(context: Context, id: Long): Boolean {
        val uri = ContentUris.withAppendedId(URI_DATABASE, id)

        val result = context.contentResolver.update(uri,
                DatabaseHelper.valuesMarkAsDeleted, null, null)

        if (result == -1) return false

        context.contentResolver.notifyChange(uri, null, false)

        return true
    }
}