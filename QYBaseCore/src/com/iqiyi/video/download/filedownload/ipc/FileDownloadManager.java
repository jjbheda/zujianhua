package com.iqiyi.video.download.filedownload.ipc;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.iqiyi.video.download.filedownload.FileDownloadExBean;
import com.iqiyi.video.download.filedownload.IDownloadCoreAidl;
import com.iqiyi.video.download.filedownload.IDownloadCoreCallback;
import com.iqiyi.video.download.filedownload.QiyiDownloadCoreService;
import com.iqiyi.video.download.filedownload.pool.DownloadThreadPool;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;

/**
 * Created by songguobin on 2017/3/17.
 * <p>
 * 主进程绑定下载核心service
 */

public class FileDownloadManager {

    private static final String TAG = "FileDownloadManager";

    private static FileDownloadManager fileDownloadManager;

    private ServiceConnection mConnection;

    private IDownloadCoreAidl mDownloader;

    private Context mContext;

    public synchronized static FileDownloadManager getInstance() {

        if (fileDownloadManager == null) {
            fileDownloadManager = new FileDownloadManager();
        }

        return fileDownloadManager;

    }

    public FileDownloadManager() {

    }


    /**
     * 本地进程通过此方法向远程进程发送消息
     *
     * @param message
     */
    public void sendMessage(FileDownloadExBean message) {

        //预处理消息
        processPreSendMessage(message);

        //ipc消息
        processSendMessage(message);

    }

    /**
     * 在主进程预处理消息
     * @param message
     */
    private void processPreSendMessage(FileDownloadExBean message){

        try{
            MessageCenter.processPreSendMessage(message);
        }catch (Exception e){
            ExceptionUtils.printStackTrace(e);
        }

    }

    /**
     * 在下载进程处理ipc消息
     * @param message
     */
    private void processSendMessage(FileDownloadExBean message){

        if (mDownloader != null) {
            try {
                mDownloader.sendMessage(message);
            } catch (RemoteException e) {
                ExceptionUtils.printStackTrace(e);
            }
        } else {
            DebugLog.d(TAG, "sendMessage-> mDownloader is null!");
        }

    }

    /**
     * 本地进程通过此方法获取远程进程的某些属性
     *
     * @param message
     * @return
     */
    public FileDownloadExBean getMessage(FileDownloadExBean message) {
        if (mDownloader != null) {
            try {
                return mDownloader.getMessage(message);
            } catch (RemoteException e) {
                ExceptionUtils.printStackTrace(e);
            }
        } else {
            DebugLog.d(TAG, "getMessage-> mDownloader is null!");
        }
        return null;
    }

    public boolean isInited() {

        return mDownloader!=null;

    }


