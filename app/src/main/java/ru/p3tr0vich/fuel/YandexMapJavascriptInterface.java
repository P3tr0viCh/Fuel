package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.webkit.JavascriptInterface;

@SuppressWarnings("unused")
class YandexMapJavascriptInterface {

    public static final String NAME = "YandexMapJavascriptInterface";

    private final Activity mActivity;

    private final double mMapCenterLatitude;
    private final double mMapCenterLongitude;

    public interface YandexMap {
        String getStartSearchControlPlaceholderContent();

        String getFinishSearchControlPlaceholderContent();

        String getEmptyBalloonContent();

        void OnDistanceChange(int distance);

        void OnMapCenterChange(String text, String title, String subtitle,
                               double latitude, double longitude);

        void OnEndLoading(boolean hasError);

        void OnErrorConstructRoute();
    }

    YandexMapJavascriptInterface(@NonNull Activity activity) {
        if (!(activity instanceof YandexMap))
            throw new ImplementException(activity, YandexMap.class);

        mActivity = activity;

        mMapCenterLatitude = PreferenceManagerFuel.getMapCenterLatitude();
        mMapCenterLongitude = PreferenceManagerFuel.getMapCenterLongitude();
    }

    @NonNull
    private YandexMap getYandexMapActivity() {
        return (YandexMap) mActivity;
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
    public void OnDistanceChange(final int distance) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getYandexMapActivity().OnDistanceChange(distance);
            }
        });
    }

    @JavascriptInterface
    public void OnMapCenterChange(final String text, final String title, final String subtitle,
                             final double latitude, final double longitude) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getYandexMapActivity().OnMapCenterChange(text, title, subtitle, latitude, longitude);
            }
        });
    }

    @JavascriptInterface
    public void OnEndLoading(final boolean hasError) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getYandexMapActivity().OnEndLoading(hasError);
            }
        });
    }

    @JavascriptInterface
    public void OnErrorConstructRoute() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getYandexMapActivity().OnErrorConstructRoute();
            }
        });
    }
}