package com.iqiyi.video.download.filedownload.task;

import android.content.Context;
import android.text.TextUtils;

import com.iqiyi.video.download.engine.switcher.ISwitcher;
import com.iqiyi.video.download.engine.task.XBaseTaskExecutor;
import com.iqiyi.video.download.engine.task.runnable.XInfiniteRetryRunnable;
import com.iqiyi.video.download.filedownload.FileDownloadConstant;
import com.iqiyi.video.download.filedownload.FileDownloadHelper;
import com.iqiyi.video.download.filedownload.db.DBRequestController;
import com.iqiyi.video.download.filedownload.http.DownloadHttpAdapter;
import com.iqiyi.video.download.filedownload.http.DownloadProgressCallback;
import com.iqiyi.video.download.filedownload.pool.DownloadThreadPool;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.StringUtils;
import org.qiyi.video.module.download.exbean.FileDownloadObject;

import java.io.File;
import java.util.Random;


/**
 * Created by songguobin on 2016/11/23.
 * <p/>
 * 文件下载任务，可下载任何文件
 * <p/>
 * 主要功能：
 * 1、处理302跳转
 * 2、处理416错误
 * 3、断点续传
 * 4、网络异常重试
 * 5、16K缓存机制
 * 6、存储空间满检测，读写异常检测
 * 7、可配置蜂窝下下载
 * 8、错误码监控
 * 9、下载成功率统计
 * 10、增加校验功能
 */
public class CdnDownloadFileTask extends XBaseTaskExecutor<FileDownloadObject> {

    private static final String TAG = "CdnDownloadFileTask";

    private Context mContext;

    public DBRequestController mDbController;// 数据库Controller

    public volatile FileDownloadRunnable mRunnable;// 负责启动和轮询的异步线程


    //网络重试，校验出错重试
    public static final int DEFAULT_RETRY_TIMES = 3;//重试次数

    public static final int MAX_RETRY_TIMES = 30;


    public CdnDownloadFileTask(Context context, FileDownloadObject bean, DBRequestController dbController) {
        super(bean);
        this.mContext = context;
        this.mDbController = dbController;
    }

    @Override
    protected boolean onStart() {

        DebugLog.log(TAG, getBean().getFileName() + ">>onStart");

        if (mRunnable != null) {

            return false;

        }

        mRunnable = new FileDownloadRunnable(mContext, this, mDbController);

        DownloadThreadPool.DOWNLOAD_POOL.submit(mRunnable);

        return true;

    }

    @Override
    protected boolean onPause() {

        DebugLog.log(TAG, getBean().getFileName() + ">>onPause");

        exitRunnable();

        return true;

    }

    @Override
    protected boolean onAbort() {

        DebugLog.log(TAG, getBean().getFileName() + ">>onAbort");

        exitRunnable();

        return true;
    }

    @Override
    protected boolean onEndSuccess() {

        DebugLog.log(TAG, getBean().getFileName() + ">>onEndSuccess");

        mRunnable = null;

        return true;
    }

    @Override
    protected boolean onEndError(String errorCode, boolean retry) {

        DebugLog.log(TAG, getBean().getFileName() + ">>onEndError");

        mRunnable = null;

        int bizType = -1;

        if (getBean().mDownloadConfig != null) {
            bizType = getBean().mDownloadConfig.type;
        }

        getBean().errorCode = bizType + "#" + errorCode;

        DebugLog.log(TAG, "errorCode = " + getBean().errorCode);

        return true;
    }

    @Override
    public long getCompleteSize() {
        return getBean().getCompleteSize();
    }

    private void exitRunnable() {

        if (mRunnable != null) {
            mRunnable.cancel();
            mRunnable = null;
        }

    }

    /**
     * 下载文件步骤：
     *
     * 下载前判断：
     * 1、检查url,filepath是否正确
     * 2、检查文件是否已经存在
     *    2.1 若文件已存在且未下载完成，更新文件大小，继续
     *    2.2 若文件已存在且下载完成，下载结束
     *    2.3 若文件不存在，则创建文件，创建失败报错结束，否则继续
     * 3、判断是否允许在4G下下载
     *
     * 下载中判断：
     *
     *
     */

    class FileDownloadRunnable extends XInfiniteRetryRunnable<FileDownloadObject> implements ISwitcher {


        private Context mContext;

        private XBaseTaskExecutor<FileDownloadObject> mHost;

        private DBRequestController mDbController;

        private boolean isDownloadSuccess;//下载成功

        private boolean isDownloadError;//下载出错

        private File downloadingFile = null;

        private File completeFile = null;

        private DownloadHttpAdapter<FileDownloadObject> httpAdapter;

        private int verifyRetryTimes = 0;//校验次数

