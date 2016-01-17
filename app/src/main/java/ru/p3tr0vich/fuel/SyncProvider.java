package ru.p3tr0vich.fuel;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class SyncProvider extends ContentProvider {

    private static final String URI_AUTHORITY = "ru.p3tr0vich.fuel.provider";

    private static final String URI_PATH_DATABASE = "database";
    private static final String URI_PATH_PREFERENCES = "preferences";

    private static final String URI_PATH_DATABASE_SYNC = "sync";

    public static final Uri URI_DATABASE =
            Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + URI_AUTHORITY + "/" + URI_PATH_DATABASE);
    public static final Uri URI_DATABASE_SYNC =
            Uri.withAppendedPath(URI_DATABASE, URI_PATH_DATABASE);
    public static final Uri URI_PREFERENCES =
            Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + URI_AUTHORITY + "/" + URI_PATH_PREFERENCES);

    private static final int DATABASE = 10;
    private static final int DATABASE_ITEM = 11;
    private static final int DATABASE_SYNC = 12;
    private static final int DATABASE_SYNC_ITEM = 13;

    private static final int PREFERENCES = 20;
    private static final int PREFERENCES_ITEM = 21;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(URI_AUTHORITY, URI_PATH_DATABASE, DATABASE);
        sURIMatcher.addURI(URI_AUTHORITY, URI_PATH_DATABASE + "/#", DATABASE_ITEM);
        sURIMatcher.addURI(URI_AUTHORITY, URI_PATH_DATABASE + "/" + URI_PATH_DATABASE_SYNC,
                DATABASE_SYNC);
        sURIMatcher.addURI(URI_AUTHORITY, URI_PATH_DATABASE + "/" + URI_PATH_DATABASE_SYNC + "/#",
                DATABASE_SYNC_ITEM);

        sURIMatcher.addURI(URI_AUTHORITY, URI_PATH_PREFERENCES, PREFERENCES);
        sURIMatcher.addURI(URI_AUTHORITY, URI_PATH_PREFERENCES + "/*", PREFERENCES_ITEM);
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
            case DATABASE_SYNC:
                return CURSOR_DIR_BASE_TYPE_DATABASE;
            case DATABASE_ITEM:
            case DATABASE_SYNC_ITEM:
                return CURSOR_ITEM_BASE_TYPE_DATABASE;

            case PREFERENCES:
                return CURSOR_DIR_BASE_TYPE_PREFERENCES;
            case PREFERENCES_ITEM:
                return CURSOR_ITEM_BASE_TYPE_PREFERENCES;
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        switch (sURIMatcher.match(uri)) {
            case DATABASE:
                return mDatabaseHelper.getSyncAllRecords();
            case DATABASE_SYNC:
                return mDatabaseHelper.getSyncChangedRecords();
            case PREFERENCES:
                return PreferenceManagerFuel.getPreferences();
            case PREFERENCES_ITEM:
                return PreferenceManagerFuel.getPreference(uri.getLastPathSegment());
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        switch (sURIMatcher.match(uri)) {
            case DATABASE:
                long id = mDatabaseHelper.insert(values);
                return ContentUris.withAppendedId(URI_DATABASE, id);
            default:
                return null;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        switch (sURIMatcher.match(uri)) {
            case DATABASE:
                return mDatabaseHelper.delete(selection);
            case DATABASE_ITEM:
                return mDatabaseHelper.delete(
                        DatabaseHelper._ID + DatabaseHelper.EQUAL + ContentUris.parseId(uri));
            case DATABASE_SYNC:
                return mDatabaseHelper.delete(
                        DatabaseHelper.COLUMN_DELETED + DatabaseHelper.EQUAL + DatabaseHelper.TRUE);
            case DATABASE_SYNC_ITEM:
                return mDatabaseHelper.delete(
                        DatabaseHelper._ID + DatabaseHelper.EQUAL + ContentUris.parseId(uri));
            default:
                return -1;
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (sURIMatcher.match(uri)) {
            case DATABASE_SYNC:
                if (values == null) values = new ContentValues();
                else values.clear();

                values.put(DatabaseHelper.COLUMN_CHANGED, DatabaseHelper.FALSE);

                return mDatabaseHelper.update(values,
                        DatabaseHelper.COLUMN_CHANGED + DatabaseHelper.EQUAL + DatabaseHelper.TRUE);
            case PREFERENCES:
                return PreferenceManagerFuel.setPreferences(values, null);
            case PREFERENCES_ITEM:
                return PreferenceManagerFuel.setPreferences(values, uri.getLastPathSegment());
            default:
                return -1;
        }
    }
}
