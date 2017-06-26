package com.iqiyi.video.download.filedownload.ipc;

/**
 * Created by songguobin on 2017/3/17.
 */

public class FileDownloadAction {

    /**
     * 添加任务
     */
    public static final int ACTION_DOWNLOAD_ADD_TASK = 1000;

    /**
     * 添加任务，带callback回调
     */
    public static final int ACTION_DOWNLOAD_ADD_TASK_WITH_CALLBACK = 1001;

    /**
     * 开始或暂停任务
     */
    public static final int ACTION_DOWNLOAD_OPERATE_TASK = 1002;


    /**
     * 删除任务
     */
    public static final int ACTION_DOWNLOAD_DEL_TASK = 1003;


    /**
     * 删除callback
     */
    public static final int ACTION_DOWNLOAD_REMOVE_CALLBACK = 1004;


    /**
     * 通过groupName删除指定任务队列
     */
    public  static final int ACTION_DOWNLOAD_CANCEL_TASKS_WITH_GROUP_NAME = 1005;


    /**
     * 通过url寻找匹配的任务，暂停或开始任务
     */
    public static final int ACTION_DOWNLOAD_OPERATE_TASK_BY_ID = 1006;


    ///////////////////回调消息////////////////////////////////////////////
    /**
     * 回调状态信息给各业务方
     */
    public static final int ACTION_DOWNLOAD_FILE_CALLBACK_DATA_STATUS = 2000;



}
