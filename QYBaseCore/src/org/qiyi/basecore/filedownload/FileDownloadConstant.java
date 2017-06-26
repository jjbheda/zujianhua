package org.qiyi.basecore.filedownload;


/**
 * @author kangle
 *  下载中用到的一些常量
 */
public class FileDownloadConstant{

    protected static final int MAX_LOAD = 10;

    /**
     * 尝试开始下载，原因（和暂停原因保持一致） —— 添加（无对应暂停原因，其他原因直接使用了PAUSED_XXX）
     */
    public final static int TRY_START_DOWNLOAD_FOR_ADD = 0;
    
    /**
     * Value of {@link #COLUMN_STATUS} when the download is waiting to start.
     */
    public final static int STATUS_PENDING = 1 << 0;

    /**
     * Value of {@link #COLUMN_STATUS} when the download is currently running.
     */
    public final static int STATUS_RUNNING = 1 << 1;

    /**
     * Value of {@link #COLUMN_STATUS} when the download is waiting to retry or resume.
     */
    public final static int STATUS_PAUSED = 1 << 2;

    /**
     * Value of {@link #COLUMN_STATUS} when the download has successfully completed.
     */
    public final static int STATUS_SUCCESSFUL = 1 << 3;

    /**
     * Value of {@link #COLUMN_STATUS} when the download has failed (and will not be retried).
     */
    public final static int STATUS_FAILED = 1 << 4;
    
    /**
     * 无法创建下载文件
     */
    public final static int ERROR_UNABLE_TO_CREATE_FILE = 1000;
    
    /**
     * 下载url不合法
     */
    public final static int ERROR_DOWNLOAD_URL_INVALID = ERROR_UNABLE_TO_CREATE_FILE + 1;
    
    /**
     * 网络错误
     */
    public final static int ERROR_HTTP_ERROR = ERROR_DOWNLOAD_URL_INVALID + 1;
    
    /**
     * 创建写文件流的时候，文件不存在
     */
    public final static int ERROR_DOWNLOAD_FILE_NOT_FOUND = ERROR_HTTP_ERROR + 1;
    
    /**
     * 写文件流的时候发生异常
     */
    public final static int ERROR_WRITING_DOWNLOAD_FILE = ERROR_DOWNLOAD_FILE_NOT_FOUND + 1;

    /**
     * 网络input stream为null
     */
    public final static int ERROR_NETWORK_NO_INPUT_STREAM = ERROR_WRITING_DOWNLOAD_FILE + 1;

    /**
     * 网络请求返回状态错误
     */
    public final static int ERROR_NETWORK_RESPONSE_CODE_ERROR = ERROR_NETWORK_NO_INPUT_STREAM + 1;

    /**
     * Socket连接超时
     */
    public final static int ERROR_NETWORK_CONNECTION_TIMEOUT = ERROR_NETWORK_RESPONSE_CODE_ERROR + 1
            ;
    /**
     * time out when read
     */
    public final static int ERROR_NETWORK_SOCKET_TIMEOUT = ERROR_NETWORK_CONNECTION_TIMEOUT + 1;

    /**
     * 从本地恢复的数据显示正在下载，这种情况下视为下载失败（可能由于意外退出所导致）
     */
    public final static int ERROR_FROM_RESTORE = ERROR_NETWORK_SOCKET_TIMEOUT + 1;

    /**
     * 未知错误
     */
    public final static int ERROR_VALIDATE_FAILED = ERROR_FROM_RESTORE + 1;

    /**
     * 未知错误
     */
    public final static int ERROR_UNKNOWN = ERROR_VALIDATE_FAILED + 1;

    /**
     * 无网暂停
     */
    public final static int PAUSED_WAITING_FOR_NETWORK = 1;

    /**
     * 非wifi下禁止下载暂停
     */
    public final static int PAUSED_QUEUED_FOR_WIFI = PAUSED_WAITING_FOR_NETWORK + 1;
    
    /**
     * 存储空间不足，下载暂停
     */
    public final static int PAUSED_INSUFFICIENT_SPACE = PAUSED_QUEUED_FOR_WIFI + 1;
    
    /**
     * 手动暂停
     */
    public final static int PAUSED_MANUALLY = PAUSED_INSUFFICIENT_SPACE + 1;
    
    /**
     * 删除造成的首先暂停，需要特殊处理
     */
    public final static int PAUSED_BY_DELETED = PAUSED_MANUALLY + 1;
    
    /**
     * 由 execute 引起的临时暂停（紧接着就会变为 STATUS_RUNNING）
     */
    public final static int PAUSED_BY_OTHER_EXECUTE = PAUSED_BY_DELETED + 1;
    
    /**
     * 由 其他手动执行的任务，达到同时下载的上限 引起的下载转 STATUS_PENDING
     */
    public final static int PAUSED_REACH_MAX_LOAD = PAUSED_BY_OTHER_EXECUTE + 1;
    
    public static String getPausedReasonStr(int reason) {
        String ret = null;
        switch (reason) {
            case PAUSED_WAITING_FOR_NETWORK:
                ret = "无可用网络，请检查网络";
                break;
            case PAUSED_QUEUED_FOR_WIFI:
                ret = "等待切换到WIFI下，会继续为您下载";
                break;
            case PAUSED_INSUFFICIENT_SPACE:
                ret = "存储空间不足";
                break;
            case PAUSED_MANUALLY:
                ret = "点击继续下载";
                break;
            case PAUSED_REACH_MAX_LOAD:
                ret = "达到同时下载上限";
                break;

            default:
                break;
        }
        return ret;
    }

    public static CharSequence getFailedReasonStr(int reason) {

        String ret = null;
        switch (reason) {
            case ERROR_UNKNOWN:
                ret = "下载过程中发生未知错误";
                break;
            case ERROR_DOWNLOAD_FILE_NOT_FOUND:
            case ERROR_WRITING_DOWNLOAD_FILE:
                ret = "文件读写异常";
                break;
            case ERROR_DOWNLOAD_URL_INVALID:
                ret = "下载地址错误";
                break;
            case ERROR_UNABLE_TO_CREATE_FILE:
                ret = "无法创建下载文件";
                break;
            case ERROR_HTTP_ERROR:
                ret = "下载过程中发生网络错误";
                break;

            default:
                break;
        }
        return ret;
    }

    /**
     * 当符合的reason增加，需要同时更新这里
     * @param reason
     * @return
     */
    public static boolean pausedByNet(int reason) {
        return (reason == FileDownloadConstant.PAUSED_QUEUED_FOR_WIFI
                || reason == FileDownloadConstant.PAUSED_WAITING_FOR_NETWORK);
    }

    /**
     * 当符合的reason增加，需要同时更新这里
     * @param reason
     * @return
     */
    public static boolean failedForNet(int reason) {
        return (reason == FileDownloadConstant.ERROR_HTTP_ERROR
                || reason == FileDownloadConstant.ERROR_NETWORK_CONNECTION_TIMEOUT
                || reason == FileDownloadConstant.ERROR_NETWORK_NO_INPUT_STREAM
                || reason == FileDownloadConstant.ERROR_NETWORK_RESPONSE_CODE_ERROR
                || reason == FileDownloadConstant.ERROR_NETWORK_SOCKET_TIMEOUT);
    }
}
