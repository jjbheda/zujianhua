package com.iqiyi.video.download.engine.task;

/**
 * Created by songguobin on 2017/1/22.
 */

public interface ITaskSchedule<T> {

    /**
     * 比较两个任务的先后执行顺序。
     * @param task1 第一个任务(比较对象)
     * @param task2 第二个任务(比较对象)
     * @return 比较结果，-1表示第一个task1优先，1表示task2优先，0表示相同优先级。
     */
    int compare(T task1, T task2);

}
