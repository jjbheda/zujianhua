package com.iqiyi.video.download.filedownload.ipc;

import android.content.Context;
import android.os.RemoteCallbackList;

import com.iqiyi.video.download.engine.downloadcenter.QiyiDownloadCenter;
import com.iqiyi.video.download.engine.downloader.IQiyiDownloader;
import com.iqiyi.video.download.filedownload.FileDownloadExBean;
import com.iqiyi.video.download.filedownload.FileDownloadController;
import com.iqiyi.video.download.filedownload.IDownloadCoreCallback;
import com.iqiyi.video.download.filedownload.QiyiFileDownloader;
import com.iqiyi.video.download.filedownload.db.DBRequestController;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.video.module.download.exbean.FileDownloadObject;

/**
 * Created by songguobin on 2017/3/17.
 */

public class DownloadCoreManager {

    private static final String TAG = "DownloadCoreManager";

    private QiyiDownloadCenter mQiyiDownloadCenter;

    private FileDownloadController mFileDownloadController;

    private IQiyiDownloader<FileDownloadObject> mFileDownloader;

    private DBRequestController mDBRequestController;

    private Context mContext;

    private static DownloadCoreManager downloadCoreManager;

    private RemoteMessageProcesser binderProcesser;

    private RemoteCallbackList<IDownloadCoreCallback> remoteCallbackList = new RemoteCallbackList<IDownloadCoreCallback>();

    public synchronized static DownloadCoreManager getInstance(Context mContext) {

        if (downloadCoreManager == null) {

            downloadCoreManager = new DownloadCoreManager(mContext);

        }

        return downloadCoreManager;

    }

    private DownloadCoreManager(Context mContext) {

        this.mContext = mContext;

    }


    public void init() {

        mQiyiDownloadCenter = new QiyiDownloadCenter(mContext);

        mQiyiDownloadCenter.registerReceiver();

        mDBRequestController = new DBRequestController();

        mDBRequestController.init();

        mFileDownloader = new QiyiFileDownloader(mContext, mDBRequestController);

        mQiyiDownloadCenter.addDownloader(FileDownloadObject.class, mFileDownloader);

        mQiyiDownloadCenter.init();

        mFileDownloadController = new FileDownloadController(mFileDownloader,mContext);

        binderProcesser = RemoteMessageProcesser.getInstance();

        binderProcesser.setRemoteCallbackList(remoteCallbackList);

        binderProcesser.setFileDownloadController(mFileDownloadController);

        mFileDownloadController.init();

    }

    public void destroy() {

        mQiyiDownloadCenter.exit();

        mQiyiDownloadCenter.unregisterReceiver();

        mFileDownloader.exit();
    }


    /////////////////////binder函数/////////////////////////////

    public void sendMessage(FileDownloadExBean msg) {

        if (binderProcesser != null) {
            binderProcesser.processRemoteMessage(msg);
        }

    }


    public FileDownloadExBean getMessage(FileDownloadExBean msg) {

        if (binderProcesser != null) {
            return binderProcesser.processRemoteMessage(msg);
        }
        return null;
    }


    public void registerCallback(IDownloadCoreCallback callback) {

        DebugLog.log(TAG,"registerCallback = " + callback.toString());
        remoteCallbackList.register(callback);

    }

    public void unregisterCallback(IDownloadCoreCallback callback) {
        DebugLog.log(TAG,"unregisterCallback = " + callback.toString());

        remoteCallbackList.unregister(callback);

    }


}
