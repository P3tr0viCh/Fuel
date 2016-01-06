package ru.p3tr0vich.fuel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

class FuelingDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "FuelingDBHelper";

    private static final String _ID = "_id";
    private static final String COLUMN_DATETIME = "datetime";
    private static final String COLUMN_COST = "cost";
    private static final String COLUMN_VOLUME = "volume";
    private static final String COLUMN_TOTAL = "total";
    private static final String COLUMN_NEW = "new";
    private static final String COLUMN_DELETED = "deleted";

    private static final String COLUMN_YEAR = "year";
    private static final String COLUMN_MONTH = "month";

    public static final int COLUMN_ID_INDEX = 0;
    public static final int COLUMN_DATETIME_INDEX = 1;
    public static final int COLUMN_COST_INDEX = 2;
    public static final int COLUMN_VOLUME_INDEX = 3;
    public static final int COLUMN_TOTAL_INDEX = 4;
//    public static final int COLUMN_NEW_INDEX = 5;
//    public static final int COLUMN_DELETED_INDEX = 6;

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "fuel.db";
    private static final String TABLE_NAME = "fueling";

    private static final int ZERO = 0;
    private static final int TRUE = 1;
    private static final int FALSE = 0;

    private static final String EQUAL = "=";

    private static final String RECORD_NOT_MARKED_AS_DELETED = COLUMN_DELETED + EQUAL + FALSE;

    private static final String SELECT_ALL = "SELECT " +
            _ID + ", " +
            COLUMN_DATETIME + ", " +
            COLUMN_COST + ", " +
            COLUMN_VOLUME + ", " +
            COLUMN_TOTAL +
            " FROM " + TABLE_NAME;
    private static final String SELECT_YEARS = "SELECT" +
            " strftime('%Y', " + COLUMN_DATETIME + "/1000, 'unixepoch', 'localtime') AS " + COLUMN_YEAR +
            " FROM " + TABLE_NAME;
    private static final String SELECT_YEARS_WHERE = " WHERE " + COLUMN_YEAR + "<'%d'" +
            " AND " + RECORD_NOT_MARKED_AS_DELETED +
            " GROUP BY " + COLUMN_YEAR + " ORDER BY " + COLUMN_YEAR + " ASC";

    private static final String SELECT_SUM_BY_MONTHS_IN_YEAR = "SELECT" +
            " SUM(" + COLUMN_COST + ")," +
            " strftime('%m', " + COLUMN_DATETIME + "/1000, 'unixepoch', 'localtime') AS " + COLUMN_MONTH +
            " FROM " + TABLE_NAME;

    private static final String WHERE = " WHERE ";
    private static final String IN_DATES = " BETWEEN %1$d AND %2$d";

    private static final String GROUP_BY_MONTH = " GROUP BY " + COLUMN_MONTH;
    private static final String ORDER_BY_DATETIME = " ORDER BY " + COLUMN_DATETIME + " DESC";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "(" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_DATETIME + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, " +
            COLUMN_COST + " REAL DEFAULT " + ZERO + ", " +
            COLUMN_VOLUME + " REAL DEFAULT " + ZERO + ", " +
            COLUMN_TOTAL + " REAL DEFAULT " + ZERO + ", " +
            COLUMN_NEW + " INTEGER DEFAULT " + TRUE + ", " +
            COLUMN_DELETED + " INTEGER DEFAULT " + FALSE +
            ");";
    private static final String DROP_TABLE = "DROP TABLE " + TABLE_NAME;

    private static final String DATABASE_CREATE = CREATE_TABLE;

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
        super(ApplicationFuel.getContext(), DATABASE_NAME, null, DATABASE_VERSION);
        mFilter = new Filter();
    }

    public FuelingDBHelper(@NonNull Filter filter) {
        this();
        setFilter(filter);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = DATABASE_CREATE;

        UtilsLog.d(TAG, "onCreate", "sql == " + sql);

        db.execSQL(sql);
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

    public FuelingRecord getFuelingRecord(long id) {
        FuelingRecord fuelingRecord = null;

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME,
                new String[]{_ID, COLUMN_DATETIME, COLUMN_COST, COLUMN_VOLUME, COLUMN_TOTAL},
                _ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst())
                fuelingRecord = new FuelingRecord(cursor);

            cursor.close();
        }

        db.close();

        return fuelingRecord;
    }

    private long doInsertRecord(@NonNull SQLiteDatabase db, @NonNull FuelingRecord fuelingRecord) {
        ContentValues values = new ContentValues();

        values.put(COLUMN_DATETIME, fuelingRecord.getDateTime());
        values.put(COLUMN_COST, fuelingRecord.getCost());
        values.put(COLUMN_VOLUME, fuelingRecord.getVolume());
        values.put(COLUMN_TOTAL, fuelingRecord.getTotal());

        return db.insert(TABLE_NAME, null, values); // TODO: add throw
    }

    public long insertRecord(@NonNull FuelingRecord fuelingRecord) {
        SQLiteDatabase db = getWritableDatabase();

        long id = doInsertRecord(db, fuelingRecord);

        db.close();

        return id;
    }

    public void swapRecords(@NonNull List<FuelingRecord> fuelingRecordList) {
        SQLiteDatabase db = getWritableDatabase();

        db.execSQL(DROP_TABLE);
        db.execSQL(CREATE_TABLE);

        for (FuelingRecord fuelingRecord : fuelingRecordList)
            doInsertRecord(db, fuelingRecord);

        db.close();
    }

    public int updateRecord(@NonNull FuelingRecord fuelingRecord) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();

        cv.put(COLUMN_DATETIME, fuelingRecord.getDateTime());
        cv.put(COLUMN_COST, fuelingRecord.getCost());
        cv.put(COLUMN_VOLUME, fuelingRecord.getVolume());
        cv.put(COLUMN_TOTAL, fuelingRecord.getTotal());
        cv.put(COLUMN_NEW, TRUE);
        cv.put(COLUMN_DELETED, FALSE);

        int id = db.update(TABLE_NAME, cv, _ID + "=?", new String[]{String.valueOf(fuelingRecord.getId())});

        db.close();

        return id;
    }

    public int deleteRecord(@NonNull FuelingRecord fuelingRecord) {
        SQLiteDatabase db = getWritableDatabase();

        int count = db.delete(TABLE_NAME, _ID + "=?", new String[]{String.valueOf(fuelingRecord.getId())});

        db.close();

        return count;
    }

    @NonNull
    private String filterModeToSql() {
        String result = "";

        if (mFilter.filterMode != FILTER_MODE_ALL) {
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

            result = COLUMN_DATETIME +
                    String.format(IN_DATES, dateFrom.getTimeInMillis(), dateTo.getTimeInMillis());
        }

        if (!result.equals("")) result += " AND ";

        return WHERE + result + RECORD_NOT_MARKED_AS_DELETED;
    }

    public Cursor getAllCursor() {
        String sql = SELECT_ALL + filterModeToSql() + ORDER_BY_DATETIME;

        UtilsLog.d(TAG, "getAllCursor", "sql == " + sql);

        return getReadableDatabase().rawQuery(sql, null);
    }

    public Cursor getYears() {
        String sql = SELECT_YEARS + String.format(SELECT_YEARS_WHERE, UtilsDate.getCurrentYear());

        UtilsLog.d(TAG, "getYears", "sql == " + sql);

        return getReadableDatabase().rawQuery(sql, null);
    }

    public Cursor getSumByMonthsForYear() {
        String sql = SELECT_SUM_BY_MONTHS_IN_YEAR + filterModeToSql() + GROUP_BY_MONTH;

        UtilsLog.d(TAG, "getSumByMonthsForYear", "sql == " + sql);

        return getReadableDatabase().rawQuery(sql, null);
    }

    @NonNull
    public List<FuelingRecord> getAllRecords() {
        String sql = SELECT_ALL + WHERE + RECORD_NOT_MARKED_AS_DELETED + ORDER_BY_DATETIME;

        UtilsLog.d(TAG, "getAllRecords", "sql == " + sql);

        List<FuelingRecord> fuelingRecords = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(sql, null);

        if (cursor.moveToFirst()) do
            fuelingRecords.add(new FuelingRecord(cursor));
        while (cursor.moveToNext());

        cursor.close();

        db.close();

        return fuelingRecords;
    }
}