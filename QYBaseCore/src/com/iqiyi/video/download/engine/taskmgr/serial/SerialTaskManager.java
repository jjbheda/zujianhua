package com.iqiyi.video.download.engine.taskmgr.serial;

import android.text.TextUtils;

import com.iqiyi.video.download.engine.task.ITaskExecutor;
import com.iqiyi.video.download.engine.task.ITaskListener;
import com.iqiyi.video.download.engine.task.ITaskSchedule;
import com.iqiyi.video.download.engine.task.XBaseTaskExecutor;
import com.iqiyi.video.download.engine.taskmgr.IDownloadTaskCreator;
import com.iqiyi.video.download.engine.taskmgr.IDownloadTaskListener;
import com.iqiyi.video.download.filedownload.FileDownloadHelper;
import com.iqiyi.video.download.filedownload.TaskBean;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.video.module.download.exbean.XTaskBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 线性执行器，实现ISerialTaskManager接口的子类。
 * 封装了线性执行、增删任务、启动、恢复、暂停等操作。
 * 执行器内部由两个队列组成：等待队列、执行队列(一个任务)。
 * 1.每个时刻，最多只有一个任务正在执行；
 * 2.每个任务都在TODO,DOING,DONE,ERROR四个状态间转换；
 * 3.如果任务从DOING到ERROR，回调SerialMgr时，
 *   会将该任务丢弃，继续执行下一个；
 * 4.如果任务从DOING到TODO，回调SerialMgr时，
 *   会将该任务重新添加进等待队列，不继续执行；
 * 5.所有的下载任务均在开始下载时创建，当用户暂停下载时，
 * 下载任务置空
 *
 *  疑问：1、start(),start(String taskId)中不需要checkSDFull
 *
 */
public class SerialTaskManager<B extends XTaskBean> implements ISerialTaskManager<B> {

    public static final String TAG = "SerialTaskManager";

    protected volatile boolean mIsWorking;// 标识运行状态

    protected volatile boolean mAuto;// 标识是否自动执行

    protected volatile TaskBean<B> mCurrentExecuted;// 当前正在运行的任务

    protected LinkedList<TaskBean<B>> mTobeExecuted;// 待执行的任务队列

    protected SerialTaskComparator mInnerComparator;// 实际用来排序的比较器

    protected CopyOnWriteArrayList<IDownloadTaskListener<B>> mListeners;// 外部监听者

    protected ITaskListener<B> mInnerTaskListener;// 内部管理器对每个Task的监听

    protected IDownloadTaskCreator<B> mCreater;//下载任务创建器

    protected ITaskSchedule<B> schedule;


