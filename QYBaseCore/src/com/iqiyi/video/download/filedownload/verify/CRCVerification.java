package com.iqiyi.video.download.filedownload.verify;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.CRC32;

/**
 * Created by songguobin on 2016/12/19.
 *
 * CRC校验
 *
 */
public class CRCVerification implements BaseVerification{

    public static final String TAG = "CRCVerification";

    static final int _BUFFER_SIZE_CRC = 1024;

    @Override
    public boolean verify(String filepath, String sign) {


        return checksumByCRC(new File(filepath),sign);

    }


    static boolean checksumByCRC(File f, String crcValue) {
        if (StringUtils.isEmpty(crcValue)) {
            return false;
        }
        byte[] buffer = getBytesBySize(f, _BUFFER_SIZE_CRC);
        if (null == buffer) {
            return false;
        }
        CRC32 crc32 = new CRC32();
        crc32.update(buffer);
        String value = StringUtils.toStr(Long.toHexString(crc32.getValue()).toUpperCase(), "");
        DebugLog.log(TAG,"input crcValue = " + crcValue);
        DebugLog.log(TAG,"calc crcValue = " + value);
        return value.equals(crcValue);
    }

    private static byte[] getBytesBySize(File f, int size) {
        if (null == f || !f.exists() || !f.canRead() || size > _BUFFER_SIZE_CRC) {
            return null;
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            byte[] buffer = new byte[size];
            int readCount = fis.read(buffer);
            if (readCount > 0) {
                return buffer;
            }
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
        return null;
    }


}
