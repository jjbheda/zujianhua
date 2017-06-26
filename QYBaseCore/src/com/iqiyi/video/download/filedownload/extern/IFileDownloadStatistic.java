package com.iqiyi.video.download.filedownload.extern;

import org.qiyi.video.module.download.exbean.FileDownloadObject;

/**
 * Created by songguobin on 2017/4/11.
 */

public interface IFileDownloadStatistic {

    void sendStatistic(FileDownloadObject fileDownloadObject);

}
