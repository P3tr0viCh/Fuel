package ru.p3tr0vich.fuel;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FuelingPreferenceManager {

    private static final String PREF_REVISION = "revision";
    private static final String PREF_CHANGED = "changed";
    private static final String PREF_LAST_SYNC = "last sync time";

    private static final String PREF_DISTANCE = "distance";
    private static final String PREF_COST = "cost";
    private static final String PREF_VOLUME = "volume";
    private static final String PREF_PRICE = "price";

    private static final String PREF_CONS = "consumption";
    private static final String PREF_SEASON = "season";

    static SharedPreferences.OnSharedPreferenceChangeListener sPreferenceChangeListener;
    private static Context sContext;
    private static SharedPreferences sSharedPreferences;

    public static void init(Context context) {
        sContext = context;
        sSharedPreferences = PreferenceManager.getDefaultSharedPreferences(sContext);
        sPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//                Functions.logD("FuelingPreferenceManager -- onSharedPreferenceChanged: key == " + key);

                if (!(key.equals(PREF_CHANGED) ||
                        key.equals(PREF_LAST_SYNC) ||
                        key.equals(PREF_REVISION) ||
                        key.equals(Functions.sApplicationContext.getString(R.string.pref_sync_enabled))))
                    putChanged(true);
            }
        };
        sSharedPreferences.registerOnSharedPreferenceChangeListener(sPreferenceChangeListener);
    }

    public static boolean isChanged() {
        return sSharedPreferences.getBoolean(PREF_CHANGED, false);
    }

    public static void putChanged(boolean changed) {
        sSharedPreferences
                .edit()
                .putBoolean(PREF_CHANGED, changed)
                .apply();

        Functions.logD("FuelingPreferenceManager -- putChanged: changed == " + changed);
    }

    public static String getLastSync() {
        long date = sSharedPreferences.getLong(PREF_LAST_SYNC, Long.MIN_VALUE);
        return date != Long.MIN_VALUE ? Functions.dateTimeToString(new Date(date)) : "";
    }

    public static void putLastSync(Date dateTime) {
        sSharedPreferences
                .edit()
                .putLong(PREF_LAST_SYNC, dateTime.getTime())
                .apply();
    }

    public static boolean isSyncEnabled() {
        return sSharedPreferences.getBoolean(sContext.getString(R.string.pref_sync_enabled), false);
    }

    public static int getRevision() {
        return sSharedPreferences.getInt(PREF_REVISION, -1);
    }

    public static void putRevision(int revision) {
        sSharedPreferences
                .edit()
                .putInt(PREF_REVISION, revision)
                .apply();

        Functions.logD("FuelingPreferenceManager -- putRevision: revision == " + revision);
    }

    public static Date getFilterDateFrom() {
        String result = sSharedPreferences.getString(sContext.getString(R.string.pref_filter_date_from), "");
        if (result.equals("")) return new Date();
        return Functions.sqlDateToDate(result);
    }

    public static Date getFilterDateTo() {
        String result = sSharedPreferences.getString(sContext.getString(R.string.pref_filter_date_to), "");
        if (result.equals("")) return new Date();
        return Functions.sqlDateToDate(result);
    }

    public static void putFilterDate(Date dateFrom, Date dateTo) {
        sSharedPreferences
                .edit()
                .putString(sContext.getString(R.string.pref_filter_date_from), Functions.dateToSQLite(dateFrom))
                .putString(sContext.getString(R.string.pref_filter_date_to), Functions.dateToSQLite(dateTo))
                .apply();
    }

    public static float getDefaultCost() {
        return Functions.textToFloat(sSharedPreferences.getString(sContext.getString(R.string.pref_def_cost), "0"));
    }

    public static float getDefaultVolume() {
        return Functions.textToFloat(sSharedPreferences.getString(sContext.getString(R.string.pref_def_volume), "0"));
    }

    public static float getLastTotal() {
        return sSharedPreferences.getFloat(sContext.getString(R.string.pref_last_total), 0);
    }

    public static void putLastTotal(float lastTotal) {
        sSharedPreferences
                .edit()
                .putFloat(sContext.getString(R.string.pref_last_total), lastTotal)
                .apply();
    }

    public static String getCalcDistance() {
        return sSharedPreferences.getString(PREF_DISTANCE, "");
    }

    public static String getCalcCost() {
        return sSharedPreferences.getString(PREF_COST, "");
    }

    public static String getCalcVolume() {
        return sSharedPreferences.getString(PREF_VOLUME, "");
    }

    public static String getCalcPrice() {
        return sSharedPreferences.getString(PREF_PRICE, "");
    }

    public static float[][] getCalcCons() {

        float[][] result = {{0, 0, 0}, {0, 0, 0}};

        result[0][0] = Functions.textToFloat(sSharedPreferences.getString(
                sContext.getString(R.string.pref_summer_city), "0"));
        result[0][1] = Functions.textToFloat(sSharedPreferences.getString(
                sContext.getString(R.string.pref_summer_highway), "0"));
        result[0][2] = Functions.textToFloat(sSharedPreferences.getString(
                sContext.getString(R.string.pref_summer_mixed), "0"));
        result[1][0] = Functions.textToFloat(sSharedPreferences.getString(
                sContext.getString(R.string.pref_winter_city), "0"));
        result[1][1] = Functions.textToFloat(sSharedPreferences.getString(
                sContext.getString(R.string.pref_winter_highway), "0"));
        result[1][2] = Functions.textToFloat(sSharedPreferences.getString(
                sContext.getString(R.string.pref_winter_mixed), "0"));

        return result;
    }

    public static int getCalcSelectedCons() {
        return sSharedPreferences.getInt(PREF_CONS, 0);
    }

    public static int getCalcSelectedSeason() {
        return sSharedPreferences.getInt(PREF_SEASON, 0);
    }

    public static void putCalc(String distance, String cost, String volume, String price,
                               int cons, int season) {
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

    public static String getMapCenterText() {
        return sSharedPreferences.getString(
                sContext.getString(R.string.pref_map_center_text),
                YandexMapJavascriptInterface.DEFAULT_MAP_CENTER_TEXT);
    }

    public static double getMapCenterLatitude() {
        return Double.longBitsToDouble(sSharedPreferences.getLong(
                sContext.getString(R.string.pref_map_center_latitude),
                Double.doubleToLongBits(YandexMapJavascriptInterface.DEFAULT_MAP_CENTER_LATITUDE)));
    }

    public static double getMapCenterLongitude() {
        return Double.longBitsToDouble(sSharedPreferences.getLong(
                sContext.getString(R.string.pref_map_center_longitude),
                Double.doubleToLongBits(YandexMapJavascriptInterface.DEFAULT_MAP_CENTER_LONGITUDE)));
    }

    public static void putMapCenter(String text, double latitude, double longitude) {
        sSharedPreferences
                .edit()
                .putString(sContext.getString(R.string.pref_map_center_text), text)
                .putLong(sContext.getString(R.string.pref_map_center_latitude),
                        Double.doubleToRawLongBits(latitude))
                .putLong(sContext.getString(R.string.pref_map_center_longitude),
                        Double.doubleToRawLongBits(longitude))
                .apply();
    }

    public static List<String> getPreferences() {

        String key;
        List<String> result = new ArrayList<>();

        // Стоимость по умолчанию
        key = sContext.getString(R.string.pref_def_cost);
        result.add(key + '=' + sSharedPreferences.getString(key, "0"));

        // Объём по умолчанию
        key = sContext.getString(R.string.pref_def_volume);
        result.add(key + '=' + sSharedPreferences.getString(key, "0"));

        // Последний пробег
        key = sContext.getString(R.string.pref_last_total);
        result.add(key + '=' + String.valueOf(sSharedPreferences.getFloat(key, 0)));

        // Местоположение
        key = sContext.getString(R.string.pref_map_center_text);
        result.add(key + '=' + String.valueOf(getMapCenterText()));

        // Широта
        key = sContext.getString(R.string.pref_map_center_latitude);
        result.add(key + '=' + String.valueOf(getMapCenterLatitude()));

        // Долгота
        key = sContext.getString(R.string.pref_map_center_longitude);
        result.add(key + '=' + String.valueOf(getMapCenterLongitude()));

        return result;
    }

    public static void setPreferences(List<String> preferences) {
        for (String preference : preferences)
            Functions.logD("FuelingPreferenceManager -- setPreferences: " + preference);
    }
}
