package org.qiyi.basecore.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.constant.BaseCoreSPConstants;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/***
 *  zhangqi SPBigString 操作工具类
 *
 * !!!!!!! 此类主要用于保存SP中的较大文件的类，使用的时候需要注意两点：
 *
 *  一  不要再程序退出前执行大文件的写入操作。
 *  二 不要写入过大的文件( 单个文件超过500K ),如果有大文件写入需求请直接使用fileUtil类进行操作。
 *
 */
public class SPBigStringFileFactory {
    private static final String TAG = SPBigStringFileFactory.class.getSimpleName();
    private static final int MEMORY_CACHE_SIZE = 500; //默认 500K
    private static final int THREAD_POOL_MIN_SIZE = 0; //
    private static final int THREAD_POOL_MAX_SIZE = 2; //最多2线程并发执行
    private static final int VERSION = 1;//存储版本
    private static final String ENCODING = "utf-8";//默认字符集
    private static final Long DELAY_TIME = 2 * 60 * 1000L;//如果没有迁移，默认开机2分钟后执行迁移操作。
    private static Map<String, MoveModule> sMapList = new HashMap<String, MoveModule>();
    private  Context sContext;
    private static Handler sUIHandler = new Handler(Looper.getMainLooper());
    private static ConcurrentMap<String, ReentrantReadWriteLock> sCurrentLocks = new ConcurrentHashMap<String, ReentrantReadWriteLock>();
    private FileThreadPoolExecutor mFileThreadPool = null;
    private LruCache<String, String> mMemoryCache;

    /** --------------------------需要迁移的SP名以及KEY -------------------------*/
    /** 插件中心 START */
    private static final String SP_NAME_FOR_PLUGIN_JSON = "SP_KEY_FOR_PLUGIN_JSON";
    private static final String SP_KEY_FOR_PLUGIN_JSON = "SP_KEY_FOR_PLUGIN_JSON";
    /** 插件中心 END */

    static {
        initConfig();
    }

