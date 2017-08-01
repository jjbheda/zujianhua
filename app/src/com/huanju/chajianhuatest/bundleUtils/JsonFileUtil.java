package com.huanju.chajianhuatest.bundleUtils;

import android.content.Context;
import android.util.Log;
import com.huanju.chajianhuatest.bundlemodel.BundleFileModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by jiangjingbo on 2017/7/14.
 */

public class JsonFileUtil {

    private static final String BUNDLE_JSON_FILENAME = "bundle_file.json";
    private static boolean hasReSaveJsonFile = false;      //每次从asset 目录读取json文件重新保存

    private static ArrayList<BundleFileModel> bundleVersionList = new ArrayList<>();
    /**
     * 读取json文件，输出为modelList
     */

    public static ArrayList<BundleFileModel> readJsonFileToList(String fileName) {
        JSONObject root = null;
        InputStream is = null;
        BufferedReader reader = null;
        try {
            StringBuilder sb = new StringBuilder();
            is = new FileInputStream(fileName);
            reader = new BufferedReader(
                    new InputStreamReader(is));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            root = new JSONObject(sb.toString());
            JSONArray data = root.getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                JSONObject taskObject = data.optJSONObject(i);
                BundleFileModel model = new BundleFileModel(taskObject.optString("bundleVersion"),taskObject.optString("md5"));
                bundleVersionList.add(model);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bundleVersionList;
    }

    public static BundleFileModel getBundleModel(Context context, String version) {
        if (!hasReSaveJsonFile){
            saveJsonFile(context, BUNDLE_JSON_FILENAME);
        }
        BundleFileModel bundleFileModel = new BundleFileModel();
        String bVersion = "";
        File apkDir = new File(context.getFilesDir(), "apkDir");
        File jsonFile = new File(apkDir, JsonFileUtil.BUNDLE_JSON_FILENAME);
        if(jsonFile.exists()) {
            if (bundleVersionList.size() == 0 ){
                bundleVersionList = readJsonFileToList(jsonFile.getAbsolutePath());
            }
            for (BundleFileModel model : bundleVersionList) {
                String bundleVersion = model.bundleVersion;
                String pckname = bundleVersion.substring(0,bundleVersion.indexOf(".apk")).replace("_",".");
                if(pckname.equals(version)) {
                    bundleFileModel.bundleVersion = model.bundleVersion;
                    bundleFileModel.md5= model.md5;
                }
            }
        }
        return bundleFileModel;
    }

    public static void saveJsonFile(Context context, String jsonFileName) {
        File apkDir = new File(context.getFilesDir(), "apkDir");
        apkDir.mkdir();
        File apkFile = new File(apkDir,jsonFileName);
        if(apkFile.exists()){
            apkFile.delete();
        }
        InputStream ins = null;
        FileOutputStream fos = null;
        try {
            ins = context.getAssets().open(jsonFileName);
            if (apkFile.length() != ins.available()) {
                fos = new FileOutputStream(apkFile);
                byte[] buf = new byte[2048];
                int l;
                while ((l = ins.read(buf)) != -1) {
                    fos.write(buf, 0, l);
                }
            }
            hasReSaveJsonFile = true;
        } catch (Exception e) {
            Log.e("TAG","解压文件出现异常");
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
    }
}
