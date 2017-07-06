package qiyi.demo1;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;
import android.util.TypedValue;
import android.graphics.Color;

/**
 * Created by jiangjingbo on 2017/5/27.
 */

public class MainAc extends BaseActivity {
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo1_activity_main);
        TextView tv = (TextView) findViewById(R.id.demo1_textView1);
        tv.setText(R.string.app_name_demo1);

       ImageView iv = (ImageView) findViewById(R.id.iv_constellatory);
        iv.setImageResource(R.drawable.shizi);

    }

}
