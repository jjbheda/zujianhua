package com.iqiyi.video.download.filedownload;

import android.content.Context;
import android.text.TextUtils;

import com.iqiyi.video.download.engine.downloader.IQiyiDownloader;
import com.iqiyi.video.download.engine.downloader.IQiyiDownloaderListener;
import com.iqiyi.video.download.filedownload.ipc.RemoteMessageProcesser;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.NetWorkTypeUtils;
import org.qiyi.basecore.utils.NetworkStatus;
import org.qiyi.video.module.download.exbean.FileDownloadObject;
import org.qiyi.video.module.download.exbean.XTaskBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songguobin on 2016/11/23.
 *
 *  文件下载控制器
 */
public class FileDownloadController {


    public static final String TAG = "FileDownloadController";

    private boolean isInit = false;

    protected Context mContext;
    // 下载任务的集合，是Service中下载数据的副本
    protected List<FileDownloadObject> mDownloadList;
    // 下载管理器
    protected IQiyiDownloader<FileDownloadObject> mDownloader;
    //下载管理器回调接口
    protected IQiyiDownloaderListener<FileDownloadObject> mDownloaderListener;


    public FileDownloadController(IQiyiDownloader<FileDownloadObject> mFileDownloader, Context mContext) {

        this.mContext = mContext;
        this.mDownloader = mFileDownloader;
        this.mDownloadList = new ArrayList<FileDownloadObject>();

    }


    public void init() {

        if (isInit) {
            DebugLog.log(TAG, "FileDownloadController-->already inited");
            return;
        }

        isInit = true;

        DebugLog.log(TAG, "FileDownloadController-->init");

        if (mDownloader != null) {

            DebugLog.log(TAG, "FileDownloadController-->load");

            this.mDownloaderListener = new InnerListener();
            //注册监听器
            this.mDownloader.registerListener(mDownloaderListener);
            //加载数据
            this.mDownloader.load(false);

        }
    }



    /**
     * 获取文件下载列表
     *
     * @return
     */
    public List<FileDownloadObject> getAllFileList() {

        return mDownloader.getAllDownloadTask();

    }



    /**
     * 开始或者暂停一个下载任务，如果当前任务是正在下载或者正在启动中，那么暂停
     * 其他状态则开始下载
     *
     * @param _dObj
     */
    public void startOrPauseDownloadTask(FileDownloadObject _dObj) {
        DebugLog.log(TAG, "startOrPauseDownloadTask");
        if (_dObj == null) {
            DebugLog.log(TAG, "startOrPauseDownloadTask>>FileDownloadObject==null,return");
            return;
        }
        if (_dObj.getStatus() == XTaskBean.STATUS_DOING ||
                _dObj.getStatus() == XTaskBean.STATUS_STARTING) {
            // 取消下载(对"启动中"和"缓存中"的任务)
            if (mDownloader != null) {
                mDownloader.stopDownload(_dObj.getId());
                DebugLog.log(TAG, "stopDownload = " + _dObj.getId());
            }
        } else {
            // 开始下载
            DebugLog.log(TAG, "checkAndDownloadFile = " + _dObj.getId());
            checkAndDownloadFile(_dObj);
        }
    }



    /**
     * 通过url找到filedownloadobject，暂停或开始任务
     *
     * @param url
     */
    public void startOrPauseDownloadTask(String url){

        FileDownloadObject bean = findFileDownloadObjectById(url);

        startOrPauseDownloadTask(bean);

    }


