package com.iqiyi.video.download.filedownload;

import com.iqiyi.video.download.engine.task.XBaseTaskExecutor;
import com.iqiyi.video.download.engine.taskmgr.serial.ISerialTaskManager;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.video.module.download.exbean.ScheduleBean;
import org.qiyi.video.module.download.exbean.XTaskBean;

/**
 * TaskBean 是存储在下载队列中的对象，该对象是具体下载对象的简化版本(为了节约内存)
 * 当启动下载任务时，通过taskId找到数据库中的真实下载对象，从而创建下载任务并开始下载
 * Created by yuanzeyao on 2015/6/3.
 */
public class TaskBean<B extends XTaskBean> //implements XTaskBean
{
    //下载任务id,必须保持唯一性
    private String taskId;

    //当前下载任务状态
    private int mStatus;

    private ScheduleBean scheduleBean;

    //下载的Task，该字段只有启动下载才创建，默认是NULL,所以在使用之前一定判断该值是否空
    public XBaseTaskExecutor<B> mDownloadTask;

    //该下载任务所属下载管理器
    private ISerialTaskManager<B> mTaskMgr;

    public TaskBean(String taskId, int mStatus)
    {
        this.mStatus=mStatus;
        this.taskId=taskId;
        DebugLog.log("ParalleTaskManager-XBean",taskId + ">> init status = " + mStatus);

    }

    public String getId() {
        return taskId;
    }

    public int getStatus() {

        return mStatus;
    }

    public void setStatus(int status) {
        this.mStatus=status;
    }

    public void setScheduleBean(ScheduleBean scheduleBean){

        this.scheduleBean = scheduleBean;

    }

    public ScheduleBean getScheduleBean(){

        return scheduleBean;

    }

    /**
     * 设置任务所属的任务管理器。
     * 当任务结束时，调用TaskMgr的notifyTaskFinished()
     * @param taskMgr
     */
   public void setTaskMgr(ISerialTaskManager<B> taskMgr)
    {
        this.mTaskMgr=taskMgr;
    }

    /**
     * 获取任务所属的任务管理器。
     * @return 返回任务所诉的任务管理器
     */
    public ISerialTaskManager<B> getTaskMgr() {
        return mTaskMgr;
    }

}
