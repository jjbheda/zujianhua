package org.qiyi.basecore.utils;

import android.text.TextUtils;

import org.qiyi.android.corejar.debug.DebugLog;

/**
 * Created by songguobin on 2017/6/13.
 */

public class ExceptionUtils {

    private static final String TAG = "ExceptionUtils";

    public static void printStackTrace(Exception e) {

        if (e != null && e.getMessage() != null) {
            DebugLog.d(TAG, e.getMessage());
        }

    }

    public static void printStackTrace(String customTag,Exception e){
        if (e != null && e.getMessage() != null) {
            if(!TextUtils.isEmpty(customTag)){
                DebugLog.d(customTag, e.getMessage());
            }else{
                DebugLog.d(TAG, e.getMessage());
            }
        }
    }

    public static void printStackTrace(Error e) {

        if (e != null && e.getMessage() != null) {
            DebugLog.d(TAG, e.getMessage());
        }

    }

    public static void printStackTrace(Throwable e){
        if (e != null && e.getMessage() != null) {
            DebugLog.d(TAG, e.getMessage());
        }
    }
}
