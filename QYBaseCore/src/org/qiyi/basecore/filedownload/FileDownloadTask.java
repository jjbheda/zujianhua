package org.qiyi.basecore.filedownload;

import android.content.Context;
import android.util.Pair;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.filedownload.FileDownloadStatus.DownloadConfiguration;
import org.qiyi.basecore.filedownload.FileDownloadStatus.IOnCompleted;
import org.qiyi.basecore.utils.NetworkStatus;

import java.io.File;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author kangle 对于下载任务的封装
 */
public class FileDownloadTask extends IChangeImp {

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "FileDownloadThread #" + mCount.getAndIncrement());
        }
    };

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
//    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;
    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new PriorityBlockingQueue<Runnable>(128, new Comparator<Runnable>() {
                @Override
                public int compare(Runnable lhs, Runnable rhs) {
                    int ret = 0;
                    if(rhs instanceof PriorityFutureTask && lhs instanceof PriorityFutureTask){
                        ret = ((PriorityFutureTask) rhs).getPriority() - ((PriorityFutureTask) lhs).getPriority();
                    }
                    return ret;
                }
            });

    /**
     * 进度至少增加 MINIMUS_DOWNLOAD_PROGRESS， 才触发{@link #onDownloadProgress(long, boolean)}
     */
    private static final long MINIMUS_DOWNLOAD_PROGRESS = 128 * 1024L;

    /**
     * 进度至少相隔MINIMUS_DOWNLOAD_INTERVAL 毫秒， 才触发{@link #onDownloadProgress(long, boolean)}
     */
    private static final int MINIMUS_DOWNLOAD_INTERVAL = 1000;
    
    /**
     * 上次 {@link #onDownloadProgress(long, boolean)} 的大小 联合 MINIMUS_DOWNLOAD_PROGRESS 判断是否应该传递进度
     */
    private long lastOnDownloadProgressSize;
    
    /**
     * 上次 {@link #onDownloadProgress(long, boolean)} 的时间 联合 MINIMUS_DOWNLOAD_INTERVAL 判断是否应该传递进度
     */
    private long lastOnDownloadProgressTime;

    private Future<?> currentFuture;
    
    public static final ThreadPoolExecutor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(MAXIMUM_POOL_SIZE,
            MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory){
        @Override
        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
            return new PriorityFutureTask<T>(callable);
        }
    };

    private FileDownloadNotification downloadNotification;

    private FileDownloadDBHelper mDbHelper;

    private Context mContext;
    private int currentRetryForNet;
    private boolean priorityChangedInQueue;

    public FileDownloadTask(Context context, FileDownloadStatus fileDownloadStatus,
                            FileDownloadCallbackHelper fileDownloadCallbackHelper, FileDownloadDBHelper mDbHelper) {
        this.mContext = context;
        this.fileDownloadCallbackHelper = fileDownloadCallbackHelper;
        this.downloadNotification = fileDownloadStatus.getDownloadNotification(context);
        this.fileDownloadStatus = fileDownloadStatus;
        this.mDbHelper = mDbHelper;
    }

    private FileDownloadCallbackHelper fileDownloadCallbackHelper;

    public FileDownloadStatus fileDownloadStatus;

    public void execute() {
        //先尝试暂停
        pause(new Pair<Integer, String>(FileDownloadConstant.PAUSED_BY_OTHER_EXECUTE,
                "由 execute 引起的临时暂停（紧接着就会变为 STATUS_RUNNING）"), true);

        Callable runnable = new FileDownloadThread(fileDownloadStatus, mContext, this);
        
        onDownloadProgress(0, false);

        currentFuture = THREAD_POOL_EXECUTOR.submit(runnable);
    }

    /**
     * @return 该task在该原因下是否可以执行
     */
    public boolean canExecute(int reason) {

        return fileDownloadStatus.status == FileDownloadConstant.STATUS_PENDING
                || ((fileDownloadStatus.status == FileDownloadConstant.STATUS_PAUSED || fileDownloadStatus.status == FileDownloadConstant.STATUS_FAILED)
                && fileDownloadStatus.reason == reason);
    }

    /**
     * @param reason 需要特殊处理 reason.first == FileDownloadConstant.PAUSED_BY_DELETED
     * @param isOutdated
     */
    @Override
    public void onPaused(Pair<Integer, String> reason, boolean isOutdated) {

        DebugLog.v("FileDownload", "onPaused for " + fileDownloadStatus + " "
                + reason.second);

        if (isOutdated) {
            return;
        }
        
        boolean isPauseForDeleting = reason.first == FileDownloadConstant.PAUSED_BY_DELETED;
        
        this.fileDownloadStatus.status = FileDownloadConstant.STATUS_PAUSED;
        this.fileDownloadStatus.reason = reason.first;
        
        if(!isPauseForDeleting){
            fileDownloadCallbackHelper.onPaused(fileDownloadStatus);
        }

        if (downloadNotification != null) {
            if(isPauseForDeleting){
                downloadNotification.onDismiss(fileDownloadStatus);
            }else{
                downloadNotification.onPaused(fileDownloadStatus);
            }
        }

        if(this.fileDownloadStatus.reason != FileDownloadConstant.PAUSED_REACH_MAX_LOAD){
            FileDownloadManager.getInstance().startDownload(null, FileDownloadConstant.PAUSED_REACH_MAX_LOAD);
        }
    }
    
    @Override
    public void onFailed(Pair<Integer, String> reason, boolean isOutdated) {

        DebugLog.v("FileDownload", "onFailed in " + FileDownloadTask.class.getName() + " "
                + reason.second);

        if (isOutdated) {
            return;
        }

        this.fileDownloadStatus.status = FileDownloadConstant.STATUS_FAILED;
        this.fileDownloadStatus.reason = reason.first;
        //文件大小的效验的失败，需要重新下载
        if (reason.first == FileDownloadConstant.ERROR_VALIDATE_FAILED) {
            fileDownloadStatus.total_size_bytes = -1;
            fileDownloadStatus.bytes_downloaded_so_far = 0;
        }

        fileDownloadCallbackHelper.onFailed(fileDownloadStatus);

        if (downloadNotification != null) {
            downloadNotification.onFailed(fileDownloadStatus);
        }

        FileDownloadManager.getInstance().startDownload(null, FileDownloadConstant.PAUSED_REACH_MAX_LOAD);
    }

    @Override
    public void onCompleted(boolean isOutdated) {

        DebugLog.v("FileDownload", "onCompleted in " + FileDownloadTask.class.getName());
        
        if (isOutdated) {
            return;
        }

        //检查文件
        File downloadedFile = fileDownloadStatus.getDownloadedFile();
        
        if(downloadedFile == null){
            onFailed(new Pair<Integer, String>(FileDownloadConstant.ERROR_DOWNLOAD_FILE_NOT_FOUND, "下载完成的文件不见了"), false);
            return;
        }
        
        fileDownloadStatus.status = FileDownloadConstant.STATUS_SUCCESSFUL;

        fileDownloadCallbackHelper.onCompleted(fileDownloadStatus);
        
        /*
         * 假如用户配置了下载完要做的事，则帮其完成（完成需求，客户端退出之后，apk下载完毕仍然能弹出安装），
         * 为了不影响正常流程，配置了这个的下载任务需要保证在client端的onCompleted中不重复做同样的事
        */
        if(fileDownloadStatus.mDownloadConfiguration.customObj instanceof IOnCompleted){
            ((IOnCompleted)fileDownloadStatus.mDownloadConfiguration.customObj).onCompleted(mContext, fileDownloadStatus);
        }

        if (downloadNotification != null) {
            downloadNotification.onCompleted(fileDownloadStatus.getDownloadedFile(),
                    fileDownloadStatus);
        }
        
        //持久化到数据库
        mDbHelper.insertOrUpdate(fileDownloadStatus, null);
        
        FileDownloadManager.getInstance().startDownload(null, FileDownloadConstant.PAUSED_REACH_MAX_LOAD, mContext);
    }

    @Override
    public void onDownloadProgress(long len, boolean isOutdated) {

        fileDownloadStatus.bytes_downloaded_so_far += len;

        if (isOutdated || checkOnProgressFrequency()) {
            return;
        }

        DebugLog.v("FileDownload", "onDownloadProgress " + fileDownloadStatus);

        fileDownloadStatus.status = FileDownloadConstant.STATUS_RUNNING;
        
        fileDownloadCallbackHelper.onDownloadProgress(fileDownloadStatus);

        // 通知栏大于1%才更新界面
        if (downloadNotification != null) {
            downloadNotification.onDownloadProgress(fileDownloadStatus);
        }
    }

    /**
     *  屏蔽过快的 进度通知（但是第一次的progress得发出去 lastOnDownloadProgressSize == 0）
     * @return
     */
    private boolean checkOnProgressFrequency() {
        
        boolean ret =
                lastOnDownloadProgressSize != 0
                        && ((fileDownloadStatus.bytes_downloaded_so_far > lastOnDownloadProgressSize && fileDownloadStatus.bytes_downloaded_so_far
                                - lastOnDownloadProgressSize < MINIMUS_DOWNLOAD_PROGRESS) || System
                                .currentTimeMillis() - lastOnDownloadProgressTime < MINIMUS_DOWNLOAD_INTERVAL);
        
        if(!ret){
            lastOnDownloadProgressSize = fileDownloadStatus.bytes_downloaded_so_far;
            lastOnDownloadProgressTime = System.currentTimeMillis();
        }
        
        return ret;
    }

    protected boolean isSameType(FileDownloadTask downloadTask) {
        
        String type = downloadTask.fileDownloadStatus.mDownloadConfiguration.getType();
        
        if ((fileDownloadStatus.mDownloadConfiguration.getType() == null 
                && type == null)
                || (fileDownloadStatus.mDownloadConfiguration.getType() != null 
                && fileDownloadStatus.mDownloadConfiguration.getType().equals(type))) {

            return true;
        }
        return false;
    }

    /**
     * 暂停下载
     * @param codeAndReason 
     */
    public void pause(Pair<Integer, String> codeAndReason, boolean isOutdated) {
        
        if(currentFuture != null){
            currentFuture.cancel(true);
        }
        
        this.currentThread = null;
        
        lastOnDownloadProgressSize = 0;
        if(downloadNotification != null){
            downloadNotification.setLastDownloadPercent(-1);
        }
        
        onPaused(codeAndReason, isOutdated);
        
    }

    protected boolean isDownloading() {
        return fileDownloadStatus.status == FileDownloadConstant.STATUS_RUNNING;
    }

    public void updateDownloadConfiguration(Context context, DownloadConfiguration mDownloadConfiguration) {
        //previous FileDownloadNotificationConfiguration
        DownloadConfiguration previousDownloadConfiguration = fileDownloadStatus.mDownloadConfiguration;
        fileDownloadStatus.mDownloadConfiguration = mDownloadConfiguration;

        FileDownloadNotification downloadNotificationNew = fileDownloadStatus.getDownloadNotification(context);
        
        //downloadNotification的更新不能从有到无（通知栏会停止更新）
        if(downloadNotificationNew != null){
            downloadNotification = downloadNotificationNew;
        }else{
            fileDownloadStatus.mDownloadConfiguration.fileDownloadNotification = previousDownloadConfiguration.fileDownloadNotification;
        }

        priorityChangedInQueue = false;
        //优先级发生变化
        if (previousDownloadConfiguration.priority != fileDownloadStatus.mDownloadConfiguration.priority) {

            //正在queue中排队
            for (Runnable runnable : THREAD_POOL_EXECUTOR.getQueue()) {
                if (runnable.equals(currentFuture)) {
                    DebugLog.v("FileDownloadTask", "priorityChangedInQueue = true");
                    priorityChangedInQueue = true;
                    break;
                }
            }
        }
    }

    /**
     * @param networkStatus
     * @return 网络发生变化的时候，是否应该由非下载状态转为下载状态（由于网络暂停 or 由于网络失败，但在允许的重试次数范围内）
     */
    protected boolean shouldRestartForNet(NetworkStatus networkStatus) {
        boolean pausedByNet = fileDownloadStatus.status == FileDownloadConstant.STATUS_PAUSED
                && FileDownloadConstant.pausedByNet(fileDownloadStatus.reason);

        boolean failedByNet = fileDownloadStatus.status == FileDownloadConstant.STATUS_FAILED
                && FileDownloadConstant.failedForNet(fileDownloadStatus.reason);

        return fileDownloadStatus.canDownload(networkStatus)
                && (pausedByNet || (failedByNet && (currentRetryForNet++ < fileDownloadStatus.mDownloadConfiguration.maxRetryForNet)));
    }

    public boolean getIsRestartAndSet() {
        boolean ret = priorityChangedInQueue;
        priorityChangedInQueue = false;
        return ret;
    }
}


