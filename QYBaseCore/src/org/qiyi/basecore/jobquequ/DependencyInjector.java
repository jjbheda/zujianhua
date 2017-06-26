package org.qiyi.basecore.jobquequ;

/**
 * interface that can be provided to {@link JobManager} for dependency injection
 * it is called before the job's onAdded method is called. for persistent jobs, also run after job is brought
 * back from disk.
 */
public interface DependencyInjector {
    public void inject(BaseJob job);
}
