package org.qiyi.basecore.imageloader;

/**
 * @author dragon
 *         用于过滤同一任务启动多遍问题
 */
public abstract class CustomRunnable implements Runnable {
    /**
     * 返回任务唯一标识
     *
     * @return
     */
    public Object getIdentity() {
        return CustomRunnable.this;
    }

    @Override
    public void run() {
    }

    /**
     * @return 回调的key（ImageView obj's key）
     */
    abstract String getSubIdentity();

    /**
     * 给未执行的任务回调结果的机会
     */
    abstract void onResult(Resource<?> bt, boolean isCached);

    /**
     * 从已执行完毕的任务中取出执行结果
     */
    abstract Resource<?> getResult();
}