    public void checkAndDownloadFile(FileDownloadObject bean) {

        DebugLog.log(TAG, "checkAndDownloadFile");

        if (mDownloader == null) {
            return;
        }

        NetworkStatus networkStatus = NetWorkTypeUtils.getNetworkStatus(mContext);

        DebugLog.log(TAG, "checkAndDownloadFile>>NetworkStatus = " + networkStatus);

        if (bean == null) {
            //自动下载
            if (networkStatus == NetworkStatus.OFF) {
                DebugLog.log(TAG, "checkAndDownloadFile>>network off,can not auto download file");
                return;
            }

            mDownloader.startDownload();

        } else {
            //手动下载
            if (networkStatus == NetworkStatus.WIFI) {
                //wifi网络
                DebugLog.log(TAG,"checkAndDownloadFile>>network wifi");
                mDownloader.startDownload(bean.getId());
            } else if (networkStatus != NetworkStatus.WIFI && networkStatus != NetworkStatus.OFF) {
                //4G网络
                DebugLog.log(TAG,"checkAndDownloadFile>>network 4G >>isAllowInMobile = " + bean.isAllowInMobile());
                if(bean.isAllowInMobile()){
                    mDownloader.startDownload(bean.getId());
                }
            } else{
                //无网络
                DebugLog.log(TAG,"checkAndDownloadFile>>network off,stop download");
            }
        }

    }

    /**
     * 文件下载-开始入口
     *
     * @param bean
     */
    public void autoStartDownloadFileTask(FileDownloadObject bean) {

        DebugLog.log(FileDownloadController.TAG, "autoStartDownloadFile");

        if (!hasRunningTask()) {
            checkAndDownloadFile(bean);
        } else {
            DebugLog.log(FileDownloadController.TAG, "autoStartDownloadFile>>hasRunningTask");
        }

    }

    /**
     * 判断当前是否有任务正在下载
     *
     * @return 如果有任务正在下载，那么返回true,如果没有任务下载则返回false;
     */
    public boolean hasRunningTask() {
        if (mDownloader != null) {
            return mDownloader.hasTaskRunning();
        }
        return false;
    }

    public FileDownloadObject findFileDownloadObjectById(String url){

        if(!TextUtils.isEmpty(url)){
            for(FileDownloadObject bean : mDownloadList){
                if(bean.getId().equals(url)){
                    return bean;
                }
            }
        }
        return null;

    }


    ////////////////////////////////////////////////////////////////////////////

    /**
     * 添加一个文件下载任务
     *
     * @param mFileObject
     */
    public void addDownloadTask(FileDownloadObject mFileObject) {
        DebugLog.log(TAG, "addDownloadTask");
        if (mFileObject == null) {
            DebugLog.log(TAG, "addDownloadTask-->mFileObject is null!");
            return;
        }
        DebugLog.log(TAG, "addDownloadTask-->mFileObject:" + mFileObject);

        if (mDownloader == null) {
            DebugLog.log(TAG, "addDownloadTask-->mFileDownloader is null!");
            return;
        }
        ArrayList<FileDownloadObject> mApkList = new ArrayList<FileDownloadObject>();
        mApkList.add(mFileObject);
        if (mDownloader.addDownloadTasks(mApkList)) {
            DebugLog.log(TAG, "addDownloadTask-->add DownloadTask Success!!");
        }
    }


    /**
     * 批量添加文件下载任务
     *
     * @param mDownloadFileList
     */
    public void addDownloadTask(List<FileDownloadObject> mDownloadFileList) {
        DebugLog.log(TAG, "addDownloadTasks-->");
        if (mDownloadFileList == null || mDownloadFileList.size() == 0) {
            DebugLog.log(TAG, "addDownloadTasks-->mDownloadFileList is null or size==0!!");
            return;
        }

        if (mDownloader == null) {
            DebugLog.log(TAG, "addDownloadTasks-->mFileDownloader is null!!");
            return;
        }

        if (mDownloader.addDownloadTasks(mDownloadFileList)) {
            DebugLog.log(TAG, "addDownloadTasks-->add DownloadTask Success!!");
        }
    }

    /**
     * 跟新数据库中某些字段的属性，主要在apk升级中使用
     *
     * @param mDownloadFileList
     */
    public void updateDownloadTasks(List<FileDownloadObject> mDownloadFileList) {

        DebugLog.log(TAG, "updateDownloadTasks-->");
        if (mDownloadFileList == null || mDownloadFileList.size() == 0) {
            DebugLog.log(TAG, "updateDownloadTasks-->mDownloadFileList is null or size==0!!");
            return;
        }

        if (mDownloader != null) {
            mDownloader.updateDownloadTasks(mDownloadFileList, -1);
        }
    }


