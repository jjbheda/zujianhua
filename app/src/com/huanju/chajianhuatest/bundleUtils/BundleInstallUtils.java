package com.huanju.chajianhuatest.bundleUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;


import com.huanju.chajianhuatest.bundlemodel.BundleFileModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by jiangjingbo on 2017/7/19.
 */

    public class BundleInstallUtils {

    private static String TAG = "BundleInstallUtils";
    private static String SP_BUNDLE = "bundle_md5";
    private static Context mContext;
    private static AssetManager mAssetManager;
    private static Resources mNewResource;
    private static Resources.Theme mTheme;
    private static ArrayList<BundleFileModel> mBundleVersionList = new ArrayList<>();
    public LoadedCallBack mLoadedCallBack;

    public interface LoadedCallBack {
        void success();
        void fail();
    }
    public BundleInstallUtils(Context context) {
        mContext = context;
    }

    private void setLoadedCallBack(LoadedCallBack loadedCallBack){
        mLoadedCallBack = loadedCallBack;
    }

    //尝试更新资源
    private static boolean updateTotalResource(BundleFileModel bundleFileModel) {
        boolean flag = false;
        try {
            String bundleApkPath = getBundleApkPath(bundleFileModel);
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

            mNewResource = new Resources(mAssetManager, supResource.getDisplayMetrics(), supResource.getConfiguration());
            flag = true;
        } catch (Exception e) {
            Log.e(TAG, "callActivityOnCreate is call ,the exception message = " + e.getMessage());
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

    private static String getMd5ByFile(BundleFileModel bundleFileModel) throws FileNotFoundException {
        SharedPreferences preferences = mContext.getSharedPreferences(SP_BUNDLE, Context.MODE_PRIVATE);
        String md5 = preferences.getString(bundleFileModel.bundleVersion+"_md5","");
        Log.d(TAG,"get md5 value  ="+md5);
        return md5;
    }

    private static String getBundleApkPath(BundleFileModel bundleFileModel) throws Exception {
        String bundleApkFilePath = "";
        File apkDir = new File(mContext.getFilesDir(), "apkDir");
        File apkFile = new File(apkDir, bundleFileModel.bundleVersion);
        if (apkFile.exists()){
            String md5Hex = getMd5ByFile(bundleFileModel);
            Log.d(TAG,"md5Hex  ="+md5Hex);
            if (!md5Hex.isEmpty() && md5Hex .equals(bundleFileModel.md5)){
                bundleApkFilePath = apkFile.getAbsolutePath();
                Log.d(TAG,"bundleApkFilePath  ="+bundleApkFilePath);
                return bundleApkFilePath;
            }
        }
        apkDir.mkdir();
        InputStream ins = null;
        FileOutputStream fos = null;
        try {
            ins = getAssets().open(bundleFileModel.bundleVersion);
            if (apkFile.length() != ins.available()) {
                fos = new FileOutputStream(apkFile);
                byte[] buf = new byte[2048];
                int l;
                while ((l = ins.read(buf)) != -1) {
                    fos.write(buf, 0, l);
                }
            }
            Log.d(TAG,"unzip file end,the fileFath  ="+bundleApkFilePath);
            bundleApkFilePath = apkFile.getAbsolutePath();
            Log.d(TAG,"saved md5 value  ="+bundleFileModel.md5);
            SharedPreferences settings = mContext.getSharedPreferences(SP_BUNDLE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(bundleFileModel.bundleVersion+"_md5",bundleFileModel.md5 );
            editor.commit();

        } catch (Exception e) {
            Log.e(TAG,"Exception catch when unzip file");
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
        Log.d(TAG,"unzip file suceess, the filePath ="+bundleApkFilePath);
        return bundleApkFilePath;
    }

    private static boolean setBundleClassloaderSucess(BundleFileModel bundleFileModel) {
        boolean flag = false;
        try {
            String apkPath = getBundleApkPath(bundleFileModel);
            ClassLoaderInjectHelper.InjectResult injectResult = ClassLoaderInjectHelper.inject(mContext, apkPath);
            if (null != injectResult && injectResult.mIsSuccessful) {
                Log.d(TAG, "setBundleClassloaderSucess success");
                flag = true;
            } else {
                flag = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    public static boolean hasLoaded(BundleFileModel fileModel) {
        Log.d(TAG, "check has install，bundle name :"+fileModel.bundleVersion+",bundlelist="+mBundleVersionList.toString());
        if(mBundleVersionList.contains(fileModel) && updateTotalResource(fileModel)){
            return true;
        }
        return false;
    }

    private boolean checkInstallBundle(BundleFileModel bundleFileModel){
        Log.d(TAG, "receive bundle install request!");
        boolean flag = false;
        if (!updateTotalResource(bundleFileModel)) {
            return flag;
        }
        if (!setBundleClassloaderSucess(bundleFileModel)) {
            return flag;
        }
        mBundleVersionList.add(bundleFileModel);
        flag = true;
        return flag;
    }

    public void installBundle(BundleFileModel bundleFileModel, LoadedCallBack loadedCallBack){
        setLoadedCallBack(loadedCallBack);
        if (!checkInstallBundle(bundleFileModel)){
            if (mLoadedCallBack != null)
                mLoadedCallBack.fail();
            return;
        }

        if (mLoadedCallBack != null)
            mLoadedCallBack.success();
    }
}
