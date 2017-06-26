package org.qiyi.basecore.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Process;
import android.text.TextUtils;
import android.util.SparseArray;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.imageloader.ImageLoader.ImageType;
import org.qiyi.basecore.imageloader.gif.GifDecode.GifDrawableDecode;
import org.qiyi.basecore.imageloader.gif.GifDrawable;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.UIUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author dragon 磁盘缓存图片
 */
public class DiskCache {
    /**
     * 通过缓存内容进行文件夹分类。
     * 1. AD广告
     * 2. TODO 可以通过文件夹分类，实现文件存放散裂化，提高IO读取效率
     */
    public static final int DISK_CACHE_TYPE_COMMON = 0;
    public static final int DISK_CACHE_TYPE_AD = DISK_CACHE_TYPE_COMMON + 1;

    private final static String TAG = "DiskCache";
    private final static long MAX_SIZE = 20 * 1024 * 1024L;//20M
    private final static long MAX_SIZE_AD = 10 * 1024 * 1024L;//10M

    // 手机存储
    private SparseArray<File> mCacheDirMap = new SparseArray<File>(3);
    // 外部存储
    private SparseArray<File> mExternalCacheDirMap = new SparseArray<File>(3);

    //存储文件夹名(默认)
    private final static String CACHE_DIR_DEFAULT = "images" + File.separator + "default";
    private final static String CACHE_DIR_AD = "images" + File.separator + "ad";

    //正在写文件扩展名
    private final static String WRITING_FILE_EXTNAME = ".w";
    //正常显示文件扩展名
    private final static String READING_FILE_EXTNAME = ".r";
    //磁盘中图片大小统计
    private volatile long mSize = 0;
    //保存删除线程
    private Thread mDeleteThread = null;

    SparseArray<String> xxx = new SparseArray<String>(3);
    private static SparseArray<String> sDirNameTypePairs = new SparseArray<String>(3);
    private static SparseArray<Long> sDirMaxSizePairs = new SparseArray<Long>(3);

    static {
        // default
        sDirNameTypePairs.put(DISK_CACHE_TYPE_COMMON, CACHE_DIR_DEFAULT);
        sDirMaxSizePairs.put(DISK_CACHE_TYPE_COMMON, MAX_SIZE);
        // ad
        sDirNameTypePairs.put(DISK_CACHE_TYPE_AD, CACHE_DIR_AD);
        sDirMaxSizePairs.put(DISK_CACHE_TYPE_AD, MAX_SIZE_AD);
    }

    public DiskCache() {
    }

    /**
     * update disk cache max size this will set the max size for diskCacheType
     *
     * @param maxSize       (MB) equals (maxsize * 1024 * 1024)
     * @param diskCacheType
     */
    public void updateMaxSize(int maxSize, int diskCacheType) {
        sDirMaxSizePairs.put(diskCacheType, maxSize * 1024L * 1024L);
    }

