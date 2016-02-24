package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.webkit.JavascriptInterface;

@SuppressWarnings("unused")
class YandexMapJavascriptInterface {

    public static final String NAME = "YandexMapJavascriptInterface";

    private final Activity mActivity;

    public interface YandexMap {

        double getMapCenterLatitude();

        double getMapCenterLongitude();

        String getStartSearchControlPlaceholderContent();

        String getFinishSearchControlPlaceholderContent();

        String getEmptyBalloonContent();

        int getStartSearchControlLeft();

        int getStartSearchControlTop();

        int getFinishSearchControlLeft();

        int getFinishSearchControlTop();

        int getZoomControlLeft();

        int getZoomControlTop();

        void onDistanceChange(int distance);

        void onMapCenterChange(String text, String title, String subtitle,
                               double latitude, double longitude);

        void onEndLoading(boolean hasError);

        void onErrorSearchPoint();

        void onErrorConstructRoute();

        void onErrorGeolocation();
    }

    YandexMapJavascriptInterface(@NonNull Activity activity) {
        if (!(activity instanceof YandexMap))
            throw new ImplementException(activity, YandexMap.class);

        mActivity = activity;
    }

    @NonNull
    private YandexMap getYandexMapActivity() {
        return (YandexMap) mActivity;
    }

    @NonNull
    public static String performGeolocation() {
        return "javascript:performGeolocation()";
    }

    @JavascriptInterface
    public double getMapCenterLatitude() {
        return getYandexMapActivity().getMapCenterLatitude();
    }

    @JavascriptInterface
    public double getMapCenterLongitude() {
        return getYandexMapActivity().getMapCenterLongitude();
    }

    @JavascriptInterface
    public int getStartSearchControlLeft() {
        return getYandexMapActivity().getStartSearchControlLeft();
    }

    @JavascriptInterface
    public int getStartSearchControlTop() {
        return getYandexMapActivity().getStartSearchControlTop();
    }

    @JavascriptInterface
    public int getFinishSearchControlLeft() {
        return getYandexMapActivity().getFinishSearchControlLeft();
    }

    @JavascriptInterface
    public int getFinishSearchControlTop() {
        return getYandexMapActivity().getFinishSearchControlTop();
    }

    @JavascriptInterface
    public int getZoomControlLeft() {
        return getYandexMapActivity().getZoomControlLeft();
    }

    @JavascriptInterface
    public int getZoomControlTop() {
        return getYandexMapActivity().getZoomControlTop();
    }

    @JavascriptInterface
    public String getStartSearchControlPlaceholderContent() {
        return getYandexMapActivity().getStartSearchControlPlaceholderContent();
    }

    @JavascriptInterface
    public String getFinishSearchControlPlaceholderContent() {
        return getYandexMapActivity().getFinishSearchControlPlaceholderContent();
    }

    @JavascriptInterface
    public String getEmptyBalloonContent() {
        return getYandexMapActivity().getEmptyBalloonContent();
    }

    @JavascriptInterface
    public void onDistanceChange(final int distance) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getYandexMapActivity().onDistanceChange(distance);
            }
        });
    }

    @JavascriptInterface
    public void onMapCenterChange(final String text, final String title, final String subtitle,
                                  final double latitude, final double longitude) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getYandexMapActivity().onMapCenterChange(text, title, subtitle, latitude, longitude);
            }
        });
    }

    @JavascriptInterface
    public void onEndLoading(final boolean hasError) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getYandexMapActivity().onEndLoading(hasError);
            }
        });
    }

    @JavascriptInterface
    public void onErrorConstructRoute() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getYandexMapActivity().onErrorConstructRoute();
            }
        });
    }
    
    @JavascriptInterface
    public void onErrorSearchPoint() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getYandexMapActivity().onErrorSearchPoint();
            }
        });
    }

    @JavascriptInterface
    public void onErrorGeolocation() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getYandexMapActivity().onErrorGeolocation();
            }
        });
    }
}