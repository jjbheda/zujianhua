package com.iqiyi.video.download.filedownload.db;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

import org.qiyi.basecore.utils.ExceptionUtils;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 异步处理task队列，排队处理task
 *
 * @author songguobin
 */
public class AsyncTaskDbQueue extends Thread {

    private final static int MESSAGE_WHAT_CALLBACK = 1;
    private final static int MESSAGE_WHAT_TIMEOUT = MESSAGE_WHAT_CALLBACK + 1;
    private Queue<AbstractDbTask> taskQueue = new LinkedList<AbstractDbTask>();
    private boolean isStop = false;

    @SuppressLint("HandlerLeak")
    private static final Handler sHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WHAT_CALLBACK:
                    ((AbstractDbTask) msg.obj).callBack();
                    break;
                case MESSAGE_WHAT_TIMEOUT:
                    ((AbstractDbTask) msg.obj).callBackTimeout();
                    break;
                default:
                    break;
            }
        }

        ;
    };

    public AsyncTaskDbQueue() {
        super("download_database");
    }

    @Override
    public void run() {
        try {
            AbstractDbTask task = null;

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
    public void addTask(AbstractDbTask task) {
        synchronized (taskQueue) {
            taskQueue.offer(task);
            taskQueue.notifyAll();
        }
    }

    /**
     *
     */
    public void addTask(AbstractDbTask task, int timeout) {
        synchronized (taskQueue) {
            taskQueue.offer(task);
            taskQueue.notifyAll();
            Message msg = sHandler.obtainMessage(MESSAGE_WHAT_TIMEOUT, task);
            sHandler.sendMessageDelayed(msg, timeout);
        }
    }

}
