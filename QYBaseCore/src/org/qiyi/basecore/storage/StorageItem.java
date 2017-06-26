package org.qiyi.basecore.storage;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.support.v4.os.EnvironmentCompat;
import android.text.TextUtils;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * SD卡路径对象
 */
public class StorageItem {
    public static final String TAG = "CHECKSD";
    public String path; // sd卡路径
    public String file_type; // 文件系统类型
    /**
     *  可用容量大小等信息在运行时会发生改变
     *  因此不再对外暴露，请通过{@link #getAvailSize()}和{@link #getTotalSize()}
     *  获取容量等信息
     */
    long usedsize; // 已经使用了的大小byte
    long totalsize; // sd卡总空间 byte
    long availsize; // sd卡可用空间 byte

    public int priority;
    public int type; // 存储卡类型
    // 增加更多存储卡的相关信息
    public boolean mRemovable = true;
    public boolean mPrimary = false;
    /** 请使用{@link #getState(Context)}获取实时挂载状态值 */
    public String mState;

    public String getStorageItemInfo() {

        return "StorageItem{" +
                "path='" + path + '\'' +
                ", totalsize=" + totalsize +
                ", availsize=" + availsize +
                ", file_type='" + file_type + '\'' +
                '}';

    }

    private class StorageSize {
        StorageSize(long usize, long tsize) {
            usedsize = usize;
            totalsize = tsize;
        }

        long usedsize;
        long totalsize;
    }

    public StorageItem(String a, String t, int p) {
        path = a;
        file_type = t;
        priority = p;

        StorageSize st_size = getStorageSize();
        if (st_size != null) {
            DebugLog.v(TAG, "StorageItem->StorageSize is not null");
            usedsize = st_size.usedsize;
            totalsize = st_size.totalsize;
            availsize = totalsize - usedsize;
        } else {
            DebugLog.v(TAG, "StorageItem->StorageSize is null");
            totalsize = 0;
        }
    }

    private StorageSize getStorageSize() {
        File file = new File(path);
        if (!file.exists()) {
            DebugLog.v(TAG, "getStorageSize->file is not exist!");
            return null;
        }
        if (!file.isDirectory()) {
            DebugLog.v(TAG, "getStorageSize->file is not Directory!");
            return null;
        }

        StorageSize st_size = null;
        try {
            StatFs localStatFs = new StatFs(path);
            long blockSize = localStatFs.getBlockSize();
            long blockCount = localStatFs.getBlockCount();
            long availCount = localStatFs.getAvailableBlocks();
            st_size = new StorageSize(blockSize * (blockCount - availCount), blockSize
                    * blockCount);
        } catch (Exception e) {
            DebugLog.d(TAG, "Invalidate path");
        }
        return st_size;
    }


    /**
     * 检查给定的路径下的/Android/data/{package_name}/files目录是否可以写
     *
     * @param mContext
     * @param path
     * @return
     */
    /* package */
    static boolean checkPathCanWrite(Context mContext, String path) {

        if (TextUtils.isEmpty(path)) {
            return false;
        }

        String mInnerPath = path + "Android/data/" + mContext.getPackageName() + "/files";

        DebugLog.v(TAG, "checkPathCanWrite:" + mInnerPath);

        File file = new File(mInnerPath);

        try {
            if (!file.exists()) {
                //这句对于4.4以后的系统非常重要
                mContext.getExternalFilesDir("");

                if (!file.exists()) {
                    DebugLog.v(TAG, "create " + mInnerPath);
                    boolean result = file.mkdirs();
                    if (result) {
                        DebugLog.v(TAG, "create success!");
                    } else {
                        DebugLog.v(TAG, "create fail!");
                    }
                } else {
                    DebugLog.v(TAG, "mInnerPath is exist!");
                }
            }
        }catch (Exception e){
            DebugLog.e(TAG, "checkPathCanWrite()>>>exception=" + e.getMessage());
        }

        return file.canWrite();
    }

    /**
     * 检查当前路径是否可写
     *
     * @param mContext
     * @return
     */
    public boolean canWrite(Context mContext) {
        return checkPathCanWrite(mContext, path);
    }

    /**
     * 测试当前卡是否真的可写，尝试写一个隐藏文件作为测试
     * @param mContext
     * @return
     */
    public boolean canRealWrite(Context mContext){
        String fileDir = path + "Android/data/" + mContext.getPackageName() + "/files";
        DebugLog.v(TAG, "canRealWrite()>>>current test path=" + fileDir);
        File file = new File(fileDir);
        if(!file.exists()){
            mContext.getExternalFilesDir(null);  //调用系统函数创建目录
            file.mkdirs();  //自己尝试创建目录
        }

        if(!file.exists()){
            DebugLog.v(TAG, "canRealWrite()>>>App files dir cannot be created");
            return false;
        }

        if(!file.canWrite()){
            DebugLog.v(TAG, "canRealWrite()>>>App files dir cannot be written");
            return false;
        }

        DebugLog.v(TAG, "canRealWrite()>>>App files dir canWrite return true, we need to write a real file for testing");
        File hideFile = new File(fileDir, ".sd");  //测试文件
        FileOutputStream fos = null;
        try{
            if(!hideFile.exists()){
                hideFile.createNewFile();  //创建文件
            }

             fos = new FileOutputStream(hideFile, true);
            String testContent = System.currentTimeMillis() + ": " + path + "\n";  //测试时间点 + 测试卡路径
            fos.write(testContent.getBytes());
            fos.flush();

            fos.close();
        }catch (IOException e){
            DebugLog.d(TAG, "canRealWrite()>>>App files dir write a file throw IOException, so we assure the sdcard is not writable, this is an exception case");
            return false;
        } finally {
            if(fos != null) {
                try{
                    fos.close();
                }catch (IOException e){
                    ExceptionUtils.printStackTrace(e);
                }

            }
        }

        DebugLog.i(TAG, "canRealWrite()>>>App files dir is really writable, path=" + hideFile.getAbsolutePath());
        return true;
    }

