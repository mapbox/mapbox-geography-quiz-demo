package com.mapbox.tappergeochallenge;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WikidataRetrofitService {

  @GET("/sparql")
  Call<String> getQuery(@Query("query") String query);

}