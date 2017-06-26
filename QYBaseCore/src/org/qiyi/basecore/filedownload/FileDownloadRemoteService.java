package org.qiyi.basecore.filedownload;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ApplicationContext;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.InteractTool;
import org.qiyi.basecore.utils.NetworkChangeReceiver;

import java.util.List;

public class FileDownloadRemoteService extends Service {
    private static final String TAG=FileDownloadRemoteService.class.getSimpleName();

    private FileDownloadRemoteServiceInterface.Stub mBinder;
    
    /**
     * 需要在主线程初始化（Looper is prepared）
     */

    private NetworkChangeReceiver networkChangeReceiver;
    
    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationContext.app = getApplication();
//        DebugLog.setDebug(true);
        
        DebugLog.d("FileDownloadRemoteService", "onCreate: ");
        
//        NotificationManager nManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
//        nManager.cancelAll();

        networkChangeReceiver=NetworkChangeReceiver.getNetworkChangeReceiver(getApplicationContext());
        FileDownloadManager fileDownloadManager = FileDownloadManager.getInstance();
        FileDownloadManager.FileDownloadNetChange netChangeCallback=fileDownloadManager.getFileDownloadNetChange();
        networkChangeReceiver.registReceiver( TAG, netChangeCallback);

    }

    @Override
    public IBinder onBind(Intent intent) {
        DebugLog.d("FileDownloadRemoteService", "onBind: ");
        if(mBinder == null){
            mBinder = initBinder();
        }
        
        return mBinder;
    }

    
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DebugLog.d("FileDownloadRemoteService", "onStartCommand: " + intent + " " +flags + " " + startId);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DebugLog.d("FileDownloadRemoteService", "onDestroy: ");


        //mDownloadReceiver.unRegist();
        networkChangeReceiver.unRegistReceiver(TAG);
        FileDownloadManager.getInstance().onDestroy();
    }

    private FileDownloadRemoteServiceInterface.Stub initBinder() {
        return new FileDownloadRemoteServiceInterface.Stub() {
            @Override
            public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                //下载非必现远程空指针异常，无法捕获调用栈，采用该方法暂时控制该异，并上报异常捕获准确异常信息
                try {
                    return super.onTransact(code, data, reply, flags);
                } catch (NullPointerException e) {
                    InteractTool.randomReportException(Log.getStackTraceString(e), 100);
                    return false;
                }
            }

            @Override
            public void unregisterCallback(FileDownloadCallback cb, String type) throws RemoteException {
                FileDownloadManager.getInstance().unRegist(cb, type);
            }
            
            @Override
            public void resumeDownload(FileDownloadStatus fileDownloadStatus) throws RemoteException {
                FileDownloadManager.getInstance().resumeDownload(fileDownloadStatus);
            }
            
            @Override
            public void registerCallback(FileDownloadCallback cb, String type) throws RemoteException {
                FileDownloadManager.getInstance().registerCallback(FileDownloadRemoteService.this, cb, type);
            }
            
            @Override
            public void pauseDownload(FileDownloadStatus fileDownloadStatus) throws RemoteException {
                FileDownloadManager.getInstance().pauseDownload(fileDownloadStatus);
            }
            
            @Override
            public List<FileDownloadStatus> getDownloads() throws RemoteException {
                return FileDownloadManager.getInstance().getDownloads();
            }
            
            @Override
            public void deleteDownloads(List<FileDownloadStatus> fileDownloadStatusList) throws RemoteException {
                FileDownloadManager.getInstance().deleteDownloads(fileDownloadStatusList);
            }
            
            @Override
            public void addDownload(List<FileDownloadStatus> fileDownloadStatusList) throws RemoteException {
                //fix crash first, look into it later
                try {
                    FileDownloadManager.getInstance().addDownload(FileDownloadRemoteService.this, fileDownloadStatusList, null);
                } catch (Exception e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        };
    }
}
