package com.huanju.chajianhuatest;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import com.huanju.chajianhuatest.bundle.ClassPathHookUtil;
import com.huanju.chajianhuatest.bundle.JsonFileUtil;
import com.huanju.chajianhuatest.bundlemodel.BundleCallbackModel;
import com.huanju.chajianhuatest.bundlemodel.BundleFileModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

/**
 * @author
 * @date 17/2/21
 */
public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

    }

}
