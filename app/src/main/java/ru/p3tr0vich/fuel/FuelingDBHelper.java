package ru.p3tr0vich.fuel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
class FuelingDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "FuelingDBHelper";

    private static final String _ID = "_id";
    private static final String COLUMN_DATETIME = "datetime";
    private static final String COLUMN_COST = "cost";
    private static final String COLUMN_VOLUME = "volume";
    private static final String COLUMN_TOTAL = "total";

    private static final String COLUMN_YEAR = "year";
    private static final String COLUMN_MONTH = "month";
    private static final String COLUMN_REVISION = "revision";
    private static final String COLUMN_IS_CHANGED = "is_changed";

    public static final int TABLE_FUELING_COLUMN_ID_INDEX = 0;
    public static final int TABLE_FUELING_COLUMN_DATETIME_INDEX = 1;
    public static final int TABLE_FUELING_COLUMN_COST_INDEX = 2;
    public static final int TABLE_FUELING_COLUMN_VOLUME_INDEX = 3;
    public static final int TABLE_FUELING_COLUMN_TOTAL_INDEX = 4;

    public static final int TABLE_SYNC_DELETED_COLUMN_DATETIME_INDEX = 0;

    private static final int DATABASE_VERSION = 1;

    private static final String FUEL_DB = "fuel.db";

    private static final String TABLE_FUELING = "fueling";
    private static final String TABLE_SYNC_CHANGED = "sync_changed";
    private static final String TABLE_SYNC_DELETED = "sync_deleted";
    private static final String TABLE_SYNC_REVISION = "sync_revision";

    private static final String TRIGGER_SYNC_INSERT = "sync_insert";
    private static final String TRIGGER_SYNC_UPDATE = "sync_update";
    private static final String TRIGGER_SYNC_DELETE = "sync_delete";

    private static final String SELECT = "SELECT ";
    private static final String FROM = " FROM ";
    private static final String WHERE = " WHERE ";
    private static final String GROUP_BY = " GROUP BY ";
    private static final String ORDER_BY = " ORDER BY ";
    private static final String AS = " AS ";

    private static final String ALL = "*";

    private static final String SELECT_ALL = SELECT + ALL + FROM + TABLE_FUELING;
    private static final String SELECT_ALL_WHERE_ID = WHERE + _ID + "=%d";

    private static final String SELECT_YEARS = SELECT +
            "strftime('%Y', " + COLUMN_DATETIME + "/1000, 'unixepoch', 'localtime')" + AS + COLUMN_YEAR +
            FROM + TABLE_FUELING;
    private static final String SELECT_YEARS_WHERE = WHERE + COLUMN_YEAR + "<'%d'" +
            GROUP_BY + COLUMN_YEAR + ORDER_BY + COLUMN_YEAR + " ASC";

    private static final String SELECT_SUM_BY_MONTHS_IN_YEAR = SELECT +
            "SUM(" + COLUMN_COST + "), " +
            "strftime('%m', " + COLUMN_DATETIME + "/1000, 'unixepoch', 'localtime')" + AS + COLUMN_MONTH +
            FROM + TABLE_FUELING;

    private static final String SELECT_REVISION =
            SELECT + COLUMN_REVISION + FROM + TABLE_SYNC_REVISION;

    private static final String SELECT_IS_CHANGED = SELECT +
            "(SELECT COUNT(*)" + FROM + TABLE_SYNC_CHANGED + ") + " +
            "(SELECT COUNT(*)" + FROM + TABLE_SYNC_DELETED + ")" +
            AS + COLUMN_IS_CHANGED;

    private static final String SELECT_SYNC_CHANGED = SELECT + TABLE_FUELING + "." + ALL +
            FROM + TABLE_FUELING + ", " + TABLE_SYNC_CHANGED +
            WHERE +
            TABLE_FUELING + "." + COLUMN_DATETIME + "=" + TABLE_SYNC_CHANGED + "." + COLUMN_DATETIME;

    private static final String SELECT_SYNC_DELETED =
            SELECT + COLUMN_DATETIME + FROM + TABLE_SYNC_DELETED;

    private static final String IN_DATES = " BETWEEN %1$d AND %2$d";

    private static final String GROUP_BY_MONTH = GROUP_BY + COLUMN_MONTH;
    private static final String ORDER_BY_DATETIME = ORDER_BY + COLUMN_DATETIME + " DESC";

    private static final String CREATE_TABLE_FUELING = "CREATE TABLE " + TABLE_FUELING + "(" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_DATETIME + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, " +
            COLUMN_COST + " REAL DEFAULT 0, " +
            COLUMN_VOLUME + " REAL DEFAULT 0, " +
            COLUMN_TOTAL + " REAL DEFAULT 0" +
            ");";
    private static final String CREATE_TABLE_SYNC_CHANGED = "CREATE TABLE " + TABLE_SYNC_CHANGED + "(" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_DATETIME + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE" +
            ");";
    private static final String CREATE_TABLE_SYNC_DELETED = "CREATE TABLE " + TABLE_SYNC_DELETED + "(" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_DATETIME + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE" +
            ");";
    private static final String CREATE_TABLE_SYNC_REVISION = "CREATE TABLE " + TABLE_SYNC_REVISION + "(" +
            COLUMN_REVISION + " INTEGER PRIMARY KEY" +
            ");";

    // Вставка новой записи
    private static final String CREATE_TRIGGER_SYNC_INSERT = "CREATE TRIGGER " + TRIGGER_SYNC_INSERT +
            " AFTER INSERT ON " + TABLE_FUELING + " FOR EACH ROW" +
            " BEGIN" +

            // Вставляем в таблицу новых или изменённых записей
            " INSERT OR REPLACE INTO " + TABLE_SYNC_CHANGED +
            "(" + COLUMN_DATETIME + ")" +
            " VALUES (NEW." + COLUMN_DATETIME + ");" +

            // Удаляем из таблицы удалённых записей
            " DELETE FROM " + TABLE_SYNC_DELETED +
            " WHERE " + COLUMN_DATETIME + "=NEW." + COLUMN_DATETIME + ";" +
            " END;";
    // Изменение существующей записи. Может измениться поле даты
    private static final String CREATE_TRIGGER_SYNC_UPDATE = "CREATE TRIGGER " + TRIGGER_SYNC_UPDATE +
            " AFTER UPDATE ON " + TABLE_FUELING + " FOR EACH ROW" +
            " BEGIN" +

            // TODO: CASE, IF in Sqlite?

            // Если изменилось поле даты
            " CASE " +
            " WHEN NEW." + COLUMN_DATETIME + "<>OLD." + COLUMN_DATETIME +
            " THEN " +
            // Удаляем из таблицы новых или изменённых записей старую дату
            " DELETE FROM " + TABLE_SYNC_CHANGED +
            " WHERE " + COLUMN_DATETIME + "=OLD." + COLUMN_DATETIME + ";" +
            // Вставляем в таблицу удалённых записей старую дату
            " INSERT OR REPLACE INTO " + TABLE_SYNC_DELETED +
            "(" + COLUMN_DATETIME + ")" +
            " VALUES (OLD." + COLUMN_DATETIME + ");" +
            " END;" +

            // Вставляем в таблицу новых или изменённых записей
            " INSERT OR REPLACE INTO " + TABLE_SYNC_CHANGED +
            "(" + COLUMN_DATETIME + ")" +
            " VALUES (NEW." + COLUMN_DATETIME + ");" +

            // Удаляем из таблицы удалённых записей
            " DELETE FROM " + TABLE_SYNC_DELETED +
            " WHERE " + COLUMN_DATETIME + "=NEW." + COLUMN_DATETIME + ";" +
            " END;";
    // Удаление записи
    private static final String CREATE_TRIGGER_SYNC_DELETE = "CREATE TRIGGER " + TRIGGER_SYNC_DELETE +
            " AFTER DELETE ON " + TABLE_FUELING + " FOR EACH ROW" +
            " BEGIN" +

            // Вставляем в таблицу удалённых записей
            " INSERT OR REPLACE INTO " + TABLE_SYNC_DELETED +
            "(" + COLUMN_DATETIME + ")" +
            " VALUES (OLD." + COLUMN_DATETIME + ");" +

            // Удаляем из таблицы новых или изменённых записей
            " DELETE FROM " + TABLE_SYNC_CHANGED +
            " WHERE " + COLUMN_DATETIME + "=OLD." + COLUMN_DATETIME + ";" +
            " END;";

    private static final String CLEAR_TABLE = "DELETE FROM ";

    private static final String CLEAR_TABLE_FUELING = CLEAR_TABLE + TABLE_FUELING;
    private static final String CLEAR_TABLE_SYNC_CHANGED = CLEAR_TABLE + TABLE_SYNC_CHANGED;
    private static final String CLEAR_TABLE_SYNC_DELETED = CLEAR_TABLE + TABLE_SYNC_DELETED;
    private static final String CLEAR_TABLE_SYNC_REVISION = CLEAR_TABLE + TABLE_SYNC_REVISION;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FILTER_MODE_ALL, FILTER_MODE_CURRENT_YEAR, FILTER_MODE_YEAR, FILTER_MODE_DATES})
    public @interface FilterMode {
    }

    public static final int FILTER_MODE_ALL = 0;
    public static final int FILTER_MODE_CURRENT_YEAR = 1;
    public static final int FILTER_MODE_YEAR = 2;
    public static final int FILTER_MODE_DATES = 3;

    static class Filter {
        public Date dateFrom;
        public Date dateTo;
        public int year;
        @FilterMode
        public int filterMode = FILTER_MODE_ALL;
    }

    private final Filter mFilter;

    public FuelingDBHelper() {
        super(ApplicationFuel.getContext(), FUEL_DB, null, DATABASE_VERSION);
        mFilter = new Filter();
    }

    public FuelingDBHelper(@NonNull Filter filter) {
        this();
        setFilter(filter);
    }

    private void execSQL(@NonNull SQLiteDatabase db, @NonNull String sql, @NonNull String function) {
        UtilsLog.d(TAG, function, "sql == " + sql);
        db.execSQL(sql);
    }

    private Cursor rawQuery(@Nullable SQLiteDatabase db, @NonNull String sql, @NonNull String function) {
        UtilsLog.d(TAG, function, "sql == " + sql);
        if (db == null) db = getReadableDatabase();
        return db.rawQuery(sql, null);
    }

    private Cursor rawQuery(@NonNull String sql, @NonNull String function) {
        return rawQuery(null, sql, function);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            execSQL(db, CREATE_TABLE_FUELING, "onCreate");

            execSQL(db, CREATE_TABLE_SYNC_CHANGED, "onCreate");
            execSQL(db, CREATE_TABLE_SYNC_DELETED, "onCreate");
            execSQL(db, CREATE_TABLE_SYNC_REVISION, "onCreate");

            execSQL(db, CREATE_TRIGGER_SYNC_INSERT, "onCreate");
            execSQL(db, CREATE_TRIGGER_SYNC_UPDATE, "onCreate");
            execSQL(db, CREATE_TRIGGER_SYNC_DELETE, "onCreate");

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        UtilsLog.d(TAG, "onUpgrade");
    }

    private void setFilter(@NonNull Filter filter) {
        mFilter.filterMode = filter.filterMode;
        mFilter.year = filter.year;
        mFilter.dateFrom = filter.dateFrom;
        mFilter.dateTo = filter.dateTo;
    }

    @Nullable
    public FuelingRecord getFuelingRecord(long id) {
        FuelingRecord fuelingRecord = null;

        SQLiteDatabase db = getReadableDatabase();

        try {
//            Cursor cursor = db.query(TABLE_FUELING,
//                    new String[]{_ID, COLUMN_DATETIME, COLUMN_COST, COLUMN_VOLUME, COLUMN_TOTAL},
//                    _ID + "=?",
//                    new String[]{String.valueOf(id)}, null, null, null);

            Cursor cursor = rawQuery(SELECT_ALL +
                    String.format(SELECT_ALL_WHERE_ID, id), "getFuelingRecord");

            if (cursor != null)
                try {
                    if (cursor.moveToFirst())
                        fuelingRecord = new FuelingRecord(cursor);
                } finally {
                    cursor.close();
                }
        } finally {
            db.close();
        }

        return fuelingRecord;
    }

    private long doInsertRecord(@NonNull SQLiteDatabase db, @NonNull FuelingRecord fuelingRecord) {
        ContentValues values = new ContentValues();

        values.put(COLUMN_DATETIME, fuelingRecord.getDateTime());
        values.put(COLUMN_COST, fuelingRecord.getCost());
        values.put(COLUMN_VOLUME, fuelingRecord.getVolume());
        values.put(COLUMN_TOTAL, fuelingRecord.getTotal());

        return db.insert(TABLE_FUELING, null, values); // TODO: add throw
    }

    public long insertRecord(@NonNull FuelingRecord fuelingRecord) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            return doInsertRecord(db, fuelingRecord);
        } finally {
            db.close();
        }
    }

    public void swapRecords(@NonNull List<FuelingRecord> fuelingRecordList) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            execSQL(db, CLEAR_TABLE_FUELING, "swapRecords");

            db.beginTransaction();
            try {
                for (FuelingRecord fuelingRecord : fuelingRecordList)
                    doInsertRecord(db, fuelingRecord);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } finally {
            db.close();
        }
    }

    public int updateRecord(@NonNull FuelingRecord fuelingRecord) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            ContentValues values = new ContentValues();

            values.put(COLUMN_DATETIME, fuelingRecord.getDateTime());
            values.put(COLUMN_COST, fuelingRecord.getCost());
            values.put(COLUMN_VOLUME, fuelingRecord.getVolume());
            values.put(COLUMN_TOTAL, fuelingRecord.getTotal());

            return db.update(TABLE_FUELING,
                    values, _ID + "=?", new String[]{String.valueOf(fuelingRecord.getId())});
        } finally {
            db.close();
        }
    }

    public int deleteRecord(@NonNull FuelingRecord fuelingRecord) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            return db.delete(TABLE_FUELING,
                    _ID + "=?", new String[]{String.valueOf(fuelingRecord.getId())});
        } finally {
            db.close();
        }
    }

    @NonNull
    private String filterModeToSql() {
        if (mFilter.filterMode == FILTER_MODE_ALL)
            return "";
        else {
            Calendar dateFrom = Calendar.getInstance();
            Calendar dateTo = Calendar.getInstance();

            if (mFilter.filterMode == FILTER_MODE_DATES) {
                dateFrom.setTimeInMillis(mFilter.dateFrom.getTime());
                dateTo.setTimeInMillis(mFilter.dateTo.getTime());
            } else {
                int year = mFilter.filterMode == FILTER_MODE_YEAR ?
                        mFilter.year : UtilsDate.getCurrentYear();

                dateFrom.set(year, Calendar.JANUARY, 1);

                dateTo.set(year, Calendar.DECEMBER, 31);
            }

            UtilsDate.setStartOfDay(dateFrom);
            UtilsDate.setEndOfDay(dateTo);

            return WHERE + COLUMN_DATETIME +
                    String.format(IN_DATES, dateFrom.getTimeInMillis(), dateTo.getTimeInMillis());
        }
    }

    public Cursor getAllCursor() {
        return rawQuery(SELECT_ALL + filterModeToSql() + ORDER_BY_DATETIME,
                "getAllCursor");
    }

    public Cursor getYears() {
        return rawQuery(SELECT_YEARS + String.format(SELECT_YEARS_WHERE, UtilsDate.getCurrentYear()),
                "getYears");
    }

    public Cursor getSumByMonthsForYear() {
        return rawQuery(SELECT_SUM_BY_MONTHS_IN_YEAR + filterModeToSql() + GROUP_BY_MONTH,
                "getSumByMonthsForYear");
    }

    public Cursor getRevision() {
        return rawQuery(SELECT_REVISION, "getRevision");
    }

    public Cursor isChanged() {
        return rawQuery(SELECT_IS_CHANGED, "isChanged");
    }

    public Cursor getChangedRecords() {
        return rawQuery(SELECT_SYNC_CHANGED, "getChangedRecords");
    }

    public Cursor getDeletedRecords() {
        return rawQuery(SELECT_SYNC_DELETED, "getDeletedRecords");
    }

    public Cursor getAllRecords() {
        return rawQuery(SELECT_ALL + ORDER_BY_DATETIME, "getAllRecords");
    }

    @NonNull
    public List<FuelingRecord> getAllRecordsList() {
        List<FuelingRecord> fuelingRecords = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        try {
            Cursor cursor = rawQuery(db, SELECT_ALL + ORDER_BY_DATETIME, "getAllRecordsList");
            if (cursor != null)
                try {
                    if (cursor.moveToFirst()) do
                        fuelingRecords.add(new FuelingRecord(cursor));
                    while (cursor.moveToNext());
                } finally {
                    cursor.close();
                }
        } finally {
            db.close();
        }

        return fuelingRecords;
    }
}