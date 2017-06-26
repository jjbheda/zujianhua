package com.iqiyi.video.download.filedownload.ipc;

/**
 * Created by songguobin on 2017/3/17.
 */

public interface FileBinderCallback {

    void bindSuccess();

    void bindFail(String errorCode);

}
