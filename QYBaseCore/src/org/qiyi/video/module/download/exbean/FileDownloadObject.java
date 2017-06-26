package org.qiyi.video.module.download.exbean;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.iqiyi.video.download.filedownload.FileDownloadConstant;

import org.qiyi.basecore.utils.ExceptionUtils;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

/**
 * 使用Builder方式构建文件下载对象
 */
public class FileDownloadObject implements XTaskBean, Parcelable, Serializable {

    private static final long serialVersionUID = 3049653229296884931L;

    //下载基础信息配置
    private String fileId;//下载url，一个任务唯一id
    private String fileName;//保存文件名
    private String filePath;//文件绝对路径

    //下载任务属性
    private int taskStatus;//任务状态
    private String downloadUrl;//下载地址

    //下载配置
    private DownloadStatus status;//下载状态
    private ScheduleBean scheduleBean;//调度实例，用于计算优先级
    public DownloadConfig mDownloadConfig;//下载配置

    //下载过程变量
    public long completeSize = -1L;//下载完成大小
    public long totalSize = -1L;//文件总大小
    public long speed;//下载速度
    public String errorCode;//错误码

    public void updateDownloadConfig(FileDownloadObject bean){
        mDownloadConfig.allowedInMobile = bean.getDownloadConfig().allowedInMobile;
        mDownloadConfig.priority = bean.getDownloadConfig().priority;
        mDownloadConfig.groupPriority = bean.getDownloadConfig().groupPriority;
        mDownloadConfig.groupName = bean.getDownloadConfig().groupName;
    }

    private FileDownloadObject(FileDownloadObject.Builder builder) {

        this.fileId = builder.mFileId;
        this.fileName = builder.mFileName;
        this.filePath = builder.mFilePath;
        this.mDownloadConfig = builder.mDownloadConfig;
        this.scheduleBean = new ScheduleBean();
    }


    /**
     * 1、默认推荐方式，构建下载文件对象，构建复杂对象
     */
    public static class Builder {

        private String mFileId;//下载地址，文件下载唯一id
        private String mFileName;//文件名字
        private String mFilePath;//文件绝对路径

        private DownloadConfig mDownloadConfig;//下载配置

        public Builder() {

            this.mDownloadConfig = new DownloadConfig();

        }


        /**
         * 设置下载url
         *
         * @param url
         * @return
         */
        public Builder url(String url) {

            this.mFileId = url;
            return this;
        }

        /**
         * 设置文件名
         *
         * @param filename
         * @return
         */
        public Builder filename(String filename) {

            this.mFileName = filename;
            return this;
        }

        /**
         * 设置下载绝对路径
         *
         * @param filepath
         * @return
         */
        public Builder filepath(String filepath) {

            this.mFilePath = filepath;
            return this;
        }

        /**
         * 配置下载方式
         *
         * @param downloadWay
         * @return
         */
        public Builder downloadWay(int downloadWay) {

            if (mDownloadConfig != null) {

                mDownloadConfig.downloadWay = downloadWay;

            }

            return this;

        }

        /**
         * 设置文件下载业务类型
         *
         * @param bizType
         * @return
         */
        public Builder bizType(int bizType) {

            if (mDownloadConfig != null) {

                mDownloadConfig.type = bizType;

            }

            return this;
        }


        /**
         * 设置文件任务组名
         *
         * @param groupName
         * @return
         */
        public Builder groupName(String groupName) {

            if (mDownloadConfig != null) {

                mDownloadConfig.groupName = groupName;

            }
            return this;
        }

        /**
         * 设置文件任务组优先级
         *
         * @param groupPriority
         * @return
         */
        public Builder groupPriority(int groupPriority) {

            if (mDownloadConfig != null) {

                mDownloadConfig.groupPriority = groupPriority;

            }

            return this;
        }

        /**
         * 设置文件下载优先级
         *
         * @param priority
         * @return
         */
        public Builder priority(int priority) {

            if (mDownloadConfig != null) {
                mDownloadConfig.priority = priority;
            }

            return this;

        }

        /**
         * 设置校验配置
         *
         * @param supportVerify 是否需要校验
         * @param verifyWay     校验方式
         * @param verifySign    校验串
         * @return
         */
        public Builder verify(boolean supportVerify, int verifyWay, String verifySign) {
            if (mDownloadConfig != null) {
                mDownloadConfig.needVerify = supportVerify;
                mDownloadConfig.verifyWay = verifyWay;
                mDownloadConfig.verifySign = verifySign;
            }
            return this;
        }


