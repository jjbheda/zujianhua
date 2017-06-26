package org.qiyi.basecore.filedownload;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.qiyi.basecore.utils.NetworkStatus;
import org.qiyi.basecore.utils.StringUtils;

import java.io.File;
import java.io.Serializable;

/**
 * 对下载情况的封装 所有关于下载情况的判断，均应封装在此类，任何询问均由此类定义
 *
 * @author kangle
 *
 */
public class FileDownloadStatus implements Parcelable, Serializable{

    private static final long serialVersionUID = 3049653229296884938L;
    private static final int MOBILE_4G = 2;
    private static final int MOBILE_4G_OR_3G = 3;

    public long bytes_downloaded_so_far;
    public long total_size_bytes = -1;
    public int status = FileDownloadConstant.STATUS_PENDING;

    /**
     * 原因必须和其状态配套使用（原因只有在对应状态下值才正确）
     */
    public int reason;

    public DownloadConfiguration mDownloadConfiguration;

    /**
     * 真正的下载地址（DownloadConfiguration中的downloadUrl可能被重定向导致改变）
     */
    private String realDownloadUrl;

    /**
     * 下载文件的完整路径（包含文件名称）
     */
    private String downloadedFileAbsolutePath;

    public FileDownloadStatus(DownloadConfiguration downloadConfiguration) {
        this.mDownloadConfiguration = downloadConfiguration;

        this.realDownloadUrl = mDownloadConfiguration.downloadUrl;
    }

    /**
     * for IPC
     *
     * @param in
     */
    public FileDownloadStatus(Parcel in) {

        this.bytes_downloaded_so_far = in.readLong();
        this.total_size_bytes = in.readLong();

        this.status = in.readInt();
        this.reason = in.readInt();

        this.mDownloadConfiguration = (DownloadConfiguration) in.readSerializable();

        this.realDownloadUrl = in.readString();
        this.downloadedFileAbsolutePath = in.readString();
    }

    public FileDownloadStatus() {

    }

    public File getDownloadedFile() {
        File downloadedFile = null;
        if (!StringUtils.isEmpty(downloadedFileAbsolutePath)
                && (downloadedFile = new File(downloadedFileAbsolutePath)).exists()) {
            return downloadedFile;
        }
        return null;
    }

    public float getDownloadPercent() {

        float ret = 0;

        if (total_size_bytes != -1) {
            ret = (float) bytes_downloaded_so_far / total_size_bytes * 100;
        }

        return ret;
    }

    /**
     * @author kangle 封装下载的所有配置信息（网络限制、存储路径等）
     *  WARN 该类不能作为内部类使用
     */
    public static class DownloadConfiguration implements Serializable {

        private static final long serialVersionUID = 6878404786225862911L;
        public static final int DEFAULT_MAX_RETRY_FOR_NET = 2;
        public final long targetSize;

        /**
         * 默认值为0，表示可在任何非wifi下的移动网络下下载
         * 2  业务方要求在4G下可以下载，
         * 3  业务方要求在4G和3G下均可下载
         */
        private final int mobileNetType;

        protected int priority;

        /**
         * interPriority的上限
         */
        protected static final int INTER_PRIORITY_UPPER_BOUND = 1000;

        //FIXME KANGLE 多进程时会有问题
        private static int currentInterPriority;

        /**
         * 当priority未设置的时候，该优先级生效（先添加的任务>后添加的任务）
         */
        public int interPriority;

        /**
         * @param downloadUrl                               必须，下载地址
         * @param downloadedFilePath                    可选，下载路径（不包含文件名称）
         * @param downloadedFileName                  可选，下载文件名称（不包含下载路径）
         * @param fileDownloadNotification             可选，可以实现一个通知栏
         * @param allowedDownloadNotUnderWifi   可选，默认false；在非wifi下是否允许下载
         * @param forceToResume                            可选，默认false；每次会强制启动任务（当任务已存在的情况下）
         * @param isSupportResume                         可选，当将该值设置为true的话，下载的 downloadUrl 对应的资源必须要保证不变的，指定的下载位置 + 文件名必须是唯一的（假如同时制定的话）
         * @param customObj                                   可选，用户自定义的object，必须实现 Serializable
         * @param maxRetry                                   可选，当网络切换成可下载状态（非wifi下允许 or wifi）,会为由于网络导致下载失败的任务进行重试；该值用于设置重试的最大次数，默认为4次
         * @param priority                                   可选，下载任务的优先级
         * @param targetSize                                   可选，下载目标的大小，当下载的文件超过该大小的时候，会切换成下载失败
         * @param mobileNetType                                可选，当 allowedDownloadNotUnderWifi ＝＝ true 的时候，指定在何种移动网络下可下载
         */
        public DownloadConfiguration(String downloadUrl, String downloadedFilePath,
                String downloadedFileName,
                FileDownloadNotificationConfiguration fileDownloadNotification,
                boolean allowedDownloadNotUnderWifi, boolean forceToResume,
                boolean isSupportResume, Serializable customObj, int maxRetry, int priority, long targetSize, int mobileNetType) {

            this.downloadUrl = downloadUrl;
            this.downloadedFilePath = downloadedFilePath;
            this.downloadedFileName = downloadedFileName;
            this.fileDownloadNotification = fileDownloadNotification;
            this.allowedDownloadNotUnderWifi = allowedDownloadNotUnderWifi;
            this.forceToResume = forceToResume;
            this.isSupportResume = isSupportResume;
            this.customObj = customObj;
            this.maxRetryForNet = maxRetry;
            this.priority = priority;

            if(++currentInterPriority >= INTER_PRIORITY_UPPER_BOUND){
                currentInterPriority = 1;
            }

            this.interPriority = INTER_PRIORITY_UPPER_BOUND - currentInterPriority;

            this.targetSize = targetSize;

            this.mobileNetType = mobileNetType;
        }