    /**
     * 线性任务管理器
     */
    public SerialTaskManager() {

        mCurrentExecuted = null;

        mTobeExecuted = new LinkedList<TaskBean<B>>();

        mInnerComparator = new SerialTaskComparator();

        mListeners = new CopyOnWriteArrayList<IDownloadTaskListener<B>>();

        mIsWorking = false;

        mAuto = true;

        mInnerTaskListener = new ITaskListener<B>() {
            @Override
            public void onStart(B task) {
                TaskBean<B> taskExecutor = getTaskById(task.getId());
                if (taskExecutor != null) {
                    taskExecutor.setStatus(task.getStatus());
                }

                for (IDownloadTaskListener<B> listener : mListeners) {
                    if (listener != null) {
                        listener.onStart(task);
                    }
                }
            }

            @Override
            public void onPause(B task) {
                TaskBean<B> taskExecutor = getTaskById(task.getId());
                if (taskExecutor != null) {
                    taskExecutor.setStatus(task.getStatus());
                }
                for (IDownloadTaskListener<B> listener : mListeners) {
                    if (listener != null) {
                        listener.onPause(task);
                    }
                }
            }

            @Override
            public void onAbort(B task) {
            }

            @Override
            public void onDoing(B task, long completeSize) {
                TaskBean<B> taskExecutor = getTaskById(task.getId());
                if (taskExecutor != null) {
                    taskExecutor.setStatus(task.getStatus());
                }
                if (!FileDownloadHelper.isSDFull(task)) {
                    //sd卡没有满
                    for (IDownloadTaskListener<B> listener : mListeners) {
                        if (listener != null) {
                            listener.onDoing(task, completeSize);
                        }

                    }
                } else {
                    pause();
                    //sd卡已经满了
                    for (IDownloadTaskListener<B> listener : mListeners) {
                        if (listener != null) {
                            listener.onSDFull(task);
                        }
                    }
                }

            }

            @Override
            public void onComplete(B task) {
                DebugLog.d(TAG, "XTaskListener->onComplete");
                TaskBean<B> taskExecutor = getTaskById(task.getId());
                if (taskExecutor != null) {
                    taskExecutor.setStatus(XTaskBean.STATUS_DONE);
                }
                for (IDownloadTaskListener<B> listener : mListeners) {
                    if (task.getStatus() != XTaskBean.STATUS_DONE) {
                        task.setStatus(XTaskBean.STATUS_DONE);
                    }
                    if (listener != null) {
                        listener.onComplete(task);
                    }
                }
                if (taskExecutor != null)
                    notifyTaskFinished(taskExecutor, false);
            }


            @Override
            public void onError(B task, String errorCode, boolean retry) {
                TaskBean<B> taskExecutor = getTaskById(task.getId());
                if (taskExecutor != null) {
                    taskExecutor.setStatus(task.getStatus());
                }

                //出错，则将当前任务置空
                if (mCurrentExecuted != null && !task.autoNextTaskWhenError()) {
                    mCurrentExecuted.mDownloadTask = null;
                    mTobeExecuted.addFirst(mCurrentExecuted);
                    mCurrentExecuted = null;
                }


                for (IDownloadTaskListener<B> listener : mListeners) {
                    if (listener != null) {
                        listener.onError(task, errorCode);
                    }

                }
                if (task.autoNextTaskWhenError()) {
                    DebugLog.d(TAG, "Task error and auto Next Task!! downloadWay:" + task.getDownWay());
                    notifyTaskFinished(taskExecutor, retry);
                } else {
                    DebugLog.d(TAG, "Task error not auto Next Task!! downloadWay:" + task.getDownWay());
                }
            }
        };
    }

    /**
     * 获取任务Id
     * @param task 任务
     * @return
     */
    @Override
    public String getTaskId(TaskBean<B> task) {
        String taskId = task != null ? task.getId() : "";
        return taskId;
    }

    /**
     * 根据id获取TaskBean
     * @param id
     * @return
     */
    @Override
    public TaskBean<B> getTaskById(String id) {

        if (TextUtils.isEmpty(id)) {
            return null;
        }

        if (mCurrentExecuted != null
                && getTaskId(mCurrentExecuted) != null
                && id.equals(getTaskId(mCurrentExecuted))) {

            return mCurrentExecuted;

        }

        for (TaskBean<B> task : mTobeExecuted) {
            if (task != null
                    && getTaskId(task) != null
                    && id.equals(getTaskId(task))) {

                return task;

            }
        }

        return null;
    }

    /**
     * 添加一个任务
     * @param task 任务
     * @return
     */
    @Override
    public synchronized boolean addTask(TaskBean<B> task) {

        //任务去重判断
        if (getTaskById(getTaskId(task)) != null) {

            DebugLog.d(TAG, "addTask>>task" + getTaskId(task) + " is already exist!");

            return false;

        }
        task.setTaskMgr(this);
        mTobeExecuted.offer(task);
        return true;
    }

    /**
     * 添加多个任务
     * @param tasks 任务
     */
    @Override
    public synchronized void addTasks(List<TaskBean<B>> tasks) {

        if (tasks == null || tasks.size() == 0)
            return;

        for (TaskBean<B> task : tasks) {

            if (task == null) {

                continue;

            }

            //任务去重判断
            if (getTaskById(getTaskId(task)) != null) {

                DebugLog.d(TAG, "addTasks>>task" + getTaskId(task) + " is already exist!");

                continue;
            }

            task.setTaskMgr(this);

            mTobeExecuted.offer(task);
        }
    }

