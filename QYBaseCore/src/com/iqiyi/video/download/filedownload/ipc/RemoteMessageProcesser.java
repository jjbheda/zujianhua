package com.iqiyi.video.download.filedownload.ipc;

import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.iqiyi.video.download.filedownload.FileDownloadController;
import com.iqiyi.video.download.filedownload.FileDownloadExBean;
import com.iqiyi.video.download.filedownload.IDownloadCoreCallback;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;

/**
 * Created by songguobin on 2017/3/17.
 * <p>
 * 下载进程事件处理者
 */

public class RemoteMessageProcesser {

    private static final String TAG = "RemoteMessageProcesser";

    private static RemoteMessageProcesser binderProcesser;

    private FileDownloadController mFileDownloadController;

    private RemoteCallbackList<IDownloadCoreCallback> remoteCallbackList;

    private Object mLock = new Object();

    public synchronized static RemoteMessageProcesser getInstance() {

        if (binderProcesser == null) {
            binderProcesser = new RemoteMessageProcesser();
        }
        return binderProcesser;

    }

    public RemoteMessageProcesser() {

    }

    public void setRemoteCallbackList(RemoteCallbackList<IDownloadCoreCallback> remoteCallbackList) {

        if(remoteCallbackList == null) {
            DebugLog.log(TAG,"setRemoteCallbackList == null");

        } else{

            DebugLog.log(TAG,"setRemoteCallbackList");

        }


        this.remoteCallbackList = remoteCallbackList;

    }

    public void setFileDownloadController(FileDownloadController mFileDownloadController){

        this.mFileDownloadController = mFileDownloadController;

    }

    public FileDownloadExBean getMessage(FileDownloadExBean message) {
        if (message == null) {
            DebugLog.d(TAG, "getMessage->message is null!");
            return null;
        }
        if (remoteCallbackList == null) {
            DebugLog.d(TAG, "getMessage->remoteCallbackList is null!");
            return null;
        }
        FileDownloadExBean result = null;
        synchronized (mLock) {
            try {
                int N = remoteCallbackList.beginBroadcast();
                if (N > 0) {
                    try {
                        result = remoteCallbackList.getBroadcastItem(0).getMessage(message);
                    } catch (RemoteException e) {
                        DebugLog.d(TAG, "BinderPrecesser>>action:" + message.getActionId() + "fail!");
                        ExceptionUtils.printStackTrace(e);
                    }
                }
                remoteCallbackList.finishBroadcast();
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }
        }

        return result;
    }

    public void sendMessage(FileDownloadExBean message) {

        if (message == null) {
            DebugLog.d(TAG, "RemoteMessageProcesser>>sendMessage->message is null!");
            return;
        }

        DebugLog.d(TAG, "RemoteMessageProcesser>>action:" + message.getActionId());

        if (remoteCallbackList == null) {
            DebugLog.d(TAG, "RemoteMessageProcesser>>sendMessage->mDownloadCallbacks is null!");
            return;
        }
        synchronized (mLock) {
            try {
                DebugLog.d(TAG, "RemoteMessageProcesser>>action:" + message.getActionId() + "get lock!");
                int N = remoteCallbackList.beginBroadcast();
                if (N > 0) {
                    try {
                        remoteCallbackList.getBroadcastItem(0).callback(message);
                        DebugLog.d(TAG, "RemoteMessageProcesser>>action:" + message.getActionId() + "success!");
                    } catch (RemoteException e) {
                        DebugLog.d(TAG, "RemoteMessageProcesser>>action:" + message.getActionId() + "fail!");
                        ExceptionUtils.printStackTrace(e);
                    }
                } else {
                    DebugLog.d(TAG, "RemoteMessageProcesser>>callback size ==0");
                }
                remoteCallbackList.finishBroadcast();
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }
        }
    }

    /**
     * 处理远程进程事件
     *
     * @param msg
     * @return
     */
    public FileDownloadExBean processRemoteMessage(FileDownloadExBean msg) {

        try {

           return MessageCenter.processRemoteMessage(msg, mFileDownloadController);

        }catch (Exception e){
            ExceptionUtils.printStackTrace(e);
        }
        return null;
    }



}
