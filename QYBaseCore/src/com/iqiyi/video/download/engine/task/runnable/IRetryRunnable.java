package com.iqiyi.video.download.engine.task.runnable;

/**
 * <pre>
 * 定义了一套执行和重试流程的Runnable。
 *      1.前期准备工作，执行一次onPreExecute()
 *      2.核心逻辑，多次重试执行doInBackground()
 *        如果执行完成不再重试，则返回true，否则返回false
 *      3.如果是正常结束的(doInBackground返回true或达到重试最大上限)，
 *        则最后回调onPostExecute()
 *      4.如果是被中断的(调用cancel())，则最后回调onCancelled()
 * User: jasontujun
 * Date: 14-4-14
 * Time: 下午8:40
 * </pre>
 */
public interface IRetryRunnable<T> extends Runnable {

    static final int INFINITE_RETRY = -1;

    /**
     * 任务是否在运行
     * @return
     */
    boolean isRunning();

    /**
     * 中断任务的运行(非立即结束)
     */
    void cancel();


    /**
     * 获取当前的重试执行doInBackground的累计次数
     * @return 如果任务是无限重试的，则返回{@link #INFINITE_RETRY}；否则返回当前重试次数
     * @see #INFINITE_RETRY
     */
    long getRetryCount();

    /**
     * 获取重试doInBackground的时间间隔(大于等于0)
     * @param retryCount 当前重试次数(如果是无限重试的，则该值为-1)
     * @return
     * @see #getRetryCount()
     */
    long getRetryInterval(long retryCount);

    /**
     * 获取数据对象
     * @return
     */
    T getBean();

    /**
     * 执行前的准备工作
     * @param bean
     */
    boolean onPreExecute(T bean);

    /**
     * 执行前的准备工作失败的回调
     * @param bean
     */
    void onPreExecuteError(T bean);

    /**
     * 主要执行的工作(会被多次重复执行)
     * @param bean
     * @return 如果任务完成，则返回true；否则返回false，并过段时间重复执行该方法
     */
    boolean onRepeatExecute(T bean);

    /**
     * 执行后的善后工作
     * @param bean
     */
    void onPostExecute(T bean);


    /**
     * 线程被中断的善后工作
     * @param bean
     */
    void onCancelled(T bean);


}
