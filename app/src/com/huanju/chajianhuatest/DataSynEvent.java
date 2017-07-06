package com.huanju.chajianhuatest;

/**
 * Created by jiangjingbo on 2017/7/5.
 */

public class DataSynEvent {
    private boolean beginInstall;
    public DataSynEvent(boolean beginInstall){
        this.beginInstall = beginInstall;
    }

    public void setBeginInstall(boolean beginInstall){
        this.beginInstall = beginInstall;
    }

    public boolean isBeginInstall(){
        return beginInstall;
    }

}