        public Builder allowedInMobile(boolean allowedInMobile) {

            if (mDownloadConfig != null) {
                mDownloadConfig.allowedInMobile = allowedInMobile;
            }
            return this;
        }

        public Builder supportJumpQueue(boolean supportJumpQueue) {
            if (mDownloadConfig != null) {
                mDownloadConfig.supportJumpQueue = supportJumpQueue;
            }
            return this;
        }

        public Builder supportResume(boolean supportResume) {
            if (mDownloadConfig != null) {
                mDownloadConfig.needResume = supportResume;
            }
            return this;
        }

        public Builder silentDownload(boolean silentDownload) {
            if (mDownloadConfig != null) {
                mDownloadConfig.slientDownload = silentDownload;
            }
            return this;
        }

        public Builder supportDB(boolean supportDB) {
            if (mDownloadConfig != null) {
                mDownloadConfig.needDb = supportDB;
            }
            return this;
        }

        public Builder customObject(Serializable customObject){

            if(mDownloadConfig != null){
                mDownloadConfig.customObject = customObject;
            }

            return this;
        }

        public Builder supportUnzip(boolean supportUnzip){

            if(mDownloadConfig != null) {
                mDownloadConfig.supportUnzip = supportUnzip;
            }

            return this;

        }

        public Builder maxRetryTimes(int maxRetryTimes){

            if(mDownloadConfig != null) {
                mDownloadConfig.maxRetryTimes = maxRetryTimes;
            }

            return this;
        }

        public FileDownloadObject build() {

            return new FileDownloadObject(this);

        }


    }


    /**
     * 2、构建文件下载对象，简单对象
     *
     * @param url      下载地址
     * @param fileName 保存文件名
     * @param filePath 保存绝对路径
     */
    public FileDownloadObject(String url, String fileName, String filePath) {

        this.fileId = url;
        this.fileName = fileName;
        this.filePath = filePath;
        this.mDownloadConfig = new DownloadConfig();
        this.scheduleBean = new ScheduleBean();

    }


    /**
     * 3、构建文件下载对象，简单对象
     *
     * @param url 下载地址
     */
    public FileDownloadObject(String url) {

        this.fileId = url;
        this.mDownloadConfig = new DownloadConfig();
        this.scheduleBean = new ScheduleBean();

    }


    //////////////////////////序列化///////////////////////////////////////////
    public FileDownloadObject(Parcel in) {
        this.fileName = in.readString();
        this.fileId = in.readString();
        this.filePath = in.readString();
        this.completeSize = in.readLong();
        this.totalSize = in.readLong();
        this.status = (DownloadStatus) in.readSerializable();
        this.taskStatus = in.readInt();
        this.speed = in.readLong();
        this.downloadUrl = in.readString();
        this.errorCode = in.readString();
        this.mDownloadConfig = (DownloadConfig) in.readSerializable();
        this.scheduleBean = (ScheduleBean) in.readSerializable();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.fileName);
        dest.writeString(this.fileId);
        dest.writeString(this.filePath);
        dest.writeLong(this.completeSize);
        dest.writeLong(this.totalSize);
        dest.writeSerializable(this.status);
        dest.writeInt(this.taskStatus);
        dest.writeLong(this.speed);
        dest.writeString(this.downloadUrl);
        dest.writeString(this.errorCode);
        dest.writeSerializable(this.mDownloadConfig);
        dest.writeSerializable(this.scheduleBean);
    }

    public static final Creator<FileDownloadObject> CREATOR = new Creator() {
        public FileDownloadObject createFromParcel(Parcel in) {
            return new FileDownloadObject(in);
        }

        public FileDownloadObject[] newArray(int size) {
            return new FileDownloadObject[size];
        }
    };

    public int describeContents() {
        return 0;
    }
