package org.qiyi.basecore.filedownload;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Pair;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.AbsNetworkChangeCallback;
import org.qiyi.basecore.utils.ApplicationContext;
import org.qiyi.basecore.utils.NetWorkTypeUtils;
import org.qiyi.basecore.utils.NetworkStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author kangle 用于http下载的通用入口类
 */
public class FileDownloadManager {

    private volatile static FileDownloadManager mInstance;

    protected static FileDownloadManager getInstance() {

        if (mInstance == null) {
            synchronized (FileDownloadManager.class) {
                if (mInstance == null) {
                    mInstance = new FileDownloadManager();
                }
            }
        }

        return mInstance;
    }

    private FileDownloadManager() {
    }

    /**
     * 下载任务集合
     */
    private List<FileDownloadTask> mDownloadTaskList = new ArrayList<FileDownloadTask>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 回调助手
     */
    private FileDownloadCallbackHelper fileDownloadCallbackHelper;

    private FileDownloadDBHelper mDbHelper;

    /**
     * 添加下载
     *
     * @param context
     * @param fileDownloadStatusList
     * @param callbackPair           假如不等于null，则为DB onQueryCompleted 的回调，用来充值 cb service is ready
     */
    public void addDownload(Context context, List<FileDownloadStatus> fileDownloadStatusList, Pair<String, FileDownloadCallback> callbackPair) {

        DebugLog.v("FileDownload", "addDownloads in " + fileDownloadStatusList);

        //真正添加的任务队列
        List<FileDownloadTask> addedDownloads = new ArrayList<FileDownloadTask>();

        //已存在相同任务，forceToResume
        List<FileDownloadTask> forceToResumeDownloads = new ArrayList<FileDownloadTask>();

        for (FileDownloadStatus fileDownloadStatus : fileDownloadStatusList) {
            if (checkDownloadConfiguration(fileDownloadStatus)) {

                FileDownloadTask fileDownloadTask = getTaskByStatus(fileDownloadStatus);

                // 已存在相同下载
                if (fileDownloadTask != null) {

                    //更新下载配置
                    fileDownloadTask.updateDownloadConfiguration(context, fileDownloadStatus.mDownloadConfiguration);

                    //强制继续下载
                    if (fileDownloadStatus.mDownloadConfiguration.forceToResume) {
                        forceToResumeDownloads.add(fileDownloadTask);
                    }

                }
                // 重新 or 继续下载
                else {

                    FileDownloadTask downloadTask =
                            new FileDownloadTask(context, fileDownloadStatus,
                                    fileDownloadCallbackHelper, mDbHelper);

                    lock.writeLock().lock();
                    try {
                        mDownloadTaskList.add(downloadTask);
                    } finally {
                        lock.writeLock().unlock();
                    }

                    addedDownloads.add(downloadTask);
                }
            }
        }

        //notify Service is ready
        if (callbackPair != null) {
            fileDownloadCallbackHelper.onDownloadListChanged(callbackPair, getDownloads());
        }
        // 下载队列发生变化，通知注册者
        else if (addedDownloads.size() > 0) {

            fileDownloadCallbackHelper.onDownloadListChanged(getDownloads());

            for (FileDownloadTask fileDownloadTask : addedDownloads) {
                if (fileDownloadTask.fileDownloadStatus.mDownloadConfiguration.forceToResume) {
                    resumeDownload(fileDownloadTask, false);
                } else {
                    startDownload(fileDownloadTask, FileDownloadConstant.TRY_START_DOWNLOAD_FOR_ADD);
                }
            }

            //更新数据库
            mDbHelper.insertOrUpdate(taskToStatus(addedDownloads), null);
        }

        //强制下载
        for (FileDownloadTask fileDownloadTask : forceToResumeDownloads) {
            resumeDownload(fileDownloadTask, fileDownloadTask.getIsRestartAndSet());
        }
    }