    /**
     * 删除单个任务
     *
     * @param task 要删除的任务
     */
    @Override
    public synchronized void removeTask(TaskBean<B> task) {

        if (task == null)
            return;

        if (task.mDownloadTask != null) {
            DebugLog.d(TAG, "removeTask>>mDownloadTask is not null and abort it");
            // 终止当前任务
            task.mDownloadTask.abort();
            task.mDownloadTask = null;
        }

        if (mCurrentExecuted == task) {
            // 如果要删除的任务是当前的任务
            DebugLog.d(TAG, "removeTask>>removedTask is currentExecuted");
            mCurrentExecuted = null;
        } else {
            mTobeExecuted.remove(task);
        }

        //如果删除的是当前执行的任务，则自动寻找下一个任务并启动下一个任务
        if (mCurrentExecuted == null) {

            DebugLog.d(TAG, "removeTask>>currentExecuted has removed!!");

            if (mAuto) {

                DebugLog.d(TAG, "removeTask>>contains currentExecuted and mAuto is true!");

                if (!start()) {

                    mIsWorking = false;

                    for (IDownloadTaskListener<B> listener : mListeners) {
                        if (listener != null) {
                            listener.onNoDowningTask();
                        }
                    }

                    DebugLog.d(TAG, "removeTask>>contains currentExecuted auto next task fail!");

                } else {

                    DebugLog.d(TAG, "removeTask>>contains currentExecuted auto next task success!");

                }
            }
        }
    }

    /**
     * 通过taskid删除任务
     *
     * @param taskId 任务的唯一Id
     */
    @Override
    public synchronized void removeTaskById(String taskId) {

        removeTask(getTaskById(taskId));

    }

    /**
     * 删除多个任务
     *
     * @param tasks  待删除的任务对象列表
     */
    @Override
    public synchronized void removeTasks(List<TaskBean<B>> tasks) {

        if (tasks == null || tasks.size() == 0)
            return;

        for (TaskBean<B> task : tasks) {

            if (task == null) {
                continue;
            }

            if (task.mDownloadTask != null) {
                DebugLog.d(TAG, "removeTasks>>mDownloadTask is not null and abort it");
                task.mDownloadTask.abort();
                task.mDownloadTask = null;
            }

            if (mCurrentExecuted == task) {
                //如果要删除的任务是当前的任务
                mCurrentExecuted = null;
                DebugLog.d(TAG,"removeTasks>>mDownloadTask equals currentExecuted");
            } else {
                //从等待队列中删除
                mTobeExecuted.remove(task);
            }
        }


        //若删除的多个任务中，有正在下载的任务，则自动寻找下一个任务并启动它
        if (mCurrentExecuted == null) {

            DebugLog.d(TAG, "removeTasks>>currentExecuted has removed");

            if (mAuto) {

                DebugLog.d(TAG, "removeTasks>>contains currentExecuted and mAuto is true!");

                if (!start()) {

                    mIsWorking = false;

                    for (IDownloadTaskListener<B> listener : mListeners) {

                        if (listener != null) {
                            listener.onNoDowningTask();
                        }

                    }
                    DebugLog.d(TAG, "removeTasks>>contains currentExecuted auto next task fail!");

                } else {
                    DebugLog.d(TAG, "removeTasks>>contains currentExecuted auto next task success!");
                }
            }

        }
    }

    /**
     * 通过taskid删除多个任务
     *
     * @param taskIds 待删除的任务Id列表
     */
    @Override
    public synchronized void removeTasksById(List<String> taskIds) {

        if (taskIds == null || taskIds.size() == 0)
            return;

        List<TaskBean<B>> tasks = new ArrayList<TaskBean<B>>();
        for (String taskId : taskIds) {
            TaskBean<B> task = getTaskById(taskId);
            if (task != null)
                tasks.add(task);
        }

        removeTasks(tasks);
    }



    /**
     * 启动任务，不指定任务，线性任务管理器自动寻找等待队列中的第一个任务
     *
     * @return
     */
    @Override
    public synchronized boolean start() {

        // 如果当前任务为空，尝试从等待队列中选择一个任务
        if (mCurrentExecuted == null) {

            mCurrentExecuted = findNextTask();

        }
        // 如果当前任务还是为空，则什么都不做
        if (mCurrentExecuted == null) {

            DebugLog.d(TAG, "start()>>mCurrentExecuted is null,do nothing");

            return false;

        }

        if (mCurrentExecuted.mDownloadTask == null) {
            // 尝试启动任务。启动任务之前，务必判断DownloadTask是否null

            DebugLog.d(TAG, "start()>>mDownloadTask is null and create DownloadTask");

            XBaseTaskExecutor<B> executor = mCreater.createDownloadTask(mCurrentExecuted.getId());

            if (executor != null) {

                mCurrentExecuted.mDownloadTask = executor;

                mCurrentExecuted.mDownloadTask.setListener(mInnerTaskListener);

            }

        }

        if (mCurrentExecuted.mDownloadTask == null) {

            DebugLog.d(TAG, "start()>>mDownloadTask create fail ,mDownlaodTask　is null");

            return false;

        }

        DebugLog.d(TAG, "start()>>taskId = " + mCurrentExecuted.getId());

        B bean = mCurrentExecuted.mDownloadTask.getBean();

        if (checkSDFull(bean)) {

            DebugLog.d(TAG, "start()>>sdcard is full" );

            return false;

        }

        if (mCurrentExecuted !=null && mCurrentExecuted.mDownloadTask!=null && ITaskExecutor.TASK_START_SUCCESS != mCurrentExecuted.mDownloadTask.start() ) {

            DebugLog.d(TAG, "start()>>mDownloadTask start fail");

            return false;

        } else {

            DebugLog.d(TAG, "start()>>mDownloadTask start success");

        }

        mIsWorking = true;

        return true;

    }