    public void stopAndClear() {
        if (mDownloader != null) {
            mDownloader.unregisterListener(mDownloaderListener);
            mDownloadList.clear();
        }
        isInit = false;
    }

    public void deleteDownloadTask(List<FileDownloadObject> beanList){

        if (beanList == null || beanList.size() == 0) {
            DebugLog.log(TAG, "deleteDownloadTask-->mDownloadFileList is null or size==0!!");
            return;
        }

        if (mDownloader == null) {
            DebugLog.log(TAG, "deleteDownloadTask-->mFileDownloader is null!!");
            return;
        }

        ArrayList<String> idList = new ArrayList<String>();

        for(FileDownloadObject bean : beanList){
            idList.add(bean.getId());
        }

        if(mDownloader.deleteDownloadTasks(idList)){
            DebugLog.log(TAG, "deleteDownloadTask-->success");
        }
    }

    public void deleteDownloadTasksWithId(List<String> beanList){

        if (beanList == null || beanList.size() == 0) {
            DebugLog.log(TAG, "deleteDownloadTask-->mDownloadFileList is null or size==0!!");
            return;
        }

        if (mDownloader == null) {
            DebugLog.log(TAG, "deleteDownloadTask-->mFileDownloader is null!!");
            return;
        }

        if(mDownloader.deleteDownloadTasks(beanList)){
            DebugLog.log(TAG, "deleteDownloadTask-->success");
        }
    }

    public void deleteDownloadTaskWithGroupName(String groupName){

        if(TextUtils.isEmpty(groupName)){
            return;
        }

        if (mDownloader == null) {
            DebugLog.log(TAG, "deleteDownloadTaskWithGroupName-->mFileDownloader is null!!");
            return;
        }

        List<FileDownloadObject> beanList = mDownloader.getAllDownloadTask();

        ArrayList<String> deleteList = new ArrayList<String>();

        for(FileDownloadObject bean :beanList){

            if(groupName.equals(bean.getGroupName())) {
                DebugLog.log(TAG,"delete groupName = " + groupName + "-taskName = " + bean.getFileName());
                deleteList.add(bean.getId());
            }

        }

        if(deleteList.size()>0){
            if(mDownloader.deleteDownloadTasks(deleteList)){
                DebugLog.log(TAG, "deleteDownloadTaskWithGroupName-->success");
            } else{
                DebugLog.log(TAG, "deleteDownloadTaskWithGroupName-->fail");
            }
        } else {
            DebugLog.log(TAG,"deleteDownloadTaskWithGroupName-->no delete task");
        }


    }


    ///////////////////////////回调给UI层/////////////////////////////////////////////

    private class InnerListener implements IQiyiDownloaderListener<FileDownloadObject> {


        private void onDownloadDataSetChanged() {
            //重新获取数据
            mDownloadList = mDownloader.getAllDownloadTask();

        }

        /**
         * 状态发生变化
         *
         * @param task
         * @param changedType
         */
        private void onDownloadDataStatusChanged(FileDownloadObject task, int changedType) {

            // callback状态给业务方
            RemoteMessageProcesser.getInstance().sendMessage(FileDownloadHelper.buildCallbackMsg(task, changedType));

            DebugLog.log(TAG, "onDownloadDataStatusChanged = changeType>>" + changedType);
            DebugLog.log(TAG, "onDownloadDataStatusChanged = " + task.toString());

        }

        @Override
        public void onLoad() {

            DebugLog.log(TAG, "FileDownloadController>>onLoad");

            if (mDownloader == null) {

                DebugLog.log(TAG, "onLoad-->mFileDownloader is null!");

                return;
            }

            onDownloadDataSetChanged();

            autoStartDownloadFileTask(null);
        }

