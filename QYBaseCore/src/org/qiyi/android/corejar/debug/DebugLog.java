package org.qiyi.android.corejar.debug;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import org.qiyi.basecore.jobquequ.JobManagerUtils;
import org.qiyi.basecore.storage.NoPermissionException;
import org.qiyi.basecore.storage.StorageCheckor;
import org.qiyi.basecore.utils.ApplicationContext;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.FileUtils;
import org.qiyi.basecore.utils.StringUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import java.io.File;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
/**
 * open debug
 * 1、检查SD卡文件"com.qiyi.video.debug.log"
 * 2、setIsDebug(true)
 */

public class DebugLog {
    /***
     * Debug开关，由打包工具修改
     * true  表示开启调试日志,内测或开发版本使用，缺省值
     * false 表示关闭调试日志，灰度或正式版本及提供给第三方的包使用
     */
    public static final boolean DEBUG_LOG_SWITCH = true;

    protected static final String TAG = "Qiyi_DebugLog";

    /**
     * Debug总开关
     */
    private static boolean isDebug = DEBUG_LOG_SWITCH;

    private static final long LOG_FILE_MAX_LENGTH = 10L * 1024 * 1024;

    private static boolean isFirstTime = true;

    private static LogInfo sLogInfo = new LogInfo();

    private final static boolean sIsShowTrace = true;

    /**
     * 是否为大播放内核打的包
     */
    private static boolean isForBigCore;

    public final static CircularLogBuffer logBuffer = new CircularLogBuffer();

    public final static CircularLogBuffer viewTraceBuffer = new CircularLogBuffer(64);

    public static boolean isForBigCore() {
        return isForBigCore;
    }

    public static void setForBigCore(boolean isForBigCore) {
        DebugLog.isForBigCore = isForBigCore;
    }

    public static final Map<String, Long> map = new LinkedHashMap<>();

    /**
     * 播放控制流程的tag
     */
    public static final String PLAY_TAG = "qiyippsplay";

    /**
     * 投递统计的tag
     */
    public static final String STAT_TAG = "QiYiStatistics";

    /**
     * 纯数据对象的tag
     */
    public static final String POJO_TAG = "QiYiData";

    /**
     * 插件控制流程的tag
     */
    public static final String APK_TAG = "apkPlayer";

    public static final String NATIVIE_LOG_TAG = "nativieLog";

    /**
     * set the value of isDebug. default is false;
     *
     * @param b
     */
    public static void setIsDebug(boolean b) {
        isDebug = b;
        PluginDebugLog.setIsDebug(b);
    }

    public static boolean isDebug() {
        return isDebug;
    }

    public static void enableLogBuffer(boolean enable) {
        logBuffer.enabled = enable;
    }

    public static void setLogSize(int logSize) {
        logBuffer.logSize = logSize;
    }

    public static void log(String LOG_CLASS_NAME, Object msg) {
        if (!StringUtils.isEmpty(LOG_CLASS_NAME) && null != msg) {
            if (isDebug()) {
                printLog(Log.INFO, LOG_CLASS_NAME,
                        "[qiyi_LOG_INFO " + LOG_CLASS_NAME + "] " + String.valueOf(msg),
                        null, 0);
            }
        }
    }

    public static void logLifeCycle(Object object, Object msg) {
        if (object != null && object.getClass() != null && msg != null) {
            String log = String.valueOf(msg);
            /*if(object instanceof android.support.v4.app.Fragment){
                log += (" attached:" + !((android.support.v4.app.Fragment)object).isDetached());
            } else if(object instanceof Fragment){
                log += (" attached:" + !((Fragment)object).isDetached());
            }*/
            String tag = object instanceof String ? String.valueOf(object) : object.getClass().getSimpleName();
            if (isDebug()) {
                printLog(Log.INFO,
                        "qiyi_LifeCycle_LOG",
                        "[qiyi_LifeCycle_LOG]-" + tag + " in lifecycle: " + log,
                        null, 0);
            }
            viewTraceBuffer.log(tag, "I", "[qiyi_LifeCycle_LOG]-" + tag + " in lifecycle: " + log);
        }
    }

