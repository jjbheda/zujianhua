package org.qiyi.basecore.filedownload;

import android.os.RemoteException;
import android.util.Pair;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author kangle 匹配 FileDownloadCallback 和 FileDownloadStatus 的type进行回调
 */
public class FileDownloadCallbackHelper {
    /**
     * 回调集合，type + callback
     */
    private List<Pair<String, FileDownloadCallback>> mFileDownloadCallbacks = new CopyOnWriteArrayList<Pair<String, FileDownloadCallback>>();

    public void onDownloadListChanged(List<FileDownloadStatus> downloads) {

        //根据type分类
        Map<String, List<FileDownloadStatus>> map = new HashMap<String, List<FileDownloadStatus>>();
        for (Pair<String, FileDownloadCallback> callbackPair : mFileDownloadCallbacks) {

            List<FileDownloadStatus> list = map.get(callbackPair.first);
            for (FileDownloadStatus downloadStatus : downloads) {
                if (callbackPair.first != null && callbackPair.first.equals(downloadStatus.mDownloadConfiguration.getType())) {
                    if (list == null) {
                        list = new ArrayList<FileDownloadStatus>();
                    }
                    list.add(downloadStatus);
                }
            }
            if (list != null) {
                map.put(callbackPair.first, list);
            }
        }

        for (Pair<String, FileDownloadCallback> callbackPair : mFileDownloadCallbacks) {
            //通知所有
            if (callbackPair.first == null) {
                try {
                    callbackPair.second.onDownloadListChanged(downloads);
                } catch (RemoteException e) {
                    if (DebugLog.isDebug()) {
                        ExceptionUtils.printStackTrace(e);
                    }
                }
            }
            //只通知匹配type的
            else {
                List<FileDownloadStatus> list = map.get(callbackPair.first);
                if (list != null) {
                    try {
                        callbackPair.second.onDownloadListChanged(list);
                    } catch (RemoteException e) {
                        if (DebugLog.isDebug()) {
                            ExceptionUtils.printStackTrace(e);
                        }
                    }
                }
            }
        }
    }
    
/*    public void onDownloadListChanged(List<FileDownloadStatus> downloads, String type) {
        for (Pair<String, FileDownloadCallback> callbackPair : mFileDownloadCallbacks) {

            if (type != null && type.equals(callbackPair.first)) {
                try {
                    callbackPair.second.onDownloadListChanged(downloads);
                } catch (RemoteException e) {
                    if (DebugLog.isDebug()) {

                    }
                }
            } else if (type == null) {
                for (FileDownloadStatus downloadStatus : downloads) {
                    if (callbackPair.first == null
                            || callbackPair.first.equals(downloadStatus.mDownloadConfiguration.getType())) {
                        try {
                            callbackPair.second.onDownloadListChanged(downloads);
                        } catch (RemoteException e) {
                            if (DebugLog.isDebug()) {

                            }
                        }
                    }
                }
            }
        }
    }*/

    public void addCallback(Pair<String, FileDownloadCallback> callbackPair) {
        mFileDownloadCallbacks.add(callbackPair);
    }

    public void onDownloadListChanged(Pair<String, FileDownloadCallback> callbackPair, List<FileDownloadStatus> downloads) {

        if(callbackPair.first == null){
            try {
                callbackPair.second.onDownloadListChanged(downloads);
            } catch (RemoteException e) {
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }else {
            List<FileDownloadStatus> matchTypeList = new ArrayList<FileDownloadStatus>();
            for (FileDownloadStatus status : downloads){
                if(callbackPair.first.equals(status.mDownloadConfiguration.getType())){
                    matchTypeList.add(status);
                }
            }
            try {
                callbackPair.second.onDownloadListChanged(matchTypeList);
            } catch (RemoteException e) {
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
    }

    public void remove(FileDownloadCallback cb, String type) {
        Pair<String, FileDownloadCallback> toBeRemove = null;
        for (Pair<String, FileDownloadCallback> callbackPair : mFileDownloadCallbacks) {
            if (type != null && type.equals(callbackPair.first)) {
                toBeRemove = callbackPair;
            }
        }
        mFileDownloadCallbacks.remove(toBeRemove);
    }

    public void onPaused(FileDownloadStatus fileDownloadStatus) {
        for (Pair<String, FileDownloadCallback> callbackPair : mFileDownloadCallbacks) {

            if (callbackPair.first == null
                    || callbackPair.first.equals(fileDownloadStatus.mDownloadConfiguration.getType())) {
                try {
                    callbackPair.second.onPaused(fileDownloadStatus);
                } catch (RemoteException e) {
                    if (DebugLog.isDebug()) {
                        ExceptionUtils.printStackTrace(e);
                    }
                }
            }
        }
    }

    public void onFailed(FileDownloadStatus fileDownloadStatus) {
        for (Pair<String, FileDownloadCallback> callbackPair : mFileDownloadCallbacks) {
            if (callbackPair.first == null
                    || callbackPair.first.equals(fileDownloadStatus.mDownloadConfiguration.getType())) {
                try {
                    callbackPair.second.onFailed(fileDownloadStatus);
                } catch (RemoteException e) {
                    if (DebugLog.isDebug()) {
                        ExceptionUtils.printStackTrace(e);
                    }
                }
            }
        }
    }

    public void onCompleted(FileDownloadStatus fileDownloadStatus) {

        for (Pair<String, FileDownloadCallback> callbackPair : mFileDownloadCallbacks) {

            if (callbackPair.first == null
                    || callbackPair.first.equals(fileDownloadStatus.mDownloadConfiguration.getType())) {
                try {
                    callbackPair.second.onCompleted(fileDownloadStatus);
                } catch (RemoteException e) {
                    if (DebugLog.isDebug()) {
                        ExceptionUtils.printStackTrace(e);
                    }
                }
            }
        }
    }

    public void onDownloadProgress(FileDownloadStatus fileDownloadStatus) {
        for (Pair<String, FileDownloadCallback> callbackPair : mFileDownloadCallbacks) {

            if (callbackPair.first == null
                    || callbackPair.first.equals(fileDownloadStatus.mDownloadConfiguration.getType())) {
                try {
                    callbackPair.second.onDownloadProgress(fileDownloadStatus);
                } catch (RemoteException e) {
                    if (DebugLog.isDebug()) {
                        ExceptionUtils.printStackTrace(e);
                    }
                }
            }
        }
    }

    public void removeAll() {
        mFileDownloadCallbacks.clear();
    }
}
