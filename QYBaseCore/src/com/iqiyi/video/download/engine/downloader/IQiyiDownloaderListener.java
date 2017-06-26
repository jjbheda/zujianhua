package com.iqiyi.video.download.engine.downloader;

import org.qiyi.video.module.download.exbean.XTaskBean;

import java.util.List;

/**
 * <pre>
 * 爱奇艺下载管理器的监听接口。
 * User: jasontujun
 * Date: 14-9-11
 * Time: 下午5:20
 * </pre>
 */
public interface IQiyiDownloaderListener<B extends XTaskBean> {

    /**
     * 从数据库加载数据完成后的回调函数。
     */
    void onLoad();

    /**
     * 批量添加下载任务后的回调函数
     * @param tasks 成功添加的下载任务
     */
    void onAdd(List<B> tasks);

    /**
     * 批量删除下载任务后的回调函数
     * @param tasks 成功删除的下载任务
     */
    void onDelete(List<B> tasks,int deleteAction);

    /**
     * 批量更新下载任务后的回调函数
     * @param tasks 成功更新的下载任务
     * @param key  跟新类型 如跟新路径或者刷新“新”
     */
    void onUpdate(List<B> tasks,int key);

    /**
     * 启动下载的回调函数
     * @param task 下载任务
     */
    void onStart(B task);

    /**
     * 停止下载的回调函数。
     * @param task 下载任务
     */
    void onPause(B task);

    /**
     * 暂停全部的回调
     */
    void onPauseAll(/**List<B> tasks*/);

    /**
     * 当下载器中没有下载任务时，触发此回调
     */
    void onNoDowningTask();

    /**
     * 下载中的回调函数。
     * @param task 下载任务
     */
    void onDownloading(B task);

    /**
     * 下载成功结束的回调函数。
     * @param task 下载任务
     */
    void onComplete(B task);

    /**
     * 执行失败结束的回调函数。
     * @param task 下载任务
     */
    void onError(B task);

    /**
     * 所有待下载任务都下载完成时会回调此函数。
     */
    void onFinishAll();

    /**
     * 下载过程中，网络中断，会自动暂停下载。
     * 如果没有任务在下载，不会回调此方法。
     */
    void onNoNetwork();

    /**
     * 下载过程中，网络切换为非wifi网络，会自动暂停下载。
     * 如果没有任务在下载，不会回调此方法。
     */
    void onNetworkNotWifi();

    /**
     * 下载过程中，网络切换为wifi网络，会自动恢复下载。
     * 如果没有任务在下载，不会回调此方法。
     */
    void onNetworkWifi();

    /**
     * 插入sd卡。
     */
    void onMountedSdCard();

    /**
     * 拔出sd卡。
     * @param isStop 是否由于拔卡导致当前下载暂停
     */
    void onUnmountedSdCard(boolean isStop);
    
    /**
     * 让所有的视频从已暂停变为等待中  
     */
    void onPrepare();

    /**
     * sd卡满
     * @param task
     */
    void onSDFull(B task);
}
