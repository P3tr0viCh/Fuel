package ru.p3tr0vich.fuel.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract

object ContactsHelper {

    @JvmStatic
    val intent: Intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            .setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE)

    @JvmStatic
    fun getPhoneNumber(context: Context, data: Intent?): String? {
        val uriData = data?.data ?: return null

        @SuppressLint("Recycle")
        val cursor = context.contentResolver.query(uriData,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)
                ?: return null

        cursor.use {
            return when {
                it.moveToFirst() -> it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                else -> null
            }
        }
    }
}