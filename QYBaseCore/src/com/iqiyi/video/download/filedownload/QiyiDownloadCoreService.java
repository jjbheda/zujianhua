package com.iqiyi.video.download.filedownload;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.iqiyi.video.download.filedownload.ipc.DownloadCoreManager;

/**
 * Created by songguobin on 2017/2/10.
 *
 * 文件下载service
 *
 */

public class QiyiDownloadCoreService extends Service{

    private Context mContext;

    @Override
    public void onCreate() {

        super.onCreate();

        mContext = this;

        DownloadCoreManager.getInstance(this).init();

    }

    @Override
    public void onDestroy() {

        super.onDestroy();

         DownloadCoreManager.getInstance(this).destroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return initBinder();

    }

    private IDownloadCoreAidl.Stub initBinder(){

        return new IDownloadCoreAidl.Stub() {
            @Override
            public void sendMessage(FileDownloadExBean msg) throws RemoteException {

                DownloadCoreManager.getInstance(mContext).sendMessage(msg);

            }

            @Override
            public FileDownloadExBean getMessage(FileDownloadExBean msg) throws RemoteException {

                return DownloadCoreManager.getInstance(mContext).getMessage(msg);

            }

            @Override
            public void registerCallback(IDownloadCoreCallback callback) throws RemoteException {

                DownloadCoreManager.getInstance(mContext).registerCallback(callback);

            }

            @Override
            public void unregisterCallback(IDownloadCoreCallback callback) throws RemoteException {

                 DownloadCoreManager.getInstance(mContext).unregisterCallback(callback);

            }
        };

    }



}
