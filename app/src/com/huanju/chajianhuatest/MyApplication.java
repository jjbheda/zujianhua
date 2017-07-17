package com.huanju.chajianhuatest;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

/**
 * @author
 * @date 17/2/21
 */
public class MyApplication extends Application {
    private static Context mContext;
    private AssetManager mAssetManager;
    private Resources mNewResource;
    private Resources.Theme mTheme;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        mContext = base;
        EventBus.getDefault().register(this);//订阅
        saveJsonFile(JsonFileUtiles.bundleJsonFileName);

    }

    //尝试更新资源
    public void updateTotalResource(String PackageName) {
        try {
            String bundleApkPath = getBundleApkPath(PackageName);
            if (bundleApkPath.isEmpty()){
                return;
            }
            String mPath = getPackageResourcePath();
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
            Log.e("Main", "supResource = " + supResource);
            mNewResource = new Resources(mAssetManager, supResource.getDisplayMetrics(), supResource.getConfiguration());
            Log.e("Main", "设置 getResource = " + getResources());
            mTheme = mNewResource.newTheme();
            mTheme.setTo(super.getTheme());

        } catch (Exception e) {
            Log.e("Main", "callActivityOnCreate被调用，请检查错误，错误信息= " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public AssetManager getAssets() {
        return mAssetManager == null ? super.getAssets() : mAssetManager;
    }

    @Override
    public Resources getResources() {
        return mNewResource == null ? super.getResources() : mNewResource;
    }

    @Override
    public Resources.Theme getTheme() {
        return mTheme == null ? super.getTheme() : mTheme;
    }

    public static Context getContext() {
        return mContext;
    }

    public String getBundleApkPath(String bundleFileName) {
        File apkDir = new File(getFilesDir(), "apkDir");
        apkDir.mkdir();
        File apkFile = new File(apkDir, bundleFileName);
        if (new File(apkDir, bundleFileName).exists()){
            Log.e("TAG",apkFile.getName()+"已解压,"+"路径："+apkFile.getAbsolutePath());
            return apkFile.getAbsolutePath();
        }

        String bundleApkFilePath = "";
        try {
            InputStream ins = getAssets().open(bundleFileName);
            if (apkFile.length() != ins.available()) {
                FileOutputStream fos = new FileOutputStream(apkFile);
                byte[] buf = new byte[2048];
                int l;
                while ((l = ins.read(buf)) != -1) {
                    fos.write(buf, 0, l);
                }
                fos.close();
            }
            ins.close();
            bundleApkFilePath = apkFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e("TAG","解压文件出现异常");
        }
        return bundleApkFilePath;
    }

    private void setBundleClassloader(String bundleVersion) {
        try {
            String cachePath = getCacheDir().getAbsolutePath();
            String apkPath = getBundleApkPath(bundleVersion);
            DexClassLoader mClassLoader = new DexClassLoader(apkPath, cachePath, cachePath, getClassLoader());
            ClassPathHookHelper.inject(mClassLoader);
            EventBus.getDefault().post(new BundleCallbackModel( bundleVersion.substring(0,bundleVersion.lastIndexOf("_")).replace("_",".")));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String saveJsonFile(String jsonFileName) {
        File apkDir = new File(getFilesDir(), "apkDir");
        apkDir.mkdir();
        File apkFile = new File(apkDir, jsonFileName);
        if(apkFile.exists()){
            apkFile.delete();
        }

        String jsonFilePath = "";
        try {
            InputStream ins = getAssets().open(jsonFileName);
            if (apkFile.length() != ins.available()) {
                FileOutputStream fos = new FileOutputStream(apkFile);
                byte[] buf = new byte[2048];
                int l;
                while ((l = ins.read(buf)) != -1) {
                    fos.write(buf, 0, l);
                }
                fos.close();
            }
            ins.close();
            jsonFilePath = apkFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e("TAG","解压文件出现异常");
        }
        return jsonFilePath;
    }

    @Subscribe(threadMode = ThreadMode.MAIN) //在ui线程执行
    public void onDataSynEvent(BundleFileModel event) {
        Log.e("TAG", "收到Bundle安装请求");
        String realBundleName = event.bundleVersion;
        updateTotalResource(realBundleName);
        setBundleClassloader(realBundleName);
    }

}
