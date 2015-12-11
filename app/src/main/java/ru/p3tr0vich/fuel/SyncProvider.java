package ru.p3tr0vich.fuel;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.HashMap;

public class SyncProvider extends ContentProvider {

    public static final Uri URI = Uri.parse("content://ru.p3tr0vich.fuel.provider/preferences");

    @Override
    public boolean onCreate() {
        Functions.sApplicationContext = getContext();
        Functions.logD("SyncProvider -- onCreate");

        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"key", "value"});

        if (TextUtils.isEmpty(selection)) {
            HashMap<String, Object> preferences = FuelingPreferenceManager.getPreferences();

            for (String key : preferences.keySet())
                matrixCursor.addRow(new String[]{key, String.valueOf(preferences.get(key))});
        } else if (selection.equals(FuelingPreferenceManager.PREF_CHANGED)) {
            matrixCursor.addRow(new String[]{FuelingPreferenceManager.PREF_CHANGED,
                    String.valueOf(FuelingPreferenceManager.isChanged())});
        } else if (selection.equals(FuelingPreferenceManager.PREF_REVISION)) {
            matrixCursor.addRow(new String[]{FuelingPreferenceManager.PREF_REVISION,
                    String.valueOf(FuelingPreferenceManager.getRevision())});
        }

        return matrixCursor;
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
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if (values == null || values.size() == 0) return 0;

        Boolean isChanged = values.getAsBoolean(FuelingPreferenceManager.PREF_CHANGED);
        if (isChanged != null) {
            FuelingPreferenceManager.putChanged(isChanged);
            return 1;
        }

        return 0;
    }
}
