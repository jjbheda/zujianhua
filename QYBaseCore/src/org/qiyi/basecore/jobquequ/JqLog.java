package org.qiyi.basecore.jobquequ;

import android.util.Log;

/**
 *  by default, logs to nowhere
 */
public class JqLog {

    private static final String TAG = "JOBS";

    private static boolean DBG = Configuration.SHOWLOG;

    public static boolean isDebugEnabled() {
        return DBG;
    }

    public static void d(String text, Object... args) {
        Log.d(TAG, String.format(text, args));
    }

    public static void e(Throwable t, String text, Object... args) {
        Log.d(TAG, String.format(text, args), t);
    }

    public static void e(String text, Object... args) {
        Log.d(TAG, String.format(text, args));
    }
}
