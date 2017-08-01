package com.huanju.chajianhuatest.bundleUtils;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import com.huanju.chajianhuatest.bundlemodel.BundleFileModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.path;

/**
 * Created by jiangjingbo on 2017/7/19.
 */

    public class BundleInstallUtils {

    private static String TAG = "BundleInstallUtils";
    private static Context mContext;
    private static AssetManager mAssetManager;
    private static Resources mNewResource;
    private static Resources.Theme mTheme;
    private static ArrayList<BundleFileModel> mBundleVersionList = new ArrayList<>();
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
    public boolean updateTotalResource(BundleFileModel bundleFileModel) {
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

    public static String getMd5ByFile(File file) throws FileNotFoundException {
        String value = "";
        FileInputStream in = new FileInputStream(file);
        try {
            MappedByteBuffer byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            value = bi.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }

    public String getBundleApkPath(BundleFileModel bundleFileModel) throws Exception {
        String bundleApkFilePath = "";
        File apkDir = new File(mContext.getFilesDir(), "apkDir");
        File apkFile = new File(apkDir, bundleFileModel.bundleVersion);

        if (apkFile.exists()){
            String md5Hex = getMd5ByFile(apkFile);
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
            Log.d(TAG,"解压文件完成，解压路径  ="+bundleApkFilePath);
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
        Log.d(TAG,"完整解压="+bundleApkFilePath);
        return bundleApkFilePath;
    }

    private boolean setBundleClassloaderSucess(BundleFileModel bundleFileModel) {
        boolean flag = false;
        try {
            File fileDir = mContext.getFilesDir();
            String apkPath = getBundleApkPath(bundleFileModel);

            List<File> files = new ArrayList<>();
            files.add(new File(apkPath));
            ClassLoaderInjectHelper.InjectResult injectResult = ClassLoaderInjectHelper.inject(mContext, apkPath);
            if (null != injectResult && injectResult.mIsSuccessful) {
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
        return mBundleVersionList.contains(fileModel);
    }

    private boolean checkInstallBundle(BundleFileModel bundleFileModel){
        Log.d(TAG, "收到Bundle安装请求");
//        String md5 = getMd5(bundleFileModel);
//        if (!md5.isEmpty() && md5.equals(bundleFileModel.md5)) {        //说明已解压，且版本为最新
//
//        }


        boolean flag = false;
        if (!updateTotalResource(bundleFileModel)) {
            return flag;
        }
        if (!setBundleClassloaderSucess(bundleFileModel)) {
            return flag;
        }
        mBundleVersionList.add(bundleFileModel);
//        saveMd5File(bundleFileModel);
        flag = true;
        return flag;
    }

//    public static void saveMd5File(BundleFileModel bundleFileModel) {
//        String path = new File(mContext.getFilesDir(), "apkDir/"+bundleFileModel.bundleVersion).getAbsolutePath()+"/";
//        String str = bundleFileModel.md5;
//        byte bt[] = new byte[1024];
//        bt = str.getBytes();
//        try {
//            FileOutputStream in = new FileOutputStream(path+bundleFileModel.bundleVersion+".txt");
//            try {
//                in.write(bt, 0, bt.length);
//                in.close();
//                // boolean success=true;
//                // System.out.println("写入文件成功");
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        } catch (FileNotFoundException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }

//    public String getMd5(BundleFileModel bundleFileModel){
//        String path = new File(mContext.getFilesDir(), "apkDir/"+bundleFileModel.bundleVersion).getAbsolutePath()+"/";
//        StringBuilder md5 = new StringBuilder();
//        File file = new File(path+bundleFileModel.bundleVersion+".txt");
//        BufferedReader reader = null;
//        try {
//            reader = new BufferedReader(new FileReader(file));
//            String tempString = null;
//            int line = 1;
//            // 一次读入一行，直到读入null为文件结束
//            while ((tempString = reader.readLine()) != null) {
//                // 显示行号
//                System.out.println("line " + line + ": " + tempString);
//                md5.append(tempString);
//                line++;
//
//            }
//            reader.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (IOException e1) {
//                }
//            }
//        }
//        return md5.toString();
//    }


    public void installBundle(BundleFileModel bundleFileModel, LoadedCallBack loadedCallBack){
        setLoadedCallBack(loadedCallBack);
        if (!checkInstallBundle(bundleFileModel)){
            if (mLoadedCallBack != null)
                mLoadedCallBack.fail();
            return;
        }

        if (mLoadedCallBack != null)
            mLoadedCallBack.sucesss();
    }
}
