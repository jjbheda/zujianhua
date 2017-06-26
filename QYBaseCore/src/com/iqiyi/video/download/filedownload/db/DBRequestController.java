package com.iqiyi.video.download.filedownload.db;


/**
 * 由基线RequestController简化而来，只负责DB
 * 
 * @author Kangle
 * 
 */
public class DBRequestController /*implements IDataTask*/
{
	/**
	 * 数据库操作队列
	 */
	private AsyncTaskDbQueue mDatabaseTaskQueue;

	public DBRequestController()
	{
		mDatabaseTaskQueue = new AsyncTaskDbQueue();
	}

	/**
	 * 初始化操作
	 */
	public void init()
	{
		//启动数据库操作线程
		mDatabaseTaskQueue.start();
	}

	/**
	 * 向数据库操作队列添加一个任务
	 * 
	 * @param task
	 */
	public void addDBTask(AbstractDbTask task)
	{
		mDatabaseTaskQueue.addTask(task);
	}

	/**
	 * 向数据库操作队列添加一个任务,此任务超时限制为timeout毫秒
	 * 
	 * @param task
	 */
	public void addDBTask(AbstractDbTask task, int timeOut)
	{
		mDatabaseTaskQueue.addTask(task, timeOut);
	}

}
