package org.qiyi.video.module.download.exbean;

/**
 * 就绪（默认）、正在下载、下载完成、暂停、失败
 */
public enum DownloadStatus {
	DEFAULT, DOWNLOADING, FINISHED, WAITING, FAILED, PAUSING, STARTING;
	
	public static DownloadStatus getByValue(int val)
	{
		for(DownloadStatus status : values())
		{
			if(status.ordinal() == val)
			{
				return status;
			}
		}
		
		return null;
	}
}
