package com.huanju.chajianhuatest.bundlemodel;

import java.io.Serializable;

/**
 * Created by jiangjingbo on 2017/7/11.
 */

public class BundleCallbackModel implements Serializable
{
    private final static long serialVersionUID = 1L;
    public String packageName = "";
    public BundleCallbackModel(String packageName){
        this.packageName = packageName;
    }
}
