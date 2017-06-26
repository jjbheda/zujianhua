package org.qiyi.basecore.utils;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import org.qiyi.android.corejar.debug.DebugLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 该类提供获取设备信息相关原生工具方法
 * 不做缓存功能，不依赖基线业务，可独立输出
 * 如IMEI，Mac Address, Device信息等
 * 需要缓存功能的API，请使用QYContext工程的API
 */
public class DeviceUtil {
    private static final String TAG = "DeviceUtil";

    // 错误Mac地址列表
    public static final List<String> FAILMAC = new ArrayList<>();

    static {
        FAILMAC.add("02:00:00:00:00:00");
        FAILMAC.add("0");
    }

    /**
     * 获取Device Name
     *
     * @return
     */
    public static String getDeviceName() {
        return android.os.Build.MANUFACTURER + "-" + Build.MODEL;
    }


    /**
     * 获取Device Name 2
     * 对应URL中的OS值
     *
     * @return
     */
    public static String getDeviceName2() {
        return Build.MANUFACTURER + "-" + Build.PRODUCT;
    }


    /**
     * 获取系统Release版本
     *
     * @return
     */
    public static String getOSVersionInfo() {
        return Build.VERSION.RELEASE;
    }


    /**
     * 获取URL中对应的UA信息
     *
     * @return
     */
    public static String getMobileModel() {
        return android.os.Build.MODEL;
    }


    /**
     * 获取UA信息
     *
     * @return
     */
    public static String getUserAgentInfo() {

        return "Android" + getOSVersionInfo() + "-" + getDeviceName2() + "(" + getMobileModel() + ")";
    }


    /**
     * 设备是否root
     *
     * @return
     */
    public static boolean isJailBreak() {
        String[] dirs = new String[]{"/system/bin/", "/system/xbin/", "/data/local/xbin/", "/data/local/bin/", "/system/sd/xbin/"};

        for (String dir : dirs) {
            if (new File(dir + "su").exists()) {
                return true;
            }
        }

        return false;
    }


    /**
     * 判断是否是华为EMUI系统
     *
     * @return true 华为EMUI系统
     * false 不是
     */
    public static boolean isHuaweiEmui() {
        boolean isEMUI = false;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method m = c.getDeclaredMethod("get", String.class);
            m.setAccessible(true);
            String s = (String) m.invoke(null, "ro.build.version.emui");
            isEMUI = !TextUtils.isEmpty(s);

            DebugLog.v(TAG, "DeviceUtils.isHuaweiEmui() " + s);
        } catch (Throwable e) {
            // ignore
            DebugLog.e(TAG, "DeviceUtils.isHuaweiEmui() reflection failed: " + e);
        }

        DebugLog.v(TAG, "DeviceUtils.isHuaweiEmui() isEmui=" + isEMUI);
        return isEMUI;
    }


    /**
     * 使用Android系统原生API获取IMEI号，该方法不做缓存功能
     * 如需缓存功能，请使用QYContext工程中的API
     *
     * @return
     */
    public static String getIMEI(@NonNull Context context) {

        String imei = "";
        boolean hasPermission = PermissionUtil.hasSelfPermission(context.getApplicationContext(), Manifest.permission.READ_PHONE_STATE);
        if (hasPermission) {
            try {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (null != tm) {
                    imei = tm.getDeviceId();
                }
            } catch (Exception e) {
                DebugLog.e(TAG, "getIMEI through system api exception " + e.getMessage());
                imei = "";
            }
        }
        // NULL值转换为"", avoid NPE
        if(imei == null){
            imei = "";
        }

        return imei;
    }


    /**
     * 使用Android系统原生API获取Mac地址，该方法不做缓存功能
     * 该方法返回的Mac地址未做任何特殊处理
     * 如需缓存功能，请使用QYContext工程中的API
     *
     * @param context
     * @return
     */
    public static String getMacAddress(@NonNull Context context) {

        String macAddr = "";
        boolean hasPermission = PermissionUtil.hasSelfPermission(context.getApplicationContext(),
                Manifest.permission.ACCESS_WIFI_STATE);
        if (hasPermission) {
            try {
                WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                if (wm != null) {
                    WifiInfo info = wm.getConnectionInfo();
                    if (info != null) {
                        macAddr = info.getMacAddress();
                    }
                }
            } catch (Exception e) {
                DebugLog.e(TAG, "getMacAddress through system api exception " + e.getMessage());
                macAddr = "";
            }
        }

        // 修复Android M上使用系统API无法获取Mac Address的问题
        if (TextUtils.isEmpty(macAddr) || FAILMAC.contains(macAddr)) {
            macAddr = getMacByConfig();
        }
        // NULL值转换为"", avoid NPE
        if(macAddr == null){
            macAddr = "";
        }

        return macAddr;
    }


    /**
     * 通过读取配置文件读取mac address
     *
     * @return
     */
    private static String getMacByConfig() {
        try {
            String macAddr = getMacAddrByInterfaceName("wlan0");
            DebugLog.v(TAG, "getMacByConfig:" + macAddr);
            if (TextUtils.isEmpty(macAddr)) {
                macAddr = getMacAddrByInterfaceName("eth0");
                DebugLog.v(TAG, "getMacByConfig2:" + macAddr);
            }
            return macAddr;
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
        return "";
    }

    /**
     * 通过读取配置文件获取mac地址
     *
     * @param interfaceName
     * @return
     */
    private static String getMacAddrByInterfaceName(String interfaceName) throws SocketException {
        if (TextUtils.isEmpty(interfaceName)) {
            return "";
        }
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface mNetWorkInterface = interfaces.nextElement();
            String mInterfaceName = mNetWorkInterface.getName();
            if (TextUtils.isEmpty(mInterfaceName) || !mInterfaceName.equals(interfaceName)) {
                continue;
            }

            byte[] addr = mNetWorkInterface.getHardwareAddress();
            if (addr == null || addr.length == 0) {
                continue;
            }

            StringBuilder buf = new StringBuilder();
            for (byte b : addr) {
                buf.append(String.format("%02X:", b));
            }
            if (buf.length() > 0) {
                buf.deleteCharAt(buf.length() - 1);
            }
            return buf.toString();
        }
        return "";


    }


    /**
     * 获取可用内存信息
     *
     * @return
     */
    public static long getAvailMemorySize() {

        return Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory();
    }


    /**
     * 得到进程名字
     *
     * @param context
     * @return
     */
    public static String getCurrentProcessName(@NonNull Context context) {

        int pid = android.os.Process.myPid();
        ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        try {
            for (ActivityManager.RunningAppProcessInfo process : manager.getRunningAppProcesses()) {
                if (process.pid == pid) {
                    return process.processName;
                }
            }
        } catch (Exception e) {
            // ActivityManager.getRunningAppProcesses() may throw NPE in some custom-made devices (oem BIRD)
        }

        //try to read process name in /proc/pid/cmdline if no result from activity manager
        String cmdline = null;
        BufferedReader processFileReader = null;
        FileReader fr = null;
        try {
            fr = new FileReader(String.format("/proc/%d/cmdline", android.os.Process.myPid()));
            processFileReader = new BufferedReader(fr);
            cmdline = processFileReader.readLine().trim();
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if (processFileReader != null) {
                FileUtils.silentlyCloseCloseable(processFileReader);
            }
            if(fr != null) {
                FileUtils.silentlyCloseCloseable(fr);
            }
        }

        return cmdline;
    }

}
