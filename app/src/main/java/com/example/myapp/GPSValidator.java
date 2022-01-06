package com.example.myapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/**
 * This class implement the GPS related functionality such as check location permission,
 * check GPS enable and get the current user location
 */

public class GPSValidator {
    private Context context;

    private LocationRequest locationRequest;
    private LocationManager locationManager;
    private FusedLocationProviderClient fusedLPC;


    public GPSValidator(Context context) {
        this.context = context;
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * This method is use to get the current location of user device
     * It check that location permission is granted and GPS is turn on first
     * before it is able to get the current location
     * @param locationCallback to determine the action to perform after successfully acquired the user location
     */
    @SuppressLint("MissingPermission")
    public void getCurrentLocation(LocationCallback locationCallback) {
        LatLng coords = new LatLng( 0 ,0);
        if (!this.checkPermissionGPS()) {
            return;
        }
        fusedLPC = LocationServices.getFusedLocationProviderClient(context);
        fusedLPC.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /**
     * This method check if location permission is granted and that GPS is enabled
     * @return false if either location not granted or GPS not enabled
     */
    public boolean checkPermissionGPS(){
        //check for location permission, if permission not granted then it will prompt to enable permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity)context,new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, HomepageActivity.LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }

        //check for gps enabled
        if(!checkGpsEnabled()){
            return false;
        }
        return true;
    }

    /**
     * This method check if GPS is enabled
     * @return false if it is not enabbled
     */
    public boolean checkGpsEnabled(){
        boolean gpsEnabled;
        gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        //it perform a request for gps using try and catch
        //if exception occur in try then it will catch and prompt the user to enable gps
        if(!gpsEnabled){
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
            builder.addLocationRequest(locationRequest);
            builder.setAlwaysShow(true);

            Task<LocationSettingsResponse> locationSettingsResponseTask = LocationServices.getSettingsClient(context.getApplicationContext()).checkLocationSettings(builder.build());
            locationSettingsResponseTask.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                @Override
                public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                    try {
                        LocationSettingsResponse response = task.getResult(ApiException.class);
                        Toast.makeText(context, "GPS is already enabled", Toast.LENGTH_SHORT).show();
                    } catch (ApiException e) {
                        e.printStackTrace();
                        //request location from device
                        if (e.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                resolvableApiException.startResolutionForResult((Activity)context, HomepageActivity.GPS_CHECK_REQUEST_CODE);
                            } catch (IntentSender.SendIntentException sendIntentException) {
                                sendIntentException.printStackTrace();
                            }
                        } else if (e.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                            Toast.makeText(context, "GPS not available", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }
        return gpsEnabled;
    }

    public void turnOffLocationUpdates(LocationCallback locationCallback){
        fusedLPC.removeLocationUpdates(locationCallback);
    }
}
