package ru.p3tr0vich.fuel.helpers;

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

import ru.p3tr0vich.fuel.models.FuelingRecord;
import ru.p3tr0vich.fuel.utils.UtilsDate;
import ru.p3tr0vich.fuel.utils.UtilsLog;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
// Try-with-resources requires API level 19 (current min is 17)
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    private static final boolean LOG_ENABLED = false;

    interface TableFuelingColumns {
        String DATETIME = "datetime";
        String COST = "cost";
        String VOLUME = "volume";
        String TOTAL = "total";
        String CHANGED = "changed";
        String DELETED = "deleted";

        String YEAR = "year";
        String MONTH = "month";
    }

    public static class TableFueling implements BaseColumns, TableFuelingColumns {

        private TableFueling() {
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
                "strftime('%Y', " + DATETIME + "/1000, 'unixepoch', 'utc')" + AS + YEAR};
        public static final int YEAR_INDEX = 0;

        private static final String[] COLUMNS_SUM_BY_MONTHS = new String[]{
                "SUM(" + COST + ")",
                "strftime('%m', " + DATETIME + "/1000, 'unixepoch', 'utc')" + AS + MONTH};
        public static final int COST_SUM_INDEX = 0;
        public static final int MONTH_INDEX = 1;

        private static final String CREATE_STATEMENT = "CREATE TABLE " + NAME + "(" +
                _ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE, " +
                DATETIME + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, " +
                COST + " REAL DEFAULT 0, " +
                VOLUME + " REAL DEFAULT 0, " +
                TOTAL + " REAL DEFAULT 0, " +
                CHANGED + " INTEGER DEFAULT " + TRUE + ", " +
                DELETED + " INTEGER DEFAULT " + FALSE +
                ");";
    }

    public static class Database {
        private static final int VERSION = 1;

        private static final String NAME = "fuel.db";

        private static final String CREATE_STATEMENT = TableFueling.CREATE_STATEMENT;

        private Database() {
        }
    }

    private static final String AND = " AND ";
    private static final String AS = " AS ";
    private static final String DESC = " DESC";

    private static final String EQUAL = "=";
    private static final int TRUE = 1;
    private static final int FALSE = 0;

    public static class Where {
        public static final String RECORD_NOT_DELETED = TableFueling.DELETED + EQUAL + FALSE;
        private static final String BETWEEN = " BETWEEN %1$d AND %2$d";

        private Where() {
        }
    }

    public static class Filter {

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({MODE_ALL, MODE_CURRENT_YEAR, MODE_YEAR, MODE_DATES})
        public @interface Mode {
        }

        public static final int MODE_ALL = 0;
        public static final int MODE_CURRENT_YEAR = 1;
        public static final int MODE_YEAR = 2;
        public static final int MODE_DATES = 3;

        public long dateFrom;
        public long dateTo;
        public int year;
        @Mode
        public int mode;

        public Filter() {
            mode = MODE_ALL;
        }

        public Filter(int year) {
            this.year = year;
            mode = MODE_YEAR;
        }

        @NonNull
        public String getSelection() {
            if (mode == MODE_ALL)
                return Where.RECORD_NOT_DELETED;
            else {
                final Calendar calendarFrom = Calendar.getInstance();
                final Calendar calendarTo = Calendar.getInstance();

                if (mode == MODE_DATES) {
                    calendarFrom.setTimeInMillis(dateFrom);
                    calendarTo.setTimeInMillis(dateTo);
                } else {
                    final int year = mode == MODE_YEAR ? this.year : UtilsDate.getCurrentYear();

                    calendarFrom.set(year, Calendar.JANUARY, 1);
                    calendarTo.set(year, Calendar.DECEMBER, 31);
                }

                UtilsDate.setStartOfDay(calendarFrom);
                UtilsDate.setEndOfDay(calendarTo);

                return TableFueling.DATETIME +
                        String.format(Locale.US, Where.BETWEEN,
                                UtilsDate.utcToLocal(calendarFrom.getTimeInMillis()),
                                UtilsDate.utcToLocal(calendarTo.getTimeInMillis())) +
                        AND + Where.RECORD_NOT_DELETED;
            }
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
        if (LOG_ENABLED)
            UtilsLog.d(TAG, "onCreate", "sql == " + Database.CREATE_STATEMENT);
        db.execSQL(Database.CREATE_STATEMENT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (LOG_ENABLED)
            UtilsLog.d(TAG, "onUpgrade");
    }

    @NonNull
    private static ContentValues getValues(@Nullable Long id,
                                           long dateTime,
                                           float cost,
                                           float volume,
                                           float total,
                                           boolean changed,
                                           @SuppressWarnings("SameParameterValue")
                                           boolean deleted,
                                           boolean convertDate) {
        ContentValues values = new ContentValues();

        if (id != null)
            values.put(TableFueling._ID, id);

        values.put(TableFueling.DATETIME,
                convertDate ?
                        UtilsDate.utcToLocal(dateTime) :
                        dateTime);
        values.put(TableFueling.COST, cost);
        values.put(TableFueling.VOLUME, volume);
        values.put(TableFueling.TOTAL, total);
        values.put(TableFueling.CHANGED, changed ? TRUE : FALSE);
        values.put(TableFueling.DELETED, deleted ? TRUE : FALSE);

        return values;
    }

    @NonNull
    public static ContentValues getValuesForSync(@Nullable Long id,
                                                 long dateTime,
                                                 float cost,
                                                 float volume,
                                                 float total) {
        return getValues(id, dateTime, cost, volume, total, false, false, false);
    }

    @NonNull
    public static ContentValues getValues(@Nullable Long id,
                                          long dateTime,
                                          float cost,
                                          float volume,
                                          float total) {
        return getValues(id, dateTime, cost, volume, total, true, false, true);
    }

    @NonNull
    public static ContentValues getValuesMarkAsDeleted() {
        ContentValues values = new ContentValues();

        values.put(TableFueling.CHANGED, TRUE);
        values.put(TableFueling.DELETED, TRUE);

        return values;
    }

    @NonNull
    private static FuelingRecord getFuelingRecord(@NonNull Cursor cursor, boolean convertDate) {
        final long dateTime = cursor.getLong(TableFueling.DATETIME_INDEX);
        return new FuelingRecord(
                cursor.getLong(TableFueling._ID_INDEX),
                convertDate ?
                        UtilsDate.localToUtc(dateTime) :
                        dateTime,
                cursor.getFloat(TableFueling.COST_INDEX),
                cursor.getFloat(TableFueling.VOLUME_INDEX),
                cursor.getFloat(TableFueling.TOTAL_INDEX));
    }

    @NonNull
    public static FuelingRecord getFuelingRecordForSync(@NonNull Cursor cursor) {
        return getFuelingRecord(cursor, false);
    }

    @NonNull
    public static FuelingRecord getFuelingRecord(@NonNull Cursor cursor) {
        return getFuelingRecord(cursor, true);
    }

    public Cursor getAll(String selection) {
        return query(TableFueling.COLUMNS, selection, null, TableFueling.DATETIME + DESC);
    }

    public Cursor getRecord(long id) {
        return query(TableFueling.COLUMNS, TableFueling._ID + EQUAL + id, null, null);
    }

    public Cursor getYears() {
        if (LOG_ENABLED)
            UtilsLog.d(TAG, "getYears");
        return query(TableFueling.COLUMNS_YEARS, Where.RECORD_NOT_DELETED,
                TableFueling.YEAR, TableFueling.YEAR);
    }

    public Cursor getSumByMonthsForYear(int year) {
        if (LOG_ENABLED)
            UtilsLog.d(TAG, "getSumByMonthsForYear", "year == " + year);
        return query(TableFueling.COLUMNS_SUM_BY_MONTHS, new Filter(year).getSelection(),
                TableFueling.MONTH, TableFueling.MONTH);
    }

    public Cursor getSyncRecords(boolean getChanged) {
        if (LOG_ENABLED)
            UtilsLog.d(TAG, "getSyncRecords", "getChanged == " + getChanged);
        return query(TableFueling.COLUMNS_WITH_DELETED,
                getChanged ? TableFueling.CHANGED + EQUAL + TRUE : null, null, null);
    }

    public int deleteMarkedAsDeleted() {
        return delete(TableFueling.DELETED + EQUAL + TRUE);
    }

    public int updateChanged() {
        ContentValues values = new ContentValues();
        values.put(TableFueling.CHANGED, FALSE);

        return update(values, TableFueling.CHANGED + EQUAL + TRUE);
    }

    private Cursor query(String[] columns, String selection, String groupBy, String orderBy) {
        if (LOG_ENABLED)
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

        return getReadableDatabase().query(TableFueling.NAME, columns, selection,
                null, groupBy, null, orderBy);
    }

    public long insert(@NonNull SQLiteDatabase db, @NonNull ContentValues values) {
        return db.insert(TableFueling.NAME, null, values);
    }

    public long insert(@NonNull ContentValues values) {
        return insert(getWritableDatabase(), values);
    }

    public int update(@NonNull ContentValues values, @NonNull String whereClause) {
        return getWritableDatabase().update(TableFueling.NAME, values, whereClause, null);
    }

    public int update(@NonNull ContentValues values, long id) {
        return getWritableDatabase().update(TableFueling.NAME, values, TableFueling._ID + EQUAL + id, null);
    }

    private int delete(@NonNull SQLiteDatabase db, @Nullable String whereClause) {
        return db.delete(TableFueling.NAME, whereClause, null);
    }

    public int delete(@Nullable String whereClause) {
        return delete(getWritableDatabase(), whereClause);
    }

    public int delete(long id) {
        return delete(TableFueling._ID + EQUAL + id);
    }
}