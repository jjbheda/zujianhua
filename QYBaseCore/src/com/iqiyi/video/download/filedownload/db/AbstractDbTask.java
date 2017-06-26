package com.iqiyi.video.download.filedownload.db;

/**
 * 异步线程队列使用的task虚基类
 * @author songguobin
 *
 */
public abstract class AbstractDbTask
{
	private CallBack mCallBack;
	
	protected int mResponseCode;
	protected Object mResponseData;
	
	
	public AbstractDbTask(CallBack callBack)
	{
		this.mCallBack = callBack;
	}
	
	/**
	 * 线程调用异步方法
	 */
	public void process()
	{
		doInBackground();
	}
	
	/**
	 * 异步操作实现方法
	 */
	protected abstract void doInBackground();
	
	/**
	 * 正常回调方法
	 */
	public synchronized void callBack()
	{
		if (mCallBack != null)
		{
			mCallBack.callBack(mResponseCode, mResponseData);
			mCallBack = null;
		}
	}
	
	/**
	 * 超时回调方法
	 */
	public synchronized void callBackTimeout()
	{
		if (mCallBack != null)
		{
			mCallBack.callBack(CallBack.RESPONSE_ERROR, null);
			mCallBack = null;
		}
	}
	
	/**
	 * 回调接口
	 * @author QIYI
	 *
	 */
	public interface CallBack
	{
		public static final int RESPONSE_OK = 200;
		public static final int RESPONSE_ERROR = -1;
		
		/**
		 * task 回调方法
		 * @param responseCode 任务状态返回值
		 * @param responseData  任务返回数据
		 */
		public void callBack(int responseCode, Object responseData);
	}
}
