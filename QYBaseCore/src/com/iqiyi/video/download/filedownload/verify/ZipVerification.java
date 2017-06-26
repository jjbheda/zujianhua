package com.iqiyi.video.download.filedownload.verify;

import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by songguobin on 2017/4/14.
 * <p>
 * 文件解压
 */

public class ZipVerification {

    private static final String ZIP = ".zip";

    private static final int _BUFFER_SIZE = 8192;

    private static final String FILE_SPLIT = "/";

    public static boolean unzipToSelfPath(String zipPath) {

        if (StringUtils.isEmpty(zipPath)) {
            return false;
        }

        return unzip(zipPath, zipPath2ZipDir(zipPath));

    }

    private static String zipPath2ZipDir(String zipPath) {
        return zipPath2ZipDir(zipPath, true);
    }

    private static String zipPath2ZipDir(String zipPath, boolean containsFileSeparator) {
        return StringUtils.toStr(zipPath.substring(0, zipPath.length() - ZIP.length()), "")
                + (containsFileSeparator ? File.separator : "");
    }

    private static boolean unzip(String zipPath, String unzipToPath) {
        if (StringUtils.isEmpty(zipPath) || StringUtils.isEmpty(unzipToPath)) {
            return false;
        }
        File zF = new File(zipPath);
        if (!zF.exists()) {
            return false;
        }
        File f = new File(unzipToPath);
        if (!f.exists()) {
            f.mkdir();
        }
        boolean r = true;
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zF);
            Enumeration<? extends ZipEntry> emu = zipFile.entries();
            while (emu.hasMoreElements()) {
                ZipEntry entry = emu.nextElement();
                if (unzipAndWriteOneFile(zipFile, entry, unzipToPath) < -1) {
                    r = false;
                }
            }
            return r;
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
            return false;
        } finally {
            if (null != zipFile) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
    }


    private static int unzipAndWriteOneFile(ZipFile zipFile, ZipEntry entry, String unzipPath) {
        if (null == entry) {
            return -2;
        }
        String name = StringUtils.toStr(getFromLastIndexToEnd(entry.getName(), FILE_SPLIT, false), "");
        if (StringUtils.isEmpty(name)) {
            return -1;
        }

        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(zipFile.getInputStream(entry));
            File file = new File(unzipPath + name);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos, _BUFFER_SIZE);
            int count;
            byte[] data = new byte[_BUFFER_SIZE];
            while ((count = bis.read(data, 0, _BUFFER_SIZE)) != -1) {
                bos.write(data, 0, count);
            }
            bos.flush();
            return 0;
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
            return -2;
        } finally {
            if (null != bos) {
                try {
                    bos.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
            if (null != fos) {
                try {
                    fos.close();
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
    }

    private static String getFromLastIndexToEnd(String string, String split, boolean containSplit) {
        return cutStringOfLastIndex(string, split, containSplit, false);
    }

    /**
     * Cut stirng base on last index.
     */
    private static String cutStringOfLastIndex(String string, String split, boolean containSplit, boolean fromStartToLastIndex) {
        if (StringUtils.isEmpty(string)) {
            return null;
        }
        int index = string.lastIndexOf(split);
        if (index < 1) {
            return null;
        }
        return fromStartToLastIndex ? string.substring(0, containSplit ? index + 1 : index)
                : string.substring(containSplit ? index : index + 1);
    }
}
