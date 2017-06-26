package com.iqiyi.video.download.filedownload;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.qiyi.video.module.download.exbean.FileDownloadObject;

import java.util.List;

/**
 * Created by songguobin on 2017/3/17.
 */

public class FileDownloadExBean implements Parcelable {

    public int actionId;//事件id,唯一标识一次事件通信

    public FileDownloadObject mFileObject;//文件下载对象

    public List<FileDownloadObject> mFileList;//文件下载对象列表

    public List<String> mUrlList;//下载地址列表

    public String sValue1;

    public String sValue2;

    public int iValue1;

    public int iValue2;

    public Bundle mBundle;

    //不参与序列化
    public Context mContext;

    public Object mObject;

    public FileDownloadExBean() {

    }

    public FileDownloadExBean(int actionId) {
        this.actionId = actionId;
    }

    public int getActionId() {

        return actionId;

    }

    protected FileDownloadExBean(Parcel in) {

        actionId = in.readInt();
        mFileObject = in.readParcelable(FileDownloadObject.class.getClassLoader());
        mFileList = (List<FileDownloadObject>) in.readArrayList(FileDownloadObject.class.getClassLoader());
        mUrlList = in.readArrayList(String.class.getClassLoader());
        sValue1 = in.readString();
        sValue2 = in.readString();
        iValue1 = in.readInt();
        iValue2 = in.readInt();
        mBundle = in.readBundle();


    }

    public static final Creator<FileDownloadExBean> CREATOR = new Creator<FileDownloadExBean>() {
        @Override
        public FileDownloadExBean createFromParcel(Parcel in) {
            return new FileDownloadExBean(in);
        }

        @Override
        public FileDownloadExBean[] newArray(int size) {
            return new FileDownloadExBean[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeInt(actionId);
        dest.writeParcelable(mFileObject, flags);
        dest.writeList(mFileList);
        dest.writeList(mUrlList);
        dest.writeString(sValue1);
        dest.writeString(sValue2);
        dest.writeInt(iValue1);
        dest.writeInt(iValue2);
        dest.writeBundle(mBundle);

    }
}
