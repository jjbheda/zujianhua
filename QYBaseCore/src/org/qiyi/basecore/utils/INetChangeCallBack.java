package org.qiyi.basecore.utils;

public interface INetChangeCallBack {
    /**
     * 仅仅监控网络，对类型没有要求
     *
     * @param isConnected true有网络
     */
    void onNetWorkChange(boolean isConnected);
}