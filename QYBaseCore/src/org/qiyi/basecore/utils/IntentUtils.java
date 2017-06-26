package org.qiyi.basecore.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;

/***
 * 获取Intent  数据，防止getXXXExtra漏洞
 * 漏洞可参看：http://blogs.360.cn/blog/android-app%E9%80%9A%E7%94%A8%E5%9E%8B%E6%8B%92%E7%BB%9D%E6%9C%8D%E5%8A%A1%E6%BC%8F%E6%B4%9E%E5%88%86%E6%9E%90%E6%8A%A5%E5%91%8A/
 *
 * @author maoxiang
 *         2015-3-25
 */
public class IntentUtils {

    /***
     * 获取boolean类型的值
     *
     * @param intent
     * @param name
     * @param defaultValue
     * @return
     */
    public static final boolean getBooleanExtra(Intent intent, String name, boolean defaultValue) {
        boolean val = defaultValue;
        try {
            val = intent.getBooleanExtra(name, defaultValue);
        } catch (Exception e) {
        }
        return val;
    }

    /***
     * 获取boolean类型的值
     *
     * @param bundle
     * @param name
     * @param defaultValue
     * @return
     */
    public static final boolean getBooleanExtra(Bundle bundle, String name, boolean defaultValue) {
        boolean val = defaultValue;
        try {
            val = bundle.getBoolean(name, defaultValue);
        } catch (Exception e) {
        }
        return val;
    }

    /**
     * 获取String类型的值
     *
     * @param intent
     * @param name
     * @return
     */
    public static final String getStringExtra(Intent intent, String name) {
        String val = null;
        try {
            val = intent.getStringExtra(name);
        } catch (Exception e) {
        }
        return val;
    }

    /**
     * 获取String类型的值
     *
     * @param bundle
     * @param name
     * @return
     */
    public static final String getStringExtra(Bundle bundle, String name) {
        String val = null;
        try {
            val = bundle.getString(name);
        } catch (Exception e) {
        }
        return val;
    }

    /***
     * 获取int类型的值
     *
     * @param intent
     * @param name
     * @param defaultValue
     * @return
     */
    public static final int getIntExtra(Intent intent, String name, int defaultValue) {
        int val = defaultValue;
        try {
            val = intent.getIntExtra(name, defaultValue);
        } catch (Exception e) {
        }
        return val;
    }

    /***
     * 获取int类型的值
     *
     * @param bundle
     * @param name
     * @param defaultValue
     * @return
     */
    public static final int getIntExtra(Bundle bundle, String name, int defaultValue) {
        int val = defaultValue;
        try {
            val = bundle.getInt(name, defaultValue);
        } catch (Exception e) {
        }
        return val;
    }

    /**
     * 获取Serializable类型的值
     *
     * @param intent
     * @param name
     * @return
     */
    public static final Serializable getSerializableExtra(Intent intent, String name) {
        Serializable val = null;
        try {
            val = intent.getSerializableExtra(name);
        } catch (Exception e) {
        }
        return val;
    }

    /**
     * 获取Parcelable类型的值
     *
     * @param intent
     * @param name
     * @return
     */
    public static final Parcelable getParcelableExtra(Intent intent, String name) {
        Parcelable val = null;
        try {
            val = intent.getParcelableExtra(name);
        } catch (Exception e) {
        }
        return val;
    }

    /***
     * 获取 long 类型的值
     *
     * @param intent
     * @param name
     * @param defaultValue
     * @return
     */
    public static final long getLongExtra(Intent intent, String name, long defaultValue) {
        long val = defaultValue;
        try {
            val = intent.getLongExtra(name, defaultValue);
        } catch (Exception e) {
        }
        return val;
    }

    /***
     * 获取 byte[] 类型的值
     *
     * @param intent
     * @param name
     * @return
     */
    public static final byte[] getByteArrayExtra(Intent intent, String name) {
        byte[] val = null;
        try {
            val = intent.getByteArrayExtra(name);
        } catch (Exception e) {
        }
        return val;
    }

    /**
     * 获取ArrayList<String>值
     *
     * @param intent
     * @param name
     * @return
     */
    public static final ArrayList<String> getStringArrayListExtra(Intent intent, String name) {
        ArrayList<String> val = null;
        try {
            val = intent.getStringArrayListExtra(name);
        } catch (Exception e) {
        }
        return val;
    }

    /**
     * 获取 Bundle 类型的值
     *
     * @param intent
     * @param name
     * @return
     */
    public static final Bundle getBundleExtra(Intent intent, String name) {
        Bundle bundle = null;
        try {
            bundle = intent.getBundleExtra(name);
        } catch (Exception e) {
        }
        return bundle;
    }

    /**
     * 获取 Uri 类型的值
     *
     * @param intent
     * @return
     */
    public static final Uri getData(Intent intent) {
        Uri uri = null;
        try {
            uri = intent.getData();
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
        return uri;
    }

}