package ru.p3tr0vich.fuel.activities

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Status
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pnikosis.materialishprogress.ProgressWheel
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.YandexMapJavascriptInterface
import ru.p3tr0vich.fuel.fragments.FragmentDialogMessage
import ru.p3tr0vich.fuel.helpers.ConnectivityHelper
import ru.p3tr0vich.fuel.helpers.LocationHelper
import ru.p3tr0vich.fuel.helpers.PreferencesHelper
import ru.p3tr0vich.fuel.models.MapCenter
import ru.p3tr0vich.fuel.models.MapCenter.Companion.EXTRA_MAP_CENTER_LATITUDE
import ru.p3tr0vich.fuel.models.MapCenter.Companion.EXTRA_MAP_CENTER_LONGITUDE
import ru.p3tr0vich.fuel.models.MapCenter.Companion.EXTRA_MAP_CENTER_TEXT
import ru.p3tr0vich.fuel.utils.Utils
import ru.p3tr0vich.fuel.utils.UtilsDate
import ru.p3tr0vich.fuel.utils.UtilsFormat
import ru.p3tr0vich.fuel.utils.UtilsLog

class ActivityYandexMap : AppCompatActivity(),
        View.OnClickListener, View.OnLongClickListener,
        YandexMapJavascriptInterface.YandexMap,
        LocationHelper.LocationHelperListener {

    @MapType
    private var type: Int = 0

    private var loading = true
    private var distance = 0
    private var time = 0

    private val mapCenter = MapCenter()

    private var toolbarYandexMap: Toolbar? = null
    private var progressWheel: ProgressWheel? = null
    private var webViewPlaceholder: FrameLayout? = null
    private var menu: Menu? = null

    private var webView: WebView? = null
    private var yandexMapJavascriptInterface: YandexMapJavascriptInterface? = null

    private var locationHelper: LocationHelper? = null

    private var btnZoomIn: FloatingActionButton? = null
    private var btnZoomOut: FloatingActionButton? = null
    private var btnGeolocation: FloatingActionButton? = null

    override val mapCenterLatitude: Double
        get() = mapCenter.latitude

    override val mapCenterLongitude: Double
        get() = mapCenter.longitude

    override val startSearchControlPlaceholderContent: String
        get() = getString(R.string.yandex_map_start_search_control_placeholder_content)

    override val finishSearchControlPlaceholderContent: String
        get() = getString(R.string.yandex_map_finish_search_control_placeholder_content)

    override val emptyBalloonContent: String
        get() = "<h3>" + getString(R.string.yandex_map_empty_geocode) + "</h3>"

    override val startSearchControlLeft: Int
        get() = Utils.getInteger(R.integer.yandex_map_start_search_left)

    override val startSearchControlTop: Int
        get() = Utils.getInteger(R.integer.yandex_map_start_search_top)

    override val finishSearchControlLeft: Int
        get() = Utils.getInteger(R.integer.yandex_map_finish_search_left)

    override val finishSearchControlTop: Int
        get() = Utils.getInteger(R.integer.yandex_map_finish_search_top)

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(MAP_TYPE_DISTANCE, MAP_TYPE_CENTER)
    annotation class MapType

    override fun onCreate(savedInstanceState: Bundle?) {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onCreate")
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yandex_map)

        when (intent.getIntExtra(EXTRA_TYPE, -1)) {
            MAP_TYPE_DISTANCE -> type = MAP_TYPE_DISTANCE
            MAP_TYPE_CENTER -> type = MAP_TYPE_CENTER
        }

        val preferencesHelper = PreferencesHelper.getInstance(this)

        mapCenter.text = getString(R.string.yandex_map_map_center_title)
        with(preferencesHelper.mapCenter) {
            mapCenter.latitude = latitude
            mapCenter.longitude = longitude
        }

        locationHelper = LocationHelper(this, this,
                REQUEST_CODE_PERMISSION_ACCESS_FINE_LOCATION)

        initUI()
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun initUI() {
        menu = null

        toolbarYandexMap = findViewById(R.id.toolbar_yandex_map)
        setSupportActionBar(toolbarYandexMap)

        toolbarYandexMap?.setNavigationIcon(R.drawable.ic_close)

        toolbarYandexMap?.setNavigationOnClickListener {
            setResult(Activity.RESULT_CANCELED, null)
            finish()
        }

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "initUI", "loading == $loading")
        }

        when (type) {
            MAP_TYPE_DISTANCE -> onRouteChange(distance, time)
            MAP_TYPE_CENTER -> onMapCenterChange(
                    mapCenter.text,
                    mapCenter.title,
                    mapCenter.subtitle,
                    mapCenter.latitude,
                    mapCenter.longitude)
        }

        progressWheel = findViewById(R.id.progress_wheel)

        btnZoomIn = findViewById(R.id.btn_zoom_in)
        btnZoomIn?.setOnClickListener(this)
        btnZoomIn?.setOnLongClickListener(this)

        btnZoomOut = findViewById(R.id.btn_zoom_out)
        btnZoomOut?.setOnClickListener(this)
        btnZoomOut?.setOnLongClickListener(this)

        btnGeolocation = findViewById(R.id.btn_geolocation)
        btnGeolocation?.setOnClickListener(this)

        webViewPlaceholder = findViewById(R.id.web_view_placeholder)

        if (webView == null) {
            webView = WebView(this)

            val webSettings = webView?.settings
            webSettings?.javaScriptEnabled = true

            yandexMapJavascriptInterface = YandexMapJavascriptInterface(this, webView!!)

            webView?.addJavascriptInterface(yandexMapJavascriptInterface,
                    YandexMapJavascriptInterface.NAME)

            webView?.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                    if (LOG_ENABLED) {
                        UtilsLog.d(TAG, "onConsoleMessage", cm.message() + " [line " + cm.lineNumber() + "]")
                    }
                    return true
                }

                override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                    if (LOG_ENABLED) {
                        UtilsLog.d(TAG, "onGeolocationPermissionsShowPrompt", "origin == $origin")
                    }
                    callback.invoke(origin, true, false)
                }
            }

            webView?.webViewClient = object : WebViewClient() {
                @Suppress("OverridingDeprecatedMember")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    this@ActivityYandexMap.runOnUiThread { Utils.openUrl(this@ActivityYandexMap, url, null) }

                    return true
                }

                @Suppress("OverridingDeprecatedMember")
                override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                    if (LOG_ENABLED) {
                        UtilsLog.d(TAG, "onReceivedError", "errorCode == $errorCode, description == $description")
                    }

                    Utils.toast(String.format(getString(R.string.text_error_webview), description))
                }

                @TargetApi(Build.VERSION_CODES.M)
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    super.onReceivedError(view, request, error)
                    if (LOG_ENABLED) {
                        UtilsLog.d(TAG, "onReceivedError", "request == $request, error == $error")
                    }

                    Utils.toast(String.format(getString(R.string.text_error_webview), error.description))
                }
            }

            var url = URL_YANDEX_MAP

            when (type) {
                MAP_TYPE_DISTANCE -> url += URL_QUERY_MAP_TYPE_DISTANCE
                MAP_TYPE_CENTER -> url += URL_QUERY_MAP_TYPE_CENTER
            }

            webView?.loadUrl(url)
        }

        if (loading) {
            progressWheel?.visibility = View.VISIBLE
        } else {
            progressWheel?.visibility = View.GONE
            setButtonsVisible()
        }

        webViewPlaceholder?.addView(webView)
    }

    override fun onDestroy() {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onDestroy")
        }

        if (webView != null) {
            webView!!.stopLoading()
            webView!!.clearCache(true)
            webView!!.destroy()
            webView = null
        }

        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onStart")
        }

        locationHelper?.connect()
    }

    override fun onStop() {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onStop")
        }

        locationHelper?.disconnect()

        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onConfigurationChanged")
        }

        if (webView != null) {
            webViewPlaceholder?.removeView(webView)
        }

        super.onConfigurationChanged(newConfig)

        setContentView(R.layout.activity_yandex_map)

        initUI()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView?.restoreState(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_yandex_map, menu)

        this.menu = menu

        if (!loading) {
            setMenuItemsVisible()
        }

        return true
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_zoom_in -> yandexMapJavascriptInterface!!.setZoomIn()
            R.id.btn_zoom_out -> yandexMapJavascriptInterface!!.setZoomOut()
            R.id.btn_geolocation -> {
                val googlePlayServicesAvailable = locationHelper!!.isGooglePlayServicesAvailable

                if (googlePlayServicesAvailable == ConnectionResult.SUCCESS) {
                    if (locationHelper!!.isRequestLocationUpdatesInProcess) {
                        Utils.toast(R.string.message_yandex_map_geolocation_in_progress)
                    } else {
                        locationHelper!!.getLocation()
                    }
                } else {
                    locationHelper!!.showErrorNotification(this, googlePlayServicesAvailable)
                }
            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        return when (view.id) {
            R.id.btn_zoom_in -> {
                yandexMapJavascriptInterface!!.setZoomToHouses()
                true
            }
            R.id.btn_zoom_out -> {
                yandexMapJavascriptInterface!!.setZoomToCities()
                true
            }
            else -> false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (type) {
            MAP_TYPE_DISTANCE -> {
                if (distance > 0)
                    when (item.itemId) {
                        R.id.action_done_x2 -> {
                            distance *= 2
                            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_DISTANCE, distance))
                            finish()
                        }
                        R.id.action_done -> {
                            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_DISTANCE, distance))
                            finish()
                        }
                    }
                else {
                    Utils.toast(R.string.text_empty_distance)
                }

                true
            }
            MAP_TYPE_CENTER -> {
                setResult(Activity.RESULT_OK, Intent()
                        .putExtra(EXTRA_MAP_CENTER_TEXT, mapCenter.text)
                        .putExtra(EXTRA_MAP_CENTER_LATITUDE, mapCenter.latitude)
                        .putExtra(EXTRA_MAP_CENTER_LONGITUDE, mapCenter.longitude))

                finish()

                true
            }
            else -> false
        }
    }

    override fun onRouteChange(distance: Int, time: Int) {
        this.distance = distance
        this.time = time

        var title = getString(R.string.title_yandex_map)
        if (distance > 0) {
            title += ": " + (distance / 1000).toString() + " " + getString(R.string.units_km)
        }

        var subtitle: String? = null
        if (this.time > 59) {
            subtitle = getString(R.string.title_yandex_map_subtitle)

            val hms = UtilsDate.splitSeconds(this.time)

            subtitle += ":"
            if (hms[0] > 0) {
                subtitle += " " + hms[0] + " " + getString(R.string.units_hours)
            }
            if (hms[1] > 0) {
                subtitle += " " + hms[1] + " " + getString(R.string.units_minutes)
            }
        }

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onRouteChange",
                    "title == $title, subTitle == $subtitle")
        }

        supportActionBar?.title = title
        supportActionBar?.subtitle = subtitle
    }

    private fun minimizeGeoCode(text: String): String {
        return if (text.startsWith(PREFIX_RUSSIA)) text.substring(PREFIX_RUSSIA.length) else text
    }

    override fun onMapCenterChange(text: String?, title: String?, subtitle: String?,
                                   latitude: Double, longitude: Double) {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onMapCenterChange", "text == $text")
        }

        if (!TextUtils.isEmpty(text)) {
            mapCenter.text = minimizeGeoCode(text!!)
        } else {
            mapCenter.text = UtilsFormat.floatToString(latitude.toFloat()) + ',' +
                    UtilsFormat.floatToString(longitude.toFloat())
        }

        if (TextUtils.isEmpty(title)) {
            mapCenter.title = mapCenter.text
        } else {
            mapCenter.title = title.toString()
        }

        mapCenter.subtitle = subtitle.toString()

        mapCenter.latitude = latitude
        mapCenter.longitude = longitude


        supportActionBar?.title = mapCenter.title

        toolbarYandexMap?.subtitle = mapCenter.subtitle
    }

    private fun setButtonsVisible() {
        Utils.setViewVisibleAnimate(btnZoomIn!!, true)
        Utils.setViewVisibleAnimate(btnZoomOut!!, true)
        Utils.setViewVisibleAnimate(btnGeolocation!!, true)
    }

    private fun setMenuItemsVisible() {
        menu?.findItem(R.id.action_done)?.isVisible = true

        if (type == MAP_TYPE_DISTANCE) {
            menu?.findItem(R.id.action_done_x2)?.isVisible = true
        }
    }

    override fun onEndLoading(hasError: Boolean) {
        if (LOG_ENABLED) UtilsLog.d(TAG, "onEndLoading", "hasError == $hasError")

        loading = false

        progressWheel!!.visibility = View.GONE

        if (!hasError) {
            setButtonsVisible()
            setMenuItemsVisible()
        }
    }

    override fun onGeolocationStart() {
        btnGeolocation!!.isEnabled = false
    }

    override fun onGeolocationFinish() {
        btnGeolocation!!.isEnabled = true
    }

    override fun onErrorSearchPoint() {
        Utils.toast(R.string.message_error_yandex_map_search_point)
    }

    override fun onErrorConstructRoute() {
        onRouteChange(0, 0)
        Utils.toast(R.string.message_error_yandex_map_route)
    }

    override fun onErrorGeolocation() {
        Utils.toast(R.string.message_error_yandex_map_geolocation)
    }


    override fun onLastLocationReturn(location: Location?) {
        if (location != null) {
            yandexMapJavascriptInterface!!.setStartLocation(
                    location.latitude, location.longitude)
        }
    }

    override fun onUpdatedLocationReturn(location: Location?) {
        if (location != null) {
            yandexMapJavascriptInterface!!.setStartLocation(
                    location.latitude, location.longitude)
        } else {
            onErrorGeolocation()
        }
    }

    override fun onResolutionRequired(status: Status) {
        try {
            status.startResolutionForResult(this, REQUEST_CODE_RESOLUTION_REQUIRED)
        } catch (e: Exception) {
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "onResolutionRequired", "Exception e == $e")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_RESOLUTION_REQUIRED -> when (resultCode) {
                Activity.RESULT_OK -> btnGeolocation?.callOnClick()
                Activity.RESULT_CANCELED -> if (LOG_ENABLED) {
                    UtilsLog.d(TAG, "onActivityResult", "REQUEST_CODE_RESOLUTION_REQUIRED result == RESULT_CANCELED")
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                REQUEST_CODE_PERMISSION_ACCESS_FINE_LOCATION -> btnGeolocation?.callOnClick()
            }
        } else {
            FragmentDialogMessage.show(this, R.string.title_message_error, R.string.message_need_permission_to_location)
        }
    }

    companion object {
        private const val TAG = "ActivityYandexMap"

        private var LOG_ENABLED = false

        const val EXTRA_TYPE = "EXTRA_TYPE"
        const val EXTRA_DISTANCE = "EXTRA_DISTANCE"

        private const val PREFIX_RUSSIA = "Россия, "

        private const val URL_YANDEX_MAP = "file:///android_asset/yandexMap.html?"

        private const val URL_QUERY_MAP_TYPE_DISTANCE = "distance"
        private const val URL_QUERY_MAP_TYPE_CENTER = "center"

        private const val REQUEST_CODE_RESOLUTION_REQUIRED = 1000
        private const val REQUEST_CODE_PERMISSION_ACCESS_FINE_LOCATION = 1001

        const val MAP_TYPE_DISTANCE = 0
        const val MAP_TYPE_CENTER = 1

        @JvmStatic
        fun start(parent: FragmentActivity, @MapType mapType: Int, requestCode: Int) {
            val connectedState = ConnectivityHelper.getConnectedState(parent.applicationContext)

            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "start", "connectedState == $connectedState")
            }

            if (connectedState != ConnectivityHelper.DISCONNECTED) {
                parent.startActivityForResult(Intent(parent, ActivityYandexMap::class.java)
                        .putExtra(EXTRA_TYPE, mapType), requestCode)
            } else {
                FragmentDialogMessage.show(parent, null,
                        parent.getString(R.string.message_error_no_internet))
            }
        }
    }
}