package org.qiyi.basecore.db;

/**
 * 请求管理者
 */
public class DbOperationWrapper {
    /**
     * 数据库操作队列
     */
    private static AsyncTaskQueue sDatabaseTaskQueue;
    private static Object sInitLock = new Object();

    /**
     * 初始化操作
     */
    private static void tryInit() {
        synchronized (sInitLock) {
            if (null == sDatabaseTaskQueue) {
                sDatabaseTaskQueue = new AsyncTaskQueue();
                // 启动数据库操作线程
                sDatabaseTaskQueue.start();
            }
        }
    }

    /**
     * 向数据库操作队列添加一个任务
     *
     * @param task
     */
    public static void addDBTask(AbstractTask task) {
        tryInit();
        sDatabaseTaskQueue.addTask(task);
    }

    /**
     * 向数据库操作队列添加一个任务,此任务超时限制为timeout毫秒
     *
     * @param task
     */
    public static void addDBTask(AbstractTask task, int timeOut) {
        tryInit();
        sDatabaseTaskQueue.addTask(task, timeOut);
    }
}