    /**
     * log 异常
     *
     * @param tag
     * @param msg 异常消息
     * @param ex  异常
     */
    public static void log(String tag, String msg, Throwable ex) {
        if (!StringUtils.isEmpty(tag) && null != msg) {
            if (isDebug()) {
                if (null == ex) {
                    printLog(Log.ERROR, tag, "[qiyi_LOG_ERROR " + tag + "] " + msg, null, 0);
                } else {
                    printLog(Log.ERROR, tag, "[qiyi_LOG_ERROR " + tag + "] " + msg, ex, 0);
                }
            }
            if (null == ex) {
                logBuffer.log(tag, "E", "[qiyi_LOG_ERROR " + tag + "] " + msg);
            } else {
                logBuffer.log(tag, "E", "[qiyi_LOG_ERROR " + tag + "] " + msg + "\n" + ex.toString());
            }
        }
    }

    public static void log(String TAG, String LOG_CLASS_NAME, Object msg) {
        if (!StringUtils.isEmpty(TAG) && null != msg) {
            if (isDebug()) {
                printLog(Log.INFO, TAG, "[qiyi_LOG_INFO " + LOG_CLASS_NAME + "] " + String.valueOf(msg), null, 0);
            }
        }
    }

    protected void Log(Object msg) {
        log(TAG, msg);
    }

    public static void v(String tag, String message) {
        if (isDebug()) {
            printLog(Log.VERBOSE, tag, message, null, 0);
        }
    }

    public static void v(String tag, String message, int methodCount) {
        if (isDebug()) {
            printLog(Log.VERBOSE, tag, message, null, methodCount);
        }
    }

    public static void i(String tag, String message) {
        if (isDebug()) {
            printLog(Log.INFO, tag, message, null, 0);
        }
    }

    public static void i(String tag, String message, int methosCount) {
        if (isDebug()) {
            printLog(Log.INFO, tag, message, null, methosCount);
        }
    }

    public static void d(String tag, String message) {
        String messageTemp = message;
        if (isDebug()) {
            printLog(Log.DEBUG,tag,message,null,0);
        }
        logBuffer.log(tag, "D", messageTemp);
    }

    public static void d(String tag, String message, int methodCount) {
        String messageTemp = message;
        if (isDebug()) {
            printLog(Log.DEBUG, tag, message, null, methodCount);
        }
        logBuffer.log(tag, "D", messageTemp);
    }

    public static void w(String tag, String message) {
        String messageTemp = message;
        if (isDebug()) {
            printLog(Log.WARN, tag, message, null, 0);
        }
        logBuffer.log(tag, "W", messageTemp);
    }

    public static void w(String tag, String message, int methodCount) {
        String messageTemp = message;
        if (isDebug()) {
            printLog(Log.WARN, tag, message, null, methodCount);
        }
        logBuffer.log(tag, "W", messageTemp);
    }

    public static void e(String tag, String message) {
        String messageTemp = message;
        if (isDebug()) {
            printLog(Log.ERROR, TAG, message, null, 0);
        }
        logBuffer.log(tag, "E", messageTemp);
    }

    public static void e(String tag, String message, int methodCount) {
        String messageTemp = message;
        if (isDebug()) {
            printLog(Log.ERROR, TAG, message, null, methodCount);
        }
        logBuffer.log(tag, "E", messageTemp);
    }

    public static void v(String tag, String category, String message) {
        if (isDebug()) {
            StackTraceElement stack[] = Thread.currentThread().getStackTrace();
            String messageTemp =
                    stack[3].getClassName() + "." + stack[3].getMethodName() + "()<"
                            + stack[3].getLineNumber() + "> : " + category + ">> " + message;
            v(tag, messageTemp);
        }
    }

    public static void i(String tag, String category, String message) {
        if (isDebug()) {
            StackTraceElement stack[] = Thread.currentThread().getStackTrace();
            String tempMessage =
                    stack[3].getClassName() + "." + stack[3].getMethodName() + "()<"
                            + stack[3].getLineNumber() + "> : " + category + ">> " + message;
            i(tag, tempMessage);
        }
    }

