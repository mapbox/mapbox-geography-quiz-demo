package com.mapbox.tappergeochallenge.model;

import android.location.Location;

/**
 * Created by LangstonSmith on 2/13/17.
 */

public class City {

  private String cityName;
  private Location cityLocation;


  public String getCityName() {
    return cityName;
  }

  public void setCityName(String cityName) {
    this.cityName = cityName.replace("\"", "");
  }

  public Location getCityLocation() {
    return cityLocation;
  }

  public void setCityLocation(Location cityLocation) {
    this.cityLocation = cityLocation;
  }
}
