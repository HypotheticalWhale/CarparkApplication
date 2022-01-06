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
import androidx.core.content.ContextCompat;

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
import com.google.android.gms.maps.model.ButtCap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
/**
 * This class implements the Route Planner Page of the application
 */
public class RoutePlannerActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int GPS_CHECK_REQUEST_CODE = 2;
    private GoogleMap gMap;
    private ApiInterface apiInterface;
    private List<LatLng> polylinelist;
    private PolylineOptions polylineOptions;
    private LatLng ori, dest;

    private Button locationButton;
    private GPSValidator gpsValidator;
    private PlacesClient placesClient;
    private AutocompleteSupportFragment locationInput, locationInput2;
    private LatLng inputCoordinates;
    private LatLng inputDestination;
    private boolean isGranted;
    private String apiKey;


    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_planner);
        apiKey = getString(R.string.search_api_key);

        Retrofit retrofit = new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl("https://maps.googleapis.com/")
                .build();
        apiInterface = retrofit.create(ApiInterface.class);

        this.initialiseUI();
        this.locationSearchSetup();
        this.getIntentData();
        gpsValidator.getCurrentLocation(locationCallback);
    }

    /**
     * The method is use to retrieve the destination
     * passed from the previous page and store it as a global variable
     */
    public void getIntentData(){
        if (getIntent().hasExtra("destination")){
            dest = getIntent().getParcelableExtra("destination");
            locationInput2.setText(getIntent().getStringExtra("destAddress"));
        }
    }

    /**
     * This methid perform initial map setup when the map is ready to use
     * @param googleMap the map involve
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        gMap.getUiSettings().setZoomControlsEnabled(true);
        gMap.getUiSettings().setScrollGesturesEnabled(true);
        gMap.getUiSettings().setZoomGesturesEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
    }

    /**
     * This method initialise the UI element of this page
     */
    public void initialiseUI() {
        locationButton = findViewById(R.id.locationButton2);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gpsValidator.getCurrentLocation(locationCallback);
            }
        });

        SupportMapFragment homeMap = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.routemap);
        homeMap.getMapAsync(this);
    }

    /**
     * This method is use to get the direction of 2 location that are stored in ori and dest global variable
     * It send a request to google direction API with the 2 location which will return a route between them
     * The route will then be display on the map
     */
    private void getDirection() {
        if(dest==null || ori==null){return;}
        String origin = ori.latitude + "," + ori.longitude;
        String destination = dest.latitude + "," + dest.longitude;
        apiInterface.getDirection("driving", "less_driving", origin, destination,
                getString(R.string.search_api_key))
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Result>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@NonNull Result result) {
                        gMap.clear();
                        polylinelist = new ArrayList<>();
                        List<Route> routeList = result.getRoutes();

                        for (Route route : routeList) {
                            String polyline = route.getOverviewPolyline().getPoints();
                            polylinelist.addAll(decodePoly(polyline));
                        }
                        polylineOptions = new PolylineOptions();
                        //placeholder colour
                        polylineOptions.color(ContextCompat.getColor(getApplicationContext(), R.color.blue));
                        polylineOptions.width(25);
                        polylineOptions.startCap(new ButtCap());
                        polylineOptions.endCap(new ButtCap());
                        polylineOptions.jointType(JointType.ROUND);
                        polylineOptions.addAll(polylinelist);
                        gMap.addPolyline(polylineOptions);

                        MarkerOptions markerStart = new MarkerOptions();
                        markerStart.position(ori);
                        MarkerOptions markerEnd = new MarkerOptions();
                        markerEnd.position(dest);
                        gMap.addMarker(markerStart);
                        gMap.addMarker(markerEnd);

                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(ori);
                        builder.include(dest);
                        gMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.d("api", "API request failed " + e.getMessage());
                    }
                });
    }
    /**
     * This method is use to decode the polyline which is returned from google direction API result
     */
    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int latitude = 0, longitude = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            latitude += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            longitude += dlng;

            LatLng p = new LatLng((((double) latitude / 1E5)), (((double) longitude / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    /**
     * This method setup the starting and destination location input bar and setup the location callback action
     */
    public void locationSearchSetup() {
        gpsValidator = new GPSValidator(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {

                super.onLocationResult(locationResult);
                //when successfully get the location it will display it on the location input
                if (locationResult.getLocations().size() > 0) {
                    int index = locationResult.getLocations().size() - 1;
                    Location location = locationResult.getLocations().get(index);
                    double inLat = location.getLatitude();
                    double inLng = location.getLongitude();
                    ori = new LatLng(inLat, inLng);
                    displayLocation(locationResult.getLocations().get(index));
                    getDirection();

                }
                //turn off GPS location updates after done
                gpsValidator.turnOffLocationUpdates(locationCallback);
            };
        };

        locationInput = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.locationInput1);
        locationInput.setCountries("SG");
        if (locationInput != null)
        {
            locationInput.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            locationInput.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    Log.i("locationautofill", "Place: " + place.getName() + ", " + place.getId() + place.getLatLng());
                    inputCoordinates = place.getLatLng();
                    double inLat = inputCoordinates.latitude;
                    double inLng = inputCoordinates.longitude;
                    ori = new LatLng(inLat, inLng);
                    getDirection();
                    if (dest == null)
                    {
                        moveCamtoMark(place.getLatLng(), place.getName());
                    }
                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.i("locationautofill", "An error occurred: " + status);
                }
            });
        }
        locationInput2 = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.locationInput2);
        locationInput2.setCountries("SG");
        if (locationInput2 != null)
        {
            locationInput2.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            locationInput2.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    Log.i("locationautofill", "Place: " + place.getName() + ", " + place.getId() + place.getLatLng());
                    inputDestination = place.getLatLng();
                    double destLat = inputDestination.latitude;
                    double destLng = inputDestination.longitude;
                    dest = new LatLng(destLat, destLng);
                    getDirection();
                    if (ori == null)
                    {
                        moveCamtoMark(place.getLatLng(), place.getName());
                    }
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
        if(gMap != null) {
            gMap.clear();
            gMap.addMarker(marker);
            gMap.animateCamera(cameraUpdate);
        }
    }

    /**
     * This method display the given location on the map and the starting location input bar
     * @param location to display
     */
    public void displayLocation(Location location){
        //display location on the input bar
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
                moveCamtoMark(cur_loc, "Current");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
        }
    }
}