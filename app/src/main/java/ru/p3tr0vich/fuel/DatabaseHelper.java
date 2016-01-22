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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
// Try-with-resources requires API level 19 (current min is 17)
class DatabaseHelper extends SQLiteOpenHelper implements BaseColumns {

    private static final String TAG = "DatabaseHelper";

    private static final String COLUMN_DATETIME = "datetime";
    private static final String COLUMN_COST = "cost";
    private static final String COLUMN_VOLUME = "volume";
    private static final String COLUMN_TOTAL = "total";
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
    public static final int COLUMN_YEAR_INDEX = 0;

    private static final String[] COLUMNS_SUM_BY_MONTHS = new String[]{
            "SUM(" + COLUMN_COST + ")",
            "strftime('%m', " + COLUMN_DATETIME + "/1000, 'unixepoch', 'localtime')" + AS + COLUMN_MONTH};
    public static final int COLUMN_COST_SUM_INDEX = 0;
    public static final int COLUMN_MONTH_INDEX = 1;

    public static final String EQUAL = "=";
    public static final int TRUE = 1;
    public static final int FALSE = 0;

    public static final String WHERE_RECORD_NOT_DELETED = COLUMN_DELETED + EQUAL + FALSE;
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
        public long dateFrom;
        public long dateTo;
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

    @SuppressWarnings("SameParameterValue")
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

    @NonNull
    public static ContentValues getValues(@Nullable Long id,
                                          long dateTime,
                                          float cost,
                                          float volume,
                                          float total,
                                          boolean changed,
                                          boolean deleted) {
        ContentValues values = new ContentValues();

        if (id != null)
            values.put(_ID, id);

        values.put(COLUMN_DATETIME, dateTime);
        values.put(COLUMN_COST, cost);
        values.put(COLUMN_VOLUME, volume);
        values.put(COLUMN_TOTAL, total);
        values.put(COLUMN_CHANGED, changed ? TRUE : FALSE);
        values.put(COLUMN_DELETED, deleted ? TRUE : FALSE);

        return values;
    }

    @NonNull
    public static ContentValues getValuesMarkAsDeleted() {
        ContentValues values = new ContentValues();

        values.put(COLUMN_CHANGED, TRUE);
        values.put(COLUMN_DELETED, TRUE);

        return values;
    }

    @Nullable
    public static FuelingRecord cursorToFuelingRecord(@Nullable Cursor cursor) {
        FuelingRecord fuelingRecord = null;

        if (cursor != null)
            try {
                if (cursor.moveToFirst())
                    fuelingRecord = new FuelingRecord(cursor);
            } finally {
                cursor.close();
            }

        return fuelingRecord;
    }

    @NonNull
    public static String filterModeToSql(@NonNull Filter filter) {
        if (filter.filterMode == FILTER_MODE_ALL)
            return WHERE_RECORD_NOT_DELETED;
        else {
            Calendar dateFrom = Calendar.getInstance();
            Calendar dateTo = Calendar.getInstance();

            if (filter.filterMode == FILTER_MODE_DATES) {
                dateFrom.setTimeInMillis(filter.dateFrom);
                dateTo.setTimeInMillis(filter.dateTo);
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

    public Cursor getAll(String selection) {
        return query(COLUMNS, selection, null, COLUMN_DATETIME + DESC);
    }

    public Cursor getRecord(long id) {
        return query(COLUMNS, _ID + EQUAL + id, null, null);
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

    public Cursor getSyncRecords(boolean getChanged) {
        UtilsLog.d(TAG, "getSyncRecords", "getChanged == " + getChanged);
        return query(COLUMNS_WITH_DELETED,
                getChanged ? COLUMN_CHANGED + EQUAL + TRUE : null, null, null);
    }

    public int deleteMarkedAsDeleted() {
        return delete(COLUMN_DELETED + EQUAL + TRUE);
    }

    public int updateChanged() {
        ContentValues values = new ContentValues();
        values.put(COLUMN_CHANGED, FALSE);

        return update(values, COLUMN_CHANGED + EQUAL + TRUE);
    }

    public Cursor query(String[] columns, String selection, String groupBy, String orderBy) {
        UtilsLog.d(TAG, "query", "columns == " + Arrays.toString(columns) +
                ", selection == " + selection + ", groupBy == " + groupBy + ", orderBy == " + orderBy);

        return getReadableDatabase().query(TABLE_FUELING, columns, selection,
                null, groupBy, null, orderBy);
    }

    public long insert(@NonNull SQLiteDatabase db, @NonNull ContentValues values) {
        return db.insert(TABLE_FUELING, null, values);
    }

    public long insert(@NonNull ContentValues values) {
        return insert(getWritableDatabase(), values);
    }

    public int update(@NonNull ContentValues values, @NonNull String whereClause) {
        return getWritableDatabase().update(TABLE_FUELING, values, whereClause, null);
    }

    public int update(@NonNull ContentValues values, long id) {
        return getWritableDatabase().update(TABLE_FUELING, values, _ID + EQUAL + id, null);
    }

    private int delete(@NonNull SQLiteDatabase db, @Nullable String whereClause) {
        return db.delete(TABLE_FUELING, whereClause, null);
    }

    public int delete(@Nullable String whereClause) {
        return delete(getWritableDatabase(), whereClause);
    }

    public int delete(long id) {
        return delete(_ID + EQUAL + id);
    }
}