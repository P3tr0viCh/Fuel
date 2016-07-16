package ru.p3tr0vich.fuel.helpers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
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

    private final Context mContext;

    private final GoogleApiClient mGoogleApiClient;

    private final LocationRequest mLocationRequest;

    private final LocationSettingsRequest mLocationSettingsRequest;

    private boolean mRequestLocationUpdatesInProcess;

    private final Handler mHandler;

    private LocationHelperListener mLocationHelperListener;

    public interface LocationHelperListener {
        void onLastLocationReturn(@Nullable Location location);

        void onUpdatedLocationReturn(@Nullable Location location);

        void onResolutionRequired(@NonNull Status status);
    }

    public LocationHelper(@NonNull Context context) {
        mContext = context;

        mHandler = new Handler();

        mLocationHelperListener = null;

        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
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
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext);
    }

//  Взято отсюда: GooglePlayServicesUtil.showErrorDialogFragment()
    @Nullable
    public String getConnectionResultTitle(int result) {
        return com.google.android.gms.common.internal.zzh.zzf(mContext, result);
    }

    @NonNull
    public String getConnectionResultMessage(int result) {
        String var11 = GooglePlayServicesUtil.zzbv(mContext);
        return com.google.android.gms.common.internal.zzh.zzc(mContext, result, var11);
    }

    /**
     * @return true if permission denied
     */
    private boolean checkSelfPermission() {
//        if (result) {
//        TODO: Consider calling
//        ActivityCompat#requestPermissions
//        here to request the missing permissions, and then overriding
//        public void onRequestPermissionsResult(int requestCode, String[] permissions,
//        int[] grantResults)
//        to handle the case where the user grants the permission. See the documentation
//        for ActivityCompat#requestPermissions for more details.
//        }

        return ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED;
    }

    public void getLocation() {
        if (checkSelfPermission()) return;

        if (mLocationHelperListener != null)
            mLocationHelperListener.onLastLocationReturn(
                    LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient));

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
                            if (checkSelfPermission()) return;

                            LocationServices.FusedLocationApi.requestLocationUpdates(
                                    mGoogleApiClient, mLocationRequest, mLocationListener)
                                    .setResultCallback(mStatusResultCallback);

                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            if (mLocationHelperListener != null)
                                mLocationHelperListener.onResolutionRequired(status);

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