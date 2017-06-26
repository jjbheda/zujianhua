package org.qiyi.basecore.utils;

import android.content.Context;
import android.text.TextUtils;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.jobquequ.AsyncJob;
import org.qiyi.basecore.jobquequ.JobManagerUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by niejunjiang on 2017/4/5.
 */

public class ConsistencyDataUtils {
    public final static String TAG = "ConsistencyDataUtils";
    private static ConcurrentMap<String, ReentrantReadWriteLock> sCurrentLocks
            = new ConcurrentHashMap<String, ReentrantReadWriteLock>();

    /**
     * 用来设置跨进程数据
     */
    public static void setValue(final Context context, final String _key, final String value) {
        JobManagerUtils.postRunnable(new Runnable() {
            @Override
            public void run() {
                ConsistencyDataOperator mOprerator = ConsistencyDataOperator.instance;
                if (mOprerator == null) {
                    mOprerator = new ConsistencyDataOperator(context) {
                        @Override
                        public boolean endRegister() {
                            return true;
                        }
                    };
                    ConsistencyDataOperator.instance = mOprerator;
                }
                ReentrantReadWriteLock lock = null;
                try {
                    lock = createOrGetLock(_key);
                    lock.writeLock().lock();
                    mOprerator.put(_key, value);
                } catch (Exception e) {
                    if (DebugLog.isDebug()) {
                        DebugLog.d(TAG, e.toString());
                    }
                } finally {
                    if (lock != null) {
                        lock.writeLock().unlock();
                    }
                    tryToRemoveLock(_key);
                }
            }
        });

    }

    /**
     * 用来同步获取方法值,可能会ANR慎用！！！
     *
     * @param context
     * @param _key
     * @param defaultValue
     * @return
     */
    public static String getValueSync(final Context context, final String _key, final String defaultValue) {
        ConsistencyDataOperator mOprerator = ConsistencyDataOperator.instance;
        if (mOprerator == null) {
            DebugLog.d("ConsistencyDataOperator", "not init");
            mOprerator = new ConsistencyDataOperator(context) {
                @Override
                public boolean endRegister() {
                    return true;
                }
            };
            ConsistencyDataOperator.instance = mOprerator;
        }
        String ret = defaultValue;
        ReentrantReadWriteLock lock = null;
        try {
            lock = createOrGetLock(_key);
            lock.writeLock().lock();
            ret = mOprerator.get(_key, defaultValue);
        } catch (Exception e) {
            if (DebugLog.isDebug()) {
                DebugLog.d(TAG, e.toString());
            }
        } finally {
            if (lock != null) {
                lock.writeLock().unlock();
            }
            tryToRemoveLock(_key);
        }
        return ret;
    }

    /**
     * 用来获异步取跨进程数据
     */
    public static void getValueAsync(final Context context, final String _key, final String defaultValue, final IQueryDataCallback callback) {
        new AsyncJob<Object, String>(String.class) {

            @Override
            public String onRun(Object... params) throws Throwable {
                return getValueSync(context, _key, defaultValue);
            }

            @Override
            public void onPostExecutor(String result) {
                if (callback != null) {
                    callback.onCallBack(result);
                }
            }
        }.execute();

    }

    /**
     * 用来删除跨进程数据
     */
    public static void removeValue(final Context context, final String _key) {
        JobManagerUtils.postRunnable(new Runnable() {
            @Override
            public void run() {
                ConsistencyDataOperator mOprerator = ConsistencyDataOperator.instance;
                if (mOprerator == null) {
                    DebugLog.i("ConsistencyDataOperator", "not init");
                    mOprerator = new ConsistencyDataOperator(context) {
                        @Override
                        public boolean endRegister() {
                            return true;
                        }
                    };
                    ConsistencyDataOperator.instance = mOprerator;
                }
                ReentrantReadWriteLock lock = null;
                try {
                    lock = createOrGetLock(_key);
                    lock.writeLock().lock();
                    mOprerator.reMove(_key);
                } catch (Exception e) {
                    if (DebugLog.isDebug()) {
                        DebugLog.d(TAG, e.toString());
                    }
                } finally {
                    if (lock != null) {
                        lock.writeLock().unlock();
                    }
                    tryToRemoveLock(_key);
                }
            }
        });
    }

    /**
     * 用来监听某个Key跨进程数据的变化
     */
    public static void registerKeyObserver(Context context, String observerKey,
                                           ConsistencyContentObserver.ICrossProcessDataChangeListener listener) {
        if (listener != null && context != null) {
            ConsistencyDataOperator mOprerator = ConsistencyDataOperator.instance;
            if (mOprerator == null) {
                DebugLog.i("ConsistencyDataOperator", "not init");
                mOprerator = new ConsistencyDataOperator(context) {
                    @Override
                    public boolean endRegister() {
                        return true;
                    }
                };
                ConsistencyDataOperator.instance = mOprerator;
            }
            mOprerator.registerContentObserver(observerKey, listener);
        }
    }

    /**
     * 移除某个key的所有监听
     */
    public static void unregisterKeyObserver(Context context, String observerKey) {
        if (!TextUtils.isEmpty(observerKey) && context != null) {
            ConsistencyDataOperator mOprerator = ConsistencyDataOperator.instance;
            if (mOprerator == null) {
                DebugLog.i("ConsistencyDataOperator", "not init");
                mOprerator = new ConsistencyDataOperator(context) {
                    @Override
                    public boolean endRegister() {
                        return true;
                    }
                };
                ConsistencyDataOperator.instance = mOprerator;
            }
            mOprerator.unregisterContentObserver(observerKey);
        }
    }

    /**
     * 移除某个key 的特定的监听
     */
    public static void unregisterKeyObserver(Context context, String observerKey,
                                             ConsistencyContentObserver.ICrossProcessDataChangeListener listener) {
        if (listener != null && context != null) {
            ConsistencyDataOperator mOprerator = ConsistencyDataOperator.instance;
            if (mOprerator == null) {
                DebugLog.i("ConsistencyDataOperator", "not init");
                mOprerator = new ConsistencyDataOperator(context) {
                    @Override
                    public boolean endRegister() {
                        return true;
                    }
                };
                ConsistencyDataOperator.instance = mOprerator;
            }
            mOprerator.unregisterContentObserver(observerKey, listener);
        }
    }

    public interface IQueryDataCallback {
        void onCallBack(String result);
    }

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
}
