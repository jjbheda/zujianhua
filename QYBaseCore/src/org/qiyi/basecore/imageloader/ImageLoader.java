package org.qiyi.basecore.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;
import android.widget.ImageView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.imageloader.gif.GifDecode.GifDrawableDecode;
import org.qiyi.basecore.imageloader.gif.GifDrawable;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.UIUtils;
import org.qiyi.net.Request;
import org.qiyi.net.Response;
import org.qiyi.net.callback.IHttpCallback;
import org.qiyi.net.convert.IResponseConvert;
import org.qiyi.net.exception.HttpException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageLoader {

    /**
     * 统计图片平均加载时常，单位ms/张
     */
    public static class ImageLoadTracker {

        public static final int LOADED_BY_FRESCO_UNKNOWN = 0;
        public static final int LOADED_BY_FRESCO_MEMORY = LOADED_BY_FRESCO_UNKNOWN + 1;
        public static final int LOADED_BY_FRESCO_DISK = LOADED_BY_FRESCO_MEMORY + 1;
        public static final int LOADED_BY_FRESCO_NET = LOADED_BY_FRESCO_DISK + 1;

        public static final int LOADED_BY_QIYI_RETURN = LOADED_BY_FRESCO_NET + 1;
        public static final int LOADED_BY_QIYI_MEMORY = LOADED_BY_QIYI_RETURN + 1;
        public static final int LOADED_BY_QIYI_DISK = LOADED_BY_QIYI_MEMORY + 1;
        public static final int LOADED_BY_QIYI_NET = LOADED_BY_QIYI_DISK + 1;

        public static final String KEY_FOR_AVERAGE = "KEY_FOR_AVERAGE";

        /**
         * key:     图片请求地址
         * value:   该url首次请求时间
         */
        private Map<String, Long> record = new LinkedHashMap<String, Long>() {
            @Override
            protected boolean removeEldestEntry(Entry<String, Long> eldest) {
                return size() > 100;
            }
        };
        private int mQiyiCount;
        private int mFrescoCount;
        private long mTotalTakes;
        private boolean hasPosted;

        private void updateTotalTakes(long takes) {
            if (Long.MAX_VALUE - mTotalTakes > takes) {
                mTotalTakes += takes;
            }
        }

        public void onAddTask(String url, boolean byFresco) {
            synchronized (record) {
                if (!record.containsKey(url)) {
                    record.put(url, System.currentTimeMillis());
                }
            }
            if (byFresco) {
                mFrescoCount++;
            } else {
                mQiyiCount++;
            }

            if (!hasPosted && sIGetFrescoSwitch != null) {
                sIGetFrescoSwitch.sendStatistic();
                hasPosted = true;
            }
        }

        public void onTaskComplete(String url, boolean success, int from) {

            if (DebugLog.isDebug()) {
                DebugLog.v(ImageLoadTracker.class.getSimpleName()
                        , "onTaskComplete, success: " + success + " remains "
                                + mFrescoCount + "/" + mQiyiCount + " from " + getFromStr(from));
            }

            Long startAt;
            synchronized (record) {
                startAt = record.get(url);
                if (startAt != null) {
                    record.remove(url);
                }
            }

            if (success && startAt != null) {
                updateTotalTakes(System.currentTimeMillis() - startAt);
            }
        }

        private String getFromStr(int from) {
            String ret = null;
            switch (from) {
                case LOADED_BY_FRESCO_UNKNOWN:
                    ret = "LOADED_BY_FRESCO_UNKNOWN";
                    break;
                case LOADED_BY_FRESCO_MEMORY:
                    ret = "LOADED_BY_FRESCO_MEMORY";
                    break;
                case LOADED_BY_FRESCO_DISK:
                    ret = "LOADED_BY_FRESCO_DISK";
                    break;
                case LOADED_BY_FRESCO_NET:
                    ret = "LOADED_BY_FRESCO_NET";
                    break;
                case LOADED_BY_QIYI_RETURN:
                    ret = "LOADED_BY_QIYI_RETURN";
                    break;
                case LOADED_BY_QIYI_MEMORY:
                    ret = "LOADED_BY_QIYI_MEMORY";
                    break;
                case LOADED_BY_QIYI_DISK:
                    ret = "LOADED_BY_QIYI_DISK";
                    break;
                case LOADED_BY_QIYI_NET:
                    ret = "LOADED_BY_QIYI_NET";
                    break;
            }
            return ret;
        }

        public void saveAverageStatistic() {
            int sum;
            long average;
            if (sIGetFrescoSwitch != null
                    && mTotalTakes > 0
                    && (sum = mFrescoCount + mQiyiCount) > 0
                    && (average = mTotalTakes / sum) < 10_000) {
                sIGetFrescoSwitch.updateStatistic(average);
            }
        }
    }

    public static final ImageLoadTracker sImageLoadTracker = new ImageLoadTracker();

    private static IGetFrescoSwitch sIGetFrescoSwitch;

    public static IGetFrescoSwitch getsIGetFrescoSwitch() {
        return sIGetFrescoSwitch;
    }

    public static void setsIGetFrescoSwitch(IGetFrescoSwitch sIGetFrescoSwitch) {
        ImageLoader.sIGetFrescoSwitch = sIGetFrescoSwitch;
    }

    /**
     * Interface for the response handlers on image requests.<br>
     * <p/>
     * The call flow is this:<br>
     * 1. Upon being attached to a request, onResponse(bitmap, true) will be invoked to reflect any
     * cached data that was already available. If the data was available.<br>
     * <p/>
     * 2. After a network response returns, only one of the following cases will happen:<br>
     * - onResponse(bitmap, false) will be called if the image was loaded. or<br>
     * - onErrorResponse will be called if there was an error loading the image.
     */
    public interface ImageListener {
        void onSuccessResponse(Bitmap bitmap, String url, boolean isCached);

        void onErrorResponse(int errorCode);
    }

    private static final String TAG = "ImageLoader";

    /**
     * @author dragon 图片类型定义
     */
    public enum ImageType {
        PNG, JPG, CIRCLE, GIF
    }

    // 线程工厂
    private final ThreadFactory sThreadFactoryDisk = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, TAG + ":disk:" + mCount.getAndIncrement());
        }
    };

    private final ThreadFactory sThreadFactoryNet = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, TAG + ":net:" + mCount.getAndIncrement());
        }
    };

    /**
     * 用于记录由于Identity相同，而没有得到执行的任务（因为这些任务有可能需要执行结果的回调） key 由 CustomRunnable 的getSubIdentity提供
     */
    private Map<String, CustomRunnable> mSameIdentityTaskMap =
            new LinkedHashMap<String, CustomRunnable>() {

                private static final long serialVersionUID = -3664050382241914314L;

                @Override
                protected boolean removeEldestEntry(Entry<String, CustomRunnable> eldest) {
                    return size() > 40;
                }
            };

    // 线程池
    private final CustomThreadPoolExecutor EXECUTOR_FOR_DISK = new CustomThreadPoolExecutor(2, 2,
            2, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(40), sThreadFactoryDisk,
            new ThreadPoolExecutor.DiscardOldestPolicy(), mSameIdentityTaskMap);

    private final CustomThreadPoolExecutor EXECUTOR_FOR_NETWORK = new CustomThreadPoolExecutor(10,
            10, 2, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1), sThreadFactoryNet,
            new ThreadPoolExecutor.DiscardOldestPolicy(), mSameIdentityTaskMap);
    // 下载队列监视器
    private MessageMonitor mMessageMonitor = new MessageMonitor();
    // 写磁盘队列监视器
    private BitmapToDiskMonitor mMessageMonitor2 = new BitmapToDiskMonitor();

    // 内存缓存
    public static ImgCacheMap<String, Resource<?>> mImageCacheMap = new ImgCacheMap<String, Resource<?>>(5, true);
    // 磁盘缓存
    private DiskCache mDiskCache = new DiskCache();
    // //通知ui更新
    // private Handler mMainHandler = new Handler(Looper.getMainLooper());
    // 单例保存
    private static ImageLoader mImageLoader = null;
    // 初始化锁
    private static Object sInitLock = new Object();

    // 统计效率相关参数
    private static volatile long sTotalLoadImageCount = 0;
    private static volatile long sLoadImageFromNetCount = 0;
    private static volatile long sLoadImageFromDiskCount = 0;

    /**
     * 图片加载器构造函数
     */
    private ImageLoader() {
        if (Build.VERSION.SDK_INT >= 9) {
            EXECUTOR_FOR_DISK.allowCoreThreadTimeOut(true);
            EXECUTOR_FOR_NETWORK.allowCoreThreadTimeOut(true);
        }

        EXECUTOR_FOR_NETWORK.execute(mMessageMonitor);
        EXECUTOR_FOR_NETWORK.execute(mMessageMonitor2);
    }

    /**
     * 获得单例对象
     *
     * @return
     */
    private static ImageLoader getInstance() {
        synchronized (sInitLock) {
            if (mImageLoader == null) {
                mImageLoader = new ImageLoader();
            }
        }
        return mImageLoader;
    }

    /**
     * Update disk cache max size
     *
     * @param maxSize (MB) equals (maxsize * 1024 * 1024)
     */
    public void updateDiskCacheMaxSize(int maxSize) {
        getInstance().mDiskCache.updateMaxSize(maxSize, DiskCache.DISK_CACHE_TYPE_COMMON);
    }

    /**
     * 根据URL判断是不是gif 格式
     *
     * @param url
     * @return
     */
    private static Boolean isGIf(String url) {
        Boolean ret = false;
        if (url != null && url.endsWith(".gif")) {
            ret = true;
        }
        return ret;
    }

    /**
     * 加载图片，以JPG格式处理图片，透明部分压缩会变黑。
     * 支持gif,通过url 判断，需要后台把gif 的url配置成“.gif”结尾
     * 非gif 图片URL 不可以".gif"结尾，否则图片无法显示
     *
     * @param iv
     */
    public static void loadImage(ImageView iv) {
        if (iv != null && iv.getContext() != null) {
            String url = null;
            if (iv.getTag() instanceof String) {
                url = (String) iv.getTag();
            }
            if (url != null) {
                if (isGIf(url)) {
                    loadImage(iv.getContext(), url, iv, ImageType.GIF, false, null,
                            DiskCache.DISK_CACHE_TYPE_COMMON, false);
                } else {
                    loadImage(iv.getContext(), url, iv, ImageType.JPG, false, null,
                            DiskCache.DISK_CACHE_TYPE_COMMON);
                }
            }
        }
    }

    /**
     * 加载gif 图片
     */
    @Deprecated
    public static void loadGifImage(ImageView iv) {
        if (iv != null && iv.getContext() != null) {
            loadImage(iv.getContext(), null, iv, ImageType.GIF, false, null, DiskCache.DISK_CACHE_TYPE_COMMON, false);
        }
    }


    /**
     * 从本地文件夹中加载图片，本地图片路径通过 view.setTag()方式随ImageView携带下来
     *
     * @param iv
     */
    public static void loadImageFromLocalExistImg(ImageView iv) {
        if (iv != null && iv.getContext() != null) {
            String url = null;
            if (iv.getTag() instanceof String) {
                url = (String) iv.getTag();
            }
            if (isGIf(url)) {
                loadImage(iv.getContext(), url, iv, ImageType.GIF, false, null,
                        DiskCache.DISK_CACHE_TYPE_COMMON, true);
            } else {
                loadImage(iv.getContext(), url, iv, ImageType.JPG, false, null,
                        DiskCache.DISK_CACHE_TYPE_COMMON, true);
            }
        }
    }

    /**
     * 加载图片，以PNG格式处理图片，并设置默认图片。
     *
     * @param iv
     * @param defaultResId
     */
    public static void loadImage(ImageView iv, int defaultResId) {
        if (iv != null) {
            if (iv.getContext() != null) {
                String url = null;
                if (iv.getTag() instanceof String) {
                    url = (String) iv.getTag();
                }
                if (url != null) {
                    if (isGIf(url)) {
                        loadImage(iv.getContext(), url, iv, ImageType.GIF, false, null,
                                DiskCache.DISK_CACHE_TYPE_COMMON);
                    } else {
                        iv.setImageResource(defaultResId);
                        loadImage(iv.getContext(), url, iv, ImageType.PNG, false, null,
                                DiskCache.DISK_CACHE_TYPE_COMMON);
                    }
                }
            }
        }
    }

    /**
     * 加载图片，以PNG格式处理图片，以保留透明部分。
     *
     * @param iv
     */
    public static void loadImageWithPNG(ImageView iv) {
        if (iv != null && iv.getContext() != null) {
            String url = null;
            if (iv.getTag() instanceof String) {
                url = (String) iv.getTag();
            }
            if (url != null) {
                if (isGIf(url)) {
                    loadImage(iv.getContext(), url, iv, ImageType.GIF, false, null,
                            DiskCache.DISK_CACHE_TYPE_COMMON);
                } else {
                    iv.setImageBitmap(null);
                    loadImage(iv.getContext(), url, iv, ImageType.PNG, false, null,
                            DiskCache.DISK_CACHE_TYPE_COMMON);
                }
            }
        }
    }

    /**
     * 加载图片，自动添加圆角处理
     *
     * @param iv
     */
    @Deprecated
    public static void loadImageCircle(ImageView iv) {
        if (iv != null && iv.getContext() != null) {
            loadImage(iv.getContext(), null, iv, ImageType.CIRCLE, false, null,
                    DiskCache.DISK_CACHE_TYPE_COMMON);
        }
    }

    @Deprecated
    public static Object getBigMap(String key, ImageType imageType) {
        if (null != getInstance().mImageCacheMap.getResource(key + imageType)) {
            return getInstance().mImageCacheMap.getResource(key + imageType).getResource();
        } else {
            return null;
        }
    }

    public static void loadImage(Context context, String url, ImageListener imgListener,
                                 boolean isFullQuality) {
        if (isGIf(url)) {
            loadImage(context, url, null, ImageType.GIF, isFullQuality, imgListener,
                    DiskCache.DISK_CACHE_TYPE_COMMON);
        } else {
            loadImage(context, url, null, ImageType.PNG, isFullQuality, imgListener,
                    DiskCache.DISK_CACHE_TYPE_COMMON);
        }
    }

    public static void loadImage(ImageView img, ImageListener imgListener, boolean isFullQuality) {
        if (img != null && img.getContext() != null) {
            String url = null;
            if (img.getTag() instanceof String) {
                url = (String) img.getTag();
            }
            if (isGIf(url)) {
                loadImage(img.getContext(), url, img, ImageType.GIF, isFullQuality, imgListener,
                        DiskCache.DISK_CACHE_TYPE_COMMON);
            } else {
                loadImage(img.getContext(), url, img, ImageType.PNG, isFullQuality, imgListener,
                        DiskCache.DISK_CACHE_TYPE_COMMON);
            }
        }
    }

    /**
     * @param context
     * @param url
     * @param iv                  假如不为null，其tag必须和url相同
     * @param type
     * @param isFullQuality
     * @param imgListener
     * @param diskCacheType
     * @param isLoadLocalExistImg
     */
    public static void loadImage(Context context, String url, ImageView iv,
                                 ImageType type, boolean isFullQuality, ImageListener imgListener,
                                 int diskCacheType, boolean isLoadLocalExistImg) {

        if (DebugLog.isDebug() && iv != null && !(iv instanceof SimpleDraweeView)) {
            DebugLog.e(TAG, "loadImage, not a QiyiDraweeView, but a " + iv.getClass().getName());
        }

        Context finalContext = null;
        if (null != context) {
            finalContext = context.getApplicationContext();
        } else if (null != iv) {
            finalContext = iv.getContext().getApplicationContext();
        }
        if (null == finalContext) {
            return;
        }

        String finalUrl = null;
        if (!TextUtils.isEmpty(url)) {
            finalUrl = url;
        } else if (iv != null && (iv.getTag() instanceof String)) {
            finalUrl = (String) iv.getTag();
        } else {
            if (imgListener != null) {
                imgListener.onErrorResponse(-1);
            }
            return;
        }
        if (DebugLog.isDebug()) {
            sTotalLoadImageCount++;
            DebugLog.v(TAG, "Totally loadImage count: " + sTotalLoadImageCount);
            DebugLog.v("finalurlIMage", finalUrl);
        }

        //在这里区分出fresco图片加载
        if (imgListener == null && iv instanceof SimpleDraweeView && sIGetFrescoSwitch != null &&
                sIGetFrescoSwitch.frescoEnabled()) {
            try {
                SimpleDraweeView draweeView = ((SimpleDraweeView) iv);
                draweeView.setImageURI(Uri.parse(finalUrl));

            } catch (Exception e) {
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                    throw new RuntimeException(e);
                }
            }
            return;
        }

        sImageLoadTracker.onAddTask(finalUrl, false);
        // 取内存资源
        Resource<?> bt = getInstance().getBitmapFromMemory(finalUrl, type);
        Object ob = null;
        if (bt != null) {
            ob = bt.getResource();
        }

        if (ob != null) {
            DebugLog.v(TAG, "loadImage memory: " + finalUrl);
            sImageLoadTracker.onTaskComplete(finalUrl, true, ImageLoadTracker.LOADED_BY_QIYI_MEMORY);
            if (ob instanceof Bitmap && !type.equals(ImageType.GIF)) {
                if (iv != null && finalUrl.equals(iv.getTag())) {
                    iv.setImageBitmap((Bitmap) ob);
                    if (imgListener != null) {
                        imgListener.onSuccessResponse((Bitmap) ob, finalUrl, true);
                    }
                } else {
                    if (imgListener != null) {
                        imgListener.onSuccessResponse((Bitmap) ob, finalUrl, true);
                    }
                }
            } else if (ob instanceof GifDrawable) {
                if(iv != null) {
                    iv.setImageDrawable((GifDrawable) ob);
                }
            }
            return;
        }
        // 取磁盘bitmap
        if (iv != null) {
            getInstance().getBitmapFromDisk(finalContext, iv, type,
                    isFullQuality, imgListener, diskCacheType,
                    isLoadLocalExistImg);
        } else {
            getInstance().getBitmapFromDisk(finalContext, finalUrl, type,
                    isFullQuality, imgListener, diskCacheType,
                    isLoadLocalExistImg);
        }
    }

    private static void loadImage(Context context, String url, ImageView iv,
                                  ImageType type, boolean isFullQuality, ImageListener imgListener,
                                  int diskCacheType) {

        loadImage(context, url, iv, type, isFullQuality, imgListener,
                diskCacheType, false);
    }

    /**
     * 控制滑动时不添加网络下载任务
     *
     * @param flag
     */
    public static void setPauseWork(boolean flag) {
        getInstance().mMessageMonitor.setPause(flag);
    }

    /**
     * 取得内存中bitmap
     *
     * @param key
     * @return
     */
    private Resource<?> getBitmapFromMemory(String key, ImageType imageType) {
        String cacheKey = key + String.valueOf(imageType);
        return mImageCacheMap.getResource(cacheKey);
    }

    /**
     * 添加图片到内存
     *
     * @param key
     * @param bt
     */
    private void putResourceToMemory(String key, Resource<?> bt, ImageType imageType) {
        String cacheKey = key + String.valueOf(imageType);
        mImageCacheMap.putResource(cacheKey, bt);
    }

    /**
     * 获取disk图片 异步加载
     *
     * @param iv
     */
    private void getBitmapFromDisk(Context appContext, ImageView iv,
                                   ImageType type, boolean isFullQuality, ImageListener callBack,
                                   int diskCacheType, boolean isLoadLocalExistImg) {
        EXECUTOR_FOR_DISK.execute(new DiskLoader(appContext, iv, type,
                isFullQuality, callBack, diskCacheType, isLoadLocalExistImg));
    }

    private void getBitmapFromDisk(Context appContext, String url, ImageType type,
                                   boolean isFullQuality, ImageListener callBack, int diskCacheType,
                                   boolean isLoadLocalExistImg) {
        EXECUTOR_FOR_DISK.execute(new DiskLoader(appContext, url, type, isFullQuality, callBack,
                diskCacheType, isLoadLocalExistImg));
    }

    public interface IGetFrescoSwitch {

        boolean frescoEnabled();

        void sendStatistic();

        void updateStatistic(long average);
    }

    /**
     * @author dragon 磁盘数据加载
     */
    class DiskLoader extends CustomRunnableImp {

        public DiskLoader(Context appContext, ImageView iv, ImageType type, boolean isFullQuality,
                          ImageListener imgLis, int diskCacheType, boolean isLoadLocalExistImage) {
            super(appContext, iv, type, isFullQuality, imgLis, diskCacheType, isLoadLocalExistImage);
        }

        public DiskLoader(Context appContext, String url, ImageType type, boolean isFullQuality,
                          ImageListener imgLis, int diskCacheType, boolean isLoadLocalExistImage) {
            super(appContext, url, type, isFullQuality, imgLis, diskCacheType,
                    isLoadLocalExistImage);
        }

        @Override
        public void run() {
            if (TextUtils.isEmpty(mUrl)) {
                DebugLog.v("DiskLoader", "processDiskBitmap mUrl null: " + mUrl);
                return;
            }

            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            processDiskBitmap();
        }

        /**
         * 处理磁盘图片
         */
        private void processDiskBitmap() {
            if (!validateProcess()) {
                return;
            }
            if (mAppContext == null) {
                DebugLog.v("DiskLoader", "DiskLoader run context is null: " + mUrl);
                sImageLoadTracker.onTaskComplete(mUrl, false, ImageLoadTracker.LOADED_BY_QIYI_RETURN);
                return;
            }
            DebugLog.v("DiskLoader", "DiskLoader Start : " + mUrl);


            Resource<?> bt = mDiskCache.getBitmapFromDisk(mAppContext, mUrl, mImageType,
                    mIsFullQuality, mDiskCacheType, mIsLoadLocalExistImage);

            // 取得磁盘资源（资源有可能是图片或者gif动态图）
            if (bt != null) {
                DebugLog.v("DiskLoader", "DiskLoader disk data back :" + mUrl);
                putResourceToMemory(mUrl, bt, mImageType);
                if (DebugLog.isDebug()) {
                    sLoadImageFromDiskCount++;
                    DebugLog.v(TAG, "LoadImage from disk count: " + sLoadImageFromDiskCount);
                }
                onResult(bt, true);
            } else {
                if (!mIsLoadLocalExistImage) {
                    // 取网络图片
                    DebugLog.v("DiskLoader", "DiskLoader load net : " + mUrl);

                    ImageView iv = null;
                    if (mImageView != null) {
                        iv = mImageView.get();
                    }
                    if (iv != null) {
                        mMessageMonitor.addRunnable(new ImageDownloader(mAppContext, iv, mImageType,
                                mIsFullQuality, mImgListener, mDiskCacheType));
                    } else {
                        mMessageMonitor.addRunnable(new ImageDownloader(mAppContext, mUrl, mImageType,
                                mIsFullQuality, mImgListener, mDiskCacheType));
                    }
                }
            }
        }
    }

    private static class CustomRunnableImp extends CustomRunnable {

        // 保存图片view
        protected WeakReference<ImageView> mImageView = null;

        // 图片地址
        protected String mUrl = null;

        // 是否是下载广告
        protected ImageType mImageType = ImageType.JPG;

        // 执行完成结果
        private WeakReference<Resource<?>> bitmapWR;

        protected boolean mIsFullQuality = false;

        // 可能会被设置的，执行完的回调
        protected ImageListener mImgListener;
        // 本地存储类型（分类存储）
        protected int mDiskCacheType;
        // Context
        protected Context mAppContext;

        //是否只是从本地已经存在的位置获取图片
        protected boolean mIsLoadLocalExistImage = false;

        // 通知ui更新
        private Handler mMainHandler = new Handler(Looper.getMainLooper());

        public CustomRunnableImp(Context appContext, ImageView iv,
                                 ImageType type, boolean isFullQuality, ImageListener imgLis,
                                 int diskCacheType, boolean isLoadLocalExistImage) {
            mImageView = new WeakReference<ImageView>(iv);
            if (iv != null && iv.getTag() != null
                    && (iv.getTag() instanceof String)) {
                mUrl = (String) iv.getTag();
            }
            mImageType = type;
            mIsFullQuality = isFullQuality;
            mImgListener = imgLis;
            mDiskCacheType = diskCacheType;
            mAppContext = appContext;
            mIsLoadLocalExistImage = isLoadLocalExistImage;
        }

        public CustomRunnableImp(Context appContext, String url,
                                 ImageType type, boolean isFullQuality, ImageListener imgLis,
                                 int diskCacheType, boolean isLoadLocalExistImage) {
            if (!TextUtils.isEmpty(url)) {
                mUrl = url;
            }
            mImageType = type;
            mIsFullQuality = isFullQuality;
            mImgListener = imgLis;
            mDiskCacheType = diskCacheType;
            mAppContext = appContext;
            mIsLoadLocalExistImage = isLoadLocalExistImage;
        }

        @Override
        public Object getIdentity() {
            if (mUrl != null) {
                return mUrl;
            } else {
                return super.getIdentity();
            }
        }

        @Override
        String getSubIdentity() {
            return this.toString();
        }

        boolean isViewValide() {
            //TODO KANGLE 当调用者未设置mImageView进来，而是选择callback的方式的话，这个方法的返回值会严重错误
            if (mImageView != null) {
                ImageView iv = mImageView.get();
                if (iv != null && (iv.getTag() instanceof String)
                        && mUrl.equals(iv.getTag())) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        @Override
        void onResult(final Resource<?> bt, final boolean isCached) {
            this.bitmapWR = new WeakReference<Resource<?>>(bt);
            if (!(bt == null || bt.getResource() == null)) {
                sImageLoadTracker.onTaskComplete(mUrl, true,
                        isCached ? ImageLoadTracker.LOADED_BY_QIYI_DISK : ImageLoadTracker.LOADED_BY_QIYI_NET);
            } else {
                sImageLoadTracker.onTaskComplete(mUrl, false, ImageLoadTracker.LOADED_BY_QIYI_NET);
            }

            if (mImageView == null && (mImgListener == null/* || mImgListener.get() == null*/)) {
                // 通过url请求图片
                DebugLog.v("DiskLoader", "DiskLoader run null with url: " + mUrl);
                return;
            }
            if (mImageView != null) {
                // 请求图片并设置ImageView
                ImageView iv = mImageView.get();
                if (iv == null || !(iv.getTag() instanceof String) || !mUrl.equals(iv.getTag())) {
                    DebugLog.v("DiskLoader", "DiskLoader run null with ImageView: " + mUrl);
                    return;
                }
            }

            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mImageView == null) {
                        if (bt != null && mImgListener != null) {
                            ImageListener cb = mImgListener/*.get()*/;
                            if (bt.getResource() instanceof Bitmap && !mImageType.equals(ImageType.GIF))
                                cb.onSuccessResponse((Bitmap) bt.getResource(), mUrl, isCached);
                        } else if (mImgListener != null) {
                            ImageListener cb = mImgListener/*.get()*/;
                            cb.onErrorResponse(-1);
                        }
                    } else {
                        ImageView iv = mImageView.get();

                        if (iv != null && (iv.getTag() instanceof String) && mUrl.equals(iv.getTag())) {
                            Object ob = null;
                            if (bt != null) {
                                ob = bt.getResource();
                            }
                            if (ob != null) {
                                if (ob instanceof Bitmap) {
                                    iv.setImageBitmap((Bitmap) ob);
                                    if (mImgListener != null) {
                                        ImageListener cb = mImgListener/*.get()*/;
                                        cb.onSuccessResponse((Bitmap) ob, mUrl, isCached);
                                    }
                                } else if (ob instanceof GifDrawable) {
                                    iv.setImageDrawable((GifDrawable) ob);
                                }
                            } else {
                                if (mImgListener != null) {
                                    ImageListener cb = mImgListener/*.get()*/;
                                    cb.onErrorResponse(-1);
                                } else {
                                    //如果请求失败一律用这个默认图片
//
                                }
                            }
                        }
                    }
                }
            });
        }

        @Override
        Resource getResult() {
            DebugLog.v("MarkManager", bitmapWR + " ");
            return bitmapWR == null ? null : bitmapWR.get();
        }

        protected boolean validateProcess() {
            if (mImageView != null) {
                ImageView iv = mImageView.get();
                if (iv == null) {
                    DebugLog.v("ImageDownloader", "mImageView has released: " + mUrl);
                    sImageLoadTracker.onTaskComplete(mUrl, false, ImageLoadTracker.LOADED_BY_QIYI_RETURN);
                    return false;
                }
            } else {
                if (mImgListener == null/* || mImgListener.get() == null*/) {
                    DebugLog.v("ImageDownloader", "Load picture with url, mCallback == null: " + mUrl);
                    sImageLoadTracker.onTaskComplete(mUrl, false, ImageLoadTracker.LOADED_BY_QIYI_RETURN);
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * @author dragon 网络加载图片
     */
    class ImageDownloader extends CustomRunnableImp {
        public ImageDownloader(Context appContext, ImageView iv, ImageType type,
                               boolean isFullQuality, ImageListener imgLis, int diskCacheType) {
            super(appContext, iv, type, isFullQuality, imgLis, diskCacheType, false);
        }

        public ImageDownloader(Context appContext, String url, ImageType type,
                               boolean isFullQuality, ImageListener imgLis, int diskCacheType) {
            super(appContext, url, type, isFullQuality, imgLis, diskCacheType, false);
        }

        @Override
        public void run() {
            if (TextUtils.isEmpty(mUrl)) {
                DebugLog.v("ImageDownloader", "processDownload mUrl null : " + mUrl);
                return;
            }

            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            processDownload();
        }

        /**
         * 下载图片处理
         */
        protected void processDownload() {
            if (!validateProcess()) {
                return;
            }

            if (mAppContext == null) {
                DebugLog.v("ImageDownloader", "ImageDownloader run context is null: " + mUrl);
                sImageLoadTracker.onTaskComplete(mUrl, false, ImageLoadTracker.LOADED_BY_QIYI_RETURN);
                return;
            }
            long time = 0;
            if (DebugLog.isDebug())
                time = System.currentTimeMillis();

            Resource<?> diskbt;
            // 判断磁盘图片是否存在
            if (mDiskCache.hasResource(mAppContext, mUrl, mDiskCacheType)) {
                DebugLog.v("ImageDownloader", "processDownload file has exits: " + mUrl);
                // 取出磁盘图片
                diskbt =
                        mDiskCache.getBitmapFromDisk(mAppContext, mUrl, mImageType, mIsFullQuality,
                                mDiskCacheType);
                if (DebugLog.isDebug()) {
                    sLoadImageFromDiskCount++;
                    DebugLog.v(TAG, "LoadImage from disk count: " + sLoadImageFromDiskCount);
                }
                onResult(diskbt, true);
                putResourceToMemory(mUrl, diskbt, mImageType);
            } else {
                getBitmapStream(mAppContext, mUrl, mImageType);
            }
        }

        /**
         * 获取图片数据,转换为resource
         * OkHttp 请求
         *
         * @param url
         * @return
         */
        private void getBitmapStream(final Context context, String url, final ImageType imageType) {
            if (TextUtils.isEmpty(url) || context == null || imageType == null) {
                DebugLog.w("ImageDownloader", "getBitmapStream para error: " + url);
                onResult(null, false);
                return;
            }

            Request<InputStream> request = new Request.Builder<InputStream>().url(url).build(InputStream.class);
            // 同步获取图片，避免CustomThreadPoolExecutor中afterExecute处理的时候图片没有下载完成
            Response<InputStream> inputStreamResponse = request.execute();
            if (inputStreamResponse == null) {
                onResult(null, false);
            } else {
                Resource resource = parseImage(inputStreamResponse.result, imageType, context);
                if (resource == null) {
                } else {
                    processResource(resource);
                }
            }
        }

        public void processResource(Resource<?> bt) {
            if (DebugLog.isDebug()) {
                sLoadImageFromNetCount++;
                DebugLog.v(TAG, "LoadImage from network count: " + sLoadImageFromNetCount);
            }
            if (bt != null) {
                // 保存到磁盘
                mMessageMonitor2.addRequest(mAppContext, mUrl, bt, mImageType, mDiskCacheType);
                if (mImageType == ImageType.CIRCLE && bt.getResource() instanceof Bitmap) {
                    Resource resource = new Resource();
                    resource.setResource(UIUtils.toRoundBitmap((Bitmap) bt.getResource()));
                    onResult(resource, false);
                    putResourceToMemory(mUrl, resource, mImageType);
                } else {
                    onResult(bt, false);
                    putResourceToMemory(mUrl, bt, mImageType);
                }
            } else {
                onResult(null, false);
                DebugLog.w("ImageDownloader", "processDownload download error: " + mUrl);
            }
        }
    }


    public Resource parseImage(InputStream is, ImageType imageType, Context context) {
        Resource resource = null;
        try {
            if (!imageType.equals(ImageType.GIF)) {
                byte[] bytes = toByteArray(is);
                Bitmap mBitmap = UIUtils.byteArray2ImgBitmap(context, bytes);
                if (null != mBitmap) {
                    resource = new Resource();
                    resource.setResource(mBitmap);
                }
            } else if (imageType.equals(ImageType.GIF)) {
                GifDrawable gifDrawable = new GifDrawableDecode(context).decode(is, 0, 0);
                if (gifDrawable != null) {
                    resource = new Resource();
                    resource.setResource(gifDrawable);
                }
            }
        } catch (Exception e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                    DebugLog.v("imageDownloader", " parseImage   输入流is关闭失败！");
                }
            }
        }
        return resource;
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

    /**
     * 请求网络图片，异步返回Bitmap对象，没有缓存处理
     *
     * @param context
     * @param url
     * @param isFullQuality
     * @param listener
     */
    public static void getBitmapRawData(final Context context, final String url, final boolean isFullQuality,
                                        final ImageListener listener) {
        if (null == context || TextUtils.isEmpty(url) || null == listener) {
            return;
        }

        Request.Builder<Bitmap> imgRequest = new Request.Builder<Bitmap>();
        imgRequest.url(url);
        imgRequest.disableAutoAddParams();
        imgRequest.parser(new IResponseConvert<Bitmap>() {
            @Override
            public Bitmap convert(byte[] bytes, String s) throws Exception {
                if (isFullQuality) {
                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                } else {
                    return UIUtils.byteArray2ImgBitmap(context.getApplicationContext(), bytes);
                }
            }

            @Override
            public boolean isSuccessData(Bitmap bitmap) {
                if (null != bitmap) {
                    return true;
                }
                return false;
            }
        });
        imgRequest.build(Bitmap.class).sendRequest(new IHttpCallback<Bitmap>() {
            @Override
            public void onResponse(Bitmap bitmap) {
                listener.onSuccessResponse(bitmap, url, false);
            }

            @Override
            public void onErrorResponse(HttpException e) {
                listener.onErrorResponse(-1);

            }
        });
    }

    /**
     * 获取图片数据
     *
     * @param context
     * @param url
     * @param isFullQuality 是否压缩图片质量
     * @param listener
     * @param needRequest   是否需要网络请求获取
     * @param needCache     是否使用缓存
     */
    public static void getBitmapRawData(final Context context, final String url, final boolean isFullQuality,
                                        final ImageListener listener, final boolean needRequest, final boolean needCache) {
        if (null == context || TextUtils.isEmpty(url) || null == listener) {
            return;
        }
        Resource<?> result = null;
        if (needCache) {
            result = getInstance().getBitmapFromMemory(url, ImageType.JPG);
            if (null != result) {
                listener.onSuccessResponse((Bitmap) result.getResource(), url, true);
                return;
            } else {
                // 从磁盘里读取
                getInstance().getBitmapFromDisk(context, url, ImageType.JPG,
                        isFullQuality, new ImageListener() {
                            @Override
                            public void onSuccessResponse(Bitmap bitmap, String url, boolean isCached) {
                                listener.onSuccessResponse(bitmap, url, isCached);
                            }

                            @Override
                            public void onErrorResponse(int errorCode) {
                                // 磁盘读取失败，是否从网络请求
                                if (needRequest) {
                                    getBitmapRawData(context, url, isFullQuality, new ImageListener() {
                                        @Override
                                        public void onSuccessResponse(Bitmap bitmap, String url, boolean isCached) {
                                            // 网络请求需要缓存
                                            putBitmapToCache(context, url, bitmap);
                                            listener.onSuccessResponse(bitmap, url, isCached);
                                        }

                                        @Override
                                        public void onErrorResponse(int errorCode) {
                                            listener.onErrorResponse(-1);
                                        }
                                    });
                                }
                            }
                        }, DiskCache.DISK_CACHE_TYPE_COMMON, true);
                return;
            }
        }
        if (needRequest) {
            getBitmapRawData(context, url, isFullQuality, new ImageListener() {
                @Override
                public void onSuccessResponse(Bitmap bitmap, String url, boolean isCached) {
                    if (needCache) {
                        putBitmapToCache(context, url, bitmap);
                    }
                    listener.onSuccessResponse(bitmap, url, false);
                }

                @Override
                public void onErrorResponse(int errorCode) {
                    listener.onErrorResponse(-1);
                }
            });
        }
        // No cache and not needRequest so just return!
        listener.onErrorResponse(-2);
    }

    /**
     * 添加图片到ImageLoader维护的disk，memory缓存中，缓存策略跟随ImageLoader,
     * 可以通过getBitmapRawData获取到缓存中的内容
     *
     * @param context
     * @param url
     * @param bitmap
     */
    public static void putBitmapToCache(Context context, String url, Bitmap bitmap) {
        if (null == context || TextUtils.isEmpty(url) || null == bitmap) {
            return;
        }
        Resource<Bitmap> res = new Resource<Bitmap>();
        res.setResource(bitmap);
        getInstance().mMessageMonitor2.addRequest(context, url, res, ImageType.JPG, DiskCache.DISK_CACHE_TYPE_COMMON);
        getInstance().putResourceToMemory(url, res, ImageType.JPG);
    }

    /**
     * @author dragon 监听消息使用
     */
    private class MessageMonitor implements Runnable {
        private static final int MSG_QUEUE_SIZE = 10;
        private static final int MSG_QUEUE_SIZE2 = 10;
        // 缓存网络下载任务
        private LinkedBlockingDeque<Runnable> mMsgQueue = new LinkedBlockingDeque<Runnable>(
                MSG_QUEUE_SIZE + 1);
        private LinkedBlockingDeque<Runnable> mMsgQueue2 = new LinkedBlockingDeque<Runnable>(
                MSG_QUEUE_SIZE2 + 1);
        // 等待锁
        private Object mLockForWait = new Object();
        /**
         * true停止
         */
        private Boolean mStop = false;
        // 暂停从消息队列取消息
        private Boolean mPause = false;

        /**
         * 设置是否暂停消息处理
         *
         * @param flag
         */
        private void setPause(Boolean flag) {
//        	flag = false;
            if (mPause.equals(flag)) {

                DebugLog.v("MessageMonitor", "setPause return flag:" + flag + "  mPause:"
                        + mPause);

                return;
            }
            DebugLog.v("MessageMonitor", "setPause flag:" + flag + "  mPause:" + mPause);

            mPause = flag;
            if (!mPause) {
                cancelWait();
            }
        }

        /**
         * 停止任务
         */
        /*
         * public void stop() { mStop = true; }
         */

        /**
         * 添加运行任务
         *
         * @param r
         */
        public void addRunnable(Runnable r) {
            if (r != null) {
                try {
                    while (mMsgQueue.size() >= MSG_QUEUE_SIZE) {
                        Runnable rr = mMsgQueue.removeFirst();
                        if (rr != null) {
                            DebugLog.v("MessageMonitor",
                                    "remove runnable "
                                            + ((ImageDownloader) rr).mUrl);
                            while (mMsgQueue2.size() >= MSG_QUEUE_SIZE2) {
                                Runnable rr2 = mMsgQueue2.removeLast();
                                DebugLog.v("MessageMonitor", "remove runnable2 "
                                        + ((ImageDownloader) rr2).mUrl);
                            }
                            mMsgQueue2.offerFirst(rr);
                        }
                    }
                    mMsgQueue.addLast(r);
                    DebugLog.v("MessageMonitor", "Current size: "
                            + mMsgQueue.size() + " add runnable "
                            + ((ImageDownloader) r).mUrl);
                } catch (NoSuchElementException nsee) {
                    DebugLog.w("MessageMonitor", "addRunnable nsee:" + nsee);
                } catch (IllegalStateException ise) {
                    DebugLog.w("MessageMonitor", "addRunnable ise:" + ise);
                } catch (NullPointerException npe) {
                    DebugLog.w("MessageMonitor", "addRunnable npe:" + npe);
                }
            }
        }

        /**
         * 请求等待
         */
        public void requestWait() {
            synchronized (mLockForWait) {
                try {
                    mLockForWait.wait();
                } catch (Exception e) {
                    DebugLog.w("MessageMonitor", "requestWait e:" + e);
                }
            }
        }

        /**
         * 取消等待
         */
        public void cancelWait() {
            synchronized (mLockForWait) {
                try {
                    mLockForWait.notifyAll();
                } catch (Exception e) {
                    DebugLog.w("MessageMonitor", "cancelWait e:" + e);
                }
            }
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            Runnable runnable = null;
            while (!mStop) {
                runnable = null;

                // 暂停取消息
                if (mPause) {
                    DebugLog.v("MessageMonitor", "run wait pause cancel");
                    requestWait();
                    continue;
                }

                if (EXECUTOR_FOR_NETWORK.getQueue().remainingCapacity() < 1) {
                    try {
                        DebugLog.v("MessageMonitor", "run sleep 40ms");
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        DebugLog.w("MessageMonitor", "run sleep :" + e);
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                try {
                    int queue1size1 = mMsgQueue.size();
                    int queue2size2 = mMsgQueue2.size();
                    if (queue1size1 > 0) {
                        runnable = mMsgQueue.takeFirst();
                        if (!((ImageDownloader) runnable).isViewValide()) {
                            while (mMsgQueue2.size() >= MSG_QUEUE_SIZE2) {
                                sImageLoadTracker.onTaskComplete(((ImageDownloader) runnable).mUrl,
                                        false, ImageLoadTracker.LOADED_BY_QIYI_RETURN);
                                mMsgQueue2.removeLast();
                            }
                            mMsgQueue2.offerFirst(runnable);
                            runnable = null;
                        }
                    } else if (queue2size2 > 0) {
                        runnable = mMsgQueue2.takeFirst();
                    } else {
                        runnable = mMsgQueue.takeFirst();
                        if (!((ImageDownloader) runnable).isViewValide()) {
                            while (mMsgQueue2.size() >= MSG_QUEUE_SIZE2) {
                                sImageLoadTracker.onTaskComplete(((ImageDownloader) runnable).mUrl,
                                        false, ImageLoadTracker.LOADED_BY_QIYI_RETURN);
                                mMsgQueue2.removeLast();
                            }
                            mMsgQueue2.offerFirst(runnable);
                            runnable = null;
                        }
                    }
                    //Log.v("MessageMonitor", "mMsgQueue1.size:" + mMsgQueue.size());
                    //Log.v("MessageMonitor", "mMsgQueue2.size:" + mMsgQueue2.size());
                } catch (InterruptedException e) {
                    DebugLog.w("MessageMonitor", "run e:" + e.getMessage());
                    Thread.currentThread().interrupt();
                } catch (IllegalStateException e) {
                    DebugLog.w("MessageMonitor", "run e:" + e.getMessage());
                } catch (Exception e) {
                    DebugLog.w("MessageMonitor", "run e:" + e.getMessage());
                }

                // 网络下载图片
                if (runnable != null) {
                    EXECUTOR_FOR_NETWORK.execute(runnable);
                }
            }
        }
    }

    private class BitmapToDiskMonitor implements Runnable {

        class BitmapInfo {
            private Context mContext;
            private String mUrl;
            private Resource<?> mResource;
            private ImageType mType;
            private int mDiskCacheType;

            public BitmapInfo(Context context, String url, Resource<?> resource,
                              ImageType type, int diskCacheType) {
                mContext = context;
                mUrl = url;
                mResource = resource;
                mType = type;
                mDiskCacheType = diskCacheType;
            }
        }

        private static final int MSG_QUEUE_SIZE = 20;
        // 图片保存请求队列
        private LinkedBlockingDeque<BitmapInfo> mMsgQueue = new LinkedBlockingDeque<BitmapInfo>(
                MSG_QUEUE_SIZE);
        /**
         * true停止
         */
        private Boolean mStop = false;

        /**
         * 添加运行任务
         */
        public void addRequest(Context context, String url, Resource<?> resource,
                               ImageType type, int diskCacheType) {
            if (url != null && resource != null) {
                try {
                    BitmapInfo info = new BitmapInfo(context, url, resource,
                            type, diskCacheType);
                    while (mMsgQueue.size() >= MSG_QUEUE_SIZE) {
                        mMsgQueue.removeFirst();
                    }
                    mMsgQueue.addLast(info);
                    DebugLog.v("BitmapToDiskMonitor", "Current size: "
                            + mMsgQueue.size() + " add runnable " + url);
                } catch (NoSuchElementException nsee) {
                    DebugLog.w("BitmapToDiskMonitor",
                            "addRunnable nsee:" + nsee);
                } catch (IllegalStateException ise) {
                    DebugLog.w("BitmapToDiskMonitor", "addRunnable ise:"
                            + ise);
                } catch (NullPointerException npe) {
                    DebugLog.w("BitmapToDiskMonitor", "addRunnable npe:"
                            + npe);
                }
            }
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            BitmapInfo runnable = null;
            while (!mStop) {
                runnable = null;
                try {
                    runnable = mMsgQueue.takeFirst();
                } catch (InterruptedException e) {
                    DebugLog.w("BitmapToDiskMonitor", "run e:" + e.getMessage());
                    Thread.currentThread().interrupt();
                }

                // 网络下载图片
                if (runnable != null) {
                    mDiskCache.putBitmapToDisk(runnable.mContext,
                            runnable.mUrl, runnable.mResource, runnable.mType,
                            runnable.mDiskCacheType);
                }
            }
        }
    }
}
