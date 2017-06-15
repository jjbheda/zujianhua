package com.huanju.chajiandemo;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;


/**
 * Created by DELL-PC on 2017/2/22.
 */

public class TestActivityTwo extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.e("Main","test this = " + this);
//        Log.e("Main","getResource = " + getResources());
//        Log.e("Main","getApplication = " + getApplication());
//        Log.e("Main","getApplication class = " + getApplication().getClass().getName());
        setContentView(R.layout.second);
//        ImageView imageView=(ImageView)findViewById(R.id.iv_01);
//        imageView.setImageResource(R.drawable.xin);
//        imageView.setImageResource(getResources().getIdentifier("fenghuang","drawable",getPackageName()));


        findViewById(R.id.to_demo1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startActivity(new Intent(MainActivity.this,TestActivity.class));
                try {
                    startActivity(new Intent(getApplicationContext(), Class.forName("qiyi.demo1.MainAc")));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

//    @Override
//    public Resources getResources() {
//        Log.e("chajian","getApplicationContext = " + getApplicationContext());
//        Log.e("chajian","getApplicationContext 2 = " + getApplication());
//        Log.e("chanjian ","getApplicationContext 2 = " + super.getResources());
//
//        if(getApplication() != null && getApplication().getResources() != null){
//            return getApplication().getResources();
//        }
//        return super.getResources();
//    }
//
//    @Override
//    public AssetManager getAssets() {
//        if(getApplication() != null && getApplication().getAssets() != null){
//            return getApplication().getAssets();
//        }
//        return super.getAssets();
//    }
//
//    @Override
//    public Resources.Theme getTheme() {
//        if(getApplication() != null && getApplication().getTheme() != null){
//            return getApplication().getTheme();
//        }
//        return super.getTheme();
//    }
}
