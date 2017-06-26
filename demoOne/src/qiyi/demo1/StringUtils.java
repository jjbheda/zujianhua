package qiyi.demo1;

import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import org.qiyi.basecore.utils.UIUtils;

/**
 * Created by jiangjingbo on 2017/6/24.
 */

public class StringUtils {
    public static int getSum(int x,int y){


        TranslateAnimation mAnimation = new TranslateAnimation(0, 0, 0, UIUtils.dip2px(260));
        mAnimation.setRepeatMode(Animation.REVERSE);
        mAnimation.setDuration(2500);
        return x+y;
    }
}
