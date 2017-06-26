package com.iqiyi.video.download.filedownload;

import android.content.Context;
import android.text.TextUtils;

import com.iqiyi.video.download.engine.data.DownloadDataSource;
import com.iqiyi.video.download.engine.downloader.BaseQiyiDownloader;
import com.iqiyi.video.download.engine.task.ITaskSchedule;
import com.iqiyi.video.download.engine.task.XBaseTaskExecutor;
import com.iqiyi.video.download.engine.taskmgr.IDownloadTaskCreator;
import com.iqiyi.video.download.engine.taskmgr.paralle.ParalleTaskManager;
import com.iqiyi.video.download.filedownload.db.DBRequestController;
import com.iqiyi.video.download.filedownload.pool.DownloadThreadPool;
import com.iqiyi.video.download.filedownload.schedule.FileSchedule;
import com.iqiyi.video.download.filedownload.task.CdnDownloadFileTask;
import com.iqiyi.video.download.filedownload.task.MultiDownloadFileTask;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.video.module.download.exbean.FileDownloadObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by songguobin on 2016/11/23.
 */
public class QiyiFileDownloader extends BaseQiyiDownloader<FileDownloadObject> {

    public static final String TAG = "QiyiFileDownloader";

    private static final int MAX_PARALLE_TASK = 2;

    private DBRequestController mDbController;

    private ITaskSchedule schedule;

    public QiyiFileDownloader(Context mContext, DBRequestController mDbController) {

        super(new ParalleTaskManager(MAX_PARALLE_TASK));

        //super(new SerialTaskManager<FileDownloadObject>());

        mTaskMgr.setDownloadCreator(new FileDownloadTaskCreator());

        mTaskMgr.setAutoRunning(true);

        this.mContext = mContext;

        this.mDbController = mDbController;

        this.schedule = new FileSchedule();

        mAllDownloadData = new DownloadDataSource<FileDownloadObject>() {
            @Override
            public String getSourceName() {
                return "File";
            }

            @Override
            public String getId(FileDownloadObject item) {
                return item.getId();
            }
        };
    }

    /**
     * 在添加下载任务之前，确定下载方式JAVA or HCDN
     *
     * @param tasks
     * @return
     */
    @Override
    public List<FileDownloadObject> onPreAddDownloadTask(List<FileDownloadObject> tasks) {
        List<FileDownloadObject> addApks = super.onPreAddDownloadTask(tasks);
        return addApks;
    }

    /**
     * apk下载的网络判断和sd卡判断在上层做，所以这里覆盖掉父类的默认
     * 逻辑
     */
    /**
     * 网络断开会调用此方法。
     * 此方法提供了默认的处理逻辑，子类可以根据需要改写此方法
     */
    protected void netWorkOff() {
        DebugLog.log(TAG, "QiyiFileDownloader-->netWorkOff");
        mTaskMgr.pause();
        mTaskMgr.setAutoRunning(false);
        mHandler.obtainMessage(LISTENER_ON_NO_NETWORK).sendToTarget();
    }

    /**
     * 网络连接上wifi网络会调用此方法
     * 此方法提供了默认的处理逻辑，子类可以根据需要改写此方法
     */
    protected void netWorkToWifi() {
        DebugLog.log(TAG, "QiyiFileDownloader-->netWorkToWifi");
        mTaskMgr.setAutoRunning(true);
        mTaskMgr.startAll();
        mHandler.obtainMessage(LISTENER_ON_NETWORK_WIFI).sendToTarget();
    }

    /**
     * 网络连接上蜂窝网络会调用此方法
     * 此方法提供了默认的处理逻辑，子类可以根据需要改写此方法
     */
    protected void netWorkToMobile() {
        DebugLog.log(TAG, "QiyiFileDownloader-->netWorkToMobile");
        //mTaskMgr.pause();
        //mTaskMgr.setAutoRunning(false);
        mHandler.obtainMessage(LISTENER_ON_NETWORK_NOT_WIFI).sendToTarget();
    }



    @Override
    protected void sdCardInsert() {

        DebugLog.log(TAG, "QiyiFileDownloader-->sdCardInsert");
    }

    @Override
    protected void sdCardRemove() {

        DebugLog.log(TAG, "QiyiFileDownloader-->sdCardRemove");
    }

    @Override
    protected void loadFromPersistence(final ILoadFromPersistenceListener<FileDownloadObject> listener) {
        if (listener != null) {
            listener.loadSuccess(new ArrayList<FileDownloadObject>());
        }
    }

    @Override
    protected boolean saveToPersistence(final List<FileDownloadObject> beans, PersistenceType dbType, final ISavePersistenceListener<FileDownloadObject> listener) {
        if (beans == null) {
            DebugLog.log(TAG, "saveToPersistence beans is null or size==0");
            return false;
        }
        DebugLog.log(TAG, "saveToPersistence>>>" + dbType);

        if (listener != null) {
            listener.addSuccess(beans);
        }

        return true;
    }

