package ru.p3tr0vich.fuel;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Status;
import com.melnykov.fab.FloatingActionButton;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import ru.p3tr0vich.fuel.helpers.ConnectivityHelper;
import ru.p3tr0vich.fuel.helpers.LocationHelper;
import ru.p3tr0vich.fuel.helpers.PreferencesHelper;
import ru.p3tr0vich.fuel.utils.Utils;
import ru.p3tr0vich.fuel.utils.UtilsFormat;
import ru.p3tr0vich.fuel.utils.UtilsLog;

public class ActivityYandexMap extends AppCompatActivity implements
        View.OnClickListener,
        YandexMapJavascriptInterface.YandexMap,
        LocationHelper.LocationHelperListener {

    private static final String TAG = "ActivityYandexMap";

    private static final String INTENT_DISTANCE = "INTENT_DISTANCE";
    private static final String INTENT_MAP_CENTER_TEXT = "INTENT_MAP_CENTER_TEXT";
    private static final String INTENT_MAP_CENTER_LATITUDE = "INTENT_MAP_CENTER_LATITUDE";
    private static final String INTENT_MAP_CENTER_LONGITUDE = "INTENT_MAP_CENTER_LONGITUDE";

    private static final String PREFIX_RUSSIA = "Россия, ";

    private static final String EXTRA_TYPE = "EXTRA_TYPE";

    private static final String URL_YANDEX_MAP = "file:///android_asset/yandexMap.html?";

    private static final String URL_QUERY_MAP_TYPE_DISTANCE = "distance";
    private static final String URL_QUERY_MAP_TYPE_CENTER = "center";

    private static final int RESOLUTION_REQUIRED_REQUEST_CODE = 1000;

    @MapType
    private int mType;

    private boolean mLoading = true;
    private int mDistance = 0;
    private MapCenter mMapCenter;

    private Toolbar mToolbarYandexMap;
    private ProgressWheel mProgressWheel;
    private FrameLayout mWebViewPlaceholder;
    private Menu mMenu;

    private WebView mWebView;
    private YandexMapJavascriptInterface mYandexMapJavascriptInterface;

    private LocationHelper mLocationHelper;

    private FloatingActionButton mBtnZoomIn;
    private FloatingActionButton mBtnZoomOut;
    private FloatingActionButton mBtnGeolocation;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MAP_TYPE_DISTANCE, MAP_TYPE_CENTER})
    public @interface MapType {
    }

    public static final int MAP_TYPE_DISTANCE = 0;
    public static final int MAP_TYPE_CENTER = 1;

    static class MapCenter {
        String text;
        String title;
        String subtitle;
        double latitude, longitude;

        MapCenter(String text, double latitude, double longitude) {
            this.text = text;
            this.title = "";
            this.subtitle = "";
            this.latitude = latitude;
            this.longitude = longitude;
        }

        MapCenter() {
            this(PreferencesHelper.DEFAULT_MAP_CENTER_TEXT,
                    PreferencesHelper.DEFAULT_MAP_CENTER_LATITUDE,
                    PreferencesHelper.DEFAULT_MAP_CENTER_LONGITUDE);
        }
    }

    public static void start(@NonNull FragmentActivity parent,
                             @MapType int mapType, int requestCode) {
        if (ConnectivityHelper.getConnectedState(parent.getApplicationContext()) != ConnectivityHelper.DISCONNECTED)
            parent.startActivityForResult(new Intent(parent, ActivityYandexMap.class)
                    .putExtra(EXTRA_TYPE, mapType), requestCode);
        else
            FragmentDialogMessage.show(parent,
                    null,
                    parent.getString(R.string.message_error_no_internet));
    }

    public static int getDistance(Intent data) {
        return data.getIntExtra(INTENT_DISTANCE, 0);
    }

    public static MapCenter getMapCenter(Intent data) {
        return new MapCenter(
                data.getStringExtra(INTENT_MAP_CENTER_TEXT),
                data.getDoubleExtra(INTENT_MAP_CENTER_LATITUDE,
                        PreferencesHelper.DEFAULT_MAP_CENTER_LATITUDE),
                data.getDoubleExtra(INTENT_MAP_CENTER_LONGITUDE,
                        PreferencesHelper.DEFAULT_MAP_CENTER_LONGITUDE));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UtilsLog.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yandex_map);

        switch (getIntent().getIntExtra(EXTRA_TYPE, -1)) {
            case MAP_TYPE_DISTANCE:
                mType = MAP_TYPE_DISTANCE;
                break;
            case MAP_TYPE_CENTER:
                mType = MAP_TYPE_CENTER;
        }

        mMapCenter = new MapCenter();
        mMapCenter.text = getString(R.string.yandex_map_map_center_title);

        mLocationHelper = new LocationHelper(this).setLocationHelperListener(this);

        initUI();
    }

    @SuppressLint({"SetJavaScriptEnabled"})
    private void initUI() {
        mMenu = null;

        mToolbarYandexMap = (Toolbar) findViewById(R.id.toolbar_yandex_map);
        setSupportActionBar(mToolbarYandexMap);

        mToolbarYandexMap.setNavigationIcon(R.drawable.ic_close);

        mToolbarYandexMap.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });

        UtilsLog.d(TAG, "initUI", "mLoading == " + mLoading);

        switch (mType) {
            case MAP_TYPE_DISTANCE:
                onDistanceChange(mDistance);
                break;
            case MAP_TYPE_CENTER:
                onMapCenterChange(
                        mMapCenter.text,
                        mMapCenter.title,
                        mMapCenter.subtitle,
                        mMapCenter.latitude,
                        mMapCenter.longitude);
                break;
        }

        mProgressWheel = (ProgressWheel) findViewById(R.id.progress_wheel);

        mBtnZoomIn = (FloatingActionButton) findViewById(R.id.btn_zoom_in);
        assert mBtnZoomIn != null;
        mBtnZoomIn.setOnClickListener(this);

        mBtnZoomOut = (FloatingActionButton) findViewById(R.id.btn_zoom_out);
        assert mBtnZoomOut != null;
        mBtnZoomOut.setOnClickListener(this);

        mBtnGeolocation = (FloatingActionButton) findViewById(R.id.btn_geolocation);
        assert mBtnGeolocation != null;
        mBtnGeolocation.setOnClickListener(this);

        mWebViewPlaceholder = (FrameLayout) findViewById(R.id.web_view_placeholder);

        if (mWebView == null) {
            mWebView = new WebView(this);

            WebSettings webSettings = mWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);

            mYandexMapJavascriptInterface = new YandexMapJavascriptInterface(this, mWebView);

            mWebView.addJavascriptInterface(mYandexMapJavascriptInterface,
                    YandexMapJavascriptInterface.NAME);

            mWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(@NonNull ConsoleMessage cm) {
                    UtilsLog.d(TAG, "onConsoleMessage", cm.message() + " [line " + cm.lineNumber() + "]");
                    return true;
                }

                @Override
                public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                    UtilsLog.d(TAG, "onGeolocationPermissionsShowPrompt", "origin == " + origin);
                    callback.invoke(origin, true, false);
                }
            });

            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    final String innerUrl = url;

                    ActivityYandexMap.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.openUrl(ActivityYandexMap.this, innerUrl, null);
                        }
                    });

                    return true;
                }

                @SuppressWarnings("deprecation")
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    UtilsLog.d(TAG, "onReceivedError", "errorCode == " + errorCode +
                            ", description == " + description);
                    Utils.toast(String.format(getString(R.string.text_error_webview), description));
                }

                @TargetApi(Build.VERSION_CODES.M) // TODO: MARSHMALLOW
                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    UtilsLog.d(TAG, "onReceivedError", "request == " + request.toString() +
                            ", error == " + error.toString());
                    Utils.toast(String.format(getString(R.string.text_error_webview), error.getDescription()));
                }
            });

            String url = URL_YANDEX_MAP;

            switch (mType) {
                case MAP_TYPE_DISTANCE:
                    url += URL_QUERY_MAP_TYPE_DISTANCE;
                    break;
                case MAP_TYPE_CENTER:
                    url += URL_QUERY_MAP_TYPE_CENTER;
                    break;
            }

            mWebView.loadUrl(url);
        }

        if (mLoading)
            mProgressWheel.setVisibility(View.VISIBLE);
        else {
            mProgressWheel.setVisibility(View.GONE);
            setButtonsVisible();
        }

        mWebViewPlaceholder.addView(mWebView);
    }

    @Override
    protected void onDestroy() {
        UtilsLog.d(TAG, "onDestroy");

        if (mWebView != null) {
            mWebView.stopLoading();
            mWebView.clearCache(true);
            mWebView.destroy();
            mWebView = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        UtilsLog.d(TAG, "onStart");

        mLocationHelper.connect();
    }

    @Override
    protected void onStop() {
        UtilsLog.d(TAG, "onStop");

        mLocationHelper.disconnect();

        super.onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        UtilsLog.d(TAG, "onConfigurationChanged");

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
        getMenuInflater().inflate(R.menu.menu_yandex_map, menu);

        mMenu = menu;

        if (!mLoading) setMenuItemsVisible();

        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_zoom_in:
                mYandexMapJavascriptInterface.setZoom(true);
                break;
            case R.id.btn_zoom_out:
                mYandexMapJavascriptInterface.setZoom(false);
                break;
            case R.id.btn_geolocation:
                final int googlePlayServicesAvailable = mLocationHelper.isGooglePlayServicesAvailable();

                if (googlePlayServicesAvailable == ConnectionResult.SUCCESS) {
                    if (mLocationHelper.isRequestLocationUpdatesInProcess())
                        Utils.toast(R.string.message_yandex_map_geolocation_in_progress);
                    else
                        mLocationHelper.getLocation();
                } else
                    FragmentDialogMessage.show(this,
                            mLocationHelper.getConnectionResultTitle(googlePlayServicesAvailable),
                            mLocationHelper.getConnectionResultMessage(googlePlayServicesAvailable));

                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (mType) {
            case MAP_TYPE_DISTANCE:
                if (mDistance > 0)
                    switch (item.getItemId()) {
                        case R.id.action_done_x2:
                            mDistance *= 2;
                        case R.id.action_done:
                            setResult(RESULT_OK, new Intent().putExtra(INTENT_DISTANCE, mDistance));
                            finish();
                    }
                else
                    Utils.toast(R.string.text_empty_distance);
                return true;
            case MAP_TYPE_CENTER:
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

    public void onDistanceChange(int distance) {
        mDistance = distance;

        String title = getString(R.string.title_yandex_map);
        if (mDistance > 0)
            title += String.format(getString(R.string.title_yandex_map_add), mDistance);

        UtilsLog.d(TAG, "onDistanceChange", "title == " + title);

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(title);
    }

    private String minimizeGeoCode(String text) {
        return text.startsWith(PREFIX_RUSSIA) ? text.substring(PREFIX_RUSSIA.length()) : text;
    }

    @Override
    public double getMapCenterLatitude() {
        return PreferencesHelper.getMapCenterLatitude();
    }

    @Override
    public double getMapCenterLongitude() {
        return PreferencesHelper.getMapCenterLongitude();
    }

    @Override
    public String getStartSearchControlPlaceholderContent() {
        return getString(R.string.yandex_map_start_search_control_placeholder_content);
    }

    @Override
    public String getFinishSearchControlPlaceholderContent() {
        return getString(R.string.yandex_map_finish_search_control_placeholder_content);
    }

    @Override
    public String getEmptyBalloonContent() {
        return "<h3>" + getString(R.string.yandex_map_empty_geocode) + "</h3>";
    }

    @Override
    public int getStartSearchControlLeft() {
        return Utils.getInteger(R.integer.yandex_map_start_search_left);
    }

    @Override
    public int getStartSearchControlTop() {
        return Utils.getInteger(R.integer.yandex_map_start_search_top);
    }

    @Override
    public int getFinishSearchControlLeft() {
        return Utils.getInteger(R.integer.yandex_map_finish_search_left);
    }

    @Override
    public int getFinishSearchControlTop() {
        return Utils.getInteger(R.integer.yandex_map_finish_search_top);
    }

    public void onMapCenterChange(String text, String title, String subtitle,
                                  double latitude, double longitude) {
        UtilsLog.d(TAG, "onMapCenterChange", "text == " + text);

        if (!TextUtils.isEmpty(text))
            mMapCenter.text = minimizeGeoCode(text);
        else
            mMapCenter.text =
                    UtilsFormat.floatToString((float) latitude) + ',' +
                            UtilsFormat.floatToString((float) longitude);

        if (TextUtils.isEmpty(title))
            mMapCenter.title = mMapCenter.text;
        else
            mMapCenter.title = title;

        mMapCenter.subtitle = subtitle;
        mMapCenter.latitude = latitude;
        mMapCenter.longitude = longitude;

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(mMapCenter.title);

        mToolbarYandexMap.setSubtitle(mMapCenter.subtitle);
    }

    private void setMenuItemVisibleTrue(@Nullable MenuItem menuItem) {
        if (menuItem != null) menuItem.setVisible(true);
    }

    private void setButtonsVisible() {
        Utils.setViewVisibleAnimate(mBtnZoomIn, true);
        Utils.setViewVisibleAnimate(mBtnZoomOut, true);
        Utils.setViewVisibleAnimate(mBtnGeolocation, true);
    }

    private void setMenuItemsVisible() {
        if (mMenu != null) {
            setMenuItemVisibleTrue(mMenu.findItem(R.id.action_done));

            if (mType == MAP_TYPE_DISTANCE)
                setMenuItemVisibleTrue(mMenu.findItem(R.id.action_done_x2));
        }
    }

    public void onEndLoading(boolean hasError) {
        UtilsLog.d(TAG, "onEndLoading", "hasError == " + hasError);

        mLoading = false;

        mProgressWheel.setVisibility(View.GONE);

        if (!hasError) {
            setButtonsVisible();
            setMenuItemsVisible();
        }
    }

    @Override
    public void onGeolocationStart() {
        mBtnGeolocation.setEnabled(false);
    }

    @Override
    public void onGeolocationFinish() {
        mBtnGeolocation.setEnabled(true);
    }

    @Override
    public void onErrorSearchPoint() {
        Utils.toast(R.string.message_error_yandex_map_search_point);
    }

    public void onErrorConstructRoute() {
        onDistanceChange(0);
        Utils.toast(R.string.message_error_yandex_map_route);
    }

    @Override
    public void onErrorGeolocation() {
        Utils.toast(R.string.message_error_yandex_map_geolocation);
    }


    @Override
    public void onLastLocationReturn(@Nullable Location location) {
        if (location != null)
            mYandexMapJavascriptInterface.setStartLocation(
                    location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onUpdatedLocationReturn(@Nullable Location location) {
        if (location != null)
            mYandexMapJavascriptInterface.setStartLocation(
                    location.getLatitude(), location.getLongitude());
        else
            onErrorGeolocation();
    }

    @Override
    public void onResolutionRequired(@NonNull Status status) {
        try {
            status.startResolutionForResult(this, RESOLUTION_REQUIRED_REQUEST_CODE);
        } catch (Exception e) {
            UtilsLog.d(TAG, "onResolutionRequired", "Exception e == " + e.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESOLUTION_REQUIRED_REQUEST_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        mBtnGeolocation.callOnClick();

                        break;
                    case Activity.RESULT_CANCELED:
                        UtilsLog.d(TAG, "onActivityResult", "RESOLUTION_REQUIRED_REQUEST_CODE result == RESULT_CANCELED");

                        break;
                    default:
                        break;
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
}