    public static void d(String tag, String category, String message) {
        String messageTemp = message;
        if (isDebug()) {
            StackTraceElement stack[] = Thread.currentThread().getStackTrace();
            messageTemp =
                    stack[3].getClassName() + "." + stack[3].getMethodName() + "()<"
                            + stack[3].getLineNumber() + "> : " + category + ">> " + message;
            d(tag, messageTemp);
        }
        logBuffer.log(tag, "D", category + ">> " + messageTemp);
    }

    public static void w(String tag, String category, String message) {
        String messageTemp = message;
        if (isDebug()) {
            StackTraceElement stack[] = Thread.currentThread().getStackTrace();
            messageTemp =
                    stack[3].getClassName() + "." + stack[3].getMethodName() + "()<"
                            + stack[3].getLineNumber() + "> : " + category + ">> " + message;
            w(tag, messageTemp);
        }
        logBuffer.log(tag, "W", category + ">> " + messageTemp);
    }

    public static void e(String tag, String category, String message) {
        String messageTemp = message;
        if (isDebug()) {
            StackTraceElement stack[] = Thread.currentThread().getStackTrace();
            messageTemp =
                    stack[3].getClassName() + "." + stack[3].getMethodName() + "()<"
                            + stack[3].getLineNumber() + "> : " + category + ">> " + message;
            e(tag, messageTemp);
        }
        logBuffer.log(tag, "E", category + ">> " + messageTemp);
    }

    /**
     * 打印对象的所有属性和值
     *
     * @param obj
     */
    public static void printObjFileds(Object obj) {
        printObjFileds(STAT_TAG, obj);
    }

    /**
     * 打印对象的所有属性和值
     *
     * @param tag
     * @param obj
     */
    public static void printObjFileds(String tag, Object obj) {
        if (!isDebug()) {
            return;
        }

        if (null == obj) {
            Log.w(POJO_TAG, tag + "====== obj is null ======");
            return;
        }

        Field[] fields = obj.getClass().getDeclaredFields();
        if (null != fields) {
            Log.i(POJO_TAG, tag + "====== " + obj.getClass().getName() + " ======");
            Log.i(POJO_TAG, tag + "============================== start ================================");

            try {
                for (Field f : fields) {
                    f.setAccessible(true);
                    if (f.getModifiers() < 8) {//只打印  public|private|protected 修饰的字段
                        Log.i(POJO_TAG, tag + " | " + f.getName() + " = " + f.get(obj));
                    }
                }
            } catch (IllegalAccessException e) {
                ExceptionUtils.printStackTrace(e);
            } catch (IllegalArgumentException e) {
                ExceptionUtils.printStackTrace(e);
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }

            Log.i(POJO_TAG, tag + "============================== end ================================");
        }

        /*
         * JAVA 反射机制中，Field的getModifiers()方法返回int类型值表示该字段的修饰符。
         * 其中，该修饰符是java.lang.reflect.Modifier的静态属性。
         *
         * 对应表如下：
         *
         * PUBLIC       : 1
         * PRIVATE      : 2
         * PROTECTED    : 4
         * STATIC       : 8
         * FINAL        : 16
         * SYNCHRONIZED : 32
         * VOLATILE     : 64
         * TRANSIENT    : 128
         * NATIVE       : 256
         * INTERFACE    : 512
         * ABSTRACT     : 1024
         * STRICT       : 2048
         *
         * 当一个方法或者字段被多个修饰符修饰的时候， getModifiers()返回的值等会各个修饰符累加的和
         */
    }

    public static boolean isPluginDebugEnvironment() {
        return false;
    }


    /**
     * 封装拼log的行为
     */
    public interface IGetLog {
        String getLog();
    }


