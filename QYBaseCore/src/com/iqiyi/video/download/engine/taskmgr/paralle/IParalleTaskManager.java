package com.iqiyi.video.download.engine.taskmgr.paralle;

import com.iqiyi.video.download.engine.taskmgr.serial.ISerialTaskManager;

import org.qiyi.video.module.download.exbean.XTaskBean;


/**
 * 并行任务执行器接口。
 * B表示数据的类型
 */
public interface IParalleTaskManager<B extends XTaskBean>
        extends ISerialTaskManager<B> {

    /**
     * 运行队列是否已空
     *
     * @return 已空则返回true；否则返回false
     */
    boolean isEmptyParallel();

    /**
     * 运行队列是否已满
     *
     * @return 已满则返回true；否则返回false
     */
    boolean isFullParallel();

    /**
     * 是否所有任务都停止
     *
     * @return
     */
    boolean isAllStop();


}
