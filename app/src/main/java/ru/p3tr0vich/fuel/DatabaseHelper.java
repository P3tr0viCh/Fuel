package ru.p3tr0vich.fuel;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
// Try-with-resources requires API level 19 (current min is 17)
class DatabaseHelper extends SQLiteOpenHelper implements BaseColumns {

    private static final String TAG = "DatabaseHelper";

    public static final String COLUMN_DATETIME = "datetime";
    public static final String COLUMN_COST = "cost";
    public static final String COLUMN_VOLUME = "volume";
    public static final String COLUMN_TOTAL = "total";
    public static final String COLUMN_CHANGED = "changed";
    public static final String COLUMN_DELETED = "deleted";

    private static final String COLUMN_YEAR = "year";
    private static final String COLUMN_MONTH = "month";

    private static final int DATABASE_VERSION = 1;

    private static final String FUEL_DB = "fuel.db";

    private static final String TABLE_FUELING = "fueling";

    private static final String AND = " AND ";
    private static final String AS = " AS ";
    private static final String DESC = " DESC";

    private static final String[] COLUMNS = new String[]{
            _ID, COLUMN_DATETIME, COLUMN_COST, COLUMN_VOLUME, COLUMN_TOTAL
    };
    public static final int COLUMN_ID_INDEX = 0;
    public static final int COLUMN_DATETIME_INDEX = 1;
    public static final int COLUMN_COST_INDEX = 2;
    public static final int COLUMN_VOLUME_INDEX = 3;
    public static final int COLUMN_TOTAL_INDEX = 4;

    private static final String[] COLUMNS_WITH_DELETED = new String[]{
            _ID, COLUMN_DATETIME, COLUMN_COST, COLUMN_VOLUME, COLUMN_TOTAL,
            COLUMN_DELETED
    };
    public static final int COLUMN_DELETED_INDEX = 5;

    private static final String[] COLUMNS_YEARS = new String[]{
            "strftime('%Y', " + COLUMN_DATETIME + "/1000, 'unixepoch', 'localtime')" + AS + COLUMN_YEAR};
    private static final String[] COLUMNS_SUM_BY_MONTHS = new String[]{
            "SUM(" + COLUMN_COST + ")",
            "strftime('%m', " + COLUMN_DATETIME + "/1000, 'unixepoch', 'localtime')" + AS + COLUMN_MONTH};

    public static final String EQUAL = "=";
    public static final int TRUE = 1;
    public static final int FALSE = 0;

    private static final String WHERE_RECORD_NOT_DELETED = COLUMN_DELETED + EQUAL + FALSE;
    private static final String WHERE_LESS = "<'%d'";
    private static final String WHERE_BETWEEN = " BETWEEN %1$d AND %2$d";

    private static final String CREATE_TABLE_FUELING = "CREATE TABLE " + TABLE_FUELING + "(" +
            _ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE, " +
            COLUMN_DATETIME + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, " +
            COLUMN_COST + " REAL DEFAULT 0, " +
            COLUMN_VOLUME + " REAL DEFAULT 0, " +
            COLUMN_TOTAL + " REAL DEFAULT 0, " +
            COLUMN_CHANGED + " INTEGER DEFAULT " + TRUE + ", " +
            COLUMN_DELETED + " INTEGER DEFAULT " + FALSE +
            ");";

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
        public int filterMode;

        Filter() {
            filterMode = FILTER_MODE_ALL;
        }

