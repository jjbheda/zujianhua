package com.iqiyi.video.download.filedownload.notification;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.view.View;
import android.widget.RemoteViews;

import org.qiyi.basecore.filedownload.FileDownloadConstant;
import org.qiyi.basecore.filedownload.FileDownloadNotificationConfiguration;
import org.qiyi.video.module.download.exbean.FileDownloadObject;

import java.util.UUID;

/**
 * Created by songguobin on 2017/2/22.
 */

public class DownloadFileNotification {

    private Context mContext;
    private NotificationManager notificationManager;
    private RemoteViews contentView;
    private Notification notification = null;

    /**
     * 上次通知栏更新百分比
     */
    private int lastDownloadPercent = -1;

    private FileDownloadNotificationConfiguration downloadNotificationConfiguration;

    public DownloadFileNotification(
            Context context,
            FileDownloadNotificationConfiguration downloadNotificationConfiguration
    ) {
        this.mContext = context;
        notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        contentView = new RemoteViews(mContext.getPackageName(), downloadNotificationConfiguration.resIdForContentView);

        this.downloadNotificationConfiguration = downloadNotificationConfiguration;

        if (downloadNotificationConfiguration.resIdForLeftIcon != -1
                && downloadNotificationConfiguration.leftDrawable != -1) {
            contentView.setImageViewResource(downloadNotificationConfiguration.resIdForLeftIcon,
                    downloadNotificationConfiguration.leftDrawable);
        }
    }

    ////////////////////////新下载进度/////////////////////////////////////////////////////////
    public void onDownloadProgress(int notifyId, FileDownloadObject bean) {

        // 百分比没有变化
        if ((int)(bean.getDownloadPercent()) == lastDownloadPercent) {
            return;
        }

        lastDownloadPercent = (int) bean.getDownloadPercent();

        contentView.setTextViewText(downloadNotificationConfiguration.resIdForStatus,
                downloadNotificationConfiguration.downloadingStr);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForPercent, View.VISIBLE);
        contentView.setTextViewText(downloadNotificationConfiguration.resIdForPercent, String.valueOf((int) bean.getDownloadPercent())
                + "%");

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForProgress, View.VISIBLE);
        contentView.setProgressBar(downloadNotificationConfiguration.resIdForProgress, 100, (int) bean.getDownloadPercent(), false);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForContent, View.GONE);

        Notification notification =
                getNotification(downloadNotificationConfiguration.downloadingStr, bean);

        notify(notifyId, notification);
    }

    private void notify(int id, Notification notification) {

        notificationManager.notify(id, notification);
    }

    public void onPaused(int notifyid, FileDownloadObject bean) {

        contentView.setTextViewText(downloadNotificationConfiguration.resIdForStatus,
                downloadNotificationConfiguration.pausedStr);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForPercent, View.GONE);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForProgress, View.GONE);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForContent, View.VISIBLE);

        contentView.setTextViewText(downloadNotificationConfiguration.resIdForContent,
                FileDownloadConstant.getPausedReasonStr(FileDownloadConstant.PAUSED_MANUALLY));

        Notification notification = getNotification(downloadNotificationConfiguration.pausedStr, bean);

        notify(notifyid, notification);
    }

    public void onFailed(int notifyId, FileDownloadObject bean) {
        contentView.setTextViewText(downloadNotificationConfiguration.resIdForStatus,
                downloadNotificationConfiguration.failedStr);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForPercent, View.GONE);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForProgress, View.GONE);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForContent, View.VISIBLE);

        contentView.setTextViewText(downloadNotificationConfiguration.resIdForContent,
                FileDownloadConstant.getFailedReasonStr(FileDownloadConstant.ERROR_UNKNOWN));

        Notification notification = getNotification(downloadNotificationConfiguration.failedStr, bean);

        notify(notifyId, notification);
    }

    public void onCompleted(int notifyId, FileDownloadObject bean) {
        contentView.setTextViewText(downloadNotificationConfiguration.resIdForStatus,
                downloadNotificationConfiguration.completedTitleStr);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForPercent, View.GONE);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForProgress, View.GONE);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForContent, View.VISIBLE);
        contentView.setTextViewText(downloadNotificationConfiguration.resIdForContent,
                downloadNotificationConfiguration.completedContentStr);

        Notification notification = getNotification(downloadNotificationConfiguration.completedTitleStr, bean);

        notify(notifyId, notification);
    }

    public void setLastDownloadPercent(int lastDownloadPercent) {
        this.lastDownloadPercent = lastDownloadPercent;
    }

    @SuppressWarnings("deprecation")
    private Notification getNotification(String ticker, FileDownloadObject bean) {

        PendingIntent pendingIntent = null;

        if (downloadNotificationConfiguration.pendingIntentClass != null) {
            Intent intent = new Intent(mContext, downloadNotificationConfiguration.pendingIntentClass);
            intent.putExtra(FileDownloadNotificationConfiguration.INTENT_KEY_FOR_FILE_DOWNLOAD_STATUS,
                    (Parcelable) bean);

            if (Activity.class.isAssignableFrom(downloadNotificationConfiguration.pendingIntentClass)) {
                pendingIntent = PendingIntent.getActivity(mContext, UUID.randomUUID().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            } else if (Service.class.isAssignableFrom(downloadNotificationConfiguration.pendingIntentClass)) {
                pendingIntent = PendingIntent.getService(mContext, UUID.randomUUID().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
        }

        if (notification == null) {
            if (android.os.Build.VERSION.SDK_INT < 11) {

                notification = new Notification();
                notification.icon = downloadNotificationConfiguration.thumbnail;
                notification.tickerText = ticker;
                notification.contentView = contentView;
                notification.contentIntent = pendingIntent;

            } else {
                notification =
                        new Notification.Builder(mContext).setSmallIcon(downloadNotificationConfiguration.thumbnail)
                                .setContent(contentView).setContentIntent(pendingIntent).setAutoCancel(false)
                                .setTicker(ticker).getNotification();
            }
        } else {
            notification.tickerText = ticker;
            notification.contentView = contentView;
            notification.contentIntent = pendingIntent;
        }

        return notification;
    }

    public void onDismiss(int notifyId) {
        notificationManager.cancel(notifyId);
    }


}