        private int networkRetryTimes = 0;//网络重试次数

        private int unzipRetryTimes = 0;//解压次数

        private Random mRandom = null;//休眠时间随机对象



        protected FileDownloadRunnable(Context context, XBaseTaskExecutor<FileDownloadObject> host,
                DBRequestController dbController) {

            this.mContext = context;

            this.mHost = host;

            this.mDbController = dbController;

            this.downloadingFile = new File(getBean().getDownloadPath() + FileDownloadConstant.TEMP_PREFIX);

            this.completeFile = new File(getBean().getDownloadPath());

            this.httpAdapter = new DownloadHttpAdapter<FileDownloadObject>();

            this.mRandom = new Random();

        }


        /**
         * 检查url，filePath是否正确
         *
         * @param bean
         * @return
         */
        @Override
        public boolean onPreExecute(FileDownloadObject bean) {

            DebugLog.log(TAG, bean.getFileName() + ">>onPreExecute");

            if (TextUtils.isEmpty(bean.getId())) {

                DebugLog.log(TAG, "文件下载地址为空");

                getBean().errorCode = FileDownloadConstant.FILE_DOWNLOAD_URL_NULL;

                return false;
            }



                if (TextUtils.isEmpty(bean.getDownloadPath())) {
                    DebugLog.log(TAG, "文件存储地址为空");

                    getBean().errorCode = FileDownloadConstant.FILE_DOWNLOAD_PATH_NULL;

                    return false;
                }

                boolean isFileValid = checkFile(bean, completeFile, downloadingFile);

                if (!isFileValid) {

                    DebugLog.log(TAG, "文件检查失败");

                    return false;

                }

          /*      NetworkStatus status = NetWorkTypeUtils.getNetworkStatus(mContext);

                if (status != NetworkStatus.WIFI && status != NetworkStatus.OFF) {

                    if (!bean.isAllowInMobile()) {

                        DebugLog.log(TAG, "不允许在蜂窝网络下载下载");

                        mHost.pause();

                        return false;
                    }

                }*/


                return true;
            }


        @Override
        public void onCancelled(FileDownloadObject bean) {

            //检查文件已存在时，标记了isDownloadSuccess=true，退出任务
            if(isDownloadSuccess) {

                mHost.endSuccess();

            }


        }

        @Override
        public void onPreExecuteError(FileDownloadObject bean) {


            mHost.endError(bean.errorCode, true);

        }


        /**
         * 停止线程
         */
        public void cancel(){

            super.cancel();

            abortDownload();

        }

        /**
         * 停止下载文件
         */
        private void abortDownload(){


            if(httpAdapter!=null){

                DebugLog.log(TAG,"abortDownload");

                httpAdapter.setRunning(false);

            }

        }


        /**
         * 1、检查文件是否下载完成
         *    1.1 若出错，则报错结束
         *    1.2 若重试错误，则重试
         *    1.3 若成功:
         *        1.3.1 若需校验，则校验文件：若校验通过，则重命名文件；若失败，则重试
         *        1.3.2 重命名文件：若成功，则下载结束；若失败，则报错结束
         *
         * @param bean
         * @return
         */


        @Override
        public boolean onRepeatExecute(FileDownloadObject bean) {

            DebugLog.log(TAG, getBean().getFileName() + ">>onRepeatExecute");

            int result = -1;

            while (isOn()) {

                result = httpAdapter.downloadFile(bean, new DownloadProgressCallback<FileDownloadObject>() {
                    @Override
                    public void onDataChanged(FileDownloadObject bean) {

                        DebugLog.log(TAG, bean.getFileName() + ">>进度:" + bean.getDownloadPercent() + "%"
                                + "  速度：" + StringUtils.byte2XB(bean.speed) + "/s");

                        mHost.notifyDoing(-1);
                    }
                });

                DebugLog.log(TAG, bean.getFileName() + ">>downloadFile result = " + result);

                if (!isOn()) {
                    //若线程退出，则中断任务
                    break;
                }

                switch (result) {

                    case FileDownloadConstant.DOWNLOAD_SUCCESS:
                        //下载成功
                        handleSuccess();
                        break;

                    case FileDownloadConstant.DOWNLOAD_ERROR:
                        //下载错误
                        handleError();
                        break;

                    case FileDownloadConstant.DOWNLOAD_INTERVAL_RETRY:
                        //间隔重试
                        handleIntervalRetry();
                        break;

                    default:
                        break;
                }

                //check是否满足退出条件
                if (isDownloadSuccess || isDownloadError) {
                    DebugLog.log(TAG, bean.getFileName() + ">>success = " + isDownloadSuccess
                            + ">>error = " + isDownloadError);
                    break;
                }

            }

            return true;
        }

