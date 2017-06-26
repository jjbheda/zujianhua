package org.qiyi.basecore.jobquequ;

/**
 * 需要回调功能的任务
 * 与{@link android.os.AsyncTask}用法相同
 *
 * @author niejunjiang
 *         2017/2/23
 */
public abstract class AsyncJob<RequestParams, Result> extends Job<RequestParams, Result> {

    /**
     * @param resultClassType 后台线程返回值类型，用来检测泛型对象是否与返回一致，如果不传，或者与返回泛型不一致
     *                        则回调方法参数为空对象。
     */
    protected AsyncJob(Class<Result> resultClassType) {
        super("", resultClassType);
    }

    /**
     * 任务在加入队列前执行该方法
     */
    @Override
    public void onAdded() {

    }

    /**
     * 任务失败|或者取消执行该方法
     */
    @Override
    protected void onCancel() {

    }

    /**
     * 任务失败后是否需要重新执行,超过默认重复次数，改方法无效
     */
    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return false;
    }

    /**
     * 该任务的优先级，值越大优先级越高
     */
    public final AsyncJob<RequestParams, Result> priority(int priority) {
        this.priority = priority;
        return this;
    }

    /**
     * 改任务设置的延时时间
     */
    public final AsyncJob<RequestParams, Result> delayInMs(long delayInMs) {
        this.delayInMs = delayInMs;
        return this;
    }

    /**
     * 该任务的集合ID,在同一集合的任务，会串行执行
     */
    public final AsyncJob<RequestParams, Result> groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    /**
     * 是否保证该任务的回调在主线程执行，如果是false,则回调执行在调用线程
     */
    public final AsyncJob<RequestParams, Result> ensureToMain(boolean ensureToMain) {
        this.ensureToMain = ensureToMain;
        return this;
    }

    /**
     * 任务失败重试次数设置
     */
    public final AsyncJob<RequestParams, Result> retryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
        return this;
    }

    /**
     * 设置任务的tag，可以用来查询该tag 没有执行的任务列表
     */
    public final AsyncJob<RequestParams, Result> jobTag(String jobTag) {
        this.jobTag = jobTag;
        return this;
    }

    /**
     * 执行任务，返回任务的唯一id
     */
    public final long execute(RequestParams... params) {
        if (params != null && params.length != 0) {
            this.params = params;
        }
        return JobManagerUtils.addJob(this);
    }

    /**
     * 取消该任务
     */
    public final void cancel() {
        isCancel = true;
        JobManagerUtils.removeJob(jobId);
        onCancel();
    }

    @Override
    public void onPostExecutor(Result result) {

    }
}
