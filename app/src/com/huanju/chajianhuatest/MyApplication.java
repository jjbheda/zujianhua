package com.huanju.chajianhuatest;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;

/**
 * @author
 * @date 17/2/21
 */
public class MyApplication extends Application {
    private static Context sContext;

    public static DexClassLoader mClassLoader;
    private AssetManager assetManager;
    private Resources newResource;
    private Resources.Theme mTheme;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
//        MyHookHelper.hookActivityResource(base);
        sContext = base;
        //读取build.gradle 内容，取出

        try {
            String apkPath = getapkPath();
            String mPath = getPackageResourcePath();

//            assetManager = AssetManager.class.newInstance();
            assetManager= getAssets();      //用主app自己的assetManager 实现资源混用
            Method addAssetPathMethod = assetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
            addAssetPathMethod.setAccessible(true);

            addAssetPathMethod.invoke(assetManager, mPath);
            addAssetPathMethod.invoke(assetManager, apkPath);


            Method ensureStringBlocks = AssetManager.class.getDeclaredMethod("ensureStringBlocks");
            ensureStringBlocks.setAccessible(true);
            ensureStringBlocks.invoke(assetManager);
//
            Resources supResource = getResources();
            Log.e("Main", "supResource = " + supResource);
            newResource = new Resources(assetManager, supResource.getDisplayMetrics(), supResource.getConfiguration());
            Log.e("Main", "设置 getResource = " + getResources());

            mTheme = newResource.newTheme();
            mTheme.setTo(super.getTheme());

//            hookreSource(base);


        } catch (Exception e) {
            Log.e("Main", "走了我的callActivityOnCreate 错了 = " + e.getMessage());
            e.printStackTrace();
        }

    }

    @Override
    public AssetManager getAssets() {
        return assetManager == null ? super.getAssets() : assetManager;
    }

    @Override
    public Resources getResources() {
        return newResource == null ? super.getResources() : newResource;
    }

    @Override
    public Resources.Theme getTheme() {
        return mTheme == null ? super.getTheme() : mTheme;
    }

    public static Context getContext() {
        return sContext;
    }




    /**从assetmanager目录中读取apk文件
    */

    public String getapkPath(){
        List<String> bundleFiles = getBundleApkPaths ();
        File apkDir = new File(getFilesDir(),"apkDir");
        apkDir.mkdir();
        String apkFilePath = "";
        try{
            File apkFile = new File(apkDir, bundleFiles.get(0));
            InputStream ins = getAssets().open(bundleFiles.get(0));
            if(apkFile.length()!=ins.available()){
                FileOutputStream fos = new FileOutputStream(apkFile);
                byte[] buf = new byte[2048];
                int l;
                while((l=ins.read(buf))!=-1){
                    fos.write(buf,0,l);
                }
                fos.close();
            }
            ins.close();
            apkFilePath = apkFile.getAbsolutePath();
        }catch (Exception e) {

        }
        return apkFilePath;
    }

    public List<String> getBundleApkPaths(){
        List<String> apkList = new ArrayList<>();
        try {
            String files[] = getAssets().list("");
           for(String fName:files){
                if (fName.endsWith(".so"))
                    apkList.add(fName);
           }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return apkList;
    }


}
