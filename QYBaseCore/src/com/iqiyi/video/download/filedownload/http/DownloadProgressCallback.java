package com.iqiyi.video.download.filedownload.http;

import org.qiyi.video.module.download.exbean.XTaskBean;

/**
 * Created by songguobin on 2017/1/22.
 *
 * 下载过程进度回调接口
 *
 */

public interface DownloadProgressCallback<B extends XTaskBean> {

    /**
     * 数据变化
     *
     * @param bean
     */
    void onDataChanged(B bean);

}
