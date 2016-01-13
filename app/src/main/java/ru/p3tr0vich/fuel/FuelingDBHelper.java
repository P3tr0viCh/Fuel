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
    private static final String COLUMN_SYNC_ID = "sync_id";
    private static final String COLUMN_CHANGED = "changed";
    private static final String COLUMN_DELETED = "deleted";

    private static final String COLUMN_YEAR = "year";
    private static final String COLUMN_MONTH = "month";

    private static final int DATABASE_VERSION = 1;

    private static final String FUEL_DB = "fuel.db";

    private static final String TABLE_FUELING = "fueling";

    private static final String SELECT = "SELECT ";
    private static final String DELETE = "DELETE";
    private static final String FROM = " FROM ";
    private static final String WHERE = " WHERE ";
    private static final String GROUP_BY = " GROUP BY ";
    private static final String ORDER_BY = " ORDER BY ";
    private static final String AND = " AND ";
    private static final String AS = " AS ";

    private static final String TABLE_FUELING_COLUMNS =
            _ID + ", " + COLUMN_DATETIME + ", " +
                    COLUMN_COST + ", " + COLUMN_VOLUME + ", " + COLUMN_TOTAL;
    private static final String TABLE_FUELING_COLUMNS_WITH_SYNC =
            TABLE_FUELING_COLUMNS + ", " +
                    COLUMN_SYNC_ID + ", " + COLUMN_DELETED;

    public static final int TABLE_FUELING_COLUMN_ID_INDEX = 0;
    public static final int TABLE_FUELING_COLUMN_DATETIME_INDEX = 1;
    public static final int TABLE_FUELING_COLUMN_COST_INDEX = 2;
    public static final int TABLE_FUELING_COLUMN_VOLUME_INDEX = 3;
    public static final int TABLE_FUELING_COLUMN_TOTAL_INDEX = 4;
    public static final int TABLE_FUELING_COLUMN_SYNC_ID_INDEX = 5;
    public static final int TABLE_FUELING_COLUMN_DELETED_INDEX = 6;

    private static final String EQUAL = "=";
    private static final String TRUE = "1";
    private static final String FALSE = "0";

    private static final String SELECT_ALL = SELECT + TABLE_FUELING_COLUMNS + FROM + TABLE_FUELING;

    private static final String SELECT_YEARS = SELECT +
            "strftime('%Y', " + COLUMN_DATETIME + "/1000, 'unixepoch', 'localtime')" + AS + COLUMN_YEAR +
            FROM + TABLE_FUELING;

    private static final String WHERE_ID = _ID + EQUAL + "%d";
    private static final String WHERE_RECORD_NOT_DELETED = COLUMN_DELETED + EQUAL + FALSE;
    private static final String WHERE_YEAR = COLUMN_YEAR + "<'%d'";

    private static final String SELECT_SUM_BY_MONTHS = SELECT +
            "SUM(" + COLUMN_COST + "), " +
            "strftime('%m', " + COLUMN_DATETIME + "/1000, 'unixepoch', 'localtime')" + AS + COLUMN_MONTH +
            FROM + TABLE_FUELING;

    private static final String SELECT_SYNC_ALL = SELECT + TABLE_FUELING_COLUMNS_WITH_SYNC +
            FROM + TABLE_FUELING;
    private static final String SELECT_SYNC_CHANGED = SELECT + TABLE_FUELING_COLUMNS_WITH_SYNC +
            FROM + TABLE_FUELING +
            WHERE + COLUMN_CHANGED + EQUAL + TRUE;

    private static final String BETWEEN_DATES = " BETWEEN %1$d AND %2$d";

    private static final String GROUP_BY_MONTH = GROUP_BY + COLUMN_MONTH;
    private static final String GROUP_BY_YEAR = GROUP_BY + COLUMN_YEAR;
    private static final String ORDER_BY_DATETIME = ORDER_BY + COLUMN_DATETIME + " DESC";
    private static final String ORDER_BY_YEAR = ORDER_BY + COLUMN_YEAR + " ASC";

    private static final String CREATE_TABLE_FUELING = "CREATE TABLE " + TABLE_FUELING + "(" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_DATETIME + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, " +
            COLUMN_COST + " REAL DEFAULT 0, " +
            COLUMN_VOLUME + " REAL DEFAULT 0, " +
            COLUMN_TOTAL + " REAL DEFAULT 0, " +
            COLUMN_SYNC_ID + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, " +
            COLUMN_CHANGED + " INTEGER DEFAULT " + TRUE + ", " +
            COLUMN_DELETED + " INTEGER DEFAULT " + FALSE +
            ");";

    private static final String CLEAR_TABLE_FUELING = DELETE + FROM + TABLE_FUELING;

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

    public static boolean getBoolean(@NonNull Cursor cursor, int columnIndex) {
        return cursor.getString(columnIndex).equals(TRUE);
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
        execSQL(db, CREATE_TABLE_FUELING, "onCreate");
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
            Cursor cursor = rawQuery(SELECT_ALL +
                    WHERE + String.format(WHERE_ID, id), "getFuelingRecord");

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

        final long dataTime = fuelingRecord.getDateTime();

        values.put(COLUMN_DATETIME, dataTime);
        values.put(COLUMN_COST, fuelingRecord.getCost());
        values.put(COLUMN_VOLUME, fuelingRecord.getVolume());
        values.put(COLUMN_TOTAL, fuelingRecord.getTotal());
        values.put(COLUMN_SYNC_ID, dataTime);
        values.put(COLUMN_CHANGED, TRUE);
        values.put(COLUMN_DELETED, FALSE);

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
            values.put(COLUMN_CHANGED, TRUE);
            values.put(COLUMN_DELETED, FALSE);

            return db.update(TABLE_FUELING,
                    values, _ID + "=?", new String[]{String.valueOf(fuelingRecord.getId())});
        } finally {
            db.close();
        }
    }

    public int deleteRecord(@NonNull FuelingRecord fuelingRecord) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            ContentValues values = new ContentValues();

            values.put(COLUMN_CHANGED, TRUE);
            values.put(COLUMN_DELETED, TRUE);

            return db.update(TABLE_FUELING,
                    values, _ID + "=?", new String[]{String.valueOf(fuelingRecord.getId())});
        } finally {
            db.close();
        }
    }

    @NonNull
    private String filterModeToSql() {
        if (mFilter.filterMode == FILTER_MODE_ALL)
            return WHERE + WHERE_RECORD_NOT_DELETED;
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
                    String.format(BETWEEN_DATES, dateFrom.getTimeInMillis(), dateTo.getTimeInMillis()) +
                    AND + WHERE_RECORD_NOT_DELETED;
        }
    }

    public Cursor getAllCursor() {
        return rawQuery(SELECT_ALL + filterModeToSql() + ORDER_BY_DATETIME,
                "getAllCursor");
    }

    public Cursor getYears() {
        return rawQuery(SELECT_YEARS +
                        WHERE + String.format(WHERE_YEAR, UtilsDate.getCurrentYear()) +
                        AND + WHERE_RECORD_NOT_DELETED +
                        GROUP_BY_YEAR + ORDER_BY_YEAR,
                "getYears");
    }

    public Cursor getSumByMonthsForYear() {
        return rawQuery(SELECT_SUM_BY_MONTHS + filterModeToSql() + GROUP_BY_MONTH,
                "getSumByMonthsForYear");
    }

    public Cursor getSyncChangedRecords() {
        return rawQuery(SELECT_SYNC_CHANGED, "getSyncChangedRecords");
    }

    public Cursor getSyncAllRecords() {
        return rawQuery(SELECT_SYNC_ALL, "getSyncAllRecords");
    }

    public void clearSyncRecords() {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.beginTransaction();
            try {
                db.delete(TABLE_FUELING, COLUMN_DELETED + EQUAL + TRUE, null);

                ContentValues values = new ContentValues();
                values.put(COLUMN_CHANGED, FALSE);

                db.update(TABLE_FUELING, values, COLUMN_CHANGED + EQUAL + TRUE, null);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } finally {
            db.close();
        }
    }

    @NonNull
    public List<FuelingRecord> getAllRecordsList() {
        List<FuelingRecord> fuelingRecords = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        try {
            Cursor cursor = rawQuery(db,
                    SELECT_ALL + WHERE + WHERE_RECORD_NOT_DELETED + ORDER_BY_DATETIME,
                    "getAllRecordsList");
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