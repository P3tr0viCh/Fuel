package ru.p3tr0vich.fuel.helpers

import android.content.*
import android.database.Cursor
import android.net.Uri
import ru.p3tr0vich.fuel.BuildConfig
import ru.p3tr0vich.fuel.utils.UtilsLog

class ContentProviderHelper : ContentProvider() {
    companion object {
        private const val TAG = "ContentProviderHelper"

        val URI_DATABASE = BaseUri.getUri(UriPath.DATABASE)
        val URI_DATABASE_YEARS = BaseUri.getUri(UriPath.DATABASE_YEARS)
        val URI_DATABASE_SUM_BY_MONTHS = BaseUri.getUri(UriPath.DATABASE_SUM_BY_MONTHS)
        val URI_DATABASE_TWO_LAST_RECORDS = BaseUri.getUri(UriPath.DATABASE_TWO_LAST_RECORDS)

        val URI_DATABASE_SYNC = BaseUri.getUri(UriPath.DATABASE_SYNC)
        val URI_DATABASE_SYNC_ALL = BaseUri.getUri(UriPath.DATABASE_SYNC_ALL)
        val URI_DATABASE_SYNC_CHANGED = BaseUri.getUri(UriPath.DATABASE_SYNC_CHANGED)

        val URI_PREFERENCES = BaseUri.getUri(UriPath.PREFERENCES)

        const val DATABASE = 10
        const val DATABASE_ITEM = 11
        private const val DATABASE_YEARS = 13
        private const val DATABASE_SUM_BY_MONTHS = 14
        private const val DATABASE_TWO_LAST_RECORDS = 15

        const val DATABASE_SYNC = 20
        private const val DATABASE_SYNC_ALL = 21
        private const val DATABASE_SYNC_CHANGED = 22

        private const val PREFERENCES = 30
        private const val PREFERENCES_ITEM = 31

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        init {
            uriMatcher.addURI(BaseUri.AUTHORITY, UriPath.DATABASE, DATABASE)
            uriMatcher.addURI(BaseUri.AUTHORITY, UriPath.DATABASE_ITEM, DATABASE_ITEM)
            uriMatcher.addURI(BaseUri.AUTHORITY, UriPath.DATABASE_YEARS, DATABASE_YEARS)
            uriMatcher.addURI(BaseUri.AUTHORITY, UriPath.DATABASE_SUM_BY_MONTHS_ITEM, DATABASE_SUM_BY_MONTHS)
            uriMatcher.addURI(BaseUri.AUTHORITY, UriPath.DATABASE_TWO_LAST_RECORDS, DATABASE_TWO_LAST_RECORDS)

            uriMatcher.addURI(BaseUri.AUTHORITY, UriPath.DATABASE_SYNC, DATABASE_SYNC)
            uriMatcher.addURI(BaseUri.AUTHORITY, UriPath.DATABASE_SYNC_ALL, DATABASE_SYNC_ALL)
            uriMatcher.addURI(BaseUri.AUTHORITY, UriPath.DATABASE_SYNC_CHANGED, DATABASE_SYNC_CHANGED)

            uriMatcher.addURI(BaseUri.AUTHORITY, UriPath.PREFERENCES, PREFERENCES)
            uriMatcher.addURI(BaseUri.AUTHORITY, UriPath.PREFERENCES_ITEM, PREFERENCES_ITEM)
        }

        private const val CURSOR_DIR_BASE_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE +
                "/vnd." + BaseUri.AUTHORITY + "."
        private const val CURSOR_ITEM_BASE_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE +
                "/vnd." + BaseUri.AUTHORITY + "."

        private const val CURSOR_DIR_BASE_TYPE_DATABASE = CURSOR_DIR_BASE_TYPE + UriPath.DATABASE
        private const val CURSOR_ITEM_BASE_TYPE_DATABASE = CURSOR_ITEM_BASE_TYPE + UriPath.DATABASE
        private const val CURSOR_DIR_BASE_TYPE_PREFERENCES = CURSOR_DIR_BASE_TYPE + UriPath.PREFERENCES
        private const val CURSOR_ITEM_BASE_TYPE_PREFERENCES = CURSOR_ITEM_BASE_TYPE + UriPath.PREFERENCES

        fun uriMatch(uri: Uri): Int {
            return uriMatcher.match(uri)
        }
    }

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var preferencesHelper: PreferencesHelper

    private object BaseUri {
        const val SCHEME = ContentResolver.SCHEME_CONTENT
        const val AUTHORITY = BuildConfig.APPLICATION_ID + ".provider"

        fun getUri(path: String): Uri {
            return Uri.Builder()
                    .scheme(SCHEME)
                    .authority(AUTHORITY)
                    .path(path)
                    .build()
        }
    }

