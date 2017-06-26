package com.iqiyi.video.download.engine.downloader;

import org.qiyi.video.module.download.exbean.XTaskBean;

import java.util.List;


/**
 * <pre>
 * 爱奇艺下载管理器的接口。
 * User: jasontujun
 * Date: 14-9-11
 * Time: 下午5:16
 * </pre>
 */
public interface IQiyiDownloader<B extends XTaskBean> {

    /**
     * 初始化下载管理器(重复调用，只会初始化一次)。
     */
    void init();

    /**
     * 退出下载管理中心。
     * 暂停所有下载，清空下载管理器的内存数据；常在程序退出前调用.
     */
    void exit();
    /**
     * 从数据持久层加载下载数据到内存，
     * 加载数据是在异步线程中执行，完成后会回调监听的onLoad()方法。
     * @param isForce 是否强制加载。如果为false，则多次调用只实际加载一次；
     *                如果为true，则清空当前内存数据重新加载。
     */
    void load(boolean isForce);

    /**
     * 批量添加下载任务。
     * 添加时会进行去重判断，添加进内存和持久层中，添加成功会回调监听的onAdd()方法。
     * @param tasks 要添加下载任务列表
     * @return 添加失败或没有可添加的，返回false(此时不会回调onAdd);添加成功返回true
     */
    boolean addDownloadTasks(List<B> tasks);

    /**
     * 批量删除指定的下载任务。
     * 删除内存中和持久层的数据，删除成功会回调监听的onDelete()方法。
     * 如果包含正在下载的任务，则会暂停并删除该任务。
     * @param tasksIds 下载任务的唯一id
     * @return 删除失败或没有可删除的，返回false(此时不会回调onDelete);删除成功返回true
     */
    boolean deleteDownloadTasks(List<String> tasksIds);


    /**
     * 同步删除任务
     * @param tasksIds
     * @return
     */
    boolean deleteDownloadTasksSync(List<String> tasksIds);


    /**
     * 删除单个任务，同时不删除文件
     * @param taskid
     * @return
     */
    boolean deleteDownloadTask(String taskid);

    /**
     * 快速删除任务
     *
     * @param taskIds
     * @return
     */
    boolean deleteDownloadTasksForFast(List<String> taskIds);

    /**
     * 通过taskId寻找下载任务对象
     * @param taskId
     * @return
     */
    B findDownloadTaskById(String taskId);

    /**
     * 删除所有下载任务(包括未完成和已完成任务)。
     * 删除数据库和本地保存的文件，删除成功会回调监听的onDelete()方法。
     * 如果包含正在下载的任务，则会暂停并删除该任务。
     */
    void clearAllDownloadTask();

    /**
     * 批量更新当前下载任务中的数据。
     * 更新内存中和持久层的数据。
     * @param tasksIds 下载任务的唯一id
     * @param key 更新的字段
     * @param value 更新的值
     * @return 更新失败或没有可更新的，返回false(此时不会回调onUpdate);删除成功返回true
     */
    boolean updateDownloadTasks(List<String> tasksIds, int key, Object value);

    /**
     * 批量更新当前下载任务中的数据。
     * @param tasks
     *         要批量跟新的数据
     * @return
     *         更新失败或没有可更新的，返回false(此时不会回调onUpdate);删除成功返回true
     */
    boolean updateDownloadTasks(List<B> tasks,int key);

    /**
     * 获取下载管理器中所有的下载任务。
     * @return 返回下载管理器中所有的下载任务。
     */
    List<B> getAllDownloadTask();

    /**
     * 启动下载(等待中->缓存中)。
     * 如果当前有任务正在下载，则什么都不做；否则启动一个等待中的任务。
     * 启动成功会回调监听的onStart()方法。
     * @return 启动成功返回true;否则返回false。
     */
    boolean startDownload();

    /**
     * 启动指定id的下载任务(等待中->缓存中)。
     * 如果当前有其他任务正在下载，则暂停该任务(缓存中->等待中)并启动指定id的下载任务。
     * 启动成功会回调监听的onStart()方法。
     * @param taskId 下载任务的唯一id
     * @return 启动成功返回true;否则返回false。
     */
    boolean startDownload(String taskId);

    /**
     * 暂停运行队列的所有任务(缓存中->等待中)。
     * (不会将任务从运行队列移除)。
     * @return 如果当前有任务，且被暂停成功，则返回true；否则返回false
     */
    boolean pauseDownload();

    /**
     * 暂停指定Id的任务(缓存中->等待中)。
     * 如果指定Id的任务不存在，或在等待队列中，则什么都不做，返回false；
     * 如果指定Id的任务存在，且在运行队列中，暂停该任务(不会将任务从运行队列移除)。
     * @param taskId 任务的唯一Id
     * @return 如果暂停成功，则返回true；否则返回false
     */
    boolean pauseDownload(String taskId);



    /**
     * 停止当前的下载任务(缓存中->已暂停)。
     * 如果当前没有正在下载的任务，则什么都不做。
     * 停止成功会回调监听的onStop()方法。
     * @return 暂停成功返回true;否则返回false。
     */
    boolean stopDownload();

    /**
     * 停止指定id的下载任务(缓存中->已暂停)，并启动下一个任务。
     * 如果指定任务不存在或不在下载，则什么都不做。
     * 停止成功会回调监听的onStop()方法。
     * @param taskId 任务的唯一Id
     * @return 暂停成功返回true;否则返回false。
     */
    boolean stopDownload(String taskId);

    /**
     * 停止任务管理器中的所有的任务(缓存中->已暂停 or 等待中->已暂停)
     * @return
     */
    boolean stopAllDownload();

    /**
     * 开始任务管理器中的所有任务(已暂停->等待中 or 已暂停->缓存中)
     * @return
     */
    boolean startAllDownload();

    /**
     * 暂停并清空内存中的任务(不会删除本地文件)。
     * 和load()为对应的方法。
     */
    void stopAndClear();


    /**
     * 设置是否自动下载
     */
    void setAutoRunning(boolean auto);
    /**
     * 是否是自动下载
     */
    boolean isAutoRunning();

    /**
     * 当前是否有正在下载的任务
     */
    boolean hasTaskRunning();

    /**
     * 当前正在下载的对象
     * @return
     *        返回当前正在下载对象，由于后期可能支持多任务下载，所以返回列表
     */
    B getRunningObject();

    /**
     * 注册监听
     */
    void registerListener(IQiyiDownloaderListener<B> listener);

    /**
     * 注销监听
     */
    void unregisterListener(IQiyiDownloaderListener<B> listener);

    static final int NET_OFF = 0;// 无网络
    static final int NET_WIFI = 1;// wifi
    static final int NET_MOBILE = 2;// 流量
    /**
     * 网络状况发生变化的处理。
     * @param netType 当前网络状况
     *                无网络: NET_OFF = 0;
     *                wifi: NET_WIFI = 1;
     *                2g流量: NET_MOBILE = 2;
     */
    void handleNetWorkChange(int netType);

    static final int SD_CARD_INSERT = 0;// 插卡
    static final int SD_CARD_REMOVE = 1;// 拔卡
    /**
     * sd卡发生变化的处理(主要包括插卡和拔卡)。
     * @param sdCardType 当前sd卡状况
     *                   插卡: SD_CARD_INSERT = 0;
     *                   拔卡: SD_CARD_REMOVE = 1;
     */
    void handleSdCardChange(int sdCardType);

    /**
     * 删除本地文件
     * @param beans
     * @return
     */
    void deleteLocalFile(List<B> beans);
    void setTaskStatus(B task, int status) ;

    void setMaxParalleNum(int paralleNum);

}
