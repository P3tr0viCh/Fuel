package ru.p3tr0vich.fuel;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.pnikosis.materialishprogress.ProgressWheel;

public class ActivityYandexMap extends AppCompatActivity {

    public static final int REQUEST_CODE_DISTANCE = 8829;
    public static final int REQUEST_CODE_MAP_CENTER = 8830;

    private static final String INTENT_DISTANCE = "INTENT_DISTANCE";
    private static final String INTENT_MAP_CENTER_TEXT = "INTENT_MAP_CENTER_TEXT";
    private static final String INTENT_MAP_CENTER_LATITUDE = "INTENT_MAP_CENTER_LATITUDE";
    private static final String INTENT_MAP_CENTER_LONGITUDE = "INTENT_MAP_CENTER_LONGITUDE";

    private static final String EXTRA_TYPE = "EXTRA_TYPE";

    private static final String MAP_HTML_DISTANCE = "file:///android_asset/distanceCalculator.html";
    private static final String MAP_HTML_CENTER = "file:///android_asset/mapCenter.html";

    private MapType mType;

    private int mDistance = -1;
    private MapCenter mMapCenter;

    private Toolbar mToolbarYandexMap;
    private ProgressWheel mProgressWheelYandexMap;
    private FrameLayout mWebViewPlaceholder;
    private WebView mWebView;

    enum MapType {DISTANCE, CENTER}

    static class MapCenter {
        String text;
        double latitude, longitude;

        MapCenter() {
            this.text = YandexMapJavascriptInterface.DEFAULT_MAP_CENTER_TEXT;
            this.latitude = YandexMapJavascriptInterface.DEFAULT_MAP_CENTER_LATITUDE;
            this.longitude = YandexMapJavascriptInterface.DEFAULT_MAP_CENTER_LONGITUDE;
        }

        MapCenter(String text, double latitude, double longitude) {
            this.text = text;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public static void start(FragmentActivity parent, MapType mapType) {
        if (Functions.isInternetConnected())
            parent.startActivityForResult(new Intent(parent, ActivityYandexMap.class).putExtra(EXTRA_TYPE, mapType),
                    mapType == MapType.DISTANCE ? REQUEST_CODE_DISTANCE : REQUEST_CODE_MAP_CENTER);
        else
            FragmentDialogMessage.showMessage(parent,
                    parent.getString(R.string.title_message_error),
                    parent.getString(R.string.message_error_no_internet));
    }

    public static int getDistance(Intent data) {
        return data.getIntExtra(INTENT_DISTANCE, 0);
    }

    public static MapCenter getMapCenter(Intent data) {
        return new MapCenter(
                data.getStringExtra(INTENT_MAP_CENTER_TEXT),
                data.getDoubleExtra(INTENT_MAP_CENTER_LATITUDE,
                        YandexMapJavascriptInterface.DEFAULT_MAP_CENTER_LATITUDE),
                data.getDoubleExtra(INTENT_MAP_CENTER_LONGITUDE,
                        YandexMapJavascriptInterface.DEFAULT_MAP_CENTER_LONGITUDE));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yandex_map);

        mType = (MapType) getIntent().getSerializableExtra(EXTRA_TYPE);
        mMapCenter = new MapCenter();

        initUI();
    }

    @SuppressLint({"SetJavaScriptEnabled", "PrivateResource"})
    private void initUI() {
        mToolbarYandexMap = (Toolbar) findViewById(R.id.toolbarYandexMap);
        setSupportActionBar(mToolbarYandexMap);

        mToolbarYandexMap.setNavigationIcon(R.drawable.abc_ic_clear_mtrl_alpha);
        mToolbarYandexMap.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });

        Functions.logD("ActivityYandexMap -- initUI: mDistance == " + mDistance);

        setDistance(mDistance);

        mProgressWheelYandexMap = (ProgressWheel) findViewById(R.id.progressWheelYandexMap);

