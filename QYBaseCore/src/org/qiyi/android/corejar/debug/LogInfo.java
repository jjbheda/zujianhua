package org.qiyi.android.corejar.debug;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by kangle on 16/3/9.
 * 封装需要的日志信息，目前提供给
 * 网络（原始流程日志）、
 * 用户反馈之网络失败、
 * 用户反馈之支付问题
 */
public class LogInfo {

    /**
     * 日志类型
     */
    public static final int LOG_TYPE_NETWORK = 0;
    public static final int LOG_TYPE_FEEDBACK_NET = LOG_TYPE_NETWORK + 1;
    public static final int LOG_TYPE_FEEDBACK_PAY = LOG_TYPE_FEEDBACK_NET + 1;

    /**
     * 不同类型日志的缓存上限，单位为byte
     * 0为只存最近一条
     */
    protected static final int BUFFER_LIMIT_NETWORK = 4 * 1024;
    private static final int BUFFER_LIMIT_FEEDBACK_NET = 30 * 1024;
    private static final int BUFFER_LIMIT_FEEDBACK_PAY = 0;

    /**
     * 所有的日志
     */
    private Map<Integer, CertainTypeLogs> allLogs = new ConcurrentHashMap<Integer, CertainTypeLogs>();

    protected CertainTypeLogs addLog(int type, String currentLog, long when) {
        CertainTypeLogs ret = null;
        if (!StringUtils.isEmpty(currentLog)) {
            ALog aLog = new ALog(currentLog, when);
            synchronized (allLogs) {
                ret = allLogs.get(type);
                if (ret == null) {
                    ret = new CertainTypeLogs(type);
                    allLogs.put(type, ret);
                }
            }
            synchronized (ret.logs) {
                int increaseLength = currentLog.length();
                ret.removeOldestIfReachLimit(increaseLength);
                ret.logs.add(aLog);
                ret.length += increaseLength;
            }
        }
        return ret;
    }

    protected String getAndClearFeedBackLog() {
        CertainTypeLogs feedbackNet;
        CertainTypeLogs feedbackPay;
        synchronized (allLogs) {
            feedbackNet = allLogs.get(LOG_TYPE_FEEDBACK_NET);
            feedbackPay = allLogs.get(LOG_TYPE_FEEDBACK_PAY);
/*            if (feedbackNet != null) {
                allLogs.remove(feedbackNet);
            }
            if (feedbackPay != null) {
                allLogs.remove(feedbackPay);
            }*/
        }
        return (feedbackNet == null ? "" : feedbackNet.getLogStrAndClear(true)) + "\n"
                + (feedbackPay == null ? "" : feedbackPay.getLogStrAndClear(true));
    }

    /**
     * 一条日志
     */
    private class ALog {
        private String logStr;
        private long when;

        private ALog(String logStr, long when) {
            this.logStr = logStr;
            this.when = when;
        }
    }

    /**
     * 同一类日志
     */
    protected class CertainTypeLogs {
        private List<ALog> logs = new CopyOnWriteArrayList<>();

        /**
         * 目前记录的该类型日志总长度，用于控制日志缓存大小
         */
        protected int length;

        private int maxLength;

        public CertainTypeLogs(int type) {
            this.maxLength = getMaxLength(type);
        }

        /**
         * warn 这个方法有些耗时，最好不要在主线程调用
         *
         * @param isFirstTime 首次记录需要添加一个日志头
         * @return
         */
        protected String getLogStrAndClear(boolean isFirstTime) {

            List<ALog> currentLogs;
            synchronized (logs) {
                currentLogs = new ArrayList<>(logs);
                logs.clear();
                length = 0;
            }

            String earliestDate = null;
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.ms");
            StringBuffer content = new StringBuffer();
            for (ALog aLog : currentLogs) {
                Date logDate = new Date(aLog.when);
                String logDateStr = formatter.format(logDate);
                if (earliestDate == null) {
                    earliestDate = logDateStr;
                }
                content.append(logDateStr + "  " + aLog.logStr + "\n");
            }

            String prefix = isFirstTime ? "\n************ start at " + earliestDate + " *************\n" : "";
            return prefix + content;
        }

        private void removeOldestIfReachLimit(int increaseLength) {

            while (length + increaseLength > maxLength && logs.size() > 0) {
                ALog aLog = logs.get(logs.size() - 1);
                logs.remove(aLog);
                length -= aLog.logStr.length();

                if (DebugLog.isDebug() && length < 0) {
                    throw new RuntimeException("CertainTypeLogs.removeOldestIfReachLimit::length is below zero");
                }
            }
        }
    }

    private int getMaxLength(int type) {
        int ret = 0;
        switch (type) {
            case LOG_TYPE_FEEDBACK_NET:
                ret = BUFFER_LIMIT_FEEDBACK_NET;
                break;
            case LOG_TYPE_FEEDBACK_PAY:
                ret = BUFFER_LIMIT_FEEDBACK_PAY;
                break;
            case LOG_TYPE_NETWORK:
                ret = BUFFER_LIMIT_NETWORK;
                break;
        }
        return ret;
    }
}