        public DownloadConfiguration(String downloadUrl, String downloadedFilePath,
                String downloadedFileName,
                FileDownloadNotificationConfiguration fileDownloadNotification,
                boolean allowedDownloadNotUnderWifi, boolean forceToResume,
                boolean isSupportResume, Serializable customObj) {

            this(downloadUrl, downloadedFilePath, downloadedFileName, fileDownloadNotification, allowedDownloadNotUnderWifi, forceToResume, isSupportResume, customObj, DEFAULT_MAX_RETRY_FOR_NET, 0, 0, 0);

        }

        public DownloadConfiguration(String downloadUrl, String downloadedFilePath,
                                     String downloadedFileName,
                                     FileDownloadNotificationConfiguration fileDownloadNotification,
                                     boolean allowedDownloadNotUnderWifi, boolean forceToResume,
                                     boolean isSupportResume, Serializable customObj, int maxRetry, int priority) {
            this(downloadUrl, downloadedFilePath, downloadedFileName, fileDownloadNotification, allowedDownloadNotUnderWifi, forceToResume, isSupportResume, customObj, maxRetry, priority, 0, 0);
        }

        /**
         * 下载地址
         */
        public String downloadUrl;

        /**
         * 下载路径（不包含文件名）
         */
        public String downloadedFilePath;

        /**
         * TODO KANGLE fix this
         * 下载文件名（不包含路径）
         */
        public String downloadedFileName;

        /**
         * 支持自定义通知栏
         */
        public FileDownloadNotificationConfiguration fileDownloadNotification;

        /**
         * 允许在非Wifi环境下下载
         */
        public boolean allowedDownloadNotUnderWifi = false;

        /**
         * 强制开始，用于add的时候，忽略该下载已经在下载队列中的事实，开始下载
         */
        public boolean forceToResume;

        /**
         * TODO KANGLE fix this
         * 是否支持续下（如果不支持，不会持久化，下载信息以内存为主）
         * 当将该值设置为true的话，下载的 downloadUrl 对应的资源必须要保证不变的，指定的下载位置 + 文件名必须是唯一的（假如同时制定的话）
         * 并且必须Override getType() 方法来指定具体type，用来从持久化状态恢复到对应的FileDownloadInterface
         */
        public boolean isSupportResume = false;

        /**
         * 下载类型
         */
        private String type;

        /**
         * 当网络切换成可下载状态（非wifi下允许 or wifi）,会为由于网络导致下载失败的任务进行重试；该值用于设置重试的最大次数，默认为4次
         */
        protected int maxRetryForNet = DEFAULT_MAX_RETRY_FOR_NET;

        /**
         * @return 下载任务的唯一标示，默认为下载的url，子类可以通过复写改变
         */
        public String getId() {
            return downloadUrl;
        }

        /**
         * @return 下载类型的唯一标示，子类可以通过复写改变（提供多个业务线的下载）
         *  WARN： 这里的type必须和 FileDownloadInterface(FileDownloadCallback fileDownloadCallback, String type)  
         *  构造方法中的type保持一致，这是为了保证FileDownloadInterface只监听此类型的下载（默认监听自己add进去的，指定type之后则会以type为主）
         */
        public String getType() {
            return type;
        }


        /**
         * 当用户未复写getType()的时候，提供一次性的默认值
         * @param type
         */
        protected void setType(String type) {
            if(this.type == null){
                this.type = type;
            }
        }

        /**
         *  相同类型的下载最大值应该保持一致
         * @return 该类型允许同时下载的最大值，子类可以通过复写改变（提供多个业务线的下载）
         */
        public int getMaxLoad() {
            return FileDownloadConstant.MAX_LOAD;
        }

        /**
         * check 下载配置信息是否正确
         *
         * @return
         */
        protected boolean isValid() {
            return !StringUtils.isEmpty(downloadUrl);
        }

        /**
         * 下载调用方 想使用的自定义数据对象（必须支持persistent）
         */
        public Serializable customObj;