    /**
     * 通过指定任务id启动任务
     *
     * @param taskId 任务的唯一Id
     * @return
     */
    @Override
    public synchronized boolean start(String taskId) {

        try {

            TaskBean<B> task = getTaskById(taskId);

            //若指定任务不存在，则创建一个指定任务
            if (task == null) {

                DebugLog.d(TAG, "start(id)>>cannot find the TaskBean = " + taskId);


                if (mCreater != null) {

                    DebugLog.d(TAG, "start(id)>>>mCreater to create TaskBean");

                    task = mCreater.createTaskBean(taskId);

                    if (task == null) {

                        DebugLog.d(TAG, "start(id)>>mCreater create TaskBean fail");

                        return false;

                    } else{

                        DebugLog.d(TAG, "start(id)>>mCreater create TaskBean success");

                    }

                }

                if (task == null) {

                    return false;

                } else {

                    //将创建的TaskBean放入队列，便于下次直接使用
                    addTask(task);

                }
            }

            // 先尝试启动指定任务,在启动任务之前，务必判断DownlaodTask是否为Null
            if (task.mDownloadTask == null) {

                DebugLog.d(TAG, "start(id)>>mDownloadTask is null and create DownloadTask");

                XBaseTaskExecutor<B> executor = mCreater.createDownloadTask(task.getId());
                if (executor != null) {
                    task.mDownloadTask = executor;
                    task.mDownloadTask.setListener(mInnerTaskListener);
                }

            }

            if (task.mDownloadTask == null) {

                DebugLog.d(TAG, "start(id)>>mDownloadTask is create fail,mDownlaodTask　is null");

                return false;
            }

            B bean = task.mDownloadTask.getBean();

            if (checkSDFull(bean)) {
                return false;
            }

            // 如果当前任务不是指定id任务，暂停当前任务，再指定新的当前任务
            if (mCurrentExecuted != task) {

                // 暂停正在执行的当前任务
                if (mCurrentExecuted != null && mCurrentExecuted.mDownloadTask != null) {

                    int result = mCurrentExecuted.mDownloadTask.pause(XTaskBean.STATUS_TODO);

                    if (result != ITaskExecutor.TASK_PAUSE_SUCCESS && result != ITaskExecutor.TASK_PAUSE_ERROR_TASK) {

                        DebugLog.d(TAG, "start(id)>>mCurrentExecuted pause fail!");

                        if (mIsWorking) {
                            DebugLog.d(TAG, "start(id)>>>mCurrentExecuted pause fail mIsWorking is true!");
                            return false;
                        }

                    } else {
                        DebugLog.d(TAG, "start(id)>>mCurrentExecuted pause success!");
                    }

                    mCurrentExecuted.mDownloadTask = null;
                }

                if (mCurrentExecuted != null) {
                    // 当前执行任务，添加回等待队列
                    mTobeExecuted.addFirst(mCurrentExecuted);
                }

                // 指定新的当前任务
                mTobeExecuted.remove(task);

                mCurrentExecuted = task;

            }


            DebugLog.d(TAG, "start(id)>>taskId = " + mCurrentExecuted.getId());

            //此处传入STATUS_DEFAULT是为了启动"暂停中"的任务

            if (ITaskExecutor.TASK_START_SUCCESS != mCurrentExecuted.mDownloadTask.start(XTaskBean.STATUS_DEFAULT)) {

                DebugLog.d(TAG, "start(id)>>mDownloadTask start fail!");

                return false;

            } else {

                DebugLog.d(TAG, "start(id)>>mDownloadTask start success!");

            }

            mIsWorking = true;

            return true;

        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }

        return false;
    }

