package ru.p3tr0vich.fuel.helpers;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import ru.p3tr0vich.fuel.models.FuelingRecord;

import static ru.p3tr0vich.fuel.helpers.ContentProviderHelper.URI_DATABASE;
import static ru.p3tr0vich.fuel.helpers.ContentProviderHelper.URI_DATABASE_SUM_BY_MONTHS;
import static ru.p3tr0vich.fuel.helpers.ContentProviderHelper.URI_DATABASE_TWO_LAST_RECORDS;
import static ru.p3tr0vich.fuel.helpers.ContentProviderHelper.URI_DATABASE_YEARS;

public class ContentResolverHelper {
    private ContentResolverHelper() {
    }

    public static Cursor getAll(@NonNull Context context, @NonNull DatabaseHelper.Filter filter) {
        return context.getContentResolver().query(URI_DATABASE, null,
                filter.getSelection(), null, null, null);
    }

    public static Cursor getYears(@NonNull Context context) {
        return context.getContentResolver().query(URI_DATABASE_YEARS,
                null, null, null, null);
    }

    public static Cursor getSumByMonthsForYear(@NonNull Context context, int year) {
        return context.getContentResolver().query(
                ContentUris.withAppendedId(URI_DATABASE_SUM_BY_MONTHS, year),
                null, null, null, null);
    }

    public static Cursor getTwoLastRecords(@NonNull Context context) {
        return context.getContentResolver().query(URI_DATABASE_TWO_LAST_RECORDS,
                null, null, null, null, null);
    }

    public static void swapRecords(@NonNull Context context, @NonNull List<FuelingRecord> fuelingRecordList) {
        context.getContentResolver().delete(URI_DATABASE, null, null);

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

        context.getContentResolver().notifyChange(URI_DATABASE, null, false);
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

    public static boolean insertRecord(@NonNull Context context, @NonNull FuelingRecord fuelingRecord) {
        Uri result = context.getContentResolver().insert(
                URI_DATABASE,
                DatabaseHelper.getValues(
                        fuelingRecord.getDateTime(),
                        fuelingRecord.getDateTime(),
                        fuelingRecord.getCost(),
                        fuelingRecord.getVolume(),
                        fuelingRecord.getTotal()));

        if (result == null) return false;

        context.getContentResolver().notifyChange(result, null, false);

        return true;
    }

    public static boolean updateRecord(@NonNull Context context, @NonNull FuelingRecord fuelingRecord) {
        Uri uri = ContentUris.withAppendedId(URI_DATABASE, fuelingRecord.getId());

        int result = context.getContentResolver().update(uri,
                DatabaseHelper.getValues(
                        null,
                        fuelingRecord.getDateTime(),
                        fuelingRecord.getCost(),
                        fuelingRecord.getVolume(),
                        fuelingRecord.getTotal()), null, null);

        if (result == -1) return false;

        context.getContentResolver().notifyChange(uri, null, false);

        return true;
    }

    public static boolean markRecordAsDeleted(@NonNull Context context, long id) {
        Uri uri = ContentUris.withAppendedId(URI_DATABASE, id);

        int result = context.getContentResolver().update(uri,
                DatabaseHelper.getValuesMarkAsDeleted(), null, null);

        if (result == -1) return false;

        context.getContentResolver().notifyChange(uri, null, false);

        return true;
    }
}