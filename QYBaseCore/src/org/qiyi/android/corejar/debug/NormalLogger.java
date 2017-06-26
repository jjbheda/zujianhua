package org.qiyi.android.corejar.debug;

import org.qiyi.android.corejar.debug.DebugLog;

/**
 * 日志打印输出
 * <p/>
 * 与{@link android.util.Log}相比，该类增加了输出最近的函数调用地址功能，方便问题的定位。
 *
 * @author zhongshan
 * @date 2015-09-20.
 */
public class NormalLogger implements ILogPrinter {

    /**
     * It is used to determine log settings such as method count, thread info visibility
     */
    private static final DebugSettings SETTINGS = new DebugSettings();


    @Override
    public ILogPrinter t(int methodCount) {
        return this;
    }

    @Override
    public DebugSettings getSettings() {
        return SETTINGS;
    }

    @Override
    public void d(String tag, String message, Object... args) {
        String msg = createMessage(message, args);
        android.util.Log.d(tag, msg);
    }

    @Override
    public void e(String tag, String message, Object... args) {
        e(null, tag, message, args);
    }

    @Override
    public void e(Throwable throwable, String tag, String message, Object... args) {
        String msg = createMessage(message, args);
        android.util.Log.e(tag, msg);
    }

    @Override
    public void w(String tag, String message, Object... args) {
        String msg = createMessage(message, args);
        android.util.Log.w(tag, msg);
    }

    @Override
    public void i(String tag, String message, Object... args) {
        String msg = createMessage(message, args);
        android.util.Log.i(tag, msg);
    }

    @Override
    public void v(String tag, String message, Object... args) {
        String msg = createMessage(message, args);
        android.util.Log.v(tag, msg);
    }

    @Override
    public void wtf(String tag, String message, Object... args) {
        String msg = createMessage(message, args);
        android.util.Log.wtf(tag, msg);
    }


    private String createMessage(String msg, Object... args) {
        String result = "";
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        int offset = getStackOffset(stack);
        result = "(" + stack[offset].getFileName() + ":" + stack[offset].getLineNumber() + ") " + msg;
        return args.length == 0 ? result : String.format(result, args);
    }

    private int getStackOffset(StackTraceElement[] trace) {
        for (int i = 3; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String name = e.getClassName();
//            if (!name.equals(NormalLogger.class.getName())
//                    && !name.equals(Logger.class.getName())
//                    && !name.equals(DebugLog.class.getName())) {
//                return i;
//            }
            if (!name.contains("Log")) {
                return i;
            }
        }
        return -1;
    }
}
