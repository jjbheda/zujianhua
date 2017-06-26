package org.qiyi.basecore.filedownload;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Created by kangle on 2015/6/20.
 * 用于实现基于优先级的线程池队列
 */
public class PriorityFutureTask<T> extends FutureTask<T> {
    private Callable callable;

    public PriorityFutureTask(Callable<T> callable) {
        super(callable);
        this.callable = callable;
    }

    public int getPriority() {
        int ret = 0;
        if(callable instanceof PriorityCallable) {
            return ((PriorityCallable) callable).getPriority();
        }
        return ret;
    }

    /**
     * Created by kangle on 2015/8/6.
     * 用于实现有优先级的线程池队列
     */
    public interface PriorityCallable<V> extends Callable<V>{
        int getPriority();
    }
}
