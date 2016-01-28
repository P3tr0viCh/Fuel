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
class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    interface FuelingColumns {
        String DATETIME = "datetime";
        String COST = "cost";
        String VOLUME = "volume";
        String TOTAL = "total";
        String CHANGED = "changed";
        String DELETED = "deleted";

        String YEAR = "year";
        String MONTH = "month";
    }

    public static class Fueling implements BaseColumns, FuelingColumns {

        private Fueling() {
        }

        public static final String NAME = "fueling";

        private static final String[] COLUMNS = new String[]{
                _ID, DATETIME, COST, VOLUME, TOTAL
        };
        public static final int _ID_INDEX = 0;
        public static final int DATETIME_INDEX = 1;
        public static final int COST_INDEX = 2;
        public static final int VOLUME_INDEX = 3;
        public static final int TOTAL_INDEX = 4;

        private static final String[] COLUMNS_WITH_DELETED = new String[]{
                _ID, DATETIME, COST, VOLUME, TOTAL, DELETED
        };
        public static final int DELETED_INDEX = 5;

        private static final String[] COLUMNS_YEARS = new String[]{
                "strftime('%Y', " + DATETIME + "/1000, 'unixepoch', 'localtime')" + AS + YEAR};
        public static final int YEAR_INDEX = 0;

        private static final String[] COLUMNS_SUM_BY_MONTHS = new String[]{
                "SUM(" + COST + ")",
                "strftime('%m', " + DATETIME + "/1000, 'unixepoch', 'localtime')" + AS + MONTH};
        public static final int COST_SUM_INDEX = 0;
        public static final int MONTH_INDEX = 1;

        private static final String CREATE_STATEMENT = "CREATE TABLE " + Fueling.NAME + "(" +
                Fueling._ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE, " +
                Fueling.DATETIME + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, " +
                Fueling.COST + " REAL DEFAULT 0, " +
                Fueling.VOLUME + " REAL DEFAULT 0, " +
                Fueling.TOTAL + " REAL DEFAULT 0, " +
                Fueling.CHANGED + " INTEGER DEFAULT " + TRUE + ", " +
                Fueling.DELETED + " INTEGER DEFAULT " + FALSE +
                ");";
    }

    public static class Database {
        private static final int VERSION = 1;

        private static final String NAME = "fuel.db";
    }

    private static final String AND = " AND ";
    private static final String AS = " AS ";
    private static final String DESC = " DESC";

    private static final String EQUAL = "=";
    private static final int TRUE = 1;
    private static final int FALSE = 0;

    public static class Where {
        public static final String RECORD_NOT_DELETED = Fueling.DELETED + EQUAL + FALSE;
        private static final String LESS = "<'%d'";
        private static final String BETWEEN = " BETWEEN %1$d AND %2$d";
    }

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
        super(context, Database.NAME, null, Database.VERSION);
    }

    @SuppressWarnings("SameParameterValue")
    public static boolean getBoolean(@NonNull Cursor cursor, int columnIndex) {
        return cursor.getInt(columnIndex) == TRUE;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        UtilsLog.d(TAG, "onCreate", "sql == " + Fueling.CREATE_STATEMENT);
        db.execSQL(Fueling.CREATE_STATEMENT);
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
                                          @SuppressWarnings("SameParameterValue")
                                          boolean deleted) {
        ContentValues values = new ContentValues();

        if (id != null)
            values.put(Fueling._ID, id);

        values.put(Fueling.DATETIME, dateTime);
        values.put(Fueling.COST, cost);
        values.put(Fueling.VOLUME, volume);
        values.put(Fueling.TOTAL, total);
        values.put(Fueling.CHANGED, changed ? TRUE : FALSE);
        values.put(Fueling.DELETED, deleted ? TRUE : FALSE);

        return values;
    }

    @NonNull
    public static ContentValues getValuesMarkAsDeleted() {
        ContentValues values = new ContentValues();

        values.put(Fueling.CHANGED, TRUE);
        values.put(Fueling.DELETED, TRUE);

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
            return Where.RECORD_NOT_DELETED;
        else {
            Calendar dateFrom = Calendar.getInstance();
            Calendar dateTo = Calendar.getInstance();

            if (filter.filterMode == FILTER_MODE_DATES) {
                dateFrom.setTimeInMillis(UtilsDate.localToUtc(filter.dateFrom));
                dateTo.setTimeInMillis(UtilsDate.localToUtc(filter.dateTo));
            } else {
                int year = filter.filterMode == FILTER_MODE_YEAR ?
                        filter.year : UtilsDate.getCurrentYear();

                dateFrom.set(year, Calendar.JANUARY, 1);
                dateTo.set(year, Calendar.DECEMBER, 31);
            }

            UtilsDate.setStartOfDay(dateFrom);
            UtilsDate.setEndOfDay(dateTo);

            return Fueling.DATETIME +
                    String.format(Locale.US, Where.BETWEEN,
                            UtilsDate.utcToLocal(dateFrom.getTimeInMillis()),
                            UtilsDate.utcToLocal(dateTo.getTimeInMillis())) +
                    AND + Where.RECORD_NOT_DELETED;
        }
    }

    public Cursor getAll(String selection) {
        return query(Fueling.COLUMNS, selection, null, Fueling.DATETIME + DESC);
    }

    public Cursor getRecord(long id) {
        return query(Fueling.COLUMNS, Fueling._ID + EQUAL + id, null, null);
    }

    public Cursor getYears() {
        UtilsLog.d(TAG, "getYears");
        return query(Fueling.COLUMNS_YEARS, Fueling.YEAR +
                        String.format(Locale.US, Where.LESS, UtilsDate.getCurrentYear()) + AND +
                        Where.RECORD_NOT_DELETED,
                Fueling.YEAR, Fueling.YEAR);
    }

    public Cursor getSumByMonthsForYear(int year) {
        UtilsLog.d(TAG, "getSumByMonthsForYear", "year == " + year);
        return query(Fueling.COLUMNS_SUM_BY_MONTHS, filterModeToSql(new Filter(year)),
                Fueling.MONTH, Fueling.MONTH);
    }

    public Cursor getSyncRecords(boolean getChanged) {
        UtilsLog.d(TAG, "getSyncRecords", "getChanged == " + getChanged);
        return query(Fueling.COLUMNS_WITH_DELETED,
                getChanged ? Fueling.CHANGED + EQUAL + TRUE : null, null, null);
    }

    public int deleteMarkedAsDeleted() {
        return delete(Fueling.DELETED + EQUAL + TRUE);
    }

    public int updateChanged() {
        ContentValues values = new ContentValues();
        values.put(Fueling.CHANGED, FALSE);

        return update(values, Fueling.CHANGED + EQUAL + TRUE);
    }

    private Cursor query(String[] columns, String selection, String groupBy, String orderBy) {
        UtilsLog.d(TAG, "query", "columns == " + Arrays.toString(columns) +
                ", selection == " + selection + ", groupBy == " + groupBy + ", orderBy == " + orderBy);

//        for (int i = 0, waitSeconds = 3; i < waitSeconds; i++) {
//            try {
//                TimeUnit.SECONDS.sleep(1);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            UtilsLog.d(TAG, "query", "wait... " + (waitSeconds - i));
//        }

        return getReadableDatabase().query(Fueling.NAME, columns, selection,
                null, groupBy, null, orderBy);
    }

    public long insert(@NonNull SQLiteDatabase db, @NonNull ContentValues values) {
        return db.insert(Fueling.NAME, null, values);
    }

    public long insert(@NonNull ContentValues values) {
        return insert(getWritableDatabase(), values);
    }

    public int update(@NonNull ContentValues values, @NonNull String whereClause) {
        return getWritableDatabase().update(Fueling.NAME, values, whereClause, null);
    }

    public int update(@NonNull ContentValues values, long id) {
        return getWritableDatabase().update(Fueling.NAME, values, Fueling._ID + EQUAL + id, null);
    }

    private int delete(@NonNull SQLiteDatabase db, @Nullable String whereClause) {
        return db.delete(Fueling.NAME, whereClause, null);
    }

    public int delete(@Nullable String whereClause) {
        return delete(getWritableDatabase(), whereClause);
    }

    public int delete(long id) {
        return delete(Fueling._ID + EQUAL + id);
    }
}