package ru.p3tr0vich.fuel.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ru.p3tr0vich.fuel.Database;
import ru.p3tr0vich.fuel.models.FuelingRecord;
import ru.p3tr0vich.fuel.utils.UtilsDate;
import ru.p3tr0vich.fuel.utils.UtilsLog;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
// Try-with-resources requires API level 19 (current min is 17)
public class DatabaseHelper extends SQLiteOpenHelper implements Database {

    private static final String TAG = "DatabaseHelper";

    private static final boolean LOG_ENABLED = false;

    public static class Filter {

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({MODE_ALL, MODE_CURRENT_YEAR, MODE_YEAR, MODE_DATES, MODE_TWO_LAST_RECORDS})
        public @interface Mode {
        }

        public static final int MODE_ALL = 0;
        public static final int MODE_CURRENT_YEAR = 1;
        public static final int MODE_YEAR = 2;
        public static final int MODE_DATES = 3;
        public static final int MODE_TWO_LAST_RECORDS = 4;

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
            else if (mode == MODE_TWO_LAST_RECORDS) {
                final Calendar calendar = Calendar.getInstance();

                return TableFueling.Columns.DATETIME +
                        String.format(Locale.US, Where.LESS_OR_EQUAL,
                                UtilsDate.utcToLocal(calendar.getTimeInMillis())) +
                        Statement.AND + Where.RECORD_NOT_DELETED;
            } else {
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

                return TableFueling.Columns.DATETIME +
                        String.format(Locale.US, Where.BETWEEN,
                                UtilsDate.utcToLocal(calendarFrom.getTimeInMillis()),
                                UtilsDate.utcToLocal(calendarTo.getTimeInMillis())) +
                        Statement.AND + Where.RECORD_NOT_DELETED;
            }
        }
    }

    public DatabaseHelper(@NonNull Context context) {
        super(context, Database.NAME, null, Database.VERSION);
    }

    @SuppressWarnings("SameParameterValue")
    public static boolean getBoolean(@NonNull Cursor cursor, int columnIndex) {
        return cursor.getInt(columnIndex) == Statement.TRUE;
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
            values.put(TableFueling.Columns._ID, id);

        values.put(TableFueling.Columns.DATETIME,
                convertDate ?
                        UtilsDate.utcToLocal(dateTime) :
                        dateTime);
        values.put(TableFueling.Columns.COST, cost);
        values.put(TableFueling.Columns.VOLUME, volume);
        values.put(TableFueling.Columns.TOTAL, total);
        values.put(TableFueling.Columns.CHANGED, changed ? Statement.TRUE : Statement.FALSE);
        values.put(TableFueling.Columns.DELETED, deleted ? Statement.TRUE : Statement.FALSE);

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

        values.put(TableFueling.Columns.CHANGED, Statement.TRUE);
        values.put(TableFueling.Columns.DELETED, Statement.TRUE);

        return values;
    }

    @NonNull
    private static FuelingRecord getFuelingRecord(@NonNull Cursor cursor, boolean convertDate) {
        final long dateTime = cursor.getLong(TableFueling.Columns.DATETIME_INDEX);
        return new FuelingRecord(
                cursor.getLong(TableFueling.Columns._ID_INDEX),
                convertDate ?
                        UtilsDate.localToUtc(dateTime) :
                        dateTime,
                cursor.getFloat(TableFueling.Columns.COST_INDEX),
                cursor.getFloat(TableFueling.Columns.VOLUME_INDEX),
                cursor.getFloat(TableFueling.Columns.TOTAL_INDEX));
    }

    @NonNull
    public static FuelingRecord getFuelingRecordForSync(@NonNull Cursor cursor) {
        return getFuelingRecord(cursor, false);
    }

    @NonNull
    public static FuelingRecord getFuelingRecord(@NonNull Cursor cursor) {
        return getFuelingRecord(cursor, true);
    }

    @Nullable
    public static List<FuelingRecord> getFuelingRecords(@Nullable Cursor cursor) {
        if (cursor == null) return null;

        List<FuelingRecord> records = new ArrayList<>();

        if (cursor.moveToFirst())
            do
                records.add(getFuelingRecord(cursor));
            while (cursor.moveToNext());

        return records;
    }

    public Cursor getAll(String selection) {
        return query(TableFueling.Columns.COLUMNS, selection, null, TableFueling.Columns.DATETIME + Statement.DESC);
    }

    public Cursor getTwoLastRecords() {
        Filter filter = new Filter();
        filter.mode = Filter.MODE_TWO_LAST_RECORDS;
        return query(TableFueling.Columns.COLUMNS, filter.getSelection(), null,
                TableFueling.Columns.DATETIME + Statement.DESC, "2");
    }

    public Cursor getRecord(long id) {
        return query(TableFueling.Columns.COLUMNS, TableFueling.Columns._ID + Statement.EQUAL + id, null, null);
    }

    public Cursor getYears() {
        if (LOG_ENABLED)
            UtilsLog.d(TAG, "getYears");
        return query(TableFueling.Columns.COLUMNS_YEARS, Where.RECORD_NOT_DELETED,
                TableFueling.Columns.YEAR, TableFueling.Columns.YEAR);
    }

    public Cursor getSumByMonthsForYear(int year) {
        if (LOG_ENABLED)
            UtilsLog.d(TAG, "getSumByMonthsForYear", "year == " + year);
        return query(TableFueling.Columns.COLUMNS_SUM_BY_MONTHS, new Filter(year).getSelection(),
                TableFueling.Columns.MONTH, TableFueling.Columns.MONTH);
    }

    public Cursor getSyncRecords(boolean getChanged) {
        if (LOG_ENABLED)
            UtilsLog.d(TAG, "getSyncRecords", "getChanged == " + getChanged);
        return query(TableFueling.Columns.COLUMNS_WITH_DELETED,
                getChanged ? TableFueling.Columns.CHANGED + Statement.EQUAL + Statement.TRUE : null, null, null);
    }

    public int deleteMarkedAsDeleted() {
        return delete(TableFueling.Columns.DELETED + Statement.EQUAL + Statement.TRUE);
    }

    public int updateChanged() {
        ContentValues values = new ContentValues();
        values.put(TableFueling.Columns.CHANGED, Statement.FALSE);

        return update(values, TableFueling.Columns.CHANGED + Statement.EQUAL + Statement.TRUE);
    }

    private Cursor query(String[] columns, String selection, String groupBy, String orderBy, String limit) {
        if (LOG_ENABLED)
            UtilsLog.d(TAG, "query", "columns == " + Arrays.toString(columns) +
                    ", selection == " + selection + ", groupBy == " + groupBy +
                    ", orderBy == " + orderBy + ", limit == " + limit);

//        for (int i = 0, waitSeconds = 3; i < waitSeconds; i++) {
//            try {
//                TimeUnit.SECONDS.sleep(1);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            UtilsLog.d(TAG, "query", "wait... " + (waitSeconds - i));
//        }

        return getReadableDatabase().query(TableFueling.NAME, columns, selection,
                null, groupBy, null, orderBy, limit);
    }

    private Cursor query(String[] columns, String selection, String groupBy, String orderBy) {
        return query(columns, selection, groupBy, orderBy, null);
    }

    public long insert(@NonNull SQLiteDatabase db, @NonNull ContentValues values) {
        return db.insertOrThrow(TableFueling.NAME, null, values);
    }

    public long insert(@NonNull ContentValues values) {
        return insert(getWritableDatabase(), values);
    }

    private int update(@NonNull SQLiteDatabase db, @NonNull ContentValues values, @NonNull String whereClause) {
        return db.update(TableFueling.NAME, values, whereClause, null);
    }

    public int update(@NonNull ContentValues values, @NonNull String whereClause) {
        return update(getWritableDatabase(), values, whereClause);
    }

    public int update(@NonNull ContentValues values, long id) {
        return update(values, TableFueling.Columns._ID + Statement.EQUAL + id);
    }

    private int delete(@NonNull SQLiteDatabase db, @Nullable String whereClause) {
        return db.delete(TableFueling.NAME, whereClause, null);
    }

    public int delete(@Nullable String whereClause) {
        return delete(getWritableDatabase(), whereClause);
    }

    public int delete(long id) {
        return delete(TableFueling.Columns._ID + Statement.EQUAL + id);
    }
}