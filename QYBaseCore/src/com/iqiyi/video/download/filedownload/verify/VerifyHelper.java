package com.iqiyi.video.download.filedownload.verify;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.qiyi.basecore.utils.ExceptionUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by songguobin on 2016/12/19.
 */
public class VerifyHelper {

    private static final String TAG = "VerifyHelper";

    /**
     * 公钥，检验patch是否完整
     */
    private static final String sPublicKey =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDyIleNBqpqoGCSfsl6XgoGhDUf"
                    + "vowQFZWgMnQuZzBQS+1WwRvAKGZrqGP+n8fVKyQBU0OSvbUN2aQrZVzohmkbx2nY"
                    + "0pIWiBKKhRxRsv9hpUp/w3ItQ3Ve7cU07qrdvWaBkdCtujQm2U4zHPU7V0M2mWPe"
                    + "Xxs+WK4orD6SZGxodwIDAQAB";


    /**
     * 校验文件是否未被修改
     *
     * @param patchPath 文件存储路径
     * @param sig       证书文件
     * @return true:表示文件未被修改，false：表示文件已被修改
     */
    public static boolean isDownloadFileModified(String patchPath, String sig) {
        //获取crccfg文件数据对象
        byte[] patch = inputToBytes(patchPath);
        //获取sign签名文件字节数组
        byte[] sign = decryptBASE64(sig);
        if (patch == null || sign == null) {
            Log.d(TAG, " patch == null or sign == null");
            return false;
        }
        boolean isUnModified = verify(patch, sPublicKey, sign);
        Log.d(TAG, "patch unmodified = " + isUnModified);
        return isUnModified;
    }

    /**
     * BASE64解密
     *
     * @param key
     * @return
     * @throws Exception
     */
    private static byte[] decryptBASE64(String key) {
        return Base64.decode(key, Base64.DEFAULT);
    }

    /**
     * BASE64加密
     *
     * @param key
     * @return
     * @throws Exception
     */
    private static String encryptBASE64(byte[] key) {
        return Base64.encodeToString(key, Base64.DEFAULT);
    }

    /**
     * 校验数字签名
     *
     * @param data      加密数据
     * @param publicKey 公钥
     * @param sign      数字签名
     * @return
     * @throws Exception
     */
    private static boolean verify(byte[] data, String publicKey, byte[] sign) {
        if (data == null) {
            Log.d(TAG, " data == null");
            return false;
        }
        if (TextUtils.isEmpty(publicKey)) {
            Log.d(TAG, " the publickey is empty or == null");
            return false;
        }
        if (sign == null) {
            Log.d(TAG, " sign == null");
            return false;
        }

        //解密公钥
        byte[] keyBytes = decryptBASE64(publicKey);
        //构造X509EncodedKeySpec对象
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
        //指定加密算法
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            //取公钥匙对象
            PublicKey publicKey2 = keyFactory.generatePublic(x509EncodedKeySpec);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey2);
            signature.update(data);
            //验证签名是否正常
            return signature.verify(sign);
        } catch (NoSuchAlgorithmException e) {
            ExceptionUtils.printStackTrace(e);
        } catch (InvalidKeySpecException e) {
            ExceptionUtils.printStackTrace(e);
        } catch (InvalidKeyException e) {
            ExceptionUtils.printStackTrace(e);
        } catch (SignatureException e) {
            ExceptionUtils.printStackTrace(e);
        }
        return false;
    }


    private static byte[] inputToBytes(String inputPath) {
        if (TextUtils.isEmpty(inputPath)) {
            return null;
        }

        FileInputStream input = null;
        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream();
            input = new FileInputStream(inputPath);
            byte[] buf = new byte[1024];
            int len;
            while ((len = input.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
            return out.toByteArray();
        } catch (FileNotFoundException e) {
            ExceptionUtils.printStackTrace(e);
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        } finally {

            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }

        return null;
    }
}


