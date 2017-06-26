package com.iqiyi.video.download.filedownload.task;

import android.content.Context;
import android.text.TextUtils;

import com.iqiyi.video.download.engine.switcher.ISwitcher;
import com.iqiyi.video.download.engine.task.XBaseTaskExecutor;
import com.iqiyi.video.download.engine.task.runnable.XFiniteRetryRunnable;
import com.iqiyi.video.download.engine.task.runnable.XInfiniteRetryRunnable;
import com.iqiyi.video.download.filedownload.FileDownloadConstant;
import com.iqiyi.video.download.filedownload.FileDownloadHelper;
import com.iqiyi.video.download.filedownload.db.DBRequestController;
import com.iqiyi.video.download.filedownload.http.DownloadHttpAdapter;
import com.iqiyi.video.download.filedownload.pool.DownloadThreadPool;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.video.module.download.exbean.FileDownloadObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 对单个文件进行多线程分段下载的http下载任务。
 */
public class MultiDownloadFileTask extends XBaseTaskExecutor<FileDownloadObject> {

    private static final String TAG = "MultiDownloadFileTask";


    private static final int MAX_RETRY_COUNT = 6;// 最大请求次数

    private static final int BUFFER_SIZE = 32 * 1024;// 每一个分段的下载缓冲区大小

    private static final int DOWNLOAD_INTERVAL_TIME = 1 * 1000;// 刷新下载进度的时间间隔

    private static final int THREAD_MAX = 3;// 并发下载的线程数

    private static final int DEFAULT_UNIT_SIZE = 2 * 1024 * 1024;// 位图文件默认的单位(1MB)

    private static final int DEFAULT_FRAGMENT_SIZE = 10 * 1024 * 1024;// 每个逻辑分段的大小(10MB)

    private Context mContext;

    private DBRequestController mDbController;// 数据库Controller

    private MainRunnable mRunnable;// 具体执行下载的线程

    private Future mFuture;

    public MultiDownloadFileTask(Context context, FileDownloadObject bean,
                                 DBRequestController dbController) {
        this(context, bean, bean.getStatus(), dbController);
    }

    public MultiDownloadFileTask(Context context, FileDownloadObject bean,
                                 int status, DBRequestController dbController) {
        super(bean, status);
        mContext = context;
        mDbController = dbController;
    }

    @Override
    protected boolean onStart() {
        if (mRunnable != null)
            return false;

        mRunnable = new MainRunnable(mContext, getBean(), this, mDbController);
        Future mFuture = DownloadThreadPool.DOWNLOAD_POOL.submit(mRunnable);
        mRunnable.setFuture(mFuture);
        return true;
    }

    @Override
    protected boolean onPause() {
        if (mRunnable == null)
            return true;

        try
        {
            mRunnable.cancel();
            mRunnable = null;
            return true;
        }catch (Exception e)
        {
            ExceptionUtils.printStackTrace(e);
        }
        return true;
    }

    @Override
    protected boolean onAbort() {
        if (mRunnable != null) {
            mRunnable.cancel();
            mRunnable = null;
        }
        return true;
    }

    @Override
    protected boolean onEndSuccess() {
        mRunnable = null;
        return true;
    }

    @Override
    protected boolean onEndError(String errorCode, boolean retry) {
        getBean().errorCode=errorCode;
        mRunnable = null;
        return true;
    }

    @Override
    public long getCompleteSize() {
        return getBean().getCompleteSize();
    }



