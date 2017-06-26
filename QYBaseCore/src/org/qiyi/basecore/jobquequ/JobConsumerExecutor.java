package org.qiyi.basecore.jobquequ;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An executor class that takes care of spinning consumer threads and making sure enough is alive.
 * works deeply coupled with {@link JobManager}
 */
public class JobConsumerExecutor {
    private int maxConsumerSize;
    private int minConsumerSize;
    private int loadFactor;
    private final ThreadGroup threadGroup;
    private final Contract contract;
    private final int keepAliveSeconds;
    private final AtomicInteger activeConsumerCount = new AtomicInteger(0);
    // key : id + (isPersistent)
    private final ConcurrentHashMap<String, JobHolder> runningJobHolders;


    public JobConsumerExecutor(Configuration config, Contract contract) {
        this.loadFactor = config.getLoadFactor();
        this.maxConsumerSize = config.getMaxConsumerCount();
        this.minConsumerSize = config.getMinConsumerCount();
        this.keepAliveSeconds = config.getConsumerKeepAlive();
        this.contract = contract;
        threadGroup = new ThreadGroup("JobConsumers");
        runningJobHolders = new ConcurrentHashMap<String, JobHolder>();
    }

    /**
     * creates a new consumer thread if needed.
     */
    public void considerAddingConsumer() {
        doINeedANewThread(false, true);
    }

    private boolean canIDie() {
        if (doINeedANewThread(true, false) == false) {
            return true;
        }
        return false;
    }

    private boolean doINeedANewThread(boolean inConsumerThread, boolean addIfNeeded) {
        //if network provider cannot notify us, we have to busy wait
        if (contract.isRunning() == false) {
            if (inConsumerThread) {
                activeConsumerCount.decrementAndGet();
            }
            return false;
        }

        synchronized (threadGroup) {
            if (isAboveLoadFactor(inConsumerThread) && canAddMoreConsumers()) {
                if (addIfNeeded) {
                    addConsumer();
                }
                return true;
            }
        }
        if (inConsumerThread) {
            activeConsumerCount.decrementAndGet();
        }
        return false;
    }

    private void addConsumer() {
        if (JqLog.isDebugEnabled()) {
            JqLog.d("adding another consumer");
        }
        synchronized (threadGroup) {
            Thread thread = new Thread(threadGroup, new JobConsumer(contract, this));
            activeConsumerCount.incrementAndGet();
            thread.start();
        }
    }

    private boolean canAddMoreConsumers() {
        synchronized (threadGroup) {
            //there is a race condition for the time thread if about to finish
            return activeConsumerCount.intValue() < maxConsumerSize;
        }
    }

    private boolean isAboveLoadFactor(boolean inConsumerThread) {
        synchronized (threadGroup) {
            //if i am called from a consumer thread, don't count me
            int consumerCnt = activeConsumerCount.intValue() - (inConsumerThread ? 1 : 0);
            boolean res =
                    consumerCnt < minConsumerSize ||
                            consumerCnt * loadFactor < contract.countRemainingReadyJobs() + runningJobHolders.size();
            if (JqLog.isDebugEnabled()) {
                JqLog.d("%s: load factor check. %s = (%d < %d)|| (%d * %d < %d + %d). consumer thread: %s", Thread.currentThread().getName(), res,
                        consumerCnt, minConsumerSize,
                        consumerCnt, loadFactor, contract.countRemainingReadyJobs(), runningJobHolders.size(), inConsumerThread);
            }
            return res;
        }

    }

    private void onBeforeRun(JobHolder jobHolder) {
        runningJobHolders.put(createRunningJobHolderKey(jobHolder), jobHolder);
    }

    private void onAfterRun(JobHolder jobHolder) {
        runningJobHolders.remove(createRunningJobHolderKey(jobHolder));
    }

    private String createRunningJobHolderKey(JobHolder jobHolder) {
        return createRunningJobHolderKey(jobHolder.getId(), jobHolder.getBaseJob().isPersistent());
    }

    private String createRunningJobHolderKey(long id, boolean isPersistent) {
        return id + "_" + (isPersistent ? "t" : "f");
    }

    /**
     * returns true if job is currently handled by one of the executor threads
     *
     * @param id         id of the job
     * @param persistent boolean flag to distinguish id conflicts
     * @return true if job is currently handled here
     */
    public boolean isRunning(long id, boolean persistent) {
        return runningJobHolders.containsKey(createRunningJobHolderKey(id, persistent));
    }

    /**
     * contract between the {@link JobManager} and {@link JobConsumerExecutor}
     */
    public static interface Contract {
        /**
         * @return if {@link JobManager} is currently running.
         */
        public boolean isRunning();

