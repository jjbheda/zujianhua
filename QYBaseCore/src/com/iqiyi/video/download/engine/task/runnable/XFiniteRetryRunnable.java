package com.iqiyi.video.download.engine.task.runnable;


import org.qiyi.basecore.utils.ExceptionUtils;

/**
 * <pre>
 * 有限重试次数的RetryRunnable
 * User: jasontujun
 * Date: 14-4-14
 * Time: 下午8:50
 * </pre>
 */
public abstract class XFiniteRetryRunnable<T> implements IRetryRunnable<T> {

    private volatile boolean isRunning;
    private long maxRetryCount;// 最大重试次数
    private long retryCount;

    protected XFiniteRetryRunnable(long max) {
        maxRetryCount = Math.max(max, 1);
        retryCount = 0;
        isRunning = true;
    }

    protected long getMaxRetryCount() {
        return maxRetryCount;
    }

    @Override
    public long getRetryCount() {
        return retryCount;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void cancel() {
        isRunning = false;
    }

    @Override
    public final void run() {
        T bean = getBean();
        int retry = 0;
    	long times = 0;
        // 准备活动
        if (!onPreExecute(bean)) {
            if (!isRunning) {
                onCancelled(bean);
            } else {
                onPreExecuteError(bean);
            }
            return;
        }

        // 核心重试逻辑
        long interval;
        while (isRunning && retryCount <= maxRetryCount) {
            if (onRepeatExecute(bean) || !isRunning)
                break;
   
        	try {
				interval = Math.max(getRetryInterval(getRetryCount()), 0);
				retry = 0;
				times = interval / 100;
				while (isRunning && retry < times) {
					Thread.sleep(100);
					retry++;
				}
			} catch (InterruptedException e) {
                ExceptionUtils.printStackTrace(e);
                Thread.currentThread().interrupt();
            }
        }

        // 结束
        if (isRunning)
            onPostExecute(bean);
        else
            onCancelled(bean);
    }
}
