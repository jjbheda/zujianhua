package com.iqiyi.video.download.filedownload.verify;

/**
 * Created by songguobin on 2016/12/19.
 *
 * RSA校验
 */
public class RSAVerification implements BaseVerification{


    @Override
    public boolean verify(String filepath, String sign) {

        return VerifyHelper.isDownloadFileModified(filepath,sign);

    }
}
