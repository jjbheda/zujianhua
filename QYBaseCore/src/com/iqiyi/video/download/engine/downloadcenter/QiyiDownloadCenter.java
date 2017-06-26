package com.iqiyi.video.download.engine.downloadcenter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.iqiyi.video.download.engine.downloader.IQiyiDownloader;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.storage.StorageCheckor;
import org.qiyi.basecore.utils.NetWorkTypeUtils;
import org.qiyi.basecore.utils.NetworkStatus;
import org.qiyi.basecore.utils.StringUtils;
import org.qiyi.video.module.download.exbean.XTaskBean;

import java.util.HashMap;

/**
 * 1、负责管理所有的下载器，目前支持视频下载和游戏下载
 * 2、提供网络和sd卡监听
 * Created by yuanzeyao on 2015/5/29.
 */
public class QiyiDownloadCenter implements IQiyiDownloadCenter {

    private static final String TAG = "QiyiDownloadCenter";


    //存储所有类型的下载器，如视频下载，游戏下载
    private HashMap<Class, IQiyiDownloader> mDownloads = new HashMap<Class, IQiyiDownloader>();

    //上下文环境，这里就是QiyiDownloadCenterService
    private Context mContext;

    //SD卡Receiver
    private EnvironmentChangeReceiver mEnvironmentReceiver;

    // 标识是否初始化过
    private boolean mHasInit;

    //自动下载回调
    //private AutoDownloadController.IAutoDownloadCallBack mAutoCallBack;

    public QiyiDownloadCenter(Context mContext) {
        if (mContext != null) {
           // DownloadDBFactory.getInstance().init(mContext.getApplicationContext());
            this.mContext = mContext.getApplicationContext();
        }
    }

    @Override
    public void init() {
        if (mHasInit) {
            DebugLog.log(TAG, "QiyiDownloadCenter is already init!");
            return;
        }
        mHasInit = true;
        //遍历所有下载器，进行初始化工作
        for (IQiyiDownloader downloader : mDownloads.values()) {
            downloader.init();
        }
    }

    /**
     * 注册广播
     */
    public void registerReceiver() {
        // 添加对网络和sd卡的监听
        if (mEnvironmentReceiver == null) {
            mEnvironmentReceiver = new EnvironmentChangeReceiver(mContext);
        }
        IntentFilter intentFilterSDCard = new IntentFilter();
        intentFilterSDCard.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilterSDCard.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilterSDCard.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilterSDCard.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilterSDCard.addDataScheme("file");
        IntentFilter intentFilterNet = new IntentFilter();
        intentFilterNet.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

       // IntentFilter autoFilter = new IntentFilter();
      //  autoFilter.addAction(AutoDownloadController.AUTO_DOWNLOAD_NEXT_REQUEST);
       // autoFilter.addAction(AutoDownloadController.AUTO_DOWNLOAD_NEXT_RETRY);

      //  IntentFilter playCoreFilter = new IntentFilter();
      //  playCoreFilter.addAction(PLAY_CORE_ACTION);

       // IntentFilter screenFilter = new IntentFilter();
       // screenFilter.addAction(Intent.ACTION_SCREEN_ON);


        mContext.registerReceiver(mEnvironmentReceiver, intentFilterSDCard);
        mContext.registerReceiver(mEnvironmentReceiver, intentFilterNet);
       // mContext.registerReceiver(mEnvironmentReceiver, autoFilter);
       // mContext.registerReceiver(mEnvironmentReceiver, playCoreFilter);
       // mContext.registerReceiver(mEnvironmentReceiver, screenFilter);
    }

    @Override
    public <T extends XTaskBean> boolean addDownloader(Class<T> clazz, IQiyiDownloader<T> downloader) {
        mDownloads.put(clazz, downloader);
        return true;
    }

