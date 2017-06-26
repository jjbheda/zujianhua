package org.qiyi.basecore.db;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import org.qiyi.basecore.utils.ExceptionUtils;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 异步处理task队列，排队处理task
 *
 * @author 揭兴波
 */
public class AsyncTaskQueue extends Thread {

    private final static int MESSAGE_WHAT_CALLBACK = 1;
    private final static int MESSAGE_WHAT_TIMEOUT = MESSAGE_WHAT_CALLBACK + 1;
    private Queue<AbstractTask> taskQueue = new LinkedList<AbstractTask>();
    private boolean isStop = false;
    /* 使用主线程的Looper创建Handler，避免子线程调用该类 */
    private static final Handler sHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WHAT_CALLBACK:
                    ((AbstractTask) msg.obj).callBack();
                    break;
                case MESSAGE_WHAT_TIMEOUT:
                    ((AbstractTask) msg.obj).callBackTimeout();
                    break;
                default:
                    break;
            }
        }

    };

    public AsyncTaskQueue() {
        super("AsyncTaskQueue");
    }

    @Override
    public void run() {
        try {

            AbstractTask task = null;

            while (!isStop) {

                synchronized (taskQueue) {
                    if (taskQueue.isEmpty()) {
                        taskQueue.wait();
                        continue;
                    } else {
                        task = taskQueue.poll();
                    }
                }
                task.process();
                sHandler.removeMessages(MESSAGE_WHAT_TIMEOUT, task);

                Message msg = sHandler.obtainMessage(MESSAGE_WHAT_CALLBACK, task);
                msg.sendToTarget();
            }
        } catch (InterruptedException e) {
            ExceptionUtils.printStackTrace(e);
            Thread.currentThread().interrupt();
        }
    }

    public void stopRun() {
        if (isAlive()) {
            isStop = true;
            this.stop();
        }
    }

    /**
     * 添加一个新任务到队列，并唤醒处理线程
     *
     * @param task
     */
    public void addTask(AbstractTask task) {
        synchronized (taskQueue) {
            taskQueue.offer(task);
            taskQueue.notifyAll();
        }
    }

    /**
     *
     */
    public void addTask(AbstractTask task, int timeout) {
        synchronized (taskQueue) {
            taskQueue.offer(task);
            taskQueue.notifyAll();
            Message msg = sHandler.obtainMessage(MESSAGE_WHAT_TIMEOUT, task);
            sHandler.sendMessageDelayed(msg, timeout);
        }
    }

}
