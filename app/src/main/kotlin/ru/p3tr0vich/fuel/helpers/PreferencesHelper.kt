package ru.p3tr0vich.fuel.helpers

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.MatrixCursor
import android.preference.PreferenceManager
import android.support.annotation.IntDef
import android.text.TextUtils

import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.utils.UtilsFormat
import ru.p3tr0vich.fuel.utils.UtilsLog

class PreferencesHelper private constructor(var context: Context) : SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val TAG = "PreferencesHelper"

        @SuppressLint("StaticFieldLeak")
        private var instance: PreferencesHelper? = null

        private var LOG_ENABLED = false

        const val SYNC_NONE = java.lang.Long.MIN_VALUE

        /**
         * Центр карты по умолчанию.
         */
        const val DEFAULT_MAP_CENTER_TEXT = "Москва, Кремль"
        /**
         * Широта центра карты по умолчанию.
         */
        const val DEFAULT_MAP_CENTER_LATITUDE = 55.752023
        /**
         * Долгота центра карты по умолчанию.
         */
        const val DEFAULT_MAP_CENTER_LONGITUDE = 37.617499

        const val PREFERENCE_TYPE_STRING = 0
        const val PREFERENCE_TYPE_INT = 1
        const val PREFERENCE_TYPE_LONG = 2

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): PreferencesHelper {
            if (instance == null) {
                instance = PreferencesHelper(context.applicationContext)
            }

            return instance as PreferencesHelper
        }
    }

    private val mSharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val keys = Keys(context)

    private val isChanged: Boolean
        get() = mSharedPreferences.getBoolean(keys.changed, true)

    val isSyncEnabled: Boolean
        get() = mSharedPreferences.getBoolean(keys.syncEnabled, false)

    val isSMSEnabled: Boolean
        get() = mSharedPreferences.getBoolean(keys.smsEnabled, false)

    val smsAddress: String
        get() = getString(keys.smsAddress)

    val smsTextPattern: String
        get() = getString(keys.smsTextPattern)

    val smsText: String
        get() = getString(keys.smsText)

    private val isFullSync: Boolean
        get() = mSharedPreferences.getBoolean(keys.databaseFullSync, false)

    val lastSyncDateTime: Long
        get() = mSharedPreferences.getLong(keys.lastSyncDateTime, SYNC_NONE)

    val lastSyncHasError: Boolean
        get() = mSharedPreferences.getBoolean(keys.lastSyncHasError, false)

    val filterDateFrom: Long
        get() = mSharedPreferences.getLong(keys.filterDateFrom, System.currentTimeMillis())

    val filterDateTo: Long
        get() = mSharedPreferences.getLong(keys.filterDateTo, System.currentTimeMillis())

    val defaultCost: Float
        get() = UtilsFormat.stringToFloat(getString(keys.defaultCost))

    val defaultVolume: Float
        get() = UtilsFormat.stringToFloat(getString(keys.defaultVolume))

    val lastTotal: Float
        get() = UtilsFormat.stringToFloat(getString(context.getString(R.string.pref_key_last_total)))

    val calcDistance: String
        get() = getString(keys.distance)

    val calcCost: String
        get() = getString(keys.cost)

    val calcVolume: String
        get() = getString(keys.volume)

    val priceAsString: String
        get() = getString(keys.price)

    val price: Float
        get() = UtilsFormat.stringToFloat(priceAsString)

    val calcCons: Array<FloatArray>
        get() {
            val result = arrayOf(floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f))

            result[0][0] = UtilsFormat.stringToFloat(getString(context.getString(R.string.pref_key_summer_city)))
            result[0][1] = UtilsFormat.stringToFloat(getString(context.getString(R.string.pref_key_summer_highway)))
            result[0][2] = UtilsFormat.stringToFloat(getString(context.getString(R.string.pref_key_summer_mixed)))
            result[1][0] = UtilsFormat.stringToFloat(getString(context.getString(R.string.pref_key_winter_city)))
            result[1][1] = UtilsFormat.stringToFloat(getString(context.getString(R.string.pref_key_winter_highway)))
            result[1][2] = UtilsFormat.stringToFloat(getString(context.getString(R.string.pref_key_winter_mixed)))

            return result
        }

    val calcSelectedCons: Int
        get() = mSharedPreferences.getInt(keys.consumption, 0)

    val calcSelectedSeason: Int
        get() = mSharedPreferences.getInt(keys.season, 0)

    val mapCenterText: String
        get() = getString(keys.mapCenterText, DEFAULT_MAP_CENTER_TEXT)

    val mapCenterLatitude: Double
        get() = java.lang.Double.longBitsToDouble(mSharedPreferences.getLong(
                keys.mapCenterLatitude,
                java.lang.Double.doubleToLongBits(DEFAULT_MAP_CENTER_LATITUDE)))

    val mapCenterLongitude: Double
        get() = java.lang.Double.longBitsToDouble(mSharedPreferences.getLong(
                keys.mapCenterLongitude,
                java.lang.Double.doubleToLongBits(DEFAULT_MAP_CENTER_LONGITUDE)))

    val preferences: Cursor
        get() = getPreferencesCursor(null)

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(PREFERENCE_TYPE_STRING, PREFERENCE_TYPE_INT, PREFERENCE_TYPE_LONG)
    annotation class PreferenceType

    class Keys constructor(context: Context) {
        val price: String = context.getString(R.string.pref_key_price)

        val defaultCost: String = context.getString(R.string.pref_key_def_cost)
        val defaultVolume: String = context.getString(R.string.pref_key_def_volume)

        val mapCenterText: String = context.getString(R.string.pref_key_map_center_text)
        val mapCenterLatitude: String = context.getString(R.string.pref_key_map_center_latitude)
        val mapCenterLongitude: String = context.getString(R.string.pref_key_map_center_longitude)

        val sync: String = context.getString(R.string.pref_key_sync)
        val syncEnabled: String = context.getString(R.string.pref_key_sync_enabled)
        val syncYandexDisk: String = context.getString(R.string.pref_key_sync_yandex_disk)

        val sms: String = context.getString(R.string.pref_key_sms)
        val smsEnabled: String = context.getString(R.string.pref_key_sms_enabled)
        val smsAddress: String = context.getString(R.string.pref_key_sms_address)
        val smsText: String = context.getString(R.string.pref_key_sms_text)
        val smsTextPattern: String = context.getString(R.string.pref_key_sms_text_pattern)

        val databaseRevision: String = context.getString(R.string.pref_key_database_revision)
        val preferencesRevision: String = context.getString(R.string.pref_key_preferences_revision)

        val changed: String = context.getString(R.string.pref_key_changed)
        val lastSyncDateTime: String = context.getString(R.string.pref_key_last_sync_date_time)
        val lastSyncHasError: String = context.getString(R.string.pref_key_last_sync_has_error)
        val databaseFullSync: String = context.getString(R.string.pref_key_database_full_sync)

        val filterDateFrom: String = context.getString(R.string.pref_key_filter_date_from)
        val filterDateTo: String = context.getString(R.string.pref_key_filter_date_to)

        val distance: String = context.getString(R.string.pref_key_distance)
        val cost: String = context.getString(R.string.pref_key_cost)
        val volume: String = context.getString(R.string.pref_key_volume)

        val consumption: String = context.getString(R.string.pref_key_consumption)
        val season: String = context.getString(R.string.pref_key_season)

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(UNKNOWN, PRICE, DEFAULT_COST, DEFAULT_VOLUME,
                MAP_CENTER_TEXT, MAP_CENTER_LATITUDE, MAP_CENTER_LONGITUDE, SYNC, SYNC_ENABLED, SYNC_YANDEX_DISK, SMS, SMS_ENABLED, SMS_ADDRESS, SMS_TEXT, SMS_TEXT_PATTERN, DATABASE_REVISION, PREFERENCES_REVISION, CHANGED, LAST_SYNC_DATE_TIME, LAST_SYNC_HAS_ERROR, DATABASE_FULL_SYNC, FILTER_DATE_FROM, FILTER_DATE_TO, DISTANCE, COST, VOLUME, CONSUMPTION, SEASON)
        annotation class KeyAsInt

        @KeyAsInt
        fun getAsInt(key: String?): Int {
            return when {
                price == key -> PRICE

                defaultCost == key -> DEFAULT_COST
                defaultVolume == key -> DEFAULT_VOLUME

                mapCenterText == key -> MAP_CENTER_TEXT
                mapCenterLatitude == key -> MAP_CENTER_LATITUDE
                mapCenterLongitude == key -> MAP_CENTER_LONGITUDE

                sync == key -> SYNC
                syncEnabled == key -> SYNC_ENABLED
                syncYandexDisk == key -> SYNC_YANDEX_DISK

                sms == key -> SMS
                smsEnabled == key -> SMS_ENABLED
                smsAddress == key -> SMS_ADDRESS
                smsText == key -> SMS_TEXT
                smsTextPattern == key -> SMS_TEXT_PATTERN

                databaseRevision == key -> DATABASE_REVISION
                preferencesRevision == key -> PREFERENCES_REVISION

                changed == key -> CHANGED
                lastSyncDateTime == key -> LAST_SYNC_DATE_TIME
                lastSyncHasError == key -> LAST_SYNC_HAS_ERROR
                databaseFullSync == key -> DATABASE_FULL_SYNC

                filterDateFrom == key -> FILTER_DATE_FROM
                filterDateTo == key -> FILTER_DATE_TO

                distance == key -> DISTANCE
                cost == key -> COST
                volume == key -> VOLUME

                consumption == key -> CONSUMPTION
                season == key -> SEASON
                else -> UNKNOWN
            }
        }

        fun isSyncKey(key: String?): Boolean {
            return when (getAsInt(key)) {
                SYNC_ENABLED, SMS_ENABLED,
                CHANGED, DATABASE_REVISION, PREFERENCES_REVISION,
                LAST_SYNC_DATE_TIME, LAST_SYNC_HAS_ERROR, DATABASE_FULL_SYNC -> false

                CONSUMPTION, COST, DEFAULT_COST, DEFAULT_VOLUME, DISTANCE,
                FILTER_DATE_FROM, FILTER_DATE_TO,
                MAP_CENTER_LATITUDE, MAP_CENTER_LONGITUDE, MAP_CENTER_TEXT,
                PRICE, SEASON,
                SMS_ADDRESS, SMS_TEXT_PATTERN, SMS_TEXT, SMS,
                SYNC_YANDEX_DISK, SYNC, UNKNOWN, VOLUME -> true
                else -> false
            }
        }

        companion object {
            const val UNKNOWN = -1

            const val PRICE = R.string.pref_key_price

            const val DEFAULT_COST = R.string.pref_key_def_cost
            const val DEFAULT_VOLUME = R.string.pref_key_def_volume

            const val MAP_CENTER_TEXT = R.string.pref_key_map_center_text
            const val MAP_CENTER_LATITUDE = R.string.pref_key_map_center_latitude
            const val MAP_CENTER_LONGITUDE = R.string.pref_key_map_center_longitude

            const val SYNC = R.string.pref_key_sync
            const val SYNC_ENABLED = R.string.pref_key_sync_enabled
            const val SYNC_YANDEX_DISK = R.string.pref_key_sync_yandex_disk

            const val SMS = R.string.pref_key_sms
            const val SMS_ENABLED = R.string.pref_key_sms_enabled
            const val SMS_ADDRESS = R.string.pref_key_sms_address
            const val SMS_TEXT = R.string.pref_key_sms_text
            const val SMS_TEXT_PATTERN = R.string.pref_key_sms_text_pattern

            const val DATABASE_REVISION = R.string.pref_key_database_revision
            const val PREFERENCES_REVISION = R.string.pref_key_preferences_revision

            const val CHANGED = R.string.pref_key_changed
            const val LAST_SYNC_DATE_TIME = R.string.pref_key_last_sync_date_time
            const val LAST_SYNC_HAS_ERROR = R.string.pref_key_last_sync_has_error
            const val DATABASE_FULL_SYNC = R.string.pref_key_database_full_sync

            const val FILTER_DATE_FROM = R.string.pref_key_filter_date_from
            const val FILTER_DATE_TO = R.string.pref_key_filter_date_to

            const val DISTANCE = R.string.pref_key_distance
            const val COST = R.string.pref_key_cost
            const val VOLUME = R.string.pref_key_volume

            const val CONSUMPTION = R.string.pref_key_consumption
            const val SEASON = R.string.pref_key_season
        }
    }

    init {
        //        mSharedPreferences.edit()
        //                .remove("last sync")
        //                .remove("full sync")
        //                .apply();

        mSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onSharedPreferenceChanged", "key == $key")
        }

        if (keys.isSyncKey(key)) {
            putChanged(true)
        }
    }

    private fun putChanged(changed: Boolean) {
        mSharedPreferences
                .edit()
                .putBoolean(keys.changed, changed)
                .apply()

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "putChanged", "changed == $changed")
        }

        if (changed)
            context.contentResolver.notifyChange(ContentProviderHelper.URI_PREFERENCES, null, false)
    }

    fun putSMSAddress(address: String) {
        mSharedPreferences
                .edit()
                .putString(keys.smsAddress, address)
                .apply()
    }

    fun putSMSTextAndPattern(text: String, pattern: String) {
        mSharedPreferences
                .edit()
                .putString(keys.smsText, text)
                .putString(keys.smsTextPattern, pattern)
                .apply()
    }

    private fun getRevision(keyRevision: String?): Int {
        return mSharedPreferences.getInt(keyRevision, -1)
    }

    private fun putRevision(keyRevision: String?, revision: Int) {
        mSharedPreferences
                .edit()
                .putInt(keyRevision, revision)
                .apply()

        if (LOG_ENABLED) UtilsLog.d(TAG, "putRevision", "$keyRevision == $revision")
    }

    fun putFullSync(fullSync: Boolean) {
        mSharedPreferences
                .edit()
                .putBoolean(keys.databaseFullSync, fullSync)
                .apply()
    }

    private fun putLastSyncDateTime(dateTime: Long) {
        mSharedPreferences
                .edit()
                .putLong(keys.lastSyncDateTime, dateTime)
                .apply()
    }

    private fun putLastSyncHasError(hasError: Boolean) {
        mSharedPreferences
                .edit()
                .putBoolean(keys.lastSyncHasError, hasError)
                .apply()
    }

    fun putFilterDate(dateFrom: Long, dateTo: Long) {
        mSharedPreferences
                .edit()
                .putLong(keys.filterDateFrom, dateFrom)
                .putLong(keys.filterDateTo, dateTo)
                .apply()
    }

    fun putLastTotal(lastTotal: Float) {
        mSharedPreferences
                .edit()
                .putString(context.getString(R.string.pref_key_last_total), lastTotal.toString())
                .apply()
    }

    fun putCalc(distance: String, cost: String, volume: String, cons: Int, season: Int) {
        mSharedPreferences
                .edit()
                .putString(keys.distance, distance)
                .putString(keys.cost, cost)
                .putString(keys.volume, volume)
                .putInt(keys.consumption, cons)
                .putInt(keys.season, season)
                .apply()
    }

    fun putMapCenter(text: String, latitude: Double, longitude: Double) {
        mSharedPreferences
                .edit()
                .putString(keys.mapCenterText, text)
                .putLong(keys.mapCenterLatitude, java.lang.Double.doubleToRawLongBits(latitude))
                .putLong(keys.mapCenterLongitude, java.lang.Double.doubleToRawLongBits(longitude))
                .apply()
    }

    private fun getString(key: String, defValue: String): String {
        return mSharedPreferences.getString(key, defValue) ?: defValue
    }

    fun getString(key: String): String {
        return getString(key, "")
    }

    @SuppressLint("SwitchIntDef")
    private fun getPreferences(preference: String?): ContentValues {
        val result = ContentValues()

        if (TextUtils.isEmpty(preference)) {
            val map = mSharedPreferences.all

            var key: String
            var value: Any?

            for (entry in map) {
                key = entry.key

                if (keys.isSyncKey(key)) {
                    value = entry.value

                    when (value) {
                        is String -> result.put(key, value)
                        is Long -> result.put(key, value)
                        is Int -> result.put(key, value)
                        is Boolean -> result.put(key, value)
                        is Float -> result.put(key, value)
                        else -> UtilsLog.d(TAG, "getPreferences",
                                "unhandled class == " + value?.javaClass?.simpleName)
                    }
                }
            }
        } else {
            when (keys.getAsInt(preference)) {
                Keys.CHANGED -> result.put(preference, isChanged)
                Keys.DATABASE_FULL_SYNC -> result.put(preference, isFullSync)
                Keys.DATABASE_REVISION,
                Keys.PREFERENCES_REVISION -> result.put(preference, getRevision(preference))
                Keys.LAST_SYNC_DATE_TIME -> result.put(preference, lastSyncDateTime)
                Keys.LAST_SYNC_HAS_ERROR -> result.put(preference, lastSyncHasError)
                else -> UtilsLog.d(TAG, "getPreferences", "unhandled preference == $preference")
            }
        }

        //        for (String key : result.keySet())
        //            UtilsLog.d(TAG, "getPreferences", "key == " + key + ", value == " + result.getAsString(key));

        return result
    }

    private fun getPreferencesCursor(preference: String?): Cursor {
        val matrixCursor = MatrixCursor(arrayOf("key", "value"))

        val preferences = getPreferences(preference)

        for (key in preferences.keySet()) {
            matrixCursor.addRow(arrayOf(key, preferences.get(key)))
        }

        return matrixCursor
    }

    fun getPreference(preference: String?): Cursor {
        return getPreferencesCursor(preference)
    }

    @SuppressLint("ApplySharedPref")
    fun setPreferences(preferences: ContentValues?, preference: String?): Int {
        if (preferences == null || preferences.size() == 0) {
            return -1
        }

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "setPreferences", "preference == $preference")
        }

        if (TextUtils.isEmpty(preference)) {
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

            val editor = mSharedPreferences.edit()

            try {
                var value: Any

                for (key in preferences.keySet()) {
                    value = preferences.get(key)

                    when (value) {
                        is String -> editor.putString(key, value)
                        is Long -> editor.putLong(key, value)
                        is Int -> editor.putInt(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is Float -> editor.putFloat(key, value)
                        else -> UtilsLog.d(TAG, "setPreferences",
                                "unhandled class == " + value.javaClass.simpleName)
                    }
                }
            } finally {
                editor.commit()

                mSharedPreferences.registerOnSharedPreferenceChangeListener(this)
            }

            return preferences.size()
        } else {
            when (keys.getAsInt(preference)) {
                Keys.CHANGED -> putChanged(preferences.getAsBoolean(preference)!!)
                Keys.DATABASE_REVISION,
                Keys.PREFERENCES_REVISION -> putRevision(preference, preferences.getAsInteger(preference)!!)
                Keys.DATABASE_FULL_SYNC -> putFullSync(preferences.getAsBoolean(preference)!!)
                Keys.LAST_SYNC_DATE_TIME -> putLastSyncDateTime(preferences.getAsLong(preference)!!)
                Keys.LAST_SYNC_HAS_ERROR -> putLastSyncHasError(preferences.getAsBoolean(preference)!!)
                else -> UtilsLog.d(TAG, "setPreferences", "unhandled preference == $preference")
            }

            return 1
        }
    }

    @PreferenceType
    fun getPreferenceType(key: String): Int {
        return when (keys.getAsInt(key)) {
            Keys.CONSUMPTION,
            Keys.SEASON -> PREFERENCE_TYPE_INT
            Keys.FILTER_DATE_FROM, Keys.FILTER_DATE_TO,
            Keys.MAP_CENTER_LATITUDE, Keys.MAP_CENTER_LONGITUDE -> PREFERENCE_TYPE_LONG
            else -> PREFERENCE_TYPE_STRING
        }
    }
}