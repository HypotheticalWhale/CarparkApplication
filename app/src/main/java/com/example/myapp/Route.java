package com.example.myapp;

/**
 * This class implement the Route entity which is form the Google direction API result
 */
public class Route {
    private OverviewPolyline overview_polyline;
    public OverviewPolyline getOverviewPolyline() { return overview_polyline;}
    public void setOverviewPolyline(OverviewPolyline overview_polyline) {
        this.overview_polyline = overview_polyline;
    }
}
