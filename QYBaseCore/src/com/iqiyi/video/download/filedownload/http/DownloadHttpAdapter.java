package com.iqiyi.video.download.filedownload.http;

import android.text.TextUtils;

import com.iqiyi.video.download.filedownload.FileDownloadConstant;
import com.iqiyi.video.download.filedownload.FileDownloadHelper;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.DeviceUtil;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.video.module.download.exbean.XTaskBean;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * Created by songguobin on 2016/12/30.
 * <p>
 * 文件下载使用.cdf作为下载中的文件的后缀名
 * 业务方在下载过程中需要给文件加上后缀名，
 * 下载完成后，通知统一下载器，下载器会进行重命名处理
 */

public class DownloadHttpAdapter<B extends XTaskBean> {

    private static final String TAG = "DownloadHttpAdapter";

    private static final int DOWNLOAD_BUFFER_SIZE = 16 * 1024;//下载缓存大小

    private static final int MAX_RECURSIVE_TIMES = 3;

    private boolean isRunning = true;

    private int recursiveTime = 0;

    /**
     * 下载文件
     *
     * @param bean
     * @param callback
     * @return 下载结果状态值
     * FileDownloadConstant.DOWNLOAD_ERROR ： 下载失败
     * FileDownloadConstant.DOWNLOAD_SUCCESS: 下载成功
     * FileDownloadConstant.DOWNLOAD_INTERVAL_RETRY:间隔重试 （网络异常）
     */

