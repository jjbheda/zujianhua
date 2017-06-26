package com.iqiyi.video.download.filedownload.verify;

import android.text.TextUtils;

import org.qiyi.basecore.utils.ExceptionUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by songguobin on 2016/12/19.
 * <p>
 * MD5校验
 */
public class MDFiveVerification implements BaseVerification {

    @Override
    public boolean verify(String filepath, String sign) {

        String fileSign = fileMD5(filepath);

        if (!TextUtils.isEmpty(fileSign) && !TextUtils.isEmpty(sign)) {
            if (fileSign != null && fileSign.equalsIgnoreCase(sign)) {
                return true;
            }
        }

        return false;

    }


    /**
     * 文件MD5校验
     *
     * @param inputFile
     * @return
     */
    public static String fileMD5(String inputFile) {

        // 缓冲区大小
        int bufferSize = 256 * 1024;
        FileInputStream fileInputStream = null;
        DigestInputStream digestInputStream = null;
        try {
            // 拿到一个MD5转换器（同样，这里可以换成SHA1）
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            // 使用DigestInputStream
            fileInputStream = new FileInputStream(inputFile);
            digestInputStream = new DigestInputStream(fileInputStream,
                    messageDigest);
            // read的过程中进行MD5处理，直到读完文件
            byte[] buffer = new byte[bufferSize];
            while (digestInputStream.read(buffer) > 0) ;
            // 获取最终的MessageDigest
            messageDigest = digestInputStream.getMessageDigest();
            // 拿到结果，也是字节数组，包含16个元素
            byte[] resultByteArray = messageDigest.digest();
            // 同样，把字节数组转换成字符串
            return byteArrayToHex(resultByteArray);
        } catch (NoSuchAlgorithmException e) {
            ExceptionUtils.printStackTrace(e);
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if (digestInputStream != null) {
                try {
                    digestInputStream.close();
                } catch (Exception e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (Exception e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
        return null;
    }

    /**
     * 下面这个函数用于将字节数组换成成16进制的字符串
     *
     * @param byteArray
     * @return
     */
    private static String byteArrayToHex(byte[] byteArray) {
        char[] digitHex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F'};
        StringBuilder sb = new StringBuilder();
        for (byte b : byteArray) {
            sb.append(digitHex[b >> 4 & 0xf]);
            sb.append(digitHex[b & 0xf]);
        }
        return sb.toString();

    }


    /**
     * 字符串md5
     *
     * @param str
     * @return
     */
    public static String md5(String str) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(str.getBytes("UTF-8"));
            byte[] byteArray = messageDigest.digest();
            // byte数组转HEX形式
            StringBuilder sb = new StringBuilder();
            char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
            for (byte b : byteArray) {
                sb.append(hexDigits[b >> 4 & 0xf]);
                sb.append(hexDigits[b & 0xf]);
            }

            return sb.toString();
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }

        return str;
    }


}
