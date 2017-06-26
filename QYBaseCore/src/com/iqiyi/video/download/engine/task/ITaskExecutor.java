package com.iqiyi.video.download.engine.task;

import org.qiyi.video.module.download.exbean.XTaskBean;

/**
 * <pre>
 * 任务执行器的接口。
 * 每个任务执行类包含一个任务数据类，
 * B表示任务数据的类型，B继承自TaskBean。
 * User: jasontujun
 * update by yuanzeyao 15-05-27
 * Date: 13-9-27
 * Time: 上午9:34
 * </pre>
 */
public interface ITaskExecutor<B extends XTaskBean> {

    public static final int TASK_START_SUCCESS=1;

    public static final int TASK_START_FAIL=2;

    public static final int TASK_START_DOING_OR_STARTING =3;

    public static final int TASK_INVALIDATED_STATUS=4;

    public static final int TASK_GIVEN_STATE_ERROR=5;

    public static final int TASK_PAUSE_SUCCESS=8;

    public static final int TASK_PAUSE_FAIL=9;

    public static final int TASK_PAUSE_ERROR_TASK=10;




    /**
     * 开始或继续下载。
     * @return 开始或继续下载是否成功
     */
    int start(int... preStatus);

    /**
     * 暂停下载（变为等待中或者暂停中）
     * @param postStatus 设置暂停后的状态
     *                   如果postStatus为{@link XTaskBean#STATUS_DEFAULT},那么变为“暂停中”
     *                   如果postStatus不传或者{@link XTaskBean#STATUS_TODO},那边变为“等待中”
     * @return
     *                   暂停下载是否成功
     */
    int pause(int... postStatus);
    
    /**
     * 终止并清除下载任务（删除相关内存和文件中的数据）。
     * @return 终止并清除下载任务是否成功
     */
    boolean abort();

    /**
     * 获取任务的数据bean
     * @return
     */
    B getBean();

    /**
     * 获取任务的唯一Id。
     * @return
     */
    String getId();

    /**
     * 设置下载任务的状态
     * @param status
     */
    void setStatus(int status);

    /**
     * 获取任务的状态。
     * @return
     */
    int getStatus();

    /**
     * 设置监听
     * @param listener
     */
    void setListener(ITaskListener<B> listener);

    /**
     * 获取监听
     * @return
     */
    ITaskListener<B> getListener();

    /**
     * 拿到下载大小
     * @return
     */
    long getCompleteSize();
}
