package com.iqiyi.video.download.engine.task;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.video.module.download.exbean.XTaskBean;

/**
 * <pre>
 * 普通任务执行器的抽象类。
 * 封装了一个任务的核心状态机逻辑。
 * 注意:
 * 1.状态机：有五个状态TODO,DOING,DONE,ERROR,DEFAULT
 *           和五个行为start,pause,abort,endSuccess,endError
 *      start = {@link XTaskBean#STATUS_TODO} / {@link XTaskBean#STATUS_ERROR} to {@link XTaskBean#STATUS_DOING}
 *      pause = {@link XTaskBean#STATUS_DOING} to {@link XTaskBean#STATUS_TODO} or {@link XTaskBean#STATUS_DEFAULT}
 *      abort = all status to {@link XTaskBean#STATUS_DONE}
 *      endSuccess ={@link XTaskBean#STATUS_DOING} to {@link XTaskBean#STATUS_DONE}
 *      endError = {@link XTaskBean#STATUS_DOING} to {@link XTaskBean#STATUS_ERROR}
 * 2.子类继承时，重写五个行为的回调方法即可：
 *      onStart(),onPause(),onAbort(),onEndSuccess(),onEndError()
 * 3.不允许在onStart()等5个自定义回调方法中，同步调用start()等5个行为方法；
 * User: jasontujun
 * update by yuanzeyao 15-05-27
 * Date: 13-9-27
 * Time: 上午10:03
 * </pre>
 */
