package org.qiyi.basecore.dynamicload;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.dynamicload.InjectTask.InjectResult;
import org.qiyi.basecore.filedownload.FileDownloadCallbackImp;
import org.qiyi.basecore.filedownload.FileDownloadInterface;
import org.qiyi.basecore.filedownload.FileDownloadStatus;
import org.qiyi.basecore.filedownload.FileDownloadStatus.DownloadConfiguration;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.webkit.URLUtil;

public class DynamicLoader {
    private static final String TAG = DynamicLoader.class.getSimpleName();

    /**
     * Inject file for so, Java(dex, apk, zip) or zip file which include so, java.
     * the file will download from the url.
     * 
     * @param context
     * @param url url to get the original plugin file
     * @param auth auth interface to privilege the original file
     * @param callback call back after finish load
     * @param asynchron do this task asyn or not
     */
    public static void inject(Context context, String url, IFileAuthenticator auth,
            LoadFinishCallback callback, boolean asynchron) {
        if (null == context || !URLUtil.isValidUrl(url)) {
            List<InjectResult>  result = new ArrayList<InjectResult>();
            result.add(new InjectResult(null, InjectResult.INVALIDATE_PARAM,
                    PluginFile.FILE_TYPE_UNKNOWN));
            postResult(getHandler(), callback, result);
            return;
        }
        // After download finish will start inject
        downloadFile(context, url, auth, callback, asynchron);
    }

    /**
     * Inject file for so, Java(dex, apk, zip) or zip file which include so, java
     * 
     * @param context
     * @param originalFile local file which will be injected
     * @param url corresponding with the originalFile, maybe null for local file
     * @param auth auth interface to privilege the original file
     * @return inject result for all single file
     * @param callback call back after finish load
     * @param asynchron do this task asyn or not
     */
    public static void inject(Context context, File originalFile, String url,
            IFileAuthenticator auth, LoadFinishCallback callback, boolean asynchron) {
        InjectTask task = new InjectTask(context, originalFile, url, auth, getHandler(), callback);
        if (asynchron) {
            task.start();
        } else {
            task.run();
        }
    }

    private static Handler getHandler() {
        Handler handler = null;
        if (Looper.myLooper() == null) {
            handler = new Handler(Looper.getMainLooper());
            DebugLog.w(TAG,
                    "Invoke inject's thread havn't init looper, forward the result to main thread");
        } else {
            handler = new Handler();
        }
        return handler;
    }

    private static void postResult(Handler handler, final LoadFinishCallback loadFinishCb,
            final List<InjectResult> result) {
        if (null != handler && null != loadFinishCb) {
            handler.post(new Runnable() {

                @Override
                public void run() {
                    loadFinishCb.onLoadFinish(result);
                }
            });
        }
    }

    private static File downloadFile(final Context context, final String url, final IFileAuthenticator auth,
            final LoadFinishCallback callback, final boolean asynchron) {
        if(FileDownloadInterface.isInited()){
            downloadImpl(context, url, auth, callback, asynchron);
        } else{
            FileDownloadInterface.initFileDownloadService(context, new FileDownloadCallbackImp() {

                @Override
                public void onDownloadListChanged(List<FileDownloadStatus> downloadStatusList)
                        throws RemoteException {
                    downloadImpl(context, url, auth, callback, asynchron);
                }

            }.getInvokeThreadCallback());
        }
        return null;
    }

    private static void downloadImpl(final Context context, final String url,
            final IFileAuthenticator auth, final LoadFinishCallback callback,
            final boolean asynchron) {
        FileDownloadInterface downloadApi =
                new FileDownloadInterface(new FileDownloadCallbackImp() {
                    @Override
                    public void onDownloadProgress(FileDownloadStatus fileDownloadStatus)
                            throws RemoteException {
                        DebugLog.d(TAG, "Download: " + url + " progress "
                                + fileDownloadStatus.getDownloadPercent());
                    }

                    @Override
                    public void onCompleted(FileDownloadStatus fileDownloadStatus)
                            throws RemoteException {
                        if (null == fileDownloadStatus.getDownloadedFile()) {
                            List<InjectResult> result = new ArrayList<InjectResult>();
                            result.add(new InjectResult(null, InjectResult.DOWNLOAD_FAILED,
                                    PluginFile.FILE_TYPE_UNKNOWN));
                            postResult(getHandler(), callback, result);
                            return;
                        }
                        inject(context, fileDownloadStatus.getDownloadedFile(), url, auth,
                                callback, asynchron);
                    }

                    @Override
                    public void onFailed(FileDownloadStatus fileDownloadStatus)
                            throws RemoteException {
                        List<InjectResult> result = new ArrayList<InjectResult>();
                        result.add(new InjectResult(null, InjectResult.DOWNLOAD_FAILED,
                                PluginFile.FILE_TYPE_UNKNOWN));
                        postResult(getHandler(), callback, result);
                        DebugLog.d(TAG, "Download: " + url + " failed.");
                    }
                }.getInvokeThreadCallback());
        FileDownloadStatus.DownloadConfiguration configuration =
                new DownloadConfiguration(url, null, null, null, true, false, false, null);

        List<DownloadConfiguration> configurations =
                new ArrayList<FileDownloadStatus.DownloadConfiguration>(2);
        configurations.add(configuration);
        downloadApi.addDownloads(configurations);
    }

    /**
     * Callback after finish load all plugins
     */
    public interface LoadFinishCallback {
        /**
         * Callback after finish load all plugins
         * 
         * @param result all plugin result for each individual plugin file
         */
        void onLoadFinish(List<InjectResult> result);
    }
}
