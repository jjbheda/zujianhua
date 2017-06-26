package org.qiyi.android.corejar.debug;

/**
 * 日志打印输出
 *
 * @author zhongshan
 * @date 2015-09-20.
 */
public interface ILogPrinter {


    /**
     * 临时修改Log部分信息
     *
     * @param methodCount 设置查看方法调用信息的个数
     */
    ILogPrinter t(int methodCount);

    DebugSettings getSettings();

    void d(String tag, String message, Object... args);

    void e(String tag, String message, Object... args);

    void e(Throwable throwable, String tag, String message, Object... args);

    void w(String tag, String message, Object... args);

    void i(String tag, String message, Object... args);

    void v(String tag, String message, Object... args);

    void wtf(String tag, String message, Object... args);

}
