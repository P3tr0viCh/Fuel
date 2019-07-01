package ru.p3tr0vich.fuel.sync

import android.content.ContentProviderClient
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.nfc.FormatException
import android.os.RemoteException
import android.text.TextUtils
import ru.p3tr0vich.fuel.helpers.ContentProviderHelper
import ru.p3tr0vich.fuel.helpers.PreferencesHelper
import ru.p3tr0vich.fuel.utils.UtilsString
import java.util.*

internal class SyncProviderPreferences(context: Context, private val provider: ContentProviderClient) {

    private val preferencesHelper = PreferencesHelper.getInstance(context)

    var preferences: List<String>
        @Throws(RemoteException::class, FormatException::class)
        get() {
            val contentValues = query(null)

            val result = ArrayList<String>()

            for (key in contentValues.keySet())
                when (preferencesHelper.getPreferenceType(key)) {
                    PreferencesHelper.PREFERENCE_TYPE_STRING -> result.add(key + SEPARATOR + UtilsString.encodeLineBreaks(contentValues.getAsString(key)))
                    PreferencesHelper.PREFERENCE_TYPE_INT -> result.add(key + SEPARATOR + contentValues.getAsInteger(key).toString())
                    PreferencesHelper.PREFERENCE_TYPE_LONG -> result.add(key + SEPARATOR + contentValues.getAsLong(key).toString())
                }

            return result
        }
        @Throws(RemoteException::class)
        set(preferences) {
            val contentValues = ContentValues()

            var index: Int
            var key: String
            var value: String

            for (preference in preferences) {
                index = preference.indexOf(SEPARATOR)

                if (index == -1) continue

                key = preference.substring(0, index)

                if (key.isEmpty()) continue

                value = preference.substring(index + 1)

                when (preferencesHelper.getPreferenceType(key)) {
                    PreferencesHelper.PREFERENCE_TYPE_STRING -> contentValues.put(key, UtilsString.decodeLineBreaks(value))
                    PreferencesHelper.PREFERENCE_TYPE_INT -> contentValues.put(key, Integer.decode(value))
                    PreferencesHelper.PREFERENCE_TYPE_LONG -> contentValues.put(key, java.lang.Long.decode(value))
                }
            }

            update(contentValues, null)
        }

    var isChanged: Boolean
        @Throws(RemoteException::class, FormatException::class)
        get() = query(preferencesHelper.keys.changed).getAsBoolean(preferencesHelper.keys.changed)!!
        @Throws(RemoteException::class)
        set(value) {
            val contentValues = ContentValues()

            contentValues.put(preferencesHelper.keys.changed, value)

            update(contentValues, preferencesHelper.keys.changed)
        }

    var databaseRevision: Int
        @Throws(RemoteException::class, FormatException::class)
        get() = getRevision(preferencesHelper.keys.databaseRevision)
        @Throws(RemoteException::class)
        set(value) {
            putRevision(preferencesHelper.keys.databaseRevision, value)
        }

    var preferencesRevision: Int
        @Throws(RemoteException::class, FormatException::class)
        get() = getRevision(preferencesHelper.keys.preferencesRevision)
        @Throws(RemoteException::class)
        set(value) {
            putRevision(preferencesHelper.keys.preferencesRevision, value)
        }

    var isDatabaseFullSync: Boolean
        @Throws(RemoteException::class, FormatException::class)
        get() = query(preferencesHelper.keys.databaseFullSync).getAsBoolean(preferencesHelper.keys.databaseFullSync)!!
        @Throws(RemoteException::class)
        set(value) {
            val contentValues = ContentValues()

            contentValues.put(preferencesHelper.keys.databaseFullSync, value)

            update(contentValues, preferencesHelper.keys.databaseFullSync)
        }

    @Throws(RemoteException::class, FormatException::class)
    private fun query(preference: String?): ContentValues {
        val cursor = provider.query(
                if (TextUtils.isEmpty(preference)) {
                    ContentProviderHelper.URI_PREFERENCES
                } else {
                    Uri.withAppendedPath(ContentProviderHelper.URI_PREFERENCES, preference)
                }, null, null, null, null)

        if (cursor == null) {
            throw FormatException("$TAG -- query: cursor == null")
        } else {
            if (cursor.count == 0) {
                throw FormatException("$TAG -- query: cursor.getCount() == 0")
            }
        }

        val result = ContentValues()

        var key: String

        cursor.use {
            if (it.moveToFirst())
                do {
                    key = it.getString(0)

                    when (preferencesHelper.getPreferenceType(key)) {
                        PreferencesHelper.PREFERENCE_TYPE_STRING -> result.put(key, it.getString(1))
                        PreferencesHelper.PREFERENCE_TYPE_INT -> result.put(key, it.getInt(1))
                        PreferencesHelper.PREFERENCE_TYPE_LONG -> result.put(key, it.getLong(1))
                    }
                } while (it.moveToNext())
        }

        return result
    }

    @Throws(RemoteException::class)
    private fun update(contentValues: ContentValues, preference: String?) {
        provider.update(
                if (TextUtils.isEmpty(preference)) {
                    ContentProviderHelper.URI_PREFERENCES
                } else {
                    Uri.withAppendedPath(ContentProviderHelper.URI_PREFERENCES, preference)
                },
                contentValues, null, null)
    }

    @Throws(RemoteException::class, FormatException::class)
    private fun getRevision(keyRevision: String): Int {
        return query(keyRevision).getAsInteger(keyRevision)!!
    }

    @Throws(RemoteException::class)
    private fun putRevision(keyRevision: String, revision: Int) {
        val contentValues = ContentValues()

        contentValues.put(keyRevision, revision)

        update(contentValues, keyRevision)
    }

    @Throws(RemoteException::class)
    fun putLastSync(dateTime: Long, hasError: Boolean) {
        val contentValues = ContentValues()

        contentValues.put(preferencesHelper.keys.lastSyncDateTime, dateTime)
        contentValues.put(preferencesHelper.keys.lastSyncHasError, hasError)

        update(contentValues, preferencesHelper.keys.lastSyncDateTime)
        update(contentValues, preferencesHelper.keys.lastSyncHasError)
    }

    companion object {
        private const val TAG = "SyncProviderPreferences"

        private const val SEPARATOR = "="
    }
}