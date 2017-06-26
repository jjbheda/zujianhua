package org.qiyi.basecore.utils;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * JSON Parser Util
 * <p/>
 * Created by syl on 15/11/22.
 * Note: has()/getXXX()方法相比直接optXXX()方法会有两倍的时间开销
 * 尽管可能只有微秒量级的差别，还是能省则省吧。
 * optXXX()方法不会抛JSONException异常
 */
public class JsonUtil {
    private static final String TAG = "JsonUtil";

    public static int readInt(JSONObject obj, String name) {
        return readInt(obj, name, 0);
    }


    public static int readInt(JSONObject obj, String name, int defaultValue) {
        if (obj == null || StringUtils.isEmpty(name)) {
            return defaultValue;
        }

        return obj.optInt(name, defaultValue);
    }


    public static long readLong(JSONObject obj, String name) {
        return readLong(obj, name, 0L);
    }


    public static long readLong(JSONObject obj, String name, long defaultValue) {
        if (obj == null || StringUtils.isEmpty(name)) {
            return defaultValue;
        }

        return obj.optLong(name, defaultValue);
    }


    public static double readDouble(JSONObject obj, String name) {
        return readDouble(obj, name, 0.0);
    }


    public static double readDouble(JSONObject obj, String name, double defaultValue) {
        if (obj == null || StringUtils.isEmpty(name)) {
            return defaultValue;
        }

        return obj.optDouble(name, defaultValue);
    }


    public static String readString(JSONObject obj, String name) {
        return readString(obj, name, "");
    }


    public static String readString(JSONObject obj, String name, String defaultValue) {
        if (obj == null || StringUtils.isEmpty(name)) {
            return defaultValue;
        }

        return obj.optString(name, defaultValue);
    }


    public static boolean readBoolean(JSONObject obj, String name, boolean defaultValue) {
        if (obj == null || StringUtils.isEmpty(name)) {
            return defaultValue;
        }

        return obj.optBoolean(name, defaultValue);
    }


    public static JSONObject readObj(JSONObject obj, String name) {
        if (obj == null || StringUtils.isEmpty(name)) {
            return null;
        }

        return obj.optJSONObject(name);
    }

    public static JSONObject readObj(JSONArray jArr, int index) {
        if (jArr == null || index < 0 || index >= jArr.length()) {
            return null;
        }

        return jArr.optJSONObject(index);
    }

    public static JSONArray readArray(JSONObject obj, String name) {
        if (obj == null || StringUtils.isEmpty(name)) {
            return null;
        }

        return obj.optJSONArray(name);
    }
}
