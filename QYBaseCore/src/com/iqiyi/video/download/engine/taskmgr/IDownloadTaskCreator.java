package com.iqiyi.video.download.engine.taskmgr;

import com.iqiyi.video.download.filedownload.TaskBean;
import com.iqiyi.video.download.engine.task.XBaseTaskExecutor;

import org.qiyi.video.module.download.exbean.XTaskBean;

/**
 * 用于为下载管理器提供创建下载任务接口
 * 1、根据下载对象id创建XBaseTaskExecutor
 * 2、根据下载对象id创建TaskBean对象
 * Created by yuanzeyao on 2015/6/4.
 */
public interface IDownloadTaskCreator<B extends XTaskBean> {

    XBaseTaskExecutor<B> createDownloadTask(String taskId);

    TaskBean<B> createTaskBean(String taskId);

}
