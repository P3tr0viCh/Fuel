package ru.p3tr0vich.fuel.helpers;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ContactsHelper {

    private ContactsHelper() {
    }

    @NonNull
    public static Intent getIntent() {
        return new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                .setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
    }

    @Nullable
    public static String getPhoneNumber(@NonNull Context context, @NonNull Intent data) {
        Uri uriData = data.getData();

        if (uriData == null) {
            return null;
        }

        Cursor cursor = context.getContentResolver().query(uriData,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);

        if (cursor != null)
            try {
                if (cursor.moveToFirst())
                    return cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            } finally {
                cursor.close();
            }

        return null;
    }
}