        Filter(int year) {
            this.year = year;
            filterMode = FILTER_MODE_YEAR;
        }
    }

    public DatabaseHelper(Context context) {
        super(context, FUEL_DB, null, DATABASE_VERSION);
    }

    public static boolean getBoolean(@NonNull Cursor cursor, int columnIndex) {
        return cursor.getInt(columnIndex) == TRUE;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        UtilsLog.d(TAG, "onCreate", "sql == " + CREATE_TABLE_FUELING);
        db.execSQL(CREATE_TABLE_FUELING);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        UtilsLog.d(TAG, "onUpgrade");
    }

    @Nullable
    public FuelingRecord getFuelingRecord(long id) {
        UtilsLog.d(TAG, "getFuelingRecord");

        FuelingRecord fuelingRecord = null;

        Cursor cursor = query(COLUMNS, _ID + EQUAL + String.valueOf(id), null, null);

        if (cursor != null)
            try {
                if (cursor.moveToFirst())
                    fuelingRecord = new FuelingRecord(cursor);
            } finally {
                cursor.close();
            }

        return fuelingRecord;
    }

    private long doInsertRecord(@NonNull SQLiteDatabase db, @NonNull FuelingRecord fuelingRecord) {
        ContentValues values = new ContentValues();

        values.put(_ID, fuelingRecord.getDateTime());
        values.put(COLUMN_DATETIME, fuelingRecord.getDateTime());
        values.put(COLUMN_COST, fuelingRecord.getCost());
        values.put(COLUMN_VOLUME, fuelingRecord.getVolume());
        values.put(COLUMN_TOTAL, fuelingRecord.getTotal());
        values.put(COLUMN_CHANGED, TRUE);
        values.put(COLUMN_DELETED, FALSE);

        return insert(db, values); // TODO: add throw
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
        UtilsLog.d(TAG, "swapRecords", "records count == " + fuelingRecordList.size());

        SQLiteDatabase db = getWritableDatabase();

        try {
            db.beginTransaction();

            delete(db, null); // delete all records

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
        ContentValues values = new ContentValues();

        values.put(COLUMN_DATETIME, fuelingRecord.getDateTime());
        values.put(COLUMN_COST, fuelingRecord.getCost());
        values.put(COLUMN_VOLUME, fuelingRecord.getVolume());
        values.put(COLUMN_TOTAL, fuelingRecord.getTotal());
        values.put(COLUMN_CHANGED, TRUE);
        values.put(COLUMN_DELETED, FALSE);

        return update(values, _ID + EQUAL + fuelingRecord.getId());
    }

    public int deleteRecord(@NonNull FuelingRecord fuelingRecord) {
        ContentValues values = new ContentValues();

        values.put(COLUMN_CHANGED, TRUE);
        values.put(COLUMN_DELETED, TRUE);

        return update(values, _ID + EQUAL + fuelingRecord.getId());
    }

    @NonNull
    private String filterModeToSql(Filter filter) {
        if (filter.filterMode == FILTER_MODE_ALL)
            return WHERE_RECORD_NOT_DELETED;
        else {
            Calendar dateFrom = Calendar.getInstance();
            Calendar dateTo = Calendar.getInstance();

            if (filter.filterMode == FILTER_MODE_DATES) {
                dateFrom.setTimeInMillis(filter.dateFrom.getTime());
                dateTo.setTimeInMillis(filter.dateTo.getTime());
            } else {
                int year = filter.filterMode == FILTER_MODE_YEAR ?
                        filter.year : UtilsDate.getCurrentYear();

                dateFrom.set(year, Calendar.JANUARY, 1);
                dateTo.set(year, Calendar.DECEMBER, 31);
            }

            UtilsDate.setStartOfDay(dateFrom);
            UtilsDate.setEndOfDay(dateTo);

            return COLUMN_DATETIME +
                    String.format(Locale.US, WHERE_BETWEEN,
                            dateFrom.getTimeInMillis(), dateTo.getTimeInMillis()) +
                    AND + WHERE_RECORD_NOT_DELETED;
        }
    }

    public Cursor getAllCursor(Filter filter) {
        UtilsLog.d(TAG, "getAllCursor");
        return query(COLUMNS, filterModeToSql(filter), null, COLUMN_DATETIME + DESC);
    }

    public Cursor getYears() {
        UtilsLog.d(TAG, "getYears");
        return query(COLUMNS_YEARS, COLUMN_YEAR +
                        String.format(Locale.US, WHERE_LESS, UtilsDate.getCurrentYear()) + AND +
                        WHERE_RECORD_NOT_DELETED,
                COLUMN_YEAR, COLUMN_YEAR);
    }

    public Cursor getSumByMonthsForYear(int year) {
        UtilsLog.d(TAG, "getSumByMonthsForYear", "year == " + year);
        return query(COLUMNS_SUM_BY_MONTHS, filterModeToSql(new Filter(year)), COLUMN_MONTH, COLUMN_MONTH);
    }

    public Cursor getSyncChangedRecords() {
        UtilsLog.d(TAG, "getSyncChangedRecords");
        return query(COLUMNS_WITH_DELETED, COLUMN_CHANGED + EQUAL + TRUE, null, null);
    }

    public Cursor getSyncAllRecords() {
        UtilsLog.d(TAG, "getSyncAllRecords");
        return query(COLUMNS_WITH_DELETED, null, null, null);
    }

    @NonNull
    public List<FuelingRecord> getAllRecordsList() {
        List<FuelingRecord> fuelingRecords = new ArrayList<>();

        Cursor cursor = query(COLUMNS, WHERE_RECORD_NOT_DELETED, null, COLUMN_DATETIME);
        if (cursor != null)
            try {
                if (cursor.moveToFirst()) do
                    fuelingRecords.add(new FuelingRecord(cursor));
                while (cursor.moveToNext());
            } finally {
                cursor.close();
            }

        return fuelingRecords;
    }

    private Cursor query(String[] columns, String selection, String groupBy, String orderBy) {
        UtilsLog.d(TAG, "query", "columns == " + Arrays.toString(columns) +
                ", selection == " + selection + ", groupBy == " + groupBy + ", orderBy == " + orderBy);

        return getReadableDatabase().query(TABLE_FUELING, columns, selection,
                null, groupBy, null, orderBy);
    }

    private long insert(@NonNull SQLiteDatabase db, @NonNull ContentValues values) {
        return db.insert(TABLE_FUELING, null, values);
    }

    public long insert(@NonNull ContentValues values) {
        return insert(getWritableDatabase(), values);
    }

    public int update(@NonNull ContentValues values, @NonNull String whereClause) {
        return getWritableDatabase().update(TABLE_FUELING, values, whereClause, null);
    }

    private int delete(@NonNull SQLiteDatabase db, @Nullable String whereClause) {
        return db.delete(TABLE_FUELING, whereClause, null);
    }

    public int delete(@Nullable String whereClause) {
        return delete(getWritableDatabase(), whereClause);
    }
}