    /**
     * 获取当前卡的挂载状态
     * @return
     */
    public String getState(Context mContext){

        if(!mRemovable && mState != null){
            // 不可移除的卡是内置卡，其挂载状态默认是不变的
            DebugLog.v(TAG, "getState()>>>storage cannot removable, state=" + mState);
            return mState;
        }

        File file = new File(path);
        String newState = null;
        //优先使用反射获取存储卡的状态
        StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        try{
            Method m = sm.getClass().getMethod("getVolumeState", String.class);
            m.setAccessible(true);

            newState = (String)m.invoke(sm, file.getAbsolutePath());
        }catch (Exception e){
            DebugLog.e(TAG, "StorageManage-->getVolumeState reflection failed");
        }

        if(newState != null){
            DebugLog.v(TAG, "StorageManager-->getVolumeState reflection success, path=" + path + ", state=" + newState);
            mState = newState;
            return newState;
        }

        //反射失效，使用系统自带的API尝试获取挂载状态
        try{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                // Android 5.0+
                newState = Environment.getExternalStorageState(file);
            }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                // Android 4.4-5.0
                newState = Environment.getStorageState(file);
            }else {
                // Below 4.4
                File externFile = Environment.getExternalStorageDirectory();
                if(externFile != null && file.getAbsolutePath().equals(externFile.getAbsolutePath())){
                    //与系统默认卡的路径保持一致
                    newState = Environment.getExternalStorageState();
                }else if(file.canRead() && file.getTotalSpace() > 0){
                    //最后只能根据可读写性判定
                    newState = Environment.MEDIA_MOUNTED;
                }
            }
        }catch (Exception e){
            DebugLog.e(TAG, "getState failed with exception " + e.getMessage());
        }catch (NoSuchMethodError e){
            DebugLog.e(TAG, "NoSuchMethodError in Environment.getStorageState");
        }

        if(newState != null){
            DebugLog.v(TAG, "getState()>>>use system Environment api, oldState=" + mState + ", newState=" + newState);
            mState = newState;
            return newState;
        }

        DebugLog.d(TAG, "getState()>>>cannot get correct state, so we assure the storage state is unknown");
        return EnvironmentCompat.MEDIA_UNKNOWN;
    }

    /**
     * 获取当前路径的总大小
     *
     * @return
     */
    public long getTotalSize() {

        File file = new File(path);
        if (!file.exists()) {
            return 0L;
        }

        long totalSize = 0L;
        try {
            StatFs localStatFs = new StatFs(path);
            long blockSize = localStatFs.getBlockSize();
            long blockCount = localStatFs.getBlockCount();
            totalSize = blockSize * blockCount;
        } catch (Exception e) {
            DebugLog.e(TAG, "Invalidate path");
        }
        return totalSize;
    }

    /**
     * 获取当前路径的可用大小
     *
     * @return
     */
    public long getAvailSize() {

        File file = new File(path);
        if (!file.exists()) {
            return 0L;
        }

        long availSize = 0L;
        try {
            StatFs localStatFs = new StatFs(path);
            long blockSize = localStatFs.getBlockSize();
            long availCount = localStatFs.getAvailableBlocks();
            availSize = blockSize * availCount;
        } catch (Exception e) {
            DebugLog.e(TAG, "Invalidate path");
        }
        return availSize;
    }

    /**
     * 创建隐藏文件
     *
     * @param mContext
     */
    public void createHideFile(Context mContext) {
        try {
            String mInnerPath = path + "Android/data/" + mContext.getPackageName() + "/files";
            File dir = new File(mInnerPath);
            if (!dir.exists()) {
                mContext.getExternalFilesDir("");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
            }
            File file = new File(dir, ".a");
            if (!file.exists()) {
                DebugLog.v(TAG, "createHideFile not exist,so create it...");
                file.createNewFile();
            } else {
                DebugLog.v(TAG, "file already exist..");
            }
            DebugLog.v(TAG, "createHideFile Success!");
        } catch (Exception e) {
            DebugLog.v(TAG, "createHideFile failed!");
        }
    }

    /**
     * 检查隐藏文件是否存在
     *
     * @param mContext
     * @return
     */
    public boolean checkHideFileExist(Context mContext) {
        try {
            String mInnerPath = path + "Android/data/" + mContext.getPackageName() + "/files";
            File dir = new File(mInnerPath);
            if (!dir.exists()) {
                mContext.getExternalFilesDir("");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
            }

            File file = new File(dir, ".a");
            return file.exists();

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 方便打印Log
     * @return
     */
    @Override
    public String toString() {
        return "StorageItem{ path=" + path + ", totalSize=" + totalsize + "bytes, availSize=" + availsize + "bytes }";
    }
}
