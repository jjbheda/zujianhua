package com.huanju.chajianhuatest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;

import qiyi.basemodule.BasePro;

/**
 * @author
 * @date 17/2/21
 */
public class MainActivity extends FragmentActivity {
    private boolean flag = false;
    private boolean flag2 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        ImageView iv = (ImageView) findViewById(R.id.test);
        iv.setImageResource(R.drawable.shuimo);
        BasePro basePro = new BasePro();
        String ss = basePro.getBaseStatusString("110");
        Log.e("TAG", ss);
        Toast.makeText(MainActivity.this, "来自主APP" + ss, Toast.LENGTH_SHORT).show();
        final SharedPreferences soNameFileSP = MainActivity.this.getSharedPreferences("soNameFile", 0);
        final SharedPreferences.Editor soSpEditor = soNameFileSP.edit();

        findViewById(R.id.bbb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (JsonFileUtiles.hasLoaded(getFilesDir(),"com.huanju.chajiandemo")) {
                    try {
                        startActivity(new Intent(getApplicationContext(), Class.forName("com.huanju.chajiandemo.TestActivityTwo")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    EventBus.getDefault().post(new BundleFileModel(JsonFileUtiles.getBundleVersion(getFilesDir(),"com.huanju.chajiandemo")));
                }
            }
        });

        findViewById(R.id.go_demo1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flag2) {
                    try {
                        startActivity(new Intent(getApplicationContext(), Class.forName("qiyi.demo1.MainAc")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    EventBus.getDefault().post(new BundleFileModel("qiyi.demo1"));
                }
            }
        });

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(final BundleCallbackModel event) {
        if (event == null || event.packageName.isEmpty())
            return;
        if (event.packageName.equals("com.huanju.chajiandemo")) {
            flag = true;
        } else if (event.packageName.equals("qiyi.demo1")) {
            flag2 = true;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, event.packageName + "加载完成", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
