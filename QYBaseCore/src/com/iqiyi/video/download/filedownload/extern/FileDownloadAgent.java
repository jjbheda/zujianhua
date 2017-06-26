package com.iqiyi.video.download.filedownload.extern;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.iqiyi.video.download.filedownload.FileDownloadCallback;
import com.iqiyi.video.download.filedownload.FileDownloadExBean;
import com.iqiyi.video.download.filedownload.FileDownloadHelper;
import com.iqiyi.video.download.filedownload.ipc.FileBinderCallback;
import com.iqiyi.video.download.filedownload.ipc.FileDownloadAction;
import com.iqiyi.video.download.filedownload.ipc.FileDownloadManager;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.video.module.download.exbean.FileDownloadObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songguobin on 2017/4/6.
 */

public class FileDownloadAgent {

    private static final String TAG = "";

    ///////////////////////添加文件下载任务///////////////////////////////////////////////////
    /**
     * 批量添加文件下载任务
     *
     * @param mContext
     * @param beanList
     */
    public static void addFileDownloadTask(final Context mContext, final List<FileDownloadObject> beanList) {

        checkBindServiceResult(mContext, new FileDownloadServiceBinderCallback() {
            @Override
            public void bindSuccess() {

                //修复filepath缺失的逻辑
               FileDownloadHelper.fixFileDownloadPathForBatch(mContext,beanList);

                addFileDownloadTask(beanList);

            }

            @Override
            public void bindFail() {

                if(beanList != null && beanList.size() >0 ){
                    DebugLog.e(TAG,beanList.size() + " tasks add fail");
                }
            }
        });
    }

    /**
     * 添加单个下载任务,带callback
     * @param mContext
     * @param bean
     * @param callback
     */
    public static void addFileDownloadTask(final Context mContext, final FileDownloadObject bean,
                                           final FileDownloadCallback callback) {

        checkBindServiceResult(mContext, new FileDownloadServiceBinderCallback() {
            @Override
            public void bindSuccess() {

                //修复filepath缺失的逻辑
                FileDownloadHelper.fixFileDownloadPath(mContext,bean);

                addFileDownloadTask(bean, callback);

            }

            @Override
            public void bindFail() {
                DebugLog.e(TAG, "one tasks add fail");
            }
        });
    }

    /**
     * 添加单个下载任务
     * @param mContext
     * @param bean
     */
    public static void addFileDownloadTask(final Context mContext, final FileDownloadObject bean) {

        checkBindServiceResult(mContext, new FileDownloadServiceBinderCallback() {
            @Override
            public void bindSuccess() {

                //修复filepath缺失的逻辑
                FileDownloadHelper.fixFileDownloadPath(mContext,bean);

                List<FileDownloadObject> beanList = new ArrayList<FileDownloadObject>();
                beanList.add(bean);
                addFileDownloadTask(beanList);

            }

            @Override
            public void bindFail() {

            }
        });
    }

    /**
     * 添加文件下载任务
     *
     * @param fileList
     */
    private static void addFileDownloadTask(List<FileDownloadObject> fileList) {

        FileDownloadExBean bean = new FileDownloadExBean(FileDownloadAction.ACTION_DOWNLOAD_ADD_TASK);
        bean.mFileList = fileList;
        FileDownloadManager.getInstance().sendMessage(bean);

    }

    /**
     * 添加文件下载任务，带callback回调
     *
     * @param fileDownloadObject
     * @param callback
     */
    private static void addFileDownloadTask(FileDownloadObject fileDownloadObject, FileDownloadCallback callback) {

        FileDownloadExBean bean = new FileDownloadExBean(FileDownloadAction.ACTION_DOWNLOAD_ADD_TASK_WITH_CALLBACK);
        bean.mFileObject = fileDownloadObject;
        bean.mObject = callback;
        FileDownloadManager.getInstance().sendMessage(bean);

    }

    /**
     * 暂停或开始文件任务
     *
     * @param fileDownloadObject
     */
    public static void startOrPauseFileDownloadTask(FileDownloadObject fileDownloadObject) {

        FileDownloadExBean bean = new FileDownloadExBean(FileDownloadAction.ACTION_DOWNLOAD_OPERATE_TASK);
        bean.mFileObject = fileDownloadObject;
        FileDownloadManager.getInstance().sendMessage(bean);

    }

    /**
     * 暂停或开始文件任务
     * @param url
     */
    public static void startOrPauseFileDownloadTask(String url) {

        FileDownloadExBean bean = new FileDownloadExBean(FileDownloadAction.ACTION_DOWNLOAD_OPERATE_TASK_BY_ID);
        Bundle bundle = new Bundle();
        bundle.putString("url",url);
        bean.mBundle = bundle;
        FileDownloadManager.getInstance().sendMessage(bean);

    }



