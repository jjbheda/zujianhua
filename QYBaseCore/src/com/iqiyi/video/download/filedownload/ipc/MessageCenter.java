package com.iqiyi.video.download.filedownload.ipc;

import android.text.TextUtils;

import com.iqiyi.video.download.filedownload.FileDownloadCallback;
import com.iqiyi.video.download.filedownload.FileDownloadController;
import com.iqiyi.video.download.filedownload.FileDownloadExBean;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.video.module.download.exbean.FileDownloadObject;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by songguobin on 2017/3/17.
 */

public class MessageCenter {

    private static final String TAG = "MessageCenter";


    ////////////////////处理下载进程IPC消息////////////////////////////////////////////////////
    public static FileDownloadExBean processRemoteMessage(FileDownloadExBean msg, FileDownloadController controller) {

        if (msg == null) {
            DebugLog.log(TAG, "msg == null");
            return null;
        }

        DebugLog.log(TAG, "actionId = " + msg.getActionId());

        switch (msg.getActionId()) {

            case FileDownloadAction.ACTION_DOWNLOAD_ADD_TASK:
                //批量添加下载任务
                controller.addDownloadTask(msg.mFileList);
                return null;

            case FileDownloadAction.ACTION_DOWNLOAD_ADD_TASK_WITH_CALLBACK:
                //添加单个下载任务
                controller.addDownloadTask(msg.mFileObject);
                return null;

            case FileDownloadAction.ACTION_DOWNLOAD_OPERATE_TASK:
                //开始或暂停任务
                controller.startOrPauseDownloadTask(msg.mFileObject);
                return null;
            case FileDownloadAction.ACTION_DOWNLOAD_OPERATE_TASK_BY_ID:
                //
                if(msg.mBundle != null) {
                    String fileId = msg.mBundle.getString("url");
                    controller.startOrPauseDownloadTask(fileId);
                } else{
                    DebugLog.log(TAG,"ACTION_DOWNLOAD_OPERATE_TASK_BY_ID>>url == null");
                }
                return null;

            case FileDownloadAction.ACTION_DOWNLOAD_DEL_TASK:
                //删除下载任务
                controller.deleteDownloadTasksWithId(msg.mUrlList);
                return null;
            case FileDownloadAction.ACTION_DOWNLOAD_CANCEL_TASKS_WITH_GROUP_NAME:
                //删除指定groupName的任务队列
                controller.deleteDownloadTaskWithGroupName(msg.sValue1);
                return null;


        }
        return null;
    }


    ////////////////////////////////处理主进程预处理消息/////////////////////////////////////////////////////////////////

    public static void processPreSendMessage(FileDownloadExBean msg) {

           if(msg == null) {
               return;
           }

        switch (msg.getActionId()){

            case FileDownloadAction.ACTION_DOWNLOAD_ADD_TASK_WITH_CALLBACK:
                //注册callback
                registerDownloadFileCallback(msg.mFileObject.getId(),msg.mObject);
                break;
            case FileDownloadAction.ACTION_DOWNLOAD_REMOVE_CALLBACK:
                //注销callback
                unregisterDownloadFileCallback(msg.sValue1,msg.mObject);
                break;

        }

    }

    /**
     * 处理下载callback，没有传callback,则不处理
     *
     * @param key
     * @param value
     */
    private static void registerDownloadFileCallback(String key, Object value) {

        if (!TextUtils.isEmpty(key) && value != null) {
            if (value instanceof FileDownloadCallback) {
                DebugLog.log(TAG,"registerDownloadFileCallback>>key = " + key + "--value = " + value.toString());
                LocalMessageProcesser.getInstance()
                        .registerCallback(key, (FileDownloadCallback) value);
            }
        }
    }

    /**
     * 移除callback
     *
     * @param key   文件下载url
     * @param value FileDownloadCallback
     */
    private static void unregisterDownloadFileCallback(String key, Object value) {

        if (!TextUtils.isEmpty(key) && value != null) {
            if (value instanceof FileDownloadCallback) {
                DebugLog.log(TAG,"unregisterDownloadFileCallback>>key = " + key + "--value = " + value.toString());
                LocalMessageProcesser.getInstance()
                        .unregisterCallback(key, (FileDownloadCallback) value);
            }
        }
    }



    ///////////////////////////从远程进程回调给主进程事件//////////////////////////////////////////////////////////
    public static FileDownloadExBean processLocalMessage(FileDownloadExBean msg) {

        if (msg == null) {
            return null;
        }

        switch (msg.getActionId()) {

            case FileDownloadAction.ACTION_DOWNLOAD_FILE_CALLBACK_DATA_STATUS:

                callbackOnDownloadStatusChanged(msg.mFileObject, msg.iValue1);

                return null;

        }


        return null;

    }

    /**
     * 文件下载状态变化
     *
     * @param changeType
     * @param fileObject
     */
    private static void callbackOnDownloadStatusChanged(FileDownloadObject fileObject, int changeType) {

        DebugLog.log(TAG, fileObject.getFileName() + ">>callbackOnDownloadStatusChanged" + ">>" + changeType);

        CopyOnWriteArrayList<FileDownloadCallback> callbackList =
                LocalMessageProcesser.getInstance().getDownloadFileCallbacks(fileObject.getId());

        if (callbackList == null || callbackList.size() == 0) {
            DebugLog.log(TAG, fileObject.getFileName() + ">>callback == null");
            return;
        }

        for (FileDownloadCallback callback : callbackList) {

            switch (changeType) {
                case FileDownloadCallback.CALLBACK_MSG_ON_START:
                    callback.onStart(fileObject);
                    break;
                case FileDownloadCallback.CALLBACK_MSG_ON_DOWNLOADING:
                    callback.onDownloading(fileObject);
                    break;
                case FileDownloadCallback.CALLBACK_MSG_ON_COMPLETE:
                    callback.onComplete(fileObject);
                    //撤销文件下载监听
                    LocalMessageProcesser.getInstance().unregisterAllCallback(fileObject.getId());
                    break;
                case FileDownloadCallback.CALLBACK_MSG_ON_ERROR:
                    callback.onError(fileObject);
                    DebugLog.log(TAG, fileObject.getFileName() + ">>callbackOnDownloadStatusChanged>>errorCode>>" + fileObject.getErrorCode());
                    //撤销文件下载监听
                    LocalMessageProcesser.getInstance().unregisterAllCallback(fileObject.getId());
                    break;
                case FileDownloadCallback.CALLBACK_MSG_ON_ABORT:
                    //下载中断
                    callback.onAbort(fileObject);
                    break;
            }

        }

    }




}
