package org.qiyi.basecore.filedownload;

import android.content.Context;
import android.os.Process;
import android.util.Pair;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.NetWorkTypeUtils;
import org.qiyi.basecore.utils.NetworkStatus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;

/**
 * @author kangle 
 */
public class FileDownloadThread implements PriorityFutureTask.PriorityCallable {

    private static final int MAX_REDIRECTS = 5;

    private static final int DEFAULT_TIMEOUT = 20000;

    private static final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    private static final int HTTP_TEMP_REDIRECT = 307;

    private static final int BUFFER_SIZE = 16 * 1024;

    private FileDownloadStatus fileDownloadStatus;
    private Context mContext;
    private IChangeImp mIChange;

    public FileDownloadThread(FileDownloadStatus fileDownloadStatus, Context context, IChangeImp iChange) {
        this.fileDownloadStatus = fileDownloadStatus;
        this.mContext = context;
        this.mIChange = iChange;

        mIChange.currentThread = this;
    }

    @Override
    public Object call() {

        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        
        // TODO KANGLE 目前只支持单线程下载
        synchronized (mIChange) {
            execute();
        }
        return null;
    }

    /**
     * 
     */
    private void execute() {

        URL url;

        // 构造URL
        try {
            url = new URL(fileDownloadStatus.getDownloadUrl());
            DebugLog.v("FileDownload", "execute for url: " + url);
        } catch (MalformedURLException e) {

            if (DebugLog.isDebug()) {
                ExceptionUtils.printStackTrace(e);
            }

            mIChange.onFailed(new Pair<Integer, String>(FileDownloadConstant.ERROR_DOWNLOAD_URL_INVALID,
                    "构造URL的时候发生MalformedURLException异常: " + e.getMessage()), this != mIChange.currentThread);
            return;
        }

        File downloadFile;

        // 尝试创建文件
        try {
            downloadFile =
                    FileDownloadTools.fixDownloadFile(mContext,
                            fileDownloadStatus.getDownloadedFileAbsolutePath(mContext));
        } catch (IOException e) {

            if (DebugLog.isDebug()) {
                ExceptionUtils.printStackTrace(e);
            }

            mIChange.onFailed(new Pair<Integer, String>(FileDownloadConstant.ERROR_UNABLE_TO_CREATE_FILE,
                    "无法在指定目录下创建待下载的文件: " + e.getMessage()), this != mIChange.currentThread);
            return;
        }

        int redirectionCount = 0;
        whileLoop: while (redirectionCount++ < MAX_REDIRECTS) {

            // 非首次尝试，Sleep 2 seconds（wait for net & storage broadcast）
            if (redirectionCount != 1) {
                try {
                    Thread.sleep(2000);
                } 
                //极其可能是上层的Interrupted，这个时候 checkIfKeepExecute 就会终止线程继续执行
                catch (InterruptedException e) {
                    if (DebugLog.isDebug()) {
                        ExceptionUtils.printStackTrace(e);
                        Thread.currentThread().interrupt();
                    }
                }
            }
                
            if (!checkIfKeepExecute(mContext)) {
                return;
            }

            // 最后一次尝试
            boolean isTheLastTry = redirectionCount == MAX_REDIRECTS;

            HttpURLConnection conn = null;
            try {

                long downloadedFileLength = downloadFile.length();

                // 已经下载完成
                if (fileDownloadStatus.total_size_bytes != -1
                        && fileDownloadStatus.total_size_bytes == downloadedFileLength) {
                    mIChange.onCompleted(this != mIChange.currentThread);
                    return;
                } 
                // 由下载记录correct下载文件
                else if (fileDownloadStatus.total_size_bytes == -1) {
                    if (!clearFile(downloadFile)) {
                        return;
                    } else {
                        downloadedFileLength = 0;
                    }
                }
                // 由文件大小更新进度
                else if (fileDownloadStatus.bytes_downloaded_so_far != downloadedFileLength) {
                    mIChange.onDownloadProgress(downloadedFileLength - fileDownloadStatus.bytes_downloaded_so_far,
                            this != mIChange.currentThread);
                }

                conn = (HttpURLConnection) url.openConnection();
                // conn.setRequestMethod("GET");
                conn.setConnectTimeout(DEFAULT_TIMEOUT);
                conn.setReadTimeout(DEFAULT_TIMEOUT);
                addRequestHeaders(conn, downloadedFileLength);

                int responseCode = conn.getResponseCode();
                switch (responseCode) {
                // 故意不写break;
                    case HttpURLConnection.HTTP_OK:
                    case HttpURLConnection.HTTP_PARTIAL:

                        int contentLength = conn.getContentLength();

                        if (contentLength == -1) {
                            mIChange.onFailed(new Pair<Integer, String>(FileDownloadConstant.ERROR_HTTP_ERROR,
                                    "从Http Header中无法取得Content-Length"), this != mIChange.currentThread);
                            return;
                        }
                        // 之前记录的下载长度与此次网络请求的长度不一致
                        else if (fileDownloadStatus.total_size_bytes != -1
                                && fileDownloadStatus.total_size_bytes != contentLength + downloadedFileLength) {

                            if(!clearFile(downloadFile)){
                                return;
                            };

                            // 更新 fileDownloadStatus.total_size_bytes，重新下载
                            fileDownloadStatus.total_size_bytes = contentLength + downloadedFileLength;
                            continue;

                        } else {
                            fileDownloadStatus.total_size_bytes = contentLength + downloadedFileLength;
                        }

                        // 重试
                        if (transferData(conn, isTheLastTry, downloadFile)) {
                            continue;
                        }
                        // 完成 or 无须重试的失败
                        else {
                            break whileLoop;
                        }

                    case HTTP_REQUESTED_RANGE_NOT_SATISFIABLE:
                    case HttpURLConnection.HTTP_ENTITY_TOO_LARGE:

                        mIChange.onCompleted(this != mIChange.currentThread);
                        return;

                    case HttpURLConnection.HTTP_MOVED_PERM:
                    case HttpURLConnection.HTTP_MOVED_TEMP:
                    case HttpURLConnection.HTTP_SEE_OTHER:
                    case HTTP_TEMP_REDIRECT:
                        final String location = conn.getHeaderField("Location");
                        url = new URL(url, location);
                        if (responseCode == HTTP_MOVED_PERM) {
                            mIChange.onDownloadUrlRedirect(url.toString(), fileDownloadStatus);
                        }
                        continue;
                    default:

                        if (isTheLastTry) {
                            mIChange.onFailed(new Pair<Integer, String>(FileDownloadConstant.ERROR_NETWORK_RESPONSE_CODE_ERROR,
                                    "HTTP 返回码错误: " + responseCode), this != mIChange.currentThread);
                        }

                        continue;
                }

            } catch (IOException e) {

                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }

                if (isTheLastTry) {
                    if(e instanceof SocketTimeoutException){
                        mIChange.onFailed(new Pair<Integer, String>(FileDownloadConstant.ERROR_NETWORK_CONNECTION_TIMEOUT,
                                "Socket连接超时: " + e.getMessage()), this != mIChange.currentThread);
                    }else{
                        mIChange.onFailed(new Pair<Integer, String>(FileDownloadConstant.ERROR_HTTP_ERROR,
                                "其他 HttpURLConnection 错误: " + e.getMessage()), this != mIChange.currentThread);
                    }
                }
                continue;
            }
        }
    }

    /**
     * 清空文件（set to length to 0）
     * @param downloadFile 
     * @return 是否成功
     */
    private boolean clearFile(File downloadFile) {
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(downloadFile, "rw");
            randomAccessFile.setLength(0);
            randomAccessFile.close();
        } catch (IOException e) {
            
            mIChange.onFailed(
                new Pair<Integer, String>(FileDownloadConstant.ERROR_WRITING_DOWNLOAD_FILE,
                        "下载记录中文件中长度与Http中ContentLength长度不一致，清除已下载文件内容时发生IOException: "
                                + e.getMessage()), this != mIChange.currentThread);
            
            return false;
        } finally {
            if(randomAccessFile != null) {
                try{
                    randomAccessFile.close();
                }catch (IOException e){
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
        return true;
        
    }

    /**
     * @param conn
     * @param isTheLastTry
     * @return 是否能 retry，continue in the loop
     */
    private boolean transferData(HttpURLConnection conn, boolean isTheLastTry, File downloadFile) {

        InputStream in = null;
        OutputStream out = null;

        try {
            // InputStream error
            try {
                in = conn.getInputStream();
                if(isTheLastTry && in == null){
                    mIChange.onFailed(new Pair<Integer, String>(FileDownloadConstant.ERROR_NETWORK_NO_INPUT_STREAM,
                            "网络input stream为null"), this != mIChange.currentThread);
                    return false;
                }
            } catch (IOException e) {

                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }

                if (isTheLastTry) {
                    if(e instanceof SocketTimeoutException){
                        mIChange.onFailed(new Pair<Integer, String>(FileDownloadConstant.ERROR_NETWORK_SOCKET_TIMEOUT,
                                "读取网络的InputStream的时候超时：" + e.getMessage()), this != mIChange.currentThread);
                    }else{
                        mIChange.onFailed(new Pair<Integer, String>(FileDownloadConstant.ERROR_HTTP_ERROR,
                                "读取网络的InputStream的时候发生异常：" + e.getMessage()), this != mIChange.currentThread);
                    }
                }
                return true;
            }

            // OutputStream error
            try {
                out = new FileOutputStream(downloadFile, true);
            } catch (FileNotFoundException e) {
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }
                mIChange.onFailed(new Pair<Integer, String>(FileDownloadConstant.ERROR_DOWNLOAD_FILE_NOT_FOUND,
                        "new FileOutputStream 的时候发生FileNotFoundException异常：" + e.getMessage()),
                        this != mIChange.currentThread);
                beforeReturn(in, out);
                return false;
            }

            final byte buffer[] = new byte[BUFFER_SIZE];

            while (true) {

                if (!checkIfKeepExecute(mContext)) {
                    beforeReturn(in, out);
                    return false;
                }

                int len = -1;

                try {
                    len = in.read(buffer);
                } catch (IOException e) {
                    if (DebugLog.isDebug()) {
                        ExceptionUtils.printStackTrace(e);
                    }

                    if (isTheLastTry) {

                        mIChange.onFailed(new Pair<Integer, String>(FileDownloadConstant.ERROR_HTTP_ERROR,
                                "InputStream read 的时候发生异常：" + e.getMessage()), this != mIChange.currentThread);
                    }
                    beforeReturn(in, out);
                    return true;
                }

                if (len == -1) {
                    break;
                }

                try {
                    out.write(buffer, 0, len);

                    if (fileDownloadStatus.mDownloadConfiguration.targetSize > 0 &&
                            fileDownloadStatus.bytes_downloaded_so_far > fileDownloadStatus.mDownloadConfiguration.targetSize) {
                        mIChange.onFailed(new Pair<Integer, String>(FileDownloadConstant.ERROR_VALIDATE_FAILED,
                                "downloaded more than configuration's targetSize"), this != mIChange.currentThread);
                        return false;
                    } else {
                        mIChange.onDownloadProgress(len, this != mIChange.currentThread);
                    }

                } catch (IOException e) {
                    if (DebugLog.isDebug()) {
                        ExceptionUtils.printStackTrace(e);
                    }

                    mIChange.onFailed(new Pair<Integer, String>(FileDownloadConstant.ERROR_WRITING_DOWNLOAD_FILE,
                            "写文件流的时候发生异常：" + e.getMessage()), this != mIChange.currentThread);

                    beforeReturn(in, out);
                    return false;
                }
            }
        } finally {
            beforeReturn(in, out);
        }

        //下载完成
        if (downloadFile.length() == fileDownloadStatus.total_size_bytes) {
            mIChange.onCompleted(this != mIChange.currentThread);
        } 
        //retry
        else if(!isTheLastTry){
            return true;
        }
        //fail
        else {
            mIChange.onFailed(new Pair<Integer, String>(FileDownloadConstant.ERROR_UNKNOWN,
                    "下载完成，但是文件大小和Content-Length对不上号：" + downloadFile.length() + " vs "
                            + fileDownloadStatus.total_size_bytes), this != mIChange.currentThread);
        }

        return false;
    }

    private void beforeReturn(InputStream in, OutputStream out) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
        if (out != null) {
            try {
                out.flush();
                out.close();
            } catch (IOException e) {
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
    }

    private boolean checkAvailableSpace() {
        // TODO KANGLE
        return true;
    }

    private void addRequestHeaders(HttpURLConnection conn, long currentBytes) {

        conn.addRequestProperty("Connection", "Keep-Alive");

        // Only splice in user agent when not already defined
        if (conn.getRequestProperty("User-Agent") == null) {
            conn.addRequestProperty("User-Agent", getUserAgentInfo());
        }

        // Defeat transparent gzip compression, since it doesn't allow us to
        // easily resume partial downloads.
        conn.setRequestProperty("Accept-Encoding", "identity");

        if (currentBytes != 0) {
            conn.addRequestProperty("Range", "bytes=" + currentBytes + "-");
        }

    }

    /**
     * @return first: 是否有网，当前网络下能否下载（Not under wifi）
     */
    private Pair<Boolean, Boolean> checkNetwork() {
        NetworkStatus networkStatus = NetWorkTypeUtils.getNetworkStatus(mContext);
        return new Pair<Boolean, Boolean>(networkStatus != NetworkStatus.OFF,
                fileDownloadStatus.canDownload(networkStatus));
    }

    /*
     * private static long getHeaderFieldLong(URLConnection conn, String field, long defaultValue) {
     * try { return Long.parseLong(conn.getHeaderField(field)); } catch (NumberFormatException e) {
     * if (DebugLog.isDebug()) {  } return defaultValue; } }
     */

    /**
     * 检查当前状态，是否适合继续执行 无法继续下载 —— 0、该线程已被标记为终止（目前是由于任务下载为单线程下载所致） 1、状态不符合（上层将状态修改为非下载状态） 2、存储容量不够了
     * 3、网络条件不允许（无网 or 非wifi下不允许）
     * 
     * @return
     */
    private boolean checkIfKeepExecute(Context context) {

        // 上层修改了下载状态
        if (fileDownloadStatus.status != FileDownloadConstant.STATUS_RUNNING || this != mIChange.currentThread) {
            return false;
        } else {

            // 剩余空间不足以下完
            if (fileDownloadStatus.total_size_bytes != -1 && !checkAvailableSpace()) {

                mIChange.onPaused(new Pair<Integer, String>(FileDownloadConstant.PAUSED_INSUFFICIENT_SPACE,
                        "剩余存储空间不足以完成下载"), this != mIChange.currentThread);
                return false;
            }

            Pair<Boolean, Boolean> netStatus = checkNetwork();

            if (netStatus.first && netStatus.second) {
                return true;
            }
            // 无网暂停 or 非wifi下禁止下载暂停
            else {

                mIChange.onPaused(new Pair<Integer, String>(!netStatus.first
                        ? FileDownloadConstant.PAUSED_WAITING_FOR_NETWORK
                        : FileDownloadConstant.PAUSED_QUEUED_FOR_WIFI, !netStatus.first ? "没有网络" : "非wifi下禁止下载"),
                        this != mIChange.currentThread);

                return false;
            }

        }
    }

    public static String getUserAgentInfo() {
        return "Android" + android.os.Build.VERSION.RELEASE + "-" + android.os.Build.MANUFACTURER
                + "-" + android.os.Build.PRODUCT + "(" + android.os.Build.MODEL + ")";
    }

    /**
     * @return 当未设置优先级的时候，自增长的interPriority起主导作用（使得先添加的任务优先级高于后添加的任务）；
     * 当设置了优先级，优先级起主导作用，优先级相同的情况下比较interPriority
     */
    @Override
    public int getPriority() {
        int ret = fileDownloadStatus.mDownloadConfiguration.priority
                * FileDownloadStatus.DownloadConfiguration.INTER_PRIORITY_UPPER_BOUND
                + fileDownloadStatus.mDownloadConfiguration.interPriority;
        return ret;
    }
}