//////////////////////////序列化///////////////////////////////////////////


    @Override
    public String getFileName() {

        if (TextUtils.isEmpty(fileName)) {

            if (!TextUtils.isEmpty(filePath)) {

                int lastIndex = filePath.lastIndexOf("/");

                if (lastIndex != -1) {
                    fileName = filePath.substring(lastIndex + 1);
                } else {
                    fileName = "unknown";
                }
            } else {
                fileName = "unknown";
            }
        }

        return fileName;
    }


    /**
     * 设置下载路径
     *
     * @param absPath
     */
    public void setDownloadPath(String absPath) {

        this.filePath = absPath;

    }


    @Override
    public String getDownloadPath() {

        return filePath;

    }


    /**
     * 设置下载地址
     *
     * @param url
     */
    public void setDownloadUrl(String url) {

        this.downloadUrl = url;

    }

    /**
     * 获取下载地址
     *
     * @return
     */
    public String getDownloadUrl() {

        if (TextUtils.isEmpty(downloadUrl)) {
            downloadUrl = fileId;
        }
        return downloadUrl;

    }

    /**
     * 获取错误码
     *
     * @return
     */
    public String getErrorCode() {

        return this.errorCode;

    }

    /**
     * 设置错误码
     *
     * @param errorCode
     */
    public void setErrorCode(String errorCode) {

        this.errorCode = errorCode;

    }

    /**
     * 获取文件总大小
     *
     * @return
     */
    @Override
    public long getFileSzie() {

        return totalSize;

    }

    /**
     * 设置文件总大小
     *
     * @param fileSize
     */
    @Override
    public void setFileSize(long fileSize) {

        this.totalSize = fileSize;

    }

    /**
     * 设置已下载大小
     *
     * @param completeSize
     */
    @Override
    public void setCompleteSize(long completeSize) {

        this.completeSize = completeSize;
    }

    /**
     * 获取已下载大小
     *
     * @return
     */
    public long getCompleteSize() {
        return this.completeSize;
    }

    /**
     * 获取速度
     *
     * @return
     */
    @Override
    public long getSpeed() {
        return speed;
    }

    /**
     * 设置速度
     *
     * @param speed
     */
    @Override
    public void setSpeed(long speed) {
        this.speed = speed;
    }

    /**
     * 获取下载中的文件路径
     *
     * @return
     */
    @Override
    public String getDownloadingPath() {
        return filePath + ".cdf";
    }

    /**
     * 获取文件的父目录
     *
     * @return
     */
    public String getSaveDir() {

        if (this.filePath != null) {
            try {
                String mPath = new File(filePath).getParent();
                return mPath;
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }
        }
        return null;
    }


    /**
     * 获取最大重试次数
     *
     * @return
     */
    public int getMaxRetryTimes() {

        return getDownloadConfig().maxRetryTimes;

    }

    /**
     * 获取任务组名
     *
     * @return
     */
    public String getGroupName() {

        return getDownloadConfig().groupName;

    }

    /**
     * 设置任务组名
     *
     * @param groupName
     */
    public void setGroupName(String groupName) {

        getDownloadConfig().groupName = groupName;

    }


    /**
     * 设置组优先级
     *
     * @param groupPriority
     */
    public void setGroupPriority(int groupPriority) {

        getDownloadConfig().groupPriority = groupPriority;

    }


    /**
     * 获取下载百分比
     *
     * @return
     */
    public float getDownloadPercent() {
        float ret = 0.0F;

        if (this.totalSize == 0) {
            return ret;
        }

        if (this.completeSize != -1L && this.totalSize != -1) {
            ret = (float) this.completeSize / (float) this.totalSize * 100.0F;
        }
        return ret;
    }


    @Override
    public boolean isAllowInMobile() {

        return getDownloadConfig().allowedInMobile;

    }

    /**
     * 获取优先级
     *
     * @return
     */
    public int getPrority() {

        int priority = getDownloadConfig().priority;

        if (priority < DownloadConfig.MIN_PRIORITY) {
            priority = DownloadConfig.MIN_PRIORITY;
        } else if (priority > DownloadConfig.MAX_PRIORITY) {
            priority = DownloadConfig.MAX_PRIORITY;
        }
        return priority;
    }

    /**
     * 获取组优先级
     *
     * @return
     */
    public int getGroupPriority() {

        int groupPriority = getDownloadConfig().groupPriority;

        if (groupPriority < DownloadConfig.MIN_PRIORITY) {
            groupPriority = DownloadConfig.MIN_PRIORITY;
        } else if (groupPriority > DownloadConfig.MAX_PRIORITY) {
            groupPriority = DownloadConfig.MAX_PRIORITY;
        }

        return groupPriority;

    }


    /**
     * 获取下载配置
     *
     * @return
     */
    public DownloadConfig getDownloadConfig() {

        if (mDownloadConfig == null) {

            mDownloadConfig = new DownloadConfig();

        }

        return mDownloadConfig;

    }

    /**
     * 获取调度优先级
     *
     * @return
     */
    public ScheduleBean getScheduleBean() {

        if (scheduleBean != null) {

            scheduleBean.prority = getPrority();

            scheduleBean.groupPriority = getGroupPriority();

        } else{
            scheduleBean = new ScheduleBean();
        }

        return scheduleBean;

    }

    /**
     * 获取下载ID,一般为url
     *
     * @return
     */
    public String getId() {

        return this.fileId;

    }


    /**
     * 获取下载方式
     *
     * @return
     */
    public int getDownWay() {

        return getDownloadConfig().downloadWay;

    }

    /**
     * 获取任务状态
     *
     * @return
     */
    public int getStatus() {

        return this.taskStatus;

    }

    /**
     * 获取当前下载状态
     *
     * @return
     * @see DownloadStatus
     */
    public DownloadStatus getDownloadStatus() {

        return this.status;

    }

    /**
     * 设置任务状态
     *
     * @param taskStatus
     */
    public void setStatus(int taskStatus) {
        this.taskStatus = taskStatus;
        switch (taskStatus) {
            case -1:
                this.status = DownloadStatus.WAITING;
                break;
            case 0:
                this.status = DownloadStatus.DEFAULT;
                break;
            case 1:
                this.status = DownloadStatus.DOWNLOADING;
                break;
            case 2:
                this.status = DownloadStatus.FINISHED;
                break;
            case 3:
                this.status = DownloadStatus.FAILED;
                break;
            case 4:
                this.status = DownloadStatus.STARTING;
                break;
            case 5:
                this.status = DownloadStatus.PAUSING;
        }

    }

    @Override
    public String toString() {
        return "FileDownloadObject{" +
                "fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", completeSize=" + completeSize +
                ", totalSize=" + totalSize +
                ", status=" + status +
                ", errorCode='" + errorCode + '\'' +
                ", speed=" + speed +
                ", taskStatus=" + taskStatus +
                ", mDownloadConfig=" + mDownloadConfig +
                '}';
    }

    public Object clone() throws CloneNotSupportedException {

        FileDownloadObject bean = null;

        try {
            bean = (FileDownloadObject) super.clone();
        } catch (CloneNotSupportedException var3) {
            bean = this;
        }

        return bean;
    }

    @Override
    public int hashCode() {
        return fileId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public static class DownloadConfig implements Serializable {

        private static final long serialVersionUID = 6878404786225862911L;

        public static final int MAX_PRIORITY = 10;//最大优先级

        public static final int MIN_PRIORITY = 0;//最小优先级

        public int type = FileBizType.BIZ_TYPE_GENERAL;//业务类型
        public int downloadWay = FileDownloadConstant.DOWNLOAD_WAY_FILE_CDN;//下载方式

        public String groupName = "defaultGroup";//任务组名
        public int groupPriority = 0;//组与组之间的优先级
        public int priority = 0;//组内优先级
        public int maxRetryTimes = -1;//最大重试次数

        public boolean slientDownload = true;//默认情况为静默下载,静默下载即没有通知栏
        public boolean needResume = true;//是否需要断点续传
        public boolean needDb = false;//是否需要保存数据库
        public boolean allowedInMobile = false;//是否需要在蜂窝网络下载
        public boolean supportJumpQueue = false;//是否支持插队，默认为false

        //校验配置
        public boolean needVerify = false;//是否需要校验
        public int verifyWay;//校验方式
        public String verifySign;//校验串

        public boolean supportUnzip = false;//是否支持解压

        public Serializable customObject;

        public HashMap<String, Object> hashMap = new HashMap<String, Object>();//传递数据

        public DownloadConfig() {

        }

        /**
         * @param type 业务类型
         */
        public DownloadConfig(int type) {
            this.type = type;
        }


        public void setCustomObject(Serializable customObject) {
            this.customObject = customObject;
        }

        @Override
        public String toString() {
            return "DownloadConfig{" +
                    "type=" + type +
                    ", priority=" + priority +
                    ", needDb=" + needDb +
                    ", needResume=" + needResume +
                    ", allowedInMobile=" + allowedInMobile +
                    ", needVerify=" + needVerify +
                    ", customObject=" + customObject +
                    ", hashMap=" + hashMap +
                    '}';
        }
    }


    public boolean recoverToDoStatus() {
        return true;
    }

    public boolean autoNextTaskWhenError() {
        return true;
    }

    public boolean isNeedForeground() {
        return false;
    }

    public int getNeeddel() {
        return 0;
    }

    @Deprecated
    public int getType() {
        return DOWNLOAD_TYPE_FILE;
    }


}
