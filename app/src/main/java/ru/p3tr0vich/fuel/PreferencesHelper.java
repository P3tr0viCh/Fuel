package ru.p3tr0vich.fuel;

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

class PreferencesHelper {

    private static final String TAG = "PreferencesHelper";

    public static final long SYNC_NONE = Long.MIN_VALUE;

    public static final String PREF_DATABASE_REVISION = "database revision";
    public static final String PREF_PREFERENCES_REVISION = "preferences revision";

    public static final String PREF_CHANGED = "changed";
    public static final String PREF_LAST_SYNC_DATE_TIME = "last sync date time";
    public static final String PREF_LAST_SYNC_HAS_ERROR = "last sync has error";
    public static final String PREF_DATABASE_FULL_SYNC = "database full sync";

    private static final String PREF_FILTER_DATE_FROM = "filter date from";
    private static final String PREF_FILTER_DATE_TO = "filter date to";

    private static final String PREF_DISTANCE = "distance";
    private static final String PREF_COST = "cost";
    private static final String PREF_VOLUME = "volume";
    private static final String PREF_PRICE = "price";

    private static final String PREF_CONS = "consumption";
    private static final String PREF_SEASON = "season";

    // @preferences
    public static final String PREF_MAP_CENTER_TEXT = "map center text";

    private static final String PREF_MAP_CENTER_LATITUDE = "map center latitude";
    private static final String PREF_MAP_CENTER_LONGITUDE = "map center longitude";

    public static final String DEFAULT_MAP_CENTER_TEXT = "Москва, Кремль";
    public static final double DEFAULT_MAP_CENTER_LATITUDE = 55.752023;  // Широта
    public static final double DEFAULT_MAP_CENTER_LONGITUDE = 37.617499; // Долгота

    // @preferences
    public static final String PREF_SYNC = "key_sync";
    public static final String PREF_SYNC_ENABLED = "sync enabled";
    public static final String PREF_SYNC_YANDEX_DISK = "key_sync_yandex_disk";

    public static final String PREF_SMS = "key_sms";
    public static final String PREF_SMS_ENABLED = "sms enabled";
    public static final String PREF_SMS_ADDRESS = "sms address";
    public static final String PREF_SMS_TEXT = "sms text";

