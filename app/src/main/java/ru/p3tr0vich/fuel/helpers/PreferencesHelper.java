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

    /**
     * Центр карты по умолчанию.
     */
    public static final String DEFAULT_MAP_CENTER_TEXT = "Москва, Кремль";
    /**
     * Широта центра карты по умолчанию.
     */
    public static final double DEFAULT_MAP_CENTER_LATITUDE = 55.752023;
    /**
     * Долгота центра карты по умолчанию.
     */
    public static final double DEFAULT_MAP_CENTER_LONGITUDE = 37.617499;

    private final Context mContext; // == ApplicationContext
    private final SharedPreferences mSharedPreferences;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PREFERENCE_TYPE_STRING, PREFERENCE_TYPE_INT, PREFERENCE_TYPE_LONG})
    public @interface PreferenceType {
    }

    public static final int PREFERENCE_TYPE_STRING = 0;
    public static final int PREFERENCE_TYPE_INT = 1;
    public static final int PREFERENCE_TYPE_LONG = 2;

    public static class Keys {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({UNKNOWN, PRICE, DEFAULT_COST, DEFAULT_VOLUME,
                MAP_CENTER_TEXT, MAP_CENTER_LATITUDE, MAP_CENTER_LONGITUDE,
                SYNC, SYNC_ENABLED, SYNC_YANDEX_DISK,
                SMS, SMS_ENABLED, SMS_ADDRESS, SMS_TEXT, SMS_TEXT_PATTERN,
                DATABASE_REVISION, PREFERENCES_REVISION, CHANGED,
                LAST_SYNC_DATE_TIME, LAST_SYNC_HAS_ERROR, DATABASE_FULL_SYNC,
                FILTER_DATE_FROM, FILTER_DATE_TO,
                DISTANCE, COST, VOLUME,
                CONSUMPTION, SEASON})
        public @interface KeyAsInt {
        }

        public static final int UNKNOWN = -1;

        public final String price;
        public static final int PRICE = R.string.pref_key_price;

        public final String defaultCost;
        public static final int DEFAULT_COST = R.string.pref_key_def_cost;
        public final String defaultVolume;
        public static final int DEFAULT_VOLUME = R.string.pref_key_def_volume;

        public final String mapCenterText;
        public static final int MAP_CENTER_TEXT = R.string.pref_key_map_center_text;
        public final String mapCenterLatitude;
        public static final int MAP_CENTER_LATITUDE = R.string.pref_key_map_center_latitude;
        public final String mapCenterLongitude;
        public static final int MAP_CENTER_LONGITUDE = R.string.pref_key_map_center_longitude;

        public final String sync;
        public static final int SYNC = R.string.pref_key_sync;
        public final String syncEnabled;
        public static final int SYNC_ENABLED = R.string.pref_key_sync_enabled;
        public final String syncYandexDisk;
        public static final int SYNC_YANDEX_DISK = R.string.pref_key_sync_yandex_disk;

        public final String sms;
        public static final int SMS = R.string.pref_key_sms;
        public final String smsEnabled;
        public static final int SMS_ENABLED = R.string.pref_key_sms_enabled;
        public final String smsAddress;
        public static final int SMS_ADDRESS = R.string.pref_key_sms_address;
        public final String smsText;
        public static final int SMS_TEXT = R.string.pref_key_sms_text;
        public final String smsTextPattern;
        public static final int SMS_TEXT_PATTERN = R.string.pref_key_sms_text_pattern;

        public final String databaseRevision;
        public static final int DATABASE_REVISION = R.string.pref_key_database_revision;
        public final String preferencesRevision;
        public static final int PREFERENCES_REVISION = R.string.pref_key_preferences_revision;
        public final String changed;
        public static final int CHANGED = R.string.pref_key_changed;
        public final String lastSyncDateTime;
        public static final int LAST_SYNC_DATE_TIME = R.string.pref_key_last_sync_date_time;
        public final String lastSyncHasError;
        public static final int LAST_SYNC_HAS_ERROR = R.string.pref_key_last_sync_has_error;
        public final String databaseFullSync;
        public static final int DATABASE_FULL_SYNC = R.string.pref_key_database_full_sync;

        public final String filterDateFrom;
        public static final int FILTER_DATE_FROM = R.string.pref_key_filter_date_from;
        public final String filterDateTo;
        public static final int FILTER_DATE_TO = R.string.pref_key_filter_date_to;

        public final String distance;
        public static final int DISTANCE = R.string.pref_key_distance;
        public final String cost;
        public static final int COST = R.string.pref_key_cost;
        public final String volume;
        public static final int VOLUME = R.string.pref_key_volume;

        public final String consumption;
        public static final int CONSUMPTION = R.string.pref_key_consumption;
        public final String season;
        public static final int SEASON = R.string.pref_key_season;

        private Keys(@NonNull Context context) {
            price = context.getString(R.string.pref_key_price);

            defaultCost = context.getString(R.string.pref_key_def_cost);
            defaultVolume = context.getString(R.string.pref_key_def_volume);

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

            consumption = context.getString(R.string.pref_key_consumption);
            season = context.getString(R.string.pref_key_season);
        }

        @KeyAsInt
        public int getAsInt(@Nullable String key) {
            if (price.equals(key)) return PRICE;
            else if (defaultCost.equals(key)) return DEFAULT_COST;
            else if (defaultVolume.equals(key)) return DEFAULT_VOLUME;
            else if (mapCenterText.equals(key)) return MAP_CENTER_TEXT;
            else if (mapCenterLatitude.equals(key)) return MAP_CENTER_LATITUDE;
            else if (mapCenterLongitude.equals(key)) return MAP_CENTER_LONGITUDE;
            else if (sync.equals(key)) return SYNC;
            else if (syncEnabled.equals(key)) return SYNC_ENABLED;
            else if (syncYandexDisk.equals(key)) return SYNC_YANDEX_DISK;
            else if (sms.equals(key)) return SMS;
            else if (smsEnabled.equals(key)) return SMS_ENABLED;
            else if (smsAddress.equals(key)) return SMS_ADDRESS;
            else if (smsText.equals(key)) return SMS_TEXT;
            else if (smsTextPattern.equals(key)) return SMS_TEXT_PATTERN;
            else if (databaseRevision.equals(key)) return DATABASE_REVISION;
            else if (preferencesRevision.equals(key)) return PREFERENCES_REVISION;
            else if (changed.equals(key)) return CHANGED;
            else if (lastSyncDateTime.equals(key)) return LAST_SYNC_DATE_TIME;
            else if (lastSyncHasError.equals(key)) return LAST_SYNC_HAS_ERROR;
            else if (databaseFullSync.equals(key)) return DATABASE_FULL_SYNC;
            else if (filterDateFrom.equals(key)) return FILTER_DATE_FROM;
            else if (filterDateTo.equals(key)) return FILTER_DATE_TO;
            else if (distance.equals(key)) return DISTANCE;
            else if (cost.equals(key)) return COST;
            else if (volume.equals(key)) return VOLUME;
            else if (consumption.equals(key)) return CONSUMPTION;
            else if (season.equals(key)) return SEASON;
            else return UNKNOWN;
        }

        private boolean isSyncKey(@Nullable String key) {
            switch (getAsInt(key)) {
                case SYNC_ENABLED:
                case SMS_ENABLED:
                case CHANGED:
                case DATABASE_REVISION:
                case PREFERENCES_REVISION:
                case LAST_SYNC_DATE_TIME:
                case LAST_SYNC_HAS_ERROR:
                case DATABASE_FULL_SYNC:
                default:
                    return false;

                case CONSUMPTION:
                case COST:
                case DEFAULT_COST:
                case DEFAULT_VOLUME:
                case DISTANCE:
                case FILTER_DATE_FROM:
                case FILTER_DATE_TO:
                case MAP_CENTER_LATITUDE:
                case MAP_CENTER_LONGITUDE:
                case MAP_CENTER_TEXT:
                case PRICE:
                case SEASON:
                case SMS_ADDRESS:
                case SMS_TEXT_PATTERN:
                case SMS_TEXT:
                case SMS:
                case SYNC_YANDEX_DISK:
                case SYNC:
                case UNKNOWN:
                case VOLUME:
                    return true;
            }
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

        if (keys.isSyncKey(key)) putChanged(true);
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
        return UtilsFormat.stringToFloat(getString(keys.defaultCost));
    }

    public float getDefaultVolume() {
        return UtilsFormat.stringToFloat(getString(keys.defaultVolume));
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
    public String getPriceAsString() {
        return getString(keys.price);
    }

    public float getPrice() {
        return UtilsFormat.stringToFloat(getPriceAsString());
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

    public void putCalc(String distance, String cost, String volume, int cons, int season) {
        mSharedPreferences
                .edit()
                .putString(keys.distance, distance)
                .putString(keys.cost, cost)
                .putString(keys.volume, volume)
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

    @SuppressLint("SwitchIntDef")
    @NonNull
    private ContentValues getPreferences(@Nullable String preference) {
        ContentValues result = new ContentValues();

        if (TextUtils.isEmpty(preference)) {
            Map<String, ?> map = mSharedPreferences.getAll();

            String key;
            Object value;

            for (Map.Entry<String, ?> entry : map.entrySet()) {
                key = entry.getKey();

                if (keys.isSyncKey(key)) {
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
            switch (keys.getAsInt(preference)) {
                case Keys.CHANGED:
                    result.put(preference, isChanged());
                    break;
                case Keys.DATABASE_FULL_SYNC:
                    result.put(preference, isFullSync());
                    break;
                case Keys.DATABASE_REVISION:
                case Keys.PREFERENCES_REVISION:
                    result.put(preference, getRevision(preference));
                    break;
                case Keys.LAST_SYNC_DATE_TIME:
                    result.put(preference, getLastSyncDateTime());
                    break;
                case Keys.LAST_SYNC_HAS_ERROR:
                    result.put(preference, getLastSyncHasError());
                    break;
                default:
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

    @SuppressLint({"CommitPrefEdits", "SwitchIntDef"})
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
            switch (keys.getAsInt(preference)) {
                case Keys.CHANGED:
                    putChanged(preferences.getAsBoolean(preference));
                    break;
                case Keys.DATABASE_REVISION:
                case Keys.PREFERENCES_REVISION:
                    putRevision(preference, preferences.getAsInteger(preference));
                    break;
                case Keys.DATABASE_FULL_SYNC:
                    putFullSync(preferences.getAsBoolean(preference));
                    break;
                case Keys.LAST_SYNC_DATE_TIME:
                    putLastSyncDateTime(preferences.getAsLong(preference));
                    break;
                case Keys.LAST_SYNC_HAS_ERROR:
                    putLastSyncHasError(preferences.getAsBoolean(preference));
                    break;
                default:
                    UtilsLog.d(TAG, "setPreferences", "unhandled preference == " + preference);
            }

            return 1;
        }
    }

    @SuppressLint("SwitchIntDef")
    @PreferenceType
    public int getPreferenceType(@NonNull String key) {
        switch (keys.getAsInt(key)) {
            case Keys.CONSUMPTION:
            case Keys.SEASON:
                return PREFERENCE_TYPE_INT;
            case Keys.FILTER_DATE_FROM:
            case Keys.FILTER_DATE_TO:
            case Keys.MAP_CENTER_LATITUDE:
            case Keys.MAP_CENTER_LONGITUDE:
                return PREFERENCE_TYPE_LONG;
            default:
                return PREFERENCE_TYPE_STRING;
        }
    }
}