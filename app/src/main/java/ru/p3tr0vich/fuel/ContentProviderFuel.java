package ru.p3tr0vich.fuel;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
// Try-with-resources requires API level 19 (current min is 17)
public class ContentProviderFuel extends ContentProvider {

    private static final String TAG = "ContentProviderFuel";

    private static final String URI_AUTHORITY = "ru.p3tr0vich.fuel.provider";

    private static final String URI_PATH_DATABASE = "database";
    private static final String URI_PATH_DATABASE_YEARS = "years";
    private static final String URI_PATH_DATABASE_SUM_BY_MONTHS = "sum_by_months";

    private static final String URI_PATH_DATABASE_SYNC = "sync";
    private static final String URI_PATH_DATABASE_SYNC_ALL = "sync_get_all";
    private static final String URI_PATH_DATABASE_SYNC_CHANGED = "sync_get_changed";

    private static final String URI_PATH_PREFERENCES = "preferences";

    public static final Uri URI_DATABASE =
            Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + URI_AUTHORITY + "/" + URI_PATH_DATABASE);
    public static final Uri URI_DATABASE_YEARS =
            Uri.withAppendedPath(URI_DATABASE, URI_PATH_DATABASE_YEARS);
    public static final Uri URI_DATABASE_SUM_BY_MONTHS =
            Uri.withAppendedPath(URI_DATABASE, URI_PATH_DATABASE_SUM_BY_MONTHS);

    public static final Uri URI_DATABASE_SYNC =
            Uri.withAppendedPath(URI_DATABASE, URI_PATH_DATABASE_SYNC);
    public static final Uri URI_DATABASE_SYNC_ALL =
            Uri.withAppendedPath(URI_DATABASE, URI_PATH_DATABASE_SYNC_ALL);
    public static final Uri URI_DATABASE_SYNC_CHANGED =
            Uri.withAppendedPath(URI_DATABASE, URI_PATH_DATABASE_SYNC_CHANGED);

    public static final Uri URI_PREFERENCES =
            Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + URI_AUTHORITY + "/" + URI_PATH_PREFERENCES);

    private static final int DATABASE = 10;
    private static final int DATABASE_ITEM = 11;
    private static final int DATABASE_YEARS = 13;
    private static final int DATABASE_SUM_BY_MONTHS = 14;

    private static final int DATABASE_SYNC = 20;
    private static final int DATABASE_SYNC_ALL = 21;
    private static final int DATABASE_SYNC_CHANGED = 22;

    private static final int PREFERENCES = 30;
    private static final int PREFERENCES_ITEM = 31;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(URI_AUTHORITY,
                URI_DATABASE.getPath(), DATABASE);
        sURIMatcher.addURI(URI_AUTHORITY,
                Uri.withAppendedPath(URI_DATABASE, "#").getPath(), DATABASE_ITEM);
        sURIMatcher.addURI(URI_AUTHORITY,
                URI_DATABASE_YEARS.getPath(), DATABASE_YEARS);
        sURIMatcher.addURI(URI_AUTHORITY,
                Uri.withAppendedPath(URI_DATABASE_SUM_BY_MONTHS, "#").getPath(), DATABASE_SUM_BY_MONTHS);

        sURIMatcher.addURI(URI_AUTHORITY,
                URI_DATABASE_SYNC.getPath(), DATABASE_SYNC);
        sURIMatcher.addURI(URI_AUTHORITY,
                URI_DATABASE_SYNC_ALL.getPath(), DATABASE_SYNC_ALL);
        sURIMatcher.addURI(URI_AUTHORITY,
                URI_DATABASE_SYNC_CHANGED.getPath(), DATABASE_SYNC_CHANGED);

        sURIMatcher.addURI(URI_AUTHORITY,
                URI_PREFERENCES.getPath(), PREFERENCES);
        sURIMatcher.addURI(URI_AUTHORITY,
                Uri.withAppendedPath(URI_PREFERENCES, "*").getPath(), PREFERENCES_ITEM);
    }

    private static final String CURSOR_DIR_BASE_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE +
            "/vnd." + URI_AUTHORITY + ".";
    private static final String CURSOR_ITEM_BASE_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE +
            "/vnd." + URI_AUTHORITY + ".";

    private static final String CURSOR_DIR_BASE_TYPE_DATABASE =
            CURSOR_DIR_BASE_TYPE + URI_PATH_DATABASE;
    private static final String CURSOR_ITEM_BASE_TYPE_DATABASE =
            CURSOR_ITEM_BASE_TYPE + URI_PATH_DATABASE;
    private static final String CURSOR_DIR_BASE_TYPE_PREFERENCES =
            CURSOR_DIR_BASE_TYPE + URI_PATH_PREFERENCES;
    private static final String CURSOR_ITEM_BASE_TYPE_PREFERENCES =
            CURSOR_ITEM_BASE_TYPE + URI_PATH_PREFERENCES;

    private DatabaseHelper mDatabaseHelper;

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case DATABASE:
            case DATABASE_YEARS:
            case DATABASE_SUM_BY_MONTHS:
            case DATABASE_SYNC:
            case DATABASE_SYNC_ALL:
            case DATABASE_SYNC_CHANGED:
                return CURSOR_DIR_BASE_TYPE_DATABASE;
            case DATABASE_ITEM:
                return CURSOR_ITEM_BASE_TYPE_DATABASE;

            case PREFERENCES:
                return CURSOR_DIR_BASE_TYPE_PREFERENCES;
            case PREFERENCES_ITEM:
                return CURSOR_ITEM_BASE_TYPE_PREFERENCES;
            default:
                UtilsLog.d(TAG, "getType", "sURIMatcher.match() == default, uri == " + uri);
                return null;
        }
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
//        UtilsLog.d(TAG, "query, uri == " + uri);
        switch (sURIMatcher.match(uri)) {
            case DATABASE:
                return mDatabaseHelper.getAll(selection);
            case DATABASE_ITEM:
                return mDatabaseHelper.getRecord(ContentUris.parseId(uri));
            case DATABASE_YEARS:
                return mDatabaseHelper.getYears();
            case DATABASE_SUM_BY_MONTHS:
                return mDatabaseHelper.getSumByMonthsForYear((int) ContentUris.parseId(uri));

            case DATABASE_SYNC_ALL:
                return mDatabaseHelper.getSyncRecords(false);
            case DATABASE_SYNC_CHANGED:
                return mDatabaseHelper.getSyncRecords(true);

            case PREFERENCES:
                return PreferenceManagerFuel.getPreferences();
            case PREFERENCES_ITEM:
                return PreferenceManagerFuel.getPreference(uri.getLastPathSegment());
            default:
                UtilsLog.d(TAG, "getType", "sURIMatcher.match() == default, uri == " + uri);
                return null;
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        switch (sURIMatcher.match(uri)) {
            case DATABASE:
                return ContentUris.withAppendedId(URI_DATABASE,
                        mDatabaseHelper.insert(values));
            default:
                UtilsLog.d(TAG, "insert", "sURIMatcher.match() == default, uri == " + uri);
                return null;
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
//        UtilsLog.d(TAG, "update, uri == " + uri);
        switch (sURIMatcher.match(uri)) {
            case DATABASE:
                return mDatabaseHelper.update(values, selection);
            case DATABASE_ITEM:
                return mDatabaseHelper.update(values, ContentUris.parseId(uri));
            case DATABASE_SYNC:
                return mDatabaseHelper.updateChanged();
            case PREFERENCES:
                return PreferenceManagerFuel.setPreferences(values, null);
            case PREFERENCES_ITEM:
                return PreferenceManagerFuel.setPreferences(values, uri.getLastPathSegment());
            default:
                UtilsLog.d(TAG, "update", "sURIMatcher.match() == default, uri == " + uri);
                return -1;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
//        UtilsLog.d(TAG, "delete, uri == " + uri);
        switch (sURIMatcher.match(uri)) {
            case DATABASE:
                return mDatabaseHelper.delete(selection);
            case DATABASE_ITEM:
                return mDatabaseHelper.delete(ContentUris.parseId(uri));
            case DATABASE_SYNC:
                return mDatabaseHelper.deleteMarkedAsDeleted();
            default:
                UtilsLog.d(TAG, "delete", "sURIMatcher.match() == default, uri == " + uri);
                return -1;
        }
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        switch (sURIMatcher.match(uri)) {
            case DATABASE:
                int numValues = values.length;

                SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

                try {
                    db.beginTransaction();
                    try {
                        for (ContentValues value : values)
                            mDatabaseHelper.insert(db, value);

                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                } finally {
                    db.close();
                }

                notifyChange(getContext());

                return numValues;
            default:
                UtilsLog.d(TAG, "bulkInsert", "sURIMatcher.match() == default, uri == " + uri);
                return -1;
        }
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        ContentProviderResult[] results = super.applyBatch(operations);

        notifyChange(getContext());

        return results;
    }

    public static Cursor getAll(@NonNull Context context, @NonNull DatabaseHelper.Filter filter) {
        return context.getContentResolver().query(URI_DATABASE, null,
                DatabaseHelper.filterModeToSql(filter), null, null, null);
    }

    public static Cursor getRecord(@NonNull Context context, long id) {
        return context.getContentResolver().query(ContentUris.withAppendedId(URI_DATABASE, id),
                null, null, null, null, null);
    }

    @Nullable
    public static FuelingRecord getFuelingRecord(@NonNull Context context, long id) {
        return DatabaseHelper.cursorToFuelingRecord(getRecord(context, id));
    }

    @NonNull
    public static List<FuelingRecord> getAllRecordsList(@NonNull Context context) {
        List<FuelingRecord> fuelingRecords = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(URI_DATABASE, null,
                DatabaseHelper.WHERE_RECORD_NOT_DELETED, null, null, null);
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

    public static void swapRecords(@NonNull Context context, @NonNull List<FuelingRecord> fuelingRecordList) {
        UtilsLog.d(TAG, "swapRecords", "records count == " + fuelingRecordList.size());

        context.getContentResolver().delete(URI_DATABASE, null, null); // delete all records

        int size = fuelingRecordList.size();
        if (size == 0) return;

        FuelingRecord fuelingRecord;

        ContentValues[] values = new ContentValues[size];

        for (int i = 0; i < size; i++) {
            fuelingRecord = fuelingRecordList.get(i);

            values[i] = DatabaseHelper.getValues(
                    fuelingRecord.getDateTime(),
                    fuelingRecord.getDateTime(),
                    fuelingRecord.getCost(),
                    fuelingRecord.getVolume(),
                    fuelingRecord.getTotal(),
                    true,
                    false);
        }

        context.getContentResolver().bulkInsert(URI_DATABASE, values);
    }

    private static void notifyChange(@Nullable Context context) {
        if (context != null)
            context.getContentResolver().notifyChange(URI_DATABASE, null, false);
    }

    public static long insertRecord(@NonNull Context context, @NonNull FuelingRecord fuelingRecord) {
        long id = ContentUris.parseId(
                context.getContentResolver().insert(
                        URI_DATABASE,
                        DatabaseHelper.getValues(
                                fuelingRecord.getDateTime(),
                                fuelingRecord.getDateTime(),
                                fuelingRecord.getCost(),
                                fuelingRecord.getVolume(),
                                fuelingRecord.getTotal(),
                                true,
                                false)));

        notifyChange(context);

        return id;
    }

    public static int updateRecord(@NonNull Context context, @NonNull FuelingRecord fuelingRecord) {
        int rowsUpdated = context.getContentResolver().update(
                ContentUris.withAppendedId(URI_DATABASE, fuelingRecord.getId()),
                DatabaseHelper.getValues(
                        null,
                        fuelingRecord.getDateTime(),
                        fuelingRecord.getCost(),
                        fuelingRecord.getVolume(),
                        fuelingRecord.getTotal(),
                        true,
                        false), null, null);

        notifyChange(context);

        return rowsUpdated;
    }

    public static int markRecordAsDeleted(@NonNull Context context, long id) {
        int rowsUpdated = context.getContentResolver().update(
                ContentUris.withAppendedId(URI_DATABASE, id),
                DatabaseHelper.getValuesMarkAsDeleted(), null, null);

        notifyChange(context);

        return rowsUpdated;
    }

    public static Cursor getYears(@NonNull Context context) {
        return context.getContentResolver().query(URI_DATABASE_YEARS, null, null, null, null);
    }

    public static Cursor getSumByMonthsForYear(@NonNull Context context, int year) {
        return context.getContentResolver().query(
                ContentUris.withAppendedId(URI_DATABASE_SUM_BY_MONTHS, year), null, null, null, null);
    }
}