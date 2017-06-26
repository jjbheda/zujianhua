package org.qiyi.basecore.utils;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.constant.BaseCoreSPConstants;
import org.qiyi.basecore.storage.StorageCheckor;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Author:yuanzeyao <br/>
 * Date:16/5/25 13:52 <br/>
 * email:yuanzeyao@qiyi.com <br/>
 */
public class CommonUtils {
    private static final String TAG = "CommonUtils";
    private static final String KEY_DEBUG = "#QY#";

    /**
     * 是否使用两种方式扫描sd卡然后合并
     *
     * @param mContext
     * @return
     */
    public static boolean scanSDDoubleAndMerge(Context mContext) {
        if (mContext != null) {

            return SharedPreferencesFactory.get(mContext, BaseCoreSPConstants.KEY_SCAN_SD_DOUBLE, false,
                    BaseCoreSPConstants.BASE_CORE_SP_FILE_MULTIPRO);
        }
        return false;
    }

    public static void setScanSDType(Context mContext, int type) {
        if (mContext != null) {
           SharedPreferencesFactory.set(mContext, BaseCoreSPConstants.KEY_SCAN_SD_DOUBLE, type == 1,
                    BaseCoreSPConstants.BASE_CORE_SP_FILE_MULTIPRO);
        }
    }

    /**
     * 存储Phone id,包括imei,mac,android_id
     *
     * @param mContext
     * @param key
     * @param value
     */
    public static void savePhoneId(Context mContext, String key, String value) {
        SharedPreferencesFactory.set(mContext, key, value, BaseCoreSPConstants.BASE_CORE_SP_FILE);
    }

    /**
     * 获取手机id,包括imei,mac,android_id
     *
     * @param mContext
     * @param key
     */
    public static String getPhoneId(Context mContext, String key) {
        return SharedPreferencesFactory.get(mContext, key, "", BaseCoreSPConstants.BASE_CORE_SP_FILE);
    }

    /**
     * debug模式下判断是否只走代理
     *
     * @param mContext
     * @return
     */
    public static boolean debugUseProxyMode(Context mContext) {
        if (mContext != null) {

            return SharedPreferencesFactory.get(mContext, BaseCoreSPConstants.DEBUG_KEY_NET_PROXY_MODE,
                    false, BaseCoreSPConstants.BASE_CORE_SP_FILE_MULTIPRO);
        }

        return false;
    }

    /**
     * debug模式下设置代理模式
     * true：只走系统代理（必须设置代理）
     * false：默认行为
     *
     * @param mContext
     */
    public static void debugSetProxyMode(Context mContext, boolean value) {
        if (mContext != null) {
            DebugLog.v(TAG, "debugSetProxyMode: " + value);
            SharedPreferencesFactory.set(mContext, BaseCoreSPConstants.DEBUG_KEY_NET_PROXY_MODE,
                    value, BaseCoreSPConstants.BASE_CORE_SP_FILE_MULTIPRO);
        }
    }

    /**
     * 是否信任*.iqiyi.com的https证书
     */
    public static boolean isBeliveCertificate(Context mContext) {
        boolean isBelive = false;
        try {
            isBelive = SharedPreferencesFactory.get(mContext, BaseCoreSPConstants.KEY_ISBELIVECERTIFICATE,
                    false, BaseCoreSPConstants.BASE_CORE_SP_FILE_MULTIPRO);
        } catch (Throwable ignore) {
        }
        return isBelive;
    }

    /**
     * 设置是否信任*.iqiyi.com的https证书
     */
    public static void setBeliveCertificate(Context mContext, boolean value) {
        if (mContext != null) {
            SharedPreferencesFactory.set(mContext, BaseCoreSPConstants.KEY_ISBELIVECERTIFICATE,
                    value, BaseCoreSPConstants.BASE_CORE_SP_FILE_MULTIPRO);
        }
    }



