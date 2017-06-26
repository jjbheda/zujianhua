package org.qiyi.basecore.jobquequ;

import android.annotation.SuppressLint;

import org.qiyi.android.corejar.debug.DebugLog;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * This class has been deprecated and will soon be removed from public api.
 * Please use {@link Job} instead which provider a cleaner constructor API.
 * Deprecated. Use {@link Job}
 */
public abstract class BaseJob<RequestParams, Result> {
    private static final int DEFAULT_RETRY_TIMES = 3;
    protected int retryTimes = DEFAULT_RETRY_TIMES;
    protected String groupId;
    protected RequestParams[] params;
    protected Class<Result> resultClassType;
    protected long jobId;
    protected boolean isCancel;
    protected boolean ensureToMain;
    /**
     * 用来查询同一类tag的未执行任务
     */
    protected String jobTag;
    private transient int currentRunCount;


    private  DefaultJobHandler mJobHandler;

    BaseJob(){}


    BaseJob(String groupId) {
        this(groupId, null);
    }

    @SuppressLint("HandlerLeak")


    BaseJob(String groupId, Class<Result> resultClassType) {
        this.groupId = groupId;
        this.resultClassType = resultClassType;


        this.mJobHandler = new DefaultJobHandler(ensureToMain) {
            @Override
            public void postSuccess(Object result) {
                onCallback(result);
            }

            @Override
            public void postFailed() {
                onCancel();
            }
        };
    }



    private void writeObject(ObjectOutputStream oos) throws IOException {
//        oos.writeBoolean(requiresNetwork);
        oos.writeObject(groupId);
    }


    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
//        requiresNetwork = ois.readBoolean();
        groupId = (String) ois.readObject();
    }

    /**
     * defines if we should add this job to disk or non-persistent queue
     *
     * @return
     */
    public final boolean isPersistent() {
        return false;
    }

    /**
     * called when the job is added to disk and committed.
     * this means job will eventually run. this is a good time to update local database and dispatch events
     * Changes to this class will not be preserved if your job is persistent !!!
     * Also, if your app crashes right after adding the job, {@code onRun} might be called without an {@code onAdded} call
     */
    public void onAdded() {
    }


    /**
     * The actual method that should to the work
     * It should finish w/o any exception. If it throws any exception, {@code shouldReRunOnThrowable} will be called to
     * decide either to dismiss the job or re-run it.
     *
     * @throws Throwable
     */
//    abstract public void onRun() throws Throwable;
    abstract public Result onRun(RequestParams... params) throws Throwable;

    final public void onCallback(Object object) {
        if (isCancel) {
            return;
        }
        if (object != null) {
            if (resultClassType != null && resultClassType.isAssignableFrom(object.getClass())) {
                onPostExecutor((Result) object);
            }
        } else {
            onPostExecutor(null);
        }
    }

    public void onPostExecutor(Result result) {
    }


    /**
     * called when a job is cancelled.
     */
    protected void onCancel() {


    }


    /**
     * if {@code onRun} method throws an exception, this method is called.
     * return true if you want to run your job again, return false if you want to dismiss it. If you return false,
     * onCancel will be called.
     */
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return false;
    }

    /**
     * Runs the job and catches any exception
     *
     * @param currentRunCount
     * @return
     */
    public final JobHolder.JobCallbackResult safeRun(int currentRunCount) {
        if (isCancel) {
            return null;
        }
        this.currentRunCount = currentRunCount;
        if (JqLog.isDebugEnabled()) {
            JqLog.d("running job %s", this.getClass().getSimpleName());
        }
        JobHolder.JobCallbackResult result = new JobHolder.JobCallbackResult();
        boolean reRun = false;
        boolean failed = false;
        try {
            result.resultObject = onRun(params);
            result.isSafe = true;
            if (JqLog.isDebugEnabled()) {
                JqLog.d("finished job %s", this.getClass().getSimpleName());
            }
        } catch (Throwable t) {
            failed = true;
            JqLog.e(t, "error while executing job");
            reRun = currentRunCount < getRetryLimit();
            if (reRun) {
                try {
                    reRun = shouldReRunOnThrowable(t);
                } catch (Throwable t2) {
                    JqLog.e(t2, "shouldReRunOnThrowable did throw an exception");
                }
            } else {
                if (DebugLog.isDebug()) {
                    //debug 模式下失败重试完成，依然失败抛出异常
                    throw new RuntimeException(t);
                }
            }
        } finally {
            if (reRun) {
                result.isSafe = false;
            } else if (failed) {
                try {
                    mJobHandler.postResult(IJobHandler.FAILED, null);
                    result.isSafe = true;
                } catch (Throwable ignored) {
                    // ignored
                }
            }
        }
        return result;
    }

    /**
     * before each run, JobManager sets this number. Might be useful for the {@link BaseJob#onRun(Object[])} ()}
     * method
     *
     * @return
     */
    protected int getCurrentRunCount() {
        return currentRunCount;
    }

    /**
     * Some jobs may require being run synchronously. For instance, if it is a job like sending a comment, we should
     * never run them in parallel (unless they are being sent to different conversations).
     * By assigning same groupId to jobs, you can ensure that that type of jobs will be run in the order they were given
     * (if their priority is the same).
     *
     * @return
     */
    public final String getRunGroupId() {
        return groupId;
    }

    /**
     * By default, jobs will be retried {@code DEFAULT_RETRY_LIMIT}  times.
     * If job fails this many times, onCancel will be called w/o calling {@code shouldReRunOnThrowable}
     *
     * @return
     */
    protected int getRetryLimit() {
        return retryTimes;
    }

    public IJobHandler getJobHandler() {
        return mJobHandler;
    }

    public void setParms(RequestParams... params) {
        if (params != null && params.length != 0) {
            this.params = params;
        }
    }

    /**
     * 取消该任务
     */
    public void cancel() {
        isCancel = true;
        JobManagerUtils.removeJob(jobId);
        onCancel();
    }
}
