package ru.p3tr0vich.fuel;

import android.content.ContentProvider;
import android.content.ContentResolver;
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

    private static final String URI_PATH_INSERT = "insert";

    public static final Uri URI_DATABASE =
            Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + URI_AUTHORITY + "/" + URI_PATH_DATABASE);
    public static final Uri URI_DATABASE_INSERT =
            Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + URI_AUTHORITY + "/" + URI_PATH_DATABASE +
                    "/" + URI_PATH_INSERT);
    public static final Uri URI_PREFERENCES =
            Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + URI_AUTHORITY + "/" + URI_PATH_PREFERENCES);

    public static final String DATABASE_GET_ALL_RECORDS = "DATABASE_GET_ALL_RECORDS";
    public static final String DATABASE_GET_CHANGED_RECORDS = "DATABASE_GET_CHANGED_RECORDS";

    public static final String DATABASE_CLEAR_SYNC_RECORDS = "DATABASE_CLEAR_SYNC_RECORDS";

    public static final String DATABASE_DELETE_RECORD = "DATABASE_DELETE_RECORD";

    private static final int DATABASE = 1;
    private static final int PREFERENCES = 2;
    private static final int PREFERENCES_ITEM = 3;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(URI_AUTHORITY, URI_PATH_DATABASE, DATABASE);
        sURIMatcher.addURI(URI_AUTHORITY, URI_PATH_PREFERENCES, PREFERENCES);
        sURIMatcher.addURI(URI_AUTHORITY, URI_PATH_PREFERENCES + "/*", PREFERENCES_ITEM);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final String cursorDirBase = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
                URI_AUTHORITY + ".";
        final String cursorItemBase = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" +
                URI_AUTHORITY + ".";

        switch (sURIMatcher.match(uri)) {
            case DATABASE:
                return cursorDirBase + URI_PATH_DATABASE;
            case PREFERENCES:
                return cursorDirBase + URI_PATH_PREFERENCES;
            case PREFERENCES_ITEM:
                return cursorItemBase + URI_PATH_PREFERENCES;
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        switch (sURIMatcher.match(uri)) {
            case PREFERENCES:
                return PreferenceManagerFuel.getAllPreferences();
            case PREFERENCES_ITEM:
                return PreferenceManagerFuel.getSinglePreference(uri.getLastPathSegment());
        }

        if (!URI_AUTHORITY.equals(uri.getAuthority())) return null;

        String path = uri.getPath();

        if (path == null) return null;

        if (path.contains(URI_PATH_DATABASE)) {
            FuelingDBHelper dbHelper = new FuelingDBHelper();

            if (DATABASE_GET_ALL_RECORDS.equals(selection))
                return dbHelper.getSyncAllRecords();

            if (DATABASE_GET_CHANGED_RECORDS.equals(selection))
                return dbHelper.getSyncChangedRecords();

            return null;
        }

//        if (path.contains(URI_PATH_PREFERENCES)) {
//            MatrixCursor matrixCursor = new MatrixCursor(new String[]{"key", "value"});
//
//            ContentValues preferences = PreferenceManagerFuel.getPreferences(selection);
//            for (String key : preferences.keySet())
//                matrixCursor.addRow(new Object[]{key, preferences.get(key)});
//
//            return matrixCursor;
//        }

        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        if (!URI_AUTHORITY.equals(uri.getAuthority())) return null;

        String path = uri.getPath();

        if (path == null) return null;

        if (path.contains(URI_PATH_DATABASE)) {
            FuelingDBHelper dbHelper = new FuelingDBHelper();

            if (path.contains(URI_PATH_INSERT)) {
                dbHelper.insertBySyncId(values);

                return null;
            }
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        if (!URI_AUTHORITY.equals(uri.getAuthority())) return -1;

        String path = uri.getPath();

        if (path == null) return -1;

        if (path.contains(URI_PATH_DATABASE)) {
            FuelingDBHelper dbHelper = new FuelingDBHelper();

            if (DATABASE_DELETE_RECORD.equals(selection)) {
                dbHelper.deleteBySyncId(Integer.valueOf(selectionArgs[0]));
                return 0;
            }
        }

        return -1;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        switch (sURIMatcher.match(uri)) {
            case PREFERENCES:
                return PreferenceManagerFuel.setPreferences(values, null);
            case PREFERENCES_ITEM:
                return PreferenceManagerFuel.setPreferences(values, uri.getLastPathSegment());
        }

        if (!URI_AUTHORITY.equals(uri.getAuthority())) return -1;

        String path = uri.getPath();

        if (path == null) return -1;

        if (path.contains(URI_PATH_DATABASE)) {
            FuelingDBHelper dbHelper = new FuelingDBHelper();

            if (DATABASE_CLEAR_SYNC_RECORDS.equals(selection)) {
                dbHelper.clearSyncRecords();
                return 0;
            }

            return -1;
        }

//        if (path.contains(URI_PATH_PREFERENCES)) {
//            PreferenceManagerFuel.setPreferences(values, selection);
//
//            return 0;
//        }

        return -1;
    }
}