    public static void saveToFile(final IGetLog iGetLog, final String fileName) {

        if (iGetLog != null) {

            JobManagerUtils.postRunnable(new Runnable() {
                @Override
                public void run() {
                    if (ApplicationContext.app != null && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                            ) {
                        String logTxt = iGetLog.getLog();
                        if (StringUtils.isEmpty(logTxt)) {
                            return;
                        }

                        if (isFirstTime) {
                            isFirstTime = false;
                        }

                        d("saveToFile", "start saving: " + fileName);

                        File filePath = StorageCheckor.getInternalStorageFilesDir(ApplicationContext.app, "DebugLog");

                        File file = new File(filePath, fileName);
                        FileUtils.string2File(logTxt, file.getAbsolutePath(), file.length() < LOG_FILE_MAX_LENGTH);
                    }
                }
            });
        }
    }
    public static void addLog(int type, final String logStr) {
        sLogInfo.addLog(type, logStr, System.currentTimeMillis());
        d(LogInfo.class.getSimpleName(), new IGetLog() {
            @Override
            public String getLog() {
                return "addLog: \n" + logStr;
            }
        }.getLog());
    }

    /**
     * @param type
     * @param iGetLog 实现该接口可以使得拼接日志的操作在非UI线程中执行（off main thread）
     */
    public static void addLog(final int type, final IGetLog iGetLog) {
        if (iGetLog != null) {
            JobManagerUtils.postRunnable(new Runnable() {
                @Override
                public void run() {
                    addLog(type, iGetLog.getLog());
                }
            });
        }
    }

    /**
     * @return might be empty
     */
    public static String getFeedBackLog() {
        String ret;
        long currentTimeMillis = System.currentTimeMillis();
        ret = sLogInfo.getAndClearFeedBackLog();
        d("getFeedBackLog", "takes: " + (System.currentTimeMillis() - currentTimeMillis) + " length is " + ret.length());
        return ret;
    }

    private static void printLog(int logType, String tag, String message, Throwable e, int methodCount) {
        if (isDebug() && !TextUtils.isEmpty(tag) && null != message) {
            if (methodCount > 0 && sIsShowTrace) {
                Logger.setTempMethodCount(5);
            }
            switch (logType) {
                case android.util.Log.ERROR:
                    if (e != null) {
                        Logger.e(tag, message);
                    } else {
                        Logger.e(tag, message, e);
                    }
                    break;
                case android.util.Log.VERBOSE:
                    Logger.v(tag, message);
                    break;
                case android.util.Log.INFO:
                    Logger.i(tag, message);
                    break;
                case android.util.Log.WARN:
                    Logger.w(tag, message);
                    break;
                //默认debug
                case android.util.Log.DEBUG:
                default:
                    Logger.d(tag, message);
                    break;
            }
        }
    }

    /**
     * 文件检查打开日志开关，搜索框后门启动阶段某些日志无法抓取，isLogabble 某些机型有bug
     * @param context
     */
    public static void checkIsOpenDebug(final Context context) {
        i(NATIVIE_LOG_TAG, "checkIsOpenDebug > isDebug = " + isDebug);
        JobManagerUtils.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (!isDebug) {
                    File extFile = null;
                    try {
                        // SD卡根目录
                        extFile = StorageCheckor.getStoragePublicDir(context, "");
                    } catch (NoPermissionException e) {
                        // Android/data/{package_name}/files目录
                        extFile = StorageCheckor.getInternalStorageFilesDir(context, "");
                    }
                    if (extFile != null) {
                        String logFileName = extFile.getAbsolutePath() + "/com.qiyi.video.debug.log";
                        File debugFile = new File(logFileName);
                        //修复旧版bug, 删除log文件夹
                        if (debugFile.exists() && debugFile.isDirectory()) {
                            File[] files = debugFile.listFiles();
                            if (files != null && files.length > 0) {
                                for (File file : files) {
                                    file.delete();
                                }
                            }
                            debugFile.delete();    //删除文件夹
                        }
                        isDebug = debugFile.exists() && debugFile.isFile();
                    }
                    Log.d(NATIVIE_LOG_TAG, "log file exist  = " + isDebug);
                }
            }
        });
    }
}
