package com.iqiyi.video.download.engine.downloader;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.iqiyi.video.download.engine.data.DownloadDataSource;
import com.iqiyi.video.download.engine.task.ITaskSchedule;
import com.iqiyi.video.download.engine.taskmgr.IDownloadTaskListener;
import com.iqiyi.video.download.engine.taskmgr.serial.ISerialTaskManager;
import com.iqiyi.video.download.filedownload.TaskBean;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.storage.StorageCheckor;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.NetWorkTypeUtils;
import org.qiyi.basecore.utils.NetworkStatus;
import org.qiyi.video.module.download.exbean.XTaskBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <pre>
 * 实现IQiyiDownloader接口的抽象下载管理器类。
 * 该基类实现了通用的基础功能，子类继承时需要实现几个定义的抽象方法，
 * 抽象方法中是具体的业务逻辑。
 * </pre>
 */
public abstract class BaseQiyiDownloader<B extends XTaskBean> implements IQiyiDownloader<B> {

    private static final String TAG = "BaseQiyiDownloader";

    protected static final int LISTENER_ON_LOAD = 1;

    protected static final int LISTENER_ON_ADD = 5;

    protected static final int LISTENER_ON_DELETE = 6;

    protected static final int LISTENER_ON_UPDATE = 7;

    protected static final int LISTENER_ON_START = 10;

    protected static final int LISTENER_ON_PAUSE = 11;

    protected static final int LISTENER_ON_STOP_ALL = 12;

    protected static final int LISTENER_ON_DOWNLOADING = 13;

    protected static final int LISTENER_ON_COMPLETE = 15;

    protected static final int LISTENER_ON_ERROR = 16;

    protected static final int LISTENER_ON_FINISH_ALL = 20;

    protected static final int LISTENER_ON_NO_NETWORK = 30;

    protected static final int LISTENER_ON_NETWORK_NOT_WIFI = 31;

    protected static final int LISTENER_ON_NETWORK_WIFI = 32;

    protected static final int LISTENER_ON_MOUNTED_SD_CARD = 33;

    protected static final int LISTENER_ON_UNMOUNTED_SD_CARD = 34;

    protected static final int LISTENER_ON_PREPARE = 35;

    protected static final int LISTENER_ON_PAUSE_ALL = 36;

    protected static final int LISTENER_ON_SDFULL = 37;

    // 持久层操作类型
    protected static enum PersistenceType {

        CREATE, DELETE, UPDATE

    }

    protected Context mContext;

    // 任务管理器
    protected ISerialTaskManager<B> mTaskMgr;

    // 内存中的下载数据集合
    protected DownloadDataSource<B> mAllDownloadData;


    // 外部监听集合
    protected List<IQiyiDownloaderListener<B>> mListeners;

    // 内部Handler，分配监听的执行时机

    protected Handler mHandler;

    // 是否已加载过下载数据
    private boolean hasLoaded;

    public BaseQiyiDownloader(ISerialTaskManager<B> taskMgr) {
        mTaskMgr = taskMgr;
        // 初始化监听集合
        mListeners = new CopyOnWriteArrayList<IQiyiDownloaderListener<B>>();
        // 注册对任务管理器的内部监听
        mTaskMgr.registerListener(new InnerListener());
        // 创建Handler
        mHandler = new InnerHandler(Looper.getMainLooper());
    }

    @Override
    public final void load(final boolean isForce) {
        DebugLog.log(TAG, "###load(), isForce:" + isForce);
        //从未加载过持久层的数据，或强制重新从持久层加载
        if (!hasLoaded || isForce) {
            hasLoaded = true;
            // 从持久层加载数据
            loadFromPersistence(new ILoadFromPersistenceListener<B>() {
                @Override
                public void loadSuccess(List<B> beans) {
                    // 先清空当前的内存中的任务
                    mTaskMgr.stopAndReset();// 清空任务管理器
                    mAllDownloadData.clear();// 清空内存
                    // 添加进内存中
                    mAllDownloadData.addAll(beans);
                    // 添加进任务管理器中
                    List<TaskBean<B>> addTasks = new ArrayList<TaskBean<B>>();
                    for (B bean : beans) {
                        // 任务状态是未完成的并且没有被标记需要删除标记的，才需要创建TaskExecutor添加进任务管理器中
                        if (bean.getStatus() != XTaskBean.STATUS_DONE && bean.getNeeddel() != 1) {
                            if ((bean.getStatus() == XTaskBean.STATUS_ERROR && bean.recoverToDoStatus())
                                    || bean.getStatus() == XTaskBean.STATUS_STARTING) {
                                //启动时，将所有的错误状态变为等待中执行状态
                                bean.setStatus(XTaskBean.STATUS_TODO);

                            }
                            TaskBean<B> task = new TaskBean<B>(bean.getId(), bean.getStatus());
                            addTasks.add(task);

                        }
                    }
                    mTaskMgr.addTasks(addTasks);
                    // 回调
                    Message msg = mHandler.obtainMessage(LISTENER_ON_LOAD);
                    mHandler.sendMessage(msg);
                }

                @Override
                public void loadFail() {
                    if (!isForce) {
                        hasLoaded = false;
                    }
                }
            });
        } else {
            // 什么都不做，直接回调
            Message msg = mHandler.obtainMessage(LISTENER_ON_LOAD);
            mHandler.sendMessage(msg);
        }
    }

