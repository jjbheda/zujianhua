package com.iqiyi.video.download.engine.taskmgr;

import org.qiyi.video.module.download.exbean.XTaskBean;

import java.util.List;

/**
 * 任务管理器监听接口
 *
 */
public interface IDownloadTaskListener<T extends XTaskBean> {

    /**
     * 添加任务后的回调函数（在UI线程）
     *
     * @param task
     */
    void onAdd(T task);

    /**
     * 批量添加任务后的回调函数（在UI线程）
     *
     * @param tasks 真正添加进队列的任务
     */
    void onAddAll(List<T> tasks);

    /**
     * 删除任务后的回调函数（在UI线程）
     *
     * @param task
     */
    void onRemove(T task);

    /**
     * 批量删除任务后的回调函数（在UI线程）
     *
     * @param tasks
     */
    void onRemoveAll(List<T> tasks);

    /**
     * 启动的回调函数
     * start、resume等操作会触发此回调。
     *
     * @param task
     */
    void onStart(T task);


    /**
     * 暂停的的回调函数。
     * stop、pause等操作会触发此回调。
     *
     * @param task
     */
    void onPause(T task);

    /**
     * 暂停全部的回调
     */
    void onPauseAll();

    /**
     * 当下载器中没有正在下载的任务时，会调用此方法
     */
    void onNoDowningTask();

    /**
     * 完成所有下载任务。
     */
    void onFinishAll();

    /**
     * 执行过程中的回调函数。
     *
     * @param task
     * @param completeSize
     */
    void onDoing(T task, long completeSize);

    /**
     * 执行成功结束的回调函数。
     *
     * @param task
     */
    void onComplete(T task);

    /**
     * 执行失败结束的回调函数。
     *
     * @param task
     * @param errorCode
     */
    void onError(T task, String errorCode);

    void onPrepare();

    void onSDFull(T task);
}