    /**
     * 绑定远程service入口，供外部调用
     *
     * @param context
     * @param callback
     */
    public void bindRemoteDownloadService(final Context context, final FileBinderCallback callback) {

        if (context == null) {
            DebugLog.log(TAG, "context == null");
            if (callback != null) {
                callback.bindFail("context");
            }
            return;
        }

        mContext = context;

//        if (mConnection != null) {
//            DebugLog.log(TAG, "mConnection already inited");
//            if (callback != null) {
//                callback.bindFail("mConnection");
//            }
//            return;
//        }

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

                handleOnServiceConnected(service, callback);

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

                handleOnServiceDisconnected();
            }
        };

        startService(context);

    }


    /**
     * 远程service连接成功
     *
     * @param service
     */
    private void handleOnServiceConnected(IBinder service, FileBinderCallback callback) {

        DebugLog.log(TAG, "handleOnServiceConnected");

        mDownloader = IDownloadCoreAidl.Stub.asInterface(service);

        try {

            notifyProcessDied(service);

            mDownloader.registerCallback(new IDownloadCoreCallback.Stub() {
                @Override
                public void callback(FileDownloadExBean msg) throws RemoteException {

                    LocalMessageProcesser.getInstance().processCallback(msg);

                }

                @Override
                public FileDownloadExBean getMessage(FileDownloadExBean msg) throws RemoteException {

                    return LocalMessageProcesser.getInstance().processCallback(msg);

                }
            });


            if (callback != null) {
                callback.bindSuccess();
            }

        } catch (RemoteException e) {
            ExceptionUtils.printStackTrace(e);
            if (callback != null) {
                callback.bindFail("RemoteException");
            }
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
            if (callback != null) {
                callback.bindFail("Exception");
            }
        }
    }

    /**
     * 远程service连接断开
     */
    private void handleOnServiceDisconnected() {

        DebugLog.log(TAG, "handleOnServiceDisconnected");

        mConnection = null;

        mDownloader = null;

    }


    /**
     * 启动、绑定service
     *
     * @param context
     */
    private void startService(Context context) {

        Intent intent = new Intent();
        intent.setClass(context, QiyiDownloadCoreService.class);
        try {
            context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            context.startService(intent);
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }

    }


    /**
     * 退出app 关闭下载Service
     *
     * @param mActivity 上下文环境
     */
    private void unRegisterRemoteDownloadService(Context mActivity) {
        // 解绑service
        if (mConnection != null) {
            try {
                DebugLog.d(TAG, "unRegisterRemoteDownloadService>>unbindService");
                mActivity.unbindService(mConnection);
                DebugLog.d(TAG, "unRegisterRemoteDownloadService>>mConnection = null");
                mConnection = null;
                mDownloader = null;
                DebugLog.d(TAG, "unRegisterRemoteDownloadService>>unbindService success");
            } catch (IllegalArgumentException e) {
                DebugLog.d(TAG, "unRegisterRemoteDownloadService>>IllegalArgumentException");
                ExceptionUtils.printStackTrace(e);
            } catch (Exception e) {
                DebugLog.d(TAG, "unRegisterRemoteDownloadService>>Exception");
                ExceptionUtils.printStackTrace(e);
            }
        } else {
            DebugLog.d(TAG, "unRegisterRemoteDownloadService is already execute!");
        }
    }


    ///////////////////////////死亡通知机制///////////////////////////////////////////////////////
    /**
     * 死亡通知机制
     *
     * @param cb
     * @return
     */
    public boolean notifyProcessDied(IBinder cb) {
        MyServiceDeathHandler deathHandler = new MyServiceDeathHandler(cb);
        try {
            android.util.Log.e(TAG, "notifyProcessDied = " + rebootServiceTime);
            cb.linkToDeath(deathHandler, 0);
        } catch (RemoteException e) {
            ExceptionUtils.printStackTrace(e);
            return false;
        }
        return true;
    }

    private int rebootServiceTime = 0;

    private int MAX_REBOOT_SERVICE_TIMES = 3;

    private class MyServiceDeathHandler implements IBinder.DeathRecipient {

        private IBinder mCb;

        public MyServiceDeathHandler(IBinder Cb) {

            mCb = Cb;

        }

        @Override
        public void binderDied() {

            try {

                android.util.Log.e(TAG, "MyServiceDeathHandler = " + rebootServiceTime);
                // release resource when host process died.

                DownloadThreadPool.DOWNLOAD_POOL.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(50L);
                            if (rebootServiceTime < MAX_REBOOT_SERVICE_TIMES) {
                                rebootServiceTime++;
                                DebugLog.d(TAG, "rebootServiceTime = " + rebootServiceTime);
                                bindRemoteDownloadService(mContext, null);
                            } else {
                                DebugLog.d(TAG, "stop reboot service");
                            }
                        } catch (InterruptedException e) {
                            ExceptionUtils.printStackTrace(e);
                            Thread.currentThread().interrupt();
                        } catch (OutOfMemoryError e) {
                            ExceptionUtils.printStackTrace(e);
                        }
                    }
                });

            } catch (OutOfMemoryError e) {
                ExceptionUtils.printStackTrace(e);
            }


        }

        public IBinder getBinder() {
            return mCb;
        }
    }


}
