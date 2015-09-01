package ru.p3tr0vich.fuel;

import android.webkit.JavascriptInterface;

class YandexMapJavascriptInterface {

    public static final String NAME = "YandexMapJavascriptInterface";

    private final ActivityYandexMap mActivityYandexMap;

    private final double mMapCenterLatitude;  // Широта
    private final double mMapCenterLongitude; // Долгота
    private final String mStartSearchControlPlaceholderContent;
    private final String mFinishSearchControlPlaceholderContent;

    YandexMapJavascriptInterface(ActivityYandexMap activityYandexMap) {
        mActivityYandexMap = activityYandexMap;

        mMapCenterLatitude = 55.752023;
        mMapCenterLongitude = 37.617499; // Москва, Кремль

        mStartSearchControlPlaceholderContent =
                mActivityYandexMap.getString(R.string.yandex_map_start_search_control_placeholder_content);
        mFinishSearchControlPlaceholderContent =
                mActivityYandexMap.getString(R.string.yandex_map_finish_search_control_placeholder_content);
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
    public void updateDistance(final int distance) {
        mActivityYandexMap.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityYandexMap.setDistance(distance);
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
}
