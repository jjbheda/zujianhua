package com.iqiyi.video.download.engine.downloadcenter;

import com.iqiyi.video.download.engine.downloader.IQiyiDownloader;

import org.qiyi.video.module.download.exbean.XTaskBean;

/**
 * 用于管理各种下载器的接口，如视频下载，游戏下载
 * Created by yuanzeyao on 2015/5/29.
 */
public interface IQiyiDownloadCenter
{
    /**
     * 对各个下载器进行一些初始化工作
     */
    void init();

    /**
     * 注册某种下载器，目前仅添加视频下载器
     * @param clazz
     *          下载类型，如视频下载，游戏下载 @see #DownloadObject.class
     * @param downloader
     *          clazz类型下载对应的下载器
     * @return
     *          添加成功返回true，否则返回false
     */
    <T extends XTaskBean> boolean addDownloader(Class<T> clazz, IQiyiDownloader<T> downloader);

    /**
     * 反注册某种下载器
     * @param clazz
     *        下载类型，如视频下载，游戏下载 @see #DownloadObject.class
     * @return
     */
    <T extends XTaskBean> boolean removeDownloader(Class<T> clazz);

    /**
     * 通过某种下载类型，拿到下载器对象
     * @param clazz
     *         下载类型，如视频下载，游戏下载 @see #DownloadObject.class
     * @return
     *         当存在clazz对应的下载器时，返回对应的下载器，否则返回Null
     */
    <T extends XTaskBean> IQiyiDownloader<T> getDownloader(Class<T> clazz);

    /**
     * 退出时，释放资源
     */
    void exit();

    //void setAutoDownloadCallBack(AutoDownloadController.IAutoDownloadCallBack callBack);

    /**
     * 注册广播(网络 SD卡 自动下载)
     */
    public void registerReceiver();

    /**
     * 取消广播(网络 SD卡 自动下载)
     */
    public void unregisterReceiver();

    /**
     * 尝试自动下载
     */
    //public void tryAutoDownload();
}
