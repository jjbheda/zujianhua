package com.huanju.chajianhuatest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexClassLoader;


/**
 * @author
 * @date 17/2/21
 */
public class MainActivity extends Activity {
    private boolean flag = false;
    private boolean flag2 = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView iv = (ImageView) findViewById(R.id.test);
        iv.setImageResource(R.drawable.shuimo);

        findViewById(R.id.bbb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flag) {
                    try {
                        startActivity(new Intent(getApplicationContext(), Class.forName("org.qiyi.qyscanqrcode.scan.ScanActivity")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    setapkClassloader();
                }
            }
        });


        findViewById(R.id.go_demo1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //这里说一下，我们真正要启动的Activity不是直接用类名.class，因为
                //我们这个应用里根本没有这个类
//                try {
//                    Intent intent = new Intent(MainActivity.this,Class.forName("qiyi.demo1.MainAc"));
//                    startActivity(intent);
//                } catch (Exception e){
//
//                }
                if (flag2) {
                    try {
                        startActivity(new Intent(getApplicationContext(), Class.forName("qiyi.demo1.MainAc")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    setapkClassloader2();
                }
            }
        });

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    private boolean setapkClassloader() {
        try {
            String cachePath = MainActivity.this.getCacheDir().getAbsolutePath();
            String apkPath = getapkPath(0);
            DexClassLoader mClassLoader = new DexClassLoader(apkPath, cachePath, cachePath, getClassLoader());
            MyHookHelper.inject(mClassLoader);
            flag = true;
//            loadResource(0);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "加载完成", Toast.LENGTH_SHORT).show();
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //资源加载

    public void loadResource(int index){
        try {
            AssetManager assetManager= getAssets();      //用主app自己的assetManager 实现资源混用
            Method addAssetPathMethod = assetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
            addAssetPathMethod.setAccessible(true);
            String apkPath = getapkPath(index);
            addAssetPathMethod.invoke(assetManager, apkPath);
            Method ensureStringBlocks = AssetManager.class.getDeclaredMethod("ensureStringBlocks");
            ensureStringBlocks.setAccessible(true);
            ensureStringBlocks.invoke(assetManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }



    private boolean setapkClassloader2() {
        try {
            String cachePath = MainActivity.this.getCacheDir().getAbsolutePath();
            String apkPath = getapkPath(1);
            DexClassLoader mClassLoader = new DexClassLoader(apkPath, cachePath, cachePath, getClassLoader());
            MyHookHelper.inject(mClassLoader);
            flag2 = true;
//            loadResource(1);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "加载完成", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**从assetmanager目录中读取apk文件
     */
    public String getapkPath(int dex){
        List<String> bundleFiles = getBundleApkPaths ();
        File apkDir = new File(getFilesDir(),"apkDir");
        apkDir.mkdir();
        String apkFilePath = "";
        try{
            File apkFile = new File(apkDir, bundleFiles.get(dex));
            InputStream ins = getAssets().open(bundleFiles.get(dex));
            if(apkFile.length()!=ins.available()){
                Log.e("TAG","开始读");
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
            e.printStackTrace();
            Log.e("TAG",e.toString());
        }
        Log.e("TAG",apkFilePath);
        return apkFilePath;
    }


    public List<String> getBundleApkPaths(){
        List<String> apkList = new ArrayList<>();
//        try {
//            String files[] = getAssets().list("");
//            for(String fName:files){
//                if (fName.endsWith(".so"))
//                    apkList.add(fName);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        Log.e("TAG",apkList.toString());
        apkList.add("org_qiyi_qyscanqrcode.so");
//        apkList.add("qiyi_demo1.apk");
        return apkList;
    }

}