    @Override
    public final boolean addDownloadTasks(List<B> tasks) {
        DebugLog.log(TAG, "###addDownloadTasks(), tasks:" + tasks);
        final List<B> addBeans = onPreAddDownloadTask(tasks);
        if (addBeans == null) {
            return false;
        }
        // 添加进任务管理器中
        List<TaskBean<B>> addTasks = new ArrayList<TaskBean<B>>();
        for (B bean : addBeans) {
            // 任务状态是未完成的，才需要创建TaskExecutor添加进任务管理器中
            if (bean.getStatus() != XTaskBean.STATUS_DONE) {
                if ((bean.getStatus() == XTaskBean.STATUS_ERROR
                        ||bean.getStatus() == XTaskBean.STATUS_STARTING
                )
                        && bean.recoverToDoStatus()) {
                    //启动时，将所有的错误状态变为等待中执行状体
                    bean.setStatus(XTaskBean.STATUS_TODO);
                }
                TaskBean<B> task = new TaskBean<B>(bean.getId(), bean.getStatus());
                task.setScheduleBean(bean.getScheduleBean());
                addTasks.add(task);

            }
        }
        mTaskMgr.addTasks(addTasks);
        // 添加进内存中
        mAllDownloadData.addAll(addBeans);


        // 添加进持久层
        saveToPersistence(addBeans, PersistenceType.CREATE, new ISavePersistenceListener<B>() {
            @Override
            public void addSuccess(List<B> beans) {
                DebugLog.log(TAG, "saveToPersistence addSuccess!");
                // 回调
                Message msg = mHandler.obtainMessage(LISTENER_ON_ADD);
                msg.obj = addBeans;
                mHandler.sendMessage(msg);
            }

            @Override
            public void addFail() {
                DebugLog.log(TAG, "saveToPersistence addFail!");
            }
        });

        return true;
    }

    @Override
    public boolean startDownload() {
        DebugLog.log(TAG, "###startDownload()");
        return mTaskMgr.start();
    }

    @Override
    public boolean startDownload(String taskId) {
        DebugLog.log(TAG, "###startDownload(), taskId:" + taskId);
        return mTaskMgr.start(taskId);
    }

    @Override
    public boolean startAllDownload() {
        DebugLog.log(TAG, "###startAllDownload()");
        return mTaskMgr.startAll();
    }

    @Override
    public boolean stopDownload() {
        DebugLog.log(TAG, "###stopDownload()");
        return mTaskMgr.stop();
    }

    @Override
    public boolean stopDownload(String taskId) {
        DebugLog.log(TAG, "###stopDownload(), taskId:" + taskId);
        return mTaskMgr.stop(taskId);
    }

    @Override
    public boolean stopAllDownload() {
        DebugLog.log(TAG, "###stopAllDownload()");
        return mTaskMgr.stopAll();
    }

    @Override
    public boolean pauseDownload() {
        DebugLog.log(TAG, "###pauseDownload()");
        return mTaskMgr.pause();
    }

    @Override
    public boolean pauseDownload(String taskId) {
        DebugLog.log(TAG, "###pauseDownload(), taskId:" + taskId);
        return mTaskMgr.pause(taskId);
    }