    @Override
    public <T extends XTaskBean> boolean removeDownloader(Class<T> clazz) {
        IQiyiDownloader mDownloader = mDownloads.remove(clazz);
        if (mDownloader != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public <T extends XTaskBean> IQiyiDownloader<T> getDownloader(Class<T> clazz) {
        return mDownloads.get(clazz);
    }

    @Override
    public synchronized void exit() {
        DebugLog.log(TAG, "QiyiDwonloadCenter-->exit");
        // 清空所有下载管理器的内容
        for (IQiyiDownloader downloader : mDownloads.values()) {
            if (downloader != null) {
                downloader.exit();
            }
        }
        mHasInit = false;
    }

    /**
     * 取消广播
     */
    public void unregisterReceiver() {
        if (mEnvironmentReceiver != null) {
            DebugLog.log(TAG, "unregisterNetworkChangeReceiver!=null");
            mContext.unregisterReceiver(mEnvironmentReceiver);
            mEnvironmentReceiver = null;
        }
    }

  /*  @Override
    public void tryAutoDownload() {
        //每次启动app的时候进行一次请求，传入-1表示不用重试
        AutoTools.handleAutoNextRequest(mContext, mAutoCallBack, -1);
    }*/

  /*  @Override
    public void setAutoDownloadCallBack(AutoDownloadController.IAutoDownloadCallBack callBack) {
        this.mAutoCallBack = callBack;
    }*/

    /**
     * 监听下载过程中的环境改变，包括网络广播和sd卡拔插广播。
     * SD卡插拔时，会重新扫描当前所有的sd卡根路径。
     *
     * @author KANGLE
     *         modify by LJQ 2014-5-15 添加当连上网络同步奇谱ID的需求
     *         modify by jasontujun 2014-8-13 连上3G时，下载设置成不自动执行;非3G时自动执行
     */
    private class EnvironmentChangeReceiver extends BroadcastReceiver {
        private final int MSG_NETWORK_CHANGED = 0;
        private final int MSG_SDCARD_CHANGED = 1;
        private NetworkStatus currentStatus;
        private Context mInnerContext;

        Handler receiverHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_SDCARD_CHANGED:
                        handleSDCardMountsInDownload(mInnerContext, (String) msg.obj);
                        break;
                    case MSG_NETWORK_CHANGED:
                        handleNetStatusChangeInDownload((NetworkStatus) msg.obj);
                        break;
                    default:
                        break;
                }
            }
        };

