package org.qiyi.basecore.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.support.annotation.NonNull;

import java.io.File;


public final class ApkUtil {

    /**
     * 获取指定APK文件的信息
     *
     * @param context
     * @param file
     * @return
     */
    public static PackageInfo getApkFileInfo(@NonNull Context context, File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        try {
            PackageManager pm = context.getPackageManager();
            String filePath = file.getAbsolutePath();
            PackageInfo packageInfo = pm.getPackageArchiveInfo(filePath,
                    PackageManager.GET_META_DATA);
            return packageInfo;
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * 判断App是否安装
     *
     * @param context
     * @param packageName
     * @return
     */
    public static boolean isAppInstalled(@NonNull Context context, String packageName) {

        PackageInfo pi = null;
        try {
            PackageManager pm = context.getPackageManager();
            pi = pm.getPackageInfo(packageName, 0);
        } catch (Exception ignore) {
            // NameNotFoundException or Package Manager has died exception
            pi = null;
        }

        return pi != null;
    }

    /**
     * 判断App是否安装，大于minVersion，认为已安装；低于minVersion，则认为未安装
     *
     * @param context
     * @param packageName
     * @param minVersionCode
     * @return
     */
    public static boolean isAppInstalled(@NonNull Context context, String packageName, int minVersionCode) {

        PackageInfo pi = null;
        try {
            PackageManager pm = context.getPackageManager();
            pi = pm.getPackageInfo(packageName, 0);

        } catch (Exception ignore) {
            // NameNotFoundException or Package Manager has died exception
            pi = null;
        }

        return pi != null && pi.versionCode >= minVersionCode;
    }


    /**
     * 获取App的versionCode
     *
     * @param context
     * @param packageName
     * @return
     */
    public static int getAppVersionCode(@NonNull Context context, String packageName) {

        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(packageName, 0);
            if (pi != null) {
                return pi.versionCode;
            }
        } catch (Exception ignore) {
            // NameNotFoundException or Package Manager has died exception
        }
        return 0;
    }

    /**
     * 获取当前Context对应App的versionCode
     *
     * @param context
     * @return
     */
    public static int getVersionCode(@NonNull Context context) {

        return getAppVersionCode(context, context.getPackageName());
    }

    /**
     * 获取App的VersionName
     *
     * @param context
     * @param packageName
     * @return
     */
    public static String getAppVersionName(@NonNull Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(packageName, 0);
            if (pi != null) {
                return pi.versionName;
            }
        } catch (Exception ignore) {
            // NameNotFoundException or Package Manager has died exception
        }
        return "";
    }


    /**
     * 获取当前Context对应App的VersionName
     *
     * @param context
     * @return
     */
    public static String getVersionName(@NonNull Context context) {

        return getAppVersionName(context, context.getPackageName());
    }


    /**
     * 获取应用第一次安装时间。
     *
     * @param context
     * @return
     */
    public static long getAppInstallTime(@NonNull Context context) {

        long installTime = -1;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            installTime = pi.firstInstallTime;
        } catch (Exception ignore) {

        }

        return installTime;
    }

    /**
     * 获取应用最后更新时间。
     *
     * @param context
     * @return
     */
    public static long getAppUpdateTime(@NonNull Context context) {

        long updateTime = -1;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            updateTime = pi.lastUpdateTime;
        } catch (Exception ignore) {

        }

        return updateTime;
    }
}
