package com.iqiyi.video.download.engine.taskmgr.paralle;

import android.text.TextUtils;

import com.iqiyi.video.download.filedownload.TaskBean;
import com.iqiyi.video.download.engine.task.ITaskExecutor;
import com.iqiyi.video.download.engine.task.ITaskListener;
import com.iqiyi.video.download.engine.task.ITaskSchedule;
import com.iqiyi.video.download.engine.task.XBaseTaskExecutor;
import com.iqiyi.video.download.engine.taskmgr.IDownloadTaskCreator;
import com.iqiyi.video.download.engine.taskmgr.IDownloadTaskListener;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.video.module.download.exbean.XTaskBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 并发执行器，实现ParallelMgr接口的子类。
 * 封装了并行执行、增删任务、启动、恢复、暂停等操作。
 * 1.每个时刻，有一个或多个(不超过设置上限)任务正在执行；
 * 2.每个任务都在TODO,DOING,DONE,ERROR四个状态间转换；
 * 3.如果任务从DOING到ERROR，回调SerialMgr时，
 * 会将该任务丢弃，继续执行下一个；
 * 4.如果任务从DOING到TODO，回调SerialMgr时，
 * 会将该任务重新添加进等待队列，不继续执行；
 */
public class ParalleTaskManager<B extends XTaskBean> implements IParalleTaskManager<B> {

    private static final String TAG = "ParalleTaskManager";

    protected volatile boolean mIsWorking;// 标识运行状态

    protected volatile boolean mAuto;// 标识是否自动执行

    protected LinkedList<TaskBean<B>> mCurrentExecuted;// 正在运行的任务队列

    protected LinkedList<TaskBean<B>> mTobeExecuted;// 待执行的任务队列

    protected ParalleTaskComparator mInnerComparator;// 实际用来排序的比较器

    protected CopyOnWriteArrayList<IDownloadTaskListener<B>> mListeners;// 外部监听者

    protected ITaskListener<B> mInnerTaskListener;// 内部管理器对每个Task的监听

    protected int mParallelLimit;// 并行任务的数量上限

    protected IDownloadTaskCreator<B> mCreater;

    protected ITaskSchedule schedule;



