package org.qiyi.basecore.filedownload;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.filedownload.FileDownloadStatus.DownloadConfiguration;
import org.qiyi.basecore.utils.ExceptionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 定义下载的调起方与下载行为的全部交互
 * 任何和下载服务相关的交互君均应由此句柄发出
 *
 * @author kangle
 */
public class FileDownloadInterface {

    private static RemoteServiceConnection mServiceConnection;

    protected FileDownloadCallback fileDownloadCallback;

    /**
     * 收集用来监听下载服务ready的callback(避免重复create)
     */
    private static List<FileDownloadCallback> fileDownloadCallbacks;

    /**
     * generate default type string
     */
    private static int typeInt = 1987;

    /**
     * current type
     */
    private String typeStr;
    /**
     * 下载服务是否初始化完成
     */
    private static boolean isInited = false;

    private Executor executorService;

    public FileDownloadInterface(FileDownloadCallback fileDownloadCallback) {
        this(fileDownloadCallback, null);
    }

    /**
     * WARN： 这里的type必须和 FileDownloadInterface(FileDownloadCallback fileDownloadCallback, String type)
     * 构造方法中的type保持一致，这是为了保证FileDownloadInterface只监听此类型的下载（默认监听自己add进去的，指定type之后则会以type为主）
     *
     * @param fileDownloadCallback
     * @param type
     */
    public FileDownloadInterface(FileDownloadCallback fileDownloadCallback, String type) {

        if (fileDownloadCallback != null) {
            this.fileDownloadCallback = fileDownloadCallback;
            try {
                typeStr = type == null ? String.valueOf(typeInt++) : type;
                mServiceConnection.downloadRemoteServiceInterface.registerCallback(fileDownloadCallback, typeStr);
            } catch (RemoteException e) {
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
    }
    
/*    public FileDownloadInterface() {
    }*/

    /**
     * 注销回调
     */
    public void unRegist() {
        if (fileDownloadCallback != null) {
            try {
                mServiceConnection.downloadRemoteServiceInterface
                        .unregisterCallback(fileDownloadCallback, typeStr);
            } catch (RemoteException e) {
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
    }

    /**
     * 注册使用下载服务，由于第一次启动可能耗时，
     * 需要在{@link FileDownloadCallback#onDownloadListChanged(java.util.List)}回调之后，再使用下载服务
     */
    public synchronized static void initFileDownloadService(Context context,
                                                            FileDownloadCallback fileDownloadCallback) {

        DebugLog.d(FileDownloadInterface.class.getSimpleName(), "initFileDownloadService: ");

        if (fileDownloadCallback == null) {
            fileDownloadCallback = new FileDownloadCallbackImp().getInvokeThreadCallback();
        }

        if (mServiceConnection == null || mServiceConnection.downloadRemoteServiceInterface == null) {
            createDownloadService(context, fileDownloadCallback);
        } else {
            try {
                mServiceConnection.downloadRemoteServiceInterface
                        .registerCallback(fileDownloadCallback, null);
            } catch (RemoteException e) {
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }
//                createDownloadService(context, fileDownloadCallback);
            }
        }
    }

    private static class DelayWorker implements Runnable {
        private Context mCT;

        public DelayWorker(Context context) {
            mCT = context;
        }

        @Override
        public void run() {
            try {
                //delay 3 minutes
                Thread.sleep(3 * 60 * 1000L);
            } catch (InterruptedException e) {
                ExceptionUtils.printStackTrace(e);
                Thread.currentThread().interrupt();
            }
            doCreateDownloadService(mCT);
        }
    }

    /**
     * 注册使用下载服务，由于第一次启动可能耗时，
     * 需要在{@link FileDownloadCallback#onDownloadListChanged(java.util.List)}回调之后，再使用下载服务
     */
    public static void initFileDownloadService(Context context) {
        initFileDownloadService(context, null);
    }

    /**
     * 添加下载，由下载端控制是否开始下载
     *
     * @param downloadConfigurations 封装所有下载的信息
     */
    public void addDownloads(List<DownloadConfiguration> downloadConfigurations) {

/*        DebugLog.d("FileDownload",
                "addDownloads in " + FileDownloadInterface.class.getSimpleName() + ",url: " + downloadConfigurations.get(0).downloadUrl);*/

        final List<FileDownloadStatus> downloadStatusList = new ArrayList<FileDownloadStatus>();
        for (Iterator<DownloadConfiguration> iterator = downloadConfigurations.iterator(); iterator
                .hasNext(); ) {
            DownloadConfiguration downloadConfiguration = iterator.next();
            downloadConfiguration.setType(typeStr);
            FileDownloadStatus fileDownloadStatus = new FileDownloadStatus(downloadConfiguration);
            downloadStatusList.add(fileDownloadStatus);
        }

        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Thread.currentThread().setName("FileDownloadInterface.addDownload");
                try {
                    long currentTimeMillis = System.currentTimeMillis();
                    mServiceConnection.downloadRemoteServiceInterface.addDownload(downloadStatusList);
                    DebugLog.v(FileDownloadInterface.class.getSimpleName(), "addDownload takes: " + (System.currentTimeMillis() - currentTimeMillis) + downloadStatusList);
                } catch (RemoteException e) {
                    if (DebugLog.isDebug()) {
                        ExceptionUtils.printStackTrace(e);
                    }
                }
                return null;
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (executorService == null) {
                executorService = Executors.newSingleThreadExecutor();
            }
            asyncTask.executeOnExecutor(executorService);
        } else {
            asyncTask.execute();
        }
    }

    /**
     * 添加单个下载
     *
     * @param downloadConfiguration
     */
    public void addDownload(DownloadConfiguration downloadConfiguration) {
        List<DownloadConfiguration> downloadConfigurations =
                new ArrayList<FileDownloadStatus.DownloadConfiguration>();
        downloadConfigurations.add(downloadConfiguration);
        addDownloads(downloadConfigurations);
    }

    /**
     * 删除下载
     *
     * @param downloadStatusList
     */
    public void deleteDownloads(List<FileDownloadStatus> downloadStatusList) {
        try {
            mServiceConnection.downloadRemoteServiceInterface.deleteDownloads(downloadStatusList);
        } catch (Exception e) {
            if (DebugLog.isDebug()) {
                ExceptionUtils.printStackTrace(e);
            }
        }
    }

    /**
     * 用户操作下载，非下载状态的变为下载，下载状态的变为暂停
     *
     * @param downloadStatus
     */
    public void onUserOperateDownload(FileDownloadStatus downloadStatus) {
        if (downloadStatus.status == FileDownloadConstant.STATUS_RUNNING) {
            // 暂停
            try {
                mServiceConnection.downloadRemoteServiceInterface.pauseDownload(downloadStatus);
            } catch (RemoteException e) {
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        } else {
            // 开始下载
            try {
                mServiceConnection.downloadRemoteServiceInterface.resumeDownload(downloadStatus);
            } catch (RemoteException e) {
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
    }

    /**
     * TODO KANGLE1，提供条件查询
     *
     * @return 符合条件的下载任务状态集合
     */
    public List<FileDownloadStatus> getDownloads() {
        List<FileDownloadStatus> fileDownloadStatusList = null;
        try {
            fileDownloadStatusList =
                    mServiceConnection.downloadRemoteServiceInterface.getDownloads();
        } catch (RemoteException e) {
            if (DebugLog.isDebug()) {
                ExceptionUtils.printStackTrace(e);
            }
        }
        return fileDownloadStatusList;
    }

    /**
     * 启动下载服务，在第一次regist的时候做一次
     *
     * @param fileDownloadCallback
     */
    private static void createDownloadService(Context context, FileDownloadCallback fileDownloadCallback) {
        synchronized (FileDownloadInterface.class) {
            if (fileDownloadCallbacks == null) {
                fileDownloadCallbacks = Collections.synchronizedList(new ArrayList<FileDownloadCallback>());
                if (AutoDownloadConfigPolicy.canAutoDownloadPlugin(context)) {
                    doCreateDownloadService(context);
                } else {
                    new Thread(new DelayWorker(context), "FileDownloadInterface.createService").start();
                }
            }

            if (fileDownloadCallbacks != null) {
                fileDownloadCallbacks.add(fileDownloadCallback);
            }
        }
    }

    private static void doCreateDownloadService(Context context) {
        Intent intent = new Intent(context, FileDownloadRemoteService.class);
        try {
            context.startService(intent);
            context.bindService(intent, initServiceConnection(new FileDownloadCallbackImp() {

                        @Override
                        // 标志着download service is ready
                        public void onDownloadListChanged(List<FileDownloadStatus> downloadStatusList) throws RemoteException {
                            synchronized (FileDownloadInterface.class) {
                                if (fileDownloadCallbacks != null) {
                                    for (FileDownloadCallback callback : fileDownloadCallbacks) {
                                        callback.onDownloadListChanged(downloadStatusList);
                                    }
                                    fileDownloadCallbacks.clear();
                                } else if (DebugLog.isDebug()) {
                                    //                                throw new RuntimeException("fileDownloadCallbacks should not be null");
                                }
                            }
                        }

                    }.getInvokeThreadCallback()),
                    Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            fileDownloadCallbacks = null;
            ExceptionUtils.printStackTrace(e);
            if (DebugLog.isDebug()) {
                throw new RuntimeException(e);
            }

        }
    }

    private static ServiceConnection initServiceConnection(
            final FileDownloadCallback fileDownloadCallback) {

        mServiceConnection = new RemoteServiceConnection() {

            @Override
            public void onServiceDisconnected(ComponentName name) {
                setInited(false);

                synchronized (FileDownloadInterface.class) {
                    fileDownloadCallbacks = null;
                }
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                downloadRemoteServiceInterface =
                        FileDownloadRemoteServiceInterface.Stub.asInterface(service);

                try {
                    downloadRemoteServiceInterface.registerCallback(fileDownloadCallback, null);
                } catch (RemoteException e) {
                    if (DebugLog.isDebug()) {
                        ExceptionUtils.printStackTrace(e);
                    }
                }
            }
        };

        return mServiceConnection;
    }

    /**
     * 注销下载服务
     */
    public static void destroyDownloadService(Context context) {
        if (mServiceConnection != null) {
            context.unbindService(mServiceConnection);
        }
    }

    private static abstract class RemoteServiceConnection implements ServiceConnection {
        protected static FileDownloadRemoteServiceInterface downloadRemoteServiceInterface;
    }

    public static boolean isInited() {
        return isInited;
    }

    public static void setInited(boolean isInited) {
        DebugLog.d(FileDownloadInterface.class.getSimpleName(), "setInited: " + isInited);
        FileDownloadInterface.isInited = isInited;
/*        if(!isInited){
            mServiceConnection = null;
        }*/
    }
}