        @Override
        public void onAdd(List<FileDownloadObject> tasks) {

            DebugLog.log(TAG, "FileDownloadController>>onAdd");

            if (mDownloader == null) {
                DebugLog.log(TAG, "onAdd-->mFileDownloader is null!");
                return;
            }

            onDownloadDataSetChanged();

            FileDownloadObject bean = null;

            if (tasks != null && tasks.size() > 0) {

                for (FileDownloadObject fileBean : tasks) {
                    if (fileBean.getDownloadConfig().supportJumpQueue) {
                        bean = fileBean;
                        DebugLog.log(TAG,fileBean.getFileName() + ">>supportJumpQueue");
                        break;
                    }
                }
            }

            if (bean == null) {
                DebugLog.log(TAG,"onAdd>>autoStartDownloadFileTask");
                autoStartDownloadFileTask(null);
            } else {
                DebugLog.log(TAG,"onAdd>>checkAndDownloadFile");
                checkAndDownloadFile(bean);
            }


        }


        @Override
        public void onDelete(List<FileDownloadObject> tasks, int deleteAction) {

            DebugLog.log(TAG, "FileDownloadController>>onDelete");

            if (tasks == null || tasks.size() == 0) {
                DebugLog.log(TAG, "onDelete-->tasks ==null or size==0");
                return;
            }

            onDownloadDataSetChanged();

        }

        @Override
        public void onUpdate(List<FileDownloadObject> tasks, int key) {


            if (tasks == null || tasks.size() == 0) {

                DebugLog.log(TAG, "onUpdate-->tasks==null");

                return;
            }

            onDownloadDataSetChanged();
        }

        @Override
        public void onStart(FileDownloadObject task) {


            if (task == null) {
                DebugLog.log(TAG, "onStart-->task is null!");
                return;
            }

            DebugLog.log(TAG, task.getFileName() + ">>onStart");

            checkTaskConfigAndNetwork(mContext,task);

            onDownloadDataStatusChanged(task, FileDownloadCallback.CALLBACK_MSG_ON_START);

        }

        @Override
        public void onPause(FileDownloadObject task) {


            if (task == null) {
                DebugLog.log(TAG, "onPause-->task is null!");
                return;
            }
            DebugLog.log(TAG, task.getFileName() + ">>onPause" + task.toString());

            onDownloadDataStatusChanged(task, FileDownloadCallback.CALLBACK_MSG_ON_ABORT);

        }

        @Override
        public void onPauseAll() {

            onDownloadDataSetChanged();

        }

        @Override
        public void onNoDowningTask() {

            DebugLog.log(TAG, "FileDownloadController>>onNoDownloadingTAsk");

        }

        /**
         * 暂停当前正在下载的任务，可以通过恢复下载
         * 注意：如果当前没有下载任务，此方法无效
         */
        public void pauseDownloadTask() {
            DebugLog.log(TAG, "pauseDownloadTask");
            if (mDownloader != null) {
                mDownloader.pauseDownload();
            }
        }

        public void pauseDownloadTask(String id){
            DebugLog.log(TAG,"pauseDownloadTask = " + id);
            if (mDownloader != null) {
                mDownloader.pauseDownload(id);
            }
        }


        /**
         * 暂停所有任务，将所有的任务变为等待状态,此方法主要用于下载界面的“全部暂停”
         * {@see XTaskBean.STATUS_TODO -> XTaskBean.STATUS_DEFAULT}
         */
        public void stopAllRunningAndWaitingTask() {
            DebugLog.log(TAG, "stopAllRunningAndWaitingTask");
            if (mDownloader != null) {
                DebugLog.log(TAG, "mDownloader.stopAllDownload");
                mDownloader.stopAllDownload();
            }
        }

        /**
         * 开始所有任务，将所有的任务变为等待下载，并启动某一个下载任务,此方法主要用于下载界面的“全部开始”
         * {@see XTaskBean.STATUS_DEFAULT -> XTaskBean.STATUS_TODO}
         */
        public void startAllWaitingTask() {
            DebugLog.log(TAG, "startAllWaitingTask");
            if (mDownloader != null) {
                DebugLog.log(TAG, "mDownloader.startAllDownload");
                mDownloader.startAllDownload();
            }
        }