    /**
     * 检查大小
     *
     * @return
     */
    public void checkSize(final Context context, final int diskCacheType) {
        if (null == context) {
            return;
        }
        if (getDirMaxSizeByType(diskCacheType) < mSize || mSize == 0) {
            DebugLog.d(TAG, "checkSize   size exceed");
            //异步删除老文件
            if (mDeleteThread == null) {
                DebugLog.d(TAG, "checkSize   size exceed thread start");

                mDeleteThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                        try {
                            File dir = getDir(context, diskCacheType);
                            ArrayList<File> list = new ArrayList<File>();
                            //遍历所有文件
                            if (dir != null) {
                                File[] files = dir.listFiles();
                                if (files != null) {
                                    mSize = 0;
                                    for (File temp : files) {
                                        if (temp != null && temp.exists() && temp.isFile()) {
                                            mSize += temp.length();
                                            list.add(temp);
                                        }
                                    }
                                }
                            }
                            //只有大小超大时执行。
                            if (mSize > getDirMaxSizeByType(diskCacheType)) {
                                Collections.sort(list, new Sorter());
                                File fd = null;
                                int count = list.size() / 3;
                                for (int i = 0; i < count; i++) {
                                    fd = list.get(i);
                                    if (fd != null && fd.exists() && fd.isFile()) {
                                        long mod = fd.lastModified();
                                        DebugLog.d(TAG, "checkSize run mod:" + mod);
                                        mSize -= fd.length();
                                        fd.delete();
                                    }
                                }
                            }
                        } catch (Exception e) {

                        }

                        mDeleteThread = null;
                    }
                });
                mDeleteThread.start();
            } else {
                DebugLog.d(TAG, "checkSize   size exceed thread has excute");
            }
        }
    }

    /**
     * 添加图片到磁盘
     * 需要兼容gif
     *
     * @param context
     * @param url
     */
    public void putBitmapToDisk(Context context, String url, Resource resource,
                                ImageType type, int diskCacheType) {
        if (resource == null || url == null || context == null) {
            DebugLog.w(TAG, "putBitmapToDisk   null");
            return;
        }

        checkSize(context, diskCacheType);

        long temp = System.currentTimeMillis();
        DebugLog.d(TAG, "putBitmapToDisk " + url);

        String hash = hashKeyForDisk(url);

        DebugLog.d(TAG, "putBitmapToDisk " + hash);

        File sourceFile = getWFile(context, hash, diskCacheType);

        DebugLog.d(TAG, "putBitmapToDisk getfile:" + (System.currentTimeMillis() - temp));
        temp = System.currentTimeMillis();

        if (sourceFile != null) {
            if (sourceFile.exists()) {
                DebugLog.d(TAG, "putBitmapToDisk sourceFile.exists()");
                sourceFile.delete();
            }
            FileOutputStream fos = null;
            try {
                sourceFile.createNewFile();
                fos = new FileOutputStream(sourceFile);

                if (!type.equals(ImageType.GIF)) {
                    Bitmap bt = (Bitmap) resource.getResource();
                    switch (type) {
                        case JPG:
                            if (bt.hasAlpha()) {
                                bt.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            } else {
                                bt.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            }
                            break;
                        case PNG:
                            bt.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            break;
                        default:
                            bt.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            break;
                    }
                } else {
                    fos.write(((GifDrawable) resource.getResource()).getData());
                }

                DebugLog.d(TAG, "putBitmapToDisk commpress:" + (System.currentTimeMillis() - temp));

                temp = System.currentTimeMillis();

                File renamefile = getRFile(context, hash, diskCacheType);
                if (renamefile != null) {
                    if (renamefile.exists()) {
                        renamefile.delete();
                    }
                    sourceFile.renameTo(getRFile(context, hash, diskCacheType));

                    DebugLog.d(TAG, "putBitmapToDisk rename:" + (System.currentTimeMillis() - temp));
                    File rFile = getRFile(context, hash, diskCacheType);
                    if(rFile != null) {
                        mSize += rFile.length();
                    }
                    temp = System.currentTimeMillis();
                }
            } catch (Exception e) {
                DebugLog.w(TAG, "putBitmapToDisk e:" + e);
                if (sourceFile.exists()) {
                    sourceFile.delete();
                }

            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e1) {
                        DebugLog.w(TAG, "putBitmapToDisk e1:" + e1);
                    }
                }
            }

        }

        DebugLog.d(TAG, "putBitmapToDisk end:" + (System.currentTimeMillis() - temp));
    }


    /**
     * 从磁盘读取图片
     *
     * @param context             上下文
     * @param url                 图片地址
     * @param type                图片类型
     * @param isFullQuality       是否全品质加载图片
     * @param diskCacheType       磁盘缓存类型
     * @param isLoadLocalExistImg 是否是本地存储的图片，这个时候 url 就是一个本地文件夹路径，而不是网络地址
     * @return
     */
    public Resource<?> getBitmapFromDisk(Context context, String url, ImageType type, boolean isFullQuality,
                                         int diskCacheType, boolean isLoadLocalExistImg) {
        if (null == context) {
            return null;
        }
        Bitmap ret = null;
        Resource retRes = null;

        DebugLog.d(TAG, "getBitmapFromDisk " + url);

        File bitmapOrGiffile = null;
        if (isLoadLocalExistImg) {
            bitmapOrGiffile = new File(url);
        } else {
            String hash = hashKeyForDisk(url);
            bitmapOrGiffile = getRFile(context, hash, diskCacheType);
        }
        if (bitmapOrGiffile != null && bitmapOrGiffile.exists()) {
            if (!type.equals(ImageType.GIF)) {//静态图然原来的逻辑
                final BitmapFactory.Options options = new BitmapFactory.Options();

                try {
                    if (!isFullQuality) {
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(bitmapOrGiffile.getAbsolutePath(), options);
                        options.inSampleSize = UIUtils.computeSampleSize(options, 480, 480 * 800);
                        options.inPreferredConfig = Config.RGB_565;
                    }
                    options.inJustDecodeBounds = false;
                    options.inDither = false;
                    options.inPurgeable = true;
                    options.inInputShareable = true;
                    ret = BitmapFactory.decodeFile(bitmapOrGiffile.getAbsolutePath(), options);
                    switch (type) {
                        case CIRCLE: {
                            Bitmap temp = UIUtils.toRoundBitmap(ret);

                            if (temp != null) {
                                ret.recycle();
                                ret = temp;
                            }
                        }
                        break;

                        default:
                            break;
                    }
                    if (ret != null) {
                        retRes = new Resource();
                        retRes.setResource(ret);
                        return retRes;
                    }

                } catch (Exception e) {
                    DebugLog.w(TAG, "getBitmapFromDisk " + e);
                } catch (OutOfMemoryError oe) {
                    DebugLog.w(TAG, "getBitmapFromDisk " + oe);
                    System.gc();
                }
            } else if (type.equals(ImageType.GIF)) {//动态图返回GifDrawable
                FileInputStream is = null;
                try {
                    is = new FileInputStream(bitmapOrGiffile);
                    GifDrawable gifDrawable = new GifDrawableDecode(context).decode(is, 0, 0);
                    if (gifDrawable != null) {
                        retRes = new Resource();
                        retRes.setResource(gifDrawable);
                        return retRes;
                    }
                } catch (Exception e) {
                    DebugLog.d(GifDrawable.DEBUGKEY + TAG, "getBitmapFormDisk" + e);
                    ExceptionUtils.printStackTrace(e);
                } catch (OutOfMemoryError oe) {
                    DebugLog.w(TAG, "getBitmapFromDisk " + oe);
                    DebugLog.d(GifDrawable.DEBUGKEY + TAG, "gifdrawable内存溢出！" + oe);
                    System.gc();
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                            is = null;
                        } catch (IOException e) {
                            DebugLog.d(GifDrawable.DEBUGKEY + TAG, "本地gif文件流关闭失败！");
                            ExceptionUtils.printStackTrace(e);
                        }
                    }
                }
            }
        }
        return retRes;
    }

    /**
     * 从磁盘读取图片
     *
     * @param context
     * @param url
     * @return
     */
    public Resource<?> getBitmapFromDisk(Context context, String url, ImageType type, boolean isFullQuality, int diskCacheType) {
        return getBitmapFromDisk(context, url, type, isFullQuality, diskCacheType, false);
    }


    /**
     * 判断磁盘是否有图片
     *
     * @param context
     * @param url
     * @return
     */
    public boolean hasResource(Context context, String url, int diskCacheType) {
        String hash = hashKeyForDisk(url);

        DebugLog.d(TAG, "getBitmapFromDisk " + hash);

        File bitmapfile = getRFile(context, hash, diskCacheType);
        if (bitmapfile != null && bitmapfile.exists()) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否正在写
     *
     * @param context
     * @param url
     * @return
     */
    public boolean isWritingDisk(Context context, String url, int diskCacheType) {
        String hash = hashKeyForDisk(url);

        DebugLog.d(TAG, "isWritingDisk " + hash);

        File bitmapfile = getWFile(context, hash, diskCacheType);
        if (bitmapfile != null && bitmapfile.exists()) {
            DebugLog.d(TAG, "isWritingDisk true");
            return true;
        }
        DebugLog.d(TAG, "isWritingDisk false");
        return false;
    }

    /**
     * 获得图片保存目录
     *
     * @param context
     * @return
     */
    private File getDir(Context context, int diskCacheType) {
        if (context == null) {
            DebugLog.w(TAG, "getDir context == null");
            return null;
        }
        File cacheDir;
        //返回外存目录
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            cacheDir = mExternalCacheDirMap.get(diskCacheType);
            if (cacheDir == null) {
                File dir = context.getExternalCacheDir();
                if (dir == null) {
                    dir = context.getCacheDir();
                }
                cacheDir = new File(dir, getDirNameByType(diskCacheType));
                mExternalCacheDirMap.put(diskCacheType, cacheDir);
            }
        }
        //返回手机目录
        else {
            cacheDir = mCacheDirMap.get(diskCacheType);
            if (cacheDir == null) {
                cacheDir = new File(context.getCacheDir(), getDirNameByType(diskCacheType));
                mCacheDirMap.put(diskCacheType, cacheDir);
            }
        }
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return cacheDir;
    }

    /**
     * 通过diskcacheType 文件夹分类获取文件夹名字
     *
     * @param diskCacheType
     * @return
     */
    private String getDirNameByType(int diskCacheType) {
        String result = sDirNameTypePairs.get(diskCacheType);
        if (TextUtils.isEmpty(result)) {
            result = CACHE_DIR_DEFAULT;
        }
        return result;
    }

    /**
     * 通过diskcacheType 文件夹分类获取文件夹最大size
     *
     * @param diskCacheType
     * @return
     */
    private long getDirMaxSizeByType(int diskCacheType) {
        Long result = sDirMaxSizePairs.get(diskCacheType);
        if (result == null || result <= 1000) {
            result = MAX_SIZE;
        }
        return result;
    }

    /**
     * 获得可读文件
     *
     * @param context
     * @param hash
     * @return
     */
    private File getRFile(Context context, String hash, int diskCacheType) {
        File file = getFile(context, hash + READING_FILE_EXTNAME, diskCacheType);

        return file;
    }

    /**
     * 获得写文件
     *
     * @param context
     * @param
     * @return
     */
    private File getWFile(Context context, String hash, int diskCacheType) {
        File file = getFile(context, hash + WRITING_FILE_EXTNAME, diskCacheType);

        return file;
    }

    /**
     * 获得文件
     *
     * @param context
     * @param name
     * @return
     */
    private File getFile(Context context, String name, int diskCacheType) {
        File file = null;
        try {
            file = new File(getDir(context, diskCacheType), name);
        } catch (Exception e) {
            DebugLog.w(TAG, "getFile e:" + e);
        }
        return file;
    }

    /**
     * 将下载地址转换加密
     *
     * @param key
     * @return
     */
    private static String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
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
    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * @author dragon 按照修改时间排序
     */
    static class Sorter implements Comparator<File> {
        public int compare(File r1, File r2) {
            long time1 = r1.lastModified();
            long time2 = r2.lastModified();
            return (int) (time1 - time2);
        }
    }
}
