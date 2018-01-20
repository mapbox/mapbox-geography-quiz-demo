package com.mapbox.tappergeochallenge;

import retrofit2.Call;
import retrofit2.http.GET;

public interface WikidataRetrofitService {

  @GET("y7avyj4v")
  Call<String> getRetrofitQuery();

}