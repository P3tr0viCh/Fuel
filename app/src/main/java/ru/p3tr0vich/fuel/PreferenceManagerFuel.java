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
import java.util.Date;
import java.util.Map;

class PreferenceManagerFuel {

    private static final String TAG = "PreferenceManagerFuel";

    public static final String PREF_DATABASE_REVISION = "database revision";
    public static final String PREF_PREFERENCES_REVISION = "preferences revision";

    public static final String PREF_CHANGED = "changed";
    public static final String PREF_LAST_SYNC = "last sync";

    public static final String SYNC_ERROR = "error";

    private static final String PREF_DISTANCE = "distance";
    private static final String PREF_COST = "cost";
    private static final String PREF_VOLUME = "volume";
    private static final String PREF_PRICE = "price";

    private static final String PREF_CONS = "consumption";
    private static final String PREF_SEASON = "season";

    private static final String PREF_MAP_CENTER_LATITUDE = "map center latitude";
    private static final String PREF_MAP_CENTER_LONGITUDE = "map center longitude";

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

    private static OnPreferencesUpdatingListener mOnPreferencesUpdatingListener = null;

    interface OnPreferencesUpdatingListener {
        void onPreferencesUpdateStart();

        void onPreferencesUpdateEnd();
    }

    public static void init(Context context) {
        sContext = context;

        sSharedPreferences = PreferenceManager.getDefaultSharedPreferences(sContext);

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
        return !key.equals(PREF_CHANGED) &&
                !key.equals(PREF_DATABASE_REVISION) &&
                !key.equals(PREF_PREFERENCES_REVISION) &&
                !key.equals(PREF_LAST_SYNC) &&
                !key.equals(sContext.getString(R.string.pref_sync_enabled));
    }

    public static void registerOnPreferencesUpdatingListener(OnPreferencesUpdatingListener onPreferencesUpdatingListener) {
        mOnPreferencesUpdatingListener = onPreferencesUpdatingListener;
    }

    public static boolean isChanged() {
        return sSharedPreferences.getBoolean(PREF_CHANGED, true);
    }

    private static void putChanged(final boolean changed) {
        sSharedPreferences
                .edit()
                .putBoolean(PREF_CHANGED, changed)
                .apply();

        UtilsLog.d(TAG, "putChanged", "changed == " + changed);

        if (changed)
            sContext.getContentResolver().notifyChange(SyncProvider.URI_PREFERENCES, null, false);
    }

    public static boolean isSyncEnabled() {
        return sSharedPreferences.getBoolean(sContext.getString(R.string.pref_sync_enabled), false);
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

    @NonNull
    public static String getLastSync() {
        return getString(PREF_LAST_SYNC);
    }

    private static void putLastSync(final String dateTime) {
        sSharedPreferences
                .edit()
                .putString(PREF_LAST_SYNC, dateTime != null ? dateTime : SYNC_ERROR)
                .apply();
    }

    private static Date getFilterDate(String date) {
        Date result = new Date();
        if (!date.equals("")) result.setTime(Long.valueOf(date));
        return result;
    }

    public static Date getFilterDateFrom() {
        return getFilterDate(getString(sContext.getString(R.string.pref_filter_date_from)));
    }

    public static Date getFilterDateTo() {
        return getFilterDate(getString(sContext.getString(R.string.pref_filter_date_to)));
    }

    public static void putFilterDate(final Date dateFrom, final Date dateTo) {
        sSharedPreferences
                .edit()
                .putString(sContext.getString(R.string.pref_filter_date_from),
                        String.valueOf(dateFrom.getTime()))
                .putString(sContext.getString(R.string.pref_filter_date_to),
                        String.valueOf(dateTo.getTime()))
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
        return getString(sContext.getString(R.string.pref_map_center_text),
                YandexMapJavascriptInterface.DEFAULT_MAP_CENTER_TEXT);
    }

    public static double getMapCenterLatitude() {
        return Double.longBitsToDouble(sSharedPreferences.getLong(
                PREF_MAP_CENTER_LATITUDE,
                Double.doubleToLongBits(YandexMapJavascriptInterface.DEFAULT_MAP_CENTER_LATITUDE)));
    }

    public static double getMapCenterLongitude() {
        return Double.longBitsToDouble(sSharedPreferences.getLong(
                PREF_MAP_CENTER_LONGITUDE,
                Double.doubleToLongBits(YandexMapJavascriptInterface.DEFAULT_MAP_CENTER_LONGITUDE)));
    }

    public static void putMapCenter(final String text, final double latitude, final double longitude) {
        sSharedPreferences
                .edit()
                .putString(sContext.getString(R.string.pref_map_center_text), text)
                .putLong(PREF_MAP_CENTER_LATITUDE,
                        Double.doubleToRawLongBits(latitude))
                .putLong(PREF_MAP_CENTER_LONGITUDE,
                        Double.doubleToRawLongBits(longitude))
                .apply();
    }

    @NonNull
    public static String getString(final String key, final String defValue) {
        return sSharedPreferences.getString(key, defValue);
    }

    @NonNull
    private static String getString(final String key) {
        return sSharedPreferences.getString(key, "");
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
                case PREF_DATABASE_REVISION:
                case PREF_PREFERENCES_REVISION:
                    result.put(preference, getRevision(preference));
                    break;
                case PREF_LAST_SYNC:
                    result.put(preference, getLastSync());
                    break;
            }

//        for (String key : result.keySet())
//            UtilsLog.d(TAG, "getPreferences", "key == " + key + ", value == " + result.getAsString(key));

        return result;
    }

    @NonNull
    private static Cursor getPreferencesCursor(@Nullable String preference) {
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"key", "value"});

        ContentValues preferences = PreferenceManagerFuel.getPreferences(preference);
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

        UtilsLog.d(TAG, "setPreferences", "preference == " + preference +
                ", mOnPreferencesUpdatingListener == " + mOnPreferencesUpdatingListener);

        if (TextUtils.isEmpty(preference)) {
            sSharedPreferences.unregisterOnSharedPreferenceChangeListener(sPreferenceChangeListener);
            try {

                if (mOnPreferencesUpdatingListener != null)
                    mOnPreferencesUpdatingListener.onPreferencesUpdateStart();
                try {
                    SharedPreferences.Editor editor = sSharedPreferences.edit();

                    Object value;

                    for (String key : preferences.keySet()) {
                        value = preferences.get(key);

                        if (value instanceof String) editor.putString(key, (String) value);
                        else if (value instanceof Long) editor.putLong(key, (Long) value);
                        else if (value instanceof Integer) editor.putInt(key, (Integer) value);
                        else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
                        else if (value instanceof Float) editor.putFloat(key, (Float) value);
                        else
                            UtilsLog.d(TAG, "getPreferences",
                                    "unhandled class == " + value.getClass().getSimpleName());
                    }

                    editor.commit();

                } finally {
                    if (mOnPreferencesUpdatingListener != null)
                        mOnPreferencesUpdatingListener.onPreferencesUpdateEnd();
                }
            } finally {
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
                case PREF_LAST_SYNC:
                    putLastSync(preferences.getAsString(preference));
                    break;
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
            case PREF_MAP_CENTER_LATITUDE:
            case PREF_MAP_CENTER_LONGITUDE:
                return PREFERENCE_TYPE_LONG;
            default:
                return PREFERENCE_TYPE_STRING;
        }
    }
}