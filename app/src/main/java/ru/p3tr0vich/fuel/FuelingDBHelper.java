package ru.p3tr0vich.fuel;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
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
    private static final String WHERE = " WHERE ";
    private static final String ORDER_BY_DATE = " ORDER BY " + COLUMN_DATETIME + " DESC, " + COLUMN_TOTAL + " DESC";

    private static final String IN_CURRENT_YEAR = " BETWEEN '%1$d-01-01' AND '%1$d-12-31'";

    private static final String DATABASE_CREATE =
            "CREATE TABLE " + TABLE_NAME + "(" +
                    _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_DATETIME + " TEXT, " +
                    COLUMN_COST + " REAL, " +
                    COLUMN_VOLUME + " REAL, " +
                    COLUMN_TOTAL + " REAL" +
                    ");";
    private static final String DROP_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    private Const.FilterMode mFilterMode;

    public FuelingDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mFilterMode = Const.FilterMode.ALL;
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

    public void setFilterMode(Const.FilterMode filterMode) {
        this.mFilterMode = filterMode;
    }

// --Commented out by Inspection START (17.05.2015 03:02):
//    public Const.FilterMode getFilterMode() {
//        return mFilterMode;
//    }
// --Commented out by Inspection STOP (17.05.2015 03:02)

    public FuelingRecord getFuelingRecord(long id) {
        FuelingRecord fuelingRecord = null;

        SQLiteDatabase db = this.getReadableDatabase();

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
        SQLiteDatabase db = this.getWritableDatabase();

        long id = doInsertRecord(db, fuelingRecord);

        db.close();

        return id;
    }

    public void insertRecords(List<FuelingRecord> fuelingRecordList) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.execSQL(DROP_TABLE);
        db.execSQL(DATABASE_CREATE);

        for (FuelingRecord fuelingRecord : fuelingRecordList)
            doInsertRecord(db, fuelingRecord);

        db.close();
    }

    public int updateRecord(FuelingRecord fuelingRecord) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues cv = new ContentValues();

        cv.put(COLUMN_DATETIME, fuelingRecord.getSQLiteDate());
        cv.put(COLUMN_COST, fuelingRecord.getCost());
        cv.put(COLUMN_VOLUME, fuelingRecord.getVolume());
        cv.put(COLUMN_TOTAL, fuelingRecord.getTotal());

        int id = db.update(TABLE_NAME, cv, _ID + "=?", new String[]{ String.valueOf(fuelingRecord.getId()) });

        db.close();

        return id;
    }

    public int deleteRecord(FuelingRecord fuelingRecord) {
        SQLiteDatabase db = this.getWritableDatabase();

        int id = db.delete(TABLE_NAME, _ID + "=?", new String[]{String.valueOf(fuelingRecord.getId())});

        db.close();

        return id;
    }

    private String filterModeToSql() {
        switch (mFilterMode) {
            case CURRENT_YEAR:
                return WHERE + COLUMN_DATETIME + String.format(IN_CURRENT_YEAR, Functions.getCurrentYear());
            default:
                return "";
        }
    }

    public Cursor getAllCursor() {
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = SELECT_ALL + filterModeToSql() + ORDER_BY_DATE;

        Log.d("XXX", "FuelingDBHelper -- getAllCursor (sql == " + sql + ")");

        return db.rawQuery(sql, null);
    }

    public List<FuelingRecord> getAllRecords() {
        List<FuelingRecord> fuelingRecords = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(SELECT_ALL + ORDER_BY_DATE, null);

        if (cursor.moveToFirst()) do {
            fuelingRecords.add(new FuelingRecord(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getFloat(2),
                    cursor.getFloat(3),
                    cursor.getFloat(4)));
        } while (cursor.moveToNext());

        cursor.close();

        db.close();

        return fuelingRecords;
    }
}