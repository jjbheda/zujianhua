package qiyi.demo1;

import android.app.Activity;
import android.os.Bundle;
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

public class MainAc extends Activity {
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo1_activity_main);
        TextView tv = (TextView) findViewById(R.id.demo1_textView1);
        tv.setText(R.string.app_name_demo1);

//        Button textView = new Button(this);
//        textView.setText("我是插件Demo1 Activity,我是代码布局，没有资源");
//        textView.setTextColor(Color.RED);
//        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,30);
//        textView.setGravity(Gravity.CENTER);
    }
}
