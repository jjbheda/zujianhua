package com.iqiyi.video.download.filedownload;

import android.content.Context;
import android.text.TextUtils;

import com.iqiyi.video.download.filedownload.extern.FileDownloadAgent;
import com.iqiyi.video.download.filedownload.ipc.FileDownloadAction;
import com.iqiyi.video.download.filedownload.verify.VerifyFactory;
import com.iqiyi.video.download.filedownload.verify.ZipVerification;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.storage.StorageCheckor;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.video.module.download.exbean.FileDownloadObject;
import org.qiyi.video.module.download.exbean.XTaskBean;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Random;

/**
 * Created by songguobin on 2017/1/17.
 * <p>
 * 提供文件重命名，解压，校验等功能
 */

public class FileDownloadHelper {

    private static final String TAG = "CdnDownloadFileTask-helper";

    public static final int DOWNLOAD_VERIFY_RETRY = 2;//校验异常

    public static final int DOWNLOAD_VERIFY_SUCCESS = 1;//校验通过

    public static final int DOWNLOAD_VERIFY_NO = 0;//无需校验


    /**
     * 校验文件,默认不需要校验，直接返回校验成功
     * 校验下载中的文件，后缀名未更改
     * 校验下载完成的文件，后缀名已更改
     *
     * @param fileObject
     * @return
     */
    public static int verifyFile(FileDownloadObject fileObject, File downloadingFile, File completeFile) {

        if (fileObject.mDownloadConfig != null && fileObject.mDownloadConfig.needVerify) {

            String verifyPath = "";

            if (downloadingFile != null && downloadingFile.exists()) {

                verifyPath = downloadingFile.getAbsolutePath();

            } else {

                if (completeFile != null && completeFile.exists()) {
                    verifyPath = completeFile.getAbsolutePath();
                }

            }

            boolean isVerifySuccess = VerifyFactory.verify(fileObject.mDownloadConfig.verifyWay,
                    verifyPath, fileObject.mDownloadConfig.verifySign);

            DebugLog.log(TAG, fileObject.getFileName() + ">>verify path = " + verifyPath);
            DebugLog.log(TAG, fileObject.getFileName() + ">>verify way = " + fileObject.mDownloadConfig.verifyWay);
            DebugLog.log(TAG, fileObject.getFileName() + ">>verify sign = " + fileObject.mDownloadConfig.verifySign);
            DebugLog.log(TAG, fileObject.getFileName() + ">>isVerifySuccess = " + isVerifySuccess);

            if (!isVerifySuccess) {
                return DOWNLOAD_VERIFY_RETRY;
            } else {
                return DOWNLOAD_VERIFY_SUCCESS;
            }
        } else {

            DebugLog.log(TAG, fileObject.getFileName() + ">>no verify config");

            return DOWNLOAD_VERIFY_NO;
        }

    }

    /**
     * 解压文件
     * @param bean
     * @param completeFile
     * @return
     */
    public static boolean unzipFile(FileDownloadObject bean,File completeFile) {

        boolean unzipResult = true;

        if (bean != null && bean.getDownloadConfig().supportUnzip) {

            unzipResult =  ZipVerification.unzipToSelfPath(completeFile.getAbsolutePath());

            DebugLog.log(TAG,bean.getFileName() + ">>解压文件结果 = " + unzipResult);
        }

        return unzipResult;

    }

    /**
     * 重命名文件
     *
     * @param downloadingFile 未下载完文件，带.cdf后缀
     * @param completeFile    下载完成文件
     * @return
     */
    public static boolean renameToCompleteFile(File downloadingFile, File completeFile) {

        boolean isRenameSuccess = false;

        if (downloadingFile != null && downloadingFile.exists()) {
            isRenameSuccess = downloadingFile.renameTo(completeFile);
            DebugLog.log(TAG, "正在重命名>>" + completeFile.getAbsolutePath());
        } else if (completeFile != null && completeFile.exists()) {
            isRenameSuccess = true;
            DebugLog.log(TAG, "无需重命名，文件已下载完成>>" + completeFile.getAbsolutePath());
        } else {
            isRenameSuccess = false;
            if (completeFile != null) {
                DebugLog.log(TAG, "重命名异常>>" + completeFile.getAbsolutePath());
            }
        }

        return isRenameSuccess;
    }


