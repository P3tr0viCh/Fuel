package ru.p3tr0vich.fuel;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.webkit.JavascriptInterface;

class YandexMapJavascriptInterface {

    public static final String NAME = "YandexMapJavascriptInterface";

    public final static String DEFAULT_MAP_CENTER_TEXT = "Москва, Кремль";
    public final static double DEFAULT_MAP_CENTER_LATITUDE = 55.752023;  // Широта
    public final static double DEFAULT_MAP_CENTER_LONGITUDE = 37.617499; // Долгота

    private final ActivityYandexMap mActivityYandexMap;

    private final double mMapCenterLatitude;
    private final double mMapCenterLongitude;

    private final String mStartSearchControlPlaceholderContent;
    private final String mFinishSearchControlPlaceholderContent;
    private final String mEmptyBalloonContent; // TODO: distance

    YandexMapJavascriptInterface(ActivityYandexMap activityYandexMap) {
        mActivityYandexMap = activityYandexMap;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mActivityYandexMap);
        mMapCenterLatitude = Double.longBitsToDouble(preferences.getLong(
                mActivityYandexMap.getString(R.string.pref_map_center_latitude),
                Double.doubleToLongBits(DEFAULT_MAP_CENTER_LATITUDE)));
        mMapCenterLongitude = Double.longBitsToDouble(preferences.getLong(
                mActivityYandexMap.getString(R.string.pref_map_center_longitude),
                Double.doubleToLongBits(DEFAULT_MAP_CENTER_LONGITUDE)));

        mStartSearchControlPlaceholderContent =
                mActivityYandexMap.getString(R.string.yandex_map_start_search_control_placeholder_content);
        mFinishSearchControlPlaceholderContent =
                mActivityYandexMap.getString(R.string.yandex_map_finish_search_control_placeholder_content);
        mEmptyBalloonContent = "<h3>" +
                mActivityYandexMap.getString(R.string.yandex_map_empty_geocode) +  "</h3>";
    }

    @JavascriptInterface
    public double getMapCenterLatitude() {
        return mMapCenterLatitude;
    }

    @JavascriptInterface
    public double getMapCenterLongitude() {
        return mMapCenterLongitude;
    }

    @JavascriptInterface
    public String getStartSearchControlPlaceholderContent() {
        return mStartSearchControlPlaceholderContent;
    }

    @JavascriptInterface
    public String getFinishSearchControlPlaceholderContent() {
        return mFinishSearchControlPlaceholderContent;
    }

    @JavascriptInterface
    public String getEmptyBalloonContent() {
        return mEmptyBalloonContent;
    }

    @JavascriptInterface
    public void updateDistance(final int distance) {
        mActivityYandexMap.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityYandexMap.setDistance(distance);
            }
        });
    }

    @JavascriptInterface
    public void updateMapCenter(final String text, final String title, final String subtitle,
                                final double latitude, final double longitude) {
        mActivityYandexMap.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityYandexMap.setMapCenter(text, title, subtitle, latitude, longitude);
            }
        });
    }

    @JavascriptInterface
    public void endInit() {
        mActivityYandexMap.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityYandexMap.endInitYandexMap();
            }
        });
    }

    @JavascriptInterface
    public void errorConstructRoute() {
        mActivityYandexMap.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityYandexMap.errorConstructRoute();
            }
        });
    }
}