    public int downloadFile(B bean, DownloadProgressCallback<B> callback) {

        URL url = null;

        try {
            url = new URL(bean.getDownloadUrl());
        } catch (MalformedURLException e) {
            ExceptionUtils.printStackTrace(e);
            bean.setErrorCode(FileDownloadConstant.FILE_DOWNLOAD_URL_ERROR);
            return FileDownloadConstant.DOWNLOAD_ERROR;
        }

        File downloadingFile = new File(bean.getDownloadingPath());

        long downloadSize = downloadingFile.length();

        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("User-Agent", DeviceUtil.getUserAgentInfo());
            connection.addRequestProperty("Range", "bytes=" + downloadSize + "-");
            connection.setConnectTimeout(FileDownloadConstant.DOWNLOAD_CONNECTION_TIMEOUT);
            connection.setReadTimeout(FileDownloadConstant.DOWNLOAD_SOCKET_TIMEOUT);
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();
            DebugLog.log(TAG, "downloadFile>>url = " + url);
            DebugLog.log(TAG, "downloadFile>>responseCode = " + responseCode);

            switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_PARTIAL:
                    //receive data from server
                    bean.setFileSize(connection.getContentLength());
                    return downloadFileByUrlConnection(connection.getInputStream(), bean, callback);

                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                    DebugLog.log(TAG, "******302跳转******");
                    String location = connection.getHeaderField("Location");

                    if (!TextUtils.isEmpty(location)) {
                        bean.setDownloadUrl(location);
                        bean.setErrorCode(FileDownloadConstant.FILE_DOWNLOAD_REDIRECT_ERROR);

                        if (recursiveTime >= MAX_RECURSIVE_TIMES) {
                            DebugLog.log(TAG, "302跳转超过最大递归次数");
                            return FileDownloadConstant.DOWNLOAD_ERROR;
                        } else {
                            recursiveTime++;
                            DebugLog.log(TAG, "recursiveTime = " + recursiveTime);
                            return downloadFile(bean, callback);
                        }

                    } else {
                        bean.setErrorCode(FileDownloadConstant.FILE_DOWNLOAD_REDIRECT_NO_LOCALTION);
                        return FileDownloadConstant.DOWNLOAD_ERROR;
                    }

                case FileDownloadConstant.DOWNLOAD_RANGE_ERROR:
                    DebugLog.log(TAG, "******416错误******");

                    bean.setErrorCode(FileDownloadConstant.FILE_DOWNLOAD_RANGE_ERROR);
                    //置空当前已下载大小
                    bean.setCompleteSize(0);

                    FileDownloadHelper.clearDownloadFile(downloadingFile);

                    if (recursiveTime >= MAX_RECURSIVE_TIMES) {
                        DebugLog.log(TAG, "416错误超过最大递归次数");
                        return FileDownloadConstant.DOWNLOAD_ERROR;
                    } else {
                        recursiveTime++;
                        DebugLog.log(TAG, "recursiveTime = " + recursiveTime);
                        return downloadFile(bean, callback);
                    }

                default:
                    //其他网络问题
                    bean.setErrorCode(FileDownloadConstant.FILE_DOWNLOAD_ILLEGAL_RESPONSE_CODE);
                    return FileDownloadConstant.DOWNLOAD_ERROR;
            }
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);

            bean.setErrorCode(FileDownloadConstant.FILE_DOWNLOAD_IO_EXCEPTION);
            return FileDownloadConstant.DOWNLOAD_INTERVAL_RETRY;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 获取小文件的inputStream流
     *
     * @param url
     * @return
     */
    public byte[] getByteStream(String url) {

        URL urlObj = null;

        try {
            urlObj = new URL(url);
        } catch (MalformedURLException e) {
            ExceptionUtils.printStackTrace(e);
            return null;
        }
//
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("User-Agent", DeviceUtil.getUserAgentInfo());
            connection.setConnectTimeout(FileDownloadConstant.DOWNLOAD_CONNECTION_TIMEOUT);
            connection.setReadTimeout(FileDownloadConstant.DOWNLOAD_SOCKET_TIMEOUT);
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();

            DebugLog.log(TAG, "getByteStream>>url = " + url);
            DebugLog.log(TAG, "getByteStream>>responseCode = " + responseCode);

            switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_PARTIAL:

                    byte[] byteResult = FileDownloadHelper.inputStrem2Byte(connection.getInputStream());

                    return byteResult;

                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                    DebugLog.log(TAG, "******302跳转******");
                    String location = connection.getHeaderField("Location");
                    if (!TextUtils.isEmpty(location)) {
                        if (recursiveTime >= MAX_RECURSIVE_TIMES) {
                            DebugLog.log(TAG, "302跳转超过最大递归次数");
                            return null;
                        } else {
                            recursiveTime++;
                            DebugLog.log(TAG, "recursiveTime = " + recursiveTime);
                            return getByteStream(location);
                        }
                    } else {
                        return null;
                    }

                default:
                    //其他网络问题
                    break;
            }
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        } catch (StackOverflowError e) {
            ExceptionUtils.printStackTrace(e);
        } catch (OutOfMemoryError e) {
            ExceptionUtils.printStackTrace(e);
        } finally {

            if (connection != null) {

                connection.disconnect();
            }
        }
        return null;
    }

    /**
     * 获取inputStream
     * @param url
     * @param startLoc 断点开始位置
     * @param endLoc 断点结束位置
     * @return
     * @throws Exception
     */
    public HttpURLConnection getConnection(String url,long startLoc,long endLoc) throws Exception {

        URL urlObj = null;

        try {
            urlObj = new URL(url);
        } catch (MalformedURLException e) {
            ExceptionUtils.printStackTrace(e);
            return null;
        }
        HttpURLConnection connection = null;
        connection = (HttpURLConnection) urlObj.openConnection();
        connection.setRequestMethod("GET");
        connection.addRequestProperty("Connection", "Keep-Alive");
        StringBuilder rangeInfo = new StringBuilder();

        if(startLoc!=-1){

            rangeInfo .append("bytes=" + startLoc + "-") ;

            //"bytes=" + startLoc + "-" + endLoc
            if(endLoc != -1 && endLoc >startLoc){
                rangeInfo.append(endLoc);
            }
        }

        DebugLog.log(TAG,"rangeInfo = " + rangeInfo.toString());
        connection.addRequestProperty("Range",  rangeInfo.toString());
        connection.setRequestProperty("User-Agent", DeviceUtil.getUserAgentInfo());
        connection.setConnectTimeout(FileDownloadConstant.DOWNLOAD_CONNECTION_TIMEOUT);
        connection.setReadTimeout(FileDownloadConstant.DOWNLOAD_SOCKET_TIMEOUT);
        connection.setInstanceFollowRedirects(true);

        int responseCode = connection.getResponseCode();

        DebugLog.log(TAG, "getConnection>>url = " + url);
        DebugLog.log(TAG, "getConnection>>responseCode = " + responseCode);

        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
            case HttpURLConnection.HTTP_PARTIAL:

                return connection;

            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
            case HttpURLConnection.HTTP_SEE_OTHER:
                DebugLog.log(TAG, "******302跳转******");
                String location = connection.getHeaderField("Location");
                if (!TextUtils.isEmpty(location)) {
                    if (recursiveTime >= MAX_RECURSIVE_TIMES) {
                        DebugLog.log(TAG, "302跳转超过最大递归次数");
                        return null;
                    } else {
                        recursiveTime++;
                        DebugLog.log(TAG, "recursiveTime = " + recursiveTime);
                        return getConnection(location,startLoc,endLoc);
                    }
                } else {
                    return null;
                }

            default:
                //其他网络问题
                break;
        }

        return null;

    }

    /**
     * 获取文件大小
     *
     * @param url
     * @return
     */
    public long getFileSize(String url) {

        URL urlObj = null;

        try {
            urlObj = new URL(url);
        } catch (MalformedURLException e) {
            ExceptionUtils.printStackTrace(e);
            return -1;
        }
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("User-Agent", DeviceUtil.getUserAgentInfo());
            connection.setConnectTimeout(FileDownloadConstant.DOWNLOAD_CONNECTION_TIMEOUT);
            connection.setReadTimeout(FileDownloadConstant.DOWNLOAD_SOCKET_TIMEOUT);
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();

            DebugLog.log(TAG, "getFileSize>>url = " + url);
            DebugLog.log(TAG, "getFileSize>>responseCode = " + responseCode);

            switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_PARTIAL:

                    return connection.getContentLength();

                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                    DebugLog.log(TAG, "******302跳转******");
                    String location = connection.getHeaderField("Location");
                    if (!TextUtils.isEmpty(location)) {
                        if (recursiveTime >= MAX_RECURSIVE_TIMES) {
                            DebugLog.log(TAG, "302跳转超过最大递归次数");
                            return -1;
                        } else {
                            recursiveTime++;
                            DebugLog.log(TAG, "recursiveTime = " + recursiveTime);
                            return getFileSize(location);
                        }
                    } else {
                        return -1;
                    }

                default:
                    //其他网络问题
                    break;
            }
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        } catch (StackOverflowError e) {
            ExceptionUtils.printStackTrace(e);
        } catch (OutOfMemoryError e) {
            ExceptionUtils.printStackTrace(e);
        } finally {

            if (connection != null) {

                connection.disconnect();
            }
        }
        return -1;
    }

    public boolean isRunning() {

        return isRunning;

    }


    public void setRunning(boolean isRunning) {

        this.isRunning = isRunning;

    }


    /////////////////////////////////////////////////////

    /**
     * 纯下载文件逻辑
     *
     * @return 下载结果状态值
     */
    private int downloadFileByUrlConnection(InputStream inputStream, B bean, DownloadProgressCallback<B> callback) {

        FileOutputStream fos = null;

        BufferedInputStream bis = null;

        File downloadingFile = null;

        long contentLength = -1;

        long completeSize = 0;

        //写文件的缓存数组
        byte[] mBuffer;

        //清空错误码
        bean.setErrorCode("");

        try {
            // 没返回InputStream，重试
            if (inputStream == null) {

                DebugLog.log(TAG, bean.getFileName() + ">>InputStream == null");

                bean.setErrorCode(FileDownloadConstant.FILE_DOWNLOAD_INPUTSTREAM_IS_NULL);

                return FileDownloadConstant.DOWNLOAD_INTERVAL_RETRY;

            }

            mBuffer = new byte[DOWNLOAD_BUFFER_SIZE];

            //从contentLength获取，若返回时200，则contentLength等于整个文件大小
            //若返回206，则contentLength表示这个文件剩余下载的大小，需要更新整个文件大小
            contentLength = bean.getFileSzie();

            completeSize = bean.getCompleteSize();

            downloadingFile = new File(bean.getDownloadingPath());

            DebugLog.log(TAG, bean.getFileName() + ">>contentLength = " + contentLength);

            DebugLog.log(TAG, bean.getFileName() + ">>completeSize = " + completeSize);

            //contentLength对于下载没有帮助，仅用于计算下载进度
            if (contentLength <= 0) {

                DebugLog.e(TAG, bean.getFileName() + ">>返回错误的ContentLength = " + contentLength);

            } else {

                //修正completeSize大小
                if (completeSize < 0) {
                    completeSize = 0;
                }

                //更新总大小
                long realSize = completeSize + contentLength;

                if (realSize > 0 && realSize != bean.getFileSzie()) {

                    DebugLog.log(TAG, bean.getFileName() + ",更新总大小！真实总大小:" + realSize);

                    bean.setFileSize(realSize);

                }
            }


            bis = new BufferedInputStream(inputStream);

            fos = new FileOutputStream(downloadingFile, true);

            int bufferStart = 0;// 之前read读取的数据量
            int numRead = 0;// 一次read读取的数据量
            long curUpdateTime = 0; // 用于控制刷新进度的变量
            long lastUpdateTime = System.currentTimeMillis();
            long lastCompleteSize = completeSize;

            while (true) {

                // 如果被中断，则整体退出
                if (!isRunning()) {

                    DebugLog.log(TAG, bean.getFileName() + " is cancelled while write file");

                    bean.setErrorCode(FileDownloadConstant.FILE_DOWNLOAD_ABORT);

                    return FileDownloadConstant.DOWNLOAD_ERROR;

                }

                curUpdateTime = System.currentTimeMillis();

                numRead = bis.read(mBuffer, bufferStart, DOWNLOAD_BUFFER_SIZE - bufferStart);
                // 已经没有数据了，退出循环
                if (numRead == -1) {
                    if (bufferStart > 0) {// buffer未填充满，但已经没数据了，则写入文件
                        fos.write(mBuffer, 0, bufferStart);
                    }
                    break;
                }

                // 递增已下载大小
                completeSize = completeSize + numRead;

                // 为了防止频繁刷新界面，间隔大于1秒
                if (curUpdateTime - lastUpdateTime >= 1000) {
                    //计算速度
                    long increaseCompleteSize = completeSize - lastCompleteSize;
                    long gapTime = curUpdateTime - lastUpdateTime;
                    long speed = increaseCompleteSize / gapTime * 1000;
                    bean.setSpeed(speed);
                    lastUpdateTime = curUpdateTime;
                    lastCompleteSize = completeSize;

                    if (callback != null) {

                        callback.onDataChanged(bean);

                    }

                }


                if (numRead + bufferStart < DOWNLOAD_BUFFER_SIZE) {
                    // buffer未填满
                    bufferStart = numRead + bufferStart;
                } else {
                    // buffer已填满，则写入文件
                    fos.write(mBuffer, 0, DOWNLOAD_BUFFER_SIZE);
                    bufferStart = 0;// buffer重新开始填充
                    // 更新已下载大小
                    bean.setCompleteSize(completeSize);
                }
            }

            //下载完成
            if (callback != null) {

                callback.onDataChanged(bean);

            }

            return FileDownloadConstant.DOWNLOAD_SUCCESS;

        } catch (IOException e) {

            ExceptionUtils.printStackTrace(e);

            DebugLog.log(TAG, "下载失败 = " + e.getMessage());

            bean.setErrorCode(FileDownloadConstant.FILE_DOWNLOAD_NETWORK_EXCEPTION);
            return FileDownloadConstant.DOWNLOAD_INTERVAL_RETRY;


        } finally {

            if (bis != null) {
                try {
                    bis.close();
                } catch (Exception e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }


            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }

    }

}
