package org.qiyi.basecore.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;

import org.qiyi.android.corejar.debug.DebugLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 首页内存缓存管理类
 * Created by shenshan on 15/5/26.
 */
public class PageDataHolder {
    private static final String TAG = "PageDataHolder";
    private Map<String, List> cardCacheMap = new ConcurrentHashMap<>(4);
    private Map<String, Object> pageDataCacheMap = new ConcurrentHashMap<>(2);
    private static PageDataHolder instance;
    private TimeTickReceiver tickReceiver = null;

    private PageDataHolder() {
        tickReceiver = new TimeTickReceiver();
    }

    public static PageDataHolder getInstance() {
        synchronized (PageDataHolder.class) {
            if (instance == null) {
                instance = new PageDataHolder();
            }
        }
        return instance;
    }

    /**
     * 页面需要缓存的数据
     * <p>
     * 大数据谨慎存放
     *
     * @param key
     * @param pageDataCache
     */
    public <T> T putPageDataCache(String key, T pageDataCache) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return (T) pageDataCacheMap.put(key, pageDataCache);
    }

    public <T> T getPageDataCache(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return (T) pageDataCacheMap.get(key);
    }

    /**
     * 放入缓存池
     *
     * @param key
     * @param cardModels
     * @return the value of any previous mapping with the specified key or null if there was no mapping.
     */
    public <T> List<T> putCardModels(String key, List<T> cardModels) {
        DebugLog.log(TAG, "putCardModels key " + key + " cardModels " + cardModels.size());
        startClearTask();
        if (!StringUtils.isEmpty(cardModels)) {
            return cardCacheMap.put(key, new ArrayList(cardModels));
        } else {
            return null;
        }
    }

    /***
     * 删除缓存
     */
    public <T> void removeCardModels(String key, T model) {
        List<T> cardModelHolders = (List<T>) cardCacheMap.get(key);
        if (!StringUtils.isEmpty(cardModelHolders) && model != null) {
            cardModelHolders.remove(model);
        }
    }

    /**
     * 取得缓存
     *
     * @param key
     * @return
     */
    public <T> List<T> getCardModels(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        List<T> cardModelHolders = (List<T>) cardCacheMap.get(key);
        if (cardModelHolders != null) {
            DebugLog.log(TAG, "getCardModels key " + key + " cardCacheMap.get(key) " + cardModelHolders.size());
        }
        if (!StringUtils.isEmpty(cardModelHolders)) {
            return new ArrayList<T>(cardModelHolders);
        }
        return cardModelHolders;
    }

    /**
     * 根据key清理
     *
     * @param key
     * @return the value of the removed mapping or null if no mapping for the specified key was found.
     */
    public <T> List<T> clearCache(String key) {
        return (List<T>) cardCacheMap.remove(key);
    }

    public void clearPrefixCache(String prefix) {
        List<String> delKeys = new ArrayList<>();
        Set<String> keys = cardCacheMap.keySet();
        if (!StringUtils.isEmpty(keys)) {
            for (String key : keys) {
                if (!StringUtils.isEmpty(keys) && key.startsWith(prefix)) {
                    delKeys.add(key);
                }
            }
        }
        if (DebugLog.isDebug()) {
            DebugLog.log(TAG,"clearPrefixCache:" + delKeys);
        }
        if (!StringUtils.isEmpty(delKeys)) {
            for (String key : keys) {
                clearCache(key);
            }
        }
    }

    /**
     * 清理所以的内存
     */
    public void clearCache() {
        cardCacheMap.clear();
        pageDataCacheMap.clear();
    }


    public class TimeTickReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            new AsyncTask<Void, Void, List<String>>() {
                @Override
                protected List<String> doInBackground(Void... params) {
                    Thread.currentThread().setName("PageDataHolder");

                    List<String> removeList = new ArrayList<String>(2);
                    Set<Map.Entry<String, List>> entries = cardCacheMap.entrySet();
                    for (Map.Entry<String, List> entry : entries) {
                        long cacheUntil = StringUtils.parseLong(getPageDataCache(entry.getKey()), -1L);
                        if (cacheUntil - System.currentTimeMillis() <= 0) {
                            removeList.add(entry.getKey());
                        }
                    }
                    return removeList;
                }

                @Override
                protected void onPostExecute(List<String> removeList) {
                    for (String key : removeList) {
                        clearCache(key);
                        if (DebugLog.isDebug()) {
                            DebugLog.v(TAG,"remove cache from tick");
                            // ToastUtils.toast(context, "remove cache from tick");Context context, CharSequence text, @Duration int duration
                        }
                    }
                    //没有缓存停止监听
                    if (cardCacheMap.size() == 0) {
                        stopClearTask();
                    }
                }
            }.execute();
        }
    }

    private IntentFilter tickFilter = null;

    public void startClearTask() {
        if (tickFilter == null) {
            try {
                tickFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
                ApplicationContext.app.registerReceiver(tickReceiver, tickFilter);
            } catch (Exception e) {
                if (DebugLog.isDebug()) {
                    throw new RuntimeException(e);
                }
                DebugLog.e(TAG, e.getLocalizedMessage());
            }
        }
    }

    public void stopClearTask() {
        if (tickFilter != null) {
            try {
                ApplicationContext.app.unregisterReceiver(tickReceiver);
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            } finally {
                tickFilter = null;
            }
        }
    }

}
