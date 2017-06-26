package org.qiyi.basecore.filedownload;

import org.qiyi.basecore.filedownload.FileDownloadCallback;
import org.qiyi.basecore.filedownload.FileDownloadStatus;

interface FileDownloadRemoteServiceInterface {

   	/**
	 * 注册监听
     */
    void registerCallback(FileDownloadCallback cb, String type);
    
    /**
     * 注销监听
     */
    void unregisterCallback(FileDownloadCallback cb, String type);
    
	/**
     * 添加下载
     */
    void addDownload(in List<FileDownloadStatus> fileDownloadStatusList);
    
	/**
     * 暂停下载
     */
    void pauseDownload(in FileDownloadStatus fileDownloadStatus);
    
	/**
     * 继续下载
     */
    void resumeDownload(in FileDownloadStatus fileDownloadStatus);
    
	/**
     * 删除下载
     */
    void deleteDownloads(in List<FileDownloadStatus> fileDownloadStatusList);
    
	/**
     * 取得下载集合
     */
    List<FileDownloadStatus> getDownloads();
}