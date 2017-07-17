package com.huanju.chajianhuatest;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.name;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

/**
 * Created by jiangjingbo on 2017/7/14.
 */

public class JsonFileUtiles {

    public static final String bundleJsonFileName = "bundle_file.json";

    /**
     * 读取json文件，输出为modelList
     */

    public static ArrayList<BundleFileModel> readJsonFileToList(String fileName) {
        ArrayList<BundleFileModel> modelList = new ArrayList<BundleFileModel>();
        JSONObject root = null;
        try {
            StringBuilder sb = new StringBuilder();
            InputStream is = new FileInputStream(fileName);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            is.close();
            reader.close();
            root = new JSONObject(sb.toString());
            JSONArray data = root.getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                JSONObject taskObject = data.optJSONObject(i);
                BundleFileModel model = new BundleFileModel(taskObject.optString("bundleVersion"));
                modelList.add(model);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return modelList;
    }

    /**
     * 判断是否已加载
     * @param packageName 包名
     * @param contextFile  Activity.getFilesDir()
     * @return
     */

    public static boolean hasLoaded(File contextFile,String packageName){
        boolean hasLoaded = false;
        File apkDir = new File(contextFile, "apkDir");
        File jsonFile = new File(apkDir,JsonFileUtiles.bundleJsonFileName);
        if(jsonFile.exists()){
            ArrayList<BundleFileModel>  list = JsonFileUtiles.readJsonFileToList(jsonFile.getAbsolutePath());
            for (BundleFileModel model : list){
                String bundleVersion = model.bundleVersion;
                String pckname = bundleVersion.substring(0,bundleVersion.lastIndexOf("_")).replace("_",".");

                if(pckname.equals(packageName)){
                    File bundleApkFile = new File(apkDir,model.bundleVersion);
                    if (bundleApkFile.exists()){
                        hasLoaded = true;
                    }
                }
            }
        }
        return hasLoaded;
    }

    public static String getBundleVersion(File contextFile,String packageName){
        String  bVersion = "";
        File apkDir = new File(contextFile, "apkDir");
        File jsonFile = new File(apkDir,JsonFileUtiles.bundleJsonFileName);
        if(jsonFile.exists()){
            ArrayList<BundleFileModel>  list = JsonFileUtiles.readJsonFileToList(jsonFile.getAbsolutePath());
            for (BundleFileModel model : list){
                String bundleVersion = model.bundleVersion;
                String pckname = bundleVersion.substring(0,bundleVersion.lastIndexOf("_")).replace("_",".");
                if(pckname.equals(packageName)){
                   bVersion = model.bundleVersion;
                }
            }
        }
        return bVersion;
    }
}
