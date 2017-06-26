package org.qiyi.basecore.utils;

import android.Manifest;
import android.content.Context;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.algorithm.MD5Algorithm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class HardwareConfigurationUtils {
    /**
     * 判断设备知否支持陀螺仪
     */
    public static boolean isSupportGyro(Context context) {
        if (null != context) {
            SensorManager mgr = (SensorManager) (context.getSystemService(Context.SENSOR_SERVICE));
            return mgr != null;
        }
        return false;
    }

    /**
     * 获取CPU核心数
     */
    public static int getCpuNum() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Get cpu freq
     */
    public static String getCPUFreq() {
        return String.valueOf(getPhoneCPUFreqPrivate());
    }

    private static long sMaxCPUFreq = -1;

    private static long getPhoneCPUFreqPrivate() {
        if (sMaxCPUFreq != -1) {
            return sMaxCPUFreq;
        }
        int cores = getCpuNum();
        ArrayList<Long> arrayList = new ArrayList<Long>();
        for (int i = 0; i < cores; i++) {
            BufferedReader br = null;
            FileReader fr = null;
            try {
                File file = new File(String.format(Locale.getDefault(), "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq", i));
                if (!file.exists()) {
                    continue;
                }
                fr = new FileReader(file);
                br = new BufferedReader(fr);
                String freq = br.readLine();
                if (TextUtils.isEmpty(freq)) {
                    continue;
                }

                arrayList.add(Long.valueOf(freq.trim()));
            } catch (IOException e) {
                ExceptionUtils.printStackTrace(e);
                return -1;
            } catch (NumberFormatException e) {
                ExceptionUtils.printStackTrace(e);
            } finally {
                if (null != br) {
                    FileUtils.silentlyCloseCloseable(br);
                }
                if(fr != null){
                    FileUtils.silentlyCloseCloseable(fr);
                }
            }
        }
        if (!arrayList.isEmpty()) {
            sMaxCPUFreq = Collections.max(arrayList);
        }
        return sMaxCPUFreq;
    }

    private static long sTotalDeviceMem = -1;

    /**
     * 获取设备内存大小(M)
     */
    public static long getTotalMemo() {
        if (sTotalDeviceMem != -1) {
            return sTotalDeviceMem;
        }
        // /proc/meminfo读出的内核信息进行解释
        String path = "/proc/meminfo";
        String content = null;
        BufferedReader br = null;
        InputStreamReader isr = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path);
            isr =new InputStreamReader(fis, "UTF-8");
            br = new BufferedReader(isr);

            String line;
            if ((line = br.readLine()) != null) {
                content = line;
            }
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if (br != null) {
                FileUtils.silentlyCloseCloseable(br);
            }
            if(isr != null){
                FileUtils.silentlyCloseCloseable(isr);
            }
            if(fis != null){
                FileUtils.silentlyCloseCloseable(fis);
            }
        }



        try {

            if (content == null) {
                return 1;
            }

            int begin = content.indexOf(':');
            int end = content.indexOf('k');

            if (begin != -1 && end != -1) {
                // 截取字符串信息
                if (begin < end) {
                    content = content.substring(begin + 1, end).trim();
                    if (TextUtils.isDigitsOnly(content)) {
                        sTotalDeviceMem = Long.parseLong(content) / 1024;
                    }
                }
            }
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
        return sTotalDeviceMem;
    }

    /**
     * Get GPU freq， only few device support this feature
     *
     * @return
     */
    public static String getPhoneGpuFreq() {
        String rt = null;
        File file = new File("/proc/gpufreq/gpufreq_opp_dump");
        if (!file.exists()) {
            return "";
        }
        BufferedReader br = null;
        FileReader fr = null;
        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);
            ArrayList<Integer> list = new ArrayList<Integer>();
            String line = br.readLine();
            while (line != null) {
                int start = line.lastIndexOf("freq = ");
                int end = line.indexOf(",");
                String r = line.substring(start + 7, end);
                int hz = 0;
                try {
                    hz = Integer.parseInt(r.trim());
                    list.add(hz);
                } catch (Exception e) {
                    hz = -1;
                }
                line = br.readLine();
            }
            int mhz = Collections.max(list).intValue() / 1000;//transfer to be MHZ
            rt = String.valueOf(mhz);
        } catch (Exception e) {
            rt = "";
        } finally {
            if (br != null) {
                FileUtils.silentlyCloseCloseable(br);
            }
            if(fr != null) {
                FileUtils.silentlyCloseCloseable(fr);
            }
        }

        return rt;
    }

    private static String sIMEI;

    public static String getImei(Context context) {
        if (null == context) {
            return "";
        }
        if (!StringUtils.isEmpty(sIMEI)) {
            return sIMEI;
        }
        if (context != null) {
            boolean hasPermission = PermissionUtil.hasSelfPermission(context.getApplicationContext(), Manifest.permission.READ_PHONE_STATE);
            if (hasPermission) {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (null != tm) {
                    String deviceId = tm.getDeviceId();
                    if (!StringUtils.isEmpty(deviceId)) {
                        sIMEI = deviceId;
                    }
                }
            }
        }
        return sIMEI;
    }

    private static String sEncodedMacAddress;
    private static String sOriginalMacAddress;

    /**
     * 获取mac地址, 复制getIMEI代码
     *
     * @param context
     * @param needOriginal true 做URLEncoder.encode(temp, "utf-8") encode, false 做md5 encode
     */
    public static String getMacAddress(Context context, boolean needOriginal) {
        if (null == context) {
            return null;
        }
        // 判断mac缓存是否为空，不为空直接返回。
        if (!StringUtils.isEmpty(sEncodedMacAddress) && !needOriginal) {
            return sEncodedMacAddress;
        }

        if (!StringUtils.isEmpty(sOriginalMacAddress) && needOriginal) {
            return sOriginalMacAddress;
        }

        WifiInfo wifiInfo = null;

        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            wifiInfo = wifiManager.getConnectionInfo();
        } catch (Exception e) {
            if (DebugLog.isDebug()) {
                ExceptionUtils.printStackTrace(e);
            }
        }

        if (null != wifiInfo) {
            String temp = wifiInfo.getMacAddress();
            if (!StringUtils.isEmpty(temp)) {
                if (needOriginal) {
                    try {
                        sOriginalMacAddress = URLEncoder.encode(temp, "utf-8");
                    } catch (Exception e) {
                        if (DebugLog.isDebug()) {
                            ExceptionUtils.printStackTrace(e);
                        }
                    }
                } else {
                    sEncodedMacAddress = MD5Algorithm.md5(temp.toUpperCase()).toLowerCase();
                }
            }
        }

        // 判断通过iwifimanager取得的mac是否为空，不为空则进行缓存。
        if (needOriginal) {
            return sOriginalMacAddress;
        } else {
            return sEncodedMacAddress;
        }
    }
}
