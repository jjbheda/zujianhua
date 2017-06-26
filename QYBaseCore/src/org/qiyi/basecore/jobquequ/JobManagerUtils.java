package org.qiyi.basecore.jobquequ;

import android.content.Context;
import android.text.TextUtils;

import org.qiyi.android.corejar.debug.DebugLog;

import java.util.List;

/**
 * 多线程任务调度管理工具类
 *
 * @author zhongshan
 * @date 2016-08-08.
 */
public class JobManagerUtils {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private volatile static JobManager sJobManager;

    private JobManagerUtils() {
    }

    /**
     * 初始化任务调度实体类，最好放在application启动时候执行, 请使用{@link #init()}
     *
     * @see #init()
     */
    @Deprecated
    public static void init(Context context) {

        Configuration configuration = new Configuration.Builder(context)
                .minConsumerCount(CPU_COUNT)//always keep at least one consumer alive
                .maxConsumerCount(CPU_COUNT * 2 + 1)//up to CPU_COUNT * 2 + 1 consumers at a time
                .loadFactor(3)//3 jobs per consumer
                .consumerKeepAlive(120)//wait 2 minute
                .showLog()
                .build();
        if (sJobManager == null) {
            synchronized (JobManagerUtils.class) {
                if (sJobManager == null) {
                    sJobManager = new JobManager(context, configuration);
                }
            }
        }
    }

    /**
     * 初始化任务调度实体类，最好放在application启动时候执行
     */
    public static void init() {
        Configuration.Builder builder = new Configuration.Builder()
                .minConsumerCount(CPU_COUNT)//always keep at least one consumer alive
                .maxConsumerCount(CPU_COUNT * 2 + 1)//up to CPU_COUNT * 2 + 1 consumers at a time
                .loadFactor(3)//3 jobs per consumer
                .consumerKeepAlive(120);//wait 2 minute
        if (DebugLog.isDebug()) {
            builder.showLog();
        }
        Configuration configuration = builder.build();
        if (sJobManager == null) {
            synchronized (JobManagerUtils.class) {
                if (sJobManager == null) {
                    sJobManager = new JobManager(configuration);
                }
            }
        }
    }

    public static long addJob(Job job) {
        if (sJobManager == null) {
            if (DebugLog.isDebug()) {
                DebugLog.i("JobManagerUtils", "sJobManager = null .");
            }
            init();
        }
        return sJobManager.addJob(job);
    }

    public static void addJobInBackground(Job job) {
        if (sJobManager == null) {
            if (DebugLog.isDebug()) {
                DebugLog.i("JobManagerUtils", "sJobManager = null .");
            }
            init();
        }
        sJobManager.addJobInBackground(job);
    }

    public static void removeJob(long jobId) {
        if (sJobManager != null) {
            sJobManager.removeJob(jobId);
        }
    }

    /**
     * @param runnable
     */
    public static AsyncJob postRunnable(Runnable runnable) {
        if (runnable != null) {
            return post(runnable, Priority.LOW_MIN, 0, "", "");
        }
        return null;
    }

    /**
     * @param runnable
     * @param delay
     */
    public static AsyncJob postDelay(Runnable runnable, long delay) {
        if (runnable != null) {
            return post(runnable, Priority.LOW_MIN, delay, "", "");
        }
        return null;
    }

    /**
     * 优先级接口
     *
     * @param runnable
     * @param priority 任务优先级
     * @param jobTag   任务名 在DEBUG模式下执行该任务的线程会动态变更为这个名字
     *                 执行完毕后名字会还原。
     *                 同时可以用来查询没有执行的任务列表
     * @return
     */
    public static AsyncJob postPriority(Runnable runnable, int priority, String jobTag) {
        if (runnable != null) {
            return post(runnable, priority, 0, "", jobTag);
        }
        return null;
    }

    /**
     * @param groupId 串行执行的任务，需要传入相同的groupId
     */
    public static AsyncJob postSerial(Runnable runnable, String groupId) {
        if (runnable != null && !TextUtils.isDigitsOnly(groupId)) {
            return post(runnable, Priority.LOW_MIN, 0, groupId, "");
        }
        return null;
    }

    /**
     *完整参数接口
     * @param runnable
     * @param priority 优先级
     * @param delay 延时时间
     * @param groupId 串行执行标志
     * @param jobTag 任务标志
     * @return
     */
    public static AsyncJob post(final Runnable runnable, int priority, long delay, String groupId, String jobTag) {
        if (runnable != null) {
            AsyncJob<Object, Object> asyncJob = new AsyncJob<Object, Object>(Object.class) {
                @Override
                public Object onRun(Object... params) throws Throwable {
                    if (runnable != null) {
                        runnable.run();
                    }
                    return null;
                }
            };
            if (delay > 0) {
                asyncJob.delayInMs(delay);
            }
            if (!TextUtils.isEmpty(groupId)) {
                asyncJob.groupId(groupId);
            }
            if (!TextUtils.isEmpty(jobTag)) {
                asyncJob.jobTag(jobTag);
            }
            if (priority >= Priority.LOW_MIN && priority <= Priority.PLAYER_LOGIC_MAX
                    || priority == Priority.APP_START) {
                asyncJob.priority(priority);
            }
            asyncJob.execute();
            return asyncJob;
        }
        return null;
    }

    public static List<BaseJob> getWaitingJobsByTag(String jobTag) {
        if (sJobManager == null) {
            return null;
        }
        return sJobManager.getWaitingJobsByTag(jobTag);
    }

    public static boolean isJobRunning(long jobId) {
        if (sJobManager == null) {
            return false;
        } else {
            JobStatus status = sJobManager.getJobStatus(jobId);
            if (status == JobStatus.RUNNING) {
                return true;
            }
        }
        return false;
    }
}
