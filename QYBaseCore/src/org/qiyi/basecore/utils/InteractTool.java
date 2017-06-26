package org.qiyi.basecore.utils;

import android.text.TextUtils;

import org.qiyi.android.corejar.debug.DebugLog;

/**
 * Created by kangle on 16/4/28.
 * 工具类，用于与上层交互
 */
public class InteractTool {

    private static ICrashReporter iCrashReporter;

    public static void setCrashReporter(ICrashReporter iCrashReporter) {
        InteractTool.iCrashReporter = iCrashReporter;
    }

    public interface ICrashReporter {
        void randomReportException(String message);

        void randomReportException(String message, int percent);

        void reportJsException(String msg, String stack, String addr);
    }

    public static void randomReportException(DebugLog.IGetLog iGetLog) {
        try {
            if (iCrashReporter != null && iGetLog != null) {
                iCrashReporter.randomReportException(iGetLog.getLog());
            }
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
    }

    /**
     * @param expectionString 异常信息
     * @param percent         0-100 百分比
     */
    public static void randomReportException(String expectionString, int percent) {
        try {
            if (iCrashReporter != null && !TextUtils.isEmpty(expectionString)) {
                iCrashReporter.randomReportException(expectionString, percent);
            }
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
    }

    public static void reportJsException(String msg, String stack, String addr) {
        try {
            if (iCrashReporter != null) {
                iCrashReporter.reportJsException(msg, stack, addr);
            }
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
    }
}
