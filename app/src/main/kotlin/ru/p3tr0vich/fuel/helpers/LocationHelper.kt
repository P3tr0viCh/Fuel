package ru.p3tr0vich.fuel.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Handler
import androidx.core.content.ContextCompat
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*

// todo
@Suppress("DEPRECATION")
class LocationHelper(private val activity: Activity,
                     private val locationHelperListener: LocationHelperListener,
                     private val requestCodePermissionAccessFineLocation: Int = 0) {

    companion object {
        private const val REQUEST_LOCATION_TIMEOUT = 60000
    }

    private val googleApiClient: GoogleApiClient = GoogleApiClient.Builder(activity)
            .addApi(LocationServices.API)
            .build()

    private val locationRequest: LocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setNumUpdates(1)
            .setInterval(1000)
            .setFastestInterval(1000)

    private val locationSettingsRequest: LocationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
            .build()

    private val handler = Handler()

    interface LocationHelperListener {
        fun onLastLocationReturn(location: Location?)

        fun onUpdatedLocationReturn(location: Location?)

        fun onResolutionRequired(status: Status)
    }

    var isRequestLocationUpdatesInProcess: Boolean = false
        private set

    val isGooglePlayServicesAvailable: Int
        get() = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)

    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 23) {
            return true
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        activity.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                requestCodePermissionAccessFineLocation)

        return false
    }

    @SuppressLint("MissingPermission")
    private val locationSettingsResultResultCallback = ResultCallback<LocationSettingsResult> { locationSettingsResult ->
        val status = locationSettingsResult.status ?: return@ResultCallback

        when (status.statusCode) {
            LocationSettingsStatusCodes.SUCCESS -> {
                if (!checkPermission()) return@ResultCallback

                LocationServices.FusedLocationApi.requestLocationUpdates(
                        googleApiClient, locationRequest, locationListener)
                        .setResultCallback(statusResultCallback)
            }
            LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->
                locationHelperListener.onResolutionRequired(status)
            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
            }
        }
    }

    private val locationTimeoutRunnable = Runnable {
        isRequestLocationUpdatesInProcess = false

        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, locationListener)

        locationHelperListener.onUpdatedLocationReturn(null)
    }

    private val statusResultCallback: ResultCallback<Status> = ResultCallback { status ->
        if (status.isSuccess) {
            isRequestLocationUpdatesInProcess = true

            handler.postDelayed(locationTimeoutRunnable, REQUEST_LOCATION_TIMEOUT.toLong())
        }
    }

    private val locationListener: LocationListener = LocationListener { location ->
        isRequestLocationUpdatesInProcess = false

        handler.removeCallbacks(locationTimeoutRunnable)

        locationHelperListener.onUpdatedLocationReturn(location)
    }

    fun connect() {
        googleApiClient.connect()
    }

    fun disconnect() {
        handler.removeCallbacks(locationTimeoutRunnable)
        googleApiClient.disconnect()
    }

    fun showErrorNotification(context: Context, errorCode: Int) {
        GoogleApiAvailability.getInstance().showErrorNotification(context, errorCode)
    }

    fun getLocation() {
        if (!checkPermission()) return

        locationHelperListener.onLastLocationReturn(
                LocationServices.FusedLocationApi.getLastLocation(googleApiClient))

        LocationServices.SettingsApi.checkLocationSettings(googleApiClient, locationSettingsRequest)
                .setResultCallback(locationSettingsResultResultCallback)
    }
}