package com.huanju.chajianhuatest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;


/**
 * @author
 * @date 17/2/21
 */
public class MainActivity extends Activity {
    private boolean flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getSystemService(Context.ACTIVITY_SERVICE);
        setContentView(R.layout.activity_main);
        ImageView iv = (ImageView) findViewById(R.id.test);
        iv.setImageResource(R.drawable.shuimo);

        findViewById(R.id.bbb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //这里说一下，我们真正要启动的Activity不是直接用类名.class，因为
                //我们这个应用里根本没有这个类
                if (flag) {
                    try {
                        startActivity(new Intent(getApplicationContext(), Class.forName("com.huanju.chajiandemo.TestActivity")));
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
                try {
                    Intent intent = new Intent(MainActivity.this,Class.forName("qiyi.demo1.MainAc"));
                    startActivity(intent);
                } catch (Exception e){

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

            //创建一个属于我们自己插件的ClassLoader，我们分析过只能使用DexClassLoader
            String cachePath = MainActivity.this.getCacheDir().getAbsolutePath();
//            String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/com_huanju_chajiandemo.apk";


            String apkPath = getapkPath();
            DexClassLoader mClassLoader = new DexClassLoader(apkPath, cachePath, cachePath, getClassLoader());
            MyHookHelper.inject(mClassLoader);

            flag = true;
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


    //原来demo的方法
    public static void patchClassLoader(ClassLoader cl, File apkFile, File optDexFile)
            throws IllegalAccessException, NoSuchMethodException, IOException, InvocationTargetException, InstantiationException, NoSuchFieldException {
        // 获取 BaseDexClassLoader : pathList
        Field pathListField = DexClassLoader.class.getSuperclass().getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object pathListObj = pathListField.get(cl);

        // 获取 PathList: Element[] dexElements
        Field dexElementArray = pathListObj.getClass().getDeclaredField("dexElements");
        dexElementArray.setAccessible(true);
        Object[] dexElements = (Object[]) dexElementArray.get(pathListObj);

        // Element 类型
        Class<?> elementClass = dexElements.getClass().getComponentType();

        // 创建一个数组, 用来替换原始的数组
        Object[] newElements = (Object[]) Array.newInstance(elementClass, dexElements.length + 1);

        // 构造插件Element(File file, boolean isDirectory, File zip, DexFile dexFile) 这个构造函数
        Constructor<?> constructor = elementClass.getConstructor(File.class, boolean.class, File.class, DexFile.class);
        Object o = constructor.newInstance(apkFile, false, apkFile, DexFile.loadDex(apkFile.getCanonicalPath(), optDexFile.getAbsolutePath(), 0));

        Object[] toAddElementArray = new Object[]{o};
        // 把原始的elements复制进去
        System.arraycopy(dexElements, 0, newElements, 0, dexElements.length);
        // 插件的那个element复制进去
        System.arraycopy(toAddElementArray, 0, newElements, dexElements.length, toAddElementArray.length);

        // 替换
        dexElementArray.set(pathListObj, newElements);

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
        return apkList;
    }

}
