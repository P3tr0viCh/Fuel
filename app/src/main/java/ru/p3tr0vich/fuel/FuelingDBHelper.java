package ru.p3tr0vich.fuel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class FuelingDBHelper extends SQLiteOpenHelper {

    private static final String _ID = "_id";
    private static final String COLUMN_DATETIME = "datetime";
    private static final String COLUMN_COST = "cost";
    private static final String COLUMN_VOLUME = "volume";
    private static final String COLUMN_TOTAL = "total";

    public static final int COLUMN_ID_INDEX = 0;
    public static final int COLUMN_DATETIME_INDEX = 1;
    public static final int COLUMN_COST_INDEX = 2;
    public static final int COLUMN_VOLUME_INDEX = 3;
    public static final int COLUMN_TOTAL_INDEX = 4;

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "fuel.db";
    private static final String TABLE_NAME = "fuelling";

    private static final String SELECT_ALL = "SELECT * FROM " + TABLE_NAME;
    private static final String SELECT_YEARS = "SELECT strftime('%Y', datetime) AS year FROM " + TABLE_NAME;
    private static final String SELECT_YEARS_WHERE = " WHERE year<'%d' GROUP BY year ORDER BY year ASC";

    private static final String SELECT_SUM_BY_MONTHS_IN_YEAR =
            "SELECT SUM(cost), strftime('%m', datetime) AS month FROM " + TABLE_NAME;

    private static final String WHERE = " WHERE ";
    private static final String IN_YEAR = " BETWEEN '%1$d-01-01' AND '%1$d-12-31'";
    private static final String IN_DATES = " BETWEEN '%1$s' AND '%2$s'";

    private static final String GROUP_BY_MONTH = " GROUP BY month";
    private static final String ORDER_BY_DATE = " ORDER BY " + COLUMN_DATETIME + " DESC, " + COLUMN_TOTAL + " DESC";

    private static final String DATABASE_CREATE =
            "CREATE TABLE " + TABLE_NAME + "(" +
                    _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_DATETIME + " TEXT, " +
                    COLUMN_COST + " REAL, " +
                    COLUMN_VOLUME + " REAL, " +
                    COLUMN_TOTAL + " REAL" +
                    ");";
    private static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FILTER_MODE_CURRENT_YEAR, FILTER_MODE_YEAR, FILTER_MODE_DATES, FILTER_MODE_ALL})
    public @interface FilterMode {
    }

    public static final int FILTER_MODE_CURRENT_YEAR = 0;
    public static final int FILTER_MODE_YEAR = 1;
    public static final int FILTER_MODE_DATES = 2;
    public static final int FILTER_MODE_ALL = 3;

    static class Filter {
        public Date dateFrom;
        public Date dateTo;
        public int year;
        @FilterMode
        public int filterMode;
    }

    private final Filter mFilter;

    public FuelingDBHelper() {
        super(Functions.sApplicationContext, DATABASE_NAME, null, DATABASE_VERSION);
        mFilter = new Filter();
        mFilter.filterMode = FILTER_MODE_ALL;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE);
        db.execSQL(DATABASE_CREATE);
    }

    public void setFilter(Filter filter) {
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

    private long doInsertRecord(SQLiteDatabase db, FuelingRecord fuelingRecord) {
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_DATETIME, fuelingRecord.getSQLiteDate());
        cv.put(COLUMN_COST, fuelingRecord.getCost());
        cv.put(COLUMN_VOLUME, fuelingRecord.getVolume());
        cv.put(COLUMN_TOTAL, fuelingRecord.getTotal());

        return db.insert(TABLE_NAME, null, cv); // TODO: add throw
    }

    public long insertRecord(FuelingRecord fuelingRecord) {
        SQLiteDatabase db = getWritableDatabase();

        long id = doInsertRecord(db, fuelingRecord);

        db.close();

        return id;
    }

    public void insertRecords(List<FuelingRecord> fuelingRecordList) {
        SQLiteDatabase db = getWritableDatabase();

        db.execSQL(DROP_TABLE);
        db.execSQL(DATABASE_CREATE);

        for (FuelingRecord fuelingRecord : fuelingRecordList)
            doInsertRecord(db, fuelingRecord);

        db.close();
    }

    public int updateRecord(FuelingRecord fuelingRecord) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();

        cv.put(COLUMN_DATETIME, fuelingRecord.getSQLiteDate());
        cv.put(COLUMN_COST, fuelingRecord.getCost());
        cv.put(COLUMN_VOLUME, fuelingRecord.getVolume());
        cv.put(COLUMN_TOTAL, fuelingRecord.getTotal());

        int id = db.update(TABLE_NAME, cv, _ID + "=?", new String[]{String.valueOf(fuelingRecord.getId())});

        db.close();

        return id;
    }

    public int deleteRecord(FuelingRecord fuelingRecord) {
        SQLiteDatabase db = getWritableDatabase();

        int count = db.delete(TABLE_NAME, _ID + "=?", new String[]{String.valueOf(fuelingRecord.getId())});

        db.close();

        return count;
    }

    private String filterModeToSql() {
        switch (mFilter.filterMode) {
            case FILTER_MODE_YEAR:
            case FILTER_MODE_CURRENT_YEAR:
                return WHERE + COLUMN_DATETIME + String.format(IN_YEAR,
                        mFilter.filterMode == FILTER_MODE_YEAR ? mFilter.year : Functions.getCurrentYear());
            case FILTER_MODE_DATES:
                return WHERE + COLUMN_DATETIME + String.format(IN_DATES,
                        Functions.dateToSQLite(mFilter.dateFrom), Functions.dateToSQLite(mFilter.dateTo));
            default:
                return "";
        }
    }

    public Cursor getAllCursor() {
        String sql = SELECT_ALL + filterModeToSql() + ORDER_BY_DATE;

        Functions.logD("FuelingDBHelper -- getAllCursor (sql == " + sql + ")");

        return getReadableDatabase().rawQuery(sql, null);
    }

    public Cursor getYears() {
        String sql = SELECT_YEARS + String.format(SELECT_YEARS_WHERE, Functions.getCurrentYear());

        Functions.logD("FuelingDBHelper -- getYears (sql == " + sql + ")");

        return getReadableDatabase().rawQuery(sql, null);
    }

    public Cursor getSumByMonthsForYear() {
        String sql = SELECT_SUM_BY_MONTHS_IN_YEAR + filterModeToSql() + GROUP_BY_MONTH;

        Functions.logD("FuelingDBHelper -- getSumByMonthsForYear (sql == " + sql + ")");

        return getReadableDatabase().rawQuery(sql, null);
    }

    public List<FuelingRecord> getAllRecords() {
        List<FuelingRecord> fuelingRecords = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(SELECT_ALL + ORDER_BY_DATE, null);

        if (cursor.moveToFirst()) do
            fuelingRecords.add(new FuelingRecord(cursor));
        while (cursor.moveToNext());

        cursor.close();

        db.close();

        return fuelingRecords;
    }
}