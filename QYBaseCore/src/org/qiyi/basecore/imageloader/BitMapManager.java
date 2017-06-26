package org.qiyi.basecore.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

/**
 * Not suggest to use this class anymore, instead use
 * {@link org.qiyi.basecore.imageloader.ImageLoader}
 * TODO delete in v8.6
 */
@Deprecated
public class BitMapManager {
    //屏幕宽度
    public int mWindowsWidth = 0;
    //屏幕高度
    public int mWindowsHeight = 0;

    public BitMapManager(Context context) {
        mWindowsWidth = context.getResources().getDisplayMetrics().widthPixels;
        mWindowsHeight = context.getResources().getDisplayMetrics().heightPixels;
    }
}
