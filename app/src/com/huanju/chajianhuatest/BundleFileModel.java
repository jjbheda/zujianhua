package com.huanju.chajianhuatest;

import java.io.Serializable;

/**
 * Created by jiangjingbo on 2017/7/14.
 */

public class BundleFileModel  implements Serializable
{
    private final static long serialVersionUID = 1L;

    public String bundleVersion = "";

    public BundleFileModel(String bundleVersion){
        this.bundleVersion = bundleVersion;
    }

}
