package com.example.myapp;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * This class implements the Nearest Carpark page of the application
 */
public class NearestCarparkActivity extends AppCompatActivity implements OnMapReadyCallback {
    private DatabaseHelper databaseHelper;
    private Circle circle;
    private LatLng locationCoord;
    private Button newbtn;
    private Button refresh;
    private Button back;
    private String[] addressarray;

    LinearLayout linearLayout;
    ArrayList<String[]> carparks;//0-car_park_no, 1-address, 2-latitude, 3-longitude
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearest_carpark);
        this.initialiseIntent();
        this.mapSetup();
    }

    /**
     * This method is use to initialise the homepageButton
     */
//    public void homepageButton(){
//        back = findViewById(R.id.back);
//        back.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent toHome = new Intent(NearestCarparkActivity.this,HomepageActivity.class);
//                startActivity(toHome);
//            }
//        });
//    }

    public void mapSetup(){
        SupportMapFragment homeMap = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView1);
        homeMap.getMapAsync(this);
    }

    /**
     * This method is use to retrieve latitude and longitude passed from the previous page
     * and store as a global variable locationCoord
     */
    public void initialiseIntent(){
        if(getIntent().hasExtra("Latitude")){
            locationCoord = new LatLng(getIntent().getDoubleExtra("Latitude", 0), getIntent().getDoubleExtra("Longitude",0));
            Log.d("NearestCarpark", "Successfully get Latitude " +locationCoord.latitude+ " and Longitude "+locationCoord.longitude);
        }
    }

    /**
     * This methid perform initial map setup when the map is ready to use
     * @param googleMap the map involve
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setScrollGesturesEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);

        this.moveCamtoMark(googleMap, locationCoord, "Current");
        this.FindNearestCP(googleMap, locationCoord.latitude, locationCoord.longitude);
        refresh = findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent toRefresh = new Intent(NearestCarparkActivity.this, NearestCarparkActivity.class);
                toRefresh.putExtra("Latitude", locationCoord.latitude);
                toRefresh.putExtra("Longitude",locationCoord.longitude);
                startActivity(toRefresh);
            }
        });
    }

    /**
     * This method move the camera on the map to the carpark location and pinpoint a marker on it
     * @param gMap the google map
     * @param coords the coordinated to move the camera to
     * @param locName the location name to place on the marker
     */
    public void moveCamtoMark(GoogleMap gMap, LatLng coords, String locName){
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(coords, 15);
        MarkerOptions marker = new MarkerOptions();
        marker.title(locName);
        marker.position(coords);
        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        circle = gMap.addCircle(new CircleOptions().radius(500).strokeColor(Color.BLUE).center(coords));
        gMap.addMarker(marker);
        gMap.animateCamera(cameraUpdate);
    }

    /**
     * This method is use to find the nearest carpark given the latitude and longitude
     * and display those carpark on the map
     * It also get the availabilty of these carpark from data.gov.sg and display them
     * @param gMap the map to display the carparks
     * @param latitude of the location to find the nearest carpark
     * @param longitude of the location to find the nearest carpark
     */
    private void FindNearestCP(GoogleMap gMap, double latitude, double longitude) {
        LinearLayout lll = new LinearLayout(this);
        databaseHelper = new DatabaseHelper(this);
        carparks = databaseHelper.getNearestCarpark(latitude, longitude);
        MarkerOptions marker = new MarkerOptions();
        LatLng coords;
        linearLayout = findViewById(R.id.nearbyLL);
        int i = 0;
        while( i < carparks.size())
        {
            coords = new LatLng(Double.parseDouble(carparks.get(i)[2]), Double.parseDouble(carparks.get(i)[3]));
            marker.title(carparks.get(i)[1]);
            marker.position(coords);
            gMap.addMarker(marker);
            i++;
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.data.gov.sg/v1/transport/carpark-availability";
        // Request a string response from the provided URL.
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                JSONArray items, carpark_data,car2;
                JSONObject itemObj,car1,car3;
                String carparkname,carpark_cpno;
                int x = 0;
                try {
                    items = response.getJSONArray("items");
                    itemObj = items.getJSONObject(0);
                    carpark_data = itemObj.getJSONArray("carpark_data");
                    // TODO: replace the 1 below with the number of JSONObjs inside carpark_data
                    int carpark_data_length = 1952;
                    // iterate through all of the JSONObjects in carpark_data and take the relevant ones
                    for(int i = 0; i < carpark_data_length; i++)
                    {
                        car1 = carpark_data.getJSONObject(i);
                        carpark_cpno = carpark_data.getJSONObject(i).getString("carpark_number");
                        car2 = car1.getJSONArray("carpark_info");
                        String lotAvail = car2.getJSONObject(0).getString("lots_available");
                        String LotType = car2.getJSONObject(0).getString("lot_type");
                        String TotalLots = car2.getJSONObject(0).getString("total_lots");

                        int j=0;
                        while(j < carparks.size()){
                            carparkname = carparks.get(j)[0];
                            if(carparkname.equals(carpark_cpno)){
                                addressarray = databaseHelper.getCarparkInfo(carparkname);
                                newbtn = new Button(NearestCarparkActivity.this);
                                newbtn.setText("Carpark: " + addressarray[1]  + "  \nAvailable Lots: " + lotAvail);
                                newbtn.setId(x+1);
                                String finalCarparkname = carparkname;
                                newbtn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent toCarparkAvailability = new Intent(NearestCarparkActivity.this,CarparkInformationActivity.class);
                                        toCarparkAvailability.putExtra("carpark_no", finalCarparkname);
                                        toCarparkAvailability.putExtra("lots_avail", lotAvail);
                                        toCarparkAvailability.putExtra("lots_type", LotType);
                                        toCarparkAvailability.putExtra("lots_total", TotalLots);
                                        startActivity(toCarparkAvailability);
                                    }
                                });
                                linearLayout.addView(newbtn);
                            }
                            j++;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(NearestCarparkActivity.this, "Response not recieved!", Toast.LENGTH_LONG).show();
            }
        });
        queue.add(request);
    }
}