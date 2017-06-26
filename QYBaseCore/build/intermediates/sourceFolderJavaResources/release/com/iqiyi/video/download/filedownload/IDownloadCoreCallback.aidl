// IDownloadFileCallback.aidl
package com.iqiyi.video.download.filedownload;

// Declare any non-default types here with import statements
import com.iqiyi.video.download.filedownload.FileDownloadExBean;

interface IDownloadCoreCallback {
    //下载器回调消息给UI层
    void callback(in FileDownloadExBean msg);
    //下载器调用本地进程信息
    FileDownloadExBean getMessage(in FileDownloadExBean msg);
}