        @Override
        public String toString() {
            return "DownloadConfiguration [downloadUrl=" + downloadUrl + ", downloadedFilePath="
                    + downloadedFilePath + ", downloadedFileName=" + downloadedFileName
                    + ", fileDownloadNotification=" + fileDownloadNotification
                    + ", allowedDownloadNotUnderWifi=" + allowedDownloadNotUnderWifi
                    + ", forceToResume=" + forceToResume + ", isSupportResume=" + isSupportResume
                    + ", customObj=" + customObj + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DownloadConfiguration) {
                DownloadConfiguration configuration = (DownloadConfiguration) o;
                if (configuration.getId().equals(this.getId())) {
                    return true;
                }
            }
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return getId().hashCode();
        }
    }

    public interface IOnCompleted {
        public void onCompleted(Context context, FileDownloadStatus fileDownloadStatus);
    }

    public String getDownloadedFileAbsolutePath(Context context) {
        if (StringUtils.isEmpty(downloadedFileAbsolutePath)) {

            File parentDirectory;

            // 用户没有指定下载路径 or 指定了无效的下载路径
            if (!checkDownloadDirectory()) {
                parentDirectory = FileDownloadTools.getDownloadPath(context);
            }else{
                parentDirectory = new File(mDownloadConfiguration.downloadedFilePath);
            }

            downloadedFileAbsolutePath =
                    new File(parentDirectory,
                            StringUtils.isEmpty(mDownloadConfiguration.downloadedFileName)
                                    ? FileDownloadTools.hashKeyForDisk(mDownloadConfiguration
                                            .getId()) : mDownloadConfiguration.downloadedFileName)
                            .getAbsolutePath();
        }
        return downloadedFileAbsolutePath;
    }

    /**
     * @return 目前的下载路径是否有效
     */
    private boolean checkDownloadDirectory() {

        if(StringUtils.isEmpty(mDownloadConfiguration.downloadedFilePath)){
            return false;
        }

        File parentDirectory = new File(mDownloadConfiguration.downloadedFilePath);

        if(!parentDirectory.exists()){
            parentDirectory.mkdirs();
        }

        if(parentDirectory.isDirectory() && parentDirectory.canWrite()){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int) for IPC
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeLong(bytes_downloaded_so_far);
        dest.writeLong(total_size_bytes);

        dest.writeInt(status);
        dest.writeInt(reason);

        dest.writeSerializable(mDownloadConfiguration);

        dest.writeString(realDownloadUrl);
        dest.writeString(downloadedFileAbsolutePath);
    }

    public static final Parcelable.Creator<FileDownloadStatus> CREATOR =
            new Parcelable.Creator<FileDownloadStatus>() {
                public FileDownloadStatus createFromParcel(Parcel in) {
                    return new FileDownloadStatus(in);
                }

                public FileDownloadStatus[] newArray(int size) {
                    return new FileDownloadStatus[size];
                }
            };

    @Override
    public boolean equals(Object o) {
        if (o instanceof FileDownloadStatus) {
            FileDownloadStatus downloadStatus = (FileDownloadStatus) o;
            return downloadStatus.mDownloadConfiguration.equals(this.mDownloadConfiguration);
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return mDownloadConfiguration.hashCode();
    }

    /**
     *
     * @return
     */
    protected FileDownloadNotification getDownloadNotification(Context context) {
        if (mDownloadConfiguration.fileDownloadNotification != null) {
            return new FileDownloadNotification(context, mDownloadConfiguration.fileDownloadNotification);
        }
        return null;
    }

    /**
     * @param networkStatus
     * @return 当前网络环境下，能否下载
     */
    protected Boolean canDownload(NetworkStatus networkStatus) {

        boolean ret = false;

        if (networkStatus != NetworkStatus.OFF) {
            if (networkStatus == NetworkStatus.WIFI) {
                ret = true;
            } else if (mDownloadConfiguration.allowedDownloadNotUnderWifi) {
                switch (mDownloadConfiguration.mobileNetType) {
                    case MOBILE_4G:
                        ret = networkStatus == NetworkStatus.MOBILE_4G;
                        break;
                    case MOBILE_4G_OR_3G:
                        ret = (networkStatus == NetworkStatus.MOBILE_4G
                                || networkStatus == NetworkStatus.MOBILE_3G);
                        break;
                    default:
                        ret = true;
                }
            }
        }

        return ret;
    }

    protected String getDownloadUrl() {
        return realDownloadUrl;
    }

    protected void setDownloadUrl(String downloadUrl) {
        this.realDownloadUrl = downloadUrl;
    }

    protected int getIdAsInteger() {
        return mDownloadConfiguration.getId().hashCode();
    }

    public String getId() {
        return mDownloadConfiguration.getId();
    }

    @Override
    public String toString() {
        return "FileDownloadStatus [bytes_downloaded_so_far=" + bytes_downloaded_so_far
                + ", total_size_bytes=" + total_size_bytes + ", status=" + status + ", reason="
                + reason + ", mDownloadConfiguration=" + mDownloadConfiguration
                + ", realDownloadUrl=" + realDownloadUrl + ", downloadedFileAbsolutePath="
                + downloadedFileAbsolutePath + "]";
    }

    protected boolean needPersistant() {
        return mDownloadConfiguration.isSupportResume;
    }

}
