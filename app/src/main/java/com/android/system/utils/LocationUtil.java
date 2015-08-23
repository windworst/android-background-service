package com.android.system.utils;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public abstract  class LocationUtil {
    static public void listenLocation(Context context, LocationListener listener){
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        String provider = "";
        if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        {
            provider = LocationManager.NETWORK_PROVIDER;
        }
        else if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            provider = LocationManager.GPS_PROVIDER;
        }
        locationManager.requestLocationUpdates(provider, 0,0, listener);
    }
}
