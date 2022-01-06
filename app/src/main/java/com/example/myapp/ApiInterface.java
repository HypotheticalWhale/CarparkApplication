package com.example.myapp;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * This interface implement the resultant query output from Google Direction API
 */
public interface ApiInterface {
    @GET("maps/api/directions/json")
    Single<Result> getDirection(@Query("mode") String mode,
                                @Query("transit_routing_preference") String preference,
                                @Query("origin") String origin,
                                @Query("destination") String destination,
                                @Query("key") String key);
}