    /**
     * 暂停任务
     *
     * @return
     */
    @Override
    public synchronized boolean pause() {

        try {

            if (mCurrentExecuted == null || mCurrentExecuted.mDownloadTask == null) {

                DebugLog.d(TAG, "pause()>>no current excuted task,do nothing");

                return false;
            }

            // 尝试暂停任务
            int result = mCurrentExecuted.mDownloadTask.pause();

            if (result != ITaskExecutor.TASK_PAUSE_SUCCESS && result != ITaskExecutor.TASK_PAUSE_ERROR_TASK) {

                DebugLog.d(TAG, "pause()>>mDownloadTask pause fail!");

                return false;

            }

            mCurrentExecuted.mDownloadTask = null;

            DebugLog.d(TAG, "pause()>>mDownloadTask pause success!");

            mIsWorking = false;

            for (IDownloadTaskListener<B> listener : mListeners) {

                if (listener != null) {

                    listener.onNoDowningTask();

                }

            }

            return true;

        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }

        return false;
    }

    @Override
    public synchronized boolean pause(String taskId) {

        try {

            if (mCurrentExecuted == null) {

                DebugLog.d(TAG, "pause(id)>>mCurrentExecuted is null ,do nothing");

                return false;

            }

            TaskBean<B> task = getTaskById(taskId);

            // 如果指定Id的任务不存在，或不在执行队列中，返回false
            if (task == null || mCurrentExecuted != task || mCurrentExecuted.mDownloadTask == null) {

                DebugLog.d(TAG, "pause(id)>>task is not currentExecuted or mDownlaodTask is null");

                return false;

            }

            // 如果指定Id的任务存在，且在运行队列中，暂停该任务
            int result = mCurrentExecuted.mDownloadTask.pause();

            //不是 暂停成功或者暂停处于Error状态的任务
            if (result != ITaskExecutor.TASK_PAUSE_SUCCESS && result != ITaskExecutor.TASK_PAUSE_ERROR_TASK) {

                DebugLog.d(TAG, "pause(id)>>pause currentExecuted task fail!");

                return false;

            }

            DebugLog.d(TAG, "pause(id)>>pause currentExecuted success!");

            mCurrentExecuted.mDownloadTask = null;

            mIsWorking = false;

            for (IDownloadTaskListener<B> listener : mListeners) {
                if (listener != null) {
                    listener.onNoDowningTask();
                }
            }

            return true;

        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }

        return false;
    }


    @Override
    public synchronized boolean stop() {

        if (mCurrentExecuted == null) {
            DebugLog.d(TAG, "stop()>>mCurrentExecuted is null");
            return false;
        }

        if (mCurrentExecuted.mDownloadTask == null) {
            DebugLog.d(TAG, "stop()>>mDownloadTask is null");
            return false;
        }

        // 尝试暂停任务
        int result = mCurrentExecuted.mDownloadTask.pause(XTaskBean.STATUS_DEFAULT);

        if (result != ITaskExecutor.TASK_PAUSE_SUCCESS && result != ITaskExecutor.TASK_PAUSE_ERROR_TASK) {

            DebugLog.d(TAG, "stop()>>stop currentExecuted fail!");

            return false;

        }

        DebugLog.d(TAG, "stop()>>stop currentExecuted success!");

        mCurrentExecuted.mDownloadTask = null;

        mTobeExecuted.offer(mCurrentExecuted);

        mCurrentExecuted = null;

        mIsWorking = false;

        for (IDownloadTaskListener<B> listener : mListeners) {
            if (listener != null) {
                listener.onNoDowningTask();
            }
        }

        return true;
    }

