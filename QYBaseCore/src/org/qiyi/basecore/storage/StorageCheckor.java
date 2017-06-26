package org.qiyi.basecore.storage;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Process;
import android.os.StatFs;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.constant.BaseCoreSPConstants;
import org.qiyi.basecore.jobquequ.JobManagerUtils;
import org.qiyi.basecore.utils.SharedPreferencesConstants;
import org.qiyi.basecore.utils.SharedPreferencesFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * SD卡扫描工具类
 * <pre>
 *     此类的主要作用如下：
 *     1、扫描{@link #scanSDCards(Context)}当前手机所有的存储路径，可用的sd存储路径缓存在{@link #sdCardItems}
 *     2、获取最大的存储路径、获取当前存储路径、获取内置卡存储路径、检查指定路径空间是否足够等功能
 *     3、获取内置卡大小、所有外置卡大小
 * </pre>
 *
 * @author yuanzeyao
 */
public class StorageCheckor {
    public static final String TAG = "CHECKSD";

    //防止扫卡逻辑同时多次执行
    private static final Object mScanLock = new Object();
    /**
     * SharedPreference Constant, 新的sp文件在{@link BaseCoreSPConstants#COMMON_SP}
     */
    private static final String OLD_STORAGE_PREF_NAME = "storage";
    private static final String OFFLINE_DOWNLOAD_DIR = "offlineDownloadDir";

    /**
     * 使用{@link StorageItem} 的形式保存当前手机的存储列表
     * 调用{@link #scanSDCards(Context)}扫描后缓存在内存中的手机存储列表
     * 因为底层没有监听SD卡插拔的广播，该数据不保证时刻是最新的
     * 请优先使用{@link #getAvailableStorageItems(Context)}和{@link #getAvailableStoragePaths(Context)}获取最新可用的存储路径；
     */
    public static List<StorageItem> sdCardItems = new ArrayList<StorageItem>();
    /**
     * 标记是否执行过{@link #scanSDCards(Context)}逻辑
     */
    private static volatile boolean isInit = false;
    /**
     * 标记扫描线程是否正在执行中，其他线程是否需要等待
     */
    private static volatile boolean isScanning = false;
    /**
     * 用户默认存储卡根路径, 缓存到内存中
     */
    private static String userDefaultPath = "";

    /**
     * 获取/data目录的总大小
     *
     * @return
     */
    public static long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return totalBlocks * blockSize;
    }

    /**
     * 获取/data目录的可用大小
     *
     * @return
     */
    public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    /**
     * 获取当前可用的SD卡列表，该方法会从所有可能路径中
     * 筛选出真实有效的路径，用于解决底层无法通过监测广播
     * 实现SD卡路径的动态更新
     *
     * @param mContext
     * @return
     */
    public static List<StorageItem> getAvailableStorageItems(Context mContext) {

        ArrayList<StorageItem> totalItems = new ArrayList<>(sdCardItems);
        ArrayList<StorageItem> availableItems = new ArrayList<>();

        for (StorageItem item : totalItems) {

            if (item.canWrite(mContext) && Environment.MEDIA_MOUNTED.equals(item.getState(mContext))) {
                // 应用私有文件目录可写，且是挂载状态
                availableItems.add(item);
                DebugLog.v(TAG, "available sdcard path: " + item.path);
            }
        }

        return availableItems;
    }


    /**
     * 获取当前可用的SD卡列表，只返回String路径
     *
     * @param mContext
     * @return
     */
    public static List<String> getAvailableStoragePaths(Context mContext) {

        ArrayList<StorageItem> totalItems = new ArrayList<>(sdCardItems);
        ArrayList<String> availablePaths = new ArrayList<>();

        for (StorageItem item : totalItems) {

            //DebugLog.w(TAG, "storage path: " + item.path + ", state: " + item.getState(mContext));
            if (item.canWrite(mContext) && Environment.MEDIA_MOUNTED.equals(item.getState(mContext))) {
                // 应用私有文件目录可写，且是挂载状态
                availablePaths.add(item.path);
                DebugLog.v(TAG, "available sdcard path: " + item.path);
            }
        }

        return availablePaths;
    }

    /**
     * 扫描手机的所有sd卡路径
     *
     * @param mContext
     */
    public static void scanSDCards(final Context mContext) {

        synchronized (mScanLock){
            long start = System.currentTimeMillis();   //统计扫卡耗时
            try {
                DebugLog.v(TAG, "sdcard is scanning......");
                isScanning = true;

                sdCardItems = StorageDetect.getStorageList(mContext);
                //置初始化标记成功
                isInit = true;
            } catch (Exception e) {
                DebugLog.e(TAG, "get sdcard path failed");
            } finally {
                isScanning = false;

                long scanCost = System.currentTimeMillis() - start;
                DebugLog.v(TAG, "scanning sdcard is over, cost time: " + scanCost + " milliseconds");
                //打印可用sd卡路径Log
                DebugLog.v(TAG, "sdcard infos: " + sdCardItems.toString());
            }
        }
    }

    /**
     * 确保扫卡线程被正确初始化并执行
     * 如果扫卡线程执行非常耗时，该方法会抛出等待超时异常
     *
     * @param mContext
     */
    private static void ensureSDCardScan(Context mContext) throws TimeoutException {

        if (!isInit) {
            //初始化异步扫卡任务
            startScanSdcardTask(mContext);
        }
//        try {
//            boolean isStart = false;
//            if (!isInit) {
//                // 尝试获取锁，case 1：直接获取成功； case 2：扫描线程正在执行，需要一定时间后才能获取成功
//                if (mScanLock.tryLock(200, TimeUnit.MILLISECONDS)) {
//                    // double check
//                    try {
//                        // 确实没有初始化过，启动异步扫卡线程
//                        if (!isInit) {
//                            DebugLog.v(TAG, "start scan sdcard task async");
//                            startScanSdcardTask(mContext);
//                            isStart = true;
//                        }
//                    } finally {
//                        mScanLock.unlock();  //释放锁
//                    }
//                } else {
//                    //获取锁失败，说明是扫描线程执行耗时了
//                    throw new TimeoutException("tryLock timeout, sdcard scan thread may do time-consuming task");
//                }
//                //启动了异步扫卡线程，待扫卡线程获取锁之后需要等待一定时间
//                if(isStart){
//                    while(!isScanning){
//                        //等待扫描线程获取锁
//                    }
//                    //尝试获取锁, 等待扫描线程结束
//                    if(mScanLock.tryLock(200, TimeUnit.MILLISECONDS)){
//                        //nop, 直接释放锁
//                        mScanLock.unlock();
//                    }else {
//                        //获取锁失败，说明是扫描线程执行耗时了
//                        throw new TimeoutException("tryLock timeout, sdcard scan thread may do time-consuming task");
//                    }
//                }
//            }
//
//            // 执行到此说明已经获取到扫描结果了
//            DebugLog.v(TAG, "ensureSDCardScan>>sdcard scan status: " + isInit);
//        } catch (InterruptedException e){
//            DebugLog.e(TAG, "ensureSDCardScan>>InterruptedException: " + e.getMessage());
//        }

    }

    /**
     * 启动异步扫卡线程，供内部调用
     *
     * @param mContext
     */
    private static void startScanSdcardTask(final Context mContext) {

        JobManagerUtils.postRunnable(new Runnable() {
            @Override
            public void run() {
                //异步扫卡
                Thread.currentThread().setName("sdcard-scan");
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                scanSDCards(mContext);
            }
        });
    }

    /**
     * 检查指定路径是否有剩余空间(剩余空间是否大于0)
     *
     * @param path
     * @return
     */
    public static boolean checkSpaceEnough(String path) {
        return checkSpaceEnough(path, 0);
    }

    /**
     * 检查指定路径剩余空间是否比指定的大小大
     *
     * @param path
     * @param mAvailableSize
     * @return
     */
    public static boolean checkSpaceEnough(String path, long mAvailableSize) {
        List<StorageItem> allItems = new ArrayList<StorageItem>(sdCardItems);
        for (StorageItem item : allItems) {
            if (path.startsWith(item.path)) {
                return item.getAvailSize() > mAvailableSize;
            }
        }
        return false;
    }

    /**
     * 拿到默认的外置sd卡，也就是第一张外置sd卡
     *
     * @return
     */
    public static StorageItem getDefaultExternalSDCardItem() {
        return getExternalSDCardItem(0);
    }

    /**
     * 拿到内置卡
     *
     * @return
     */
    public static StorageItem getInternalSDCardItem() {
        if (sdCardItems.size() > 0) {
            //默认第一张就是内置卡
            return sdCardItems.get(0);
        } else {
            return null;
        }
    }

    /**
     * 如果有多张sd卡，根据传入的index拿到对应的sd卡
     *
     * @param index
     * @return
     */
    private static StorageItem getExternalSDCardItem(int index) {
        if (index < 0) {
            return null;
        }

        ArrayList<StorageItem> totalItems = new ArrayList<>(sdCardItems);
        ArrayList<StorageItem> allExternSDCard = new ArrayList<StorageItem>();

        for (StorageItem item : totalItems) {
            if (item.type == StorageDetect.EXTERNAL_STORAGE_TYPE) {
                allExternSDCard.add(item);
            }
        }

        if (index < allExternSDCard.size()) {
            return allExternSDCard.get(index);
        } else {
            return null;
        }
    }

    /**
     * 获取当前所有可用的SD卡中剩余容量最大的一个
     *
     * @param mContext
     * @return
     */
    public static StorageItem getMaxStorageItem(Context mContext) {

        List<StorageItem> allItems = getAvailableStorageItems(mContext);
        if (allItems.size() == 0) {
            return null;
        }

        long maxSize = 0L;
        StorageItem maxItem = null;

        for (StorageItem item : allItems) {

            long size = item.getAvailSize();
            if (size > maxSize) {
                maxSize = size;
                maxItem = item;
            }
        }

        return maxItem;
    }

    /**
     * 通过路径拿到对应的StorageItem
     *
     * @param path
     * @return
     */
    public static StorageItem getStorageItemByPath(String path) {
        List<StorageItem> allStorages = new ArrayList<StorageItem>(sdCardItems);
        for (StorageItem tmp : allStorages) {
            if (tmp.path.equals(path)) {
                return tmp;
            }
        }
        return null;
    }

    /**
     * 给定一个文件路径，找出这个文件所在的sd卡
     *
     * @param path
     * @return
     */
    public static StorageItem getStorageItemContainPath(String path) {

        if (TextUtils.isEmpty(path)) {
            DebugLog.v(TAG, "getStorageItemContainPath()>>>path is empty");
            return null;
        }

        List<StorageItem> allItems = new ArrayList<>(sdCardItems);
        for (StorageItem item : allItems) {

            String realPath = new File(item.path).getAbsolutePath();
            if (path.startsWith(realPath)) {
                //路径以根路径为前缀
                return item;
            }
        }

        return null;
    }

    /**
     * 返回内置SD卡/Android/data/{package_name}/files下的目录，
     * 内置卡存储空间在某些低端机上容量有限，建议存储小文件；
     * 大文件（如视频、apk等）建议存储到{@link #getUserPreferFilesDir(Context, String)}路径。
     *
     * @param mContext
     * @param subFolder 可以指定子目录，如果为null或""，则不创建子目录；
     *                  建议根据业务合理创建子目录进行区分
     * @return 返回内置SD卡/Android/data/{package_name}/files目录
     * <p>
     * 示例：
     * (1) getInternalStorageFilesDir(context, null) 返回内置sd卡/Android/data/{package_name}/files目录
     * (2) getInternalStorageFilesDir(context, "video") 返回内置sd卡/Android/data/{package_name}/files/video目录
     */
    public static File getInternalStorageFilesDir(Context mContext, String subFolder) {

        try {
            ensureSDCardScan(mContext);
            //
            if (sdCardItems.size() > 0) {
                String root = sdCardItems.get(0).path;
                String filesDir = root + "Android/data/" + mContext.getPackageName() + "/files";

                DebugLog.v(TAG, "getInternalStorageFilesDir>>>internal storage files path: " + filesDir);
                //创建相应目录
                return ensureDirExist(mContext, filesDir, subFolder);
            }
        } catch (TimeoutException e) {
            DebugLog.e(TAG, "getInternalStorageFilesDir>>>wait sdcard scanning timeout, use system api instead!");
        }

        //扫卡超时了或扫不出卡(权限问题或File.canWrite调用返回错误)
        try {
            /**
             * mContext.getExternalFilesDir() sometimes will throw java.lang.NullPointerException
             * at android.os.Parcel.readException(Parcel.java:1471)
             * at android.os.Parcel.readException(Parcel.java:1419)
             * at android.os.storage.IMountService$Stub$Proxy.mkdirs(IMountService.java:750)
             * <href>https://code.google.com/p/android/issues/detail?id=62119</href>
             */
            File file = mContext.getExternalFilesDir(subFolder);
            if (file != null && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                //非空且是挂载状态，透传系统API
                DebugLog.v(TAG, "getInternalStorageFilesDir>>>storage files path with system api: " + file.getAbsolutePath());
                return file;
            }
        } catch (Exception e) {
            DebugLog.e(TAG, "getInternalStorageFilesDir>>>exception=" + e.getMessage());
        }

        DebugLog.w(TAG, "no available sdcards in the system");
        //没有扫描到卡，系统API路径也没有挂载，一般不会执行到这里
        return null;
    }

    /**
     * 返回内置SD卡/Android/data/{package_name}/cache下的目录，
     * 该目录主要用于存储一些应用缓存数据，这些数据在存储空间不足或
     * 用户手动清除缓存时会被清理；
     *
     * @param mContext
     * @param subFolder 可以指定子目录，如果为null或""，则不创建子目录；
     *                  建议根据业务合理创建子目录进行区分
     * @return 返回内置SD卡/Android/data/{package_name}/cache目录
     * <p>
     * 示例：
     * (1) getInternalStorageCacheDir(context, null) 返回内置sd卡/Android/data/{package_name}/cache目录
     * (2) getInternalStorageCacheDir(context, "image") 返回内置sd卡/Android/data/{package_name}/cache/image目录
     */
    public static File getInternalStorageCacheDir(Context mContext, String subFolder) {


        try {
            ensureSDCardScan(mContext);
            //
            if (sdCardItems.size() > 0) {
                String root = sdCardItems.get(0).path;
                String cacheDir = root + "Android/data/" + mContext.getPackageName() + "/cache";

                DebugLog.v(TAG, "getInternalStorageCacheDir>>>internal storage cache path: " + cacheDir);

                return ensureDirExist(mContext, cacheDir, subFolder);
            }
        } catch (TimeoutException e) {
            DebugLog.v(TAG, "getInternalStorageCacheDir>>>wait sdcard scanning timeout, use system api instead!");
        }

        //扫卡超时了或扫不出卡(权限问题或File.canWrite调用返回错误)
        try {
            /**
             * mContext.getExternalFilesDir() sometimes will throw java.lang.NullPointerException
             * or java.lang.ArrayIndexOutOfBoundsException: length=0; index=0
             * at android.app.ContextImpl.getExternalCacheDir(ContextImpl.java:969)
             * at android.content.ContextWrapper.getExternalCacheDir(ContextWrapper.java:235)
             */
            File file = mContext.getExternalCacheDir();
            if (file != null && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                //非空且是挂载状态，透传系统API
                DebugLog.v(TAG, "getInternalStorageCacheDir>>>storage cache path with system api: " + file.getAbsolutePath());
                String cacheDir = file.getAbsolutePath();
                return ensureDirExist(mContext, cacheDir, subFolder);
            }
        } catch (Exception e) {
            DebugLog.e(TAG, "getInternalStorageCacheDir>>>exception=" + e.getMessage());
        }

        DebugLog.w(TAG, "no available sdcards in the system");
        //没有扫描到卡，系统API路径也没有挂载，一般不会执行到这里
        return null;
    }

    /**
     * 返回用户设置的存储卡上/Android/data/{package_name}/files下的目录
     * 若用户尚未设置, 则设置最大容量卡为默认存储并返回；
     * <p>
     * 在单卡手机上该路径是内置卡；在双卡手机上，
     * 该路径可能是内置sd卡，也可能是外置sd卡，取决于用户设置；
     * 当用户设置的路径不可用时，该方法会自动选择下一张可用的sd卡
     *
     * @param mContext
     * @param subFolder 可以指定子目录，如果为null或""，则不创建子目录；
     *                  建议根据业务合理创建子目录进行区分
     * @return 返回用户设置的存储卡上/Android/data/{package_name}/files下的目录
     * <p>
     * 示例：
     * (1) getUserPreferFilesDir(context, null) 返回用户设置的sd卡/Android/data/{package_name}/files目录
     * (2) getUserPreferFilesDir(context, "video") 返回用户设置的sd卡/Android/data/{package_name}/files/video目录
     */
    public static File getUserPreferFilesDir(Context mContext, String subFolder) {

        String root = getCurrentRootPath(mContext);
        boolean isValid = (!TextUtils.isEmpty(root)) && StorageItem.checkPathCanWrite(mContext, root);  // sp中有值且路径可写
        if (isValid) {
            DebugLog.v(TAG, "getUserPreferFilesDir>>>storage path: " + root + " in sp is valid");
            if (!isInit) {
                //没有扫过卡，提前异步初始化一次
                startScanSdcardTask(mContext);
            }
        } else {
            DebugLog.d(TAG, "getUserPreferFilesDir>>>storage path: " + root + " in sp is invalid");
            try {
                //确保sd卡正确初始化
                ensureSDCardScan(mContext);

                if (sdCardItems.size() > 0) {
                    //选择最大卡逻辑
                    StorageItem item = getMaxStorageItem(mContext);
                    root = item != null ? item.path : "";
                    setCurrentRootPath(mContext, root);   // save current settings
                }
            } catch (TimeoutException e) {
                DebugLog.e(TAG, "getUserPreferFilesDir>>>wait sdcard scanning timeout, use system api instead!");
            }
            //
            if (TextUtils.isEmpty(root)) {
                //扫卡失败，透传系统API的调用结果
                File file = Environment.getExternalStorageDirectory();
                if (file != null && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    //
                    root = file.getAbsolutePath() + "/";
                    setCurrentRootPath(mContext, root);   // save current settings
                }
            }
        }

        if (!TextUtils.isEmpty(root)) {
            String filesDir = root + "Android/data/" + mContext.getPackageName() + "/files";

            DebugLog.v(TAG, "getUserPreferFilesDir>>>user prefer files path: " + filesDir);

            return ensureDirExist(mContext, filesDir, subFolder);
        }

        DebugLog.w(TAG, "no available sdcards in the system");
        //确实没有扫描到卡，一般不会执行到这里
        return null;
    }

    /**
     * 返回系统默认的SD卡根路径，如果系统默认路径不存在，则返回内置SD卡的根路径；
     * 存储到该路径下的文件，即使app被卸载了，也不会被清理，还会保留在SD卡上；
     * <p>
     * 应用层根据使用场景决定是否需要{@link Manifest.permission#WRITE_EXTERNAL_STORAGE}的校验；
     * 默认应该使用{@link #getStoragePublicDir(Context, String)}强制开启权限的校验；
     * 针对一些特殊应用场景或应用层可以确认确实不需要申请写权限的场景，可以通过此API设置；
     * For Example：
     * (1) 只需要读取SD卡根路径，不进行文件IO操作，如获取分区容量大小，可读写性等；
     * (2) 读取SD卡根路径或下面的Public目录路径，通过Intent形式传递给系统app（图库、相册等）
     * <p>
     * Note：如果期望对SD卡根路径进行文件IO操作，一定要注意Android 6.0的动态权限检查或申请。
     * <p>
     * Note: 请不要通过此路径拼接得到/Android/data/{package_name}下的路径，应使用
     * {@link #getInternalStorageFilesDir(Context, String)} or
     * {@link #getUserPreferFilesDir(Context, String)}
     *
     * @param mContext
     * @param subFolder 可以指定子目录，如果为null或""，则不创建子目录；
     *                  注意：在没有要求check权限且没有获取写权限时，此方法不会自动创建目录；
     * @return 返回系统默认的SD卡根路径下的目录
     * @throws #NoPermissionException 没有权限异常
     */
    public static File getStoragePublicDir(Context mContext, String subFolder,
                       boolean requestCheckPermission) throws NoPermissionException {

        boolean hasPermission = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;  //是否有权限

        if (requestCheckPermission && !hasPermission) {
            //需要请求权限且没有权限，直接抛异常
            DebugLog.w(TAG, "getStoragePublicDir>>>has no permission to write external storage");
            throw new NoPermissionException(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        //读取根路径，不进行文件IO操作不需要权限
        File rootFile = Environment.getExternalStorageDirectory();
        if (rootFile != null && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //不为null且是挂载状态，正常case
            DebugLog.v(TAG, "getStoragePublicDir>>>valid storage root path with system api: " + rootFile.getAbsolutePath());
        } else {
            DebugLog.v(TAG, "getStoragePublicDir>>>storage path with system api is not available");
            try {
                //确保sd卡正确初始化
                ensureSDCardScan(mContext);
                //
                if (sdCardItems.size() > 0) {
                    //非正常case，返回内置卡路径
                    String rootPath = sdCardItems.get(0).path;
                    rootFile = new File(rootPath.substring(0, rootPath.lastIndexOf("/")));  //去除最后一个"/"

                    DebugLog.v(TAG, "getStoragePublicDir>>>valid storage root path with sdcard api: " + rootPath);
                } else {
                    //异常case
                    DebugLog.d(TAG, "no availbale sdcard in the system");
                    rootFile = null;
                }
            } catch (TimeoutException e) {
                //异常case
                DebugLog.e(TAG, "getStoragePubDir()>>>wait sdcard scanning timeout, return null to the user");
                rootFile = null;
            }
        }

        File targetFile = null;
        if (rootFile != null && !TextUtils.isEmpty(subFolder)) {
            targetFile = new File(rootFile, subFolder);
        } else {
            targetFile = rootFile;
        }

        //有权限的情形下，帮助创建目录；
        if (hasPermission) {
            if (targetFile != null && !targetFile.exists()) {
                targetFile.mkdirs();
                DebugLog.v(TAG, "getStoragePublicDir()>>>has write permission, try to make dirs " + subFolder);
            }
        } else {
            DebugLog.v(TAG, "getStoragePublicDir()>>>no write permission, not help to create subFolder " + subFolder);
        }

        return targetFile;
    }

    /**
     * 默认强制要求{@link Manifest.permission#WRITE_EXTERNAL_STORAGE}的校验；
     * 没有权限，会抛出异常；
     *
     * @param mContext
     * @param subFolder
     * @return
     * @throws NoPermissionException 没有权限异常
     * @see #getStoragePublicDir(Context, String, boolean) 查看具体使用场景
     */
    public static File getStoragePublicDir(Context mContext, String subFolder) throws NoPermissionException {

        return getStoragePublicDir(mContext, subFolder, true);
    }

    /**
     * 确保指定的目录存在，如果不存在就尽量创建；
     *
     * @param mContext
     * @param parent
     * @param child
     * @return
     */
    private static File ensureDirExist(Context mContext, String parent, String child) {

        File file = new File(parent);
        try {
            if (!file.exists()) {
                //这句对于4.4以后的系统非常重要
                mContext.getExternalFilesDir("");

                if (!file.exists()) {
                    DebugLog.v(TAG, "create " + parent);
                    boolean result = file.mkdirs();
                    if (result) {
                        DebugLog.v(TAG, "create success!");
                    } else {
                        //file = null;    //暂时注掉，防止因权限问题引发空指针异常, 应用层一般会try-catch IOException
                        DebugLog.v(TAG, "create fail!");
                    }
                } else {
                    DebugLog.v(TAG, "mInnerPath is exist!");
                }
            }
        } catch (Exception e) {
            DebugLog.e(TAG, "ensureDirExist()>>>exception=" + e.getMessage());
        }

        if (file != null && !TextUtils.isEmpty(child)) {
            file = new File(parent, child);
            if (!file.exists()) {
                boolean result = file.mkdirs();
                if (result) {
                    DebugLog.v(TAG, "create success!");
                } else {
                    //file = null;   //暂时注掉，防止因权限问题引发空指针异常, 应用层一般会try-catch IOException
                    DebugLog.v(TAG, "create failed");
                }
            }
        }

        return file;
    }

    /**
     * 获取当前默认的存储路径
     *
     * @param mContext
     * @return 如果没有设置当前路径，那么返回0
     */
    public static StorageItem getCurrentStorageItem(Context mContext) {
        return getStorageItemByPath(getCurrentRootPath(mContext));
    }

    /**
     * 找到一个可用空间比指定空间大的路径
     *
     * @param mAvailableSize
     * @return
     */
    public static StorageItem findStorageItemByAvailableSize(long mAvailableSize) {
        List<StorageItem> allItems = new ArrayList<StorageItem>(sdCardItems);
        for (StorageItem item : allItems) {
            if (item.getAvailSize() > mAvailableSize) {
                return item;
            }
        }
        return null;
    }

    /**
     * 获取内置sd卡大小
     *
     * @return
     */
    public static long getInnerSDItemSize() {
        StorageItem item = getInternalSDCardItem();
        return item == null ? 0L : item.getTotalSize();
    }

    /**
     * 获取所有外置sd卡的总大小
     *
     * @return
     */
    public static long getAllExternalSDItemSize() {
        List<StorageItem> allItems = new ArrayList<StorageItem>(sdCardItems);
        long totalSize = 0L;
        for (StorageItem mItem : allItems) {
            if (mItem.type == StorageDetect.EXTERNAL_STORAGE_TYPE) {
                totalSize += mItem.getTotalSize();
            }
        }
        return totalSize;
    }

    /**
     * 获取当前选中的存储路径根目录
     * 该方法没有对sd卡路径的有效性进行校验，请慎重调用此方法！！！
     * 在某些系统上写SD卡根目录存在权限问题，如4.4+的外置sd卡
     * 可以通过{@link #getUserPreferFilesDir(Context, String)}获取应用的files目录进行数据存储
     *
     * @param mContext
     * @return
     */
    public static String getCurrentRootPath(Context mContext) {

        if (!TextUtils.isEmpty(userDefaultPath)) {
            // hit the memory cache
            return userDefaultPath;
        }

        String origPath = SharedPreferencesFactory.get(mContext, OFFLINE_DOWNLOAD_DIR, "", OLD_STORAGE_PREF_NAME);
        if (!TextUtils.isEmpty(origPath)) {
            //迁移sp数据到common_sp
            SharedPreferencesFactory.remove(mContext, OFFLINE_DOWNLOAD_DIR, OLD_STORAGE_PREF_NAME);
            SharedPreferencesFactory.set(mContext, OFFLINE_DOWNLOAD_DIR, origPath, BaseCoreSPConstants.COMMON_SP);
            userDefaultPath = origPath;
        } else {
            //从新的sp中读取数据
            userDefaultPath = SharedPreferencesFactory.get(mContext, OFFLINE_DOWNLOAD_DIR, "", BaseCoreSPConstants.COMMON_SP);
        }

        return userDefaultPath;
    }

    /**
     * 设置当前选中的存储路径根目录
     * 使用该方法设置默认存储卡，请一定要使用{@link #getCurrentRootPath(Context)}获取默认存储卡
     * or使用{@link #getUserPreferFilesDir(Context, String)}获取默认存储卡上的/Android/data/{package_name}/files目录
     *
     * @param mContext
     * @param path
     */
    public static void setCurrentRootPath(Context mContext, String path) {

        if (path != null) {
            userDefaultPath = path;  // update the memory cache
            //数据存储到新的sp文件
            SharedPreferencesFactory.set(mContext, OFFLINE_DOWNLOAD_DIR, path, BaseCoreSPConstants.COMMON_SP);
        }
    }

    /**
     * 设置当前离线下载sd卡
     * 如果第一次安装，那么选择较大的卡
     * 如果是升级安装，则保持之前的sd卡选择 for pad
     */
    public static void setOfflineDownloadDirStatusExt(Context mContext) {
        StorageCheckor.scanSDCards(mContext);
        StorageItem maxItem = StorageCheckor.getMaxStorageItem(mContext);
        String offlineDir = SharedPreferencesFactory.get(mContext, SharedPreferencesConstants.OFFLINE_DOWNLOAD_DIR, "");
        if (TextUtils.isEmpty(offlineDir)) {
            DebugLog.log(TAG, "setOfflineDownloadDirStatus-->first time install!");
            //第一次安装
            setMaxPathAsCurrentPath(mContext, maxItem);
        } else {
            //升级安装
            if (offlineDir.equals(OFFLINE_DOWNLOAD_SDCARD)) {
                //从6.8及之前版本升级,并且升级前是选择的外置sd卡
                DebugLog.log(TAG, "setOfflineDownloadDirStatus-->from version6.8 update install and select sdcard!");
                //获取第一张外置sd卡(6.8之前,仅仅记录了是外置卡还是内置卡，所以这里如果是外置卡，默认使用第一张外置卡)
                StorageItem firstSDItem = StorageCheckor.getDefaultExternalSDCardItem();
                if (firstSDItem != null) {
                    SharedPreferencesFactory.set(mContext, SharedPreferencesConstants.OFFLINE_DOWNLOAD_DIR, firstSDItem.path);
                } else {
                    //如果外置sd卡不存在了，那么使用内置卡
                    StorageItem innerItem = StorageCheckor.getInternalSDCardItem();
                    if (innerItem != null) {
                        SharedPreferencesFactory.set(mContext, SharedPreferencesConstants.OFFLINE_DOWNLOAD_DIR, innerItem.path);
                    } else {
                        //内置卡也没有了，那就是没有存储空间
                        SharedPreferencesFactory.set(mContext, SharedPreferencesConstants.OFFLINE_DOWNLOAD_DIR, "");
                    }
                }
            } else if (offlineDir.equals(OFFLINE_DOWNLOAD_LOCAL)) {
                //从6.8及之前版本升级，并且升级前是内置卡
                DebugLog.log(TAG, "setOfflineDownloadDirStatus-->from version6.8 update install and select local!");
                StorageItem innerItem = StorageCheckor.getInternalSDCardItem();
                if (innerItem != null) {
                    SharedPreferencesFactory.set(mContext, SharedPreferencesConstants.OFFLINE_DOWNLOAD_DIR, innerItem.path);
                } else {
                    //内置卡也没有了，那就是没有存储空间
                    SharedPreferencesFactory.set(mContext, SharedPreferencesConstants.OFFLINE_DOWNLOAD_DIR, "");
                }
            } else {
                //从6.8.1及之后版本升级
                DebugLog.log(TAG, "setOfflineDownloadDirStatus-->from version6.8.1 update install and select path:" + offlineDir);
                StorageItem item = StorageCheckor.getStorageItemByPath(offlineDir);
                if (item == null) {
                    DebugLog.log(TAG, "setOfflineDownloadDirStatus-->" + offlineDir
                            + " is not exist!,so we auto select max item");
                    setMaxPathAsCurrentPath(mContext, maxItem);
                }
            }
        }

    }

    /**
     * 将可用容量最大的设置为当前路径 for pad
     *
     * @param mContext
     * @param maxItem
     */
    public static void setMaxPathAsCurrentPath(Context mContext, StorageItem maxItem) {
        if (maxItem != null) {
            DebugLog.log(TAG, "setOfflineDownloadDirStatus-->first time install! and path:" + maxItem.path);
            SharedPreferencesFactory.set(mContext, SharedPreferencesConstants.OFFLINE_DOWNLOAD_DIR, maxItem.path);
            if (maxItem.type != StorageDetect.EXTERNAL_STORAGE_TYPE) {
                DebugLog.log(TAG, "setOfflineDownloadDirStatus-->current path is local!");

            } else {
                DebugLog.log(TAG, "setOfflineDownloadDirStatus-->current path is sdcard!");
            }
        } else {
            //没有扫描到sd卡
            DebugLog.log(TAG, "setOfflineDownloadDirStatus-->first time install! but not find sdcard!");
            SharedPreferencesFactory.set(mContext, SharedPreferencesConstants.OFFLINE_DOWNLOAD_DIR, "");
        }
    }

    public static final String OFFLINE_DOWNLOAD_LOCAL = "local";
    public static final String OFFLINE_DOWNLOAD_SDCARD = "sdcard";


    /**
    /**
     * 获取cacheDir
     * @param mContext
     * @return
     */
    public static String getCacheDir(Context mContext){

        String baseDirectory = "/data/data/com.qiyi.video/cache";

        try {
            //这个地方老是报空指针
            if (mContext != null && mContext.getCacheDir() != null) {
                baseDirectory = mContext.getCacheDir().getAbsolutePath();
            }
        } catch (Exception e) {
            DebugLog.e(TAG, e.getMessage());
        }

        return baseDirectory;
    }
}