    private SPBigStringFileFactory(Context mContext) {
        this.sContext = mContext;
        if (mFileThreadPool == null) {
            // LinkedBlockingQueue  runable按队列添加先后顺序执行
            mFileThreadPool = new FileThreadPoolExecutor(THREAD_POOL_MIN_SIZE, THREAD_POOL_MAX_SIZE, 1000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        }
        if (mMemoryCache == null) {
            mMemoryCache = new LruCache<String, String>(MEMORY_CACHE_SIZE * 1024) {
                @Override
                protected int sizeOf(String key, String value) {
                    if (value != null) {
                        return value.length();
                    } else {
                        return 0;
                    }
                }
            };
        }
    }

  /*  private static class SPBigStringFileFactoryHolder {
        private final static SPBigStringFileFactory instance = new SPBigStringFileFactory(sContext);
    }*/

    //在用到的时候再去初始化  classLoader 去加载SPBigStringFileFactoryHolder 类 并且是单例模式实现
   /* public static SPBigStringFileFactory getInstance(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("SPBigStringFileFactory  getInstance context is  null ");
        }
        sContext = context.getApplicationContext();
        return SPBigStringFileFactoryHolder.instance;
    }*/


    private volatile static SPBigStringFileFactory mInstance;

    public static SPBigStringFileFactory getInstance(Context mContext) {

        if (mInstance == null) {
            synchronized (SPBigStringFileFactory.class) {
                if (mInstance == null) {
                    mInstance = new SPBigStringFileFactory(mContext);
                }
            }
        }

        return mInstance;
    }



    /**
     * 查询对应的 key 有没有存储
     */
    public boolean hasKeySync(String key) {
        ReentrantReadWriteLock lock = null;
        try {
            lock = createOrGetLock(key);
            lock.readLock().lock();
            File file = getSPFile(key, sContext, false);
            if (!file.exists()) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            DebugLog.d(TAG, "hasKeySync Exception   " + e.getMessage());
            return false;
        } finally {
            if (lock != null) {
                lock.readLock().unlock();
            }
            tryToRemoveLock(key);
        }
    }

    /**
     * 当前线程 同步存储String 到 file文件  如果之前有存储 直接覆盖掉
     *
     * @param key
     * @param value
     */

    public boolean addKeySync(String key, String value) {
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
            return false;
        }
        ReentrantReadWriteLock lock = null;
        try {
            lock = createOrGetLock(key);
            lock.writeLock().lock();
            File file = getSPFile(key, sContext, true);
            boolean result = FileUtils.string2File(value, file.getPath());
            if (result) {
                mMemoryCache.put(key, value);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            DebugLog.d(TAG, "addKeySync Exception   " + e.getMessage());
            return false;
        } finally {
            if (lock != null) {
                lock.writeLock().unlock();
            }
            tryToRemoveLock(key);
        }
    }

    /**
     * 当前线程 异步同步存储String 到 file文件  如果之前有存储 直接覆盖掉
     *
     * @param key
     * @param value
     */

    public void addKeyAsync(String key, String value) {
        addKeyAsync(key, value, true, null);
    }

    /***
     * 开启线程 异步存储String 到 file文件  如果之前有存储  直接覆盖掉
     * 如果当前调用线程存在looper , 则在当前调用线程执行回调，如果强制要求在UI线程调用,则在UI线程执行回调、如果都没有则在线程池中的线程中执行回调。
     *
     * @param key
     * @param value
     * @param callBackOnUIThread 是否在UI线程调用
     * @param listener           添加回调监听  这里可以为空就是没有回调
     */

    public void addKeyAsync(final String key, final String value, final boolean callBackOnUIThread, @Nullable final ISPStringFileListener listener) {
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
            if (listener != null) {
                addOrRemoveKeyCallback(key, listener, callBackOnUIThread, false, true);
            } else {
                return;
            }
        }

        mFileThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                ReentrantReadWriteLock lock = null;
                try {
                    lock = createOrGetLock(key);
                    lock.writeLock().lock();

                    File file = getSPFile(key, sContext, true);
                    boolean result = FileUtils.string2File(value, file.getPath());
                    if (result) {
                        mMemoryCache.put(key, value);
                    }
                    if (listener != null) {
                        addOrRemoveKeyCallback(key, listener, callBackOnUIThread, result, true);
                    }
                } catch (Exception e) {
                    if (listener != null) {
                        addOrRemoveKeyCallback(key, listener, callBackOnUIThread, false, true);
                    }
                    DebugLog.d(TAG, "addKeyAsync Exception   " + e.getMessage());
                } finally {
                    if (lock != null) {
                        lock.writeLock().unlock();
                    }
                    tryToRemoveLock(key);
                }
            }
        });
    }

    /***
     * （注意!! 此方法是，是专门获取从Sp迁移到file的BigString的方法，)
     *
     * 同步获取  已经从SP迁移到file中的,或者还在old SP中的  的key值的兼容方法
     *
     * @param key
     * @param defaultValue
     * @param oldSharedPreferencesName
     * @return
     */
    public String getKeyMergeFromSPSync(String key, String defaultValue, @NonNull String oldSharedPreferencesName) {
        if (TextUtils.isEmpty(key)) {
            return defaultValue;
        }

        if (sMapList.containsKey(key)) {
            boolean hasMove = SharedPreferencesFactory.get(sContext, BaseCoreSPConstants.MOVE_FLAG, false);
            if (hasMove) {
                return getKeyFileSyncDetail(key, defaultValue);
            } else {
                //如果没有迁移完成先读取文件存储，因为有可能这个值在新版本被直接写在文件中去 ，这里优先以文件为第一参考
                String resultStr = getKeyFileSyncDetail(key, defaultValue);
                if (TextUtils.isEmpty(resultStr) || resultStr.equals(defaultValue)) { //如果没有取到值 再去SP 中取一下
                    return SharedPreferencesFactory.get(sContext, key, defaultValue, oldSharedPreferencesName);
                } else {
                    return resultStr;
                }
            }
        } else {
            return getKeyFileSyncDetail(key, defaultValue);
        }
    }

    /**
     * 当前线程同步获取 SPFile中 存储 key 的值 ，如果是从SP迁移过来的建议使用getKeySync(String key, String defaultValue, @NonNull String oldSharedPreferencesName)
     *
     * @param key
     * @return if result is empty return   defaultValue
     */

    public String getKeySync(String key, String defaultValue) {
        if (TextUtils.isEmpty(key)) {
            return defaultValue;
        }
        String result = defaultValue;
        if (sMapList.containsKey(key)) {
            return getKeyMergeFromSPSync(key, defaultValue, SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
        } else {
            result = getKeyFileSyncDetail(key, defaultValue);
        }

        return TextUtils.isEmpty(result) ? defaultValue : result;
    }


    /**
     * 当前线程同步获取 SPFile中 存储 key 的值
     *
     * @param key
     * @return if result is empty return   defaultValue
     */

    private String getKeyFileSyncDetail(String key, String defaultValue) {
        if (TextUtils.isEmpty(key)) {
            return defaultValue;
        }
        String result = defaultValue;
        ReentrantReadWriteLock lock = null;
        try {
            lock = createOrGetLock(key);
            lock.readLock().lock();
            result = getFileFromMemCache(key);
            if (!TextUtils.isEmpty(result)) {
                return result;
            } else {
                File file = getSPFile(key, sContext, false);
                if (!file.exists()) {
                    return defaultValue;
                }
                result = FileUtils.file2String(file, ENCODING);
                if (!TextUtils.isEmpty(result)) {
                    addFileToMemoryCache(key, result);
                }
            }
        } catch (Exception e) {
            DebugLog.d(TAG, "getKeySync Exception   " + e.getMessage());
            return defaultValue;
        } finally {
            if (lock != null) {
                lock.readLock().unlock();
            }
            tryToRemoveLock(key);
        }
        return TextUtils.isEmpty(result) ? defaultValue : result;
    }

    /**
     * 异步回调  开启线程 获取file 中的值
     * 如果当前调用线程存在looper , 则在当前调用线程执行回调，如果强制要求在UI线程调用,则在UI线程执行回调、如果都没有则在线程池中的线程中执行回调。
     *
     * @param key
     * @param defaultValue
     * @param callBackOnUIThread
     * @param listener
     */

    private void getKeyAsyncWithCallBackDetail(final String key, final String defaultValue, final boolean callBackOnUIThread, @NonNull final ISPStringFileListener listener) {
        if (TextUtils.isEmpty(key) && listener != null) {
            doLoaderCallback(key, listener, callBackOnUIThread, defaultValue);
            return;
        } else if (listener == null) {
            return;
        }

        String result = getFileFromMemCache(key);
        if (!TextUtils.isEmpty(result)) {
            doLoaderCallback(key, listener, callBackOnUIThread, result);
        } else {
            mFileThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    ReentrantReadWriteLock lock = null;
                    try {
                        lock = createOrGetLock(key);
                        lock.readLock().lock();
                        File file = getSPFile(key, sContext, false);
                        if (!file.exists()) {
                            doLoaderCallback(key, listener, callBackOnUIThread, defaultValue);
                            return;
                        }
                        String value = FileUtils.file2String(file, ENCODING);
                        if (!TextUtils.isEmpty(value)) {
                            addFileToMemoryCache(key, value);
                        }
                        doLoaderCallback(key, listener, callBackOnUIThread, TextUtils.isEmpty(value) ? defaultValue : value);
                    } catch (Exception e) {
                        DebugLog.d(TAG, "getKeyAsyncWithCallBack   Exception   " + e.getMessage());
                        doLoaderCallback(key, listener, callBackOnUIThread, defaultValue);
                    } finally {
                        if (lock != null) {
                            lock.readLock().unlock();
                        }
                        tryToRemoveLock(key);
                    }
                }
            });
        }
    }

    /***
     * {  如果是从SP迁移过来的,请使用下面的 getKeyAsyncWithCallBack(... 含有 oldSharedPreferencesName) ,不然获取不到值!!!!! }
     * <p>
     * 异步回调  开启线程 获取file 中的值
     * 如果当前调用线程存在looper , 则在当前调用线程执行回调，如果强制要求在UI线程调用,则在UI线程执行回调、如果都没有则在线程池中的线程中执行回调。
     *
     * @param key
     * @param defaultValue
     * @param callBackOnUIThread
     * @param listener
     */

    public void getKeyAsyncWithCallBack(final String key, final String defaultValue, final boolean callBackOnUIThread, @NonNull final ISPStringFileListener listener) {
        if (TextUtils.isEmpty(key) && listener != null) {
            doLoaderCallback(key, listener, callBackOnUIThread, defaultValue);
            return;
        } else if (listener == null) {
            return;
        }
        if (sMapList.containsKey(key)) {//防止不规范使用这个方法,去获取从deafault中迁移过来的SP中的key,获取不到value情况
            getKeyMergeFromSPAsyncWithCallBack(key, defaultValue, SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME, callBackOnUIThread, listener);
            return;
        }
        getKeyAsyncWithCallBackDetail(key, defaultValue, callBackOnUIThread, listener);
    }

    /**
     * （注意!! 此方法是，是专门迁移相关的方法，不涉及迁移的请直接使用  不带oldSharedPreferencesName参数的 getKeyAsyncWithCallBack（））。
     * 不在白名单中的key 使用这个获取不到SP中的值.
     * <p>
     * 异步获取  已经从SP迁移到file中的,或者还在old SP中的  的key值的兼容方法
     * 如果当前调用线程存在looper , 则在当前调用线程执行回调，如果强制要求在UI线程调用,则在UI线程执行回调、如果都没有则在线程池中的线程中执行回调。
     *
     * @param key
     * @param defaultValue
     * @param oldSharedPreferencesName
     * @param callBackOnUIThread
     * @param listener
     */
    public void getKeyMergeFromSPAsyncWithCallBack(final String key, final String defaultValue, @NonNull String oldSharedPreferencesName, final boolean callBackOnUIThread, @NonNull final ISPStringFileListener listener) {
        if (TextUtils.isEmpty(key) && listener != null) {
            doLoaderCallback(key, listener, callBackOnUIThread, defaultValue);
            return;
        } else if (listener == null) {
            return;
        }

        if (sMapList.containsKey(key)) {
            boolean hasMove = SharedPreferencesFactory.get(sContext, BaseCoreSPConstants.MOVE_FLAG, false);
            if (hasMove) {
                getKeyAsyncWithCallBackDetail(key, defaultValue, callBackOnUIThread, listener);
            } else {
                //如果没有迁移完成先读取文件存储，因为有可能这个值在新版本被直接写在文件中去 ，这里优先以文件为第一参考
                if (hasKeySync(key)) {
                    getKeyAsyncWithCallBackDetail(key, defaultValue, callBackOnUIThread, listener); //异步获取数据
                } else {
                    String result = SharedPreferencesFactory.get(sContext, key, defaultValue, oldSharedPreferencesName);
                    doLoaderCallback(key, listener, callBackOnUIThread, result);
                }
            }
        } else {
            getKeyAsyncWithCallBackDetail(key, defaultValue, callBackOnUIThread, listener);
        }
    }


    /**
     * 获取某一个key的值的   回掉函数、如果调用线程存在looper , 则返回调用线程，如果强制要求在UI线程调用,则在UI线程回调、如果都没有则在线程池中的线程调用。
     *
     * @param key
     * @param listener
     * @param callBackOnUIThread
     * @param value
     */
    private void doLoaderCallback(final String key, final ISPStringFileListener listener, final boolean callBackOnUIThread, final String value) {
        if (Looper.myLooper() != null && !callBackOnUIThread) {
            Handler sHandler = new Handler(Looper.myLooper());
            sHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onLoaderKey(key, value);
                }
            });
        } else {
            sUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onLoaderKey(key, value);
                }
            });
        }
    }

    /**
     * 开启线程 异步删除 key 不需要回调函数
     *
     * @param key
     */
    public void removeKeyAsync(final String key) {
        if (TextUtils.isEmpty(key)) {
            return;
        }

        mFileThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                ReentrantReadWriteLock lock = null;
                try {
                    lock = createOrGetLock(key);
                    lock.writeLock().lock();
                    File file = getSPFile(key, sContext, false);
                    if (!file.exists()) {
                        return;
                    }
                    boolean result = FileUtils.deleteFile(file);
                    if (result) {
                        removeFromMemoryCache(key);
                    }
                } catch (Exception e) {
                    DebugLog.d(TAG, "removeKeyAsync Exception   " + e.getMessage());
                } finally {
                    if (lock != null) {
                        lock.writeLock().unlock();
                    }
                    tryToRemoveLock(key);
                }
            }
        });
    }

    /**
     * 当前线程 同步删除 key
     *
     * @param key
     */

    public boolean removeKeySync(String key) {
        if (TextUtils.isEmpty(key)) {
            return false;
        }
        ReentrantReadWriteLock lock = null;
        try {
            lock = createOrGetLock(key);
            lock.writeLock().lock();

            File file = getSPFile(key, sContext, false);
            if (!file.exists()) {
                return true;
            }
            boolean result = FileUtils.deleteFile(file);
            if (result) {
                removeFromMemoryCache(key);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            DebugLog.d(TAG, "removeKeySync Exception   " + e.getMessage());
            return false;
        } finally {
            if (lock != null) {
                lock.writeLock().unlock();
            }
            tryToRemoveLock(key);
        }

    }

    /**
     * 异步删除 key 并且有回调
     *
     * @param key
     */
    public void removeKeyAsyncWithCallBack(final String key, final boolean callBackOnUIThread, @NonNull final ISPStringFileListener listener) {
        if (TextUtils.isEmpty(key) && listener != null) {
            addOrRemoveKeyCallback(key, listener, callBackOnUIThread, false, false);
            return;
        }
        if (key == null || listener == null) {
            return;
        }
        mFileThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                ReentrantReadWriteLock lock = null;
                try {
                    lock = createOrGetLock(key);
                    lock.writeLock().lock();
                    File file = getSPFile(key, sContext, false);
                    if (!file.exists()) {
                        addOrRemoveKeyCallback(key, listener, callBackOnUIThread, true, false);
                        return;
                    }
                    boolean result = FileUtils.deleteFile(file);
                    if (result) {
                        removeFromMemoryCache(key);
                    }
                    addOrRemoveKeyCallback(key, listener, callBackOnUIThread, result, false);
                } catch (Exception e) {
                    addOrRemoveKeyCallback(key, listener, callBackOnUIThread, false, false);
                    DebugLog.d(TAG, "removeKeyAsyncWithCallBack Exception   " + e.getMessage());
                } finally {
                    if (lock != null) {
                        lock.writeLock().unlock();
                    }
                    tryToRemoveLock(key);
                }
            }
        });
    }


    /**
     * 添加或者删除 某一个key的回掉函数、如果调用线程存在looper , 则返回调用线程，如果强制要求在UI线程调用,则在UI线程回调、如果都没有则在线程池中的线程调用。
     *
     * @param key
     * @param listener
     * @param callBackOnUIThread
     * @param value
     * @param isAdd              添加删除区别
     */

    public void addOrRemoveKeyCallback(final String key, final ISPStringFileListener listener, final boolean callBackOnUIThread, final boolean value, final boolean isAdd) {
        if (Looper.myLooper() != null && !callBackOnUIThread) {
            Handler sHandler = new Handler(Looper.myLooper());
            sHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (isAdd) {
                        listener.onAddKey(key, value);
                    } else {
                        listener.onRemoveKey(key, value);
                    }
                }
            });
        } else {
            sUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (isAdd) {
                        listener.onAddKey(key, value);
                    } else {
                        listener.onRemoveKey(key, value);
                    }
                }
            });
        }
    }


    /**
     * @param key
     * @param mContext
     * @param ismkdirs 如果文件不存在是否 创建 文件
     * @return //返回/data/data/PackageName/files
     */

    private File getSPFile(String key, Context mContext, boolean ismkdirs) {
        File ExternalFile = mContext.getFilesDir();
        File keyFilePath = new File(ExternalFile.getAbsolutePath() + "/" + VERSION + "/");

        File keyFile = new File(keyFilePath, key);
        if (!keyFile.getParentFile().exists() && ismkdirs) {
            keyFile.getParentFile().mkdirs();
        }
        return keyFile;
    }


    private void addFileToMemoryCache(String key, String value) {
        if (!TextUtils.isEmpty(value)) {
            mMemoryCache.put(key, value);
        }
    }

    private void removeFromMemoryCache(String key) {
        if (!TextUtils.isEmpty(key)) {
            mMemoryCache.remove(key);
        }
    }

    private String getFileFromMemCache(String key) {
        return mMemoryCache.get(key);
    }


    /**
     * key 状态的监听
     */
    public interface ISPStringFileListener {
        void onLoaderKey(String key, String value);

        void onRemoveKey(String key, Boolean isOk);

        void onAddKey(String key, Boolean isOk);
    }

    /**
     * Get or create ReentrantReadWriteLock for given key
     *
     * @param key 基于key 的读写锁
     * @return lock   under this key
     */
    private static ReentrantReadWriteLock createOrGetLock(String key) {
        synchronized (sCurrentLocks) {
            if (sCurrentLocks.containsKey(key)) {
                return sCurrentLocks.get(key);
            } else {
                if (!sCurrentLocks.containsKey(key)) {
                    sCurrentLocks.put(key, new ReentrantReadWriteLock());
                }
                return sCurrentLocks.get(key);
            }
        }
    }

    /**
     * Try to release ReentrantReadWriteLock by judge lock's hold count
     *
     * @param key for this lock
     */
    private static void tryToRemoveLock(String key) {
        synchronized (sCurrentLocks) {
            if (sCurrentLocks.containsKey(key)) {
                ReentrantReadWriteLock lock = sCurrentLocks.get(key);
                if (!lock.hasQueuedThreads() && lock.getReadHoldCount() == 0 && lock.getWriteHoldCount() == 0) {
                    sCurrentLocks.remove(key);
                }
            }
        }

    }
    /**
     * 此方法用于保证程序退出前,阻塞 app  kill progres 关闭，直到所有需要保存的file task都执行完毕，
     */
    public void syncFileToData() {
        mFileThreadPool.doWaitFinishTask();
    }

    /**
     * 初始化需要迁移的key的白名单列表
     */

    static void initConfig() {
        sMapList.put("DFP_DEV_ENV_INFO", new MoveModule("DFP_DEV_ENV_INFO", SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME));
        sMapList.put(SharedPreferencesConstants.BULLET_CH_DEFAULT,
                new MoveModule(SharedPreferencesConstants.BULLET_CH_DEFAULT, SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME));
        sMapList.put(SharedPreferencesConstants.ANGLE_ICONS2_IN_INIT_APP,
                new MoveModule(SharedPreferencesConstants.ANGLE_ICONS2_IN_INIT_APP, SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME));
        /* 插件中心迁移SP START */
        sMapList.put(SP_KEY_FOR_PLUGIN_JSON, new MoveModule(SP_KEY_FOR_PLUGIN_JSON, SP_NAME_FOR_PLUGIN_JSON));
        /* 插件中心迁移SP END */
        sMapList.put(SharedPreferencesConstants.KEY_OPERATOR_JSON,
                new MoveModule(SharedPreferencesConstants.KEY_OPERATOR_JSON, SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME));
        /*意见反馈*/
        sMapList.put(SharedPreferencesConstants.SP_FEEDBACK_DATA,
                new MoveModule(SharedPreferencesConstants.SP_FEEDBACK_DATA, SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME));
    }

    /**
     * 此方法用于批量迁移SP中指定的key到file文件中去
     */
    public void doBatchMove() {
        sUIHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean moveFlag = SharedPreferencesFactory.get(sContext, BaseCoreSPConstants.MOVE_FLAG, false);//获取是否需要迁移标致
                if (!moveFlag) {
                    mFileThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            boolean hasAllMove = true;
                            for (String key : sMapList.keySet()) {
                                MoveModule moveModule = sMapList.get(key);
                                boolean result = moveStringKeyToFileFactory(moveModule.getmKey(), moveModule.getmOldSPName());
                                if (!result) {
                                    moveModule.setmMoveStatus(false);
                                    hasAllMove = false;  // 如果有一条数据没有迁移完成, 整体的迁移标致 MOVE_FLAG = false 先迁移部分数据
                                } else {
                                    moveModule.setmMoveStatus(true);
                                }
                            }
                            for (String key : sMapList.keySet()) {
                                MoveModule moveModule = sMapList.get(key);
                                if(moveModule.ismMoveStatus()){
                                    SharedPreferencesFactory.remove(sContext, moveModule.getmKey(), moveModule.getmOldSPName());
                                }
                            }
                            if (hasAllMove) {
                                SharedPreferencesFactory.set(sContext, BaseCoreSPConstants.MOVE_FLAG, true, true);//修改迁移标致
                            }
                        }

                    });
                }
            }
        }, DELAY_TIME);
    }

    /**
     * 迁移  default_sharePreference 中指定key 对应的String 到SPBigFileFactory管理的文件中去
     */
    public boolean moveStringKeyFromDefaultToFileFactory(String key) {
        return moveStringKeyToFileFactory(key, SharedPreferencesConstants.DEFAULT_SHAREPREFERENCE_NAME);
    }

    /**
     * 迁移指定sharedPreferencesName 中的 key 对应的String 到SPBigFileFactory管理的文件中去
     *
     * @param key
     * @param sharedPreferencesName
     * @return
     */
    public boolean moveStringKeyToFileFactory(String key, String sharedPreferencesName) {
        boolean isExist = SharedPreferencesFactory.hasKey(sContext, key, sharedPreferencesName); //检测原来的SP是否含有key 是否需要迁移
        boolean isFileHasNewData = hasKeySync(key);  //新的文件中没有新数据的key时候, 才会从老的SP中迁移过来，如果有的话就不迁移了,后续直接删除SP的key对应的老值
        if (isExist && !isFileHasNewData) {  //如果没有直接返回true
            String str = SharedPreferencesFactory.get(sContext, key, "", sharedPreferencesName);
            if (!TextUtils.isEmpty(str)) {      //原来的Str 存在并且不为空
                if (addKeySync(key, str)) {  // 将老的Str 添加到新的file文件系统中去
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    static class MoveModule {
        private String mKey;
        private String mOldSPName;
        private boolean mMoveStatus = false;

        public MoveModule(String key, String oldSPName) {
            this.mKey = key;
            this.mOldSPName = oldSPName;
        }

        public String getmKey() {
            return mKey;
        }

        public void setmKey(String mKey) {
            this.mKey = mKey;
        }

        public String getmOldSPName() {
            return mOldSPName;
        }

        public void setmOldSPName(String mOldSPName) {
            this.mOldSPName = mOldSPName;
        }

        public boolean ismMoveStatus() {
            return mMoveStatus;
        }

        public void setmMoveStatus(boolean mMoveStatus) {
            this.mMoveStatus = mMoveStatus;
        }
    }
}