    /**
     * 通过指定任务id暂停任务
     * @param taskId 任务的唯一Id
     * @return
     */
    @Override
    public synchronized boolean stop(String taskId) {

        TaskBean<B> task = getTaskById(taskId);

        // 如果指定Id的任务不存在，或不在执行队列中，返回false
        if (task == null) {
            DebugLog.d(TAG, "stop(id)>>cannot find the task!");
            return false;
        }

        if (mCurrentExecuted != task) {
            DebugLog.d(TAG, "stop(id)>>task not equals mCurrentExcuted");
            return false;
        }

        if (task.mDownloadTask == null) {
            DebugLog.d(TAG, "stop(id)>>currentExecuted.mDownloadTask is null");
            return false;
        }


        // 如果指定Id的任务存在，且在运行队列中，暂停该任务
        int result = mCurrentExecuted.mDownloadTask.pause(XTaskBean.STATUS_DEFAULT);

        if (result != ITaskExecutor.TASK_PAUSE_SUCCESS && result != ITaskExecutor.TASK_PAUSE_ERROR_TASK) {

            DebugLog.d(TAG, "stop(id)>>stop currentExecuted fail");

            return false;

        }

        DebugLog.d(TAG, "stop(id)>>stop currentExecuted success!");

        mCurrentExecuted.mDownloadTask = null;

        mTobeExecuted.offer(mCurrentExecuted);

        mCurrentExecuted = null;

        if (mAuto) {
            //自动寻找下一个任务
            if (!start()) {
                mIsWorking = false;
                for (IDownloadTaskListener<B> listener : mListeners) {
                    if (listener != null) {
                        listener.onNoDowningTask();
                    }
                }

            }
        }

        return true;
    }

    /**
     * 清空任务队列
     */
    @Override
    public synchronized void stopAndReset() {

        mIsWorking = false;
        // 结束并清空当前任务
        if (mCurrentExecuted != null && mCurrentExecuted.mDownloadTask != null) {

            mCurrentExecuted.mDownloadTask.pause();

            mCurrentExecuted.mDownloadTask = null;

        }

        mCurrentExecuted = null;
        // 清空等待队列中的任务
        mTobeExecuted.clear();

        // 通知监听者
        for (IDownloadTaskListener<B> listener : mListeners) {
            if (listener != null) {
                listener.onNoDowningTask();
            }
        }

    }


    /**
     * 寻找下一个任务。
     * 1.将任务排序，返回第一个是TODO状态的任务(其他状态的任务忽略)
     * 2.如果没有以上的任务，则返回null
     *
     * @return
     */
    protected synchronized TaskBean<B> findNextTask() {

        if (DebugLog.isDebug()) {

            for (TaskBean<B> bean : mTobeExecuted) {
                DebugLog.d(TAG, "findNextTask>>mTobeExecuted taskid = " + bean.getId()
                        + ">>status = " + bean.getStatus());
            }

        }

        if(schedule != null){
            Collections.sort(mTobeExecuted,mInnerComparator);
        }


        TaskBean<B> nextTask = null;// 最终的结果，下一个待执行任务

        for (TaskBean<B> task : mTobeExecuted) {

            // 不是TODO状态的任务，跳过
            if (task.getStatus() != XTaskBean.STATUS_TODO) {
                continue;
            }

            DebugLog.d(TAG, "findNextTask>>nextTask id = " + task.getId());

            nextTask = task;

            break;

        }


        // 如果找到下一个任务，则将其从等待队列中移除
        if (nextTask != null) {

            mTobeExecuted.remove(nextTask);

        }else{

            DebugLog.d(TAG,"findNextTask>>cannot find next task");

        }

        return nextTask;
    }

