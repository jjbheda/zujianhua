package com.iqiyi.video.download.engine.task.runnable;


import org.qiyi.basecore.utils.ExceptionUtils;

/**
 * <pre>
 * 无限重试次数的RetryRunnable
 * User: jasontujun
 * Date: 14-4-14
 * Time: 下午8:28
 * </pre>
 */
public abstract class XInfiniteRetryRunnable<T> implements IRetryRunnable<T> {

    private volatile boolean isRunning;
	
    
    protected XInfiniteRetryRunnable() {
        isRunning = true;
    }

    @Override
    public long getRetryCount() {
        return INFINITE_RETRY;
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
        while (isRunning) {
            // 如果任务执行完成或者外部中断，则退出循环
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
            onPostExecute(bean);// 正常结束情况下，调用onPostExecute
        else
            onCancelled(bean);// 被中断情况下，调用onCancelled
    }
}
