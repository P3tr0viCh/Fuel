package ru.p3tr0vich.fuel;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
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

    public static final String PREF_REVISION = "revision";
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

    private static OnPreferencesChangedListener mOnPreferencesChangedListener = null;

    interface OnPreferencesChangedListener {
        void onPreferencesChanged();
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
        return !key.equals(PREF_CHANGED) && !key.equals(PREF_REVISION) && !key.equals(PREF_LAST_SYNC) &&
                !key.equals(sContext.getString(R.string.pref_sync_enabled));
    }

    public static void registerOnPreferencesChangedListener(OnPreferencesChangedListener onPreferencesChangedListener) {
        mOnPreferencesChangedListener = onPreferencesChangedListener;
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

        if (mOnPreferencesChangedListener != null && changed)
            mOnPreferencesChangedListener.onPreferencesChanged();
    }

    public static boolean isSyncEnabled() {
        return sSharedPreferences.getBoolean(sContext.getString(R.string.pref_sync_enabled), false);
    }

    private static int getRevision() {
        return sSharedPreferences.getInt(PREF_REVISION, -1);
    }

    private static void putRevision(final int revision) {
        sSharedPreferences
                .edit()
                .putInt(PREF_REVISION, revision)
                .apply();

        UtilsLog.d(TAG, "putRevision", "revision == " + revision);
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

    public static Date getFilterDateFrom() {
        String result = getString(sContext.getString(R.string.pref_filter_date_from));
        if (result.equals("")) return new Date();
        return Functions.sqlDateToDate(result);
    }

    public static Date getFilterDateTo() {
        String result = getString(sContext.getString(R.string.pref_filter_date_to));
        if (result.equals("")) return new Date();
        return Functions.sqlDateToDate(result);
    }

    public static void putFilterDate(final Date dateFrom, final Date dateTo) {
        sSharedPreferences
                .edit()
                .putString(sContext.getString(R.string.pref_filter_date_from), Functions.dateToSQLite(dateFrom))
                .putString(sContext.getString(R.string.pref_filter_date_to), Functions.dateToSQLite(dateTo))
                .apply();
    }

    public static float getDefaultCost() {
        return Functions.textToFloat(getString(sContext.getString(R.string.pref_def_cost)));
    }

    public static float getDefaultVolume() {
        return Functions.textToFloat(getString(sContext.getString(R.string.pref_def_volume)));
    }

    public static float getLastTotal() {
        // TODO // FIXME: 23.12.2015 remove getLong
        try {
            return Functions.textToFloat(getString(sContext.getString(R.string.pref_last_total)));
        } catch (Exception e) {
            UtilsLog.d(TAG, "getLastTotal", "Exception");
            return sSharedPreferences.getLong(sContext.getString(R.string.pref_last_total), 0);
        }
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

        result[0][0] = Functions.textToFloat(getString(sContext.getString(R.string.pref_summer_city)));
        result[0][1] = Functions.textToFloat(getString(sContext.getString(R.string.pref_summer_highway)));
        result[0][2] = Functions.textToFloat(getString(sContext.getString(R.string.pref_summer_mixed)));
        result[1][0] = Functions.textToFloat(getString(sContext.getString(R.string.pref_winter_city)));
        result[1][1] = Functions.textToFloat(getString(sContext.getString(R.string.pref_winter_highway)));
        result[1][2] = Functions.textToFloat(getString(sContext.getString(R.string.pref_winter_mixed)));

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
    public static String getString(final String key) {
        return sSharedPreferences.getString(key, "");
    }

    @NonNull
    public static ContentValues getPreferences(@Nullable String preference) {

//        sSharedPreferences.edit()
//                .remove("Yandex_Maps")
//                .remove("chb2")
//                .remove("wifi enabled")
//                .remove("last time")
//                .remove("sync")
//                .remove("prefer wifi")
//                .remove("last sync time")
//                .apply();

        ContentValues result = new ContentValues();

        if (TextUtils.isEmpty(preference)) {
            Map<String, ?> map = sSharedPreferences.getAll();

            Object value;

            for (String key : map.keySet())
                if (isSyncKey(key)) {

                    value = map.get(key);

                    if (value instanceof Integer) result.put(key, (Integer) value);
                    else if (value instanceof Long) result.put(key, (Long) value);
                    else if (value instanceof String) result.put(key, (String) value);
                    else if (value instanceof Boolean) result.put(key, (Boolean) value);
                    else
                        throw new ClassCastException("Unhandled class " + value.getClass().getSimpleName());
                }
        } else
            switch (preference) {
                case PREF_CHANGED:
                    result.put(preference, isChanged());
                    break;
                case PREF_REVISION:
                    result.put(preference, getRevision());
                    break;
                case PREF_LAST_SYNC:
                    result.put(preference, getLastSync());
                    break;
            }

//        for (String key : result.keySet())
//            Functions.logD("PreferenceManagerFuel -- getPreferences: key == " + key
//                    + ", value == " + result.getAsString(key));

        return result;
    }

    @SuppressLint("CommitPrefEdits")
    public static void setPreferences(@Nullable ContentValues preferences,
                                      @Nullable String preference) {
        if (preferences == null || preferences.size() == 0) return;

        if (TextUtils.isEmpty(preference)) {
            sSharedPreferences.unregisterOnSharedPreferenceChangeListener(sPreferenceChangeListener);

            SharedPreferences.Editor editor = sSharedPreferences.edit();

            Object value;

            for (String key : preferences.keySet()) {
                value = preferences.get(key);

                if (value instanceof Integer) editor.putInt(key, (Integer) value);
                else if (value instanceof Long) editor.putLong(key, (Long) value);
                else if (value instanceof String) editor.putString(key, (String) value);
                else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
                else
                    throw new ClassCastException("Unhandled class " + value.getClass().getSimpleName());
            }

            editor.commit();

            sSharedPreferences.registerOnSharedPreferenceChangeListener(sPreferenceChangeListener);
        } else {
            switch (preference) {
                case PREF_CHANGED:
                    putChanged(preferences.getAsBoolean(preference));
                    break;
                case PREF_REVISION:
                    putRevision(preferences.getAsInteger(preference));
                    break;
                case PREF_LAST_SYNC:
                    putLastSync(preferences.getAsString(preference));
                    break;
            }
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