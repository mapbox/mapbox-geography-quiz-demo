package com.mapbox.tappergeochallenge;

import com.mapbox.services.commons.geojson.Feature;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface WikidataRetrofitService {


  @GET("User/{username}")
  Call<Feature> getCities(@Path("username") String userName, @Query("access_token") String accessToken);





}