        @Override
        public void onDownloading(FileDownloadObject task) {


            if (task == null) {
                DebugLog.log(TAG, "onDownloading-->task is null!");
                return;
            }

            DebugLog.log(TAG, task.getFileName() + ">>onDownloading>>" + task.toString());

            mDownloadList = mDownloader.getAllDownloadTask();

            onDownloadDataStatusChanged(task, FileDownloadCallback.CALLBACK_MSG_ON_DOWNLOADING);

            checkTaskConfigAndNetwork(mContext,task);

        }

        @Override
        public void onComplete(FileDownloadObject task) {


            if (task == null) {
                DebugLog.log(TAG, "onComplete-->task is null");
                return;
            }
            DebugLog.log(TAG, task.getFileName() + ">>onComplete");

            onDownloadDataStatusChanged(task, FileDownloadCallback.CALLBACK_MSG_ON_COMPLETE);

            deleteDownloadTaskIfNeed(task);

        }

        @Override
        public void onError(FileDownloadObject task) {


            if (task == null) {
                DebugLog.log(TAG, "onError-->task is null");
                return;
            }
            DebugLog.log(TAG, task.getFileName() + ">>onError");

            DebugLog.log(TAG, "errorCode = " + task.errorCode);

            FileDownloadHelper.deliverFileDownloadErrorCode(task);//pingback需要投递

            onDownloadDataStatusChanged(task, FileDownloadCallback.CALLBACK_MSG_ON_ERROR);

            deleteDownloadTaskIfNeed(task);

        }

        @Override
        public void onFinishAll() {

            DebugLog.log(TAG, "FileDownloadController>>onFinishAll");

        }

        @Override
        public void onNoNetwork() {

            DebugLog.log(TAG, "FileDownloadController>>onNoNetwork");

        }

        @Override
        public void onNetworkNotWifi() {

            DebugLog.log(TAG, "FileDownloadController>>onNetworkNotWifi");

        }

        @Override
        public void onNetworkWifi() {

            DebugLog.log(TAG, "FileDownloadController>>onNetworkWifi");

        }

        @Override
        public void onMountedSdCard() {

            DebugLog.log(TAG, "FileDownloadController>>onMountedSdCard");


        }

        @Override
        public void onUnmountedSdCard(boolean isStop) {

            DebugLog.log(TAG, "FileDownloadController>>onUnmountedSdCard");


        }

        @Override
        public void onPrepare() {

            DebugLog.log(TAG, "FileDownloadController>>onPrepare");

            onDownloadDataSetChanged();

            checkAndDownloadFile(null);

        }

        @Override
        public void onSDFull(FileDownloadObject task) {

            DebugLog.log(TAG, "FileDownloadController>>onSDFull");

        }

        private void deleteDownloadTaskIfNeed(FileDownloadObject task) {

            //不需要存储到数据库，则完成一个任务，删除此任务在内存中的数据
            if (!task.mDownloadConfig.needDb && mDownloader != null) {
                DebugLog.log(TAG, "onComplete remove task = " + task.getFileName());
                mDownloader.deleteDownloadTask(task.getId());
            }

        }

        /**
         * 通过任务配置信息，检测是否需要在蜂窝网络停止下载
         * @param mContext
         * @param task
         */
        private void checkTaskConfigAndNetwork(Context mContext,FileDownloadObject task){

            NetworkStatus networkStatus = NetWorkTypeUtils.getNetworkStatus(mContext);
            if (!(networkStatus == NetworkStatus.WIFI || networkStatus == NetworkStatus.OFF)) {


                if (task!= null &&!task.isAllowInMobile()) {
                    DebugLog.log(TAG, task.getFileName() + ">>checkTaskConfigAndNetwork>>allow in mobile  = " + task.isAllowInMobile());
                    startOrPauseDownloadTask(task);
                }
            }

        }
    }
}
