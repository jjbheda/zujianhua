package com.huanju.chajianhuatest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import dalvik.system.DexClassLoader;
import qiyi.basemodule.BasePro;

/**
 * @author
 * @date 17/2/21
 */
public class MainActivity extends FragmentActivity {
    private boolean flag = false;
    private boolean flag2 = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView iv = (ImageView) findViewById(R.id.test);
        iv.setImageResource(R.drawable.shuimo);
        BasePro basePro = new BasePro();
        String ss =basePro.getBaseStatusString("110");
        Log.e("TAG",ss);
        Toast.makeText(MainActivity.this,"来自主APP"+ss,Toast.LENGTH_SHORT).show();
        final SharedPreferences soNameFileSP = MainActivity.this.getSharedPreferences("soNameFile", 0);
        final SharedPreferences.Editor soSpEditor = soNameFileSP.edit();

        findViewById(R.id.bbb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String chajiandemoName = soNameFileSP.getString("soName", "");
                if (!chajiandemoName.isEmpty() && chajiandemoName.equals("com_huanju_chajiandemo")) {
                    try {
                        startActivity(new Intent(getApplicationContext(), Class.forName("com.huanju.chajiandemo.TestActivityTwo")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    soSpEditor.putString("soName", "com_huanju_chajiandemo");
                    soSpEditor.commit();
                    setapkClassloader();
                }
            }
        });

        findViewById(R.id.go_demo1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        try {
            String files[] = getAssets().list("");
            for(String fName:files){
                if (fName.endsWith(".so"))
                    apkList.add(fName);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.e("TAG",apkList.toString());
//        apkList.add("com_huanju_chajiandemo.so");
//        apkList.add("qiyi_demo1.apk");
        return apkList;
    }

}