        /**
         * should insert the given {@link JobHolder} to related {@link JobQueue}. if it already exists, should replace the
         * existing one.
         *
         * @param jobHolder
         */
        public void insertOrReplace(JobHolder jobHolder);

        /**
         * should remove the job from the related {@link JobQueue}
         *
         * @param jobHolder
         */
        public void removeJob(JobHolder jobHolder);

        /**
         * should return the next job which is available to be run.
         *
         * @param wait
         * @param waitUnit
         * @return next job to execute or null if no jobs are available
         */
        public JobHolder getNextJob(int wait, TimeUnit waitUnit);

        /**
         * @return the number of Jobs that are ready to be run
         */
        public int countRemainingReadyJobs();
    }

    /**
     * a simple {@link Runnable} that can take jobs from the {@link Contract} and execute them
     */
    private class JobConsumer implements Runnable {
        private final Contract contract;
        private final JobConsumerExecutor executor;
        private boolean didRunOnce = false;

        public JobConsumer(Contract contract, JobConsumerExecutor executor) {
            this.executor = executor;
            this.contract = contract;
        }

        @Override
        public void run() {
            boolean canDie;
            do {
                String threadName = null;
                if (DebugLog.isDebug()) {
                    threadName = Thread.currentThread().getName();
                }
                try {
                    if (JqLog.isDebugEnabled()) {
                        if (didRunOnce == false) {
                            JqLog.d("starting consumer %s", Thread.currentThread().getName());
                            didRunOnce = true;
                        } else {
                            JqLog.d("re-running consumer %s", Thread.currentThread().getName());
                        }
                    }
                    JobHolder nextJob;
                    FutureJob job;
                    do {
                        //while 循环局部变量内存分配回收引发内存泄漏问题修正
                        nextJob = null;
                        job = null;
                        nextJob = contract.isRunning() ? contract.getNextJob(executor.keepAliveSeconds, TimeUnit.SECONDS) : null;
                        if (nextJob != null) {
                            executor.onBeforeRun(nextJob);
                            job = new FutureJob(contract, nextJob, new Worker(nextJob));
                            job.run();
                            if (DebugLog.isDebug()) {
                                if (!TextUtils.isEmpty(threadName)) {
                                    Thread.currentThread().setName(threadName);
                                }
                            }
                            executor.onAfterRun(nextJob);
                        }
                    } while (nextJob != null);
                } finally {
                    //to avoid creating a new thread for no reason, consider not killing this one first
                    canDie = executor.canIDie();
                    if (JqLog.isDebugEnabled()) {
                        if (canDie) {
                            JqLog.d("finishing consumer %s", Thread.currentThread().getName());
                        } else {
                            JqLog.d("didn't allow me to die, re-running %s", Thread.currentThread().getName());
                        }
                    }
                }
            } while (!canDie);
        }
    }

    private static class FutureJob extends FutureTask {
        JobHolder jobHolder;
        Contract mContract;

        public FutureJob(@NonNull Contract contract, @NonNull JobHolder jobHolder, @NonNull Callable callable) {
            super(callable);
            this.jobHolder = jobHolder;
            mContract = contract;
        }

        @Override
        protected void done() {
            Object o = null;
            try {
                o = get();
            } catch (InterruptedException e) {
                ExceptionUtils.printStackTrace(e);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                ExceptionUtils.printStackTrace(e);
            }
            //此处不放入上面的try中，防止get()方法异常，jobHoldr会长保留在队列中，引发内存泄漏
            if (o != null && o instanceof JobHolder.JobCallbackResult) {
                JobHolder.JobCallbackResult result = (JobHolder.JobCallbackResult) o;
                if (result.isSafe) {
                    mContract.removeJob(jobHolder);
                    if (jobHolder.getBaseJob() != null) {
                        IJobHandler jobHandler = jobHolder.getBaseJob().getJobHandler();
                        if (jobHandler != null) {
                            jobHandler.postResult(IJobHandler.SUCCESS, result.resultObject);
                        }
                    }
                } else {
                    mContract.insertOrReplace(jobHolder);
                }
            } else {
                mContract.removeJob(jobHolder);
            }
        }
    }

    private static class Worker implements Callable {

        JobHolder jobHolder;

        public Worker(@NonNull JobHolder holder) {
            jobHolder = holder;
        }

        @Override
        public Object call() throws Exception {
            if (jobHolder != null) {
                if (DebugLog.isDebug()) {
                    if (jobHolder.getBaseJob() != null
                            && !TextUtils.isEmpty(jobHolder.getBaseJob().jobTag)) {
                        Thread.currentThread().setName(jobHolder.getBaseJob().jobTag);
                    }
                }
                return jobHolder.safeRun(jobHolder.getRunCount());
            }
            return null;
        }
    }
}
