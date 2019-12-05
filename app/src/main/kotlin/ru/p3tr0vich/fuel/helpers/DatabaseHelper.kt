package ru.p3tr0vich.fuel.helpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.IntDef
import ru.p3tr0vich.fuel.models.DatabaseModel
import ru.p3tr0vich.fuel.models.FuelingRecord
import ru.p3tr0vich.fuel.utils.Utils
import ru.p3tr0vich.fuel.utils.UtilsDate
import ru.p3tr0vich.fuel.utils.UtilsLog
import java.util.*

class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DatabaseModel.Database.NAME, null, DatabaseModel.Database.VERSION) {

    //todo: @Parcelize
    class Filter()  {
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(MODE_ALL, MODE_CURRENT_YEAR, MODE_YEAR, MODE_DATES, MODE_TWO_LAST_RECORDS)
        annotation class Mode

        var dateFrom: Long = 0
        var dateTo: Long = 0
        private var year: Int = 0
        @Mode
        var mode: Int = MODE_ALL

        val selection: String
            get() {
                when (mode) {
                    MODE_ALL -> return DatabaseModel.Where.RECORD_NOT_DELETED
                    MODE_TWO_LAST_RECORDS -> {
                        val calendar = Calendar.getInstance()

                        return DatabaseModel.TableFueling.Columns.DATETIME +
                                String.format(Locale.US, DatabaseModel.Where.LESS_OR_EQUAL,
                                        UtilsDate.utcToLocal(calendar.timeInMillis)) +
                                DatabaseModel.Statement.AND + DatabaseModel.Where.RECORD_NOT_DELETED
                    }
                    else -> {
                        val calendarFrom = Calendar.getInstance()
                        val calendarTo = Calendar.getInstance()

                        if (mode == MODE_DATES) {
                            calendarFrom.timeInMillis = dateFrom
                            calendarTo.timeInMillis = dateTo
                        } else {
                            val year = if (mode == MODE_YEAR) this.year else UtilsDate.currentYear

                            calendarFrom.set(year, Calendar.JANUARY, 1)
                            calendarTo.set(year, Calendar.DECEMBER, 31)
                        }

                        UtilsDate.setStartOfDay(calendarFrom)
                        UtilsDate.setEndOfDay(calendarTo)

                        return DatabaseModel.TableFueling.Columns.DATETIME +
                                String.format(Locale.US, DatabaseModel.Where.BETWEEN,
                                        UtilsDate.utcToLocal(calendarFrom.timeInMillis),
                                        UtilsDate.utcToLocal(calendarTo.timeInMillis)) +
                                DatabaseModel.Statement.AND + DatabaseModel.Where.RECORD_NOT_DELETED
                    }
                }
            }

        constructor(year: Int) : this() {
            this.year = year
            mode = MODE_YEAR
        }

        companion object {
            const val MODE_ALL = 0
            const val MODE_CURRENT_YEAR = 1
            const val MODE_YEAR = 2
            const val MODE_DATES = 3
            const val MODE_TWO_LAST_RECORDS = 4
        }
    }

    val twoLastRecords: Cursor
        get() {
            val filter = Filter()
            filter.mode = Filter.MODE_TWO_LAST_RECORDS
            return query(DatabaseModel.TableFueling.Columns.COLUMNS, filter.selection, null,
                    DatabaseModel.TableFueling.Columns.DATETIME + DatabaseModel.Statement.DESC, "2")
        }

