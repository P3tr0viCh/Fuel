package ru.p3tr0vich.fuel.helpers;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import ru.p3tr0vich.fuel.R;
import ru.p3tr0vich.fuel.utils.UtilsFormat;
import ru.p3tr0vich.fuel.utils.UtilsLog;

public class PreferencesHelper implements SharedPreferences.OnSharedPreferenceChangeListener {

    @SuppressLint("StaticFieldLeak")
    private static PreferencesHelper instance;

    private static final String TAG = "PreferencesHelper";

    public static final long SYNC_NONE = Long.MIN_VALUE;

    public static final String DEFAULT_MAP_CENTER_TEXT = "Москва, Кремль";
    public static final double DEFAULT_MAP_CENTER_LATITUDE = 55.752023;  // Широта
    public static final double DEFAULT_MAP_CENTER_LONGITUDE = 37.617499; // Долгота

    private Context mContext; // == ApplicationContext
    private SharedPreferences mSharedPreferences;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PREFERENCE_TYPE_STRING, PREFERENCE_TYPE_INT, PREFERENCE_TYPE_LONG})
    public @interface PreferenceType {
    }

    public static final int PREFERENCE_TYPE_STRING = 0;
    public static final int PREFERENCE_TYPE_INT = 1;
    public static final int PREFERENCE_TYPE_LONG = 2;

    public static class Keys {
        public final String mapCenterText;
        public final String mapCenterLatitude;
        public final String mapCenterLongitude;

        public final String sync;
        public final String syncEnabled;
        public final String syncYandexDisk;

        public final String sms;
        public final String smsEnabled;
        public final String smsAddress;
        public final String smsText;
        public final String smsTextPattern;

        public final String databaseRevision;
        public final String preferencesRevision;

        public final String changed;
        public final String lastSyncDateTime;
        public final String lastSyncHasError;
        public final String databaseFullSync;

        public final String filterDateFrom;
        public final String filterDateTo;

        public final String distance;
        public final String cost;
        public final String volume;
        public final String price;

        public final String consumption;
        public final String season;

        private Keys(@NonNull Context context) {
            mapCenterText = context.getString(R.string.pref_key_map_center_text);
            mapCenterLatitude = context.getString(R.string.pref_key_map_center_latitude);
            mapCenterLongitude = context.getString(R.string.pref_key_map_center_longitude);

            sync = context.getString(R.string.pref_key_sync);
            syncEnabled = context.getString(R.string.pref_key_sync_enabled);
            syncYandexDisk = context.getString(R.string.pref_key_sync_yandex_disk);

            sms = context.getString(R.string.pref_key_sms);
            smsEnabled = context.getString(R.string.pref_key_sms_enabled);
            smsAddress = context.getString(R.string.pref_key_sms_address);
            smsText = context.getString(R.string.pref_key_sms_text);
            smsTextPattern = context.getString(R.string.pref_key_sms_text_pattern);

            databaseRevision = context.getString(R.string.pref_key_database_revision);
            preferencesRevision = context.getString(R.string.pref_key_preferences_revision);

            changed = context.getString(R.string.pref_key_changed);
            lastSyncDateTime = context.getString(R.string.pref_key_last_sync_date_time);
            lastSyncHasError = context.getString(R.string.pref_key_last_sync_has_error);
            databaseFullSync = context.getString(R.string.pref_key_database_full_sync);

            filterDateFrom = context.getString(R.string.pref_key_filter_date_from);
            filterDateTo = context.getString(R.string.pref_key_filter_date_to);

            distance = context.getString(R.string.pref_key_distance);
            cost = context.getString(R.string.pref_key_cost);
            volume = context.getString(R.string.pref_key_volume);
            price = context.getString(R.string.pref_key_price);

            consumption = context.getString(R.string.pref_key_consumption);
            season = context.getString(R.string.pref_key_season);
        }
    }

    public final Keys keys;

    private PreferencesHelper(@NonNull Context context) {
        mContext = context.getApplicationContext();

        keys = new Keys(context);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        mSharedPreferences.edit()
                .remove("last sync")
                .remove("full sync")
                .apply();

        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public static synchronized PreferencesHelper getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new PreferencesHelper(context);
        }

        return instance;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        UtilsLog.d(TAG, "onSharedPreferenceChanged", "key == " + key);

        if (isSyncKey(key)) putChanged(true);
    }

    private boolean isSyncKey(@NonNull String key) {
        return !(key.equals(keys.syncEnabled) ||
                key.equals(keys.smsEnabled) ||
                key.equals(keys.changed) ||
                key.equals(keys.databaseRevision) ||
                key.equals(keys.preferencesRevision) ||
                key.equals(keys.lastSyncDateTime) ||
                key.equals(keys.lastSyncHasError) ||
                key.equals(keys.databaseFullSync));
    }

    private boolean isChanged() {
        return mSharedPreferences.getBoolean(keys.changed, true);
    }

    private void putChanged(boolean changed) {
        mSharedPreferences
                .edit()
                .putBoolean(keys.changed, changed)
                .apply();

        UtilsLog.d(TAG, "putChanged", "changed == " + changed);

        if (changed)
            mContext.getContentResolver().notifyChange(ContentProviderHelper.URI_PREFERENCES, null, false);
    }

    public boolean isSyncEnabled() {
        return mSharedPreferences.getBoolean(keys.syncEnabled, false);
    }

    public boolean isSMSEnabled() {
        return mSharedPreferences.getBoolean(keys.smsEnabled, false);
    }

    @NonNull
    public String getSMSAddress() {
        return getString(keys.smsAddress);
    }

    public void putSMSAddress(String address) {
        mSharedPreferences
                .edit()
                .putString(keys.smsAddress, address)
                .apply();
    }

    @NonNull
    public String getSMSTextPattern() {
        return getString(keys.smsTextPattern);
    }

    @NonNull
    public String getSMSText() {
        return getString(keys.smsText);
    }

    public void putSMSTextAndPattern(String text, String pattern) {
        mSharedPreferences
                .edit()
                .putString(keys.smsText, text)
                .putString(keys.smsTextPattern, pattern)
                .apply();
    }

    private int getRevision(String keyRevision) {
        return mSharedPreferences.getInt(keyRevision, -1);
    }

    private void putRevision(String keyRevision, int revision) {
        mSharedPreferences
                .edit()
                .putInt(keyRevision, revision)
                .apply();

        UtilsLog.d(TAG, "putRevision", keyRevision + " == " + revision);
    }

    private boolean isFullSync() {
        return mSharedPreferences.getBoolean(keys.databaseFullSync, false);
    }

    public void putFullSync(boolean fullSync) {
        mSharedPreferences
                .edit()
                .putBoolean(keys.databaseFullSync, fullSync)
                .apply();
    }

    public long getLastSyncDateTime() {
        return mSharedPreferences.getLong(keys.lastSyncDateTime, SYNC_NONE);
    }

    public boolean getLastSyncHasError() {
        return mSharedPreferences.getBoolean(keys.lastSyncHasError, false);
    }

    private void putLastSyncDateTime(long dateTime) {
        mSharedPreferences
                .edit()
                .putLong(keys.lastSyncDateTime, dateTime)
                .apply();
    }

    private void putLastSyncHasError(boolean hasError) {
        mSharedPreferences
                .edit()
                .putBoolean(keys.lastSyncHasError, hasError)
                .apply();
    }

    public long getFilterDateFrom() {
        return mSharedPreferences.getLong(keys.filterDateFrom, System.currentTimeMillis());
    }

    public long getFilterDateTo() {
        return mSharedPreferences.getLong(keys.filterDateTo, System.currentTimeMillis());
    }

    public void putFilterDate(long dateFrom, long dateTo) {
        mSharedPreferences
                .edit()
                .putLong(keys.filterDateFrom, dateFrom)
                .putLong(keys.filterDateTo, dateTo)
                .apply();
    }

    public float getDefaultCost() {
        return UtilsFormat.stringToFloat(getString(mContext.getString(R.string.pref_key_def_cost)));
    }

    public float getDefaultVolume() {
        return UtilsFormat.stringToFloat(getString(mContext.getString(R.string.pref_key_def_volume)));
    }

    public float getLastTotal() {
        return UtilsFormat.stringToFloat(getString(mContext.getString(R.string.pref_key_last_total)));
    }

    public void putLastTotal(float lastTotal) {
        mSharedPreferences
                .edit()
                .putString(mContext.getString(R.string.pref_key_last_total), String.valueOf(lastTotal))
                .apply();
    }

    @NonNull
    public String getCalcDistance() {
        return getString(keys.distance);
    }

    @NonNull
    public String getCalcCost() {
        return getString(keys.cost);
    }

    @NonNull
    public String getCalcVolume() {
        return getString(keys.volume);
    }

    @NonNull
    public String getCalcPrice() {
        return getString(keys.price);
    }

    public float[][] getCalcCons() {

        float[][] result = {{0, 0, 0}, {0, 0, 0}};

        result[0][0] = UtilsFormat.stringToFloat(getString(mContext.getString(R.string.pref_key_summer_city)));
        result[0][1] = UtilsFormat.stringToFloat(getString(mContext.getString(R.string.pref_key_summer_highway)));
        result[0][2] = UtilsFormat.stringToFloat(getString(mContext.getString(R.string.pref_key_summer_mixed)));
        result[1][0] = UtilsFormat.stringToFloat(getString(mContext.getString(R.string.pref_key_winter_city)));
        result[1][1] = UtilsFormat.stringToFloat(getString(mContext.getString(R.string.pref_key_winter_highway)));
        result[1][2] = UtilsFormat.stringToFloat(getString(mContext.getString(R.string.pref_key_winter_mixed)));

        return result;
    }

    public int getCalcSelectedCons() {
        return mSharedPreferences.getInt(keys.consumption, 0);
    }

    public int getCalcSelectedSeason() {
        return mSharedPreferences.getInt(keys.season, 0);
    }

    public void putCalc(String distance, String cost, String volume,
                        String price, int cons, int season) {
        mSharedPreferences
                .edit()
                .putString(keys.distance, distance)
                .putString(keys.cost, cost)
                .putString(keys.volume, volume)
                .putString(keys.price, price)
                .putInt(keys.consumption, cons)
                .putInt(keys.season, season)
                .apply();
    }

    @NonNull
    public String getMapCenterText() {
        return getString(keys.mapCenterText, DEFAULT_MAP_CENTER_TEXT);
    }

    public double getMapCenterLatitude() {
        return Double.longBitsToDouble(mSharedPreferences.getLong(
                keys.mapCenterLatitude,
                Double.doubleToLongBits(DEFAULT_MAP_CENTER_LATITUDE)));
    }

    public double getMapCenterLongitude() {
        return Double.longBitsToDouble(mSharedPreferences.getLong(
                keys.mapCenterLongitude,
                Double.doubleToLongBits(DEFAULT_MAP_CENTER_LONGITUDE)));
    }

    public void putMapCenter(final String text, final double latitude, final double longitude) {
        mSharedPreferences
                .edit()
                .putString(keys.mapCenterText, text)
                .putLong(keys.mapCenterLatitude, Double.doubleToRawLongBits(latitude))
                .putLong(keys.mapCenterLongitude, Double.doubleToRawLongBits(longitude))
                .apply();
    }

    @NonNull
    private String getString(String key, @NonNull String defValue) {
        return mSharedPreferences.getString(key, defValue);
    }

    @NonNull
    public String getString(String key) {
        return getString(key, "");
    }

    @NonNull
    private ContentValues getPreferences(@Nullable String preference) {
        ContentValues result = new ContentValues();

        if (TextUtils.isEmpty(preference)) {
            Map<String, ?> map = mSharedPreferences.getAll();

            String key;
            Object value;

            for (Map.Entry<String, ?> entry : map.entrySet()) {
                key = entry.getKey();

                if (isSyncKey(key)) {
                    value = entry.getValue();

                    if (value instanceof String) result.put(key, (String) value);
                    else if (value instanceof Long) result.put(key, (Long) value);
                    else if (value instanceof Integer) result.put(key, (Integer) value);
                    else if (value instanceof Boolean) result.put(key, (Boolean) value);
                    else if (value instanceof Float) result.put(key, (Float) value);
                    else
                        UtilsLog.d(TAG, "getPreferences",
                                "unhandled class == " + value.getClass().getSimpleName());
                }
            }
        } else {
            if (preference.equals(keys.changed)) {
                result.put(preference, isChanged());
            } else if (preference.equals(keys.databaseFullSync)) {
                result.put(preference, isFullSync());
            } else if (preference.equals(keys.databaseRevision) || preference.equals(keys.preferencesRevision)) {
                result.put(preference, getRevision(preference));
            } else if (preference.equals(keys.lastSyncDateTime)) {
                result.put(preference, getLastSyncDateTime());
            } else if (preference.equals(keys.lastSyncHasError)) {
                result.put(preference, getLastSyncHasError());
            } else {
                UtilsLog.d(TAG, "getPreferences", "unhandled preference == " + preference);
            }
        }
//        for (String key : result.keySet())
//            UtilsLog.d(TAG, "getPreferences", "key == " + key + ", value == " + result.getAsString(key));

        return result;
    }

    @NonNull
    private Cursor getPreferencesCursor(@Nullable String preference) {
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"key", "value"});

        ContentValues preferences = getPreferences(preference);
        for (String key : preferences.keySet())
            matrixCursor.addRow(new Object[]{key, preferences.get(key)});

        return matrixCursor;
    }

    @NonNull
    public Cursor getPreferences() {
        return getPreferencesCursor(null);
    }

    @NonNull
    public Cursor getPreference(@Nullable String preference) {
        return getPreferencesCursor(preference);
    }

    @SuppressLint("CommitPrefEdits")
    public int setPreferences(@Nullable ContentValues preferences,
                              @Nullable String preference) {
        if (preferences == null || preferences.size() == 0) return -1;

        UtilsLog.d(TAG, "setPreferences", "preference == " + preference);

        if (TextUtils.isEmpty(preference)) {
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            try {
                Object value;

                for (String key : preferences.keySet()) {
                    value = preferences.get(key);

                    if (value instanceof String) editor.putString(key, (String) value);
                    else if (value instanceof Long) editor.putLong(key, (Long) value);
                    else if (value instanceof Integer) editor.putInt(key, (Integer) value);
                    else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
                    else if (value instanceof Float) editor.putFloat(key, (Float) value);
                    else
                        UtilsLog.d(TAG, "setPreferences",
                                "unhandled class == " + value.getClass().getSimpleName());
                }
            } finally {
                editor.commit();
                mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
            }

            return preferences.size();
        } else {
            if (preference.equals(keys.changed)) {
                putChanged(preferences.getAsBoolean(preference));
            } else if (preference.equals(keys.databaseRevision) ||
                    preference.equals(keys.preferencesRevision)) {
                putRevision(preference, preferences.getAsInteger(preference));
            } else if (preference.equals(keys.databaseFullSync)) {
                putFullSync(preferences.getAsBoolean(preference));
            } else if (preference.equals(keys.lastSyncDateTime)) {
                putLastSyncDateTime(preferences.getAsLong(preference));
            } else if (preference.equals(keys.lastSyncHasError)) {
                putLastSyncHasError(preferences.getAsBoolean(preference));
            } else {
                UtilsLog.d(TAG, "setPreferences", "unhandled preference == " + preference);
            }

            return 1;
        }
    }

    @PreferenceType
    public int getPreferenceType(@NonNull String key) {
        if (key.equals(keys.consumption) || key.equals(keys.season))
            return PREFERENCE_TYPE_INT;

        if (key.equals(keys.filterDateFrom) || key.equals(keys.filterDateTo) ||
                key.equals(keys.mapCenterLatitude) || key.equals(keys.mapCenterLongitude))
            return PREFERENCE_TYPE_LONG;

        return PREFERENCE_TYPE_STRING;
    }
}