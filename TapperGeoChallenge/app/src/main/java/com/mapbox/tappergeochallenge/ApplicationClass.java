package com.mapbox.tappergeochallenge;

import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

public class ApplicationClass extends MultiDexApplication {
  @Override
  public void onCreate() {
    super.onCreate();
    MultiDex.install(this);
  }
}