    @Override
    public final boolean deleteDownloadTasks(List<String> tasksIds) {
        DebugLog.log(TAG, "###deleteDownloadTasks(), tasksIds:" + tasksIds);
        if (tasksIds == null || tasksIds.size() == 0) {
            return false;
        }
        DebugLog.log(TAG, "通过downloadKey,从mAllDownloadData中获取downloadObject");

        // 筛选出被删除的对象
        final List<B> removedBeans = new ArrayList<B>();
        for (String taskId : tasksIds) {
            B bean = mAllDownloadData.getById(taskId);
            if (bean != null) {
                removedBeans.add(bean);
            }
        }
        // 如果没有可删除的，直接结束
        DebugLog.log(TAG, " removedBeans size = " + removedBeans.size());

        if (removedBeans.size() == 0) {
            return false;
        }

        // 删除TaskMgr中的任务
        mTaskMgr.removeTasksById(tasksIds);
        // 删除内存中的数据
        mAllDownloadData.deleteAllById(tasksIds);
        // 删除本地文件
        IDeleteFileListener<B> listener = new IDeleteFileListener<B>() {
            @Override
            public void deleteSuccess(List<B> beans) {
                DebugLog.log(TAG, "deleteSuccess");
                // 本地视频文件删除完毕之后，删除数据库
                saveToPersistence(beans, PersistenceType.DELETE, null);
                // 回调
                Message msg = mHandler.obtainMessage(LISTENER_ON_DELETE);
                msg.obj = beans;
                mHandler.sendMessage(msg);
            }

            @Override
            public void deleteFailed(List<B> beans) {
                DebugLog.log(TAG, "deleteFailed");
                //删除失败，需要将对象重新加入内存
                mAllDownloadData.addAll(beans);
                //回调
                Message msg = mHandler.obtainMessage(LISTENER_ON_DELETE);
                msg.arg1 = 0x10;   // 0x10 represent failed
                msg.obj = beans;
                mHandler.sendMessage(msg);
            }
        };
        //删除本地文件之前，先将需要删除的记录needdel字段标记为"1"
        DebugLog.log(TAG, "将需要删除的记录needdel字段标记为1");
        setDeleteFlag(removedBeans);
        DebugLog.log(TAG, "更新数据库");
        saveToPersistence(removedBeans, PersistenceType.UPDATE, null);
        DebugLog.log(TAG, "删除本地文件");
        deleteLocalFile(removedBeans, listener);
        return true;
    }

    @Override
    public final boolean deleteDownloadTasksSync(List<String> tasksIds) {
        DebugLog.log(TAG, "###deleteDownloadTasksSync(), tasksIds:" + tasksIds);
        if (tasksIds == null || tasksIds.size() == 0) {
            return false;
        }
        DebugLog.log(TAG, "通过downloadKey,从mAllDownloadData中获取downloadObject");

        // 筛选出被删除的对象
        final List<B> removedBeans = new ArrayList<B>();
        for (String taskId : tasksIds) {
            B bean = mAllDownloadData.getById(taskId);
            if (bean != null) {
                removedBeans.add(bean);
            }
        }
        // 如果没有可删除的，直接结束
        DebugLog.log(TAG, " 如果没有可删除的，直接结束");

        if (removedBeans.size() == 0) {
            return false;
        }

        // 删除TaskMgr中的任务
        mTaskMgr.removeTasksById(tasksIds);
        // 删除内存中的数据
        mAllDownloadData.deleteAllById(tasksIds);
        // 删除本地文件
        IDeleteFileListener<B> listener = new IDeleteFileListener<B>() {
            @Override
            public void deleteSuccess(List<B> beans) {
                DebugLog.log(TAG, "deleteSuccess");
                // 本地视频文件删除完毕之后，删除数据库
                saveToPersistence(beans, PersistenceType.DELETE, null);
                // 回调
                Message msg = mHandler.obtainMessage(LISTENER_ON_DELETE);
                msg.arg1 = 0;  // 0 represent success
                msg.obj = beans;
                mHandler.sendMessage(msg);
            }

            @Override
            public void deleteFailed(List<B> beans) {
                DebugLog.log(TAG, "deleteFailed");
                //删除失败，需要将对象重新加入内存
                mAllDownloadData.addAll(beans);
                //回调
                Message msg = mHandler.obtainMessage(LISTENER_ON_DELETE);
                msg.arg1 = 0x10;   // 0x10 represent failed
                msg.obj = beans;
                mHandler.sendMessage(msg);
            }
        };
        //删除本地文件之前，先将需要删除的记录needdel字段标记为"1"
        DebugLog.log(TAG, "将需要删除的记录needdel字段标记为1");
        setDeleteFlag(removedBeans);
        DebugLog.log(TAG, "更新数据库");
        saveToPersistence(removedBeans, PersistenceType.UPDATE, null);
        DebugLog.log(TAG, "删除本地文件");
        deleteLocalFileSync(removedBeans, listener);
        return true;
    }

