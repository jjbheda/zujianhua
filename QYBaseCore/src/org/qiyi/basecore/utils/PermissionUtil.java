package org.qiyi.basecore.utils;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;

import org.qiyi.android.corejar.debug.DebugLog;


/**
 * Utility class that wraps access to the runtime permissions API in M and provides basic helper
 * methods.
 */
public class PermissionUtil {
    /**
     * 读取手机状态
     */
    public static final int PERMISSION_PHONE_STATE = 0x000;
    /**
     * 摄像头权限
     */
    public static final int PERMISSION_CAMEAR = 0X001;

    /**
     * 定位权限
     */
    public static final int PERMISSION_LOCATION = 0X002;

    /**
     * sd卡权限
     */
    public static final int PERMISSION_STORAGE = 0x003;

    /**
     * 使用麦克风
     */
    public static final int PERMISSION_RECORD_AUDIO = 0x004;

    public static final int PERMISSION_PLAYER_CAPTURE_STORE = 0x005;

    private static final String TAG = "PermissionUtil";

    public static boolean hasSelfPermission(Context context, String permission) {
        boolean hasPermission = false;
        try {
            hasPermission =
                    (context.checkPermission(permission, android.os.Process.myPid(), Process.myUid()) == PackageManager
                            .PERMISSION_GRANTED);
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
        DebugLog.d(TAG, "hasSelfPermission " + permission + ", hasPermission =" + hasPermission);
        return hasPermission;
    }

    public static boolean requestPhoneStateInWelcomeActivity(Application application) {

        boolean hasPhoneStatePermission = hasSelfPermission(application, android.Manifest.permission.READ_PHONE_STATE);
        boolean isNewPermissionModel = isNewPermissionModel(application);
        return !hasPhoneStatePermission && isNewPermissionModel;
    }

    private static boolean isNewPermissionModel(Application application) {
        boolean isOsAbove23 = Build.VERSION.SDK_INT >= 23;
        int targetSdkVersion = 0;
        try {
            PackageInfo packageInfo = application.getPackageManager().getPackageInfo(application.getPackageName(), 0);
            targetSdkVersion = packageInfo.applicationInfo.targetSdkVersion;
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
        boolean isNewPermissionModel = (isOsAbove23 && targetSdkVersion >= 23);
        DebugLog.d(TAG, "isNewPermissionModel = " + isNewPermissionModel);
        return isNewPermissionModel;
    }
}
