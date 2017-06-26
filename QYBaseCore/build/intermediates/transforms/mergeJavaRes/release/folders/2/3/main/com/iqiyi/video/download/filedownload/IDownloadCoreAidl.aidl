// IDownloadFileAidl.aidl
package com.iqiyi.video.download.filedownload;

// Declare any non-default types here with import statements
import com.iqiyi.video.download.filedownload.FileDownloadExBean;
import com.iqiyi.video.download.filedownload.IDownloadCoreCallback;

interface IDownloadCoreAidl {


   void sendMessage(in FileDownloadExBean msg);

   FileDownloadExBean getMessage(in FileDownloadExBean msg);


   void registerCallback(IDownloadCoreCallback callback);

   void unregisterCallback(IDownloadCoreCallback callback);


}