    /**
     *
     * @param tasksIds
     * @return
     */
    @Override
    public final boolean deleteDownloadTasksForFast(List<String> tasksIds) {
        DebugLog.log(TAG, "###deleteDownloadTasksForFast(), tasksIds:" + tasksIds);
        if (tasksIds == null || tasksIds.size() == 0) {
            return false;
        }

        // 筛选出被删除的对象
        final List<B> removedBeans = new ArrayList<B>();
        for (String taskId : tasksIds) {
            B bean = mAllDownloadData.getById(taskId);
            if (bean != null) {
                removedBeans.add(bean);
            }
        }
        // 如果没有可删除的，直接结束
        if (removedBeans.size() == 0) {
            return false;
        }

        // 删除TaskMgr中的任务
        mTaskMgr.removeTasksById(tasksIds);
        // 删除内存中的数据
        mAllDownloadData.deleteAllById(tasksIds);
        // 删除本地文件
        IDeleteFileListener<B> listener = new IDeleteFileListener<B>() {
            @Override
            public void deleteSuccess(List<B> beans) {
                // 本地视频文件删除完毕之后，删除数据库
                saveToPersistence(beans, PersistenceType.DELETE,null);
                // 回调
                Message msg = mHandler.obtainMessage(LISTENER_ON_DELETE);
                msg.obj = beans;
                msg.arg1 = 1;
                mHandler.sendMessage(msg);
            }

            @Override
            public void deleteFailed(List<B> beans) {
                DebugLog.log(TAG, "delete file failed");
            }
        };
        //删除本地文件之前，先将需要删除的记录needdel字段标记为"1"
        DebugLog.log(TAG, "将需要删除的记录needdel字段标记为1");
        setDeleteFlag(removedBeans);
        DebugLog.log(TAG, "更新数据库");
        saveToPersistence(removedBeans, PersistenceType.UPDATE, null);
        DebugLog.log(TAG, "删除本地文件");
        deleteLocalFile(removedBeans, listener);
        return true;
    }

    @Override
    public final boolean deleteDownloadTask(String taskid) {

        DebugLog.log(TAG, "###deleteDownloadTask(), taskid:" + taskid);
        if (taskid == null) {
            return false;
        }

        // 筛选出被删除的对象
        final List<B> removedBeans = new ArrayList<B>();
            B bean = mAllDownloadData.getById(taskid);
            if (bean != null) {
                removedBeans.add(bean);
            }

        // 如果没有可删除的，直接结束
        if (removedBeans.size() == 0) {
            return false;
        }
        ArrayList<String> tasksIds = new ArrayList<String>();
        tasksIds.add(taskid);

        // 删除TaskMgr中的任务
        mTaskMgr.removeTasksById(tasksIds);
        // 删除内存中的数据
        mAllDownloadData.deleteAllById(tasksIds);


        return true;
    }

    @Override
    public final void clearAllDownloadTask() {
        DebugLog.log(TAG, "###clearAllDownloadTask()");
        if (mAllDownloadData.size() == 0) {
            return;
        }

        final List<B> removedBeans = mAllDownloadData.copyAll();
        // 删除TaskMgr中的任务
        mTaskMgr.stopAndReset();
        // 删除内存中的数据
        mAllDownloadData.clear();
        // 删除本地文件
        IDeleteFileListener<B> listener = new IDeleteFileListener<B>() {
            @Override
            public void deleteSuccess(List<B> beans) {
                // 本地视频文件删除完毕之后，删除数据库
                saveToPersistence(beans, PersistenceType.DELETE, null);
                // 回调
                Message msg = mHandler.obtainMessage(LISTENER_ON_DELETE);
                msg.obj = beans;
                mHandler.sendMessage(msg);
            }

            @Override
            public void deleteFailed(List<B> beans) {

            }
        };
        //删除本地文件之前，先将需要删除的记录needdel字段标记为"1"
        setDeleteFlag(removedBeans);
        saveToPersistence(removedBeans, PersistenceType.UPDATE, null);
        deleteLocalFile(removedBeans, listener);
    }

    @Override
    public final boolean updateDownloadTasks(List<String> tasksIds, int key, Object value) {
        DebugLog.log(TAG, "###updateDownloadTasks(), tasksIds:" + tasksIds
                + ", key:" + key + ", value:" + value);
        if (mAllDownloadData.size() == 0 || tasksIds == null ||
                tasksIds.size() == 0 || value == null) {
            return false;
        }

        // 筛选出被更新的对象
        List<B> updatedBeans = new ArrayList<B>();
        for (String taskId : tasksIds) {
            B bean = mAllDownloadData.getById(taskId);
            if (bean != null) {
                updatedBeans.add(bean);
            }
        }
        // 如果没有可更新的，直接结束
        if (updatedBeans.size() == 0) {
            return false;
        }

        // 子类实现的更新逻辑
        if (!onUpdateDownloadTask(updatedBeans, key, value)) {
            return false;
        }

        // 更新内存数据成功，更新进持久层
        saveToPersistence(updatedBeans, PersistenceType.UPDATE, null);
        // 回调
        Message msg = mHandler.obtainMessage(LISTENER_ON_UPDATE);
        msg.obj = updatedBeans;
        msg.arg1 = key;
        mHandler.sendMessage(msg);
        return true;
    }

