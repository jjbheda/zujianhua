package com.huanju.chajianhuatest.bundlemodel;

import java.io.Serializable;

/**
 * Created by jiangjingbo on 2017/7/14.
 */

public class BundleFileModel  implements Serializable
{
    private final static long serialVersionUID = 1L;

    public String bundleVersion = "";
    public String md5 = "";

    public BundleFileModel() {
    }
    public BundleFileModel(String bundleVersion,String md5) {
        this.bundleVersion = bundleVersion;
        this.md5 = md5;
    }

}
