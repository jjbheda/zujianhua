package org.qiyi.basecore.filedownload;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.os.Environment;

import org.qiyi.basecore.storage.StorageCheckor;

/**
 * @author kangle 文件下载的工具类
 */
public class FileDownloadTools {

    /**
     * @param context
     * @return 返回内部下载路径（只有在外部存储不可用的情况下才会使用）
     */
/*    public static String getDownloadInternalStoragePath(Context context) {
        return context.getFilesDir().getAbsolutePath();
    }*/

    /**
     * @param context
     * @return
     */
    public static File getDownloadPath(Context context) {

        File ret = StorageCheckor.getUserPreferFilesDir(context, "Download");

        return ret == null ? context.getFilesDir() : ret;
    }

    public synchronized static File fixDownloadFile(Context context, String downloadedFileAbsolutePath) throws IOException {
        File downloadFile = new File(downloadedFileAbsolutePath);
        if (!downloadFile.exists()) {
            File parentDirectory = new File(downloadFile.getParent());
            if (!parentDirectory.exists()) {
                context.getExternalFilesDir(null);
                parentDirectory.mkdirs();
            }
            if (!downloadFile.createNewFile()) {
                throw new IOException("can't create downloaded file: " + downloadFile.getAbsolutePath());
            }
        }

        return downloadFile;
    }
    
    /**
     * 将下载地址转换加密
     * 
     * @param key
     * @return
     */
    public static String hashKeyForDisk(String key)
    {
        String cacheKey;
        try
        {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        }
        catch (NoSuchAlgorithmException e)
        {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }
    
    /**
     * 字节与字符转换
     * 
     * @param bytes
     * @return
     */
    private static String bytesToHexString(byte[] bytes)
    {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++)
        {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1)
            {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
