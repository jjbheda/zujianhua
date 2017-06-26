package org.qiyi.basecore.utils;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by yudunfu on 16-7-1.
 * 缓存initLogin接口返回的角标地址
 */
public class DynamicIconResolver {

    /**
     * 根据 Key 值保存图标
     */
    private static ConcurrentHashMap<String, ICON> sIconsMap;

    private DynamicIconResolver() {
    }

    /**
     * 从存 Map 中获取对应的图标 URL
     * @param context              Context 对象
     * @param key                  初始化接口返回数据中的 Key 值
     * @return 对应的图标的 URL
     */
    public static String getIconCachedUrl(Context context, String key) {
        boolean isTraditionalChinese = context.getResources().getConfiguration().locale.getCountry().equals("TW") ||
                context.getResources().getConfiguration().locale.getCountry().equals("HK");
        return getIconCachedUrl(context, key, isTraditionalChinese);
    }

    /**
     * 从存 Map 中获取对应的图标 URL
     *
     * @param context              Context 对象
     * @param key                  初始化接口返回数据中的 Key 值
     * @param isTraditionalChinese 是否是繁体中文
     * @return 对应的图标的 URL
     */
    public static String getIconCachedUrl(Context context, String key, boolean isTraditionalChinese) {
        // 如果 Map 没有被初始化，先从本地 SP 中初始化 Map
        if (sIconsMap == null || sIconsMap.size() == 0) {
            String jsonOfIcons = SPBigStringFileFactory.getInstance(context).getKeyMergeFromSPSync(SharedPreferencesConstants.ANGLE_ICONS2_IN_INIT_APP, "",SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
            JSONArray iconsArray = null;
            try {
                iconsArray = new JSONArray(jsonOfIcons);
            } catch (JSONException e) {
                ExceptionUtils.printStackTrace(e);
            }
            parseMarkJson(iconsArray);
        }
        return selectIconUrl(key, isTraditionalChinese);
    }


    /**
     * 根据规则选择不同的图标
     *
     * @param key                  初始化接口返回数据中的 Key 值
     * @param isTraditionalChinese 是否是繁体中文
     * @return 对应的图标的 URL
     */
    private static String selectIconUrl(String key, boolean isTraditionalChinese) {
        if (sIconsMap == null) {
            return null;
        } else {
            ICON icon = sIconsMap.get(key);
            if (icon == null) {
                return null;
            } else {
                return (isTraditionalChinese && !TextUtils.isEmpty(icon.twv)) ? icon.twv : icon.v;
            }
        }
    }

    /**
     * 解析角标(两种方式兼容)
     *
     * @param iconsArray
     * @return
     */
    public static void parseMarkJson(JSONArray iconsArray) {
        if (sIconsMap == null) {
            sIconsMap = new ConcurrentHashMap<>();
        }
        if (iconsArray != null && iconsArray.length() > 0) {
            JSONObject icon;
            for (int i = 0; i < iconsArray.length(); i++) {
                icon = iconsArray.optJSONObject(i);
                if (icon != null) {
                    String key = "";
                    if (icon.has("id")) {
                        key = icon.optString("id");
                    } else if (icon.has("k")) {
                        key = icon.optString("k");
                    }
                    if (!TextUtils.isEmpty(key)) {
                        ICON ic = new ICON();
                        ic.k = key;
                        ic.w = icon.optString("w");
                        ic.h = icon.optString("h");
                        if (icon.has("url")) {
                            ic.v = icon.optString("url");
                        } else if (icon.has("v")) {
                            ic.v = icon.optString("v");
                        }
                        if (icon.has("url_tw")) {
                            ic.twv = icon.optString("url_tw");
                        } else if (icon.has("twv")) {
                            ic.twv = icon.optString("twv");
                        }
                        sIconsMap.put(key, ic);
                    }
                }
            }
        }
    }

    private static class ICON {
        /**
         * 图标宽
         */
        public String w;
        /**
         * 图标高
         */
        public String h;
        /**
         * 图标Key值
         */
        public String k;
        /**
         * 通用图标URL
         */
        public String v;
        /**
         * 台湾站图标URL
         */
        public String twv;
    }

}
