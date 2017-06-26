package org.qiyi.android.gps;

import android.Manifest;
import android.content.Context;
import android.support.annotation.NonNull;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.SharedPreferencesConstants;
import org.qiyi.basecore.utils.SharedPreferencesFactory;
import org.qiyi.basecore.utils.PermissionUtil;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public  class GpsLocByBaiduSDK {

    public static double mLocGPS_latitude = 0.000000;
    public static double mLocGPS_longitude = 0.000000;
    public static String mLocGPS_province = "";
    @NonNull
    private final Context mContext;
    private LocationClient mLocationClient = null;
    private static GpsLocByBaiduSDK _instance = null;

    private static final int TIMEOUT = 3 * 1000;
    private static final int BAIDU_GPS_INTERVAL = 1800 * 1000;
    private String coorType = "bd09ll";
    private int getDataPriority = LocationClientOption.GpsFirst;
    private Callback mAbsOnAnyTimeCallBack = null;


    public static final String KEY_LOCATION_LATI = "KEY_LOCATION_LATI";
    public static final String KEY_LOCATION_LONGTI = "KEY_LOCATION_LONGTI";

    public static final String mLocGPS_separate = ",";
    /**
     * 各类逻辑用到的默认值
     */
    public final static String S_DEFAULT = "-1";
    /**
     * gps 无效数据
     */
    public static final double mLocGps_invalidValue = 4.9E-324;

    private String TAG = "GpsLocByBaiduSDK";

    private GpsLocByBaiduSDK(@NonNull Context context) {
        mContext = context.getApplicationContext();
        initLocationClient();
    }

    public synchronized static GpsLocByBaiduSDK getInstance(@NonNull Context context) {
        synchronized (GpsLocByBaiduSDK.class) {
            if (null == _instance) {
                _instance = new GpsLocByBaiduSDK(context);
            }
            return _instance;
        }
    }

    /**
     * 优先使用内存位置信息，没有再使用SharedPreference里面存的
     */
    public String getGPSLocationStr() {
        if(mContext == null){
            return "";
        }
        boolean isMemLocationValid = isLocationValid(GpsLocByBaiduSDK.mLocGPS_latitude, GpsLocByBaiduSDK.mLocGPS_longitude);
        if (isMemLocationValid) {
            String gpsInfo = String.valueOf(GpsLocByBaiduSDK.mLocGPS_longitude) + mLocGPS_separate + String.valueOf(GpsLocByBaiduSDK.mLocGPS_latitude);
            return gpsInfo;
        } else if (PermissionUtil.hasSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)) {
            try {
                requestMyLoc();
            } catch (Exception e) {
                DebugLog.e("GpsLocByBaiduSDK", e.getMessage());
            }
        }

        String BI_LOCATION_LONGTI = BiSharedPreferencesHelper.getInstance(mContext).getStringValue(BiSharedPreferencesHelper.BI_LOCATION_LONGTI, "0.0");
        String BI_LOCATION_LATI = BiSharedPreferencesHelper.getInstance(mContext).getStringValue(BiSharedPreferencesHelper.BI_LOCATION_LATI, "0.0");
        String gpsInfo_bi = BI_LOCATION_LONGTI + mLocGPS_separate + BI_LOCATION_LATI;
        if(isLocationValid(Double.valueOf(BI_LOCATION_LATI),Double.valueOf(BI_LOCATION_LONGTI))){
            return gpsInfo_bi;
        }
        return "";
    }

    /**
     * 专门为插件提供位置信息的接口
     *
     * 优先使用内存信息，没有再使用SP缓存
     */
    public String getGPSLocationStrForPlugin() {
        if (null == mContext) {
            return "";
        }
        boolean isMemLocInfoValid =
                isLocationValid(GpsLocByBaiduSDK.mLocGPS_latitude, GpsLocByBaiduSDK.mLocGPS_longitude);

        if (isMemLocInfoValid) {
            return String.valueOf(GpsLocByBaiduSDK.mLocGPS_longitude) +
                    mLocGPS_separate + String.valueOf(GpsLocByBaiduSDK.mLocGPS_latitude) +
                    GpsLocByBaiduSDK.mLocGPS_province;
        } else if (PermissionUtil.hasSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)) {
            try {
                requestMyLoc();
            } catch (Exception e) {
                DebugLog.e("GpsLocByBaiduSDK", e.getMessage());
            }
        }

        String BI_LOCATION_LONGTI = BiSharedPreferencesHelper.getInstance(mContext)
                .getStringValue(BiSharedPreferencesHelper.BI_LOCATION_LONGTI, "0.0");
        String BI_LOCATION_LATI = BiSharedPreferencesHelper.getInstance(mContext)
                .getStringValue(BiSharedPreferencesHelper.BI_LOCATION_LATI, "0.0");
        String BI_LOCATION_PROVINCE = BiSharedPreferencesHelper.getInstance(mContext)
                .getStringValue(BiSharedPreferencesHelper.BI_LOCATION_PROVINCE, "");
        String gpsInfo_bi = BI_LOCATION_LONGTI + mLocGPS_separate +
                BI_LOCATION_LATI + mLocGPS_separate + BI_LOCATION_PROVINCE;
        if(isLocationValid(Double.valueOf(BI_LOCATION_LATI),Double.valueOf(BI_LOCATION_LONGTI))){
            return gpsInfo_bi;
        }
        return "";
    }

    private void initLocationClient() {
        if (null == mContext) {
            return;
        }
        // 如果设置不启动baidu loc sdk 则不再初始化LocationClient
        if (!S_DEFAULT.equals(SharedPreferencesFactory.get(mContext, SharedPreferencesConstants.KEY_SETTING_GPS_LOC_OFF,
                S_DEFAULT))) {
            resetLonAndLat();
            return;
        }
        mLocationClient = new LocationClient(mContext);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(false);
        option.setIsNeedAddress(true);
        option.setCoorType(coorType);
        option.setPriority(getDataPriority);
        option.setScanSpan(BAIDU_GPS_INTERVAL);
        option.setProdName(""); // get constants ?
        option.setTimeOut(TIMEOUT);
        option.SetIgnoreCacheException(true);
        mLocationClient.setLocOption(option);
        mLocationClient.registerLocationListener(new MyBDLocationListener());
    }

    public void stopLocationClient() {
        try {
            if (null != mLocationClient && mLocationClient.isStarted()) {
                mLocationClient.stop();
            }
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
    }

    public void requestMyLoc() {
        // 如果设置不启动baidu loc sdk 则如果mAbsOnAnyTimeCallBack不为空则直接回调，并retrun
        if (!S_DEFAULT.equals(SharedPreferencesFactory.get(mContext, SharedPreferencesConstants.KEY_SETTING_GPS_LOC_OFF,
                S_DEFAULT))) {
            resetLonAndLat();
            if (null != mAbsOnAnyTimeCallBack) {
                mAbsOnAnyTimeCallBack.onPostExecuteCallBack();
            }
            return;
        }
        if (null == mLocationClient) {
            initLocationClient();
        }
        if (null != mLocationClient && !mLocationClient.isStarted()) {
            startLocationClient();
        }
        if (null != mLocationClient) {
            mLocationClient.requestLocation();
        }
    }

    private void startLocationClient() {
        if (null != mLocationClient && !mLocationClient.isStarted()) {
            mLocationClient.start();
        }
    }

    private class MyBDLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null || mContext == null) {
                return;
            }
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            String province = location.getProvince();
            DebugLog.log(TAG, "location type " + location.getLocType() + " " + lon + " " + lat);

            if (BigDecimal.valueOf(0) != BigDecimal.valueOf(lat)
                    && BigDecimal.valueOf(0) != BigDecimal.valueOf(lon)
                    && BigDecimal.valueOf(mLocGps_invalidValue) != BigDecimal.valueOf(lat)
                    && BigDecimal.valueOf(mLocGps_invalidValue) != BigDecimal.valueOf(lon)) {
                mLocGPS_latitude = lat;
                mLocGPS_longitude = lon;
                mLocGPS_province = province;
                SharedPreferencesFactory.set(mContext, SharedPreferencesConstants.PHONE_TICKETS_GPS_INFO, getLocationStr());
                String s1=String.valueOf(mLocGPS_latitude);
                if(s1!=null) {
                    BiSharedPreferencesHelper.getInstance(mContext).putStringValue(BiSharedPreferencesHelper.BI_LOCATION_LATI, s1);
                }
                s1=String.valueOf(mLocGPS_longitude);
                if(s1!=null) {
                    BiSharedPreferencesHelper.getInstance(mContext).putStringValue(BiSharedPreferencesHelper.BI_LOCATION_LONGTI, s1);
                }
                if(mLocGPS_province!=null) {
                    BiSharedPreferencesHelper.getInstance(mContext).putStringValue(BiSharedPreferencesHelper.BI_LOCATION_PROVINCE, mLocGPS_province);
                }

                BiSharedPreferencesHelper.getInstance(mContext).putLongValue(BiSharedPreferencesHelper.BI_LOCATION_TIMESTAMP
                        ,System.currentTimeMillis());
            }
            String locInfo = String.valueOf(lon) + mLocGPS_separate + String.valueOf(lat) + mLocGPS_separate + mLocGPS_province;
            if (null != mAbsOnAnyTimeCallBack) {
                mAbsOnAnyTimeCallBack.onPostExecuteCallBack(locInfo);
            }
        }
    }

    public void setmAbsOnAnyTimeCallBack(Callback mAbsOnAnyTimeCallBack) {
        this.mAbsOnAnyTimeCallBack = mAbsOnAnyTimeCallBack;
    }

    public static String getLocationStr() {
        DecimalFormat df = new DecimalFormat("0.000000");
        return df.format(GpsLocByBaiduSDK.mLocGPS_longitude) + mLocGPS_separate
                + df.format(GpsLocByBaiduSDK.mLocGPS_latitude);
    }

    private void resetLonAndLat() {
        mLocGPS_latitude = 0.0;
        mLocGPS_longitude = 0.0;
    }

    private boolean isLocationValid(double latitude, double longitude) {
        return latitude != 0 && longitude != 0 && mLocGps_invalidValue != latitude && mLocGps_invalidValue != longitude;
    }

    public static class LocationCallBack implements Callback{
        private IGPSWebView mCommonWebView;
        public LocationCallBack(IGPSWebView mCommonWebView) {
            this.mCommonWebView = mCommonWebView;
        }

        @Override
        public void onPostExecuteCallBack(Object... objects) {
            String gpsInfo = null;
            if (0 != GpsLocByBaiduSDK.mLocGPS_latitude && 0 != GpsLocByBaiduSDK.mLocGPS_longitude) {
                gpsInfo = getLocationStr();
            }
            this.mCommonWebView.onLocationUpdated(gpsInfo, true);
        }
    }
    
    public interface IGPSWebView{
        void onLocationUpdated(String gpsInfo, boolean isNeedToNotify);
    }

    public interface Callback{
        void onPostExecuteCallBack(Object... objects);
    }
}
