package org.qiyi.basecore.filedownload;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.SharedPreferencesFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class AutoDownloadConfigPolicy {
    private final static ArrayList<String> mBlackList = new ArrayList<String>();
    private static final long MAXIMUM_MEMORY = 768 * 1024L;//单位KB，表示1G
    public static  final int MAX_CPU = 2;

    static {
        mBlackList.add("Coolpad 5216D");
        mBlackList.add("Coolpad 5219");
        mBlackList.add("Coolpad 5892");
        mBlackList.add("Coolpad 5952");
        mBlackList.add("Coolpad 7620L");
        mBlackList.add("Coolpad 8702");
        mBlackList.add("Coolpad 8702D");
        mBlackList.add("Coolpad 8729");
        mBlackList.add("Coolpad 8730L");
        mBlackList.add("Coolpad 9190L");
        mBlackList.add("Coolpad 9190_T00");
        mBlackList.add("EBEN T7");
        mBlackList.add("GN9000L");
        mBlackList.add("HUAWEI Y321-C00");
        mBlackList.add("HUAWEI G520-0000");
        mBlackList.add("HUAWEI C8813D");
        mBlackList.add("HUAWEI C8813");
        mBlackList.add("koobee M2");
        mBlackList.add("Lenovo S810t");
        mBlackList.add("Lenovo A355e");
        mBlackList.add("Lenovo A750e");
        mBlackList.add("Lenovo A560");
        mBlackList.add("MI 1S");
        mBlackList.add("R2017");
        mBlackList.add("N5117");
        mBlackList.add("R6007");
        mBlackList.add("R7007");
        mBlackList.add("R8000");
        mBlackList.add("R8007");
        mBlackList.add("R831S");
        mBlackList.add("TCL S830U");
        mBlackList.add("vivo Y22L");
        mBlackList.add("vivo Xplay");
        mBlackList.add("vivo Xplay3S");
        mBlackList.add("ZTE Q802T");
        mBlackList.add("ZTE Q505T");
        mBlackList.add("ZTE Q301C");
        mBlackList.add("ZTE Grand S II LTE");
        mBlackList.add("8720");
        mBlackList.add("6050Y");
    }

    private static int getCpuNum() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static long getTotalMemo() {
        long mTotal = MAXIMUM_MEMORY + 1;
        // /proc/meminfo读出的内核信息进行解释
        String path = "/proc/meminfo";
        String content = null;
        BufferedReader br = null;
        FileInputStream fis = null;
        InputStreamReader isr= null;
        try {
            fis = new FileInputStream(path);
            isr = new InputStreamReader(fis,"UTF-8");
            br = new BufferedReader(isr);

            String line;
            if ((line = br.readLine()) != null) {
                content = line;
            }
        } catch (FileNotFoundException e) {
            ExceptionUtils.printStackTrace(e);
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        } catch (Exception e){
            ExceptionUtils.printStackTrace(e);
        }  finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
            if(isr != null) {
                try{
                    isr.close();
                }catch (IOException e){
                    ExceptionUtils.printStackTrace(e);
                }
            }

            if(fis != null) {
                try{
                    fis.close();
                }catch (IOException e){
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }

        if(TextUtils.isEmpty(content)){
            return mTotal;
        }



        try {

            if(content == null) {
                return mTotal;
            }

            int begin = content.indexOf(':');
            int end = content.indexOf('k');

            if (begin != -1 && end != -1) {
                // 截取字符串信息
                if (begin < end) {
                    content = content.substring(begin + 1, end).trim();
                    if (TextUtils.isDigitsOnly(content)) {
                        mTotal = Long.parseLong(content);
                    }
                }
            }
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }

        return mTotal;
    }

    private static boolean isLowQualityDevice() {
        return (getCpuNum() <= MAX_CPU || getTotalMemo() <= MAXIMUM_MEMORY);
    }

    private static boolean isBelowKitkatVersion() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT;
    }

    public static boolean canAutoDownloadPlugin(Context context) {
        int needDelayService = SharedPreferencesFactory.get(context, "SP_DELAY_FILEDOWNLOAD", 1);
        return needDelayService != 1 || !mBlackList.contains(android.os.Build.MODEL) || !isLowQualityDevice() || !isBelowKitkatVersion();
    }
}
