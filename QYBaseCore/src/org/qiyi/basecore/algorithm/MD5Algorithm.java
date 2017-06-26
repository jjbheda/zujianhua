package org.qiyi.basecore.algorithm;

import org.qiyi.android.corejar.debug.DebugLog;

import java.security.MessageDigest;

public class MD5Algorithm {
    private static final String TAG = "MD5Algorithm";

    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4',
            '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * MD5算法，返回小写
     * @param str
     * @return
     */
    public static String md5(String str) {

        return md5(str, true);
    }


    /**
     * MD5算法，支持大小写
     * @param str
     * @param isLower
     *      true：返回小写字符
     *      false： 返回大写字符
     * @return
     */
    public static String md5(String str, boolean isLower){

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(str.getBytes("UTF-8"));
            byte[] byteArray = messageDigest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte b : byteArray) {
                hexString.append(HEX_DIGITS[b >> 4 & 0xf]);
                hexString.append(HEX_DIGITS[b & 0xf]);
            }

            String res = hexString.toString();
            if(!isLower){
                //大写
                res = res.toUpperCase();
            }

            return res;
        } catch (Exception e) {
            DebugLog.e(TAG, "md5 error " + e.getMessage());
        }

        return str;
    }
}