    private object UriPath {
        const val DATABASE = "database"
        const val DATABASE_ITEM = "$DATABASE/#"
        const val DATABASE_YEARS = "$DATABASE/years"
        const val DATABASE_SUM_BY_MONTHS = "$DATABASE/sum_by_months"
        const val DATABASE_SUM_BY_MONTHS_ITEM = "$DATABASE_SUM_BY_MONTHS/#"
        const val DATABASE_TWO_LAST_RECORDS = "$DATABASE/two_last_records"

        const val DATABASE_SYNC = "$DATABASE/sync"
        const val DATABASE_SYNC_ALL = "$DATABASE/sync_get_all"
        const val DATABASE_SYNC_CHANGED = "$DATABASE/sync_get_changed"

        const val PREFERENCES = "preferences"
        const val PREFERENCES_ITEM = "$PREFERENCES/*"
    }

    override fun onCreate(): Boolean {
        assert(context != null)

        databaseHelper = DatabaseHelper(context!!)
        preferencesHelper = PreferencesHelper.getInstance(context!!)

        return true
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            DATABASE, DATABASE_YEARS,
            DATABASE_SUM_BY_MONTHS,
            DATABASE_TWO_LAST_RECORDS,
            DATABASE_SYNC,
            DATABASE_SYNC_ALL,
            DATABASE_SYNC_CHANGED -> CURSOR_DIR_BASE_TYPE_DATABASE

            DATABASE_ITEM -> CURSOR_ITEM_BASE_TYPE_DATABASE

            PREFERENCES -> CURSOR_DIR_BASE_TYPE_PREFERENCES

            PREFERENCES_ITEM -> CURSOR_ITEM_BASE_TYPE_PREFERENCES
            else -> {
                UtilsLog.d(TAG, "getType", "uriMatcher.match() == default, uri == $uri")
                null
            }
        }
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        return try {
            when (uriMatcher.match(uri)) {
                DATABASE -> databaseHelper.getAll(selection!!)
                DATABASE_ITEM -> databaseHelper.getRecord(ContentUris.parseId(uri))
                DATABASE_YEARS -> databaseHelper.years
                DATABASE_SUM_BY_MONTHS -> databaseHelper.getSumByMonthsForYear(ContentUris.parseId(uri).toInt())
                DATABASE_TWO_LAST_RECORDS -> databaseHelper.twoLastRecords

                DATABASE_SYNC_ALL -> databaseHelper.getSyncRecords(false)
                DATABASE_SYNC_CHANGED -> databaseHelper.getSyncRecords(true)

                PREFERENCES -> preferencesHelper.getPreferencesCursor()
                PREFERENCES_ITEM -> preferencesHelper.getPreferencesCursor(uri.lastPathSegment)
                else -> {
                    UtilsLog.d(TAG, "query", "uriMatcher.match() == default, uri == $uri")
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UtilsLog.d(TAG, "query", "exception == $e")
            null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return try {
            when (uriMatcher.match(uri)) {
                DATABASE -> ContentUris.withAppendedId(URI_DATABASE, databaseHelper.insert(values!!))
                else -> {
                    UtilsLog.d(TAG, "insert", "uriMatcher.match() == default, uri == $uri")
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UtilsLog.d(TAG, "insert", "exception == $e")
            null
        }
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return try {
            when (uriMatcher.match(uri)) {
                DATABASE -> databaseHelper.update(values!!, selection!!)
                DATABASE_ITEM -> databaseHelper.update(values!!, ContentUris.parseId(uri))
                DATABASE_SYNC -> databaseHelper.updateChanged()
                PREFERENCES -> preferencesHelper.setPreferences(values, null)
                PREFERENCES_ITEM -> preferencesHelper.setPreferences(values, uri.lastPathSegment)
                else -> {
                    UtilsLog.d(TAG, "update", "uriMatcher.match() == default, uri == $uri")
                    -1
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UtilsLog.d(TAG, "update", "exception == $e")
            -1
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return try {
            when (uriMatcher.match(uri)) {
                DATABASE -> databaseHelper.delete(selection)
                DATABASE_ITEM -> databaseHelper.delete(ContentUris.parseId(uri))
                DATABASE_SYNC -> databaseHelper.deleteMarkedAsDeleted()
                else -> {
                    UtilsLog.d(TAG, "delete", "uriMatcher.match() == default, uri == $uri")
                    -1
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UtilsLog.d(TAG, "delete", "exception == $e")
            -1
        }
    }

    override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
        return try {
            when (uriMatcher.match(uri)) {
                DATABASE -> {
                    val numValues = values.size

                    val db = databaseHelper.writableDatabase

                    db.use {
                        it.beginTransaction()
                        try {
                            for (value in values) {
                                databaseHelper.insert(it, value)
                            }

                            it.setTransactionSuccessful()
                        } finally {
                            it.endTransaction()
                        }
                    }

                    numValues
                }
                else -> {
                    UtilsLog.d(TAG, "bulkInsert", "uriMatcher.match() == default, uri == $uri")
                    -1
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UtilsLog.d(TAG, "bulkInsert", "exception == $e")
            -1
        }
    }
}