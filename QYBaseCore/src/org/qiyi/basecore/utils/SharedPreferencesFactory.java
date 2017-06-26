package org.qiyi.basecore.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.HashMap;

/**
 * SharedPreferences 工具类，不应包含业务相关的方法，只提供存储、获取数据的能力，
 * 业务方自己在相关业务中定义key 值
 */
public class SharedPreferencesFactory {

    /**
     * 默认的sharedPreferences 中是否存在_key
     */
    public static boolean hasKey(Context context, String _key) {
        if (null != context && !TextUtils.isEmpty(_key)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context,
                    SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
            if (null != helper) {
                return helper.hasKey(_key);
            }
        }
        return false;
    }

    /**
     * 指定的sharedPreferences 中是否存在_key
     */
    public static boolean hasKey(Context context, String _key, String sharedPreferenceName) {
        if (null != context && !TextUtils.isEmpty(_key) && !TextUtils.isEmpty(sharedPreferenceName)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context, sharedPreferenceName);
            if (null != helper) {
                return helper.hasKey(_key);
            }
        }
        return false;
    }

    /**
     * 存储String 在默认的 sharedPreferences 中
     */
    public static void set(Context context, String _key, String _value) {
        set(context, _key, _value, false);
    }

    public static void set(Context context, String _key, String _value, boolean saveImmediately) {
        if (null != context && !TextUtils.isEmpty(_key)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context,
                    SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
            if (null != helper) {
                helper.putString(_key, _value, saveImmediately);
            }
        }
    }

    /**
     * 存储Long 在默认的 sharedPreferences 中
     */
    public static void set(Context context, String _key, long _value) {
        set(context, _key, _value, false);
    }

    /**
     * 存储Long 在默认的 sharedPreferences 中
     */
    public static void set(Context context, String _key, float _value) {
        set(context, _key, _value, false);
    }

    /**
     * 存储float  value到某个指定sharedPreferences
     */
    public static void set(Context context, String _key, float value, String sharedPreferencesName) {
        set(context, _key, value, sharedPreferencesName, false);
    }

    public static void set(Context context, String _key, float value, String sharedPreferencesName, boolean saveImmediately) {
        if (null != context && !TextUtils.isEmpty(_key) && !TextUtils.isEmpty(sharedPreferencesName)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context, sharedPreferencesName);
            if (null != helper) {
                helper.putFloat(_key, value, saveImmediately);
            }
        } else {
            set(context, _key, value, saveImmediately);
        }
    }

    /**
     * 获取float 在默认的 sharedPreferences 中
     */
    public static float get(Context context, String _key, float _defaultValue) {
        if (null != context && !TextUtils.isEmpty(_key)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context,
                    SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
            if (null != helper) {
                return helper.getFloat(_key, _defaultValue);
            }
        }
        return _defaultValue;
    }

    /**
     * 获取某个指定sharedPreferences 的float  value
     */
    public static float get(Context context, String _key, float _defaultValue, String sharedPreferencesName) {
        if (null != context && !TextUtils.isEmpty(_key) && !TextUtils.isEmpty(sharedPreferencesName)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context, sharedPreferencesName);
            if (null != helper) {
                return helper.getFloat(_key, _defaultValue);
            }
        } else {
            return get(context, _key, _defaultValue);
        }
        return _defaultValue;
    }

    public static void set(Context context, String _key, long _value, boolean saveImmediately) {
        if (null != context && !TextUtils.isEmpty(_key)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context,
                    SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
            if (null != helper) {
                helper.putLong(_key, _value, saveImmediately);
            }
        }
    }

    public static void set(Context context, String _key, float _value, boolean saveImmediately) {
        if (null != context && !TextUtils.isEmpty(_key)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context,
                    SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
            if (null != helper) {
                helper.putFloat(_key, _value, saveImmediately);
            }
        }
    }

    /**
     * 存储int 在默认的 sharedPreferences 中
     */
    public static void set(Context context, String _key, int _value) {
        set(context, _key, _value, false);
    }

    public static void set(Context context, String _key, int _value, boolean saveImmediately) {
        if (null != context && !TextUtils.isEmpty(_key)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context,
                    SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
            if (null != helper) {
                helper.putInt(_key, _value, saveImmediately);
            }
        }
    }

    /**
     * 存储boolean 在默认的 sharedPreferences 中
     */
    public static void set(Context context, String _key, boolean _value) {
        set(context, _key, _value, false);
    }

    public static void set(Context context, String _key, boolean _value, boolean saveImmediately) {
        if (null != context && !TextUtils.isEmpty(_key)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context,
                    SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
            if (null != helper) {
                helper.putBoolean(_key, _value, saveImmediately);
            }
        }
    }

    /**
     * 获取String 在默认的 sharedPreferences 中
     */
    public static String get(Context context, String _key, String _defaultValue) {
        if (null != context && !TextUtils.isEmpty(_key)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context,
                    SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
            if (null != helper) {
                return helper.getString(_key, _defaultValue);
            }
        }
        return _defaultValue;
    }

    /**
     * 获取long 在默认的 sharedPreferences 中
     */
    public static long get(Context context, String _key, long _defaultValue) {
        if (null != context && !TextUtils.isEmpty(_key)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context,
                    SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
            if (null != helper) {
                return helper.getLong(_key, _defaultValue);
            }
        }
        return _defaultValue;
    }

    /**
     * 获取int 在默认的 sharedPreferences 中
     */
    public static int get(Context context, String _key, int _defaultValue) {
        if (null != context && !TextUtils.isEmpty(_key)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context,
                    SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
            if (null != helper) {
                return helper.getInt(_key, _defaultValue);
            }
        }
        return _defaultValue;
    }

    /**
     * 获取boolean 在默认的 sharedPreferences 中
     */
    public static boolean get(Context context, String _key, boolean _defaultValue) {
        if (null != context && !TextUtils.isEmpty(_key)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context,
                    SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
            if (null != helper) {
                return helper.getBoolean(_key, _defaultValue);
            }
        }
        return _defaultValue;
    }


    /**
     * 根据产品需求，注释掉渠道广告相关代码
     * 渠道开机屏广告
     */
    /**
     * public static final String getBootPicStartDateForChannel(Context mContext, String _defaultValue) {
     * return get(mContext, BOOT_PIC_START_DATE_CHANNEL, _defaultValue);
     * }
     * public static final void setBootPicStartDateForChannel(Context mContext, String value) {
     * set(mContext, BOOT_PIC_START_DATE_CHANNEL, value);
     * }
     * public static final String getBootPicEndDateForChannel(Context mContext, String _defaultValue) {
     * return get(mContext, BOOT_PIC_END_DATE_CHANNEL, _defaultValue);
     * }
     * public static final void setBootPicEndDateForChannel(Context mContext, String value) {
     * set(mContext, BOOT_PIC_END_DATE_CHANNEL, value);
     * }
     * public static final String getBootPicUrlForChannel(Context mContext, String _defaultValue) {
     * return get(mContext, BOOT_PIC_URL_CHANNEL, _defaultValue);
     * }
     * public static final void setBootPicUrlForChannel(Context mContext, String value) {
     * set(mContext, BOOT_PIC_URL_CHANNEL, value);
     * }
     * public static final String getBootPackgeDateForChannel(Context mContext, String _defaultValue) {
     * return get(mContext, BOOT_PIC_APK_PACKAGE_DATE_CHANNEL, _defaultValue);
     * }
     * public static final void setBootPackgeDateForChannel(Context mContext, String value) {
     * set(mContext, BOOT_PIC_APK_PACKAGE_DATE_CHANNEL, value);
     * }
     * public static final String getBootApkDownloadUrlForChannel(Context mContext, String _defaultValue) {
     * return get(mContext, BOOT_PIC_APK_DOWNLOAD_URL_CHANNEL, _defaultValue);
     * }
     * public static final void setBootApkDownloadUrlForChannel(Context mContext, String value) {
     * set(mContext, BOOT_PIC_APK_DOWNLOAD_URL_CHANNEL, value);
     * }
     * public static final String getBootApkNameForChannel(Context mContext, String _defaultValue) {
     * return get(mContext, BOOT_PIC_APK_NAME_DATE_CHANNEL, _defaultValue);
     * }
     * public static final void setBootApkNameForChannel(Context mContext, String value) {
     * set(mContext, BOOT_PIC_APK_NAME_DATE_CHANNEL, value);
     * }
     **/


    /**
     * 获取sharedPreferences
     */
    public static SharedPreferences getSharedPrefs(Context mContext, String sharedPreferencesName) {
        if (mContext != null && !TextUtils.isEmpty(sharedPreferencesName)) {
            return mContext.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE);
        } else {
            return null;
        }
    }

    /**
     * 为默认sharedPreferences某个key增加监听
     */
    public static void addOnSharedPreferenceChangListener(Context mContext,
                                                          String key, ConfigurationHelper.IOnSharedChangeListener listener) {
        if (mContext != null && !TextUtils.isEmpty(key)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(mContext,
                    SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
            if (null != helper) {
                helper.addOnSharedPreferenceChangListener(key, listener);
            }
        }
    }

    /**
     * 为指定的sharedPreferences的某个key 增加监听
     */
    public static void addOnSharedPreferenceChangListener(Context mContext, String sharePreferencesName,
                                                          String key, ConfigurationHelper.IOnSharedChangeListener listener) {
        if (null != mContext && !TextUtils.isEmpty(key) && !TextUtils.isEmpty(sharePreferencesName)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(mContext, sharePreferencesName);
            if (null != helper) {
                helper.addOnSharedPreferenceChangListener(key, listener);
            }
        }
    }

    /**
     * 清除某个指定sharedPreferences的所有数据
     */
    public static void clearAllData(Context context, String sharedPreferencesName) {
        if (context != null && TextUtils.isEmpty(sharedPreferencesName)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context, sharedPreferencesName);
            if (null != helper) {
                helper.clear();
            }
        }
    }

    public static HashMap<String, String> getAppVersion(String result) {
        HashMap<String, String> data = new HashMap<>();
        if (TextUtils.isEmpty(result)) {
            return data;
        }
        String[] strings = result.split("#QY#");
        if (strings.length != 2) {
            return data;
        }
        if (!TextUtils.isEmpty(strings[0])) {
            data.put("version", strings[0]);
        }
        if (!TextUtils.isEmpty(strings[1])) {
            data.put("time", strings[1]);
        }
        return data;
    }

    /**
     * 存储int  value到某个指定sharedPreferences
     */
    public static void set(Context context, String _key, int value, String sharedPreferencesName) {
        set(context, _key, value, sharedPreferencesName, false);
    }

    public static void set(Context context, String _key, int value, String sharedPreferencesName, boolean saveImmediately) {
        if (null != context && !TextUtils.isEmpty(sharedPreferencesName) && !TextUtils.isEmpty(_key)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context, sharedPreferencesName);
            if (null != helper) {
                helper.putInt(_key, value, saveImmediately);
            }
        } else {
            set(context, _key, value, saveImmediately);
        }
    }

    public static int get(Context context, String _key, int _defaultValue, String sharedPreferencesName) {
        if (null != context && !TextUtils.isEmpty(sharedPreferencesName) && !TextUtils.isEmpty(_key)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context, sharedPreferencesName);
            if (null != helper) {
                return helper.getInt(_key, _defaultValue);
            }
        } else {
            return get(context, _key, _defaultValue);
        }
        return _defaultValue;
    }


    /**
     * 存储String  value到某个指定sharedPreferences
     */
    public static void set(Context context, String _key, String value, String sharedPreferencesName) {
        set(context, _key, value, sharedPreferencesName, false);
    }

    public static void set(Context context, String _key, String value, String sharedPreferencesName, boolean saveImmediately) {
        if (null != context && !TextUtils.isEmpty(_key) && !TextUtils.isEmpty(sharedPreferencesName)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context, sharedPreferencesName);
            if (null != helper) {
                helper.putString(_key, value, saveImmediately);
            }
        } else {
            set(context, _key, value, saveImmediately);
        }
    }

    public static String get(Context context, String _key, String _defaultValue, String sharedPreferencesName) {
        if (null != context && !TextUtils.isEmpty(_key) && !TextUtils.isEmpty(sharedPreferencesName)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context, sharedPreferencesName);
            if (null != helper) {
                return helper.getString(_key, _defaultValue);
            }
        } else {
            return get(context, _key, _defaultValue);
        }
        return _defaultValue;
    }


    /**
     * 存储boolean  value到某个指定sharedPreferences
     */
    public static void set(Context context, String _key, boolean value, String sharedPreferencesName) {
        set(context, _key, value, sharedPreferencesName, false);
    }

    public static void set(Context context, String _key, boolean value, String sharedPreferencesName, boolean saveImmediately) {
        if (null != context && !TextUtils.isEmpty(_key) && !TextUtils.isEmpty(sharedPreferencesName)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context, sharedPreferencesName);
            if (null != helper) {
                helper.putBoolean(_key, value, saveImmediately);
            }
        } else {
            set(context, _key, value, saveImmediately);
        }
    }

    public static boolean get(Context context, String _key, boolean _defaultValue, String sharedPreferencesName) {
        if (null != context && !TextUtils.isEmpty(_key) && !TextUtils.isEmpty(sharedPreferencesName)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context, sharedPreferencesName);
            if (null != helper) {
                return helper.getBoolean(_key, _defaultValue);
            }
        } else {
            return get(context, _key, _defaultValue);
        }
        return _defaultValue;
    }


    /**
     * 存储long  value到某个指定sharedPreferences
     */
    public static void set(Context context, String _key, long value, String sharedPreferencesName) {
        set(context, _key, value, sharedPreferencesName, false);
    }

    public static void set(Context context, String _key, long value, String sharedPreferencesName, boolean saveImmediately) {
        if (null != context && !TextUtils.isEmpty(_key) && !TextUtils.isEmpty(sharedPreferencesName)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context, sharedPreferencesName);
            if (null != helper) {
                helper.putLong(_key, value, saveImmediately);
            }
        } else {
            set(context, _key, value, saveImmediately);
        }
    }

    /**
     * 获取某个指定sharedPreferences 的long  value
     */
    public static long get(Context context, String _key, long _defaultValue, String sharedPreferencesName) {
        if (null != context && !TextUtils.isEmpty(_key) && !TextUtils.isEmpty(sharedPreferencesName)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context, sharedPreferencesName);
            if (null != helper) {
                return helper.getLong(_key, _defaultValue);
            }
        } else {
            return get(context, _key, _defaultValue);
        }
        return _defaultValue;
    }

    public static void remove(Context context, String _key, String sharedPreferenceName) {
        remove(context, _key, sharedPreferenceName, false);
    }

    public static void remove(Context context, String _key, String sharedPreferenceName, boolean saveImmediately) {
        if (null != context && !TextUtils.isEmpty(_key) && !TextUtils.isEmpty(sharedPreferenceName)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(context, sharedPreferenceName);
            if (null != helper) {
                helper.remove(_key, saveImmediately);
            }
        } else {
            remove(context, _key, saveImmediately);
        }
    }

    public static void remove(Context mContext, String _key) {
        remove(mContext, _key, false);
    }

    public static void remove(Context mContext, String _key, boolean saveImmediately) {
        if (null != mContext && !TextUtils.isEmpty(_key)) {
            ConfigurationHelper helper = ConfigurationHelper.getInstance(mContext,
                    SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
            if (null != helper) {
                helper.remove(_key, saveImmediately);
            }
        }
    }

}