        @Override
        public void onPostExecute(FileDownloadObject bean) {

            DebugLog.log(TAG, getBean().getFileName() + ">>onPostExecute:" + getBean().getDownloadPath());

            if (isDownloadSuccess) {
                DebugLog.log(TAG, bean.getFileName() + ">>下载成功");
                mHost.endSuccess();
            } else {
                DebugLog.log(TAG, bean.getFileName() + ">>下载失败");
                mHost.endError(getBean().errorCode, true);
            }

        }

        @Override
        public boolean isOn() {

            return isRunning();

        }

        @Override
        public long getRetryInterval(long retryCount) {
            return 1000;
        }

        @Override
        public FileDownloadObject getBean() {
            return mHost.getBean();
        }


        /////////////////////业务方法/////////////////////////////////////

        /**
         * 间隔重试，主要是网络重试
         * <p>
         * isDownloadError = true,标识退出while循环，结束下载任务
         */
        private void handleIntervalRetry() {

            DebugLog.log(TAG, getBean().getFileName() + ">>网络异常重试");

            int sleepTime = FileDownloadHelper.getSleepTimeForfiniteRetry(mRandom, networkRetryTimes, getMaxRetryTimes());

            if (sleepTime == -1) {

                DebugLog.log(TAG, "有限重试结束");

                networkRetryTimes = 0;

                getBean().errorCode = FileDownloadConstant.FILE_DOWNLOAD_NETWORK_EXCEPTION;

                //标记下载出错，退出while循环
                isDownloadError = true;

            } else {


                networkRetryTimes++;

                DebugLog.log(TAG, "有限重试>>>networkRetryTimes:" + networkRetryTimes + ">>>sleepTime>>>" + sleepTime);

                FileDownloadHelper.sleep(isOn(), sleepTime);

            }

        }

        /**
         * 下载失败
         * <p>
         * isDownloadError = true,下载出错，退出while循环，结束下载任务
         */
        private void handleError() {

            isDownloadError = true;

        }


        /**
         * 下载成功
         * <p>
         * isDownloadSuccess = true，校验成功，重命名文件成功，退出while循环，结束下载任务
         * isDownloadError = true，校验失败或重命名失败
         */
        private void handleSuccess() {


            //1、校验文件，可选
            int verifyResult = FileDownloadHelper.verifyFile(getBean(), downloadingFile, completeFile);

            DebugLog.log(TAG, "verifyResult = " + verifyResult);

            if (verifyResult == FileDownloadHelper.DOWNLOAD_VERIFY_RETRY) {
                DebugLog.log(TAG, getBean().getFileName() + ">>校验未通过");
                handleVerifyError();
                return;
            } else {
                if(verifyResult == FileDownloadHelper.DOWNLOAD_VERIFY_SUCCESS) {
                    DebugLog.log(TAG, getBean().getFileName() + ">>校验通过");
                }
            }


            //2、重命名，必选

            boolean isRenameSuccess = handleRenameFile();

            if(isRenameSuccess){
                DebugLog.log(TAG, getBean().getFileName() + ">>重命名成功");
            } else{
                DebugLog.log(TAG, getBean().getFileName() + ">>重命名失败");
            }

            //3、文件解压，可选
            boolean unzipResult = FileDownloadHelper.unzipFile(getBean(),completeFile);

            if(!unzipResult){
                DebugLog.log(TAG,getBean().getFileName() + ">>解压失败");
                handleUnzipError();
                return;
            }

        }




        /**
         * 校验错误
         */
        private void handleVerifyError() {

            if (verifyRetryTimes >= getMaxRetryTimes()) {

                DebugLog.log(TAG, "exceed max verify times,return error");

                verifyRetryTimes = 0;

                getBean().errorCode = FileDownloadConstant.FILE_DOWNLOAD_VERIFY_ERROR;

                isDownloadError = true;

            } else {

                verifyRetryTimes++;

                DebugLog.log(TAG, "verifyRetryTimes = " + verifyRetryTimes);

                //置空当前已下载大小
                getBean().completeSize = 0;

                FileDownloadHelper.clearDownloadFile(downloadingFile);

            }

        }

        /**
         * 解压错误
         */
        private void handleUnzipError() {

            if (unzipRetryTimes >= getMaxRetryTimes()) {

                DebugLog.log(TAG, "exceed max unzip times,return error");

                unzipRetryTimes = 0;

                getBean().errorCode = FileDownloadConstant.FILE_DOWNLOAD_UNZIP_ERROR;

                isDownloadError = true;

            } else {

                unzipRetryTimes++;

                DebugLog.log(TAG, "unzipRetryTimes = " + unzipRetryTimes);

                //置空当前已下载大小
                getBean().completeSize = 0;

                FileDownloadHelper.clearDownloadFile(completeFile);

            }

        }




