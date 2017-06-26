package org.qiyi.basecore.utils;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

/**
 * Helper class for user to create handler without UI thread
 */
public class WorkHandler {
    private HandlerThread mWorkThread;

    private Handler mWorkHandler;

    public WorkHandler(String name, Handler.Callback callback, int priority) {
        mWorkThread = new HandlerThread(name, priority);
        mWorkThread.start();
        mWorkHandler = new Handler(mWorkThread.getLooper(), callback);
    }

    public WorkHandler(String name, Handler.Callback callback) {
        this(name, callback, Process.THREAD_PRIORITY_DEFAULT);
    }

    public WorkHandler(String name, int priority) {
        this(name, null, priority);
    }

    public WorkHandler(String name) {
        this(name, null);
    }

    public Handler getWorkHandler() {
        return mWorkHandler;
    }

    /**
     * @return return the handler is alive or not
     */
    public boolean isAlive() {
        try {
            return mWorkThread.isAlive();
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
        return false;
    }

    /**
     * Ask the handler to quit
     *
     * @return true for handler quit successfully
     */
    public boolean quit() {
        if (Build.VERSION.SDK_INT >= 18) {
            return mWorkThread.quitSafely();
        } else {
            return mWorkThread.quit();
        }
    }
}
