package org.qiyi.basecore.multiwindow;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import org.qiyi.android.corejar.debug.DebugLog;

import java.util.List;

/**
 * Created by chulili on 2016/10/10.
 *
 * 提供判断当前activity是否处于MultiWindow模式下的方法类
 *
 */
public class MultiWindowManager {

    private final static String TAG = MultiWindowManager.class.getSimpleName();

    private volatile static MultiWindowManager mInstance;
    private boolean mIsSupportMW = true;

    public static MultiWindowManager getInstance() {
        if (null == mInstance) {
            synchronized (MultiWindowManager.class) {
                if (null == mInstance) {
                    mInstance = new MultiWindowManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 当前activity是否处于multiwindow模式下
     * @param activity
     * @return
     */
    public boolean isInMultiWindowMode(Activity activity) {

        if (null == activity) {
            return false;
        }

        DebugLog.v(TAG, "Build.VERSION.SDK_INT == " + Build.VERSION.SDK_INT);

        if (Build.VERSION.SDK_INT > 23 && mIsSupportMW) {
            if (activity.isInMultiWindowMode() ) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断系统是否能够支持multiwindow模式
     * @return
     */
    public boolean isSupportMultiWindow() {
        return mIsSupportMW && Build.VERSION.SDK_INT > 23;
    }

    /**
     * 在7.0以上系统的手机上，由不支持MultiWindow的页面返回的时候调用
     * @param context
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void backToMultWindowActivity(Context context) {

        if (context == null) {
            return;
        }

        if (!isSupportMultiWindow()) {
            return;
        }

        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.AppTask> recentTasks = activityManager.getAppTasks();
        int backTaskId = -1;

        if (recentTasks != null && recentTasks.size() >= 2 && recentTasks.get(1).getTaskInfo().id != -1) {
            backTaskId = recentTasks.get(1).getTaskInfo().id;
        }

        if (backTaskId > 0) {
            activityManager.moveTaskToFront(backTaskId, ActivityManager.MOVE_TASK_WITH_HOME);
        }

    }

}
