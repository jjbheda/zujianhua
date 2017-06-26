package org.qiyi.basecore.exit;

import android.content.Context;
import android.content.Intent;

import org.qiyi.basecore.filedownload.FileDownloadInterface;
import org.qiyi.basecore.filedownload.FileDownloadRemoteService;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.pluginlibrary.install.PluginInstallerService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by niejunjiang on 2016/6/16.
 */
public class ProcessKillHelper {

    public interface IExitObserver {
        void doExit(Context context);
    }

    private volatile static ProcessKillHelper mInstance;
    private final static String TAG = ProcessKillHelper.class.toString();

    public static final int PROCESS_KILL_POLICY_LEGACY = 0;
    public static final int PROCESS_KILL_POLICY_NO_PROCESS_SURVIVE = 1;

    private int mPolicy = PROCESS_KILL_POLICY_LEGACY;

    private Context mContext;
    private List<IExitObserver> mObserverList = new ArrayList<IExitObserver>();

    private ProcessKillHelper(Context context) {
        if (context != null) {
            mContext = context.getApplicationContext();
        }
    }

    public static ProcessKillHelper getInstance(Context context) {
        if (mInstance == null) {
            synchronized (ProcessKillHelper.class) {
                if (mInstance == null) {
                    mInstance = new ProcessKillHelper(context);
                }
            }
        }
        return mInstance;
    }

    public void doRealExit() {
        notifyExit(mContext);
        if (mPolicy == PROCESS_KILL_POLICY_NO_PROCESS_SURVIVE) {
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    public void stopRemoteDownLoadService() {
        try {
            mContext.stopService(new Intent(mContext, FileDownloadRemoteService.class));
            FileDownloadInterface.destroyDownloadService(mContext);
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
    }


    public void stopPluginInstallService() {
        Intent intent = new Intent();
        intent.setClass(mContext, PluginInstallerService.class);
        mContext.stopService(intent);
    }

    /**
     * 注册监听应用退出事件
     *
     * @param observer
     */
    public void registerExitObserver(IExitObserver observer) {
        if (observer != null && !mObserverList.contains(observer)) {
            mObserverList.add(observer);
        }
    }

    /**
     * 取消注册监听应用退出事件
     *
     * @param observer
     */
    public void unregisterExitObserver(IExitObserver observer) {
        if (observer != null && mObserverList.contains(observer)) {
            mObserverList.remove(observer);
        }
    }

    /**
     * 通知所有attach的观察者应用退出，关闭的相关service
     *
     * @param context
     */
    private void notifyExit(Context context) {
        if (mObserverList != null && mObserverList.size() > 0) {
            for (IExitObserver observer : mObserverList) {
                observer.doExit(context);
            }
        }
    }

    public int getPolicy() {
        return mPolicy;
    }

    public void setPolicy(int policy) {
        mPolicy = policy;
    }
}
