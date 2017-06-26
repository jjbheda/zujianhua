package org.qiyi.basecore.utils;

/**
 * copy from {@link com.facebook.csslayout.FloatUtil}
 *
 * @author zhaokaiyuan
 */
public final class FloatUtils {

    private static final float EPSILON = 1.0E-5F;

    private FloatUtils() {
    }

    /**
     * 比较两个float大小
     */
    public static boolean floatsEqual(float f1, float f2) {
        return !Float.isNaN(f1) && !Float.isNaN(f2) ? (Math.abs(f2 - f1) < EPSILON) : (Float.isNaN(f1) && Float.isNaN(f2));
    }
}
