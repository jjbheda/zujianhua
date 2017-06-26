package org.qiyi.basecore.filedownload;

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

import org.qiyi.android.corejar.debug.DebugLog;

import java.io.File;
import java.util.UUID;

/**
 * @author kangle 通知栏
 */
public class FileDownloadNotification {

    private Context mContext;
    private NotificationManager notificationManager;
    private RemoteViews contentView;
    private Notification notification = null;

    /**
     * 上次通知栏更新百分比
     */
    private int lastDownloadPercent = -1;
    private FileDownloadNotificationConfiguration downloadNotificationConfiguration;

    public FileDownloadNotification(Context context,
            FileDownloadNotificationConfiguration downloadNotificationConfiguration) {
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



    private void notify(int id, Notification notification) {
        
        notificationManager.notify(id, notification);
    }






    public void setLastDownloadPercent(int lastDownloadPercent) {
        this.lastDownloadPercent = lastDownloadPercent;
    }




    /////////////////////////////////////////////////////////////////////////////////////////////////////
    public void onDownloadProgress(FileDownloadStatus fileDownloadStatus) {
        int downloadPercent = (int) fileDownloadStatus.getDownloadPercent();

        // 百分比没有变化
        if (downloadPercent == lastDownloadPercent) {
            return;
        }

        DebugLog.v(FileDownloadNotification.class.getSimpleName(), "getDownloadPercentStr: " + downloadPercent);

        lastDownloadPercent = downloadPercent;

        contentView.setTextViewText(downloadNotificationConfiguration.resIdForStatus,
                downloadNotificationConfiguration.downloadingStr);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForPercent, View.VISIBLE);
        contentView.setTextViewText(downloadNotificationConfiguration.resIdForPercent, String.valueOf(downloadPercent)
                + "%");

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForProgress, View.VISIBLE);
        contentView.setProgressBar(downloadNotificationConfiguration.resIdForProgress, 100, downloadPercent, false);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForContent, View.GONE);

        Notification notification =
                getNotification(downloadNotificationConfiguration.downloadingStr, fileDownloadStatus);

        notify(fileDownloadStatus.getIdAsInteger(), notification);
    }

    public void onPaused(FileDownloadStatus fileDownloadStatus) {

        contentView.setTextViewText(downloadNotificationConfiguration.resIdForStatus,
                downloadNotificationConfiguration.pausedStr);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForPercent, View.GONE);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForProgress, View.GONE);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForContent, View.VISIBLE);

        contentView.setTextViewText(downloadNotificationConfiguration.resIdForContent,
                FileDownloadConstant.getPausedReasonStr(fileDownloadStatus.reason));

        Notification notification = getNotification(downloadNotificationConfiguration.pausedStr, fileDownloadStatus);

        notify(fileDownloadStatus.getIdAsInteger(), notification);
    }

    public void onFailed(FileDownloadStatus fileDownloadStatus) {
        contentView.setTextViewText(downloadNotificationConfiguration.resIdForStatus,
                downloadNotificationConfiguration.failedStr);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForPercent, View.GONE);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForProgress, View.GONE);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForContent, View.VISIBLE);

        contentView.setTextViewText(downloadNotificationConfiguration.resIdForContent,
                FileDownloadConstant.getFailedReasonStr(fileDownloadStatus.reason));

        Notification notification = getNotification(downloadNotificationConfiguration.failedStr, fileDownloadStatus);

        notify(fileDownloadStatus.getIdAsInteger(), notification);
    }

    public void onCompleted(File downloadedFile, FileDownloadStatus fileDownloadStatus) {
        contentView.setTextViewText(downloadNotificationConfiguration.resIdForStatus,
                downloadNotificationConfiguration.completedTitleStr);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForPercent, View.GONE);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForProgress, View.GONE);

        contentView.setViewVisibility(downloadNotificationConfiguration.resIdForContent, View.VISIBLE);
        contentView.setTextViewText(downloadNotificationConfiguration.resIdForContent,
                downloadNotificationConfiguration.completedContentStr);

        Notification notification = getNotification(downloadNotificationConfiguration.completedTitleStr, fileDownloadStatus);

        notify(fileDownloadStatus.getIdAsInteger(), notification);
    }

    @SuppressWarnings("deprecation")
    private Notification getNotification(String ticker, FileDownloadStatus downloadStatus) {

        PendingIntent pendingIntent = null;

        if (downloadNotificationConfiguration.pendingIntentClass != null) {
            Intent intent = new Intent(mContext, downloadNotificationConfiguration.pendingIntentClass);
            intent.putExtra(FileDownloadNotificationConfiguration.INTENT_KEY_FOR_FILE_DOWNLOAD_STATUS,
                    (Parcelable) downloadStatus);

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

    public void onDismiss(FileDownloadStatus fileDownloadStatus) {
        notificationManager.cancel(fileDownloadStatus.getIdAsInteger());
    }

}
