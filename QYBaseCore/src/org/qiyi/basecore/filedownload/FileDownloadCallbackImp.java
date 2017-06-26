package org.qiyi.basecore.filedownload;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;

import java.util.List;

/**
 * @author kangle 下载状态回调的实现，回调将会在 初始化 的线程中 必须要在有Looper的Thread中创建（例如 UI Thread 或者
 *         {@link HandlerThread}）
 */
public class FileDownloadCallbackImp extends FileDownloadCallback.Stub {

    private FileDownloadCallback.Stub stub;

    public FileDownloadCallbackImp() {
        super();

        stub = new FileDownloadCallback.Stub() {

            @Override
            public void onPaused(FileDownloadStatus fileDownloadStatus) throws RemoteException {
                handler.obtainMessage(0, fileDownloadStatus).sendToTarget();
            }

            @Override
            public void onFailed(FileDownloadStatus fileDownloadStatus) throws RemoteException {
                handler.obtainMessage(1, fileDownloadStatus).sendToTarget();
            }

            @Override
            public void onDownloadListChanged(List<FileDownloadStatus> downloadStatusList)
                    throws RemoteException {
                FileDownloadInterface.setInited(true);
                handler.obtainMessage(2, downloadStatusList).sendToTarget();
            }

            @Override
            public void onDownloadProgress(FileDownloadStatus fileDownloadStatus)
                    throws RemoteException {
                handler.obtainMessage(3, fileDownloadStatus).sendToTarget();
            }

            @Override
            public void onCompleted(FileDownloadStatus fileDownloadStatus) throws RemoteException {
                handler.obtainMessage(4, fileDownloadStatus).sendToTarget();
            }
        };
    }

    @Override
    public void onPaused(FileDownloadStatus fileDownloadStatus) throws RemoteException {}

    @Override
    public void onFailed(FileDownloadStatus fileDownloadStatus) throws RemoteException {}

    @Override
    public void onDownloadListChanged(List<FileDownloadStatus> downloadStatusList)
            throws RemoteException {}

    @Override
    public void onDownloadProgress(FileDownloadStatus fileDownloadStatus) throws RemoteException {}

    @Override
    public void onCompleted(FileDownloadStatus fileDownloadStatus) throws RemoteException {}


    /**
     * stub 是下载服务真正的回调，会回调到binder线程，该类通过装饰模式将回调转移到new出FileDownloadCallbackImp的线程中
     * 
     * @return
     */
    public FileDownloadCallback.Stub getInvokeThreadCallback() {
        return stub;
    }

    Handler handler = getHandler(this);

    private static Handler getHandler(FileDownloadCallbackImp fileDownloadCallbackImp) {
        Handler handler = null;
        if (Looper.myLooper() == null) {
            handler = fileDownloadCallbackImp.new MyHandler(Looper.getMainLooper());
        } else {
            handler = fileDownloadCallbackImp.new MyHandler();
        }
        return handler;
    }

    private class MyHandler extends Handler {

        public MyHandler(Looper mainLooper) {
            super(mainLooper);
        }

        public MyHandler() {
            super();
        }

        @Override
        public void handleMessage(Message msg) {

            try {
                switch (msg.what) {
                    case 0:
                        onPaused((FileDownloadStatus) msg.obj);
                        break;
                    case 1:
                        onFailed((FileDownloadStatus) msg.obj);
                        break;
                    case 2:
                        onDownloadListChanged((List<FileDownloadStatus>) msg.obj);
                        break;
                    case 3:
                        onDownloadProgress((FileDownloadStatus) msg.obj);
                        break;
                    case 4:
                        onCompleted((FileDownloadStatus) msg.obj);
                        break;

                    default:
                        break;
                }
            } catch (RemoteException e) {
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
            super.handleMessage(msg);
        }
    }
}