    @SuppressWarnings("WeakerAccess")
    // private - поле удаляется сборщиком мусора
    static SharedPreferences.OnSharedPreferenceChangeListener sPreferenceChangeListener; // TODO: this
    private static Context sContext;
    private static SharedPreferences sSharedPreferences;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PREFERENCE_TYPE_STRING, PREFERENCE_TYPE_INT, PREFERENCE_TYPE_LONG})
    public @interface PreferenceType {
    }

    public static final int PREFERENCE_TYPE_STRING = 0;
    public static final int PREFERENCE_TYPE_INT = 1;
    public static final int PREFERENCE_TYPE_LONG = 2;

    private PreferencesHelper() {

    }

    public static void init(@NonNull Context context) {
        sContext = context;

        sSharedPreferences = PreferenceManager.getDefaultSharedPreferences(sContext);

//        sSharedPreferences.edit()
//                .remove("default cost")
//                .apply();

        sPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                UtilsLog.d(TAG, "onSharedPreferenceChanged", "key == " + key);

                if (isSyncKey(key)) putChanged(true);
            }
        };
        sSharedPreferences.registerOnSharedPreferenceChangeListener(sPreferenceChangeListener);
    }

    private static boolean isSyncKey(String key) {
        switch (key) {
            case PREF_CHANGED:
            case PREF_DATABASE_REVISION:
            case PREF_PREFERENCES_REVISION:
            case PREF_LAST_SYNC_DATE_TIME:
            case PREF_LAST_SYNC_HAS_ERROR:
            case PREF_DATABASE_FULL_SYNC:
            case PREF_SYNC_ENABLED:
            case PREF_SMS_ENABLED:
                return false;
            default:
                return true;
        }
    }

    private static boolean isChanged() {
        return sSharedPreferences.getBoolean(PREF_CHANGED, true);
    }

    private static void putChanged(final boolean changed) {
        sSharedPreferences
                .edit()
                .putBoolean(PREF_CHANGED, changed)
                .apply();

        UtilsLog.d(TAG, "putChanged", "changed == " + changed);

        if (changed)
            sContext.getContentResolver().notifyChange(ContentProviderHelper.URI_PREFERENCES, null, false);
    }

    public static boolean isSyncEnabled() {
        return sSharedPreferences.getBoolean(PREF_SYNC_ENABLED, false);
    }

    public static boolean isSMSEnabled() {
        return sSharedPreferences.getBoolean(PREF_SMS_ENABLED, false);
    }

    @NonNull
    public static String getSMSAddress() {
        return getString(PREF_SMS_ADDRESS);
    }

    public static void putSMSAddress(final String address) {
        sSharedPreferences
                .edit()
                .putString(PREF_SMS_ADDRESS, address)
                .apply();
    }

    @NonNull
    public static String getSMSText() {
        return getString(PREF_SMS_TEXT);
    }

    private static int getRevision(String keyRevision) {
        return sSharedPreferences.getInt(keyRevision, -1);
    }

    private static void putRevision(final String keyRevision, final int revision) {
        sSharedPreferences
                .edit()
                .putInt(keyRevision, revision)
                .apply();

        UtilsLog.d(TAG, "putRevision", keyRevision + " == " + revision);
    }

    private static boolean isFullSync() {
        return sSharedPreferences.getBoolean(PREF_DATABASE_FULL_SYNC, false);
    }

    public static void putFullSync(final boolean fullSync) {
        sSharedPreferences
                .edit()
                .putBoolean(PREF_DATABASE_FULL_SYNC, fullSync)
                .apply();
    }

    public static long getLastSyncDateTime() {
        return sSharedPreferences.getLong(PREF_LAST_SYNC_DATE_TIME, SYNC_NONE);
    }

    public static boolean getLastSyncHasError() {
        return sSharedPreferences.getBoolean(PREF_LAST_SYNC_HAS_ERROR, false);
    }

    private static void putLastSyncDateTime(final long dateTime) {
        sSharedPreferences
                .edit()
                .putLong(PREF_LAST_SYNC_DATE_TIME, dateTime)
                .apply();
    }

    private static void putLastSyncHasError(final boolean hasError) {
        sSharedPreferences
                .edit()
                .putBoolean(PREF_LAST_SYNC_HAS_ERROR, hasError)
                .apply();
    }

    public static long getFilterDateFrom() {
        return sSharedPreferences.getLong(PREF_FILTER_DATE_FROM, System.currentTimeMillis());
    }

    public static long getFilterDateTo() {
        return sSharedPreferences.getLong(PREF_FILTER_DATE_TO, System.currentTimeMillis());
    }

    public static void putFilterDate(final long dateFrom, final long dateTo) {
        sSharedPreferences
                .edit()
                .putLong(PREF_FILTER_DATE_FROM, dateFrom)
                .putLong(PREF_FILTER_DATE_TO, dateTo)
                .apply();
    }

    public static float getDefaultCost() {
        return UtilsFormat.stringToFloat(getString(sContext.getString(R.string.pref_def_cost)));
    }

    public static float getDefaultVolume() {
        return UtilsFormat.stringToFloat(getString(sContext.getString(R.string.pref_def_volume)));
    }

    public static float getLastTotal() {
        return UtilsFormat.stringToFloat(getString(sContext.getString(R.string.pref_last_total)));
    }

    public static void putLastTotal(final float lastTotal) {
        sSharedPreferences
                .edit()
                .putString(sContext.getString(R.string.pref_last_total), String.valueOf(lastTotal))
                .apply();
    }

    @NonNull
    public static String getCalcDistance() {
        return getString(PREF_DISTANCE);
    }

    @NonNull
    public static String getCalcCost() {
        return getString(PREF_COST);
    }

    @NonNull
    public static String getCalcVolume() {
        return getString(PREF_VOLUME);
    }

    @NonNull
    public static String getCalcPrice() {
        return getString(PREF_PRICE);
    }

    public static float[][] getCalcCons() {

        float[][] result = {{0, 0, 0}, {0, 0, 0}};

        result[0][0] = UtilsFormat.stringToFloat(getString(sContext.getString(R.string.pref_summer_city)));
        result[0][1] = UtilsFormat.stringToFloat(getString(sContext.getString(R.string.pref_summer_highway)));
        result[0][2] = UtilsFormat.stringToFloat(getString(sContext.getString(R.string.pref_summer_mixed)));
        result[1][0] = UtilsFormat.stringToFloat(getString(sContext.getString(R.string.pref_winter_city)));
        result[1][1] = UtilsFormat.stringToFloat(getString(sContext.getString(R.string.pref_winter_highway)));
        result[1][2] = UtilsFormat.stringToFloat(getString(sContext.getString(R.string.pref_winter_mixed)));

        return result;
    }

    public static int getCalcSelectedCons() {
        return sSharedPreferences.getInt(PREF_CONS, 0);
    }

    public static int getCalcSelectedSeason() {
        return sSharedPreferences.getInt(PREF_SEASON, 0);
    }

    public static void putCalc(final String distance, final String cost, final String volume,
                               final String price, final int cons, final int season) {
        sSharedPreferences
                .edit()
                .putString(PREF_DISTANCE, distance)
                .putString(PREF_COST, cost)
                .putString(PREF_VOLUME, volume)
                .putString(PREF_PRICE, price)
                .putInt(PREF_CONS, cons)
                .putInt(PREF_SEASON, season)
                .apply();
    }

    @NonNull
    public static String getMapCenterText() {
        return getString(PREF_MAP_CENTER_TEXT, DEFAULT_MAP_CENTER_TEXT);
    }

    public static double getMapCenterLatitude() {
        return Double.longBitsToDouble(sSharedPreferences.getLong(
                PREF_MAP_CENTER_LATITUDE,
                Double.doubleToLongBits(DEFAULT_MAP_CENTER_LATITUDE)));
    }

    public static double getMapCenterLongitude() {
        return Double.longBitsToDouble(sSharedPreferences.getLong(
                PREF_MAP_CENTER_LONGITUDE,
                Double.doubleToLongBits(DEFAULT_MAP_CENTER_LONGITUDE)));
    }

    public static void putMapCenter(final String text, final double latitude, final double longitude) {
        sSharedPreferences
                .edit()
                .putString(PREF_MAP_CENTER_TEXT, text)
                .putLong(PREF_MAP_CENTER_LATITUDE,
                        Double.doubleToRawLongBits(latitude))
                .putLong(PREF_MAP_CENTER_LONGITUDE,
                        Double.doubleToRawLongBits(longitude))
                .apply();
    }

    @NonNull
    private static String getString(final String key, final String defValue) {
        return sSharedPreferences.getString(key, defValue);
    }

    @NonNull
    public static String getString(final String key) {
        return getString(key, "");
    }

    @NonNull
    private static ContentValues getPreferences(@Nullable String preference) {
        ContentValues result = new ContentValues();

        if (TextUtils.isEmpty(preference)) {
            Map<String, ?> map = sSharedPreferences.getAll();

            Object value;

            for (String key : map.keySet())
                if (isSyncKey(key)) {

                    value = map.get(key);

                    if (value instanceof String) result.put(key, (String) value);
                    else if (value instanceof Long) result.put(key, (Long) value);
                    else if (value instanceof Integer) result.put(key, (Integer) value);
                    else if (value instanceof Boolean) result.put(key, (Boolean) value);
                    else if (value instanceof Float) result.put(key, (Float) value);
                    else
                        UtilsLog.d(TAG, "getPreferences",
                                "unhandled class == " + value.getClass().getSimpleName());
                }
        } else
            switch (preference) {
                case PREF_CHANGED:
                    result.put(preference, isChanged());
                    break;
                case PREF_DATABASE_FULL_SYNC:
                    result.put(preference, isFullSync());
                    break;
                case PREF_DATABASE_REVISION:
                case PREF_PREFERENCES_REVISION:
                    result.put(preference, getRevision(preference));
                    break;
                case PREF_LAST_SYNC_DATE_TIME:
                    result.put(preference, getLastSyncDateTime());
                    break;
                case PREF_LAST_SYNC_HAS_ERROR:
                    result.put(preference, getLastSyncHasError());
                    break;
                default:
                    UtilsLog.d(TAG, "getPreferences", "unhandled preference == " + preference);
            }

//        for (String key : result.keySet())
//            UtilsLog.d(TAG, "getPreferences", "key == " + key + ", value == " + result.getAsString(key));

        return result;
    }

    @NonNull
    private static Cursor getPreferencesCursor(@Nullable String preference) {
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"key", "value"});

        ContentValues preferences = PreferencesHelper.getPreferences(preference);
        for (String key : preferences.keySet())
            matrixCursor.addRow(new Object[]{key, preferences.get(key)});

        return matrixCursor;
    }

    @NonNull
    public static Cursor getPreferences() {
        return getPreferencesCursor(null);
    }

    @NonNull
    public static Cursor getPreference(@Nullable String preference) {
        return getPreferencesCursor(preference);
    }

    @SuppressLint("CommitPrefEdits")
    public static int setPreferences(@Nullable ContentValues preferences,
                                     @Nullable String preference) {
        if (preferences == null || preferences.size() == 0) return -1;

        UtilsLog.d(TAG, "setPreferences", "preference == " + preference);

        if (TextUtils.isEmpty(preference)) {
            sSharedPreferences.unregisterOnSharedPreferenceChangeListener(sPreferenceChangeListener);
            SharedPreferences.Editor editor = sSharedPreferences.edit();
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
                sSharedPreferences.registerOnSharedPreferenceChangeListener(sPreferenceChangeListener);
            }

            return preferences.size();
        } else {
            switch (preference) {
                case PREF_CHANGED:
                    putChanged(preferences.getAsBoolean(preference));
                    break;
                case PREF_DATABASE_REVISION:
                case PREF_PREFERENCES_REVISION:
                    putRevision(preference, preferences.getAsInteger(preference));
                    break;
                case PREF_DATABASE_FULL_SYNC:
                    putFullSync(preferences.getAsBoolean(preference));
                    break;
                case PREF_LAST_SYNC_DATE_TIME:
                    putLastSyncDateTime(preferences.getAsLong(preference));
                    break;
                case PREF_LAST_SYNC_HAS_ERROR:
                    putLastSyncHasError(preferences.getAsBoolean(preference));
                    break;
                default:
                    UtilsLog.d(TAG, "setPreferences", "unhandled preference == " + preference);
            }

            return 1;
        }
    }

    @PreferenceType
    public static int getPreferenceType(@NonNull String key) {
        switch (key) {
            case PREF_CONS:
            case PREF_SEASON:
                return PREFERENCE_TYPE_INT;
            case PREF_FILTER_DATE_FROM:
            case PREF_FILTER_DATE_TO:
            case PREF_MAP_CENTER_LATITUDE:
            case PREF_MAP_CENTER_LONGITUDE:
                return PREFERENCE_TYPE_LONG;
            default:
                return PREFERENCE_TYPE_STRING;
        }
    }
}