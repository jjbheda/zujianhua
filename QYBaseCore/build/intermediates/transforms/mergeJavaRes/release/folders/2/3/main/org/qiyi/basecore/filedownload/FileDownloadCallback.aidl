package org.qiyi.basecore.filedownload;

import org.qiyi.basecore.filedownload.FileDownloadStatus;

/**
 * @author kangle
 *  下载变动的回调
 */
interface FileDownloadCallback {
    
    /**
     *  下载队列发生变化，由初始化完毕（从数据库中恢复下载记录成功）、添加、删除引起
     * @param downloadStatusList
     */
    void onDownloadListChanged(in List<FileDownloadStatus> downloadStatusList);
    
    /**
     * 进度发生变化
     * 
     * @param fileDownloadStatus
     */
    void onDownloadProgress(in FileDownloadStatus fileDownloadStatus);

    /**
     * 暂停
     * 
     * @param pausedReason 暂停原因 see PAUSED_XXX in DownloadManager
     */
    void onPaused(in FileDownloadStatus fileDownloadStatus);


    /**
     * 失败
     * 
     * @param failedReason 失败原因 see ERROR_XXX in DownloadManager
     */
    void onFailed(in FileDownloadStatus fileDownloadStatus);

    /**
     * 下载完成，该方法能保证 回调中的 downloaded File 的有效性（不需要额外判断null || file exist等）
     * 
     * @param finishedFile
     */
    void onCompleted(in FileDownloadStatus fileDownloadStatus);
    
}