package org.qiyi.basecore.utils;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.view.View;

import org.qiyi.android.corejar.debug.DebugLog;

/**
 * 颜色工具类
 * Created by shisong on 2016/6/27.
 */
public class ColorUtil {

    /**
     * 解析字符串成color值,如果解析失败则返回透明颜色值
     *
     * @param colorStr color字符串
     * @return 解析结果
     */
    public static int parseColor(String colorStr) {
        return parseColor(colorStr, 0x00000000);
    }

    /**
     * 解析字符串成color值
     *
     * @param colorStr     color字符串
     * @param defaultColor 默认color值
     * @return 解析结果
     */
    public static int parseColor(String colorStr, int defaultColor) {
        try {
            return Color.parseColor(colorStr);
        } catch (Exception e) {
            return defaultColor;
        }
    }

    /**
     * 重置颜色透明度
     *
     * @param alpha 取值范围0~1 0代表全透明，1代表完全不透明
     * @param color 初始颜色值
     * @return 重置后的颜色值
     */
    public static int alphaColor(float alpha, int color) {
        if (alpha > 1.0f) {
            alpha = 1.0f;
        }
        if (alpha < 0.0f) {
            alpha = 0.0f;
        }
        return Color.argb((int) (alpha * 255), Color.red(color), Color.green(color), Color.blue(color));
    }

    /***
     * 从资源里获取颜色
     *
     * @param resources resources对象
     * @param id        颜色ID
     * @return 颜色值
     */
    @ColorInt
    public static int getColor(Resources resources, @ColorRes int id, int defaultColor) {
        try {
            if (resources != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    return resources.getColor(id);
                } else {

                    return resources.getColor(id, null);
                }
            }
        } catch (Throwable e) {
            if (DebugLog.isDebug()) {
                DebugLog.log("error", "getColor error:" + e);

            }
        }
        return defaultColor;

    }

    /**
     * 设置背景色的alpha值
     *
     * @param view  view对象
     * @param alpha value from 0 to 255
     */
    public static void setAlpha(View view, int alpha) {
        if (view == null) {
            return;
        }
        Drawable drawable = view.getBackground();
        if (drawable instanceof ColorDrawable) {
            int baseColor = ((ColorDrawable) drawable).getColor();
            int color = (baseColor << 8 >>> 8) | (alpha << 24);
            view.setBackgroundColor(color);
        } else {
            view.getBackground().setAlpha(alpha);
        }
    }

}