    /**
     *  自动寻找下一个任务，并启动任务
     *
     * @param task 已结束的task
     * @param addBack 是否添加回等待队列
     */
    @Override
    public synchronized void notifyTaskFinished(TaskBean<B> task, boolean addBack) {

        DebugLog.d(TAG, "notifyTaskFinished start");

        if (task == null) {
            return;
        }

        // 如果不是当前正在执行的任务（可能是没执行就被外部pause或abort了）

        if (task != mCurrentExecuted) {

            DebugLog.d(TAG, "notifyTaskFinished>>finshed task is not mCurrentExecuted");

            // 如果是TODO状态，且addBack为true，才能加回等待队列
            if (addBack && task.getStatus() == XTaskBean.STATUS_TODO) {

                if (!mTobeExecuted.contains(task))
                    mTobeExecuted.offer(task);

            } else {
                // 否则，直接丢弃该任务
                mTobeExecuted.remove(task);

            }

            return;
        }


        mCurrentExecuted = findNextTask();

        // 如果是TODO或ERROR结束的，且addBack为true，添加回等待队列
        if (addBack && task.getStatus() != XTaskBean.STATUS_DONE
                && !mTobeExecuted.contains(task)) {

            DebugLog.d(TAG, "notifyTaskFinished>>add back to mTobeExecuted");

            mTobeExecuted.offer(task);

        }

        // 如果已经标记停止，或者不自动执行，则什么都不做
        DebugLog.d(TAG, "notifyTaskFinished>>mIsWorking = " + mIsWorking + "--mAuto = " + mAuto);

        if (!mIsWorking || !mAuto) {

            DebugLog.d(TAG, "notifyTaskFinished>>callback onNoDowningTask");
            for (IDownloadTaskListener<B> listener : mListeners) {
                if (listener != null) {
                    listener.onNoDowningTask();
                }
            }
            return;
        }


        if (mCurrentExecuted != null) {

            if (mCurrentExecuted.mDownloadTask == null) {

                DebugLog.d(TAG, "notifyTaskFinished>>mCurrentExecuted.mDownloadTask is null and create DownloadTask");

                XBaseTaskExecutor<B> executor = mCreater.createDownloadTask(mCurrentExecuted.getId());
                if (executor != null) {
                    mCurrentExecuted.mDownloadTask = executor;
                    mCurrentExecuted.mDownloadTask.setListener(mInnerTaskListener);
                }
            }

            if (mCurrentExecuted.mDownloadTask != null) {

                DebugLog.d(TAG, "notifyTaskFinished>>mDownloadTask is not null,start task = " + mCurrentExecuted.getId());

                if (ITaskExecutor.TASK_START_SUCCESS == mCurrentExecuted.mDownloadTask.start()) {

                    DebugLog.d(TAG, "notifyTaskFinished>>start success");

                } else {

                    DebugLog.d(TAG, "notifyTaskFinished>>start fail");

                }
            }

        } else {

            DebugLog.d(TAG, "notifyTaskFinished>>cannot find next task");

            mIsWorking = false;

            if (mTobeExecuted.size() == 0) {

                // 当前没有执行任务，等待队列也没任务，则回调onFinishAll()
                for (IDownloadTaskListener<B> listener : mListeners) {
                    if (listener != null) {
                        DebugLog.d(TAG, "notifyTaskFinished>>task has been finished,callback onFinishAll");
                        listener.onFinishAll();
                    }
                }

            } else {
                // 当前没有执行任务，等待队列有任务，则回调onStopAll()
                for (IDownloadTaskListener<B> listener : mListeners) {
                    if (listener != null) {
                        listener.onNoDowningTask();
                        DebugLog.d(TAG, "notifyTaskFinished>>mTobeExcuted has task,callback onStopAll()");

                    }
                }
            }
        }

        DebugLog.d(TAG, "notifyTaskFinished>>end");


    }

    /**
     * 暂停所有任务
     * @return
     */
    @Override
    public synchronized boolean stopAll(){

        try {

            if (mCurrentExecuted == null && mTobeExecuted.size() == 0) {
                DebugLog.d(TAG, "stopAll()>>mCurrentExecuted and mTobeaExcuted is null");
                return false;
            }

            //停止所有的等待队列
            for (TaskBean<B> task : mTobeExecuted) {
                if (task != null) {
                    task.setStatus(XTaskBean.STATUS_DEFAULT);
                    if (task.mDownloadTask == null)
                        continue;
                    task.mDownloadTask.pause(XTaskBean.STATUS_DEFAULT);
                    task.mDownloadTask = null;
                }
            }

            if (mCurrentExecuted != null) {

                mCurrentExecuted.setStatus(XTaskBean.STATUS_DEFAULT);

                if (mCurrentExecuted.mDownloadTask != null) {
                    mCurrentExecuted.mDownloadTask.pause(XTaskBean.STATUS_DEFAULT);
                    mCurrentExecuted.mDownloadTask = null;
                }

            }

            for (IDownloadTaskListener<B> listener : mListeners) {
                if (listener != null) {
                    listener.onPauseAll();
                }
            }

            DebugLog.d(TAG, "stopAll()>>stop all success");

            mIsWorking = false;

            for (IDownloadTaskListener<B> listener : mListeners) {
                if (listener != null) {
                    listener.onNoDowningTask();
                }
            }

            return true;
        } catch (Exception e) {

        }
        return false;
    }