    @Override
    protected boolean deleteLocalFile(final List<FileDownloadObject> beans, final IDeleteFileListener<FileDownloadObject> listener) {

        DebugLog.log(TAG, "deleteLocalFile");

        final List<String> deleteList = new ArrayList<>();

        for (FileDownloadObject bean : beans) {
            DebugLog.log(TAG, "删除文件 = " + bean.getDownloadingPath());
            deleteList.add(bean.getDownloadingPath());
        }

        if (deleteList.size() > 0) {

            DownloadThreadPool.DOWNLOAD_POOL.submit(new Runnable() {
                @Override
                public void run() {
                    ArrayList<String> deleteStatistic = new ArrayList<String>();

                    for (String deletePath : deleteList) {

                        boolean isDeleteSuccess = FileDownloadHelper.deleteFile(deletePath);

                        if (isDeleteSuccess) {
                            deleteStatistic.add(deletePath);
                        }

                    }

                    DebugLog.log(TAG, "删除文件统计 = " + deleteList.size() + "/" + deleteStatistic.size());

                }
            });
        }

        return true;
    }

    @Override
    protected boolean deleteLocalFileSync(List<FileDownloadObject> beans, IDeleteFileListener<FileDownloadObject> listener) {
        DebugLog.log(TAG, "deleteLocalFileSync");

        return false;
    }

    @Override
    protected boolean onUpdateDownloadTask(List<FileDownloadObject> beans, int key, Object value) {
        DebugLog.log(TAG, "onUpdateDownloadTask");
        switch (key){

            case FileDownloadConstant.UPDATE_FILE_DOWNLOAD_OBJECT:
                if(value == null){
                    return false;
                }
                HashMap<String,FileDownloadObject> hashMap = new HashMap<String,FileDownloadObject>((HashMap<String,FileDownloadObject>) value);

                 for(FileDownloadObject bean:beans){

                     DebugLog.log(TAG,"update  bean before = " + bean.toString());

                     bean.updateDownloadConfig(hashMap.get(bean.getId()));

                     DebugLog.log(TAG,"update bean after= " + bean.toString());
                 }

                return true;

        }
        return false;
    }

    @Override
    protected boolean onUpdateDownloadTask(List<FileDownloadObject> beans, int key) {
        DebugLog.log(TAG, "onUpdateDownloadTask");



        return true;
    }

    @Override
    protected void setDeleteFlag(List<FileDownloadObject> beans) {

        //文件下载功能，下载中心不提供设置是否需要删除标记

        DebugLog.log(TAG, "setDeleteFlag");

    }

    @Override
    protected void setTaskSchedule(ITaskSchedule<FileDownloadObject> schedule) {

        mTaskMgr.setTaskSchedule(schedule);

    }

    @Override
    public void init() {

        DebugLog.log(TAG, "****QiyiFileDownloader init****");
        setTaskSchedule(schedule);

    }

    @Override
    public void exit() {
        DebugLog.log(TAG, "****QiyiFileDownloader exit****");
        stopAndClear();
    }


    @Override
    public boolean isAutoRunning() {
        return mTaskMgr.isAutoRunning();
    }

    @Override
    public void deleteLocalFile(List<FileDownloadObject> beans) {
        deleteLocalFile(beans, new IDeleteFileListener<FileDownloadObject>() {
            @Override
            public void deleteSuccess(List<FileDownloadObject> beans) {
                DebugLog.log(TAG, "local apk data delete success");
            }

            @Override
            public void deleteFailed(List<FileDownloadObject> beans) {
                DebugLog.log(TAG, "local apk data delete failed");
            }
        });
    }

    @Override
    public void setMaxParalleNum(int paralleNum) {

    }

    public class FileDownloadTaskCreator implements IDownloadTaskCreator {

        @Override
        public XBaseTaskExecutor createDownloadTask(String taskId) {
            if (TextUtils.isEmpty(taskId)) {
                DebugLog.log(TAG, "FileDownloadTaskCreator->createDownloadTask->taskId is null");
                return null;
            }
            DebugLog.log(TAG, "FileDownloadTaskCreator->createDownloadTask->taskId:" + taskId);

            FileDownloadObject fileObject = mAllDownloadData.getById(taskId);

            if (fileObject == null) {
                DebugLog.log(TAG, "FileDownloadTaskCreator->createDownloadTask fileObject is null!");
                return null;
            }

            switch (fileObject.getDownWay()) {
                case FileDownloadConstant.DOWNLOAD_WAY_FILE_CDN:
                    DebugLog.log(TAG, "FileDownloadTaskCreator-->DOWNLOAD_WAY_FILE_CDN");
                    return new CdnDownloadFileTask(mContext, fileObject, mDbController);

                case FileDownloadConstant.DOWNLOAD_WAY_FILE_CDN_MULTI:
                    DebugLog.log(TAG, "FileDownloadTaskCreator-->DOWNLOAD_WAY_FILE_CDN_MULTI");
                    return new MultiDownloadFileTask(mContext, fileObject, mDbController);
                default:
                    DebugLog.log(TAG, "FileDownloadTaskCreator-->DOWNLOAD_WAY_FILE_CDN:default");
                    return new CdnDownloadFileTask(mContext, fileObject, mDbController);
            }
        }

        @Override
        public TaskBean createTaskBean(String taskId) {
            FileDownloadObject task = mAllDownloadData.getById(taskId);
            if (task == null) {
                DebugLog.log(TAG, "createTaskBean  task ==null");
                return null;
            }
            return new TaskBean<FileDownloadObject>(taskId, task.getStatus());
        }
    }


}
