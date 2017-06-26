package com.iqiyi.video.download.filedownload;

/**
 * Created by songguobin on 2016/11/21.
 */
public class FileDownloadConstant {


    public static final int DOWNLOAD_CONNECTION_TIMEOUT = 30000;// 连接超时

    public static final int DOWNLOAD_SOCKET_TIMEOUT = 30000;// 返回超时

    public static final String TEMP_PREFIX = ".cdf";

    public static final int DOWNLOAD_RANGE_ERROR = 416;//range错误

    public static final int SDCARD_FULL_TEST_SIZE = 128 * 1024;// 默认读写缓存大小，用于测试存储卡是否满

    public static final int DOWNLOAD_WAY_FILE_CDN = 30;//CDN方式下载文件

    @Deprecated
    public static final int DOWNLOAD_WAY_FILE_CUBE = 31;//cube方式下载文件

    public static final int DOWNLOAD_WAY_FILE_CDN_MULTI = 32;//CDN方式多线程下载文件

    ///////////////////文件下载状态///////////////////////////////////
    /**
     * 下载成功
     */
    public static final int DOWNLOAD_SUCCESS = 1000;

    /**
     * 下载失败
     */
    public static final int DOWNLOAD_ERROR = 1001;

    /**
     * 下载间隔重试
     */
    public static final int DOWNLOAD_INTERVAL_RETRY = 1002;

    /**
     * 下载瞬间重试
     */
    public static final int DOWNLOAD_INSTANT_RETRY = 1003;

    //========================= 文件下载错误 start=================================
    public static final String FILE_DOWNLOAD_SDCARD_FULL = "10000";//存储空间满

    public static final String FILE_DOWNLOAD_URL_NULL = "10001";//url为空

    public static final String FILE_DOWNLOAD_PATH_NULL = "10002";//下载路径为空

    public static final String FILE_DOWNLOAD_CREATE_FILE_FAIL = "10003";//创建文件失败

    public static final String FILE_DOWNLOAD_CREATE_DIR_FAIL = "10004";//创建文件夹失败

    public static final String FILE_DOWNLOAD_INPUTSTREAM_IS_NULL = "10005";//服务器返回为空

    public static final String FILE_DOWNLOAD_URL_ERROR = "10006";//下载url错误

    public static final String FILE_DOWNLOAD_IO_EXCEPTION = "10007";//io异常

    public static final String FILE_DOWNLOAD_VERIFY_ERROR = "10008";//校验错误

    public static final String FILE_DOWNLOAD_UNZIP_ERROR = "10009";//解压失败

    public static final String FILE_DOWNLOAD_CUBE_START_TASK_FAIL = "10010";//cube启动任务失败

    public static final String FILE_DOWNLOAD_RENAME_FAIL = "10011";//重命名失败

    public static final String FILE_DOWNLOAD_NETWORK_EXCEPTION = "10012";//网络异常

    public static final String FILE_DOWNLOAD_REDIRECT_ERROR = "10013";//302跳转异常

    public static final String FILE_DOWNLOAD_REDIRECT_NO_LOCALTION = "10014";//302跳转，无location地址

    public static final String FILE_DOWNLOAD_RANGE_ERROR = "10015";//416错误

    public static final String FILE_DOWNLOAD_ILLEGAL_RESPONSE_CODE = "10016";//非法的response code

    public static final String FILE_DOWNLOAD_ABORT = "10017";//下载中断

    public static final String FILE_DOWNLOAD_ILLEGAL_LENGTH = "10018";//文件长度不正确



    public static final String FILE_DOWNLOAD_SO_VERIFY_ERROR = "20000";//大播放内核库校验失败

    //========================= 文件下载错误 end=================================

    public static final String DEFAULT_CHARSET = "UTF-8";

    public static final long STORAGE_15M = 15 * 1024 * 1024L;//15M

    public static final int UPDATE_FILE_DOWNLOAD_OBJECT = 1000;


}
