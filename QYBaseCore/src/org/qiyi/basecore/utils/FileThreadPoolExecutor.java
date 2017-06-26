package org.qiyi.basecore.utils;

/**
 * Created by zhangqi on 2017/2/22.
 *   用于file文件线程池队列执行完毕以后 再关闭app 保证数据完整性.
 *    经过测试  如果不执行 shutdown() 的话 isTerminated()  isTerminating() 返回不可靠  ，而shutdown() 又会立即关闭线程池， 也不可用，
 *    这里监控排队队列的数量来实现,保证队列task 都被执行.
 */

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class FileThreadPoolExecutor extends ThreadPoolExecutor {
    private boolean hasFinish = false;
    private Object mLock = new Object();

    public FileThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
         synchronized (mLock) {
            if (this.getQueue().size() == 0)
            {
                this.hasFinish = true;
                mLock.notifyAll();
            }
        }
    }

    public void doWaitFinishTask() {
        synchronized (mLock) {
            if (this.hasFinish == false && this.getQueue().size() > 0) {
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    ExceptionUtils.printStackTrace(e);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}