    /**
     * 监控下载的线程
     */
    protected static class MainRunnable extends XInfiniteRetryRunnable<FileDownloadObject>
            implements ISwitcher {

        private static ExecutorService mThreadPool;// 线程池
        static {
            createThreadPool();// 创建线程池
        }

        private Future mFuture;// 在暂停线程时用于中断阻塞的Future对象

        private String realUrl;// 真实的下载地址

        private String errorCode;// 错误码

        private boolean isDownloadSuccess;

        private long lastUpdateTime;


        private Context mContext;

        private FileDownloadObject mBean;

        private XBaseTaskExecutor<FileDownloadObject> mHost;

        private DBRequestController mDbController;

        private FragmentRunnable[] fragmentTasks;

        private Future[] fragmentFutures;

        private BitmapInfo bitmapInfo;

        private DownloadHttpAdapter<FileDownloadObject> adapter;

        protected MainRunnable(Context context, FileDownloadObject bean,
                               XBaseTaskExecutor<FileDownloadObject> host,
                               DBRequestController dbController) {
            super();

            lastUpdateTime = 0;
            isDownloadSuccess = false;

            mContext = context;
            mBean = bean;
            mHost = host;
            mDbController = dbController;

            realUrl = bean.getId();

            adapter = new DownloadHttpAdapter<>();
        }

        public void setFuture(Future future) {
            mFuture = future;
        }


        private static void createThreadPool() {
            try {
                mThreadPool = Executors.newFixedThreadPool(THREAD_MAX);
            } catch (Exception e) {
                DebugLog.log(TAG, "未知原因导致无法创建线程池" + e.getMessage());
                if (mThreadPool != null) {
                    mThreadPool.shutdownNow();
                    mThreadPool = null;
                }
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            if (mFuture != null) {
                mFuture.cancel(true);
            }
            cancelAllTask();
        }

        private void cancelAllTask() {
            if (fragmentTasks != null) {
                for(FragmentRunnable sectionTask : fragmentTasks)
                    if (sectionTask != null)
                        sectionTask.cancel();
            }
            if (fragmentFutures != null) {
                for (int i = 0; i < fragmentFutures.length; i++) {
                    Future future = fragmentFutures[i];
                    if (future != null) {
                        boolean result = future.cancel(true);
                        if(fragmentTasks != null && fragmentTasks[i] != null) {
                            DebugLog.log(TAG, "取消线程,name=" + fragmentTasks[i].logTag + ",result=" + result);
                        }
                    }
                }
            }
        }

        @Override
        public long getRetryInterval(long retryCount) {
            return 1000;
        }

        @Override
        public FileDownloadObject getBean() {
            return mBean;
        }

        @Override
        public boolean onPreExecute(FileDownloadObject bean) {

            // 修复下载目录完全被删除的情况
            File saveFile = new File(bean.getSaveDir(), bean.getFileName());
            if (!saveFile.exists()) {
                File parentDirectory = new File(saveFile.getParent());
                if (!parentDirectory.exists()) {
                    boolean result = parentDirectory.mkdirs();
                    DebugLog.log(TAG, getBean().getFileName() +
                            ",下载目录被删除,尝试重新创建结果：" + result);
                }
                try {
                    saveFile.createNewFile();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }


            // 如果被中断
            if (!isRunning()) {
                return false;
            }


            // (如果总大小未知)请求文件总大小，便于后面进行分段
            int requestCount = 0;
            Random mRandom=new Random();
            while (isRunning() && bean.totalSize == -1 && requestCount <= 5) {
                long totalSize = adapter.getFileSize(bean.getDownloadUrl());
                if (totalSize > 0) {
                    bean.totalSize=totalSize;
                    DebugLog.log(TAG, getBean().getFileName() + ",获取总大小成功!");
                    break;
                }
                if (!isRunning())
                    break;
                // 如果下载失败，等待一段时间后，再次执行
                requestCount++;
                DebugLog.log(TAG, getBean().getFileName() +
                        ",获取总大小失败，requestCount:" + requestCount);
                int sleepTime = FileDownloadHelper.getSleepTime(mRandom, requestCount);//Math.min(1000 + requestCount * 2000, 10000);
                DebugLog.log(TAG, "sleepTime->" + sleepTime);
                FileDownloadHelper.sleep(isRunning(), sleepTime);
            }
            // 如果被中断
            if (!isRunning()) {
                return false;
            }
            if (bean.totalSize == 0) {
                return false;
            }
            DebugLog.log(TAG, getBean().getFileName() + ",文件总大小是:" + bean.totalSize);

            // 读取位图文件(如果不存在，则创建)
            File bitmapFile = new File(bean.getDownloadPath() + ".bitmap");
            bitmapInfo = new BitmapInfo(bitmapFile.getAbsolutePath());
            if (!bitmapFile.exists()) {
                DebugLog.log(TAG, getBean().getFileName() + ",没有位图文件，创建位图文件");
                // 没有配置文件(如果有视频文件，则删除)
                File downloadFile = new File(bean.getSaveDir(), bean.getFileName());
                if (downloadFile.exists()) {
                    DebugLog.log(TAG, getBean().getFileName() + ",没有位图文件，但存在视频文件，则删除视频文件:"
                            + downloadFile.getAbsolutePath());
                    boolean result = downloadFile.delete();
                    DebugLog.log(TAG, getBean().getFileName() + ",删除视频文件结果:" + result);
                }
                // 如果没有配置文件，则初始化bitmapInfo，创建配置文件
                bitmapInfo.unit = DEFAULT_UNIT_SIZE;
                bitmapInfo.size = bean.totalSize;
                int bcount = (int) (bitmapInfo.size / bitmapInfo.unit);
                if (bitmapInfo.size % bitmapInfo.unit != 0) {
                    bcount++;
                }
                bitmapInfo.bits = new int[bcount];// 默认所有比特位都是0
                if (!bitmapInfo.saveToConfig()) {
                    DebugLog.log(TAG, getBean().getFileName() + ",第一次创建和保存位图文件失败!!");
                    return false;
                }
                // 创建视频文件(先占用这么大的空间)
                if (!createFullFile(downloadFile, bean.totalSize)) {
                    DebugLog.log(TAG, getBean().getFileName() + ",第一次创建视频文件(先占用这么大的空间)失败!!");
                    return false;
                }
            } else {
                if (!bitmapInfo.readFromConfig()) {
                    DebugLog.log(TAG, getBean().getFileName() + ",读取位图文件失败!!");
                    return false;
                }
                DebugLog.log(TAG, getBean().getFileName() + ",读取位图文件成功");
            }

            // 创建分段下载任务
            int fragmentCount = (int) (bitmapInfo.size / DEFAULT_FRAGMENT_SIZE);// 分段数

            if (bitmapInfo.size % DEFAULT_FRAGMENT_SIZE != 0) {
                fragmentCount++;
            }

            fragmentTasks = new FragmentRunnable[fragmentCount];

            for (int i = 0; i < fragmentCount; i++) {
                long start = i * DEFAULT_FRAGMENT_SIZE * 1L;
                long end = start + DEFAULT_FRAGMENT_SIZE - 1;
                if (end > bitmapInfo.size - 1) {// 最后一个分段，end位置不能超过文件总大小
                    end = bitmapInfo.size - 1;
                }
                fragmentTasks[i] = new FragmentRunnable(mContext, realUrl, saveFile, bitmapInfo, start, end);
            }

            // 如果线程池为空，尝试创建
            if (mThreadPool == null) {
                createThreadPool();
            }

            // 启动线程池并行下载
            fragmentFutures = new Future[fragmentCount];
            for (int i = 0; i < fragmentTasks.length; i++) {
                fragmentFutures[i] = mThreadPool.submit(fragmentTasks[i]);
            }

            return true;
        }

        @Override
        public void onPreExecuteError(FileDownloadObject bean) {

            mHost.endError(errorCode, false);
        }

        @Override
        public boolean onRepeatExecute(FileDownloadObject bean) {

            long curUpdateTime = System.currentTimeMillis();
            long curCompleteSize = 0;
            // 重新计算已下载大小
            long remain = bitmapInfo.size % bitmapInfo.unit;// 最后一个bit位的余数

            for (int i = 0; i < bitmapInfo.bits.length; i++) {
                if (bitmapInfo.bits[i] == 1) {
                    if (i == bitmapInfo.bits.length - 1 && remain != 0) {
                        curCompleteSize = curCompleteSize + remain;
                    } else {
                        curCompleteSize = curCompleteSize + bitmapInfo.unit;
                    }
                }
            }
            DebugLog.log(TAG,"bitmapInfo = " + bitmapInfo.toString());

            bean.completeSize= curCompleteSize;

            DebugLog.log(TAG, bean.getFileName() + "--downloading:"
                    + curCompleteSize + ", " + FileDownloadHelper.calculatePercent(curCompleteSize, bean.totalSize) + "%");

            // 为了防止频繁刷新界面，间隔大于1秒，且增幅大于10KB通知一次
            if (curUpdateTime - lastUpdateTime >= DOWNLOAD_INTERVAL_TIME) {

                lastUpdateTime = curUpdateTime;

                mHost.notifyDoing(-1);// 通知进度
            }

            mHost.notifyDoing(curCompleteSize);
            // 判断下载是否出错失败
            for (FragmentRunnable sectionTask : fragmentTasks) {
                // 有一个分段下载结束后失败，则整体停止下载，失败
                if (sectionTask.isError) {
                    errorCode = sectionTask.errorCode;
                    isDownloadSuccess = false;
                    return true;
                }
            }
            // 判断下载是否结束
            for (int i : bitmapInfo.bits) {
                // 有一个bit位没下完就是没下完
                if (i == 0)
                    return false;
            }
            // 下载成功
            bean.completeSize = (bean.totalSize);
            mHost.notifyDoing(-1);// 通知进度
            isDownloadSuccess = true;
            return true;
        }

        @Override
        public void onPostExecute(FileDownloadObject bean) {
            // 停止所有下载
            cancelAllTask();

            if (isDownloadSuccess) {
                DebugLog.log(TAG, bean.getFileName() + ",下载结束，成功");
                mHost.endSuccess();
            } else {
                DebugLog.log(TAG, bean.getFileName() + ",下载失败了，errorCode:" + errorCode);
                mHost.endError(errorCode, true);
            }
        }

        @Override
        public void onCancelled(FileDownloadObject bean) {
            DebugLog.log(TAG, bean.getFileName() + "，下载中断..");
        }

        @Override
        public boolean isOn() {
            return isRunning();
        }



        private static boolean createFullFile(File file, long size) {
            RandomAccessFile raFile = null;
            try {
                if (!file.exists()) {
                    File dir = file.getParentFile();
                    if (dir != null)
                        dir.mkdirs();
                    file.createNewFile();
                }
                 raFile = new RandomAccessFile(file, "rwd");
                raFile.seek(0);
                raFile.writeByte(1);
                raFile.seek(size - 1);
                raFile.writeByte(1);
                return true;
            } catch (IOException e) {
                ExceptionUtils.printStackTrace(e);
            }finally {
                if(raFile != null){
                    try{
                        raFile.close();
                    }catch (IOException e){
                        ExceptionUtils.printStackTrace(e);
                    }
                }
            }
            return false;
        }
    }

    /**
     * 每个分段下载的线程
     */
    private static class FragmentRunnable extends XFiniteRetryRunnable<String> {
        private Context mContext;

        private String logTag;// 打印日志的该分段的tag
        private String mUrl;
        private File mSaveFile;
        private BitmapInfo mBitmapInfo;// 代表位图文件的对象
        private long mStartLoc;// 分段的起始位置(单位:byte)
        private long mEndLoc;// 分段的结束位置(单位:byte)
        private int mCurrentBit;// 当前正在下载的对应bit位置

        private byte[] mBuffer;// 内存缓存区
        private String errorCode;
        private boolean isError;// 标识是否出错
        private DownloadHttpAdapter<FileDownloadObject> adapter = null;

        public FragmentRunnable(Context context, String url, File saveFile,
                                BitmapInfo bitmapInfo, long start, long end) {
            super(MAX_RETRY_COUNT);
            mContext = context;
            mUrl = url;
            mSaveFile = saveFile;
            mBitmapInfo = bitmapInfo;
            mStartLoc = start;
            mEndLoc = end;
            adapter = new DownloadHttpAdapter<>();

            logTag = "Fragment" + mStartLoc + "_" + mEndLoc;
        }

        @Override
        public void cancel() {
            super.cancel();

        }

        @Override
        public long getRetryInterval(long retryCount) {
            return 10000;
        }

        @Override
        public String getBean() {
            return mUrl;
        }

        @Override
        public boolean onPreExecute(String url) {
            if (mStartLoc > mEndLoc) {
                DebugLog.log(TAG, logTag + ",该分段已下载完成!");
                return false;
            }
            // 跳过已经下载完的bit位，从未下载的bit位开始
            mCurrentBit = (int) (mStartLoc / mBitmapInfo.unit);
            while (mBitmapInfo.bits[mCurrentBit] != 0) {
                // 当前bit位已经下完，则检查下一个bit位，递增mStartLoc
                mStartLoc = mStartLoc + mBitmapInfo.unit;
                mCurrentBit++;
                if (mStartLoc > mEndLoc) {
                    DebugLog.log(TAG, logTag + ",该分段已下载完成!2");
                    return false;
                }
            }

            mBuffer = new byte[BUFFER_SIZE];
            return true;
        }

        @Override
        public void onPreExecuteError(String url) {
        }

        @Override
        public boolean onRepeatExecute(String url) {
            if (mStartLoc > mEndLoc) {
                DebugLog.log(TAG, logTag + "该分段已经下载完成，不用再下载了!");
                return true;
            }
            DebugLog.log(TAG, logTag + "该分段开始下载，下载范围:" + mStartLoc
                    + " - " + mEndLoc + ",当前bit位置:" + mCurrentBit);
            DebugLog.log(TAG, logTag + ",下载realUrl:" + url);

            HttpURLConnection connection = null;

            InputStream inputStream = null;

            RandomAccessFile raFile = null;
            BufferedInputStream bis = null;

            try {

                connection = adapter.getConnection(url,mStartLoc,mEndLoc);

                // http无响应
                if (connection == null) {
                    DebugLog.log(TAG, logTag + ",http无响应,response == null");
                    errorCode = FileDownloadConstant.FILE_DOWNLOAD_IO_EXCEPTION;
                    return false;
                }
                // 如果被中断
                if (!isRunning()) {
                    DebugLog.log(TAG, logTag + " Is Cancelled1");
                    return false;
                }

                int statusCode =  -1;

                statusCode = connection.getResponseCode();


                DebugLog.log(TAG, logTag + ",服务器返回状态:" + statusCode);
                // 错误返回码，重新请求
                if (statusCode != 200 && statusCode != 206 && statusCode != 416) {
                    DebugLog.log(TAG, logTag + ",下载错误，重新请求");
                    errorCode = FileDownloadConstant.FILE_DOWNLOAD_ILLEGAL_RESPONSE_CODE;
                    return false;
                }

                inputStream = connection.getInputStream();
                // http没返回InputStream
                if (inputStream == null) {
                    DebugLog.log(TAG, logTag + ",服务器响应没有内容，InputStream == null");
                    errorCode = FileDownloadConstant.FILE_DOWNLOAD_INPUTSTREAM_IS_NULL;
                    return false;
                }

                long length = connection.getContent() == null ? 0 : connection.getContentLength();
                DebugLog.log(TAG, logTag + "，服务器返回ContentLength =" + length);
                if (length <= 0) {
                    DebugLog.log(TAG, logTag + ",请求失败,ContentLength<0");
                    errorCode = FileDownloadConstant.FILE_DOWNLOAD_ILLEGAL_LENGTH;
                    return false;
                }

                // 写入文件
                long downloadSize = 0;// 这次连接总共读取的累计数据量
                long readUnitSize = 0;// 几次read读取的数据量，到达bit单位大小(1MB)清零重新计数


                    bis = new BufferedInputStream(inputStream);
                    raFile = new RandomAccessFile(mSaveFile, "rwd");
                    raFile.seek(mStartLoc);
                    int bufferStart = 0;// 之前read读取的数据量
                    int numRead = 0;// 一次read读取的数据量
                    while (true) {
                        if (!isRunning()) {// 如果被中断，则整体退出
                            DebugLog.log(TAG, logTag + " Is Cancelled2");
                            return false;
                        }
                        numRead = bis.read(mBuffer, bufferStart, BUFFER_SIZE - bufferStart);
                        // 已经没有数据了，退出循环
                        if (numRead == -1) {
                            if (bufferStart > 0) {// buffer未填充满，但已经没数据了，则写入文件
                                raFile.write(mBuffer, 0, bufferStart);
                                readUnitSize = readUnitSize + bufferStart;
                                if (mStartLoc + readUnitSize > mEndLoc) {// 如果是最后一部分剩余数据(不足1MB)
                                    mStartLoc = mStartLoc + readUnitSize;
                                    mBitmapInfo.setBitAndSaveToConfig(mCurrentBit);// 当前bit单位写完成
                                }
                            }
                            break;
                        }
                        downloadSize = downloadSize + numRead;// 递增已下载大小
                        // buffer未填满
                        if (numRead + bufferStart < BUFFER_SIZE) {
                            bufferStart = numRead + bufferStart;
                        }
                        // buffer已填满，则写入文件
                        else {
                            raFile.write(mBuffer, 0, BUFFER_SIZE);
                            bufferStart = 0;// buffer重新开始填充
                            readUnitSize = readUnitSize + BUFFER_SIZE;
                            if (readUnitSize == mBitmapInfo.unit) {
                                mStartLoc = mStartLoc + mBitmapInfo.unit;
                                mBitmapInfo.setBitAndSaveToConfig(mCurrentBit);// 当前bit单位写完成
                                mCurrentBit++;// 递增到下一个bit单元(下一个1MB)
                                readUnitSize = 0;
                            }
                        }
                    }

                    DebugLog.log(TAG, logTag + ",此次下载结束。mStartLoc=" + mStartLoc + ",mEndLoc" + mEndLoc);
                    if (mStartLoc > mEndLoc) {
                        DebugLog.log(TAG, logTag + ",下载完成!");
                        return true;
                    }


                return false;
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
                return false;
            } finally {

                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        ExceptionUtils.printStackTrace(e);
                    }
                }

                if(inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        ExceptionUtils.printStackTrace(e);
                    }
                }

                if (raFile != null) {
                    try {
                        raFile.close();
                    } catch (Exception e) {
                        ExceptionUtils.printStackTrace(e);
                    }
                }

                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        @Override
        public void onPostExecute(String url) {
            mBuffer = null;// 回收缓存数组
            isError = mStartLoc < mEndLoc;// 如果重试多次还没下载完，则认为失败
        }

        @Override
        public void onCancelled(String url) {
            mBuffer = null;// 回收缓存数组
        }
    }

    /**
     * 位图信息，代表分段下载的进度。
     */
    private static class BitmapInfo {
        public String savePath;// 位图文件存放位置
        public long unit;// 单个比特代表的单位大小
        public long size;// 该位图文件代表的总大小
        public volatile int[] bits;// 比特数组

        private BitmapFile config;

        @Override
        public String toString() {
            return "BitmapInfo{" +
                    "savePath='" + savePath + '\'' +
                    ", unit=" + unit +
                    ", size=" + size +
                    ", bits=" + Arrays.toString(bits) +
                    '}';
        }

        public BitmapInfo(String savePath) {
            this.config = new BitmapFile();
            this.savePath = savePath;
        }

        public boolean readFromConfig() {
            if (config != null) {
                try {
                    File file = new File(savePath);
                    return config.parse(file, this);
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                    return false;
                }
            }
            return false;
        }

        public synchronized boolean saveToConfig() {
            if (config != null) {
                config.create(this);
                return config.update(this);
            }
            return false;
        }

        public synchronized boolean setBitAndSaveToConfig(int i) {
            if (bits != null) {
                bits[i] = 1;
                if (config != null) {
                    config.create(this);
                    return config.update(this);
                }
            }
            return false;
        }
    }

    /**
     * 位图配置文件。用来记录分段下载的进度。
     */
    private static class BitmapFile {
        private final static String KEY_UNIT = "unit";// 单个比特代表的单位大小
        private final static String KEY_SIZE = "size";// 该位图文件代表的总大小
        private final static String KEY_BITS = "bits";// 比特数组

        public boolean create(BitmapInfo bean) {
            File file = getFile(bean);
            if (file.exists()) {
                return false;
            }
            try {
                // 如果父目录不存在，则创建
                File dir = file.getParentFile();
                if (!dir.exists()) {
                    boolean b = dir.mkdirs();
                    DebugLog.log(TAG, "Bitmap File create-dir return: " + b
                            + ",dirPath:" + dir.getAbsolutePath());
                }
                // 创建文件
                if (!file.createNewFile()) {
                    DebugLog.log(TAG, "Bitmap File create return false,filePath:"
                            + file.getAbsolutePath());
                    return false;
                }
                return file.exists();
            } catch (IOException e) {
                DebugLog.log(TAG, "Bitmap File create throw Exception:" + e
                        + ",filePath:" + file.getAbsolutePath());
                ExceptionUtils.printStackTrace(e);
                return false;
            }
        }

        public boolean update(BitmapInfo bean) {
            Properties properties = new Properties();
            properties.put(KEY_UNIT, Long.toString(bean.unit));
            properties.put(KEY_SIZE, Long.toString(bean.size));
            if (bean.bits != null) {
                StringBuilder sb = new StringBuilder();
                for (int b : bean.bits) {
                    sb.append(b);
                }
                properties.put(KEY_BITS, sb.toString());
            }
            try {
                DebugLog.log(TAG, "更新位图文件的路径:" + getFile(bean));
                FileOutputStream stream = new FileOutputStream(getFile(bean));
                properties.store(stream, "");
                stream.close();
                return true;
            } catch (FileNotFoundException e) {
                DebugLog.log(TAG, "更新位图文件FileNotFoundException");
                ExceptionUtils.printStackTrace(e);
            } catch (IOException e) {
                DebugLog.log(TAG, "更新位图文件IOException");
                ExceptionUtils.printStackTrace(e);
            } catch (Exception e) {
                DebugLog.log(TAG, "更新位图文件Exception");
                ExceptionUtils.printStackTrace(e);
            }
            return false;
        }

        public boolean parse(File file, BitmapInfo result) throws IOException {
            DebugLog.log(TAG, "加载位图文件...");
            if (file == null || !file.exists() || result == null)
                return false;

            DebugLog.log(TAG, "加载位图文件2...");
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(file);
                Properties properties = new Properties();
                properties.load(stream);
                result.unit = Long.parseLong(properties.getProperty(KEY_UNIT, "0"));
                result.size = Long.parseLong(properties.getProperty(KEY_SIZE, "0"));
                String bitStr = properties.getProperty(KEY_BITS, "");
                if (!TextUtils.isEmpty(bitStr)) {
                    int[] bits = new int[bitStr.length()];
                    char[] chars = bitStr.toCharArray();
                    for (int i = 0; i < chars.length; i++) {
                        bits[i] = Integer.parseInt("" + chars[i]);
                    }
                    result.bits = bits;
                }
            } finally {
                if (stream != null)
                    try {
                        stream.close();
                    } catch (Exception e) {
                        ExceptionUtils.printStackTrace(e);
                    }
            }
            return true;
        }

        public File getFile(BitmapInfo bean) {
            return new File(bean.savePath);
        }
    }
}
