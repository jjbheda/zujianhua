package org.qiyi.basecore.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.qiyi.android.corejar.debug.DebugLog;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = NetworkChangeReceiver.class.getSimpleName();

    private static final int MSG_NETWORK_CHANGED = 0;
    // 主动查询网络状态
    private static final int MSG_RETRIEVE_NETWORK_STATUS = 1;
    // 主动查询间隔时长
    private static final long RETRIEVE_STEP = 2 * 1000L;

    private volatile static NetworkChangeReceiver sNetworkChangeReceiver;
    private Context mContext;
    private NetworkStatus mCurrentNetworkStatus;
    private Map<String, INetChangeCallBack> mCallbackMap = new ConcurrentHashMap<String, INetChangeCallBack>();
    private CopyOnWriteArraySet<String> mNeedAutoRetrieveTag = new CopyOnWriteArraySet<String>();

    private NetworkChangeReceiver() {
    }

    /**
     * @param context
     * @return
     */
    public static NetworkChangeReceiver getNetworkChangeReceiver(Context context) {
        if (sNetworkChangeReceiver == null) {
            synchronized (NetworkChangeReceiver.class) {
                if (sNetworkChangeReceiver == null) {
                    sNetworkChangeReceiver = new NetworkChangeReceiver();
                    sNetworkChangeReceiver.mContext = context.getApplicationContext();
                    sNetworkChangeReceiver.register(sNetworkChangeReceiver.mContext);
                    sNetworkChangeReceiver.mCurrentNetworkStatus = NetWorkTypeUtils.getNetworkStatus(
                            sNetworkChangeReceiver.mContext);
                }
            }
        }
        return sNetworkChangeReceiver;
    }

    public static boolean hasInstance() {
        return sNetworkChangeReceiver == null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String name = StringUtils.toStr(intent.getAction(), "");
        this.mContext = context.getApplicationContext();
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(name)) {
            //可以删除第一次注册时系统的callback调用
            mHandler.removeMessages(MSG_NETWORK_CHANGED);
            NetworkStatus status = NetWorkTypeUtils.getNetworkStatusFor4G(context);

            Message msg = Message.obtain();
            msg.what = MSG_NETWORK_CHANGED;
            msg.obj = status;
            mHandler.sendMessage(msg);
        }
    }

    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_NETWORK_CHANGED:
                    handleNetStatusChange((NetworkStatus) msg.obj);
                    if (canLoopRetrieveMsg()) {
                        cleanRetrieveMsg(this);
                        sendRetrieveMsg(this);
                    }
                    break;
                case MSG_RETRIEVE_NETWORK_STATUS:
                    NetworkStatus status = NetWorkTypeUtils.getNetworkStatusFor4G(mContext);
                    if (null != status) {
                        handleNetStatusChange(status);
                    }
                    if (canLoopRetrieveMsg()) {
                        cleanRetrieveMsg(this);
                        sendRetrieveMsg(this);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void sendRetrieveMsg(@NonNull Handler handler) {
        Message retrieveMsg = Message.obtain();
        retrieveMsg.what = MSG_RETRIEVE_NETWORK_STATUS;
        handler.sendMessageDelayed(retrieveMsg, RETRIEVE_STEP);
    }

    private void cleanRetrieveMsg(@NonNull Handler handler) {
        handler.removeMessages(MSG_RETRIEVE_NETWORK_STATUS);
    }

    private boolean canLoopRetrieveMsg() {
        return mNeedAutoRetrieveTag.size() > 0;
    }

    private void handleNetStatusChange(NetworkStatus status) {
        //第一次register时会走一次
        if (mCurrentNetworkStatus == null) {
            mCurrentNetworkStatus = status;
            return;
        }

        if (mCurrentNetworkStatus.compareTo(status) == 0) {
            return;
        }

        mCurrentNetworkStatus = status;
        Iterator<Map.Entry<String, INetChangeCallBack>> entries = mCallbackMap.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, INetChangeCallBack> entry = entries.next();
            if (entry.getValue() != null) {
                handleMessageChangeCallback(status, entry.getValue());
            }
        }
    }

    private boolean ensureMainThread(final NetworkStatus status, final INetChangeCallBack callback) {
        //要保证这个回调是在主线程
        if (callback == null) {
            return true;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    handleMessageChangeCallback(status, callback);
                }
            });
            return true;
        }
        return false;
    }

    private void handleMessageChangeCallback(final NetworkStatus status, final INetChangeCallBack callback) {
        if (ensureMainThread(status, callback)) {
            return;
        }
        if (callback instanceof AbsNetworkChangeCallback) {
            handleMessageChangeCallback(status, (AbsNetworkChangeCallback) callback);
        } else {
            callback.onNetWorkChange(NetworkStatus.OFF != status);
        }
    }


    private void handleMessageChangeCallback(final NetworkStatus status, final AbsNetworkChangeCallback callback) {
        if (ensureMainThread(status, callback)) {
            return;
        }
        callback.onNetWorkChange(NetworkStatus.OFF != status);
        callback.onNetworkChange(status);

        if (NetworkStatus.WIFI == status) {
            callback.onChangeToWIFI(status);
        }

        if (NetworkStatus.OFF == status) {
            callback.onChangeToOff(status);
        }

        if (NetworkStatus.MOBILE_2G == status || NetworkStatus.MOBILE_3G == status || NetworkStatus.MOBILE_4G == status) {
            callback.onChangeToMobile2GAnd3GAnd4G(status);
        }

        if (NetworkStatus.MOBILE_2G == status) {
            callback.onChangeToMobile2G(status);
        }

        if (NetworkStatus.MOBILE_3G == status) {
            callback.onChangeToMobile3G(status);
        }

        if (NetworkStatus.MOBILE_4G == status) {
            callback.onChangeToMobile4G(status);
        }

        if (NetworkStatus.OFF != status && NetworkStatus.OTHER != status) {
            callback.onChangeToConnected(status);
        }

        if (NetworkStatus.OFF != status && NetworkStatus.WIFI != status) {
            callback.onChangeToNotWIFI(status);
        }
    }

    /**
     * 注册callback到receiver中
     *
     * @param tag
     * @param callback
     */
    public void registReceiver(String tag, AbsNetworkChangeCallback callback) {
        registerNetworkChangObserver(tag, callback, false);
    }

    /**
     * 注册callback到receiver中
     *
     * @param callBack
     */
    public void registReceiver(INetChangeCallBack callBack) {
        if (callBack != null) {
            String tag = String.valueOf(callBack.hashCode());
            if (mCallbackMap.get(tag) == callBack) {
                DebugLog.v(TAG, "该callback已经注册网络变化监听");
                return;
            } else {
                mCallbackMap.put(tag, callBack);
                if (canLoopRetrieveMsg() && !mHandler.hasMessages(MSG_RETRIEVE_NETWORK_STATUS)) {
                    sendRetrieveMsg(mHandler);
                }
            }
        }
    }

    /**
     * 添加网络监听变化监听
     *
     * @param tag                建议一个特殊不重复的字符串，如果为空则采用{callback}的hashCode()
     * @param callback
     * @param needRetrieveBySelf 是否开启固定间隔主动查询网络状态(由于在一些特殊情况下系统的网络切换事件的
     *                           Broadcast比较晚或者没有)，true 开启自查询，false不开启
     */
    public void registerNetworkChangObserver(String tag, AbsNetworkChangeCallback callback, boolean needRetrieveBySelf) {
        if (null == callback) {
            return;
        }
        if (TextUtils.isEmpty(tag)) {
            tag = callback.hashCode() + "";
        }
        if (mCallbackMap.get(tag) == callback) {
            DebugLog.v(TAG, "该callback已经注册网络变化监听");
            return;
        } else {
            mCallbackMap.put(tag, callback);
            callback.mNeedRetrieveBySelf = needRetrieveBySelf;
            if (needRetrieveBySelf) {
                mNeedAutoRetrieveTag.add(tag);
                if (canLoopRetrieveMsg() && !mHandler.hasMessages(MSG_RETRIEVE_NETWORK_STATUS)) {
                    sendRetrieveMsg(mHandler);
                }
            }
            handleMessageChangeCallback(mCurrentNetworkStatus, callback);
        }
    }

    /**
     * 全局注册网络变化监听器
     *
     * @param context
     */
    private void register(Context context) {
        try {
            IntentFilter intentFilterNet = new IntentFilter();
            intentFilterNet.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(this, intentFilterNet);
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
    }

    /**
     * 取消全局变化监听器
     */
    public void unRegister() {
        if (mContext != null && sNetworkChangeReceiver != null) {
            try {
                cleanRetrieveMsg(mHandler);
                mContext.unregisterReceiver(this);
            } catch (Exception e) {
                DebugLog.d(TAG, "execption in unRegister: " + e);
            }
        }
    }

    /**
     * 取消receiver中的callback
     *
     * @param tag
     */
    public void unRegistReceiver(String tag) {
        if (!TextUtils.isEmpty(tag) && mCallbackMap.containsKey(tag)) {
            INetChangeCallBack removed = mCallbackMap.remove(tag);
            if (removed instanceof AbsNetworkChangeCallback) {
                if (((AbsNetworkChangeCallback) removed).mNeedRetrieveBySelf) {
                    mNeedAutoRetrieveTag.remove(tag);
                    if (!canLoopRetrieveMsg()) {
                        cleanRetrieveMsg(mHandler);
                    }
                }
            }
        }
    }

    /**
     * 取消receiver中的callback
     *
     * @param callBack
     */
    public void unRegistReceiver(INetChangeCallBack callBack) {
        if (callBack != null) {
            String tag = String.valueOf(callBack.hashCode());
            if (mCallbackMap.containsKey(tag)) {
                mCallbackMap.remove(tag);
                if (callBack instanceof AbsNetworkChangeCallback &&
                        ((AbsNetworkChangeCallback) callBack).mNeedRetrieveBySelf) {
                    mNeedAutoRetrieveTag.remove(tag);
                    if (!canLoopRetrieveMsg()) {
                        cleanRetrieveMsg(mHandler);
                    }
                }
            }
        }
    }
}