    val years: Cursor
        get() {
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "getYears")
            }

            return query(DatabaseModel.TableFueling.Columns.COLUMNS_YEARS, DatabaseModel.Where.RECORD_NOT_DELETED,
                    DatabaseModel.TableFueling.Columns.YEAR, DatabaseModel.TableFueling.Columns.YEAR + DatabaseModel.Statement.DESC)
        }

    override fun onCreate(db: SQLiteDatabase) {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onCreate", "sql == " + DatabaseModel.Database.CREATE_STATEMENT)
        }

        db.execSQL(DatabaseModel.Database.CREATE_STATEMENT)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (LOG_ENABLED)
            UtilsLog.d(TAG, "onUpgrade")
    }

    fun getAll(selection: String): Cursor {
        return query(DatabaseModel.TableFueling.Columns.COLUMNS, selection, null, DatabaseModel.TableFueling.Columns.DATETIME + DatabaseModel.Statement.DESC)
    }

    fun getRecord(id: Long): Cursor {
        return query(DatabaseModel.TableFueling.Columns.COLUMNS, DatabaseModel.TableFueling.Columns.ID + DatabaseModel.Statement.EQUAL + id, null, null)
    }

    fun getSumByMonthsForYear(year: Int): Cursor {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "getSumByMonthsForYear", "year == $year")
        }
        return query(DatabaseModel.TableFueling.Columns.COLUMNS_SUM_BY_MONTHS, Filter(year).selection,
                DatabaseModel.TableFueling.Columns.MONTH, DatabaseModel.TableFueling.Columns.MONTH)
    }

    fun getSyncRecords(getChanged: Boolean): Cursor {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "getSyncRecords", "getChanged == $getChanged")
        }
        return query(DatabaseModel.TableFueling.Columns.COLUMNS_WITH_DELETED,
                if (getChanged) DatabaseModel.TableFueling.Columns.CHANGED + DatabaseModel.Statement.EQUAL + DatabaseModel.Statement.TRUE else null, null, null)
    }

    fun deleteMarkedAsDeleted(): Int {
        return delete(DatabaseModel.TableFueling.Columns.DELETED + DatabaseModel.Statement.EQUAL + DatabaseModel.Statement.TRUE)
    }

    fun updateChanged(): Int {
        val values = ContentValues()
        values.put(DatabaseModel.TableFueling.Columns.CHANGED, DatabaseModel.Statement.FALSE)

        return update(values, DatabaseModel.TableFueling.Columns.CHANGED + DatabaseModel.Statement.EQUAL + DatabaseModel.Statement.TRUE)
    }

    private fun query(columns: Array<String>, selection: String?, groupBy: String?, orderBy: String?, limit: String? = null): Cursor {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "query", "columns == " + columns.contentToString() +
                    ", selection == " + selection + ", groupBy == " + groupBy +
                    ", orderBy == " + orderBy + ", limit == " + limit)
        }

        if (QUERY_WAIT_ENABLED) {
            Utils.wait(3)
        }

        return readableDatabase.query(DatabaseModel.TableFueling.NAME, columns, selection, null, groupBy, null, orderBy, limit)
    }

    fun insert(db: SQLiteDatabase, values: ContentValues): Long {
        return db.insertOrThrow(DatabaseModel.TableFueling.NAME, null, values)
    }

    fun insert(values: ContentValues): Long {
        return insert(writableDatabase, values)
    }

    private fun update(db: SQLiteDatabase, values: ContentValues, whereClause: String): Int {
        return db.update(DatabaseModel.TableFueling.NAME, values, whereClause, null)
    }

    fun update(values: ContentValues, whereClause: String): Int {
        return update(writableDatabase, values, whereClause)
    }

    fun update(values: ContentValues, id: Long): Int {
        return update(values, DatabaseModel.TableFueling.Columns.ID + DatabaseModel.Statement.EQUAL + id)
    }

    private fun delete(db: SQLiteDatabase, whereClause: String?): Int {
        return db.delete(DatabaseModel.TableFueling.NAME, whereClause, null)
    }

    fun delete(whereClause: String?): Int {
        return delete(writableDatabase, whereClause)
    }

    fun delete(id: Long): Int {
        return delete(DatabaseModel.TableFueling.Columns.ID + DatabaseModel.Statement.EQUAL + id)
    }

    companion object {
        private const val TAG = "DatabaseHelper"

        private var LOG_ENABLED = false

        private var QUERY_WAIT_ENABLED = false

        @JvmStatic
        fun getBoolean(cursor: Cursor, columnIndex: Int): Boolean {
            return cursor.getInt(columnIndex) == DatabaseModel.Statement.TRUE
        }

        @JvmStatic
        private fun getValues(id: Long?,
                              dateTime: Long,
                              cost: Float,
                              volume: Float,
                              total: Float,
                              changed: Boolean,
                              deleted: Boolean,
                              convertDate: Boolean): ContentValues {
            val values = ContentValues()

            if (id != null)
                values.put(DatabaseModel.TableFueling.Columns.ID, id)

            values.put(DatabaseModel.TableFueling.Columns.DATETIME,
                    if (convertDate)
                        UtilsDate.utcToLocal(dateTime)
                    else
                        dateTime)
            values.put(DatabaseModel.TableFueling.Columns.COST, cost)
            values.put(DatabaseModel.TableFueling.Columns.VOLUME, volume)
            values.put(DatabaseModel.TableFueling.Columns.TOTAL, total)
            values.put(DatabaseModel.TableFueling.Columns.CHANGED, if (changed) DatabaseModel.Statement.TRUE else DatabaseModel.Statement.FALSE)
            values.put(DatabaseModel.TableFueling.Columns.DELETED, if (deleted) DatabaseModel.Statement.TRUE else DatabaseModel.Statement.FALSE)

            return values
        }

        @JvmStatic
        fun getValuesForSync(id: Long?,
                             dateTime: Long,
                             cost: Float,
                             volume: Float,
                             total: Float): ContentValues {
            return getValues(id, dateTime, cost, volume, total, changed = false, deleted = false, convertDate = false)
        }

        @JvmStatic
        fun getValues(id: Long?,
                      dateTime: Long,
                      cost: Float,
                      volume: Float,
                      total: Float): ContentValues {
            return getValues(id, dateTime, cost, volume, total, changed = true, deleted = false, convertDate = true)
        }

        @JvmStatic
        val valuesMarkAsDeleted: ContentValues
            get() {
                val values = ContentValues()

                values.put(DatabaseModel.TableFueling.Columns.CHANGED, DatabaseModel.Statement.TRUE)
                values.put(DatabaseModel.TableFueling.Columns.DELETED, DatabaseModel.Statement.TRUE)

                return values
            }

        @JvmStatic
        private fun getFuelingRecord(cursor: Cursor, convertDate: Boolean): FuelingRecord {
            val dateTime = cursor.getLong(DatabaseModel.TableFueling.Columns.DATETIME_INDEX)
            return FuelingRecord(
                    cursor.getLong(DatabaseModel.TableFueling.Columns.ID_INDEX),
                    if (convertDate)
                        UtilsDate.localToUtc(dateTime)
                    else
                        dateTime,
                    cursor.getFloat(DatabaseModel.TableFueling.Columns.COST_INDEX),
                    cursor.getFloat(DatabaseModel.TableFueling.Columns.VOLUME_INDEX),
                    cursor.getFloat(DatabaseModel.TableFueling.Columns.TOTAL_INDEX))
        }

        @JvmStatic
        fun getFuelingRecordForSync(cursor: Cursor): FuelingRecord {
            return getFuelingRecord(cursor, false)
        }

        @JvmStatic
        fun getFuelingRecord(cursor: Cursor): FuelingRecord {
            return getFuelingRecord(cursor, true)
        }

        @JvmStatic
        fun getFuelingRecords(cursor: Cursor?): List<FuelingRecord>? {
            if (cursor == null) return null

            val records = ArrayList<FuelingRecord>()

            if (cursor.moveToFirst())
                do
                    records.add(getFuelingRecord(cursor))
                while (cursor.moveToNext())

            return records
        }
    }
}