package ru.p3tr0vich.fuel.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

public class LocationHelper {

    private static final int REQUEST_LOCATION_TIMEOUT = 60000;

    private final Activity mActivity;

    private final GoogleApiClient mGoogleApiClient;

    private final LocationRequest mLocationRequest;

    private final LocationSettingsRequest mLocationSettingsRequest;

    private boolean mRequestLocationUpdatesInProcess;

    private final Handler mHandler;

    private int mRequestCodePermissionAccessFineLocation;

    private LocationHelperListener mLocationHelperListener;

    public interface LocationHelperListener {
        void onLastLocationReturn(@Nullable Location location);

        void onUpdatedLocationReturn(@Nullable Location location);

        void onResolutionRequired(@NonNull Status status);
    }

    public LocationHelper(@NonNull Activity activity) {
        mActivity = activity;

        mHandler = new Handler();

        mLocationHelperListener = null;

        mGoogleApiClient = new GoogleApiClient.Builder(mActivity)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1)
                .setInterval(1000)
                .setFastestInterval(1000);

        mLocationSettingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest)
                .setAlwaysShow(true)
                .build();
    }

    @NonNull
    public LocationHelper setRequestCodePermissionAccessFineLocation(int requestCodePermissionAccessFineLocation) {
        mRequestCodePermissionAccessFineLocation = requestCodePermissionAccessFineLocation;
        return this;
    }

    public void connect() {
        mGoogleApiClient.connect();
    }

    public void disconnect() {
        mHandler.removeCallbacks(mLocationTimeoutRunnable);
        mGoogleApiClient.disconnect();
    }

    @NonNull
    public LocationHelper setLocationHelperListener(@Nullable LocationHelperListener locationHelperListener) {
        mLocationHelperListener = locationHelperListener;
        return this;
    }

    public int isGooglePlayServicesAvailable() {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mActivity);
    }

    public void showErrorNotification(Context context, int errorCode) {
        GoogleApiAvailability.getInstance().showErrorNotification(context, errorCode);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }

        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        mActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, mRequestCodePermissionAccessFineLocation);

        return false;
    }

    public void getLocation() {
        if (!checkPermission()) return;

        if (mLocationHelperListener != null) {
            mLocationHelperListener.onLastLocationReturn(
                    LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient));
        }

        LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, mLocationSettingsRequest)
                .setResultCallback(mLocationSettingsResultResultCallback);
    }

    private final ResultCallback<LocationSettingsResult> mLocationSettingsResultResultCallback =
            new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                    final Status status = locationSettingsResult.getStatus();

                    if (status == null) return;

                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            if (!checkPermission()) return;

                            LocationServices.FusedLocationApi.requestLocationUpdates(
                                    mGoogleApiClient, mLocationRequest, mLocationListener)
                                    .setResultCallback(mStatusResultCallback);

                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            if (mLocationHelperListener != null) {
                                mLocationHelperListener.onResolutionRequired(status);
                            }

                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            break;
                    }
                }
            };

    private final Runnable mLocationTimeoutRunnable = new Runnable() {
        public void run() {
            mRequestLocationUpdatesInProcess = false;

            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener);

            if (mLocationHelperListener != null)
                mLocationHelperListener.onUpdatedLocationReturn(null);
        }
    };

    private final ResultCallback<Status> mStatusResultCallback = new ResultCallback<Status>() {
        @Override
        public void onResult(@NonNull Status status) {
            if (status.isSuccess()) {
                mRequestLocationUpdatesInProcess = true;

                mHandler.postDelayed(mLocationTimeoutRunnable, REQUEST_LOCATION_TIMEOUT);
            }
        }
    };

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mRequestLocationUpdatesInProcess = false;

            mHandler.removeCallbacks(mLocationTimeoutRunnable);

            if (mLocationHelperListener != null)
                mLocationHelperListener.onUpdatedLocationReturn(location);
        }
    };

    public boolean isRequestLocationUpdatesInProcess() {
        return mRequestLocationUpdatesInProcess;
    }
}