    /**
     * 启动所有任务
     *
     * @return
     */
    @Override
    public synchronized boolean startAll() {

        if (mCurrentExecuted == null && mTobeExecuted.size() == 0) {
            DebugLog.d(TAG, "startAll()>>mCurrentExecuted and mTobeaExcuted is null");
            return false;
        }

        if (mCurrentExecuted != null && mCurrentExecuted.getStatus() != XTaskBean.STATUS_DOING && mCurrentExecuted.getStatus() != XTaskBean.STATUS_DONE) {
            mCurrentExecuted.setStatus(XTaskBean.STATUS_TODO);
        }

        if (mCurrentExecuted != null && mCurrentExecuted.mDownloadTask != null) {
            if (mCurrentExecuted.mDownloadTask.getStatus() != XTaskBean.STATUS_DONE && mCurrentExecuted.mDownloadTask.getStatus() != XTaskBean.STATUS_DOING) {
                mCurrentExecuted.mDownloadTask.setStatus(XTaskBean.STATUS_TODO);
            }
        }

        for (TaskBean<B> task : mTobeExecuted) {

            if (task.getStatus() != XTaskBean.STATUS_DONE
                    && task.getStatus() != XTaskBean.STATUS_DOING) {

                task.setStatus(XTaskBean.STATUS_TODO);

            }

            if (task.mDownloadTask != null) {
                task.mDownloadTask.setStatus(XTaskBean.STATUS_TODO);
            }

        }

        for (IDownloadTaskListener<B> listener : mListeners) {
            if (listener != null) {
                listener.onPrepare();
            }
        }

        return true;
    }


    /**
     * 插队启动任务
     *
     * @param taskId 任务的唯一Id
     */
    @Override
    public synchronized void setRunningTask(String taskId) {
        TaskBean<B> task = getTaskById(taskId);
        if (mCurrentExecuted == null && task != null) {
            mTobeExecuted.remove(task);
            mCurrentExecuted = task;
        }
    }

    /**
     * 获取当前执行任务
     *
     * @return
     */
    @Override
    public synchronized TaskBean<B> getRunningTask() {

        return mCurrentExecuted;

    }

    /**
     * 为下载器加入任务创建器
     * 注意：此方法务必在创建下载器{@link #SerialTaskManager()}之后立马调用
     *
     * @param creator
     */
    @Override
    public synchronized void setDownloadCreator(IDownloadTaskCreator<B> creator) {

        this.mCreater = creator;

    }

    /**
     * 修改任务状态
     *
     * @param task
     * @param status
     */
    @Override
    public void setTaskStatus(B task, int status) {

        TaskBean<B> taskExecutor = getTaskById(task.getId());
        if (taskExecutor != null) {
            taskExecutor.setStatus(status);
            task.setStatus(status);
        }
    }

    @Override
    public void setTaskSchedule(ITaskSchedule<B> schedule) {

        this.schedule = schedule;

    }

    @Override
    public void setMaxParalle(int paralleNum) {

    }

    /**
     * 检测SD卡是否满
     *
     * @param bean
     * @return
     */
    protected boolean checkSDFull(B bean) {
        if (bean == null) {
            return false;
        }
        if (FileDownloadHelper.isSDFull(bean)) {
            for (IDownloadTaskListener<B> listener : mListeners) {
                if (listener != null) {
                    listener.onSDFull(bean);
                }

            }
            return true;
        }
        return false;
    }

    @Override
    public void setAutoRunning(boolean auto) {

        mAuto = auto;

    }

    @Override
    public boolean isAutoRunning() {

        return mAuto;

    }

    @Override
    public boolean hasTaskRunning() {

        return mCurrentExecuted != null && mIsWorking;

    }

    @Override
    public void registerListener(IDownloadTaskListener<B> listener) {
        mListeners.add(listener);
    }

    @Override
    public void unregisterListener(IDownloadTaskListener<B> listener) {
        mListeners.remove(listener);
    }

    @Override
    public List<IDownloadTaskListener<B>> getListeners() {
        return mListeners;
    }

    /**
     * 内部Comparator<T>子类，用于对mTobeExecuted进行优先级排序。
     *
     * 暂时没有排序需求，不开启此功能
     */
    private class SerialTaskComparator implements Comparator<TaskBean<B>>{
        @Override
        public int compare(TaskBean<B> lhs, TaskBean<B> rhs) {

           return 0;
        }
    }




}
