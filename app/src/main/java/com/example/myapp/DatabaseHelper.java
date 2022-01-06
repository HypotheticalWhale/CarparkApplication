package com.example.myapp;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
/**
 * This class implement the helper class to access the database
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "jjjys.db";
    private static final int DATABASE_VERSION=5;

    private static final String CARPARK_CSV_NAME="hdb_carpark_info.csv";
    private static final String CARPARK_TABLE="carpark_info";
    //Table columns
    //car_park_no,address,latitude,longitude,car_park_type,
    // type_of_parking_system,short_term_parking,free_parking,
    // night_parking,car_park_decks,gantry_height,car_park_basement,
    // normal_hours,other_hours

    public static final int CARPARK_COLUMN = 14; //exclude id
    public static final int CARPARK_ROW = 2162;

    private Context context;

    public DatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        this.csvtoDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE "+ CARPARK_TABLE +" (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "car_park_no TEXT, address text, latitude text, longitude text, car_park_type text, " +
                "type_of_parking_system text, short_term_parking text, " +
                "free_parking text, night_parking text, car_park_decks text, " +
                "gantry_height text, car_park_basement text, normal_hours text, other_hours text)";
        db.execSQL(query);
        Log.d(TAG, "Created carpark table");
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + CARPARK_TABLE);
        Log.d(TAG, "Updated database");
        onCreate(db);
    }

    /**
     * This method query the database to get all carpark information of a single carpark
     * @param carparkNum the carpark number to get information
     * @return the result from the query
     */
    public String[] getCarparkInfo(String carparkNum){
        //car_park_no,address,latitude,longitude,car_park_type,
        // type_of_parking_system,short_term_parking,free_parking,
        // night_parking,car_park_decks,gantry_height,car_park_basement,
        // normal_hours,other_hours
        SQLiteDatabase db = this.getReadableDatabase();
        String[] result = new String[CARPARK_COLUMN];
        Cursor cursor = null;

        if(db!=null){
            String query = "select * from carpark_info where car_park_no = \"" + carparkNum + "\"";
            cursor = db.rawQuery(query, null);
            if(cursor!=null){
                while(cursor.moveToNext()){
                    for(int i=0; i<CARPARK_COLUMN; i++){
                        result[i] = cursor.getString(i+1);
                    }
                }
            }
        }

        if (!cursor.isClosed()) {cursor.close();}
        if(db.isOpen()){db.close();}
        return result;
    }

    /**
     * This method query the database to get the nearest carpark based on the given latitude and longitude
     * It get the nearest carpark within 500m with a offset of 0.004 in latitude/longitude coordinates
     * @param latitude of a location to find nearest carpark
     * @param longitude of a location to find nearest carpark
     * @return the result from the query
     */
    public ArrayList<String[]> getNearestCarpark(double latitude, double longitude){
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<String[]> result = new ArrayList<String[]>();
        //get carpark within a radius of 500m
        double offset = 0.004;
        double fromLatitude = latitude - offset;
        double toLatitude = latitude + offset;
        double fromLongitude = longitude - offset;
        double toLongitude = longitude + offset;
        Cursor cursor = null;

        if(db!=null){
            String query = "select car_park_no, address, latitude, longitude from carpark_info where latitude " +
                    "between "+ fromLatitude +" and "+ toLatitude + " " +
                    "and longitude between "+ fromLongitude + " and "+ toLongitude + " LIMIT 6";
            cursor = db.rawQuery(query, null);
            if(cursor!=null){
                while(cursor.moveToNext()){
                    //car_park_no, address, latitude, longitude
                    result.add(new String[]{cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)});
                }
            }
        }
        if(!cursor.isClosed()){cursor.close();}
        if(db.isOpen()){db.close();}
        return result;
    }

    /**
     * This method query the database to get all the latitude, longitude, address and carpark number
     * @return the result from the query
     */
    public ArrayList<String[]> getLatLong(){
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<String[]> result = new ArrayList<String[]>();
        Cursor cursor = null;
        int count =0;

        if(db!=null){
            String query = "SELECT car_park_no, address, latitude, longitude from "+CARPARK_TABLE;
            cursor = db.rawQuery(query, null);
            if(cursor!=null){
                while(cursor.moveToNext()){
                    //car_park_no, address, latitude, longitude
                    result.add(new String[]{cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)});
                    if(count==CARPARK_ROW){break;}
                    count+=1;
                }
            }
        }else{Log.d(TAG, "Database not found/failed to open");}

        if(!cursor.isClosed()){cursor.close();}
        if(db.isOpen()){db.close();}
        return result;
    }


    public long addSingleEntry(SQLiteDatabase db,String[] columnNames, String[] values){
        ContentValues cv = new ContentValues();
        for(int i=0; i<CARPARK_COLUMN; i++) {
            cv.put(columnNames[i], values[i]);
        }
        long result = db.insert(CARPARK_TABLE, null, cv);
        return result;
    }

    public int getNumRow(@NonNull SQLiteDatabase db){
        String query = "SELECT COUNT(*) FROM " + CARPARK_TABLE;
        Cursor cursor = db.rawQuery(query, null);
        int count = 0;
        if(cursor!=null){
            cursor.moveToNext();
            count = cursor.getInt(0);
        }
        if(!cursor.isClosed()){cursor.close();}
        return count;
    }

    /**
     * This method store all the data in the csv into the database
     * It is only perform when the database is empty which is during the first time use
     */
    public void csvtoDatabase(){
        SQLiteDatabase db = this.getWritableDatabase();
        if(db==null){Log.d(TAG, "Database not found/failed to open");}

        int count =0;
        if (this.getNumRow(db)<CARPARK_ROW) { //empty database
            db.execSQL("DELETE FROM " + CARPARK_TABLE);
            Log.d(TAG, "Empty carpark infoamtion table, adding from csv");
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = null;

            try {
                inputStream = assetManager.open(CARPARK_CSV_NAME);
                Log.d(TAG, "csv file found");
                CSVReader reader = new CSVReader(new InputStreamReader(inputStream));
                String[] allColumnName = reader.readNext(); //store column name
                String[] nextLine;
                nextLine = reader.readNext();
                while (nextLine != null) {
                    count += 1;
                    if(this.addSingleEntry(db, allColumnName, nextLine)==-1){
                        Log.d(TAG, "Failed to insert "+nextLine[0]);
                        break;
                    }
                    nextLine = reader.readNext();
                }
                reader.close();
                inputStream.close();
                Log.d(TAG, "Inserted "+count+" entry");
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "File not found");
            }
        }
        else{Log.d(TAG, "Carpark information table exists");}
        if(db.isOpen()){db.close();}
    }

}
