package org.qiyi.basecore.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import org.qiyi.android.corejar.debug.DebugLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;

public class ScreenTool {

    private static int sNaviBarHeight = -1;
    private static int sHasNaviBar = -1;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static int getWidth(Activity _activity) {
        DisplayMetrics dm = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= 17) {
            _activity.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        } else {
            _activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        }

        return dm.widthPixels;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static int getHeight(Activity _activity) {
        DisplayMetrics dm = new DisplayMetrics();
        _activity.getWindowManager().getDefaultDisplay().getMetrics(dm);

        if (Build.VERSION.SDK_INT >= 17) {
            _activity.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        } else {
            _activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        }

        return dm.heightPixels;
    }

    /**
     * 获取屏幕分辨率
     *
     * @param context
     * @param split
     * @return
     */
    public static String getResolution(Context context, String split) {

        String resolution = "";
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            resolution = ScreenTool.getWidth(context) + split + ScreenTool.getHeight(context);
        } else {
            resolution = ScreenTool.getHeight(context) + split + ScreenTool.getWidth(context);
        }
        return resolution;

    }

    /**
     * 获取屏幕分辨率，反向的
     *
     * @param context
     * @param split
     * @return
     */
    public static String getResolution_IR(Context context, String split) {

        String resolution = "";
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            resolution = ScreenTool.getHeight(context) + split + ScreenTool.getWidth(context);
        } else {
            resolution = ScreenTool.getWidth(context) + split + ScreenTool.getHeight(context);
        }

        return resolution;
    }

    public static int getWidth(Context mContext) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    public static int getHeight(Context mContext) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels;
    }


    /**
     * 获取屏幕Dpi
     *
     * @param mContext
     * @return
     */
    public static int getScreenDpi(Context mContext) {
        int ret = DisplayMetrics.DENSITY_MEDIUM;
        if (mContext != null) {
            try {
                DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
                ret = dm.densityDpi;

            } catch (Exception e) {
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                    throw new RuntimeException(e);
                }

            }
        }
        return ret;
    }

    /**
     * 获取屏幕密度
     *
     * @param context
     * @return
     */
    public static float getScreenDensity(Context context) {
        float density = 0;
        if (context != null) {
            try {
                DisplayMetrics dm = context.getApplicationContext().getResources().getDisplayMetrics();
                density = dm.density;
            } catch (Exception e) {
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
        return density;
    }

    /**
     * 根据屏幕密度（取整）获取合适的图片倍数1x、2x、3x等
     *
     * @param context
     * @return
     */
    public static int getScreenScale(Context context) {
        float density = getScreenDensity(context);
        return Math.round(density);
    }

    /**
     * 横屏判断
     *
     * @param mContext
     * @return
     */
    public static boolean isLandScape(Context mContext) {
        return mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }


    /**
     * 这个尺寸没有包括action bar, 例如华为P8，值为4.33, 720*1184
     *
     * @param context
     * @return
     */
    public static float getScreenSize(Context context) {
        float ret = 0;
        try {
            DisplayMetrics dm = context.getApplicationContext().getResources().getDisplayMetrics();
            int w = dm.widthPixels;
            int h = dm.heightPixels;
            float dpi = dm.densityDpi;
            BigDecimal bd =  BigDecimal.valueOf(Math.sqrt(w * w + h * h * 1.0) / dpi);
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
            ret = bd.floatValue();
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
        return ret;
    }

    /**
     * 这个尺寸准确, 例如华为P8，值为4.59, 720*1280
     *
     * @param context
     * @return
     */
    public static float getScreenRealSize(Context context) {
        if (Build.VERSION.SDK_INT >= 17) {
            float ret = 0;
            try {
                WindowManager mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                Display mDisplay = mWindowManager.getDefaultDisplay();
                DisplayMetrics mDisplayMetrics = new DisplayMetrics();
                mDisplay.getRealMetrics(mDisplayMetrics);
                int w = mDisplayMetrics.widthPixels;
                int h = mDisplayMetrics.heightPixels;
                float dpi = mDisplayMetrics.densityDpi;
                BigDecimal bd = BigDecimal.valueOf(Math.sqrt(w * w + h * h * 1.0) / dpi);
                bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                ret = bd.floatValue();
                return ret;
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
                return getScreenSize(context);
            }
        } else {
            return getScreenSize(context);
        }
    }


    //获取手机状态栏高度
    public static int getStatusBarHeight(Context context) {
        Class<?> c;
        Object obj;
        Field field;
        int x;
        int statusBarHeight = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
        return statusBarHeight;
    }

    /**
     * 获取NavigationBar的高度值
     *
     * @return 如果拥有navigationBar，返回该navigationBar的实际高度值，否则返回0.
     */
    public static int getNavigationBarHeight(Context context) {
        if (sNaviBarHeight < 0) {
            if (hasNavigationBar(context)) {
                //The device has a navigation bar
                Resources resources = context.getResources();
                int orientation = resources.getConfiguration().orientation;
                int resourceId;
                if (isTablet(context)) {
                    resourceId = resources.getIdentifier(orientation == Configuration.ORIENTATION_PORTRAIT ?
                            "navigation_bar_height" : "navigation_bar_height_landscape", "dimen", "android");
                } else {
                    resourceId = resources.getIdentifier(orientation == Configuration.ORIENTATION_PORTRAIT ?
                            "navigation_bar_height" : "navigation_bar_width", "dimen", "android");
                }
                sNaviBarHeight = resourceId > 0 ? context.getResources().getDimensionPixelSize(resourceId) : 0;
            } else {
                sNaviBarHeight = 0;
            }
        }

        return sNaviBarHeight;
    }

    public static boolean hasNavigationBar(Context context) {
        if (sHasNaviBar == -1) {
            Resources res = context.getResources();
            int resourceId = res.getIdentifier("config_showNavigationBar", "bool", "android");
            if (resourceId != 0) {
                String sNavBarOverride = "";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    try {
                        Class c = Class.forName("android.os.SystemProperties");
                        Method m = c.getDeclaredMethod("get", String.class);
                        m.setAccessible(true);
                        sNavBarOverride = (String) m.invoke(null, "qemu.hw.mainkeys");
                    } catch (Throwable e) {
                        sNavBarOverride = "";
                    }
                }
                boolean hasNav = res.getBoolean(resourceId);
                // check override flag (see static block)
                if ("1".equals(sNavBarOverride)) {
                    hasNav = false;
                } else if ("0".equals(sNavBarOverride)) {
                    hasNav = true;
                }
                DebugLog.i("navigationbar", "hasnavigationbar = " + hasNav + " ;sNavBarOverride = " + sNavBarOverride);
                sHasNaviBar = hasNav ? 1 : 0;
            } else { // fallback
                boolean hasMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey();
                //国内厂商有的虚拟手机该方法会返回true(如华为荣耀6)
                boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
                boolean hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);
                DebugLog.i("navigationbar", "hasMenuKey = " + hasMenuKey + " ;hasHomeKey = " + hasHomeKey + " ;hasBackKey  = "
                        + hasBackKey);
                sHasNaviBar = !hasMenuKey && !hasBackKey ? 1 : 0;
            }
        }

        return sHasNaviBar == 1;
    }

    private static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >=
                Configuration.SCREENLAYOUT_SIZE_LARGE;
    }


    //网络图片大小
    private static final int IMAGE_WIDTH = 220;
    private static final int IMAGE_HIGH = 124;

    /**
     * 调整图片显示大小
     * @param imageView
     */
    public static void resizeItemIcon(ImageView imageView){

        Context context = imageView.getContext();
        if(context == null){
            return;
        }

        int windowsWidth = context.getResources().getDisplayMetrics().widthPixels;
        int windowsHeight = context.getResources().getDisplayMetrics().heightPixels;

        int spaceWidth = UIUtils.dip2px(context, 25f);

        int realImageWidth = (windowsWidth - spaceWidth) / 2;
        int realImageHeight = realImageWidth * IMAGE_HIGH / IMAGE_WIDTH;

        ViewGroup.LayoutParams lp = imageView.getLayoutParams();

        if (windowsWidth < windowsHeight) {
            lp.width = realImageWidth * 7 / 10;
            lp.height = realImageHeight * 7 / 10;
        } else {
            lp.width = realImageWidth * 4 / 10;
            lp.height = realImageHeight * 4 / 10;
        }

        imageView.setLayoutParams(lp);
    }
}
