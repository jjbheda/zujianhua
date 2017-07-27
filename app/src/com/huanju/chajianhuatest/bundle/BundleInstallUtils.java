package com.huanju.chajianhuatest.bundle;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;

import dalvik.system.DexClassLoader;


/**
 * Created by jiangjingbo on 2017/7/19.
 */

    public class BundleInstallUtils {

    private static String TAG = "BundleInstallUtils";
    private static Context mContext;
    private static AssetManager mAssetManager;
    private static Resources mNewResource;
    private static Resources.Theme mTheme;
    private static ArrayList<String> mBundleVersionList = new ArrayList<>();
    public LoadedCallBack mLoadedCallBack;

    public interface LoadedCallBack {
        void sucesss();
        void fail();
    }

    public BundleInstallUtils(Context context) {
        mContext = context;
    }

    private void setLoadedCallBack(LoadedCallBack loadedCallBack){
        mLoadedCallBack = loadedCallBack;
    }

    //尝试更新资源
    public boolean updateTotalResource(String PackageName) {
        boolean flag = false;
        try {
            String bundleApkPath = getBundleApkPath(PackageName);
            if (bundleApkPath.isEmpty()) {
                return flag;
            }
            String mPath = mContext.getPackageResourcePath();
            mAssetManager = AssetManager.class.newInstance();
//            mAssetManager= getAssets();      //用主app自己的assetManager 实现资源混用
            Method addAssetPathMethod = mAssetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
            addAssetPathMethod.setAccessible(true);
            addAssetPathMethod.invoke(mAssetManager, mPath);
            addAssetPathMethod.invoke(mAssetManager, bundleApkPath);
            Method ensureStringBlocks = AssetManager.class.getDeclaredMethod("ensureStringBlocks");
            ensureStringBlocks.setAccessible(true);
            ensureStringBlocks.invoke(mAssetManager);
            Resources supResource = getResources();
            Log.e(TAG, "supResource = " + supResource);

            mNewResource = new Resources(mAssetManager, supResource.getDisplayMetrics(), supResource.getConfiguration());
            Log.e(TAG, "设置 getResource = " + getResources());
            flag = true;
        } catch (Exception e) {
            Log.e(TAG, "callActivityOnCreate被调用，请检查错误，错误信息= " + e.getMessage());
            e.printStackTrace();
        }
        return flag;
    }

    public static AssetManager getAssets() {
        return mAssetManager == null ? mContext.getAssets() : mAssetManager;
    }

    public  static Resources getResources() {
        return mNewResource == null ? mContext.getResources() : mNewResource;
    }

    public static Context getContext() {
        return mContext;
    }

    public String getBundleApkPath(String bundleFileName) throws Exception {
        File apkDir = new File(mContext.getFilesDir(), "apkDir");
        apkDir.mkdir();
        String bundleApkFilePath = "";
        InputStream ins = null;
        FileOutputStream fos = null;
        try {
            File apkFile = new File(apkDir, bundleFileName);
            ins = getAssets().open(bundleFileName);
            if (apkFile.length() != ins.available()) {
                fos = new FileOutputStream(apkFile);
                byte[] buf = new byte[2048];
                int l;
                while ((l = ins.read(buf)) != -1) {
                    fos.write(buf, 0, l);
                }

            }
            bundleApkFilePath = apkFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG,"解压文件出现异常");
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return bundleApkFilePath;
    }

    private boolean setBundleClassloaderSucess(String bundleVersion) {
        boolean flag = false;
        try {
            String cachePath = mContext.getCacheDir().getAbsolutePath();
            String apkPath = getBundleApkPath(bundleVersion);
            DexClassLoader mClassLoader = new DexClassLoader(apkPath, cachePath, cachePath, mContext.getClassLoader());
            ClassPathHookUtil.inject(mContext,mClassLoader);
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    public static boolean hasLoaded(String bundleVersion) {
        return mBundleVersionList.contains(bundleVersion);
    }

    private boolean checkInstallBundle(String bundleVersion){
        boolean flag = false;
        Log.e(TAG, "收到Bundle安装请求");
        if (!updateTotalResource(bundleVersion)) {
            return flag;
        }
        if (!setBundleClassloaderSucess(bundleVersion)) {
            return flag;
        }
        mBundleVersionList.add(bundleVersion);
        flag = true;
        return flag;
    }

    public void installBundle(String bundleVersion, LoadedCallBack loadedCallBack){
        setLoadedCallBack(loadedCallBack);
        if (!checkInstallBundle(bundleVersion)){
            if (mLoadedCallBack != null)
                mLoadedCallBack.fail();
            return;
        }

        if (mLoadedCallBack != null)
            mLoadedCallBack.sucesss();
    }
}
