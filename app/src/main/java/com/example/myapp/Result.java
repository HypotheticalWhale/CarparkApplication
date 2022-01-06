package com.example.myapp;

import java.util.List;

/**
 * This class implement the Result entity which is form the Google direction API result
 */
public class Result {

    private List<Route> routes;
    private String status;

    public List<Route> getRoutes() { return routes; }
    public void setRoutes(List<Route> routes) { this.routes = routes; }
    public String getStatus() { return status; }
    public void setStatus(String status){this.status = status;}
}
