package com.example.myapp;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * This class implements the Home Page of the application
 */
public class HomepageActivity extends AppCompatActivity implements OnMapReadyCallback {
    public final static int LOCATION_PERMISSION_REQUEST_CODE = 1;
    public final static int GPS_CHECK_REQUEST_CODE = 2;
    private boolean isGranted;
    private AutocompleteSupportFragment locationInput;
    private Button locationButton, nearestCarparkButton, routePlannerButton, bookmarkButton;
    private GPSValidator gpsValidator;
    private LocationCallback locationCallback;
    private PlacesClient placesClient;
    private String apiKey;
    private GoogleMap gMap;
    private LatLng inputCoordinates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);
        apiKey = getString(R.string.search_api_key);
        this.initialiseUI();
        this.locationSearchSetup();
        isGranted = gpsValidator.checkPermissionGPS();

        if (isGranted) {
            if (checkGooglePlayService()) {
                Toast.makeText(this, "Google Play Services available", Toast.LENGTH_SHORT).show();
                SupportMapFragment homeMap = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.MapFUserInput);
                homeMap.getMapAsync(this);
                gpsValidator.getCurrentLocation(locationCallback);
            } else {
                Toast.makeText(this, "Google Play Services Unavailable", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * This method check if google play services is installed in the user device
     * As it is required to use google rated services such as map
     * @return true if google play services is installed
     */
    private boolean checkGooglePlayService() {
        GoogleApiAvailability mapAPI = GoogleApiAvailability.getInstance();
        int result = mapAPI.isGooglePlayServicesAvailable(this);
        if (result == ConnectionResult.SUCCESS) {
            return true;
        } else if (mapAPI.isUserResolvableError(result)) {
            Dialog mapDialog = mapAPI.getErrorDialog(this, result, 201, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    Toast.makeText(HomepageActivity.this, "User Cancelled Dialog", Toast.LENGTH_SHORT).show();
                }
            });
            mapDialog.show();
        }
        return false;
    }

    /**
     * This methid perform initial map setup when the map is ready to use
     * @param googleMap the map involve
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setScrollGesturesEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap.setMyLocationEnabled(true);
        populateMap();
    }

    /**
     * This method populate the map with all the carparks using markers to pinpoint them
     */
    private void populateMap() {
        DatabaseHelper databaseHelper;
        databaseHelper = new DatabaseHelper(this);
        ArrayList<String[]> carparks = databaseHelper.getLatLong();
        MarkerOptions marker = new MarkerOptions();
        LatLng coords;
        int i = 0;
        while( i < carparks.size())
        {
            coords = new LatLng(Double.parseDouble(carparks.get(i)[2]), Double.parseDouble(carparks.get(i)[3]));
            marker.title(carparks.get(i)[1]);
            marker.position(coords);
            gMap.addMarker(marker);
            i++;
        }
    }

    /**
     * This method handle the result from prompting user to enable location permission
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
                isGranted = true;
                gpsValidator.getCurrentLocation(locationCallback);
                SupportMapFragment homeMap = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.MapFUserInput);
                homeMap.getMapAsync(this);
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * This method handle the result from prompting user to enable GPS
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //the return from turn on GPS prompt
        if (requestCode == GPS_CHECK_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "GPS enabled", Toast.LENGTH_SHORT).show();
                gpsValidator.getCurrentLocation(locationCallback);
                SupportMapFragment homeMap = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.MapFUserInput);
                homeMap.getMapAsync(this);
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "GPS required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * This method initialise the UI element of this page
     */
    public void initialiseUI(){
        locationButton = findViewById(R.id.locationButton);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gpsValidator.getCurrentLocation(locationCallback);
            }
        });
        nearestCarparkButton = findViewById(R.id.btnNearestCP);
        nearestCarparkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent toNearestCarpark = new Intent(HomepageActivity.this, NearestCarparkActivity.class);
                toNearestCarpark.putExtra("Latitude", inputCoordinates.latitude);
                toNearestCarpark.putExtra("Longitude", inputCoordinates.longitude);
                startActivity(toNearestCarpark);
            }
        });


        routePlannerButton = findViewById(R.id.btnRoutePlanner);
        routePlannerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent toRoutePlanner = new Intent(HomepageActivity.this, RoutePlannerActivity.class);
                startActivity(toRoutePlanner);
            }
        });
    }

    /**
     * This method setup the location input bar and setup the location callback action
     */
    public void locationSearchSetup(){
        gpsValidator = new GPSValidator(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //when successfully get the location it will display it on the location input
                if(locationResult.getLocations().size() > 0){
                    int index = locationResult.getLocations().size()-1;
                    displayLocation(locationResult.getLocations().get(index));
                }
                //turn off GPS location updates after done
                gpsValidator.turnOffLocationUpdates(locationCallback);
            }
        };

        if(!Places.isInitialized()){
            Places.initialize(getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(this);
        locationInput  = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.locationInput);
        locationInput.setCountries("SG");

        if (locationInput != null) {
            locationInput.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            locationInput.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    Log.i("locationautofill", "Place: " + place.getName() + ", " + place.getId() + place.getLatLng());
                    inputCoordinates = place.getLatLng();
                    moveCamtoMark(place.getLatLng(), place.getName());
                }
                @Override
                public void onError(@NonNull Status status) {
                    Log.i("locationautofill", "An error occurred: " + status);
                }
            });
        }
    }

    /**
     * This method move the camera on the map to the carpark location and pinpoint a marker on it
     * @param coords the coordinated to move the camera to
     * @param locName the location name to place on the marker
     */
    public void moveCamtoMark(LatLng coords, String locName){
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(coords, 15);
        MarkerOptions marker = new MarkerOptions();
        marker.title(locName);
        marker.position(coords);
        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        if(gMap != null) {
            gMap.addMarker(marker);
            gMap.animateCamera(cameraUpdate);
        }
    }

    /**
     * This method display the given location on the map and the location input bar
     * @param location to display
     */
    public void displayLocation(Location location){
        if(location!=null) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                double latitude =  location.getLatitude();
                double longitude = location.getLongitude();

                locationInput.setText(addresses.get(0).getAddressLine(0));

                LatLng cur_loc = new LatLng(latitude, longitude);
                inputCoordinates = cur_loc;
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(cur_loc, 15);
                if(gMap != null) {
                    gMap.animateCamera(cameraUpdate);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
        }
    }
}