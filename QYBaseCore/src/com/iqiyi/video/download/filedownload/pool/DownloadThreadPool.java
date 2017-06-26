package com.iqiyi.video.download.filedownload.pool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description:管理下载中的所有用到的线程池
 * 目前下载中用到了两个线程池：
 * DOWNLOAD_POOL 用于所有需要新开线程的地方
 * F4V_POOL 用于F4v的分片下载，由于分片下载要控制
 * 并行下载数量，所以单独使用一个固定线程数的线程池
 * User： yuanzeyao.
 * Date： 2015-08-13 16:08
 */
public class DownloadThreadPool {
    //线程池中最大线程数量,这里控制F4v并行下载的分块
    public static final int THREAD_MAX = 2;
    //specially for downloader process to handle download task
    public static final CancelableThreadPoolExecutor DOWNLOAD_POOL = createCacheThreadPool();
    //specially for f4v download task
    public static final CancelableThreadPoolExecutor F4V_POOL = createFixThreadPool(THREAD_MAX);


    /**
     * 类似{@link Executors#newCachedThreadPool()}
     * @return
     */
    public static synchronized CancelableThreadPoolExecutor createCacheThreadPool() {
        CancelableThreadPoolExecutor mExecutor = new CancelableThreadPoolExecutor(0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "CacheThread#" + mCount.getAndIncrement());
            }
        });
        return mExecutor;
    }


    /**
     * 类似{@link Executors#newFixedThreadPool(int)}
     * @param core_pool_size
     * @return
     */
    public static synchronized CancelableThreadPoolExecutor createFixThreadPool(int core_pool_size) {
        BlockingQueue<Runnable> mPoolWorkQueue =
                new LinkedBlockingQueue<Runnable>();
        CancelableThreadPoolExecutor mExecutor = new CancelableThreadPoolExecutor(core_pool_size, core_pool_size, 0L,
                TimeUnit.SECONDS, mPoolWorkQueue, new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "FixThread#" + mCount.getAndIncrement());
            }
        }, new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                // nop
            }
        });

        return mExecutor;
    }
}
