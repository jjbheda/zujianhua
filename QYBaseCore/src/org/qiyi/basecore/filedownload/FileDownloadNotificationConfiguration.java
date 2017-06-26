package org.qiyi.basecore.filedownload;

import java.io.Serializable;

/**
 * @author kangle 配置自定义通知栏，和 FileDownloadNotification 配合使用
 */
public class FileDownloadNotificationConfiguration implements Serializable {

    private static final long serialVersionUID = -3642003766596609762L;

    public static final String INTENT_KEY_FOR_FILE_DOWNLOAD_STATUS = "INTENT_KEY_FOR_FILE_DOWNLOAD_STATUS";

    public FileDownloadNotificationConfiguration(int resIdForContentView, String downloadingStr, int resIdForStatus,
            int resIdForPercent, int resIdForProgress, int resIdForContent, int thumbnail, int resIdForLeftIcon,
            int leftDrawable, String pausedStr, String completedTitleStr, String completedContentStr, String failedStr,
            Class<?> pendingIntentClass) {

        this.resIdForContentView = resIdForContentView;
        this.downloadingStr = downloadingStr;
        this.resIdForStatus = resIdForStatus;
        this.resIdForPercent = resIdForPercent;
        this.resIdForProgress = resIdForProgress;
        this.resIdForContent = resIdForContent;

        this.thumbnail = thumbnail;
        this.resIdForLeftIcon = resIdForLeftIcon;
        this.leftDrawable = leftDrawable;

        this.pausedStr = pausedStr;
        this.completedTitleStr = completedTitleStr;
        this.completedContentStr = completedContentStr;
        this.failedStr = failedStr;

        this.pendingIntentClass = pendingIntentClass;
    }

    public int resIdForContentView;
    public String downloadingStr;
    public int resIdForStatus;
    public int resIdForPercent;
    public int resIdForProgress;
    public int resIdForContent;

    public int thumbnail;
    public int resIdForLeftIcon;
    public int leftDrawable;

    public String pausedStr;
    public String completedTitleStr;
    public String completedContentStr;
    public String failedStr;

    public Class<?> pendingIntentClass;

}
