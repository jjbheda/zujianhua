package org.qiyi.basecore.jobquequ;

import java.io.Serializable;

/**
 * Base class for all of your jobs.
 * If you were using {@link BaseJob}, please move to this instance since BaseJob will be removed from the public api.
 */
public abstract class Job<RequestParams, Result> extends BaseJob<RequestParams, Result> implements Serializable {
    private static final long serialVersionUID = 1L;
    protected transient int priority;
    protected transient long delayInMs;

    protected Job(Params params, Class<Result> resultClassType) {
        this(params.getGroupId(), resultClassType);
        this.priority = params.getPriority();
        this.delayInMs = params.getDelayMs();
    }

    protected Job(String groupId, Class<Result> resultClassType) {
        super(groupId, resultClassType);
    }

    /**
     * used by {@link JobManager} to assign proper priority at the time job is added.
     * This field is not preserved!
     *
     * @return priority (higher = better)
     */
    public final int getPriority() {
        return priority;
    }

    /**
     * used by {@link JobManager} to assign proper delay at the time job is added.
     * This field is not preserved!
     *
     * @return delay in ms
     */
    public final long getDelayInMs() {
        return delayInMs;
    }

}