    private List<FileDownloadStatus> taskToStatus(List<FileDownloadTask> tasks) {
        List<FileDownloadStatus> status = new ArrayList<FileDownloadStatus>();

        for (FileDownloadTask task : tasks) {
            status.add(task.fileDownloadStatus);
        }

        return status;
    }

    /**
     * 检查该下载任务是否存在
     *
     * @param fileDownloadStatus
     * @return
     */
    private FileDownloadTask getTaskByStatus(FileDownloadStatus fileDownloadStatus) {
        lock.readLock().lock();
        try {
            for (FileDownloadTask fileDownloadTask : mDownloadTaskList) {
                if (fileDownloadStatus.equals(fileDownloadTask.fileDownloadStatus)) {
                    return fileDownloadTask;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    /**
     * 达到该任务类型的最大下载上限 or 达到整个下载服务的最大下载上限
     *
     * @param downloadTask
     * @param downloadTask
     * @return 假如超出，应该暂停的那个任务（null 则说明没有超出）
     */
    private FileDownloadTask reachMaxLoad(FileDownloadTask downloadTask) {

        // 第一个正在下载的任务
        FileDownloadTask firstDownloadingTask = null;

        // 第一个相同类型，正在下载的任务
        FileDownloadTask firstSameTypeDownloadingTask = null;

        int typeMax = downloadTask.fileDownloadStatus.mDownloadConfiguration.getMaxLoad();

        int totalCount = 0;
        int typeCount = 0;

        lock.readLock().lock();
        try {
            for (FileDownloadTask fileDownloadTask : mDownloadTaskList) {
                if (fileDownloadTask.isDownloading()) {

                    if (firstDownloadingTask == null) {
                        firstDownloadingTask = fileDownloadTask;
                    }

                    totalCount++;
                    // 相同类型
                    if (fileDownloadTask.isSameType(downloadTask)) {

                        if (firstSameTypeDownloadingTask == null) {
                            firstSameTypeDownloadingTask = fileDownloadTask;
                        }

                        typeCount++;
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return typeCount >= typeMax
                ? firstSameTypeDownloadingTask
                : totalCount >= FileDownloadConstant.MAX_LOAD ? firstDownloadingTask : null;

    }

    /**
     * 检查下载配置信息是否正确
     *
     * @param fileDownloadStatus
     * @return
     */
    private boolean checkDownloadConfiguration(FileDownloadStatus fileDownloadStatus) {

        if (fileDownloadStatus == null || fileDownloadStatus.mDownloadConfiguration == null
                || !fileDownloadStatus.mDownloadConfiguration.isValid()) {
            return false;
        }

        return true;
    }

    /**
     * @param task
     * @param force 在queue中排队的任务resumeDownload的时候优先级发生变化会force restart
     */
    private void resumeDownload(FileDownloadTask task, boolean force) {
        if (task == null || (task.isDownloading() && !force)) {
            return;
        }

        FileDownloadTask toBePaused = reachMaxLoad(task);

        if (toBePaused != null) {
            toBePaused.pause(new Pair<Integer, String>(FileDownloadConstant.PAUSED_REACH_MAX_LOAD,
                    "同时下载的任务达到上限，按规则暂停"), false);
        }

        task.execute();
    }

    protected void resumeDownload(FileDownloadStatus fileDownloadStatus) {
        resumeDownload(getTaskByStatus(fileDownloadStatus), false);
    }

    /**
     * init 的时候也会调到这里来，完全是为了初始化，因此cb不需要add到mFileDownloadCallbacks中
     *
     * @param context
     * @param cb
     * @param type
     * @throws RemoteException
     */
    protected void registerCallback(final Context context, final FileDownloadCallback cb, final String type)
            throws RemoteException {

        final Pair<String, FileDownloadCallback> callbackPair = new Pair<String, FileDownloadCallback>(type, cb);

        if (fileDownloadCallbackHelper == null) {
            fileDownloadCallbackHelper = new FileDownloadCallbackHelper();
        }

        // 首次启动下载服务，从数据库还原到内存
        if (mDbHelper == null) {
            mDbHelper = new FileDownloadDBHelper(context);
            mDbHelper.query(new FileDownloadDBHelper.DBHelperCallback() {
                @Override
                public void onQueryCompleted(List<FileDownloadStatus> result) {
                    //纠正从本地restore数据的下载状态
                    for (FileDownloadStatus fileDownloadStatus : result){
                        if(fileDownloadStatus != null && fileDownloadStatus.status == FileDownloadConstant.STATUS_RUNNING){
                            DebugLog.v(FileDownloadManager.class.getSimpleName(), "correct downloading state to download failed");
                            fileDownloadStatus.status = FileDownloadConstant.STATUS_FAILED;
                            fileDownloadStatus.reason = FileDownloadConstant.ERROR_FROM_RESTORE;
                        }
                    }
                    addDownload(context, result, callbackPair);
                }

                @Override
                public void onInsertOrUpdateCompleted() {

                }
            });
        } else {
            fileDownloadCallbackHelper.addCallback(callbackPair);

            fileDownloadCallbackHelper.onDownloadListChanged(callbackPair, getDownloads());
        }
    }

    protected void pauseDownload(FileDownloadStatus fileDownloadStatus) {
        FileDownloadTask task = getTaskByStatus(fileDownloadStatus);
        if (task != null) {
            task.pause(new Pair<Integer, String>(FileDownloadConstant.PAUSED_MANUALLY, "手动暂停"), false);
        }
    }

    public List<FileDownloadStatus> getDownloads() {
        List<FileDownloadStatus> list = new ArrayList<FileDownloadStatus>();

        lock.readLock().lock();
        try {
            for (FileDownloadTask fileDownloadTask : mDownloadTaskList) {
                list.add(fileDownloadTask.fileDownloadStatus);
            }
        } finally {
            lock.readLock().unlock();
        }
        return list;
    }

    public void deleteDownloads(List<FileDownloadStatus> fileDownloadStatusList) {

        List<FileDownloadStatus> toDeleteList = new ArrayList<FileDownloadStatus>();
        //暂停并删除下载任务
        for (FileDownloadStatus fileDownloadStatus : fileDownloadStatusList) {
            FileDownloadTask task = getTaskByStatus(fileDownloadStatus);
            if (task != null) {
                task.pause(new Pair<Integer, String>(FileDownloadConstant.PAUSED_BY_DELETED, "因为要删除，首先暂停"), false);

                //删除任务
                lock.writeLock().lock();
                try {
                    mDownloadTaskList.remove(task);
                } finally {
                    lock.writeLock().unlock();
                }

                toDeleteList.add(task.fileDownloadStatus);
            }
        }

        //通知删除事件
        fileDownloadCallbackHelper.onDownloadListChanged(getDownloads());
        //删除数据库和文件
        if (mDbHelper != null) {
            mDbHelper.delete(toDeleteList);
        }
    }

    public void unRegist(FileDownloadCallback cb, String type) {
        fileDownloadCallbackHelper.remove(cb, type);
    }

    public void onDestroy() {

        if (fileDownloadCallbackHelper != null) {
            fileDownloadCallbackHelper.removeAll();
        }

        if (mDbHelper != null) {
            mDbHelper.insertOrUpdate(getDownloads(), new FileDownloadDBHelper.DBHelperCallback() {

                @Override
                public void onQueryCompleted(List<FileDownloadStatus> result) {

                }

                @Override
                public void onInsertOrUpdateCompleted() {
                    mDbHelper.close();
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            });
        } else {
            android.os.Process.killProcess(android.os.Process.myPid());
        }

    }

    /**
     * 尝试下载
     *
     * @param downloadTask = null的时候为自动下载
     * @param reason       下载的原因
     * @param context      for stopping service
     *
     */
    public void startDownload(FileDownloadTask downloadTask, int reason, Context context) {

        //自动下载（未指定下载任务）
        if (downloadTask == null) {
            lock.readLock().lock();
            try {
                boolean haveTask = false;
                for (FileDownloadTask fileDownloadTask : mDownloadTaskList) {
                    if (fileDownloadTask.canExecute(reason)) {
                        haveTask = true;
                        startDownload(fileDownloadTask, reason);
                        break;
                    }else if(fileDownloadTask.fileDownloadStatus.status == FileDownloadConstant.STATUS_RUNNING){
                        haveTask = true;
                    }
                }

                //没有任务了，尝试结束下载服务
                if(!haveTask && context != null){
                    DebugLog.v("FileDownloadRemoteService", "stopService: ");
                    context.stopService(new Intent(context, FileDownloadRemoteService.class));
                }
            } finally {
                lock.readLock().unlock();
            }
        }
        //正在下载的任务达到上限
        else if (reachMaxLoad(downloadTask) != null) {

            downloadTask.pause(new Pair<Integer, String>(FileDownloadConstant.PAUSED_REACH_MAX_LOAD,
                    "同时下载的任务达到上限，先暂停"), false);
        }
        //可以执行
        else if (downloadTask.canExecute(reason)) {
            downloadTask.execute();
        }
    }

    public void startDownload(FileDownloadTask downloadTask, int reason) {
        startDownload(downloadTask, reason, null);
    }

    public FileDownloadNetChange getFileDownloadNetChange() {
        return new FileDownloadNetChange();

    }

    class FileDownloadNetChange extends AbsNetworkChangeCallback {

        @Override
        public void onChangeToOff(NetworkStatus networkStatus) {

            lock.readLock().lock();
            try {
                for (FileDownloadTask fileDownloadTask : mDownloadTaskList) {
                    if (fileDownloadTask.fileDownloadStatus.status == FileDownloadConstant.STATUS_RUNNING) {
                        fileDownloadTask.onPaused(new Pair<Integer, String>(
                                        FileDownloadConstant.PAUSED_WAITING_FOR_NETWORK, "由注册的网络监听告知已经切换到没有网络的环境"),
                                false);
                    }
                }
            } finally {
                lock.readLock().unlock();
            }

        }


        @Override
        public void onChangeToWIFI(NetworkStatus networkStatus) {

            lock.readLock().lock();
            try {
                for (FileDownloadTask fileDownloadTask : mDownloadTaskList) {
                    if (fileDownloadTask.shouldRestartForNet(NetworkStatus.WIFI)) {
                        startDownload(fileDownloadTask, fileDownloadTask.fileDownloadStatus.reason);
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public void onChangeToNotWIFI(NetworkStatus networkStatus) {

            //由于目前的网络变化通知没有正确鉴别4G网络（将4G当作3G处理），这里纠正下网络状态
            networkStatus = NetWorkTypeUtils.getNetworkStatusFor4G(ApplicationContext.app);
            DebugLog.d("FileDownloadManager", "onChangeToNotWIFI: " + networkStatus);

            lock.readLock().lock();
            try {
                for (FileDownloadTask fileDownloadTask : mDownloadTaskList) {
                    //暂停非wifi下不允许下载的任务
                    if (fileDownloadTask.fileDownloadStatus.status == FileDownloadConstant.STATUS_RUNNING &&
                            !fileDownloadTask.fileDownloadStatus.canDownload(networkStatus)) {

                        fileDownloadTask.pause(new Pair<Integer, String>(
                                FileDownloadConstant.PAUSED_WAITING_FOR_NETWORK, "切换到非wifi环境，并且当前任务禁止在非wifi下下载"), false);
                    }
                    //恢复/重试 非wifi下允许下载的任务
                    else if (fileDownloadTask.shouldRestartForNet(networkStatus)) {
                        startDownload(fileDownloadTask, fileDownloadTask.fileDownloadStatus.reason);
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
        }

    }

}