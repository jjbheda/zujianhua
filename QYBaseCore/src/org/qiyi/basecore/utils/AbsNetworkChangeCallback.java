package org.qiyi.basecore.utils;


public abstract class AbsNetworkChangeCallback implements INetChangeCallBack {

    boolean mNeedRetrieveBySelf = false;

    @Override
    public void onNetWorkChange(boolean isConnected) {
    }

    /**
     * 网络发生变化
     */
    public void onNetworkChange(NetworkStatus networkStatus) {
    }

    /**
     * 没有任何网络
     */
    public void onChangeToOff(NetworkStatus networkStatus) {
    }

    /**
     * 网络转换为WIFI
     */
    public void onChangeToWIFI(NetworkStatus networkStatus) {
    }

    /**
     * 连接到网络，但非wifi情况
     */
    public void onChangeToNotWIFI(NetworkStatus networkStatus) {
    }

    /**
     * 连接到网络
     */
    public void onChangeToConnected(NetworkStatus networkStatus) {
    }

    /**
     * 网络变为2G或3G或4G
     */
    public void onChangeToMobile2GAnd3GAnd4G(NetworkStatus networkStatus) {

    }

    public void onChangeToMobile2G(NetworkStatus networkStatus) {
    }

    public void onChangeToMobile3G(NetworkStatus networkStatus) {
    }

    public void onChangeToMobile4G(NetworkStatus networkStatus) {
    }

    public void onDestroy() {
    }
}
