package com.example.myapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.Calendar;
import org.w3c.dom.Text;

/**
 * This class implement the Carpark Information Page of the application
 */
public class CarparkInformationActivity extends AppCompatActivity implements OnMapReadyCallback {
    private LatLng locationCoord;
    private Button CalRateButton, directionButton;
    private String carparkNum;
    private String carparkLotsAvail;
    private String carparkLotsType;
    private String carparkLotsTotal;
    private String[] carparkInfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carpark_information);
        this.getIntentData();
        this.getInforFromDB();
        this.initialiseUI();
        this.displayInfo();
    }

    /**
     * The method is use to retrieve the carpark number, lots availability, lots type and lots total
     * passed from the previous page and store them as global variables
     */
    public void getIntentData(){
        if (getIntent().hasExtra("carpark_no")){
            carparkNum = getIntent().getStringExtra("carpark_no");
            carparkLotsAvail = getIntent().getStringExtra("lots_avail");
            carparkLotsType = getIntent().getStringExtra("lots_type");
            carparkLotsTotal = getIntent().getStringExtra("lots_total");
        }
    }

    /**
     * This method initialise the UI element of this page
     */
    public void initialiseUI(){
        SupportMapFragment homeMap = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.CarparkInfoMap);
        homeMap.getMapAsync(this);
        CalRateButton = findViewById(R.id.btn_CalRate);
        CalRateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView txtDuration = (TextView) findViewById(R.id.txtv_userDuration);
                TextView txtTotalPrice = (TextView) findViewById(R.id.txtv_TotalPrice);
                String strDuration = txtDuration.getText().toString();
                double payment = calculateRate(strDuration);
                txtTotalPrice.setText(String.valueOf(payment));
            }
        });
        directionButton = findViewById(R.id.directionButton);
        directionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent toRoutePlanner = new Intent(CarparkInformationActivity.this, RoutePlannerActivity.class);
                toRoutePlanner.putExtra("destination", new LatLng(Double.parseDouble(carparkInfo[2]), Double.parseDouble(carparkInfo[3])));
                toRoutePlanner.putExtra("destAddress", carparkInfo[1]);
                startActivity(toRoutePlanner);
            }
        });
    }

    /**
     * This method calculate the total cost of parking at the carpark based on the user input and the carpark rate
     * The carpark rate is determined based on the day of the week
     * @param strDuration the duration to park
     * @return the cost calculated
     */
    public double calculateRate(String strDuration){
        double totalCost = 0;
        double CPrate;
        String dayofweek = getDay();
        boolean within_range = checkWithinTimeRange();
        // if not within normal hours, pass off-hours value to rate calculator
        if(dayofweek.equals("sun") || !within_range) {
            CPrate = Double.parseDouble(carparkInfo[13]);
        }
        else {
            CPrate = Double.parseDouble(carparkInfo[12]);
        }

        if(strDuration.matches("^[+-]?(\\d*\\.)?\\d+$")) {
            double duration = Double.parseDouble(strDuration);
            // CPrate multiply by 2 due to CP charging in rate/half-hr, system uses rate/hr
            totalCost = (double) Math.round((duration * (CPrate*2))*100.0)/100.0;
        }
        else{
            Toast.makeText(CarparkInformationActivity.this, "Please enter only Numbers", Toast.LENGTH_LONG).show();
        }
        return totalCost;
    }

    /**
     * This method checks the current day
     * @return the current day in short form
     */
    public String getDay(){
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        String strDay;
        switch (day) {
            case Calendar.SUNDAY:
                strDay = "sun";
                break;
            case Calendar.MONDAY:
                strDay = "mon";
                break;
            case Calendar.TUESDAY:
                strDay = "tues";
                break;
            case Calendar.WEDNESDAY:
                strDay = "wed";
                break;
            case Calendar.THURSDAY:
                strDay = "thur";
                break;
            case Calendar.FRIDAY:
                strDay = "fri";
                break;
            case Calendar.SATURDAY:
                strDay = "sat";
                break;
            default:
                strDay = "error!";
        }
        return strDay;
    }

    /**
     * @return true if the current time is between 7am - 5pm
     * else return false
     */
    public boolean checkWithinTimeRange() {
        Calendar currTime = Calendar.getInstance();
        int ampm = currTime.get(Calendar.AM_PM);
        int hour = currTime.get(Calendar.HOUR_OF_DAY);
        if(ampm == 0 && hour >= 7){
            return true;
        }
        else if (ampm == 1 && hour <= 17){
            return true;
        }
        return false;
    }

    /**
     * This method is use to retrieve carpark information from the database and store them in global variable
     * It also set the carpark rate based on the
     */
    public void getInforFromDB(){
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        carparkInfo = databaseHelper.getCarparkInfo(carparkNum);
    }

    /**
     * This method display all the information retrieved from the database into the textview on the page
     */
    public void displayInfo(){
        TextView output = findViewById(R.id.address);
        output.setText(carparkInfo[1]);
        output = findViewById(R.id.carparkType);
        output.setText(carparkInfo[4]);
        output = findViewById(R.id.typeOfParkingSystem);
        output.setText(carparkInfo[5]);
        output = findViewById(R.id.shortTermParking);
        output.setText(carparkInfo[6]);
        output = findViewById(R.id.freeParking);
        output.setText(carparkInfo[7]);
        output = findViewById(R.id.nightParking);
        output.setText(carparkInfo[8]);
        output = findViewById(R.id.carparkDecks);
        output.setText(carparkInfo[9]);
        output = findViewById(R.id.gantryHeigh);
        output.setText(carparkInfo[10]);
        output = findViewById(R.id.carparkBasement);
        output.setText(carparkInfo[11]);
        output = findViewById(R.id.normalHourRate);
        output.setText(Double.toString(Double.parseDouble(carparkInfo[12])*2));
        output = findViewById(R.id.otherHourRate);
        output.setText(Double.toString(Double.parseDouble(carparkInfo[13])*2));
        output = findViewById(R.id.totalLots);
        output.setText(carparkLotsTotal);
        output = findViewById(R.id.lotType);
        output.setText(carparkLotsType);
        output = findViewById(R.id.lotsAvailable);
        output.setText(carparkLotsAvail);
    }

    /**
     * This methid perform initial map setup when the map is ready to use
     * @param googleMap the map involve
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.getUiSettings().setScrollGesturesEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);

        locationCoord = new LatLng(Double.parseDouble(carparkInfo[2]), Double.parseDouble(carparkInfo[3]));
        this.moveCamtoMark(googleMap, locationCoord, carparkInfo[1]);
    }

    /**
     * This method move the camera on the map to the carpark location and pinpoint a marker on it
     * @param gMap the google map
     * @param coords the coordinated to move the camera to
     * @param locName the location name to place on the marker
     */
    public void moveCamtoMark(GoogleMap gMap, LatLng coords, String locName){
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(coords, 17);
        MarkerOptions marker = new MarkerOptions();
        marker.title(locName);
        marker.position(coords);
        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        gMap.addMarker(marker);
        gMap.animateCamera(cameraUpdate);
    }
}