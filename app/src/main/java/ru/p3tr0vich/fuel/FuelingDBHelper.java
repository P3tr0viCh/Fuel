package ru.p3tr0vich.fuel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class FuelingDBHelper extends SQLiteOpenHelper {

    public static final String _ID = "_id";
    public static final String COLUMN_DATETIME = "datetime";
    public static final String COLUMN_COST = "cost";
    public static final String COLUMN_VOLUME = "volume";
    public static final String COLUMN_TOTAL = "total";

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "fuel.db";
    private static final String TABLE_NAME = "fuelling";

    private static final String SELECT_ALL = "SELECT * FROM " + TABLE_NAME;
    private static final String SELECT_YEARS = "SELECT strftime('%Y', datetime) AS YEAR FROM " + TABLE_NAME +
            " GROUP BY YEAR ORDER BY YEAR ASC";
    private static final String SELECT_SUM_BY_MONTHS_IN_YEAR =
            "SELECT SUM(cost), strftime('%m', datetime) AS MONTH FROM " + TABLE_NAME;

    private static final String WHERE = " WHERE ";
    private static final String IN_YEAR = " BETWEEN '%1$d-01-01' AND '%1$d-12-31'";
    private static final String IN_DATES = " BETWEEN '%1$s' AND '%2$s'";

    private static final String GROUP_BY_MONTH = " GROUP BY MONTH";
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

    enum FilterMode {CURRENT_YEAR, YEAR, DATES, ALL}

    static class Filter {
        public Date dateFrom;
        public Date dateTo;
        public int year;
        public FilterMode filterMode;
    }

    private final Filter mFilter;

    public FuelingDBHelper() {
        super(Functions.sApplicationContext, DATABASE_NAME, null, DATABASE_VERSION);
        mFilter = new Filter();
        mFilter.filterMode = FilterMode.ALL;
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
            cursor.moveToFirst();

            fuelingRecord = new FuelingRecord(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getFloat(2),
                    cursor.getFloat(3),
                    cursor.getFloat(4)
            );

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

        int id = db.delete(TABLE_NAME, _ID + "=?", new String[]{String.valueOf(fuelingRecord.getId())});

        db.close();

        return id;
    }

    private String filterModeToSql() {
        switch (mFilter.filterMode) {
            case YEAR:
            case CURRENT_YEAR:
                return WHERE + COLUMN_DATETIME + String.format(IN_YEAR,
                        mFilter.filterMode == FilterMode.YEAR ? mFilter.year : Functions.getCurrentYear());
            case DATES:
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
        return getReadableDatabase().rawQuery(SELECT_YEARS, null);
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
            fuelingRecords.add(new FuelingRecord(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getFloat(2),
                    cursor.getFloat(3),
                    cursor.getFloat(4)));
        while (cursor.moveToNext());

        cursor.close();

        db.close();

        return fuelingRecords;
    }
}