    /**
     * 清理文件
     *
     * @param destFile
     */
    public static void clearDownloadFile(File destFile) {

        try {
            if (destFile != null && destFile.exists()) {
                deleteFile(destFile.getAbsolutePath());
            }
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
    }


    /**
     * 创建目录
     *
     * @param fileDirPath
     * @return
     */
    public static boolean createDir(String fileDirPath){

        boolean isDirCreateSuccess = false;

        try {

            File parentFile = new File(fileDirPath);

            isDirCreateSuccess = parentFile.mkdirs();

        }catch (Exception e){

            ExceptionUtils.printStackTrace(e);

        }

        return isDirCreateSuccess;

    }

    /**
     * 创建文件
     *
     * @param filePath
     * @return
     */
    public static boolean createFile(String filePath){

        boolean isFileCreateSuccess = false;

        try {

            File file = new File(filePath);

            isFileCreateSuccess = file.createNewFile();

            DebugLog.log(TAG, "isFileCreateSuccess = " + isFileCreateSuccess);

        } catch (IOException e) {

            ExceptionUtils.printStackTrace(e);

        }

        return isFileCreateSuccess;

    }


    public static <B extends XTaskBean> boolean isSDFull(B bean) {
        if (bean == null || TextUtils.isEmpty(bean.getSaveDir())) {
            return false;
        }

        long availableSize = 0L;
        String video_path = bean.getSaveDir();
        if (!TextUtils.isEmpty(video_path)) {
            return !StorageCheckor.checkSpaceEnough(video_path, FileDownloadConstant.STORAGE_15M);
        } else {
            return true;
        }
    }
    public static final int LOOPER_RETRY = 6;

    /**
     * 有限重试次数
     * 6此重试时间[135,165]
     *
     * @param mRandom
     * @param retryTimes
     * @param totalTimes
     * @return
     */
    public static int getSleepTimeForfiniteRetry(Random mRandom, int retryTimes, int totalTimes) {
        if (retryTimes >= totalTimes) {
            return -1;
        }
        retryTimes = retryTimes % LOOPER_RETRY;
        DebugLog.log(TAG, "retryTimes = " + retryTimes);
        // 拿到一个[5s-10s]的随机数
        int random = mRandom.nextInt(6) + 5;
        return ((retryTimes + 1) * 5 + random) * 1000;
    }

    /**
     * 计算百分比的数值。如果2/8，则返回25
     *
     * @param completeSize
     * @param totalSize
     */
    public static int calculatePercent(long completeSize, long totalSize) {
        if (totalSize <= 0) {
            return 0;
        }

        double percent = (double) completeSize / (double) totalSize;
        return (int) (100 * percent);
    }


    /**
     * inputStream转换成byte数组
     *
     * @param inputStream
     * @return
     */
    public static byte[] inputStrem2Byte(InputStream inputStream) {


        if (inputStream == null) {

            return null;

        }

        ByteArrayOutputStream swapStream = null;

        byte[] resultByte = null;

        try {

            swapStream = new ByteArrayOutputStream();

            byte[] buff = new byte[1024];

            int rc = 0;

            while ((rc = inputStream.read(buff, 0, buff.length)) != -1) {

                swapStream.write(buff, 0, rc);

            }

            resultByte = swapStream.toByteArray();

        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        } finally {

            if (swapStream != null) {

                try {
                    swapStream.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }

            }

            if(inputStream != null){
                try{
                    inputStream.close();
                }catch (IOException e){
                    ExceptionUtils.printStackTrace(e);
                }
            }

        }

        return resultByte;
    }


    /**
     * byte转换成文件
     * @param inputBytes
     * @param savePath
     * @return
     */
    public static int byte2File(byte[] inputBytes,String savePath) {

        if (inputBytes == null || inputBytes.length == 0) {
            return -1;
        }
        String responseStr = null;
        try {
            responseStr = new String(inputBytes, FileDownloadConstant.DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            ExceptionUtils.printStackTrace(e);
        }

        if (responseStr == null) {
            return -1;
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(savePath);
            fos.write(inputBytes, 0, inputBytes.length);
            fos.flush();
            return 1;
        } catch (FileNotFoundException e) {
            ExceptionUtils.printStackTrace(e);
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
        return -1;
    }

    /**
     * 避免直接使用秒为睡眠单位，而是将睡眠时间分片
     *
     * @param isOn 控制循环是否继续
     * @param time 线程睡眠多长时间
     */
    public static void sleep(boolean isOn, long time) {
        int retry = 0;
        long times = time / 1000;
        while (isOn && retry < times) {
            try {
                Thread.sleep(1000);
                retry++;
            } catch (InterruptedException e) {
                DebugLog.log(TAG, "InterruptedException->" + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 网络请求重试间隔时间策略：重试次数*5s+随机数，随机数取值范围为[5s，10s]；
     *
     * @param retryTimes
     */
    public static int getSleepTime(Random mRandom, int retryTimes) {

        //拿到一个[5s-10s]的随机数
        int random = mRandom.nextInt(6) + 5;
        return (retryTimes * 5 + random) * 1000;
    }

    public static FileDownloadExBean buildCallbackMsg(FileDownloadObject fileDownloadObject, int changeType){

        FileDownloadExBean msg = new FileDownloadExBean(FileDownloadAction.ACTION_DOWNLOAD_FILE_CALLBACK_DATA_STATUS);
        msg.iValue1 = changeType;
        msg.mFileObject = fileDownloadObject;
        return msg;

    }

    public static boolean deleteFile(String filepath) {

        if(TextUtils.isEmpty(filepath)){
            //路径不存在，则表示不用删除
            return true;
        }

        File originalFile = new File(filepath);

        if(!originalFile.exists()){
            //如果文件不存在，则表示不用删除
            return true;
        }

        File targetFile = new File(filepath + ".del");
        boolean renameStatus = originalFile.renameTo(targetFile);

        if(!renameStatus){
            DebugLog.log(TAG,filepath + ">>重命名失败");
            targetFile = originalFile;
        }

        boolean deleteStatus = targetFile.delete();

        if(!deleteStatus) {
            DebugLog.log(TAG, filepath + ">>删除失败");
        }

        return deleteStatus;
    }



    /**
     * @param context
     * @return
     */
    public static File getDownloadPath(Context context) {

        File ret = StorageCheckor.getUserPreferFilesDir(context, "Download");

        return ret == null ? context.getFilesDir() : ret;
    }

    /**
     * 获取下载路径
     * @param context
     * @param filename
     * @return
     */
    public static String getDownloadPath(Context context,String filename){

        File dirFile = getDownloadPath(context);

        return dirFile + "/" + filename;


    }


    /**
     * 修复业务方不传filepath的问题
     * @param fileDownloadObject
     * @return
     */
    public static void fixFileDownloadPath(Context mContext,FileDownloadObject fileDownloadObject){

        if(fileDownloadObject!=null && TextUtils.isEmpty(fileDownloadObject.getDownloadPath())){
            if(!TextUtils.isEmpty(fileDownloadObject.getFileName())){
                //文件名存在的情况
                fileDownloadObject.setDownloadPath(getDownloadPath(mContext,fileDownloadObject.getFileName()));
            } else{
                //文件名不存在的情况
                int lastIndex = fileDownloadObject.getId().lastIndexOf("/");
                if(lastIndex != -1) {
                    //可以从url中获取后缀名，通过“/”
                    String suffix = fileDownloadObject.getId().substring(lastIndex + 1);
                    fileDownloadObject.setDownloadPath(getDownloadPath(mContext,suffix ));
                } else{
                    //随机产生名字
                    Random random = new Random();
                    int seed = random.nextInt(10000);
                    String randomName = "unknown_" + seed + "_" + System.currentTimeMillis();
                    fileDownloadObject.setDownloadPath(getDownloadPath(mContext,randomName));
                }
            }
            DebugLog.log(TAG,"add file task>>filepath = " + fileDownloadObject.getDownloadPath());
        }
    }

    /**
     * 批量修复业务方不传filepath的问题
     * @param mContext
     * @param fileDownloadList
     */
    public static void fixFileDownloadPathForBatch(Context mContext, List<FileDownloadObject> fileDownloadList){

        for(FileDownloadObject fileDownloadObject : fileDownloadList) {

            fixFileDownloadPath(mContext,fileDownloadObject);

        }

    }


    /**
     * 投递文件下载错误码
     *
     * @param fileObject
     */
    public static void deliverFileDownloadErrorCode(FileDownloadObject fileObject){

        if(FileDownloadAgent.getFileDownloadStatistic()!=null){

            FileDownloadAgent.getFileDownloadStatistic().sendStatistic(fileObject);

        } else{

            DebugLog.log(TAG,"FileDownloadStatistic interface == null,can not deliver error code");

        }

    }


}
