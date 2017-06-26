package org.qiyi.video.module.download.exbean;

/**
 * <pre>
 * 任务数据的接口。
 * User: jasontujun
 * Date: 13-9-28
 * Time: 下午7:15
 * </pre>
 */
public interface XTaskBean extends Cloneable {

    // ============ 任务的状态值 ============ //
    public static final int STATUS_DEFAULT = -1;// 默认状态(未进执行队列)
    public static final int STATUS_TODO = 0;// 未执行状态
    public static final int STATUS_DOING = 1;// 正在执行状态
    public static final int STATUS_DONE = 2;// 已完成状态
    public static final int STATUS_ERROR = 3;// 错误状态
    public static final int STATUS_STARTING = 4;// 正在启动状态
    public static final int STATUS_PAUSING = 5;// 正在暂停状态

    public static final int DOWNLOAD_TYPE_VIDEO = 1;//下载类型是视频
    public static final int DOWNLOAD_TYPE_GAME = 2;//下载类型是游戏或者应用
    public static final int DOWNLOAD_TYPE_FILE = 3;//下载任意文件

    /**
     * 获取下载任务的唯一Id，用于区分不同的下载任务。
     *
     * @return 返回该下载任务的唯一Id
     */
    String getId();

    /**
     * 获取任务类型
     *
     * @return 返回任务所属的类型值
     */
    int getDownWay();

    /**
     * 获取下载对象类型，主要包括视频下载和游戏下载
     *
     * @return
     */
    int getType();

    /**
     * 获取当前状态。
     *
     * @return 返回状态值
     * @see #STATUS_DEFAULT
     * @see #STATUS_ERROR
     * @see #STATUS_TODO
     * @see #STATUS_DOING
     * @see #STATUS_DONE
     * @see #STATUS_PAUSING
     * @see #STATUS_STARTING
     */
    int getStatus();

    /**
     * 设置当前状态
     *
     * @param status 状态值
     * @see #STATUS_DEFAULT
     * @see #STATUS_ERROR
     * @see #STATUS_TODO
     * @see #STATUS_DOING
     * @see #STATUS_DONE
     * @see #STATUS_PAUSING
     * @see #STATUS_STARTING
     */
    void setStatus(int status);

    /**
     * 用于克隆一份XTaskBean对象数据
     *
     * @return
     * @throws CloneNotSupportedException
     */
    public Object clone() throws CloneNotSupportedException;

    /**
     * 拿到是否需要删除的标识
     *
     * @return 0：不用删除
     * 1：需要删除
     */
    public int getNeeddel();

    /**
     * 拿到下载任务的存储路径目录
     *
     * @return
     */
    public String getSaveDir();

    /**
     * 判断出错了的任务，下载载入内存时，是否恢复TODO
     *
     * @return
     */
    public boolean recoverToDoStatus();

    /**
     * 当任务下载出错时，是否自动执行下一集下载
     *
     * @return
     */
    public boolean autoNextTaskWhenError();

    /**
     * 获取已经下载的大小
     * <p>
     * 可废弃
     *
     * @return
     */
    public long getCompleteSize();

    /**
     * 下载此任务时，是否需要将Service 设置为foreground
     *
     * @return
     */
    public boolean isNeedForeground();


    /**
     * 设置错误码
     *
     * @param errorCode
     */
    public void setErrorCode(String errorCode);

    /**
     * 是否允许在蜂窝下载下载
     *
     * @return
     */
    public boolean isAllowInMobile();

    /**
     * 获取调度器
     *
     * @return
     */
    public ScheduleBean getScheduleBean();


    public String getDownloadUrl();

    public void setDownloadUrl(String url);

    public String getDownloadPath();

    public long getFileSzie();

    public void setFileSize(long fileSize);

    public void setCompleteSize(long completeSize);

    public String getFileName();

    public long getSpeed();

    public void setSpeed(long speed);

    public String getDownloadingPath();

}
