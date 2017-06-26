package org.qiyi.basecore.filedownload;

import android.util.Pair;

import java.util.concurrent.Callable;

public class IChangeImp implements IChange {

    protected Callable currentThread = null;
    
    @Override
    public void onPaused(Pair<Integer, String> reason, boolean isOutdated) {

    }

    @Override
    public void onFailed(Pair<Integer, String> reason, boolean isOutdated) {

    }

    @Override
    public void onCompleted(boolean isOutdated) {

    }

    @Override
    public void onDownloadUrlRedirect(String redirectUrl, FileDownloadStatus fileDownloadStatus) {
        fileDownloadStatus.setDownloadUrl(redirectUrl);
    }

    @Override
    public void onDownloadProgress(long len, boolean isOutdated) {
        
    }

}
