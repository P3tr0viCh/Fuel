package ru.p3tr0vich.fuel;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class SyncProvider extends ContentProvider {

    private static final String URI_AUTHORITY = "ru.p3tr0vich.fuel.provider";

    private static final String URI_PATH_DATABASE = "database";
    private static final String URI_PATH_PREFERENCES = "preferences";

    public static final Uri URI_DATABASE =
            Uri.parse("content://" + URI_AUTHORITY + "/" + URI_PATH_DATABASE);
    public static final Uri URI_PREFERENCES =
            Uri.parse("content://" + URI_AUTHORITY + "/" + URI_PATH_PREFERENCES);

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if (!uri.getAuthority().equals(URI_AUTHORITY)) return null;

        String path = uri.getPath();

        if (path.contains(URI_PATH_PREFERENCES)) {
            MatrixCursor matrixCursor = new MatrixCursor(new String[]{"key", "value"});

            ContentValues preferences = PreferenceManagerFuel.getPreferences(selection);
            for (String key : preferences.keySet())
                matrixCursor.addRow(new Object[]{key, preferences.get(key)});

            return matrixCursor;
        }

        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return -1;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if (!uri.getAuthority().equals(URI_AUTHORITY)) return -1;

        String path = uri.getPath();

        if (path.contains(URI_PATH_PREFERENCES)) {
            PreferenceManagerFuel.setPreferences(values, selection);

            return 0;
        }

        return -1;
    }
}
