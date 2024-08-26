package ru.p3tr0vich.fuel.helpers

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.MatrixCursor
import androidx.preference.PreferenceManager
import android.text.TextUtils
import androidx.annotation.IntDef
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.models.MapCenter
import ru.p3tr0vich.fuel.models.MapCenter.Companion.DEFAULT_MAP_CENTER_LATITUDE
import ru.p3tr0vich.fuel.models.MapCenter.Companion.DEFAULT_MAP_CENTER_LONGITUDE
import ru.p3tr0vich.fuel.models.MapCenter.Companion.DEFAULT_MAP_CENTER_TEXT
import ru.p3tr0vich.fuel.utils.UtilsFormat
import ru.p3tr0vich.fuel.utils.UtilsLog

class PreferencesHelper private constructor(private val context: Context) :
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val keys = Keys(context)

    private var isChanged: Boolean
        get() = sharedPreferences.getBoolean(keys.changed, true)
        set(value) {
            sharedPreferences
                .edit()
                .putBoolean(keys.changed, value)
                .apply()

            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "putChanged", "value == $value")
            }

            if (value) {
                context.contentResolver.notifyChange(
                    ContentProviderHelper.URI_PREFERENCES,
                    null
                )
            }
        }

    val isSyncEnabled: Boolean
        get() = sharedPreferences.getBoolean(keys.syncEnabled, false)

    val isSMSEnabled: Boolean
        get() = sharedPreferences.getBoolean(keys.smsEnabled, false)

    var smsAddress: String
        get() = getString(keys.smsAddress)
        set(value) {
            sharedPreferences
                .edit()
                .putString(keys.smsAddress, value)
                .apply()
        }

    var smsTextPattern: String
        get() = getString(keys.smsTextPattern)
        set(value) {
            sharedPreferences
                .edit()
                .putString(keys.smsTextPattern, value)
                .apply()
        }

    var smsText: String
        get() = getString(keys.smsText)
        set(value) {
            sharedPreferences
                .edit()
                .putString(keys.smsText, value)
                .apply()
        }

    var isFullSync: Boolean
        get() = sharedPreferences.getBoolean(keys.databaseFullSync, false)
        set(value) {
            sharedPreferences
                .edit()
                .putBoolean(keys.databaseFullSync, value)
                .apply()
        }

    var lastSyncDateTime: Long
        get() = sharedPreferences.getLong(keys.lastSyncDateTime, SYNC_NONE)
        set(value) {
            sharedPreferences
                .edit()
                .putLong(keys.lastSyncDateTime, value)
                .apply()
        }

    var lastSyncHasError: Boolean
        get() = sharedPreferences.getBoolean(keys.lastSyncHasError, false)
        set(value) {
            sharedPreferences
                .edit()
                .putBoolean(keys.lastSyncHasError, value)
                .apply()
        }

    val filterDateFrom: Long
        get() = sharedPreferences.getLong(keys.filterDateFrom, System.currentTimeMillis())

    val filterDateTo: Long
        get() = sharedPreferences.getLong(keys.filterDateTo, System.currentTimeMillis())

    val defaultCost: Float
        get() = UtilsFormat.stringToFloat(getString(keys.defaultCost))

    val defaultVolume: Float
        get() = UtilsFormat.stringToFloat(getString(keys.defaultVolume))

    var lastTotal: Float
        get() = UtilsFormat.stringToFloat(getString(context.getString(R.string.pref_key_last_total)))
        set(value) {
            sharedPreferences
                .edit()
                .putString(context.getString(R.string.pref_key_last_total), value.toString())
                .apply()
        }

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

            result[0][0] =
                UtilsFormat.stringToFloat(getString(keys.summerCity))
            result[0][1] =
                UtilsFormat.stringToFloat(getString(keys.summerHighway))
            result[0][2] =
                UtilsFormat.stringToFloat(getString(keys.summerMixed))
            result[1][0] =
                UtilsFormat.stringToFloat(getString(keys.winterCity))
            result[1][1] =
                UtilsFormat.stringToFloat(getString(keys.winterHighway))
            result[1][2] =
                UtilsFormat.stringToFloat(getString(keys.winterMixed))

            return result
        }

    val calcSelectedCons: Int
        get() = sharedPreferences.getInt(keys.consumption, 0)

    val calcSelectedSeason: Int
        get() = sharedPreferences.getInt(keys.season, 0)

    var mapCenter: MapCenter
        get() = MapCenter(
            getString(keys.mapCenterText, DEFAULT_MAP_CENTER_TEXT),
            java.lang.Double.longBitsToDouble(
                sharedPreferences.getLong(
                    keys.mapCenterLatitude,
                    java.lang.Double.doubleToLongBits(DEFAULT_MAP_CENTER_LATITUDE)
                )
            ),
            java.lang.Double.longBitsToDouble(
                sharedPreferences.getLong(
                    keys.mapCenterLongitude,
                    java.lang.Double.doubleToLongBits(DEFAULT_MAP_CENTER_LONGITUDE)
                )
            )
        )
        set(value) {
            sharedPreferences
                .edit()
                .putString(keys.mapCenterText, value.text)
                .putLong(
                    keys.mapCenterLatitude,
                    java.lang.Double.doubleToRawLongBits(value.latitude)
                )
                .putLong(
                    keys.mapCenterLongitude,
                    java.lang.Double.doubleToRawLongBits(value.longitude)
                )
                .apply()
        }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(PREFERENCE_TYPE_STRING, PREFERENCE_TYPE_INT, PREFERENCE_TYPE_LONG)
    annotation class PreferenceType

    class Keys constructor(context: Context) {
        val price = context.getString(R.string.pref_key_price)

        val defaultCost = context.getString(R.string.pref_key_def_cost)
        val defaultVolume = context.getString(R.string.pref_key_def_volume)

        val mapCenterText = context.getString(R.string.pref_key_map_center_text)
        val mapCenterLatitude = context.getString(R.string.pref_key_map_center_latitude)
        val mapCenterLongitude = context.getString(R.string.pref_key_map_center_longitude)

        val sync = context.getString(R.string.pref_key_sync)
        val syncEnabled = context.getString(R.string.pref_key_sync_enabled)
        val syncYandexDisk = context.getString(R.string.pref_key_sync_yandex_disk)

        val sms = context.getString(R.string.pref_key_sms)
        val smsEnabled = context.getString(R.string.pref_key_sms_enabled)
        val smsAddress = context.getString(R.string.pref_key_sms_address)
        val smsText = context.getString(R.string.pref_key_sms_text)
        val smsTextPattern = context.getString(R.string.pref_key_sms_text_pattern)

        val databaseRevision = context.getString(R.string.pref_key_database_revision)
        val preferencesRevision = context.getString(R.string.pref_key_preferences_revision)

        val changed = context.getString(R.string.pref_key_changed)
        val lastSyncDateTime = context.getString(R.string.pref_key_last_sync_date_time)
        val lastSyncHasError = context.getString(R.string.pref_key_last_sync_has_error)
        val databaseFullSync = context.getString(R.string.pref_key_database_full_sync)

        val filterDateFrom = context.getString(R.string.pref_key_filter_date_from)
        val filterDateTo = context.getString(R.string.pref_key_filter_date_to)

        val distance = context.getString(R.string.pref_key_distance)
        val cost = context.getString(R.string.pref_key_cost)
        val volume = context.getString(R.string.pref_key_volume)

        val consumption = context.getString(R.string.pref_key_consumption)
        val season = context.getString(R.string.pref_key_season)

        val summerCity = context.getString(R.string.pref_key_summer_city)
        val summerHighway = context.getString(R.string.pref_key_summer_highway)
        val summerMixed = context.getString(R.string.pref_key_summer_mixed)
        val winterCity = context.getString(R.string.pref_key_winter_city)
        val winterHighway = context.getString(R.string.pref_key_winter_highway)
        val winterMixed = context.getString(R.string.pref_key_summer_mixed)

        fun isSyncKey(key: String?): Boolean {
            return when (key) {
                syncEnabled,
                smsEnabled,
                changed, databaseRevision, preferencesRevision,
                lastSyncDateTime, lastSyncHasError, databaseFullSync -> false

                consumption, cost, defaultCost, defaultVolume, distance -> true

                filterDateFrom, filterDateTo -> true

                mapCenterLatitude, mapCenterLongitude, mapCenterText -> true

                price, season -> true
                summerCity, summerHighway, summerMixed,
                winterCity, winterHighway, winterMixed -> true

                smsAddress, smsTextPattern, smsText, sms -> true
                syncYandexDisk, sync, volume -> true

                else -> false
            }
        }
    }

    init {
        //        sharedPreferences.edit()
        //                .remove("last sync")
        //                .remove("full sync")
        //                .apply();

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onSharedPreferenceChanged", "key == $key")
        }

        if (keys.isSyncKey(key)) {
            isChanged = true
        }
    }

    private fun getRevision(keyRevision: String?): Int {
        return sharedPreferences.getInt(keyRevision, -1)
    }

    private fun putRevision(keyRevision: String?, revision: Int) {
        sharedPreferences
            .edit()
            .putInt(keyRevision, revision)
            .apply()

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "putRevision", "$keyRevision == $revision")
        }
    }

    fun putFilterDate(dateFrom: Long, dateTo: Long) {
        sharedPreferences
            .edit()
            .putLong(keys.filterDateFrom, dateFrom)
            .putLong(keys.filterDateTo, dateTo)
            .apply()
    }

    fun putCalc(distance: String, cost: String, volume: String, cons: Int, season: Int) {
        sharedPreferences
            .edit()
            .putString(keys.distance, distance)
            .putString(keys.cost, cost)
            .putString(keys.volume, volume)
            .putInt(keys.consumption, cons)
            .putInt(keys.season, season)
            .apply()
    }

    private fun getString(key: String, defValue: String): String {
        return sharedPreferences.getString(key, defValue) ?: defValue
    }

    fun getString(key: String): String {
        return getString(key, "")
    }

    private fun getPreferences(preference: String?): ContentValues {
        val result = ContentValues()

        if (TextUtils.isEmpty(preference)) {
            for ((key, value) in sharedPreferences.all) {
                if (keys.isSyncKey(key)) {
                    when (value) {
                        is String -> result.put(key, value)
                        is Long -> result.put(key, value)
                        is Int -> result.put(key, value)
                        is Boolean -> result.put(key, value)
                        is Float -> result.put(key, value)

                        else -> throw IllegalArgumentException("$TAG -- getPreferences: unhandled class == ${value?.javaClass?.simpleName}")
                    }
                }
            }
        } else {
            when (preference) {
                keys.changed -> result.put(preference, isChanged)

                keys.databaseFullSync -> result.put(preference, isFullSync)

                keys.databaseRevision,
                keys.preferencesRevision -> result.put(preference, getRevision(preference))

                keys.lastSyncDateTime -> result.put(preference, lastSyncDateTime)
                keys.lastSyncHasError -> result.put(preference, lastSyncHasError)

                else -> throw IllegalArgumentException("$TAG -- getPreferences: unhandled preference == $preference")
            }
        }

        if (LOG_ENABLED) {
            for (key in result.keySet()) {
                UtilsLog.d(
                    TAG,
                    "getPreferences($preference)",
                    "key == $key, value == ${result.getAsString(key)}"
                )
            }
        }

        return result
    }

    fun getPreferencesCursor(preference: String? = null): Cursor {
        val matrixCursor = MatrixCursor(arrayOf("key", "value"))

        val preferences = getPreferences(preference)

        for (key in preferences.keySet()) {
            matrixCursor.addRow(arrayOf(key, preferences.get(key)))
        }

        return matrixCursor
    }

    @SuppressLint("ApplySharedPref")
    fun setPreferences(preferences: ContentValues?, preference: String?): Int {
        if (preferences == null || preferences.size() == 0) {
            return -1
        }

        if (LOG_ENABLED) {
            UtilsLog.d(
                TAG,
                "setPreferences",
                "preferences == $preferences, preference == $preference"
            )
        }

        if (TextUtils.isEmpty(preference)) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

            val editor = sharedPreferences.edit()

            try {
                for ((key, value) in preferences.valueSet()) {
                    when (value) {
                        is String -> editor.putString(key, value)
                        is Long -> editor.putLong(key, value)
                        is Int -> editor.putInt(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is Float -> editor.putFloat(key, value)

                        else -> throw IllegalArgumentException("$TAG -- setPreferences: unhandled class == ${value?.javaClass?.simpleName}")
                    }
                }
            } finally {
                editor.commit()

                sharedPreferences.registerOnSharedPreferenceChangeListener(this)
            }

            return preferences.size()
        } else {
            when (preference) {
                keys.changed -> isChanged = preferences.getAsBoolean(preference)

                keys.databaseRevision,
                keys.preferencesRevision -> putRevision(
                    preference,
                    preferences.getAsInteger(preference)
                )

                keys.databaseFullSync -> isFullSync = preferences.getAsBoolean(preference)

                keys.lastSyncDateTime -> lastSyncDateTime = preferences.getAsLong(preference)
                keys.lastSyncHasError -> lastSyncHasError = preferences.getAsBoolean(preference)

                else -> throw IllegalArgumentException("$TAG -- setPreferences: unhandled preference == $preference")
            }

            return 1
        }
    }

    @PreferenceType
    fun getPreferenceType(key: String): Int {
        return when (key) {
            keys.consumption,
            keys.season -> {
                PREFERENCE_TYPE_INT
            }

            keys.filterDateFrom, keys.filterDateTo,
            keys.mapCenterLatitude, keys.mapCenterLongitude -> {
                PREFERENCE_TYPE_LONG
            }

            else -> {
                PREFERENCE_TYPE_STRING
            }
        }
    }

    companion object {
        private const val TAG = "PreferencesHelper"

        @SuppressLint("StaticFieldLeak")
        private var instance: PreferencesHelper? = null

        private var LOG_ENABLED = false

        const val SYNC_NONE = java.lang.Long.MIN_VALUE

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
}