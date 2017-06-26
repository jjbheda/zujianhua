package com.iqiyi.video.download.filedownload;

import org.qiyi.video.module.download.exbean.FileDownloadObject;

/**
 * Created by songguobin on 2016/12/30.
 *
 *  单个文件下载
 *
 */

public interface FileDownloadCallback {

    public static final int CALLBACK_MSG_ON_START = 100;

    public static final int CALLBACK_MSG_ON_DOWNLOADING = 101;

    public static final int CALLBACK_MSG_ON_COMPLETE = 102;

    public static final int CALLBACK_MSG_ON_ERROR = 103;

    public static final int CALLBACK_MSG_ON_ABORT = 104;


    /**
     * 开始下载
     *
     * @param bean
     */
    void onStart(FileDownloadObject bean);

    /**
     * 下载中
     *
     * @param bean
     */
    void onDownloading(FileDownloadObject bean);

    /**
     * 下载完成
     *
     * @param bean
     */
    void onComplete(FileDownloadObject bean);

    /**
     * 下载出错
     *
     * @param bean
     */
    void onError(FileDownloadObject bean);

    /**
     * 下载中断
     *
     * @param bean
     */
    void onAbort(FileDownloadObject bean);

}