        private EnvironmentChangeReceiver(Context context) {
            this.mInnerContext = context;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String name = StringUtils.toStr(intent.getAction(), "");
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(name)) {
                DebugLog.log(TAG, "网络onReceive");
                receiverHandler.removeMessages(MSG_NETWORK_CHANGED);
                Bundle bundle = intent.getExtras();
                NetworkInfo netInfo = null;
                if (bundle != null) {
                    netInfo = (NetworkInfo) bundle.get(ConnectivityManager.EXTRA_NETWORK_INFO);
                }

                if (netInfo == null) {
                    return;
                }
                DebugLog.log(TAG, "netInfo = " + netInfo.getType());

                NetworkInfo.State state = netInfo.getState();
                ConnectivityManager connectivityManager = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

                if (activeNetInfo != null) {
                    DebugLog.log(TAG,
                            "activeNetInfo.getType() = " + activeNetInfo.getType());
                }
                if ((state == NetworkInfo.State.CONNECTED)
                        && (activeNetInfo != null)
                        && (activeNetInfo.getType() != netInfo.getType())) {
                    return;
                }
                NetworkStatus status = NetWorkTypeUtils.getNetworkStatusFor4G(context);
                Message message = Message.obtain();
                message.what = MSG_NETWORK_CHANGED;
                message.obj = status;
                DebugLog.log(TAG, "status = " + status);
                //非wifi下需要立即处理
                if (!(NetworkStatus.WIFI == status
                        || NetworkStatus.OFF == status)) {
                    receiverHandler.sendMessage(message);
                } else {
                    receiverHandler.sendMessageDelayed(message, 1000);
                }

            } /*else if (name.equals(AutoDownloadController.AUTO_DOWNLOAD_NEXT_REQUEST)) {
                AutoTools.handleAutoNextRequest(mContext, mAutoCallBack, 0);
            } else if (name.equals(AutoDownloadController.AUTO_DOWNLOAD_NEXT_RETRY)) {
                AutoTools.handleAutoNextRetry(mContext, mAutoCallBack);
            } else if (name.equals(PLAY_CORE_ACTION)) {
                DebugLog.log(TAG, "onRceive-->action:qy.player.core.type");
                String playCore = intent.getStringExtra("current_play_core");
                if (!TextUtils.isEmpty(playCore)) {
                    DebugLog.log(TAG, "playCore:" + playCore);
                    if (!AutoDownloadConfig.getInstance().getPlayCore().equals(playCore)) {
                        AutoDownloadConfig.getInstance().setPlayCore(playCore);
                    }

                }

            } else if (name.equals(Intent.ACTION_SCREEN_ON)) {
                if (AutoDownloadConfig.getInstance().getToastType() == 1) {
                    int resourceId = ResourcesTool.getResourceIdForString("phone_download_auto_success");
                    if (resourceId == -1) {
                        ResourcesTool.init(mContext);
                        resourceId = ResourcesTool.getResourceIdForString("phone_download_auto_success");
                    }
                    if (resourceId != -1) {
                        Toast.makeText(mContext, mContext.getString(resourceId), Toast.LENGTH_LONG).show();
                    }
                }
                AutoDownloadConfig.getInstance().setToastType(0);
            }*/ else {
                receiverHandler.removeMessages(MSG_SDCARD_CHANGED);
                Message message = Message.obtain();
                message.what = MSG_SDCARD_CHANGED;
                message.obj = name;
                receiverHandler.sendMessageDelayed(message, 2000);
            }


        }


        /**
         * 处理网络变化
         *
         * @param status
         */
        private void handleNetStatusChangeInDownload(NetworkStatus status) {
            // 无变化
            if (status == currentStatus) {
                return;
            }
            DebugLog.log(TAG, "handleNetStatusChangeInDownload:" + status + ", last status:" + currentStatus);
            // 2/3G连接，暂停并且询问
            if (NetworkStatus.MOBILE_2G == status || NetworkStatus.MOBILE_3G == status
                    || NetworkStatus.MOBILE_4G == status
                    || NetworkStatus.OTHER == status) {
                DebugLog.log(TAG, ">>handleNetStatusChangeInDownload:2/3G连接");
                for (IQiyiDownloader downloader : mDownloads.values()) {
                    downloader.handleNetWorkChange(IQiyiDownloader.NET_MOBILE);
                }

               /* //恢复移動网络时，检查开启追剧模式的剧集是否存在 最新剧集集合为空的，如果有，则请求
                if (!TextUtils.isEmpty(AutoDownloadController.getInstance().findAllOpenAndEmptyEpisodeEntity())) {
                    DebugLog.log(TAG, "has empty episode set,so request it!");
                    AutoTools.requestLatestEpisodes(AutoDownloadController.getInstance().findAllOpenAndEmptyEpisodeEntity(), true);
                }*/
            }
            // wifi连接，恢复因为网络暂停的任务
            else if (NetworkStatus.WIFI == status && currentStatus != null) {
                DebugLog.log(TAG, ">>handleNetStatusChangeInDownload:wifi连接");
                for (IQiyiDownloader downloader : mDownloads.values()) {
                    downloader.handleNetWorkChange(IQiyiDownloader.NET_WIFI);
                }

              /*  //恢复wifi网络时，检查开启追剧模式的剧集是否存在 最新剧集集合为空的，如果有，则请求
                if (!TextUtils.isEmpty(AutoDownloadController.getInstance().findAllOpenAndEmptyEpisodeEntity())) {
                    DebugLog.log(TAG, "has empty episode set,so request it!");
                    AutoTools.requestLatestEpisodes(AutoDownloadController.getInstance().findAllOpenAndEmptyEpisodeEntity(), true);
                }

                //恢复wifi网络时，检查上次是否有因为没有网络导致没有执行的请求
                if (AutoDownloadConfig.getInstance().getNeedRequestWhenHasNet()) {
                    AutoTools.handleAutoNextRequest(mContext, mAutoCallBack, 0);
                }
*/
            }
            // 无网络，暂停并且通知
            else if (NetworkStatus.OFF == status) {
                DebugLog.log(TAG, ">>handleNetStatusChangeInDownload:无网络");
                for (IQiyiDownloader downloader : mDownloads.values()) {
                    downloader.handleNetWorkChange(IQiyiDownloader.NET_OFF);
                }
            }
            currentStatus = status;
        }

        /**
         * 处理SD卡插拔逻辑
         *
         * @param context
         * @param action
         */
        private void handleSDCardMountsInDownload(Context context, String action) {
            DebugLog.log(TAG, "handleSDCardMountsInDownload :" + action);
            // 插卡
            if (action.equals("android.intent.action.MEDIA_MOUNTED")) {
                DebugLog.log(TAG, "handleSDCardMountsInDownload-->插卡");
                // 重新扫描可用sd卡
                new AsyncTask<Context, Void, Void>() {
                    @Override
                    protected Void doInBackground(Context... params) {
                        Thread.currentThread().setName("download-sdcard");
                        StorageCheckor.scanSDCards(params[0]);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        for (IQiyiDownloader downloader : mDownloads.values()) {
                            downloader.handleSdCardChange(IQiyiDownloader.SD_CARD_INSERT);
                        }
                    }
                }.execute(context);
            }
            // 拔卡
            else if (action.equals("android.intent.action.MEDIA_REMOVED")
                    || action.equals("android.intent.action.MEDIA_UNMOUNTED")
                    || action.equals("android.intent.action.MEDIA_BAD_REMOVAL")) {
                DebugLog.log(TAG, "handleSDCardMountsInDownload-->拔卡");
                new AsyncTask<Context, Void, Void>() {
                    @Override
                    protected Void doInBackground(Context... params) {
                        Thread.currentThread().setName("download-sdcard");
                        StorageCheckor.scanSDCards(params[0]);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        for (IQiyiDownloader downloader : mDownloads.values()) {
                            downloader.handleSdCardChange(IQiyiDownloader.SD_CARD_REMOVE);
                        }
                    }
                }.execute(context);
            }
        }
    }


}