    /**
     * For Debug UI
     * 在搜索栏里输入#QY#1124(当前日期,格式为MMDD)开启debug模式
     *
     * @param context
     * @return
     */
    public static boolean isAvailableDebug(Context context) {
        if (context == null || !DebugLog.isDebug()) {
            return false;
        }
        String key = SharedPreferencesFactory.get(context, SharedPreferencesConstants.QIYI_DEBUG_KEY, "");
        if (!key.startsWith(KEY_DEBUG)) {
            return false;
        }
        key = key.replaceFirst(KEY_DEBUG, "");
        Calendar c = Calendar.getInstance();
        int month = c.get(Calendar.MONTH) + 1;
        String monthStr;
        if (month < 10) {
            monthStr = "0" + month;
        } else {
            monthStr = String.valueOf(month);
        }
        int day = c.get(Calendar.DAY_OF_MONTH);
        String dayStr;
        if (day < 10) {
            dayStr = "0" + day;
        } else {
            dayStr = String.valueOf(day);
        }
        String password = monthStr + dayStr;
        if (key.equals(password)) {
            return true;
        }
        return false;
    }

    public static void writeStringIntoFile(String result, String file_name, Context context, String time, String result_code) {
        if (context == null) {
            return;
        }
        String DIR_PATH = getDirPath(context);
        File dirFile = new File(DIR_PATH);
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }
        if (!dirFile.isDirectory()) {
            dirFile.delete();
            dirFile.mkdir();
        }
        File logFile = new File(DIR_PATH + "/" + file_name);
        ArrayList<String> lines = new ArrayList();
        if (logFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(logFile));
                String str = "";
                while ((str = reader.readLine()) != null) {
                    String newStr = str;
                    lines.add(newStr);

                }

            } catch (FileNotFoundException e) {
                ExceptionUtils.printStackTrace(e);
            } catch (IOException e) {
                ExceptionUtils.printStackTrace(e);
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        ExceptionUtils.printStackTrace(e);
                    }
                }
            }
        }
        lines.add(getJSONString(result, time, result_code));
        int size = lines.size();
        ArrayList<String> newlines = new ArrayList();
        if (size > 20) {
            int extra = size - 20;
            for (int num = extra; num < size; num++) {
                newlines.add(lines.get(num));
            }
        } else {
            newlines.addAll(lines);
        }
        if (logFile.exists()) {
            logFile.delete();
        }
        try {
            logFile.createNewFile();
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        }
        StringBuffer finalResult = new StringBuffer();
        int currentSize = newlines.size();
        for (int num = 0; num < currentSize; num++) {
            finalResult.append(newlines.get(num));
            if (num != (currentSize - 1)) {
                finalResult.append("\n");
            }
        }
        FileWriter fw = null;
        try {
            fw = new FileWriter(DIR_PATH + "/" + file_name, true);
            fw.write(finalResult.toString());
            fw.close();
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if(fw != null){
                try {
                    fw.close();
                }catch (IOException e){
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
    }


    public static String getDirPath(Context context) {
        File file = StorageCheckor.getInternalStorageFilesDir(context, "push_log");
        if (file == null) {
            return "";
        }
        return file.getAbsolutePath();
    }

    public static String getCurrentDateTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日    HH:mm:ss     ");
        Date curDate = new Date(System.currentTimeMillis()); //get current time
        return formatter.format(curDate);
    }

    private static String getJSONString(String message, String time, String result_code) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("message", message);
            jsonObject.put("time", time);
            jsonObject.put("result_code", result_code);
        } catch (JSONException e) {
            ExceptionUtils.printStackTrace(e);
        }

        String result = jsonObject.toString();
        return result;
    }

    /**
     * 对象反序列化，byte[]数组转Serializable对象
     *
     * @param bytes
     * @return
     */
    public static Object byteArray2Object(byte[] bytes) {
        Object obj = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;

        try {
            bis = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bis);
            obj = ois.readObject();
            ois.close();
            bis.close();
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if (null != ois) {
                try {
                    ois.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }

            if (null != bis) {
                try {
                    bis.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
        return obj;
    }

    /**
     * 对象序列化，Serializable对象转byte[]数组
     *
     * @param obj
     * @return
     */
    public static byte[] object2ByteArray(Serializable obj) {
        if (null == obj) {
            return null;
        }

        byte[] bytes = null;
        ByteArrayOutputStream bo = null;
        ObjectOutputStream oo = null;
        try {
            bo = new ByteArrayOutputStream();
            oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);
            bytes = bo.toByteArray();
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if (null != oo) {
                try {
                    oo.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
            if (null != bo) {
                try {
                    bo.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
        return bytes;
    }
}
