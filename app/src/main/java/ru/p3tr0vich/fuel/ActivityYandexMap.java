package ru.p3tr0vich.fuel;

// TODO: Кнопка ГОТОВО недоступна, пока не выбрано расстояние

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.pnikosis.materialishprogress.ProgressWheel;


public class ActivityYandexMap extends AppCompatActivity {

    public static final int REQUEST_CODE = 8829;

    private static final String INTENT_DISTANCE = "INTENT_DISTANCE";

    private static final String MAP_HTML = "file:///android_asset/distanceCalculator.html";

    private int mDistance = -1;

    private ProgressWheel mProgressWheelYandexMap;
    private FrameLayout mWebViewPlaceholder;
    private WebView mWebView;

    public static void start(Activity parent) {
        parent.startActivityForResult(new Intent(parent, ActivityYandexMap.class), REQUEST_CODE);
    }

    public static int getDistance(Intent data) {
        return data.getIntExtra(INTENT_DISTANCE, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yandex_map);

        initUI();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initUI() {
        Toolbar mToolbarYandexMap = (Toolbar) findViewById(R.id.toolbarYandexMap);
        setSupportActionBar(mToolbarYandexMap);

        mToolbarYandexMap.setNavigationIcon(R.drawable.abc_ic_clear_mtrl_alpha);
        mToolbarYandexMap.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });

        Log.d(Const.LOG_TAG, "ActivityYandexMap -- initUI: mDistance == " + mDistance);

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
                    Log.d(Const.LOG_TAG, cm.message() + " -- from line " + cm.lineNumber() + " of " + cm.sourceId());
                    return true;
                }
            });

            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Toast.makeText(ActivityYandexMap.this,
                            String.format(getString(R.string.text_error_webview), description), Toast.LENGTH_SHORT).show();
                }
            });

            mWebView.loadUrl(MAP_HTML);
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
        Log.d(Const.LOG_TAG, "ActivityYandexMap -- onDestroy");
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
        getMenuInflater().inflate(R.menu.menu_yandex_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                setResult(RESULT_OK, new Intent().putExtra(INTENT_DISTANCE, mDistance));
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setDistance(int distance) {
        mDistance = distance;

        String title = getString(R.string.title_activity_yandex_map);
        if (mDistance > 0)
            title += String.format(getString(R.string.title_activity_yandex_map_add), mDistance);

        Log.d(Const.LOG_TAG, "ActivityYandexMap -- setDistance: title == " + title);

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(title);
    }

    public void endInitYandexMap() {
        Log.d(Const.LOG_TAG, "ActivityYandexMap -- endInitYandexMap");

        mDistance = 0;

        mProgressWheelYandexMap.setVisibility(View.GONE);
    }
}
