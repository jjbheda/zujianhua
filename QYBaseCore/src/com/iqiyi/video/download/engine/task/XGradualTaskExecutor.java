package com.iqiyi.video.download.engine.task;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.video.module.download.exbean.XTaskBean;

/**
 * <pre>
 * 渐变式任务执行器的抽象类。
 * 相比于XBaseTaskExecutor，在TODO和DOING之间多了1个渐变状态:STARTING。
 * 适用于启动和暂停时需要长时间异步操作的任务
 * 注意:
 * 1.状态机：有六个状态TODO,DOING,DONE,ERROR,STARTING,PAUSING
 *           和七个行为start,startFinish,pause,pauseFinish,abort,endSuccess,endError
 *      start = {@link XTaskBean#STATUS_ERROR} / {@link XTaskBean#STATUS_TODO} to {@link XTaskBean#STATUS_DOING}
 *      startFinish ={@link XTaskBean#STATUS_STARTING} to {@link XTaskBean#STATUS_DOING}
 *      pause = {@link XTaskBean#STATUS_STARTING}  to {@link XTaskBean#STATUS_PAUSING}
 *      abort =  all Status to {@link XTaskBean#STATUS_DONE}
 *      endSuccess ={@link XTaskBean#STATUS_DOING}  to {@link XTaskBean#STATUS_DONE}
 *      endError = {@link XTaskBean#STATUS_DOING} or {@link XTaskBean#STATUS_STARTING}  to {@link XTaskBean#STATUS_ERROR}
 * 2.子类继承时，重写五个行为的回调方法即可：
 *      onStart(),onPause(),onAbort(),onEndSuccess(),onEndError()
 * 3.不允许在onStart()等5个自定义回调方法中，同步调用start()等7个行为方法；
 * User: jasontujun
 * update by yuanzeyao 2015/6/9
 * Date: 13-9-27
 * Time: 上午10:03
 * </pre>
 */
public abstract class XGradualTaskExecutor<B extends XTaskBean>
        extends XBaseTaskExecutor<B> {

    public static final String TAG = "XGradualTaskExecutor";

    public XGradualTaskExecutor(B bean) {
        super(bean);
    }

    public XGradualTaskExecutor(B bean, int status) {
        super(bean, status);
    }

    @Override
    public final int start(int... preStatus) {
        synchronized (this) {
            int oldStatus = getStatus();
            DebugLog.log("ParalleTaskManager-XGra","start(status)>>status = " + getBean().getId() + ">>"+oldStatus);

            if (oldStatus == XTaskBean.STATUS_STARTING || oldStatus == XTaskBean.STATUS_DOING) {
                DebugLog.log("ParalleTaskManager-XGra", "start 当前任务正在启动中...,不要再次启动!");
                return TASK_START_DOING_OR_STARTING;
            }

            if (getStatus() != XTaskBean.STATUS_TODO
                    && getStatus() != XTaskBean.STATUS_ERROR
                    && (preStatus.length == 0
                    || getStatus() != preStatus[0])) {
                DebugLog.log("ParalleTaskManager-XGra", "start 失败>>>" + getBean().getId() + ">>" + getStatus() + "只有处于TODO 或者ERROR或者DEFAULT状态才可以启动");
                return TASK_INVALIDATED_STATUS;
            }

            setStatus(XTaskBean.STATUS_STARTING);
            if (!onStart()) { // 启动失败，直接结束
                DebugLog.log("ParalleTaskManager-XGra", "start 启动失败>>>" + getBean().getId() + ">>" + getStatus() + " onStart失败");
                setStatus(oldStatus);
                return TASK_START_FAIL;
            }
            DebugLog.log("ParalleTaskManager-XGra", "start 启动成功>>>" + getBean().getId() + ">>");

        }

        if (getListener() != null)
            getListener().onStart(getBean());
        return TASK_START_SUCCESS;
    }

    /**
     * 启动完成时调用此方法，此方法会把状态改成STATUS_DOING。
     * 注意:此方法不可以在onStart()中调用，只允许在onStart()外或异步线程中调用。
     *
     * @return
     */
    public final boolean startFinish() {
        DebugLog.log(TAG, "startFinish");
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_STARTING)
                return false;
            setStatus(XTaskBean.STATUS_DOING);
        }
        notifyDoing(-1);
        return true;
    }

    @Override
    public final boolean abort() {
        synchronized (this) {
            if (getStatus() != XTaskBean.STATUS_TODO
                    && getStatus() != XTaskBean.STATUS_DOING
                    && getStatus() != XTaskBean.STATUS_STARTING)
                return false;

            if (!onAbort())
                return false;

            setStatus(XTaskBean.STATUS_DONE);
        }
        if (getListener() != null)
            getListener().onAbort(getBean());
        return true;
    }


}