public abstract class XBaseTaskExecutor<B extends XTaskBean>
        implements ITaskExecutor<B> {

    public static final String TAG="XBaseTaskExecutor";
    private volatile int mStatus;// 状态
    private B mBean;// 任务数据
    private ITaskListener<B> mListener;

    public XBaseTaskExecutor(B bean) {
        mBean = bean;
        mStatus = bean.getStatus();
    }

    public XBaseTaskExecutor(B bean, int status) {
        mBean = bean;
        mStatus = status;
    }

    @Override
    public B getBean() {
        return mBean;
    }

    @Override
    public String getId() {
        return mBean.getId();
    }

    @Override
    public synchronized void setStatus(int status) {
        mStatus = status;
        mBean.setStatus(status);
    }

    @Override
    public synchronized int getStatus() {
        return mStatus;
    }

    @Override
    public void setListener(ITaskListener<B> listener) {
        mListener = listener;
    }

    @Override
    public ITaskListener<B> getListener() {
        return mListener;
    }

    @Override
    public int start(int... preStatus) {
        synchronized (this) {
            int oldStatus = getStatus();
            DebugLog.log(TAG, "start>>>status:" + oldStatus);
            if (oldStatus == XTaskBean.STATUS_STARTING || oldStatus == XTaskBean.STATUS_DOING) {
                //当前任务正在启动中..
                DebugLog.log(TAG, "start>>>任务正在启动中：status:" + oldStatus);
                return TASK_START_DOING_OR_STARTING;
            }

            if (getStatus() != XTaskBean.STATUS_TODO
                    && getStatus() != XTaskBean.STATUS_ERROR
                    && (preStatus.length == 0
                    || getStatus() != preStatus[0])) {
                //在三种情况下可以启动下载任务：STATUS_TODO,STATUS_ERROR,preStatus[0]指定状态才能启动
                DebugLog.log(TAG, "start>>>当前任务处于非法启动状态 status:" + getStatus());
                return TASK_INVALIDATED_STATUS;
            }

            //将当前任务标记为正在启动状态
            setStatus(XTaskBean.STATUS_STARTING);

            if (!onStart()) {
                setStatus(oldStatus);
                return TASK_START_FAIL;
            }
            setStatus(XTaskBean.STATUS_DOING);
        }

        if (mListener != null) {
            mListener.onStart(getBean());
        }

        DebugLog.log(TAG, "start>>>当前任务启动成功 status:" + getStatus());

        return TASK_START_SUCCESS;
    }

    @Override
    public int pause(int... postStatus) {
        synchronized (this) {

            if (getStatus() == XTaskBean.STATUS_ERROR) {
                DebugLog.log("ParalleTaskManager", "pause>>>暂停一个处于错误状态的任务！！");
                return TASK_PAUSE_ERROR_TASK;
            }
            if (postStatus.length > 0 && (postStatus[0] != XTaskBean.STATUS_DEFAULT && postStatus[0] != XTaskBean.STATUS_TODO)) {

                DebugLog.log("ParalleTaskManager", "pause>>>postStatus指定的状态不合法 status:" + postStatus[0]);
                return TASK_GIVEN_STATE_ERROR;
            }

            if (getStatus() != XTaskBean.STATUS_DOING && getStatus() != XTaskBean.STATUS_STARTING) {
                DebugLog.log("ParalleTaskManager", "pause>>>指定暂停DOING或者STARTING 状态的任务！status:" + getStatus());
                return TASK_INVALIDATED_STATUS;
            }

            if (!onPause() && postStatus.length == 0) {
                DebugLog.log("ParalleTaskManager", "pause>>>当前任务暂停失败 status:" + getStatus());
                return TASK_PAUSE_FAIL;
            }


            if (postStatus.length > 0) {
                setStatus(postStatus[0]);
            } else {
                setStatus(XTaskBean.STATUS_TODO);
            }
        }
        if (mListener != null)
            mListener.onPause(getBean());
        DebugLog.log("ParalleTaskManager", "pause>>>当前任务暂停成功 status:" + getStatus());
        return TASK_PAUSE_SUCCESS;
    }

    @Override
    public boolean abort() {
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_TODO
                    && getStatus() != XTaskBean.STATUS_DOING)
                return false;

            if (!onAbort())
                return false;

            setStatus(XTaskBean.STATUS_DONE);
        }
        if (mListener != null)
            mListener.onAbort(getBean());
        return true;
    }

    public boolean endSuccess() {
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_DOING)
                return false;

            if (!onEndSuccess())
                return false;

            setStatus(XTaskBean.STATUS_DONE);
        }
        if (mListener != null)
            mListener.onComplete(getBean());
        return true;
    }

    public boolean endError(String errorCode, boolean retry) {
        synchronized (this) {
            DebugLog.log(TAG,"endError>>>getStatus() = "+getStatus());
            if (getStatus() != XTaskBean.STATUS_DOING && getStatus() != XTaskBean.STATUS_STARTING) {
                DebugLog.log(TAG, "endError>>>指定错误结束的任务不是DOING或者STARTING 状态的任务！status:" + getStatus());
                return false;
            }
            if (!onEndError(errorCode, retry)) {
                return false;
            }
            setStatus(XTaskBean.STATUS_ERROR);
        }
        if (mListener != null) {
            mListener.onError(getBean(), errorCode, retry);
        }
        DebugLog.log(TAG, "endError>>>成功！");
        return true;
    }

    /**
     * 通知外部任务正在执行的进度
     */
    public void notifyDoing(long completeSize) {
        if (mListener != null) {
            if (this.getStatus() != this.getBean().getStatus()) {
                //保持下载任务和下载对象的状态统一
                DebugLog.log(TAG, "下载任务和下载对象的状态不统一：taskStatus = " + this.getStatus() + "and ObjectStatus =" + this.getBean().getStatus());
                this.getBean().setStatus(this.getStatus());
            }
            mListener.onDoing(getBean(), completeSize);
        }

    }

    /////////////各任务自行实现相应的逻辑/////////////////////////////////
    /**
     * 启动任务的回调函数。
     * @return 启动成功返回true;否则返回false
     */
    protected abstract boolean onStart();

    /**
     * 暂停任务的回调函数。
     * @return 暂停成功返回true;否则返回false
     */
    protected abstract boolean onPause();

    /**
     * 终止任务的回调函数。
     * @return 终止成功返回true;否则返回false
     */
    protected abstract boolean onAbort();

    /**
     * 任务成功结束的回调。
     * @return 没有发生异常返回true;否则返回false
     */
    protected abstract boolean onEndSuccess();

    /**
     * 任务失败结束的回调。
     * @param errorCode 错误码
     * @param retry 如果需要重试，则为true；否则为false
     * @return 没有发生异常返回true;否则返回false
     */
    protected abstract boolean onEndError(String errorCode, boolean retry);

}
