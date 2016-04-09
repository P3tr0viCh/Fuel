package ru.p3tr0vich.fuel;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
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

    private static class BaseUri {

        private static final String SCHEME = ContentResolver.SCHEME_CONTENT;
        private static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";

        private BaseUri() {
        }

        public static Uri getUri(String path) {
            return new Uri.Builder()
                    .scheme(SCHEME)
                    .authority(AUTHORITY)
                    .path(path)
                    .build();
        }
    }

    private static class UriPath {

        private static final String DATABASE = "database";
        private static final String DATABASE_ITEM = DATABASE + "/#";
        private static final String DATABASE_YEARS = DATABASE + "/years";
        private static final String DATABASE_SUM_BY_MONTHS = DATABASE + "/sum_by_months";
        private static final String DATABASE_SUM_BY_MONTHS_ITEM = DATABASE_SUM_BY_MONTHS + "/#";

        private static final String DATABASE_SYNC = DATABASE + "/sync";
        private static final String DATABASE_SYNC_ALL = DATABASE + "/sync_get_all";
        private static final String DATABASE_SYNC_CHANGED = DATABASE + "/sync_get_changed";

        private static final String PREFERENCES = "preferences";
        private static final String PREFERENCES_ITEM = PREFERENCES + "/*";
    }

    public static final Uri URI_DATABASE = BaseUri.getUri(UriPath.DATABASE);
    private static final Uri URI_DATABASE_YEARS = BaseUri.getUri(UriPath.DATABASE_YEARS);
    private static final Uri URI_DATABASE_SUM_BY_MONTHS = BaseUri.getUri(UriPath.DATABASE_SUM_BY_MONTHS);

    public static final Uri URI_DATABASE_SYNC = BaseUri.getUri(UriPath.DATABASE_SYNC);
    public static final Uri URI_DATABASE_SYNC_ALL = BaseUri.getUri(UriPath.DATABASE_SYNC_ALL);
    public static final Uri URI_DATABASE_SYNC_CHANGED = BaseUri.getUri(UriPath.DATABASE_SYNC_CHANGED);

    public static final Uri URI_PREFERENCES = BaseUri.getUri(UriPath.PREFERENCES);

    public static final int DATABASE = 10;
    public static final int DATABASE_ITEM = 11;
    private static final int DATABASE_YEARS = 13;
    private static final int DATABASE_SUM_BY_MONTHS = 14;

    public static final int DATABASE_SYNC = 20;
    private static final int DATABASE_SYNC_ALL = 21;
    private static final int DATABASE_SYNC_CHANGED = 22;

    private static final int PREFERENCES = 30;
    private static final int PREFERENCES_ITEM = 31;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(BaseUri.AUTHORITY, UriPath.DATABASE, DATABASE);
        sURIMatcher.addURI(BaseUri.AUTHORITY, UriPath.DATABASE_ITEM, DATABASE_ITEM);
        sURIMatcher.addURI(BaseUri.AUTHORITY, UriPath.DATABASE_YEARS, DATABASE_YEARS);
        sURIMatcher.addURI(BaseUri.AUTHORITY, UriPath.DATABASE_SUM_BY_MONTHS_ITEM, DATABASE_SUM_BY_MONTHS);

        sURIMatcher.addURI(BaseUri.AUTHORITY, UriPath.DATABASE_SYNC, DATABASE_SYNC);
        sURIMatcher.addURI(BaseUri.AUTHORITY, UriPath.DATABASE_SYNC_ALL, DATABASE_SYNC_ALL);
        sURIMatcher.addURI(BaseUri.AUTHORITY, UriPath.DATABASE_SYNC_CHANGED, DATABASE_SYNC_CHANGED);

        sURIMatcher.addURI(BaseUri.AUTHORITY, UriPath.PREFERENCES, PREFERENCES);
        sURIMatcher.addURI(BaseUri.AUTHORITY, UriPath.PREFERENCES_ITEM, PREFERENCES_ITEM);
    }

    private static final String CURSOR_DIR_BASE_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE +
            "/vnd." + BaseUri.AUTHORITY + ".";
    private static final String CURSOR_ITEM_BASE_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE +
            "/vnd." + BaseUri.AUTHORITY + ".";

    private static final String CURSOR_DIR_BASE_TYPE_DATABASE =
            CURSOR_DIR_BASE_TYPE + UriPath.DATABASE;
    private static final String CURSOR_ITEM_BASE_TYPE_DATABASE =
            CURSOR_ITEM_BASE_TYPE + UriPath.DATABASE;
    private static final String CURSOR_DIR_BASE_TYPE_PREFERENCES =
            CURSOR_DIR_BASE_TYPE + UriPath.PREFERENCES;
    private static final String CURSOR_ITEM_BASE_TYPE_PREFERENCES =
            CURSOR_ITEM_BASE_TYPE + UriPath.PREFERENCES;

    private DatabaseHelper mDatabaseHelper;

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    public static int uriMatch(@NonNull Uri uri) {
        return sURIMatcher.match(uri);
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
                return PreferencesHelper.getPreferences();
            case PREFERENCES_ITEM:
                return PreferencesHelper.getPreference(uri.getLastPathSegment());
            default:
                UtilsLog.d(TAG, "query", "sURIMatcher.match() == default, uri == " + uri);
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
                return PreferencesHelper.setPreferences(values, null);
            case PREFERENCES_ITEM:
                return PreferencesHelper.setPreferences(values, uri.getLastPathSegment());
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

                return numValues;
            default:
                UtilsLog.d(TAG, "bulkInsert", "sURIMatcher.match() == default, uri == " + uri);
                return -1;
        }
    }

    public static Cursor getAll(@NonNull Context context, @NonNull DatabaseHelper.Filter filter) {
        return context.getContentResolver().query(URI_DATABASE, null,
                filter.getSelection(), null, null, null);
    }

    @Nullable
    public static FuelingRecord getFuelingRecord(@NonNull Context context, long id) {
        final Cursor cursor = context.getContentResolver().query(
                ContentUris.withAppendedId(URI_DATABASE, id), null, null, null, null, null);

        if (cursor != null)
            try {
                if (cursor.moveToFirst())
                    return DatabaseHelper.getFuelingRecord(cursor);
            } finally {
                cursor.close();
            }

        return null;
    }

    @NonNull
    public static List<FuelingRecord> getAllRecordsList(@NonNull Context context) {
        List<FuelingRecord> fuelingRecords = new ArrayList<>();

        final Cursor cursor = context.getContentResolver().query(URI_DATABASE, null,
                DatabaseHelper.Where.RECORD_NOT_DELETED, null, null, null);

        if (cursor != null)
            try {
                if (cursor.moveToFirst()) do
                    fuelingRecords.add(DatabaseHelper.getFuelingRecord(cursor));
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
                    fuelingRecord.getTotal());
        }

        context.getContentResolver().bulkInsert(URI_DATABASE, values);

        notifyChangeAfterUser(context, -1);
    }

    private static void notifyChange(@Nullable Context context, @NonNull Uri uri) {
        if (context != null)
            context.getContentResolver().notifyChange(uri, null, false);
    }

    private static void notifyChangeAfterUser(@Nullable Context context, long id) {
        notifyChange(context, id == -1 ?
                URI_DATABASE :
                ContentUris.withAppendedId(URI_DATABASE, id));
    }

    public static void notifyChangeAfterSync(@Nullable Context context) {
        notifyChange(context, URI_DATABASE_SYNC);
    }

    public static long insertRecord(@NonNull Context context, @NonNull FuelingRecord fuelingRecord) {
        final Uri result = context.getContentResolver().insert(
                URI_DATABASE,
                DatabaseHelper.getValues(
                        fuelingRecord.getDateTime(),
                        fuelingRecord.getDateTime(),
                        fuelingRecord.getCost(),
                        fuelingRecord.getVolume(),
                        fuelingRecord.getTotal()));

        long id;

        try {
            id = ContentUris.parseId(result);
        } catch (Exception e) {
            id = -1;
        }

        notifyChangeAfterUser(context, id);

        return id;
    }

    public static int updateRecord(@NonNull Context context, @NonNull FuelingRecord fuelingRecord) {
        final long id = fuelingRecord.getId();

        final int rowsUpdated = context.getContentResolver().update(
                ContentUris.withAppendedId(URI_DATABASE, id),
                DatabaseHelper.getValues(
                        null,
                        fuelingRecord.getDateTime(),
                        fuelingRecord.getCost(),
                        fuelingRecord.getVolume(),
                        fuelingRecord.getTotal()), null, null);

        notifyChangeAfterUser(context, id);

        return rowsUpdated;
    }

    public static int markRecordAsDeleted(@NonNull Context context, long id) {
        final int rowsUpdated = context.getContentResolver().update(
                ContentUris.withAppendedId(URI_DATABASE, id),
                DatabaseHelper.getValuesMarkAsDeleted(), null, null);

        notifyChangeAfterUser(context, -1);

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