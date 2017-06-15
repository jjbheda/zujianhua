package qiyi.demo1;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;


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
    }
}