/**
 * @author kangle 只用于FileDownloadThread 对 FileDownloadTask的回执
 * 
 */
interface IChange {


    /**
     * 下载进度更新
     * 
     * @param len 新增下载量
     * @param isOutdated 是否是过时的callback（由之前尚未彻底停止的线程发出）
     */
    void onDownloadProgress(long len, boolean isOutdated);

    /**
     * 下载由线程自主暂停
     * 
     * @param reason 自主暂停原因（code & desc）
     * @param isOutdated 是否是过时的callback（由之前尚未彻底停止的线程发出）
     */
    void onPaused(Pair<Integer, String> reason, boolean isOutdated);

    /**
     * 下载失败
     * 
     * @param reason 失败原因（code & desc）
     * @param isOutdated 是否是过时的callback（由之前尚未彻底停止的线程发出）
     */
    void onFailed(Pair<Integer, String> reason, boolean isOutdated);

    /**
     * 下载完成
     * 
     * @param isOutdated 是否是过时的callback（由之前尚未彻底停止的线程发出）
     */
    void onCompleted(boolean isOutdated);

    /**
     * 等发现下载URL存在重定向，更新下载的URL
     * 
     * @param redirectUrl
     * @param fileDownloadStatus
     */
    void onDownloadUrlRedirect(String redirectUrl, FileDownloadStatus fileDownloadStatus);
}
