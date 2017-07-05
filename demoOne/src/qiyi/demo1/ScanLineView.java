package qiyi.demo1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;


/**
 * Created by zhangqixun on 17/6/7.
 */
public class ScanLineView extends View {

    private Context mContext;
    private Bitmap mScanLine;
    private Rect mRect;
    private TranslateAnimation mAnimation;

    public ScanLineView(Context context) {
        super(context);
        init(context);
    }

    public ScanLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ScanLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mRect = new Rect();
//        mScanLine = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.scan_line);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mRect.set(0, 0, getWidth(), getHeight());
        canvas.drawBitmap(mScanLine, null, mRect, null);
    }
}
