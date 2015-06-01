package ru.p3tr0vich.fuel;


import android.webkit.JavascriptInterface;

class YandexMapJavascriptInterface {

    public static final String NAME = "YandexMapJavascriptInterface";

    private final ActivityYandexMap mActivityYandexMap;

    private final String mStartSearchControlPlaceholderContent;
    private final String mFinishSearchControlPlaceholderContent;

    YandexMapJavascriptInterface(ActivityYandexMap activityYandexMap) {
        mActivityYandexMap = activityYandexMap;

        mStartSearchControlPlaceholderContent =
                mActivityYandexMap.getString(R.string.yandex_map_start_search_control_placeholder_content);
        mFinishSearchControlPlaceholderContent =
                mActivityYandexMap.getString(R.string.yandex_map_finish_search_control_placeholder_content);
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
