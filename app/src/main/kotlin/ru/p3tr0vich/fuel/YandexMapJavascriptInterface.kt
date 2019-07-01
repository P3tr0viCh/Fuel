package ru.p3tr0vich.fuel

import android.app.Activity
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebView

import ru.p3tr0vich.fuel.utils.UtilsLog

@Suppress("unused")
internal class YandexMapJavascriptInterface(private val activity: Activity, private val webView: WebView) {

    companion object {
        const val NAME = "YandexMapJavascriptInterface"

        private var LOG_ENABLED = false

        private const val JS = "javascript:"

        private const val ZOOM_HOUSES = 17
        private const val ZOOM_CITIES = 8
    }

    interface YandexMap {
        val mapCenterLatitude: Double

        val mapCenterLongitude: Double

        val startSearchControlPlaceholderContent: String

        val finishSearchControlPlaceholderContent: String

        val emptyBalloonContent: String

        val startSearchControlLeft: Int

        val startSearchControlTop: Int

        val finishSearchControlLeft: Int

        val finishSearchControlTop: Int

        /**
         * @param distance расстояние в метрах.
         * @param time     время в пути в секундах.
         */
        fun onRouteChange(distance: Int, time: Int)

        /**
         * @param text      полное наименования географической точки.
         * @param title     основное название (напр., название города).
         * @param subtitle  дополнительное название (напр., улица и номер дома).
         * @param latitude  широта.
         * @param longitude долгота.
         */
        fun onMapCenterChange(text: String?, title: String?, subtitle: String?,
                              latitude: Double, longitude: Double)

        fun onEndLoading(hasError: Boolean)

        fun onGeolocationStart()

        fun onGeolocationFinish()

        fun onErrorSearchPoint()

        fun onErrorConstructRoute()

        fun onErrorGeolocation()
    }

    private val yandexMapActivity: YandexMap
        get() = activity as YandexMap

    val mapCenterLatitude: Double
        @JavascriptInterface
        get() = yandexMapActivity.mapCenterLatitude

    val mapCenterLongitude: Double
        @JavascriptInterface
        get() = yandexMapActivity.mapCenterLongitude

    val startSearchControlLeft: Int
        @JavascriptInterface
        get() = yandexMapActivity.startSearchControlLeft

    val startSearchControlTop: Int
        @JavascriptInterface
        get() = yandexMapActivity.startSearchControlTop

    val finishSearchControlLeft: Int
        @JavascriptInterface
        get() = yandexMapActivity.finishSearchControlLeft

    val finishSearchControlTop: Int
        @JavascriptInterface
        get() = yandexMapActivity.finishSearchControlTop

    val startSearchControlPlaceholderContent: String
        @JavascriptInterface
        get() = yandexMapActivity.startSearchControlPlaceholderContent

    val finishSearchControlPlaceholderContent: String
        @JavascriptInterface
        get() = yandexMapActivity.finishSearchControlPlaceholderContent

    val emptyBalloonContent: String
        @JavascriptInterface
        get() = yandexMapActivity.emptyBalloonContent

    init {
        if (activity !is YandexMap)
            throw ImplementException(activity, YandexMap::class.java)
    }

    private fun runJavaScript(script: String) {
        var js = script
        js = JS + js

        if (LOG_ENABLED) {
            UtilsLog.d(NAME, "runJavaScript", "script == $js")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(js, null)
        } else {
            webView.loadUrl(js)
        }
    }

    fun setStartLocation(latitude: Double, longitude: Double) {
        runJavaScript("setStartLocation($latitude, $longitude)")
    }

    fun setZoomIn() {
        runJavaScript("setZoomIn()")
    }

    fun setZoomOut() {
        runJavaScript("setZoomOut()")
    }

    fun setZoomToHouses() {
        runJavaScript("setZoom($ZOOM_HOUSES)")
    }

    fun setZoomToCities() {
        runJavaScript("setZoom($ZOOM_CITIES)")
    }

    @JavascriptInterface
    fun onRouteChange(distance: Int, time: Int) {
        activity.runOnUiThread { yandexMapActivity.onRouteChange(distance, time) }
    }

    @JavascriptInterface
    fun onMapCenterChange(text: String, title: String, subtitle: String,
                          latitude: Double, longitude: Double) {
        activity.runOnUiThread { yandexMapActivity.onMapCenterChange(text, title, subtitle, latitude, longitude) }
    }

    @JavascriptInterface
    fun onGeolocationStart() {
        activity.runOnUiThread { yandexMapActivity.onGeolocationStart() }
    }

    @JavascriptInterface
    fun onGeolocationFinish() {
        activity.runOnUiThread { yandexMapActivity.onGeolocationFinish() }
    }

    @JavascriptInterface
    fun onEndLoading(hasError: Boolean) {
        activity.runOnUiThread { yandexMapActivity.onEndLoading(hasError) }
    }

    @JavascriptInterface
    fun onErrorConstructRoute() {
        activity.runOnUiThread { yandexMapActivity.onErrorConstructRoute() }
    }

    @JavascriptInterface
    fun onErrorSearchPoint() {
        activity.runOnUiThread { yandexMapActivity.onErrorSearchPoint() }
    }

    @JavascriptInterface
    fun onErrorGeolocation() {
        activity.runOnUiThread { yandexMapActivity.onErrorGeolocation() }
    }
}