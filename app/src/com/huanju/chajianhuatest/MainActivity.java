package com.huanju.chajianhuatest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.huanju.chajianhuatest.bundleUtils.BundleInstallUtils;
import com.huanju.chajianhuatest.bundleUtils.JsonFileUtil;
import com.huanju.chajianhuatest.bundlemodel.BundleFileModel;

import qiyi.basemodule.BasePro;

/**
 * @author
 * @date 17/2/21
 */
public class MainActivity extends Activity {
    private boolean flag = false;
    private boolean flag2 = false;
    public static final String TAG = "MainActivity";
    private static final String PACKAGE_NAME = "com.huanju.chajiandemo";
    private static final String TAGET_ACTIVITY = "com.huanju.chajiandemo.TestActivityTwo";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView iv = (ImageView) findViewById(R.id.test);
        iv.setImageResource(R.drawable.shuimo);
        BasePro basePro = new BasePro();
        String ss = basePro.getBaseStatusString("110");
        Log.e("TAG", ss);
        Toast.makeText(MainActivity.this, "来自主APP" + ss, Toast.LENGTH_SHORT).show();

        findViewById(R.id.bbb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "加载流程启动................");
                final Context context = MainActivity.this;
                final BundleFileModel model = JsonFileUtil.getBundleVersion(MainActivity.this, PACKAGE_NAME);
                try {
                    if (BundleInstallUtils.hasLoaded(model)) {
                        Intent intent = new Intent();
                        intent.setClass(MainActivity.this, Class.forName("com.huanju.chajiandemo.TestActivityTwo"));
                        startActivity(intent);
                    } else {
                        Log.d(TAG, "加载二维码模块");
                        BundleInstallUtils bundleInstallUtils = new BundleInstallUtils(context);
                        bundleInstallUtils.installBundle(model, new BundleInstallUtils.LoadedCallBack() {
                            @Override
                            public void success() {
                                Log.e(TAG, "LocalVideo module bundle success");
                                Intent intent = new Intent();
                                try {
                                    intent.setClass(context, Class.forName(TAGET_ACTIVITY));
                                    context.startActivity(intent);
                                } catch (Exception e) {
                                    Log.e(TAG, "LocalVideo module install bundle exception");
                                }catch (NoClassDefFoundError error){
                                    Log.e(TAG, "LocalVideo module install bundle error");
                                    error.printStackTrace();
                                }
                            }


                            @Override
                            public void fail() {
                                Log.d(TAG, "加载二维码模块失败");
                            }
                        });
                    }
                } catch (ClassNotFoundException e) {
                    Log.d(TAG, "加载二维码模块失败");
                    e.printStackTrace();
                }
            }
        });

//        findViewById(R.id.go_demo1).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (flag2) {
//                    try {
//                        startActivity(new Intent(getApplicationContext(), Class.forName("qiyi.demo1.MainAc")));
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    EventBus.getDefault().post(new BundleFileModel(JsonFileUtil.getBundleVersion(MainActivity.this,"qiyi.demo1")));
//                }
//            }
//        });

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