        /**

        /**
         * 重命名文件
         */
        private boolean handleRenameFile(){

            boolean isRenameSuccess = FileDownloadHelper.renameToCompleteFile(downloadingFile, completeFile);

            if (isRenameSuccess) {
                //重命名成功，退出循环
                isDownloadSuccess = true;

            } else {

                getBean().errorCode = FileDownloadConstant.FILE_DOWNLOAD_RENAME_FAIL;
                //重命名失败，报错
                isDownloadError = true;
            }

            return isRenameSuccess;

        }

        /**
         * 检查文件是否存在
         * 若completeFile存在，则中断任务
         * 若downloadingFile存在，则计算completeSize;
         * 若downloadFile不存在，则
         *
         * @param completeFile
         * @param downloadingFile
         * @return
         */
        private boolean checkFile(FileDownloadObject fileDownloadObject, File completeFile, File downloadingFile) {

            if (!completeFile.exists()) {

                if (downloadingFile.exists()) {
                    //downloadingFile存在，计算completeSize
                    fileDownloadObject.completeSize = downloadingFile.length();

                    DebugLog.log(TAG, fileDownloadObject.getFileName() + ",文件已存在！起始位置:" + fileDownloadObject.completeSize);

                } else {
                    //downloadingFile不存在，则创建文件
                    DebugLog.log(TAG, fileDownloadObject.getFileName() + ",文件不存在,重新开始下载");

                    File parentFile = downloadingFile.getParentFile();

                    if (!parentFile.exists()) {
                        //父目录不存在，创建父目录
                        boolean isDirCreateSuccess = FileDownloadHelper.createDir(parentFile.getAbsolutePath());

                        if (!isDirCreateSuccess) {
                            DebugLog.log(TAG, fileDownloadObject.getFileName() + ",创建父目录失败");
                            fileDownloadObject.errorCode = FileDownloadConstant.FILE_DOWNLOAD_CREATE_DIR_FAIL;
                            return false;
                        }
                    }

                    //创建downloadingFile文件
                    boolean isFileCreateSuccess = FileDownloadHelper.createFile(downloadingFile.getAbsolutePath());

                    if (!isFileCreateSuccess) {
                        DebugLog.log(TAG, fileDownloadObject.getFileName() + ",创建文件失败");
                        fileDownloadObject.errorCode = FileDownloadConstant.FILE_DOWNLOAD_CREATE_FILE_FAIL;
                        return false;
                    }
                }

            } else {


                //completeFile已存在，中断任务
                DebugLog.log(TAG, fileDownloadObject.getFileName() + ">>已存在，文件下载成功,退出任务");

                if(fileDownloadObject.getDownloadConfig().needVerify){
                    //对于需要校验的文件，判断本地文件存在后，再通过校验算法进行校验
                    //若校验成功，按照原来逻辑运行，若校验失败，则需要删除文件，相当于重新开始下载
                    DebugLog.log(TAG,"needVerify = " + fileDownloadObject.getDownloadConfig().needVerify);
                    int verifyResult = FileDownloadHelper.verifyFile(fileDownloadObject,completeFile,downloadingFile);
                    if(verifyResult!=1){
                        DebugLog.log(TAG,"verify failed ,delete file = " + completeFile.getAbsolutePath());
                        completeFile.delete();
                        return true;
                    } else{
                        DebugLog.log(TAG,"verify success");
                    }
                } else{
                    DebugLog.log(TAG,"no need to verify file");

                }

                //标记下载成功
                isDownloadSuccess = true;
                //标记线程运行标识
                cancel();


                /**
                 * 在FileDownloadController中，有可能出现先回调onComplete，然后再回调onStart，
                 * 业务回调时，会发现调用完onComplete时，callback会被注销掉，因此onStart无法回调给业务方
                 * 即使onComplete时，不销毁callback，也会出现onComplete和onStart顺序颠倒的问题
                 */
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException e) {
                    ExceptionUtils.printStackTrace(e);
                    Thread.currentThread().interrupt();
                }
            }
            return true;
        }


    }


    /**
     * 获取最大重试次数
     *
     * @return
     */
    private int getMaxRetryTimes(){

        int retryTimes = getBean().getMaxRetryTimes();
        DebugLog.log(TAG,"config max retry times = " + retryTimes);
        if(retryTimes == -1){
            //表示未配置重试次数
            return DEFAULT_RETRY_TIMES;
        } else if(retryTimes > 0) {
            return retryTimes;
        } else if(retryTimes > MAX_RETRY_TIMES){
            return MAX_RETRY_TIMES;
        } else {
            //retryTimes<=0,表示配置不正确
            return DEFAULT_RETRY_TIMES;
        }

    }




}
