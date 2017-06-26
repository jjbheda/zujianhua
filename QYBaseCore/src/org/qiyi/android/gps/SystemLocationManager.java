package org.qiyi.android.gps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.SharedPreferencesFactory;

/**
 * Provide latitude & longitude from Android system API
 *
 * 通过系统API定位注意事项：
 * 1) 三星（Android 4.x）, 金立（Android 4.x）部分机型在没有检查permission前调用 LocationManager.isProviderEnabled抛出SecurityException。
 * 2) 如果对Android 4.x以上版本都增加ContextCompact.checkSelfPermission, Lenovo Z2， S930, A368t等机型（Android4.x）抛出异常。
 * 3) 小米4S在进行GPS定位时，Android系统会自动弹出通知栏提醒用户应用正在进行GPS定位。三星A7会持续在LOG中报错，系统顶部会出现提示GPS标志。
 *
 * Created by Palmerma on 2016/12/27.
 */

public class SystemLocationManager {

    private String TAG = "SystemLocationManager";
    private static SystemLocationManager systemLocationManager;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private final static String KEY_SYSTEM_LOCATION_LATITUDE = "key_system_location_latitude";
    private final static String KEY_SYSTEM_LOCATION_LONGITUDE = "key_system_location_longitude";

    private SystemLocationManager() {

    }

    public static SystemLocationManager getInstance() {
        synchronized (SystemLocationManager.class) {
            if (systemLocationManager == null) {
                systemLocationManager = new SystemLocationManager();
            }
            return systemLocationManager;
        }
    }


    public double[] getLocationFromSystem(Context context) {
        if (context == null) {
            return null;
        }
        if (locationManager == null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }
        if (locationListener == null) {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        DebugLog.v(TAG, "location changed latitude " + latitude + " longitude " + longitude);
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            };
        }
        // 如果对Android 4.x以上版本都增加ContextCompact.checkSelfPermission, Lenovo Z2， S930, A368t等机型（Android4.x）抛出异常。
        try {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                return getLocationFromNetwork(context);
            }
        }catch (Exception e){
            ExceptionUtils.printStackTrace(e);
        }
        return null;
    }

    private double[] getLocationFromNetwork(Context context) {
        if (locationManager == null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            if (locationListener != null) {
                try {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1800000, 0, this.locationListener);
                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        double[] locationValue = {latitude, longitude};
                        storeLocation(context, latitude, longitude);
                        DebugLog.v(TAG, "Network Location: " + latitude + " " + longitude);
                        return locationValue;
                    } else {
                        DebugLog.d(TAG, " Network Location failed");
                        return null;
                    }
                } catch (SecurityException e) {
                    ExceptionUtils.printStackTrace(e);
                    return null;
                } catch (Exception e) {
                    ExceptionUtils.printStackTrace(e);
                    return null;
                }
            } else {
                DebugLog.d(TAG, "Network Location location listener is null");
                return null;
            }
        }
        DebugLog.d(TAG, "Network failed");
        return null;
    }

    public void removeLocationListener() {
        if (locationListener != null && locationManager != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                ExceptionUtils.printStackTrace(e);
            }
        }
    }

    private void storeLocation(Context context, double latitude, double longitude) {
        if (context == null) {
            return;
        }
        SharedPreferencesFactory.set(context, KEY_SYSTEM_LOCATION_LATITUDE, String.valueOf(latitude));
        SharedPreferencesFactory.set(context, KEY_SYSTEM_LOCATION_LONGITUDE, String.valueOf(longitude));
    }

    public String[] getLocation(Context context) {
        if (context == null) {
            return new String[]{"", ""};
        }
        String latitudeStr = SharedPreferencesFactory.get(context, KEY_SYSTEM_LOCATION_LATITUDE, "");
        String longitudeStr = SharedPreferencesFactory.get(context, KEY_SYSTEM_LOCATION_LONGITUDE, "");
        return new String[]{latitudeStr, longitudeStr};
    }

}