    /**
     * 并发执行器
     *
     * @param parallelLimit 最大并发任务数
     */
    public ParalleTaskManager(int parallelLimit) {

        mParallelLimit = Math.max(parallelLimit, 1);

        mCurrentExecuted = new LinkedList<TaskBean<B>>();

        mTobeExecuted = new LinkedList<TaskBean<B>>();

        mInnerComparator = new ParalleTaskComparator();

        mListeners = new CopyOnWriteArrayList<IDownloadTaskListener<B>>();

        mIsWorking = false;

        mAuto = true;

        //内部任务监听器
        mInnerTaskListener = new ITaskListener<B>() {
            @Override
            public void onStart(B task) {

                TaskBean<B> taskExecutor = getTaskById(task.getId());
                if (taskExecutor != null) {
                    taskExecutor.setStatus(task.getStatus());
                }

                for (IDownloadTaskListener<B> listener : mListeners) {
                    listener.onStart(task);
                }
            }

            @Override
            public void onPause(B task) {


                TaskBean<B> taskExecutor = getTaskById(task.getId());

                if (taskExecutor != null) {
                    taskExecutor.setStatus(task.getStatus());
                }

                for (IDownloadTaskListener<B> listener : mListeners) {
                    listener.onPause(task);
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

                for (IDownloadTaskListener<B> listener : mListeners) {
                    listener.onDoing(task, completeSize);
                }
            }

            @Override
            public void onComplete(B task) {

                TaskBean<B> taskExecutor = getTaskById(task.getId());
                if (taskExecutor != null) {
                    taskExecutor.setStatus(XTaskBean.STATUS_DONE);
                }

                for (IDownloadTaskListener<B> listener : mListeners) {
                    listener.onComplete(task);
                }

                if (taskExecutor != null) {
                    notifyTaskFinished(taskExecutor, false);
                }
            }

            @Override
            public void onError(B task, String errorCode, boolean retry) {

                TaskBean<B> taskExecutor = getTaskById(task.getId());
                if (taskExecutor != null) {
                    taskExecutor.setStatus(task.getStatus());
                }

                for (IDownloadTaskListener<B> listener : mListeners) {
                    listener.onError(task, errorCode);
                }

                if (taskExecutor != null) {
                    notifyTaskFinished(taskExecutor, retry);
                }
            }
        };

    }

    /**
     * @return 是否当前执行队列为空
     */
    @Override
    public boolean isEmptyParallel() {

        return mCurrentExecuted.size() == 0;

    }

    /**
     * @return 当前执行队列是否满
     */
    @Override
    public boolean isFullParallel() {

        DebugLog.log(TAG,"mParallelLimit = " + mParallelLimit);
        DebugLog.log(TAG,"mCurrentExecuted = " + mCurrentExecuted.size());


        return mCurrentExecuted.size() >= mParallelLimit;

    }

    /**
     * @return 是否所有任务都被停止
     */
    @Override
    public boolean isAllStop() {

        for (TaskBean<B> task : mCurrentExecuted) {
            if (task.getStatus() == XTaskBean.STATUS_DOING
                    || task.getStatus() == XTaskBean.STATUS_STARTING) {
                DebugLog.log(TAG,"task is doing or starting = " + task.getId());
                return false;
            }
        }

        return true;
    }

    @Override
    public void setMaxParalle(int paralleNum) {

        int paramGap = paralleNum - mParallelLimit;

        this.mParallelLimit = paralleNum;

        if(mParallelLimit == mCurrentExecuted.size()){
            // paralleLimit equals currentTask size,ie. 3-3,2-2,1-1
            DebugLog.log(TAG,"setMaxParalle>>paralleNum equals currentExcuted Task num,do nothing");
            return;
        }



        notifyParalleNumChanged(paramGap);

    }

    private void notifyParalleNumChanged(int paramGap){

        DebugLog.log(TAG,"notifyParalleNumChanged>>paramGap = " + paramGap);

        if(paramGap > 0 ){
            DebugLog.log(TAG,"notifyParalleNumChanged>>paramGap>0,start to find next task");
            start();//开始任务，自动寻找任务，并且达到并行任务极限值
        } else if(paramGap == 0){
            DebugLog.log(TAG,"notifyParalleNumChanged>>paramGap==0,do nothing");
        } else{
            DebugLog.log(TAG,"notifyParalleNumChanged>>paramGap<0,pause additional task");
            if (isEmptyParallel()) {
                DebugLog.log(TAG, "notifyParalleNumChanged>>pause()>>pause all task fail,parallel is empty");
                return ;
            }
            List<TaskBean<B>> stopTasks = new ArrayList<TaskBean<B>>();

            for (int i = mCurrentExecuted.size()-1; i > Math.max(mCurrentExecuted.size()-1 - Math.abs(paramGap),0); i--) {

                if (mCurrentExecuted.get(i).mDownloadTask != null) {
                    DebugLog.log(TAG, "notifyParalleNumChanged>>pause task>>" + i +"--" +mCurrentExecuted.get(i).getId());
                    mCurrentExecuted.get(i).mDownloadTask.pause();
                    stopTasks.add(0,mCurrentExecuted.get(i));
                }
            }

            mCurrentExecuted.removeAll(stopTasks);

            mTobeExecuted.addAll(0,stopTasks);

        }


    }



    /**
     * @return 如果所有任务都是暂停状态，则返回true;否则返回false
     */
    private boolean setStopIfAllStop() {
        if (!isAllStop())
            return false;
        mIsWorking = false;
        DebugLog.log(TAG, "setStopIfAllStop()>>mIsWorking == false");
        return true;
    }

    /**
     * 获取task的id
     *
     * @param task 任务
     * @return
     */
    @Override
    public String getTaskId(TaskBean<B> task) {

        String taskId = task != null ? task.getId() : "";

        return taskId;

    }

    /**
     * 通过taskid获取taskbean
     *
     * @param id 任务id
     * @return
     */
    @Override
    public TaskBean<B> getTaskById(String id) {

        if (TextUtils.isEmpty(id)) {
            return null;
        }

        try {
            for (TaskBean<B> task : mCurrentExecuted) {
                if (id.equals(getTaskId(task)))
                    return task;
            }

            for (TaskBean<B> task : mTobeExecuted) {
                if (id.equals(getTaskId(task)))
                    return task;
            }

        }catch (ConcurrentModificationException e){
            ExceptionUtils.printStackTrace(e);
        }

        return null;
    }

    /**
     * 添加一个任务
     *
     * @param task 任务
     * @return
     */
    @Override
    public synchronized boolean addTask(TaskBean<B> task) {

        // 判断是否重复
        if (getTaskById(getTaskId(task)) != null) {

            DebugLog.d(TAG, "addTask>>task" + getTaskId(task) + " is already exist!");

            return false;
        }

        task.setTaskMgr(this);
        task.setStatus(XTaskBean.STATUS_TODO);
        mTobeExecuted.offer(task);
        return true;
    }


    /**
     * 添加多个任务
     *
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
            if (getTaskById(getTaskId(task)) != null) {
                // 判断是否重复
                DebugLog.d(TAG, "addTask>>task" + getTaskId(task) + " is already exist!");
                continue;
            }

            task.setTaskMgr(this);
           // task.setStatus(XTaskBean.STATUS_TODO);
            mTobeExecuted.offer(task);
        }

    }


    /**
     * 删除任务
     *
     * @param task 要删除的任务
     */
    @Override
    public synchronized void removeTask(TaskBean<B> task) {

        if (task == null)
            return;

        if (task.mDownloadTask != null) {
            // 终止当前任务
            task.mDownloadTask.abort();

        }

        if (mCurrentExecuted != null && mCurrentExecuted.contains(task)) {
            // 删除执行队列中的任务
            mCurrentExecuted.remove(task);
        } else {
            //删除等待队列中的任务
            mTobeExecuted.remove(task);
        }
        //若删除的多个任务中，有正在下载的任务，则自动寻找下一个任务并启动它
        if (mCurrentExecuted == null || mCurrentExecuted.size() == 0 || !isFullParallel()) {

            DebugLog.d(TAG, "removeTasks>>currentExecuted has been removed");

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
     * @param tasks 待删除的任务对象列表
     */
    @Override
    public synchronized void removeTasks(List<TaskBean<B>> tasks) {

        DebugLog.log(TAG,"removeTasks");

        if (tasks == null || tasks.size() == 0) {
            DebugLog.log(TAG,"removeTasks>>tasks size = 0");
            return;
        }

        for (TaskBean<B> task : tasks) {

            if (task == null)
                continue;

            if (task.mDownloadTask != null) {
                //终止任务
                task.mDownloadTask.abort();
            }

            if (mCurrentExecuted.contains(task)) {
                mCurrentExecuted.remove(task);
            } else {
                mTobeExecuted.remove(task);
            }
        }

        //若删除的多个任务中，有正在下载的任务，则自动寻找下一个任务并启动它
        if (mCurrentExecuted == null || mCurrentExecuted.size() == 0 || !isFullParallel()) {

            DebugLog.d(TAG, "removeTasks>>currentExecuted has been removed");

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

        } else{
            DebugLog.log(TAG,"removeTasks>>do not enable auto start task");
        }


    }

    /**
     * 通过taskid删除多个任务
     *
     * @param taskIds 待删除的任务Id列表
     */
    @Override
    public synchronized void removeTasksById(List<String> taskIds) {

        DebugLog.log(TAG,"removeTasksById");

        if (taskIds == null || taskIds.size() == 0) {
            DebugLog.log(TAG,"removeTasksById>>taskIds size = 0");
            return;
        }

        List<TaskBean<B>> tasks = new ArrayList<TaskBean<B>>();
        for (String taskId : taskIds) {
            TaskBean<B> task = getTaskById(taskId);
            if (task != null)
                tasks.add(task);
        }
        removeTasks(tasks);
    }


    /**
     * 启动所有任务
     *
     * @return
     */
    @Override
    public synchronized boolean start() {

        DebugLog.log(TAG,"start>>mParallelLimit = " + mParallelLimit);
        DebugLog.log(TAG,"start>>mCurrentExecuted = " + mCurrentExecuted.size());

        // 如果运行队列未满，则启动多个等待队列中的任务直到满
        while (!isFullParallel()) {

            TaskBean<B> task = findNextTask(null);

            if (task == null) {
                break;
            }

            DebugLog.log(TAG, "start()>>findTask = " + task.getId() + ">>status = " + task.getStatus());

            mCurrentExecuted.offer(task);
        }


        // 如果运行队列为空，则什么都不做
        if (isEmptyParallel()) {

            return false;

        }


        // 启动运行队列的所有任务
        for (TaskBean<B> task : mCurrentExecuted) {

            // 尝试启动任务。启动任务之前，务必判断DownloadTask是否null
            if (task.mDownloadTask == null) {

                XBaseTaskExecutor<B> executor = mCreater.createDownloadTask(task.getId());

                DebugLog.log(TAG, "start()>>create mDownloadTask = " + task.getId());

                if (executor != null) {
                    task.mDownloadTask = executor;
                    task.mDownloadTask.setListener(mInnerTaskListener);
                }

            }

            if (task.mDownloadTask == null) {
                DebugLog.log(TAG, "start()>>mDownloadTask create fail ,mDownlaodTask　is null");
                return false;
            }

            if(task.mDownloadTask.getStatus() == XTaskBean.STATUS_STARTING || task.mDownloadTask.getStatus()  == XTaskBean.STATUS_DOING ){

                DebugLog.log(TAG, "current task = " + task.getId() + "is doing or starting,continue downlaoding");

            } else{

                int status = task.mDownloadTask.start();
                // 先尝试启动指定任务
                if (ITaskExecutor.TASK_START_SUCCESS == status) {

                    DebugLog.log(TAG, "start()>>taskId = " + task.getId());

                    mIsWorking = true;

                } else{

                    task.setStatus(XTaskBean.STATUS_DOING);

                    DebugLog.log(TAG, "start()>>start task fail = " + task.getId() + ">> status = "+status);

                }
            }

        }
        return true;
    }


    /**
     * 启动指定id任务
     *
     * @param taskId 任务的唯一Id
     * @return
     */
    @Override
    public synchronized boolean start(String taskId) {

        TaskBean<B> task = getTaskById(taskId);
        // 如果指定Id的任务不存在，则什么都不做，返回false
        if (task == null) {
            return false;
        }


        if (task.mDownloadTask == null) {

            XBaseTaskExecutor<B> executor = mCreater.createDownloadTask(task.getId());

            if (executor != null) {
                task.mDownloadTask = executor;
                task.mDownloadTask.setListener(mInnerTaskListener);
            }

        }

        //创建mDownloadTask失败，则什么都不做
        if(task.mDownloadTask == null) {
            DebugLog.log(TAG,"start(String taskId)>>mDownloadTask == null,do nothing");
            return false;
        }

        int status =  task.mDownloadTask.start(XTaskBean.STATUS_DEFAULT);
        // 先尝试启动指定任务
        if (ITaskExecutor.TASK_START_SUCCESS != status) {
            DebugLog.log(TAG, "start(id)>>start task fail = " +  task.getId() + "-status = " +status);
            return false;
        } else{
            task.setStatus(XTaskBean.STATUS_DOING);
            DebugLog.log(TAG, "start(id)>>start task success = " + task.getId());

        }

        // 如果指定Id的任务不在运行队列中
        if (!mCurrentExecuted.contains(task)) {
            // 如果运行队列已满，则替换一个任务
            if (isFullParallel()) {
                TaskBean<B> oldTask = mCurrentExecuted.getLast();
                oldTask.mDownloadTask.pause();
                DebugLog.log(TAG,"oldTask.mDownloadTask = " +oldTask.mDownloadTask.getId() +">>"+ oldTask.mDownloadTask.getStatus());
                mCurrentExecuted.remove(oldTask);//从当前队列删除
                mTobeExecuted.addFirst(oldTask);// 添加回等待队列
            }

            // 调整指定id的任务所在的队列
            mTobeExecuted.remove(task);
            mCurrentExecuted.offer(task);
        }
        mIsWorking = true;
        return true;
    }


    /**
     * 暂停所有任务，状态变成等待中
     *
     * @return
     */
    @Override
    public synchronized boolean pause() {

        if (isEmptyParallel()) {
            DebugLog.log(TAG, "pause()>>pause all task fail,parallel is empty");
            return false;
        }
        List<TaskBean<B>> stopTasks = new ArrayList<TaskBean<B>>();

        // 尝试暂停任务
        for (TaskBean<B> task : mCurrentExecuted) {
            if (task.mDownloadTask != null) {

                task.mDownloadTask.pause();

            }
            stopTasks.add(task);
        }

        // 没有任务暂停成功，则返回false
        if (stopTasks.size() == 0) {
            DebugLog.log(TAG, "stop()>>stop all task fail");
            return false;
        }

        // 添加回等待队列
        mCurrentExecuted.removeAll(stopTasks);

        mTobeExecuted.addAll(0, stopTasks);

        DebugLog.log(TAG, "pause()>>pause all task success");

        return true;
    }

    /**
     * 暂停指定id任务，状态变成等待中
     *
     * @param taskId 任务的唯一Id
     * @return
     */
    @Override
    public synchronized boolean pause(String taskId) {

        TaskBean<B> task = getTaskById(taskId);
        // 如果指定Id的任务不存在，或在等待队列中，则什么都不做，返回false
        if (task == null) {
            DebugLog.log(TAG, "pause(id)>>>task == null");
            return false;
        }

        if (!mCurrentExecuted.contains(task)) {
            DebugLog.log(TAG, "pause(id)>>>mCurrentExecuted does not contains task");
            return false;
        }

        // 如果指定Id的任务存在，且在运行队列中，暂停该任务
        int result = task.mDownloadTask.pause();

        if (result != ITaskExecutor.TASK_PAUSE_SUCCESS
                && result != ITaskExecutor.TASK_PAUSE_ERROR_TASK) {

            DebugLog.log(TAG, "pause(id)>>>pause fail = " + taskId);
            return false;
        } else{
            DebugLog.log(TAG, "pause(id)>>>pause success = " + taskId);
        }

        mCurrentExecuted.remove(task);
        mTobeExecuted.offer(task);

        if (mAuto) {
            //自动寻找下一个任务
            if (!start()) {
                mIsWorking = false;
            }
        }


        return true;
    }


    /**
     * 停止所有任务，状态变成已暂停
     *
     * @return
     */
    @Override
    public synchronized boolean stop() {

        if (isEmptyParallel()) {
            DebugLog.log(TAG, "stop()>>>parallel is empty");
            return false;
        }
        // 尝试暂停任务
        List<TaskBean<B>> stopTasks = new ArrayList<TaskBean<B>>();

        for (TaskBean<B> task : mCurrentExecuted) {

            int result = task.mDownloadTask.pause(XTaskBean.STATUS_DEFAULT);

            if (result != ITaskExecutor.TASK_PAUSE_SUCCESS && result != ITaskExecutor.TASK_PAUSE_ERROR_TASK) {
                DebugLog.log(TAG, "stop()>>>stop currentExecuted fail!");
                continue;
            } else{
                task.setStatus(XTaskBean.STATUS_DEFAULT);
            }

            stopTasks.add(task);

        }

        // 没有任务暂停成功，则返回false
        if (stopTasks.size() == 0) {
            DebugLog.log(TAG, "stop()>>stop all task fail");
            return false;
        }

        // 添加回等待队列
        mCurrentExecuted.removeAll(stopTasks);

        mTobeExecuted.addAll(0, stopTasks);

        setStopIfAllStop();

        DebugLog.log(TAG, "stop()>>stop all task success");

        return true;
    }

    /**
     * 停止指定的任务，状态变成已暂停
     *
     * @param taskId 任务的唯一Id
     * @return
     */
    @Override
    public synchronized boolean stop(String taskId) {

        TaskBean<B> task = getTaskById(taskId);

        // 如果指定Id的任务不存在，或在等待队列中，则什么都不做，返回false
        if (task == null) {
            DebugLog.log(TAG, "stop(id)>>task == null");
            return false;
        }

        if (!mCurrentExecuted.contains(task)) {
            DebugLog.log(TAG, "stop(id)>>mCurrentExecuted does not contains task");
            return false;
        }

        // 如果指定Id的任务存在，且在运行队列中，暂停该任务(将任务从运行队列移回等待队列)

        int result = task.mDownloadTask.pause(XTaskBean.STATUS_DEFAULT);

        if (result != ITaskExecutor.TASK_PAUSE_SUCCESS
                && result != ITaskExecutor.TASK_PAUSE_ERROR_TASK) {
            DebugLog.log(TAG, "stop(id)>>>stop task fail = "  + task.getId() + ">> status = " + task.getStatus());
            return false;
        } else{
            DebugLog.log(TAG, "stop(id)>>stop task success = " + task.getId());
            task.setStatus(XTaskBean.STATUS_DEFAULT);

        }


        // 添加回等待队列
        mCurrentExecuted.remove(task);

        mTobeExecuted.addFirst(task);

        if (mAuto) {
            //自动寻找下一个任务
            if (!start()) {
                mIsWorking = false;
            }
        }


        return true;
    }


    /**
     * 终止并清空任务队列
     */
    @Override
    public synchronized void stopAndReset() {
        mIsWorking = false;
        // 终止并清空当前任务
        for (TaskBean<B> task : mCurrentExecuted) {
            if (task.mDownloadTask != null) {
                task.mDownloadTask.pause();
            }
        }

        mCurrentExecuted.clear();
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
     * 设置是否自动下载
     *
     * @param auto true表示开启自动执行;false表示关闭自动执行。
     */
    @Override
    public void setAutoRunning(boolean auto) {

        mAuto = auto;

    }

    /**
     * 返回是否可以自动下载
     *
     * @return
     */
    @Override
    public boolean isAutoRunning() {

        return mAuto;

    }

    /**
     * 寻找下一个任务。
     * 策略：1.将任务排序，过滤，返回第一个是TODO状态的任务(其他状态的任务忽略)
     * 2.如果没有符合1要求的任务，则返回第一个TODO状态但被过滤的任务
     * 3.如果没有以上的任务，则返回null
     *
     * @return 返回下一个待执行的任务，如果没有可执行的任务，则返回null
     * //@see #setTaskScheduler(XTaskScheduler)
     */
    protected TaskBean<B> findNextTask(TaskBean<B> curTask) {


        DebugLog.log(TAG,"***findNextTask start***");

        try {

            if (schedule != null) {
                DebugLog.log(TAG, "sort the list");
                Collections.sort(mTobeExecuted, mInnerComparator);
            }

            DebugLog.log(TAG, "***list mTobeExecuted start***");
            for (TaskBean<B> task : mTobeExecuted) {
                if(task.getScheduleBean() != null) {
                    DebugLog.log(TAG, "list mTobeExecuted>>" + task.getId() + "--status = " + task.getStatus() + "-- " + task.getScheduleBean().toString());
                } else{
                    DebugLog.log(TAG, "list mTobeExecuted>>" + task.getId() + "--status = " + task.getStatus());
                }
            }
            DebugLog.log(TAG, "***list mTobeExecuted end***");

            if (curTask != null) {
                DebugLog.log(TAG, "findNextTask>>last task = " + curTask.getId());
            } else {
                DebugLog.log(TAG, "findNextTask>>last task = null");
            }

            TaskBean<B> nextTask = null;// 最终的结果，下一个待执行任务

            for (TaskBean<B> task : mTobeExecuted) {
                // 不是TODO状态的任务，跳过
                if (task.getStatus() != XTaskBean.STATUS_TODO) {
                    DebugLog.log(TAG, "findNextTask skip = " + task.getId() + ">>status = " + task.getStatus());
                    continue;
                } else {
                    nextTask = task;
                    DebugLog.log(TAG, "findNextTask target = " + task.getId() + ">>status = " + task.getStatus());
                    break;
                }
            }

            // 如果找到下一个任务，则将其从等待队列中移除
            if (nextTask != null) {

                mTobeExecuted.remove(nextTask);

            } else {

                DebugLog.log(TAG, "findNextTask>>can not find next task");
            }

            DebugLog.log(TAG, "***findNextTask end***");

            return nextTask;

        }catch (Exception e){
            ExceptionUtils.printStackTrace(e);
        }
        return null;
    }

    @Override
    public synchronized void notifyTaskFinished(TaskBean<B> task, boolean addBack) {

        if (task == null) {
            DebugLog.log(TAG, "notifyTaskFinished>>task is null");
            return;
        }

        // 如果不是在执行队列中的任务（可能是没执行就被外部pause或abort了）
        if (!mCurrentExecuted.contains(task)) {

            DebugLog.log(TAG, "notifyTaskFinished>>mCurrentExecuted does not contains task = " + task.getId());
            // 如果是TODO状态添，且addBack为true，才能加回等待队列
            if (addBack && task.getStatus() == XTaskBean.STATUS_TODO) {
                if (!mTobeExecuted.contains(task)) {
                    mTobeExecuted.offer(task);
                }
            } else {
                mTobeExecuted.remove(task);// 否则，直接丢弃该任务
            }
            return;
        }

        // 是执行队列中的的任务
        if (task.getStatus() == XTaskBean.STATUS_DOING) {
            // 正在执行,非法状态
            DebugLog.log(TAG, task.getId() + ">>notifyTaskFinished>>task status is doing,error status");
            return;
        }

        // 如果是TODO结束的，ERROR结束的，或是DONE结束的，寻找下一个任务
        mCurrentExecuted.remove(task);

        TaskBean<B> nextTask = findNextTask(task);

        if (nextTask != null) {

            mCurrentExecuted.offer(nextTask);

        }

        // 下一个任务为空，但当前等待队列不为空，则说明等待队列中所有的任务都是非todo状态（即暂停状态，错误状态）
        //boolean allError = (nextTask == null && mTobeExecuted.size() > 0);

        // 如果是TODO或ERROR结束的，且addBack为true，添加回等待队列
        if (addBack && task.getStatus() != XTaskBean.STATUS_DONE && !mTobeExecuted.contains(task)) {

            DebugLog.log(TAG, "notifyTaskFinished>>addback to mTobeExecuted = " + task.getId());

            mTobeExecuted.offer(task);
        }


        // 如果已经标记停止，或者不自动执行，则什么都不做
        DebugLog.log(TAG, "notifyTaskFinished>>mIsWorking = " + mIsWorking + ">>mAuto = " + mAuto);

        if (!mIsWorking || !mAuto) {
            DebugLog.log(TAG, "notifyTaskFinished>>stop working or stop auto excute task");
            if (isAllStop()) {
                DebugLog.log(TAG, "notifyTaskFinished>>all task stoped");
                for (IDownloadTaskListener<B> listener : mListeners)
                    listener.onNoDowningTask();
            }
            return;
        }

        if (nextTask != null) {

            if (nextTask.mDownloadTask == null) {

                XBaseTaskExecutor<B> executor = mCreater.createDownloadTask(nextTask.getId());
                if (executor != null) {
                    nextTask.mDownloadTask = executor;
                    nextTask.mDownloadTask.setListener(mInnerTaskListener);
                }

            }

            if (nextTask.mDownloadTask != null) {

                int status = nextTask.mDownloadTask.start();

                if (ITaskExecutor.TASK_START_SUCCESS == status) {

                    DebugLog.log(TAG, "notifyTaskFinished>>start success = " + nextTask.getId());

                } else {

                    DebugLog.log(TAG, "notifyTaskFinished>>start fail = " + nextTask.getId() + ">> status = " +status);

                }
            }
        } else {
            // 没有下一个任务
            if (isEmptyParallel() && mTobeExecuted.size() == 0) {
                // 执行队列没有任务，等待队列也没任务，则回调onFinishAll()
                mIsWorking = false;
                DebugLog.log(TAG, "notifyTaskFinished>>mTobeExcuted is empty,callback onFinishAll()");
                for (IDownloadTaskListener<B> listener : mListeners)
                    listener.onFinishAll();
            }
        }
    }


    /**
     * 内部Comparator<T>子类，用于对mTobeExecuted进行优先级排序。
     * 通过传入的TaskScheduler来实际进行排序比较。
     */
    private class ParalleTaskComparator implements Comparator<TaskBean<B>> {

        @Override
        public int compare(TaskBean<B> lhs, TaskBean<B> rhs) {

            if(schedule != null ){
               return schedule.compare(lhs.getScheduleBean(),rhs.getScheduleBean());
            }else{
                return 0;
            }
        }
    }

    /**
     * 停止所有任务
     *
     * @return
     */
    @Override
    public synchronized boolean stopAll() {


        if (mCurrentExecuted.size() == 0 && mTobeExecuted.size() == 0) {
            DebugLog.log(TAG, "stopAll()>>mCurrentExecuted and mTobeaExcuted is null");
            return false;
        }
        List<TaskBean<B>> stopTasks = new ArrayList<TaskBean<B>>();

        for (TaskBean<B> task : mCurrentExecuted) {
            if (task != null) {
                task.setStatus(XTaskBean.STATUS_DEFAULT);
                if (task.mDownloadTask == null)
                    continue;
                task.mDownloadTask.pause(XTaskBean.STATUS_DEFAULT);
                task.mDownloadTask = null;
                stopTasks.add(task);
            }
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


        mCurrentExecuted.clear();

        mTobeExecuted.addAll(0,stopTasks);

        mIsWorking = false;

        for (IDownloadTaskListener<B> listener : mListeners) {
            if (listener != null) {
                listener.onPauseAll();
            }
        }

        DebugLog.d(TAG, "stopAll()>>stop all task success");

        return true;

    }

    /**
     * 启动所有任务
     *
     * @return
     */
    @Override
    public synchronized boolean startAll() {

        if (mCurrentExecuted.size() == 0 && mTobeExecuted.size() == 0) {
            DebugLog.d(TAG, "startAll()>>mCurrentExecuted and mTobeaExcuted is null");
            return false;
        }

        for (TaskBean<B> task : mCurrentExecuted) {

            if (task.getStatus() != XTaskBean.STATUS_DONE
                    && task.getStatus() != XTaskBean.STATUS_DOING) {

                task.setStatus(XTaskBean.STATUS_TODO);

            }

            if (task.mDownloadTask != null) {
                task.mDownloadTask.setStatus(XTaskBean.STATUS_TODO);
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

        DebugLog.d(TAG, "startAll()>>start all task success");

        return true;
    }


    /**
     * 暂时返回当前下载队列中的第一个任务
     * 后期正式上线并发下载器时，再修改此处
     *
     * @return
     */
    @Override
    public synchronized TaskBean<B> getRunningTask() {

        if (!mCurrentExecuted.isEmpty()) {

            for (TaskBean<B> task : mCurrentExecuted) {
                if (task != null) {
                    return task;
                }
            }

        }

        return null;

    }

    /////////////////////////////////////////////////////////////////////////////


    /////////////////////////////////////////////////////////////////////////////
    /**
     * 插队启动执行任务
     *
     * @param taskId 任务的唯一Id
     */
    @Override
    public synchronized void setRunningTask(String taskId) {
        // 如果运行队列已满，则什么都不做
        if (isFullParallel())
            return;

        // 如果运行队列未满，则将指定任务从等待队列添加进运行队列
        TaskBean<B> task = getTaskById(taskId);
        if (!mCurrentExecuted.contains(task) && task != null) {
            mTobeExecuted.remove(task);
            mCurrentExecuted.addLast(task);
        }
    }

    /**
     * 为下载器加入任务创建器
     *
     * @param creator
     */
    @Override
    public synchronized void setDownloadCreator(IDownloadTaskCreator<B> creator) {

        this.mCreater = creator;

    }

    /**
     * 设置任务状态
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



    /**
     * 注册外部监听者
     *
     * @param listener 外部监听者
     */
    @Override
    public void registerListener(IDownloadTaskListener<B> listener) {
        mListeners.add(listener);
    }

    /**
     * 移出外部监听者
     *
     * @param listener 外部监听者
     */
    @Override
    public void unregisterListener(IDownloadTaskListener<B> listener) {
        mListeners.remove(listener);
    }

    /**
     * 获取所有listener
     *
     * @return
     */
    @Override
    public List<IDownloadTaskListener<B>> getListeners() {

        return mListeners;

    }


    /**
     * 是否还有任务可以下载
     * <p>
     * 并行下载器是否达到并发个数
     *
     * @return
     */
    @Override
    public boolean hasTaskRunning() {

        if (mCurrentExecuted.size() >= mParallelLimit) {
            return true;
        } else {
            return false;
        }

    }


}