        mWebViewPlaceholder = (FrameLayout) findViewById(R.id.webViewPlaceholder);

        if (mWebView == null) {
            mWebView = new WebView(this);

            mWebView.getSettings().setJavaScriptEnabled(true);

            mWebView.addJavascriptInterface(new YandexMapJavascriptInterface(this), YandexMapJavascriptInterface.NAME);

            mWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(@NonNull ConsoleMessage cm) {
                    Functions.logD(cm.message() + " -- from line " + cm.lineNumber() + " of " + cm.sourceId());
                    return true;
                }
            });

            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)));

                    return true;
                }

                @SuppressWarnings("deprecation")
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Toast.makeText(ActivityYandexMap.this,
                            String.format(getString(R.string.text_error_webview), description),
                            Toast.LENGTH_SHORT).show();
                }

                @TargetApi(Build.VERSION_CODES.M) // TODO: MARSHMALLOW
                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    Toast.makeText(ActivityYandexMap.this,
                            String.format(getString(R.string.text_error_webview),
                                    error.getDescription()), Toast.LENGTH_SHORT).show();
                }
            });

            switch (mType) {
                case DISTANCE:
                    mWebView.loadUrl(MAP_HTML_DISTANCE);
                    break;
                case CENTER:
                    mWebView.loadUrl(MAP_HTML_CENTER);
                    break;
            }
        }

        if (mDistance == -1) mProgressWheelYandexMap.setVisibility(View.VISIBLE);

        mWebViewPlaceholder.addView(mWebView);
    }

    @Override
    protected void onDestroy() {
        if (mWebView != null) {
            mWebView.stopLoading();
            mWebView.clearCache(true);
            mWebView.destroy();
            mWebView = null;
        }
        super.onDestroy();
        Functions.logD("ActivityYandexMap -- onDestroy");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mWebView != null) mWebViewPlaceholder.removeView(mWebView);

        super.onConfigurationChanged(newConfig);

        setContentView(R.layout.activity_yandex_map);

        initUI();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mWebView.restoreState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(
                mType == MapType.DISTANCE ? R.menu.menu_yandex_map : R.menu.menu_yandex_map_center,
                menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (mType) {
            case DISTANCE:
                if (mDistance > 0)
                    switch (item.getItemId()) {
                        case R.id.action_done_x2:
                            mDistance *= 2;
                        case R.id.action_done:
                            setResult(RESULT_OK, new Intent().putExtra(INTENT_DISTANCE, mDistance));
                            finish();
                    }
                else
                    Toast.makeText(this, R.string.text_empty_distance, Toast.LENGTH_SHORT).show();
                return true;
            case CENTER:
                setResult(RESULT_OK, new Intent()
                        .putExtra(INTENT_MAP_CENTER_TEXT, mMapCenter.text)
                        .putExtra(INTENT_MAP_CENTER_LATITUDE, mMapCenter.latitude)
                        .putExtra(INTENT_MAP_CENTER_LONGITUDE, mMapCenter.longitude));
                finish();
                return true;
            default:
                return false;
        }
    }

    public MapType getType() {
        return mType;
    }

    public void setDistance(int distance) {
        mDistance = distance;

        String title = getString(R.string.title_yandex_map);
        if (mDistance > 0)
            title += String.format(getString(R.string.title_yandex_map_add), mDistance);

        Functions.logD("ActivityYandexMap -- setDistance: title == " + title);

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(title);
    }

    public void setMapCenter(final String text, final String name,
                             final double latitude, final double longitude) {
        Functions.logD("ActivityYandexMap -- setMapCenterText: text == " + text);

        mMapCenter.text = text;
        mMapCenter.latitude = latitude;
        mMapCenter.longitude = longitude;

        mToolbarYandexMap.setSubtitle(name);
    }

    public void endInitYandexMap() {
        Functions.logD("ActivityYandexMap -- endInitYandexMap");

        mDistance = 0;

        mProgressWheelYandexMap.setVisibility(View.GONE);
    }
}