    @Override
    public boolean updateDownloadTasks(List<B> tasks, int key) {
        DebugLog.log(TAG, "###updateDownloadTasks(List<B> tasks)");
        if (mAllDownloadData.size() == 0 || tasks == null || tasks.size() == 0) {
            return false;
        }

        List<B> updateBeans = new ArrayList<B>();
        for (B bean : tasks) {
            if (mAllDownloadData.contains(bean)) {
                updateBeans.add(mAllDownloadData.getById(bean.getId()));
            }
        }

        if (updateBeans.size() == 0) {
            return false;
        }

        // 子类实现的更新逻辑
        if (!onUpdateDownloadTask(updateBeans, key)) {
            return false;
        }
        // 更新内存数据成功，更新进持久层
        saveToPersistence(updateBeans, PersistenceType.UPDATE, null);
        // 回调
        Message msg = mHandler.obtainMessage(LISTENER_ON_UPDATE);
        msg.obj = updateBeans;
        mHandler.sendMessage(msg);
        return true;
    }

    @Override
    public List<B> getAllDownloadTask() {
        DebugLog.log(TAG, "###getAllDownloadTask()");
        return mAllDownloadData.copyAll();
    }


    ///////////////////注册监听，消息转发//////////////////////////////////////////////
    @Override
    public void registerListener(IQiyiDownloaderListener<B> listener) {
        DebugLog.log(TAG, "###registerListener(), listener:" + listener);
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    @Override
    public void unregisterListener(IQiyiDownloaderListener<B> listener) {
        DebugLog.log(TAG, "###unregisterListener(), listener:" + listener);
        mListeners.remove(listener);
    }

    /**
     * 内部监听，转发TaskMgr的回调给外部处理
     */
    protected class InnerListener implements IDownloadTaskListener<B> {

        @Override
        public void onAdd(B task) {
            DebugLog.log(TAG, "###onAdd(), task:" + task);
        }

        @Override
        public void onAddAll(List<B> tasks) {
            DebugLog.log(TAG, "###onAddAll(), task:" + tasks);
        }

        @Override
        public void onRemove(B task) {
            DebugLog.log(TAG, "###onRemove(), task:" + task);
        }

        @Override
        public void onRemoveAll(final List<B> tasks) {
            DebugLog.log(TAG, "###onRemoveAll(), task:" + tasks);
        }

        @Override
        public void onNoDowningTask() {
            DebugLog.log(TAG, "###onNoDowningTask()");
            // 转发回调s
           // mHandler.removeMessages(LISTENER_ON_STOP_ALL);
            mHandler.obtainMessage(LISTENER_ON_STOP_ALL).sendToTarget();
        }

        @Override
        public void onFinishAll() {
            DebugLog.log(TAG, "###onFinishAll()");
            // 转发回调
           // mHandler.removeMessages(LISTENER_ON_FINISH_ALL);
            mHandler.obtainMessage(LISTENER_ON_FINISH_ALL).sendToTarget();
        }

        @Override
        public void onStart(B task) {
            DebugLog.log(TAG, "###onStart(), task:" + task.getId());
            //mHandler.removeMessages(LISTENER_ON_START);
            Message msg = mHandler.obtainMessage(LISTENER_ON_START);
            try {
                msg.obj = task.clone();
            } catch (CloneNotSupportedException e) {
                msg.obj = task;
            }
            mHandler.sendMessage(msg);
        }

        @Override
        public void onPause(B task) {
            DebugLog.log(TAG, "###onPause(), task:" + task);
           // mHandler.removeMessages(LISTENER_ON_PAUSE);
            Message msg = mHandler.obtainMessage(LISTENER_ON_PAUSE);
            try {
                msg.obj = task.clone();
            } catch (CloneNotSupportedException e) {
                msg.obj = task;
            }
            mHandler.sendMessage(msg);

        }

        @Override
        public void onPauseAll() {
            DebugLog.log(TAG, "###onPauseAll()");
           // mHandler.removeMessages(LISTENER_ON_PAUSE_ALL);
            mHandler.obtainMessage(LISTENER_ON_PAUSE_ALL).sendToTarget();

        }

        @Override
        public void onDoing(B task, long completeSize) {
            DebugLog.log(TAG, "###onDoing(), task:" + task + ", completeSize:" + completeSize);
            //mHandler.removeMessages(LISTENER_ON_DOWNLOADING);
            Message msg = mHandler.obtainMessage(LISTENER_ON_DOWNLOADING);
            msg.obj = task;
            mHandler.sendMessage(msg);

        }

        @Override
        public void onComplete(B task) {
            DebugLog.log(TAG, "###onComplete(), task Status:" + task.getStatus());
            //mHandler.removeMessages(LISTENER_ON_COMPLETE);
            Message msg = mHandler.obtainMessage(LISTENER_ON_COMPLETE);
            try {
                msg.obj = task.clone();
            } catch (CloneNotSupportedException e) {
                msg.obj = task;
            }
            mHandler.sendMessage(msg);
        }

        @Override
        public void onError(B task, String errorCode) {
            DebugLog.log(TAG, "###onError(), task:" + task + ", errorCode:" + errorCode);
            //mHandler.removeMessages(LISTENER_ON_ERROR);
            task.setErrorCode(errorCode);
            Message msg = mHandler.obtainMessage(LISTENER_ON_ERROR);
            try {
                msg.obj = task.clone();
            } catch (CloneNotSupportedException e) {
                ExceptionUtils.printStackTrace(e);
                msg.obj = task;
            }
            mHandler.sendMessage(msg);
        }

        @Override
        public void onPrepare() {
            DebugLog.log(TAG, "###onPrepare()");
            //直接使用mAllDownloadData就可以
            //mHandler.removeMessages(LISTENER_ON_PREPARE);
            mHandler.obtainMessage(LISTENER_ON_PREPARE).sendToTarget();
        }

        @Override
        public void onSDFull(B task) {
            DebugLog.log(TAG, "###onSDFull()");
          //  mHandler.removeMessages(LISTENER_ON_SDFULL);
            Message msg = mHandler.obtainMessage(LISTENER_ON_SDFULL);
            try {
                msg.obj = task.clone();
            } catch (CloneNotSupportedException e) {
                msg.obj = task;
            }
            mHandler.sendMessage(msg);
        }
    }

    /**
     * 内部handler，用于调度到UI线程再回调到外部
     */
    private class InnerHandler extends Handler {

        public InnerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LISTENER_ON_LOAD:
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onLoad();
                    }
                    break;
                case LISTENER_ON_ADD:
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onAdd((List<B>) msg.obj);
                    }
                    break;
                case LISTENER_ON_DELETE:
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onDelete((List<B>) msg.obj, msg.arg1);
                    }
                    break;
                case LISTENER_ON_UPDATE:
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onUpdate((List<B>) msg.obj, msg.arg1);
                    }
                    break;
                case LISTENER_ON_START:
                    B start_task = (B) msg.obj;
                    List<B> start_tasks = new ArrayList<B>();
                    start_tasks.add(start_task);
                    saveToPersistence(start_tasks, PersistenceType.UPDATE, null);
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onStart(start_task);
                    }
                    break;
                case LISTENER_ON_PAUSE:
                    B task_pause = (B) msg.obj;
                    List<B> pause_tasks = new ArrayList<B>();
                    pause_tasks.add(task_pause);
                    saveToPersistence(pause_tasks, PersistenceType.UPDATE, null);
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onPause(task_pause);
                    }
                    break;
                case LISTENER_ON_PAUSE_ALL:
                    int size = mAllDownloadData.size();
                    for (int i = 0; i < size; i++) {
                        B pauseTask = mAllDownloadData.get(i);
                        if (pauseTask != null && pauseTask.getStatus() != XTaskBean.STATUS_DONE &&
                                pauseTask.getStatus() != XTaskBean.STATUS_ERROR
                                ) {
                            pauseTask.setStatus(XTaskBean.STATUS_DEFAULT);
                        }

                    }
                    saveToPersistence(mAllDownloadData.getAll(), PersistenceType.UPDATE, null);
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onPauseAll();
                    }
                    break;
                case LISTENER_ON_STOP_ALL:
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onNoDowningTask();
                    }
                    break;
                case LISTENER_ON_DOWNLOADING:
                    B down_task = (B) msg.obj;
                    List<B> down_tasks = new ArrayList<B>();
                    down_tasks.add(down_task);
                    saveToPersistence(down_tasks, PersistenceType.UPDATE, null);
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onDownloading(down_task);
                    }
                    break;
                case LISTENER_ON_COMPLETE:
                    B comple_task = (B) msg.obj;
                    List<B> comple_tasks = new ArrayList<B>();
                    comple_tasks.add(comple_task);
                    if (comple_task.getStatus() != XTaskBean.STATUS_DONE) {
                        comple_task.setStatus(XTaskBean.STATUS_DONE);
                    }
                    saveToPersistence(comple_tasks, PersistenceType.UPDATE, null);
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onComplete(comple_task);
                    }
                    break;
                case LISTENER_ON_ERROR:
                    B error_task = (B) msg.obj;
                    List<B> error_tasks = new ArrayList<B>();
                    error_tasks.add(error_task);
                    saveToPersistence(error_tasks, PersistenceType.UPDATE, null);
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onError(error_task);
                    }
                    break;
                case LISTENER_ON_FINISH_ALL:
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onFinishAll();
                    }
                    break;
                case LISTENER_ON_NO_NETWORK:
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onNoNetwork();
                    }
                    break;
                case LISTENER_ON_NETWORK_NOT_WIFI:
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onNetworkNotWifi();
                    }
                    break;
                case LISTENER_ON_NETWORK_WIFI:
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onNetworkWifi();
                    }
                    break;
                case LISTENER_ON_MOUNTED_SD_CARD:
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onMountedSdCard();
                    }
                    break;
                case LISTENER_ON_UNMOUNTED_SD_CARD:
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onUnmountedSdCard(msg.arg1 != 0);
                    }
                    break;
                case LISTENER_ON_PREPARE:
                    int pre_size = mAllDownloadData.size();
                    for (int i = 0; i < pre_size; i++) {
                        B pre_task = mAllDownloadData.get(i);
                        if (pre_task != null && pre_task.getStatus() != XTaskBean.STATUS_DONE && pre_task.getStatus() != XTaskBean.STATUS_DOING) {
                            pre_task.setStatus(XTaskBean.STATUS_TODO);
                        }
                    }
                    saveToPersistence(mAllDownloadData.getAll(), PersistenceType.UPDATE, null);
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onPrepare();
                    }
                    break;
                case LISTENER_ON_SDFULL:
                    for (IQiyiDownloaderListener<B> listener : mListeners) {
                        listener.onSDFull((B) msg.obj);
                    }
                    break;

            }
        }
    }


    ////////////////////处理网络，存储卡逻辑////////////////////////////////
    @Override
    public void handleNetWorkChange(int netType) {
        switch (netType) {
            case NET_MOBILE:
                netWorkToMobile();
                break;
            case NET_WIFI:
                netWorkToWifi();
                break;
            case NET_OFF:
                netWorkOff();
                break;
        }
    }

    /**
     * 网络断开会调用此方法。
     * 此方法提供了默认的处理逻辑，子类可以根据需要改写此方法
     */
    protected void netWorkOff() {
        DebugLog.log(TAG, "BaseQiyiDownloader-->netWorkOff");
        mTaskMgr.pause();
        mTaskMgr.setAutoRunning(false);
        mHandler.obtainMessage(LISTENER_ON_NO_NETWORK).sendToTarget();
    }

    /**
     * 网络连接上wifi网络会调用此方法
     * 此方法提供了默认的处理逻辑，子类可以根据需要改写此方法
     */
    protected void netWorkToWifi() {
        DebugLog.log(TAG, "BaseQiyiDownloader-->netWorkToWifi");
        mTaskMgr.setAutoRunning(true);
        mTaskMgr.start();
        mHandler.obtainMessage(LISTENER_ON_NETWORK_WIFI).sendToTarget();
    }

    /**
     * 网络连接上蜂窝网络会调用此方法
     * 此方法提供了默认的处理逻辑，子类可以根据需要改写此方法
     */
    protected void netWorkToMobile() {
        DebugLog.log(TAG, "BaseQiyiDownloader-->netWorkToMobile");
        mTaskMgr.pause();
        mTaskMgr.setAutoRunning(false);
        mHandler.obtainMessage(LISTENER_ON_NETWORK_NOT_WIFI).sendToTarget();
    }

    @Override
    public void handleSdCardChange(int sdCardType) {
        switch (sdCardType) {
            case SD_CARD_INSERT:
                sdCardInsert();
                break;
            case SD_CARD_REMOVE:
                sdCardRemove();
                break;
        }
    }

    protected void sdCardRemove() {
        // 检查当前下载的任务的路径是否存在，如果不存在，则暂停该任务
        TaskBean<B> task = mTaskMgr.getRunningTask();
        if (task != null) {
            B bean = task.mDownloadTask == null ? null : task.mDownloadTask.getBean();
            if (!(bean == null || StorageCheckor.checkSpaceEnough(bean.getSaveDir()))) {
                // 回调
                Message msg = mHandler.obtainMessage(LISTENER_ON_UNMOUNTED_SD_CARD);
                msg.arg1 = 1;// 标明暂停了下载
                mHandler.sendMessage(msg);
                // 暂停下载
                mTaskMgr.pause();
            } else {
                // 回调
                Message msg = mHandler.obtainMessage(LISTENER_ON_UNMOUNTED_SD_CARD);
                msg.arg1 = 0;// 标明下载没被暂停
                mHandler.sendMessage(msg);
            }
        }

    }

    protected void sdCardInsert() {
        // 回调
        mHandler.obtainMessage(LISTENER_ON_MOUNTED_SD_CARD).sendToTarget();
        // 继续因为拔卡而暂停的任务（这里必须检查网络）
        if (NetWorkTypeUtils.getNetworkStatus(mContext) == NetworkStatus.WIFI) {
            mTaskMgr.start();
        }
    }


    //////////////////////下载其他业务/////////////////////////////////////////////////

    @Override
    public B findDownloadTaskById(String taskId) {
        DebugLog.log(TAG, "findDownloadTaskById");
        B bean = null;
        if (!TextUtils.isEmpty(taskId)) {
            bean = mAllDownloadData.getById(taskId);
        } else {
            DebugLog.log(TAG, "taskId is empty,can not find download task");
        }
        return bean;
    }

    @Override
    public void stopAndClear() {
        DebugLog.log(TAG, "###stopAndClear()");
        // 清空任务管理器
        mTaskMgr.stopAndReset();
        // 清空内存
        mAllDownloadData.clear();
        hasLoaded = false;
    }

    @Override
    public void setAutoRunning(boolean auto) {
        DebugLog.log(TAG, "###setAutoRunning(), auto:" + auto);
        mTaskMgr.setAutoRunning(auto);
    }

    @Override
    public boolean hasTaskRunning() {
        return mTaskMgr.hasTaskRunning();
    }

    @Override
    public B getRunningObject() {
        TaskBean<B> runningTask = mTaskMgr.getRunningTask();
        B bean = null;
        if (runningTask != null) {
            bean = mAllDownloadData.getById(runningTask.getId());
        }
        return bean;
    }

    public void setTaskStatus(B task, int status) {
        DebugLog.log(TAG, "###setTaskStatus()");
        mTaskMgr.setTaskStatus(task, status);
    }



    /**
     * 在添加任务之前进行重复检查
     *
     * @param tasks
     * @return
     */
    public List<B> onPreAddDownloadTask(List<B> tasks) {
        DebugLog.log(TAG, "onPreAddDownloadTask");
        if (tasks == null || tasks.size() == 0) {
            return null;
        }
        // 去重判断
        final List<B> addBeans = new ArrayList<B>();
        for (B bean : tasks) {
            if (!mAllDownloadData.contains(bean)) {
                DebugLog.e(TAG, "add download task");
                addBeans.add(bean);
            } else {
                DebugLog.e(TAG, "duplicated download task>>" + bean.getId());
            }
        }
        // 如果没有可添加的，直接结束
        if (addBeans.size() == 0) {
            return null;
        }

        return addBeans;

    }

    ///////////////////接口 + 抽象方法//////////////////////////////////////

    /**
     * 从持久层加载后的回调
     */
    protected interface ILoadFromPersistenceListener<B> {
        void loadSuccess(List<B> beans);

        void loadFail();
    }

    //从持久层添加后回调
    protected interface ISavePersistenceListener<B> {

        void addSuccess(List<B> beans);

        void addFail();
    }

    /**
     * 从持久层删除后的回调
     */
    protected interface IDeleteFileListener<B> {
        /**
         * 删除成功的回调
         * @param beans
         */
        void deleteSuccess(List<B> beans);

        /**
         * 删除失败的回调
         * @param beans
         */
        void deleteFailed(List<B> beans);
    }

    /**
     * 从持久层加载数据到内存。
     * 此方法有可能在UI线程执行，请实现时将相关IO操作放到异步线程中。
     */
    protected abstract void loadFromPersistence(ILoadFromPersistenceListener<B> listener);

    /**
     * 从内存同步到持久层。
     * 此方法有可能在UI线程执行，请实现时将相关IO操作放到异步线程中。
     */
    protected abstract boolean saveToPersistence(List<B> beans, PersistenceType dbType, ISavePersistenceListener<B> listener);

    /**
     * 删除视频文件。
     * 此方法有可能在UI线程执行，请实现时将相关IO操作放到异步线程中。
     *
     * @return 如果删除结束后会回调listener，返回true；否则返回false。
     */
    protected abstract boolean deleteLocalFile(List<B> beans, IDeleteFileListener<B> listener);

    /**
     * 同步删除视频
     *
     * @param beans
     * @param listener
     * @return
     */
    protected abstract boolean deleteLocalFileSync(List<B> beans, IDeleteFileListener<B> listener);

    /**
     * /**
     * 子类自定义的更新下载数据的逻辑(只负责更新内存中的对象数据)。
     */
    protected abstract boolean onUpdateDownloadTask(List<B> beans, int key, Object value);

    /**
     * 子类自定义的跟新下载数据的逻辑
     *
     * @param beans 已经跟新后的下载对象列表
     * @return
     */
    protected abstract boolean onUpdateDownloadTask(List<B> beans, int key);

    /**
     * 在删除本地文件前先将需要删除的记录的needdel字段置为 1
     *
     * @param beans
     */
    protected abstract void setDeleteFlag(List<B> beans);

    /**
     * 设置任务调度器，用于计算任务优先级
     *
     * @param schedule
     */
    protected abstract void setTaskSchedule(ITaskSchedule<B> schedule);
}