    /**
     * 通过指定的fileDownloadObject,删除单个任务
     * @param fileDownloadObject
     */
    public static void deleteFileDownloadTask(FileDownloadObject fileDownloadObject){

        if(fileDownloadObject == null) {
            DebugLog.e(TAG,"deleteFileDownloadTask>>bean  == null");
            return;
        }

        FileDownloadExBean bean = new FileDownloadExBean(FileDownloadAction.ACTION_DOWNLOAD_DEL_TASK);

        ArrayList<String> urlList = new ArrayList<String>();
        urlList.add(fileDownloadObject.getId());

        bean.mUrlList = urlList;
        FileDownloadManager.getInstance().sendMessage(bean);

    }

    /**
     * 通过指定的FileDownloadObject列表，删除多个任务
     * @param beanList
     */
    public static void deleteFileDownloadTask(List<FileDownloadObject> beanList){

        if(beanList == null || beanList.size() == 0){

            DebugLog.e(TAG,"deleteFileDownloadTask>>bean list == null");
            return;

        }

        FileDownloadExBean bean = new FileDownloadExBean(FileDownloadAction.ACTION_DOWNLOAD_DEL_TASK);

        ArrayList<String> urlList = new ArrayList<String>();
        for(FileDownloadObject fileDownloadObject:beanList){
            urlList.add(fileDownloadObject.getId());
        }

        bean.mUrlList = urlList;
        FileDownloadManager.getInstance().sendMessage(bean);

    }

    /**
     * 通过指定的url列表，删除多个任务
     * @param urlList
     */
    public static void deleteFileDownloadTaskWithUrl(List<String> urlList){

        if(urlList == null || urlList.size() == 0){
            DebugLog.e(TAG,"deleteFileDownloadTaskWithUrl>>url list == null");
            return;
        }

        FileDownloadExBean bean = new FileDownloadExBean(FileDownloadAction.ACTION_DOWNLOAD_DEL_TASK);
        bean.mUrlList = urlList;
        FileDownloadManager.getInstance().sendMessage(bean);

    }

    /**
     * 通过指定的url，删除一个任务
     * @param url
     */
    public static void deleteFileDownloadTaskWithUrl(String url){

        if(TextUtils.isEmpty(url)){
            DebugLog.e(TAG,"deleteFileDownloadTaskWithUrl>>url == null");
            return;
        }

        FileDownloadExBean bean = new FileDownloadExBean(FileDownloadAction.ACTION_DOWNLOAD_DEL_TASK);

        ArrayList<String> urlList = new ArrayList<String>();
        urlList.add(url);
        bean.mUrlList = urlList;

        FileDownloadManager.getInstance().sendMessage(bean);

    }

    /**
     * 检测是否绑定了service
     *
     * @param mContext
     * @param binderCallback
     */
    private static void checkBindServiceResult( Context mContext,final FileDownloadServiceBinderCallback binderCallback) {

        boolean isServiceBinded = FileDownloadManager.getInstance().isInited();

        if (!isServiceBinded) {

            DebugLog.log(TAG, "file service未绑定>>绑定service");

            FileDownloadManager.getInstance().bindRemoteDownloadService(mContext, new FileBinderCallback() {
                @Override
                public void bindSuccess() {
                    DebugLog.log(TAG, "bindSuccess");

                    if (binderCallback != null) {
                        binderCallback.bindSuccess();
                    }
                }

                @Override
                public void bindFail(String errorCode) {
                    DebugLog.log(TAG, "bindFail");
                    if (binderCallback != null) {
                        binderCallback.bindFail();
                    }
                }
            });

        } else {
            DebugLog.log(TAG, "file service已绑定>>直接添加下载任务");

            if (binderCallback != null) {
                binderCallback.bindSuccess();
            }
        }

    }


    private interface FileDownloadServiceBinderCallback {

        void bindSuccess();

        void bindFail();

    }

    /**
     * 删除文件下载callback
     * @param key
     * @param callback
     */
    public static void unregisterFileDownloadCallback(String key,FileDownloadCallback callback){

        if(TextUtils.isEmpty(key) || callback == null){
            DebugLog.e(TAG,"deleteFileDownloadCallback>>key == null || callback == null");
            return;
        }

        FileDownloadExBean bean = new FileDownloadExBean(FileDownloadAction.ACTION_DOWNLOAD_REMOVE_CALLBACK);
        bean.sValue1 = key;
        bean.mObject = callback;
        FileDownloadManager.getInstance().sendMessage(bean);


    }

    /**
     * 取消指定groupName任务
     *
     * @param groupName
     */
    public static void cancelFileDownloadTask(String groupName) {

        if(!TextUtils.isEmpty(groupName)) {

            FileDownloadExBean bean = new FileDownloadExBean(FileDownloadAction.ACTION_DOWNLOAD_CANCEL_TASKS_WITH_GROUP_NAME);
            bean.sValue1 = groupName;
            FileDownloadManager.getInstance().sendMessage(bean);

        } else {
            DebugLog.log(TAG,"groupName == null");
        }

    }


    private static IFileDownloadStatistic iFileDownloadStatistic;

    public synchronized static void setFileDownloadStatistics(IFileDownloadStatistic statistics){

        iFileDownloadStatistic = statistics;

    }

    public synchronized static IFileDownloadStatistic getFileDownloadStatistic(){

        return iFileDownloadStatistic;

    }




}
