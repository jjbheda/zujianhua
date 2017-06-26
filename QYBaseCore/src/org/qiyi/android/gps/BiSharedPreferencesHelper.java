package org.qiyi.android.gps;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.SharedPreferencesFactory;

/**
 * 这个类从插件中心的SDK中移动过来
 * TODO: 需要对接{@link org.qiyi.basecore.utils.SharedPreferencesFactory}
 *
 * 用于bi统计
 */
public class BiSharedPreferencesHelper {
    public static final String PREFERENCE_NAME = "bi4sdk";

    @SuppressLint("StaticFieldLeak")
    private static BiSharedPreferencesHelper instance;
    @SuppressLint("StaticFieldLeak")
    private Context context;

    public static BiSharedPreferencesHelper getInstance(Context context) {
        if (instance == null) {
            instance = new BiSharedPreferencesHelper(context);
        }
        return instance;
    }

    private BiSharedPreferencesHelper(Context mcontext) {
        if (mcontext == null) {
            return;
        }
        context = mcontext.getApplicationContext();
    }

    // for bi
    //public static final String IP = "IP";// ip地址
    public static final String LOG_DEBUG_KEY = "LOG_DEBUG_KEY";// debug开关
    //public static final String SHUT_DOWN_TIME = "BI_SHUT_DOWN_TIME";// 关机时间
    public static final String BI_UUID = "BI_UUID";// uuid,BI业务，string类型
    public static final String BI_PLATFROM = "BI_PLATFROM";// 平台信息，BI业务，string类型
    public static final String BI_LOGIN_ID = "BI_LOGIN_ID";// 用户登录ID，BI业务，string类型
   // public static final String BI_BE_KILLED = "BI_BE_KILLED";// 表示服务是否被杀
    public static final String BI_SWITCH = "BI_SWITCH";// bi开关，BI业务，boolean类型
    //public static final String BI_FIRST_LAUCH = "BI_FIRST_LAUCH"; // BI第一次启动
    //public static final String BI_DELIVER_PERIOD = "BI_DELIVER_PERIOD";// 投递周期
    //public static final String BI_SEARCH_INFO_PERIOD = "BI_SEARCH_INFO_PERIOD";// 用户信息扫描周期
    public static final String BI_LOCATION_LATI = "BI_LOCATION_LATI";// 纬度,BI业务，string类型
    public static final String BI_LOCATION_LONGTI = "BI_LOCATION_LONGTI";// 经度，BI业务，string类型
    public static final String BI_LOCATION_TIMESTAMP = "BI_LOCATION_TIMESTAMP";//位置信息更新时间，BI业务，long类型

  /////////////非BI业务
    public static final String BI_LOCATION_PROVINCE = "BI_LOCATION_PROVINCE";//省份,非BI业务

    public long getLongValue(String key) {
        if (!TextUtils.isEmpty(key)) {
            try {
                return SharedPreferencesFactory.get(context, key, 0L, PREFERENCE_NAME);
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }
        }
        return 0L;
    }

    public String getStringValue(String key) {
        if (!TextUtils.isEmpty(key)) {
            try {
                return SharedPreferencesFactory.get(context, key, "", PREFERENCE_NAME);
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }
        }
        return null;
    }

    public String getStringValue(String key, String defValue) {
        if (!TextUtils.isEmpty(key)) {
            try {
                return SharedPreferencesFactory.get(context, key, defValue, PREFERENCE_NAME);
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }
        }
        return defValue;
    }

    public int getIntValue(String key) {
        if (!TextUtils.isEmpty(key)) {
            try {
                return SharedPreferencesFactory.get(context, key, 0, PREFERENCE_NAME);
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }
        }
        return 0;
    }

    public boolean getBooleanValue(String key) {
        if (!TextUtils.isEmpty(key)) {
            try {
                return SharedPreferencesFactory.get(context, key, false, PREFERENCE_NAME);
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }
        }
        return true;
    }

    public float getFloatValue(String key) {
        if (!TextUtils.isEmpty(key)) {
            try {
                return SharedPreferencesFactory.get(context, key, 0f, PREFERENCE_NAME);
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }
        }
        return 0f;
    }

    public void putStringValue(String key, String value) {
        if (!TextUtils.isEmpty(key)) {
            SharedPreferencesFactory.set(context, key, value, PREFERENCE_NAME);
        }
    }

    public void putIntValue(String key, int value) {
        if (!TextUtils.isEmpty(key)) {
            SharedPreferencesFactory.set(context, key, value, PREFERENCE_NAME);
        }
    }

    public void putBooleanValue(String key, boolean value) {
        if (!TextUtils.isEmpty(key)) {
            SharedPreferencesFactory.set(context, key, value, PREFERENCE_NAME);
        }
    }

    public void putLongValue(String key, long value) {
        if (!TextUtils.isEmpty(key)) {
            SharedPreferencesFactory.set(context, key, value, PREFERENCE_NAME);
        }
    }

    public void putFloatValue(String key, Float value) {
        if (!TextUtils.isEmpty(key)) {
            SharedPreferencesFactory.set(context, key, value, PREFERENCE_NAME);
        }
    }

}
