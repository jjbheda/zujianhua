package com.huanju.chajiandemo;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.huanju.chajiandemo.fragment.HelloFragment;
import com.huanju.chajiandemo.fragment.SecondFragment;
import java.lang.reflect.Method;
import qiyi.basemodule.BasePro;
import static android.R.attr.x;



/**
 * Created by DELL-PC on 2017/2/22.
 */

public class TestActivityTwo extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.second);
//        ImageView imageView=(ImageView)findViewById(R.id.iv_01);
//        imageView.setImageResource(R.drawable.baiyang);
//        imageView.setImageResource(getResources().getIdentifier("fenghuang","drawable",getPackageName()));
        TextView tv = (TextView) findViewById(R.id.to_demo1);
        if(tv == null){
            Log.e("TAG","tv is null ");
        } else {
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        startActivity(new Intent(getApplicationContext(), Class.forName("qiyi.demo1.MainAc")));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });
        }


        BasePro basePro = new BasePro();
        String ss =basePro.getBaseStatusString("112");
        Log.e("TAG",ss);
        Toast.makeText(TestActivityTwo.this,"来自插件APP"+ss,Toast.LENGTH_SHORT).show();
        HelloFragment rightFragment = new HelloFragment();
        FragmentManager fm = TestActivityTwo.this.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.fragment_lt, rightFragment, "Fragment");
        ft.commit();

        TextView tv_change = (TextView) findViewById(R.id.change_fragment);
        tv_change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SecondFragment rightFragment = new SecondFragment();
                FragmentManager fm = TestActivityTwo.this.getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                ft.replace(R.id.fragment_lt, rightFragment, "Fragment2");
                ft.commit();
            }
        });

    }

    @Override
    public Resources getResources() {

        if(getApplication() != null && getApplication().getResources() != null){
            return getApplication().getResources();
        }
        return super.getResources();
    }

    @Override
    public AssetManager getAssets() {
        if(getApplication() != null && getApplication().getAssets() != null){
            return getApplication().getAssets();
        }
        return super.getAssets();
    }

    @Override
    public Resources.Theme getTheme() {
        if(getApplication() != null && getApplication().getTheme() != null){
            return getApplication().getTheme();
        }
        return super.getTheme();
    }
}
