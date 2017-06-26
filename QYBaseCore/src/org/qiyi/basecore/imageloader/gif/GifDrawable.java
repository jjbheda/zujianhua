package org.qiyi.basecore.imageloader.gif;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.view.Gravity;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.imageloader.gif.GifDecode.GifDecoder;

/**
 * Created by niejunjiang on 2015/10/24.
 */
public class GifDrawable extends Drawable implements Animatable {
    public static final String DEBUGKEY = GifDrawable.class.toString();

    /**
     * paint 画gif专用,有可能会
     */
    private final Paint paint;


    /**
     * 画的区域大小，目前都是根据原动态图的大小来的
     */
    private final Rect destRect;

    /**
     * GifState 保存的是gif的基本信息，持有gif的第一帧bitmap
     * gif头信息，gif的data信息，gif的长宽信息
     */
    private GifState state;

    /**
     * Gif解码器，要哪一帧解哪一帧（但是也是要让顺序解）
     */
    private GifDecoder decoder;

    /**
     * isRunning 只是表示gif是否能够播放，不表示是否正在播放中
     * 正常是否在播放中需要根据gifStatus判断
     */
    private Boolean isRunning = true;

    /**
     * 这个Gifdrawable 是否被回收，
     */
    private Boolean isRecycled = false;

    /**
     * 初始转态
     */
    public static final int STATUS_DEFAULT = -1;

    /**
     * 运行状态
     */
    public static final int STATUS_START = STATUS_DEFAULT + 1;
    /**
     * 暂停转态
     */
    public static final int STATUS_STOP = STATUS_START + 1;

    /**
     * 可见状态
     */
    public static final int STATUS_VISIBLE = STATUS_STOP + 1;

    /**
     * 不可见状态
     */
    public static final int STATUS_INVISIBLE = STATUS_VISIBLE + 1;

    /**
     * 回收状态
     */
    public static final int STATUS_RECYCLE = 4;


    /**
     * gif状态标识-1，默认状态表示
     */
    private int gifStatus = -1;

    /**
     * 这个gif到目前为止，播放的次数,大于maxLoopConut是停止播放
     */
    private int loopCount = -1;

    /**
     * gif解析出来自带的给定的循环次数，默认-1是无限循环播放
     */
    private int maxLoopCount;

    private final int PLAY_FOREVER = -1;

    private boolean applyGravity;

    /**
     * 每一帧播放时间，由gif 解码出来的，
     */
    private long delayTime;

    /**
     * 每一帧显示的时候的时间
     */
    private long currentInvalidateTime;

    /**
     * 下一帧显示的时间，应该是上一帧的显示时间+上一帧的delayTime
     */
    private long nextTargetTime;


    /**
     * 记录当前第几帧
     */
    private int currentindex = 0;

    /**
     * 记录当前帧的bitmap
     */
    private Bitmap currentFrame = null;

    /**
     * 记录gif 的总的帧数
     */
    private final int totlaFrames;

    private Runnable invaliteTask = new Runnable() {
        @Override
        public void run() {
            GifDrawable.this.invalidateSelf();
        }
    };

    public GifDrawable(Context context,
                       int width, int height, GifHeader header, byte[] data, Bitmap firstFrame, boolean isAutomPaly) {
        this(new GifState(header, data, context, width, height, firstFrame));
        if (isAutomPaly) {
            isRunning = true;
        } else {
            isRunning = false;
        }
    }

    public GifDrawable(GifState state) {
        this.destRect = new Rect();
        if (state == null) {
            throw new NullPointerException("GifState must not be null");
        } else {
            this.state = state;
            this.decoder = new GifDecoder();
            this.paint = new Paint();
            decoder.setData(state.gifHeader, state.data);
            this.totlaFrames = decoder.getFrameCount();
            /**
             * 默认设置无限制播放
             */
            this.maxLoopCount = PLAY_FOREVER;
            this.currentFrame = state.firstFrame;
            this.currentindex = 0;
        }
    }

    public Bitmap getFirstFrame() {
        return this.state.firstFrame;
    }

    public byte[] getData() {
        return this.decoder.getData();
    }

    public int getFrameCount() {
        return this.totlaFrames;
    }

    @Override
    public void start() {
        setStatus(STATUS_START);
    }

    @Override
    public void stop() {
        setStatus(STATUS_STOP);
        if (Build.VERSION.SDK_INT < 11) {
            this.reset();
        }
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        if (!visible) {
            //不可见停止播放
            setStatus(STATUS_INVISIBLE);
        } else {
            setStatus(STATUS_VISIBLE);
        }
        return super.setVisible(visible, restart);
    }

    /**
     * 这个宽度是gif解码出来后本来的宽度
     *
     * @return int
     */
    public int getIntrinsicWidth() {
        return this.state.firstFrame.getWidth();
    }

    public int getIntrinsicHeight() {
        return this.state.firstFrame.getHeight();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        this.applyGravity = true;
    }

    @Override
    protected boolean onLevelChange(int level) {
        return super.onLevelChange(level);
    }

    /**
     * 状态信息由drawable自己控制,为防止状态错乱，不可为public
     *
     * @param status
     */
    private void setStatus(int status) {
        this.gifStatus = status;
        checkStatus();
    }

    /**
     * 可获取到当前状态当时不能随便控制，只能stop start 接口启动或暂停
     *
     * @return
     */
    public int getstatus() {
        return this.gifStatus;
    }

    private void checkStatus() {
        switch (gifStatus) {
            case STATUS_DEFAULT:
            case STATUS_START:
                isRunning = true;
                invalidateSelf();
                break;
            case STATUS_RECYCLE:
                isRunning = false;
                isRecycled = true;
                break;
            case STATUS_STOP:
                isRunning = false;
                invalidateSelf();
            case STATUS_INVISIBLE:
                break;
            case STATUS_VISIBLE:
                reset();
                break;
            default:
                break;
        }
    }


    @Override
    public void draw(final Canvas canvas) {
        if (!this.isRecycled) {
            if (this.applyGravity) {
                Gravity.apply(Gravity.AXIS_CLIP, this.getIntrinsicWidth(), this.getIntrinsicHeight(), this.getBounds(), this.destRect);
                this.applyGravity = false;
            }
            DebugLog.d(DEBUGKEY, "isRunning=" + isRunning);

            if (isRunning) {
                this.decoder.advance();
                currentFrame = decoder.getNextFrame();
                currentindex = decoder.getCurrentFrameIndex();
                this.currentInvalidateTime = SystemClock.uptimeMillis();
                this.delayTime = decoder.getDelay(currentindex);
                this.nextTargetTime = currentInvalidateTime + delayTime;
                canvas.drawBitmap(currentFrame, (Rect) null, this.destRect, this.paint);
                if (currentindex == getFrameCount() - 1) {
                    loopCount++;
                    DebugLog.d(DEBUGKEY, "循环次数loopcount" + loopCount + ",默认次数maxLoopcount" + maxLoopCount);
                }
                if (loopCount <= maxLoopCount || maxLoopCount == PLAY_FOREVER) {
                    scheduleSelf(invaliteTask, nextTargetTime);
                } else {
                    stop();
                    DebugLog.d(DEBUGKEY, "stop！！！");
                }
            } else {
                canvas.drawBitmap(currentFrame, (Rect) null, this.destRect, this.paint);
                DebugLog.d(DEBUGKEY, "停止");
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        this.paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        this.paint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return -2;
    }

    public void reset() {
        this.isRunning = true;
        this.currentFrame = state.firstFrame;
        this.loopCount = -1;
        this.maxLoopCount = PLAY_FOREVER;
        this.currentindex = 0;
        decoder.resetFrameIndex();
        this.invalidateSelf();
    }

    public ConstantState getConstantState() {
        return this.state;
    }


    public void recycle() {
        setStatus(STATUS_RECYCLE);
        if (decoder != null) {
            decoder.clear();
            decoder = null;
        }
        if (state != null) {
            state.clear();
            state = null;
        }
        if (currentFrame != null) {
            currentFrame.recycle();
            currentFrame = null;
        }
        DebugLog.i(DEBUGKEY, "GifDrawable 被回收！");
    }

    boolean isRecycled() {
        return this.isRecycled;
    }

    /**
     * 默认无线循环-1
     * @param loopCount
     */
    public void setLoopCount(int loopCount) {
        if (loopCount <= 0 && loopCount != -1 && loopCount != 0) {
            throw new IllegalArgumentException("Loop count must be greater than 0, or -1（forever）");
        } else {
            if (loopCount == 0) {
                this.maxLoopCount = this.decoder.getLoopCount();
            } else {
                this.maxLoopCount = loopCount;
            }

        }
    }

    static class GifState extends ConstantState {
        private static final int GRAVITY = Gravity.FILL;
        GifHeader gifHeader;
        byte[] data;
        Context context;
        int targetWidth;
        int targetHeight;
        Bitmap firstFrame;

        public GifState(GifHeader header, byte[] data, Context context,
                        int targetWidth, int targetHeight,
                        Bitmap firstFrame) {
            if (firstFrame == null) {
                throw new NullPointerException("The first frame of the GIF must not be null");
            }
            gifHeader = header;
            this.data = data;
            this.firstFrame = firstFrame;
            this.context = context.getApplicationContext();
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return newDrawable();
        }

        @Override
        public Drawable newDrawable() {
            return new GifDrawable(this);
        }

        @Override
        public int getChangingConfigurations() {
            return 0;
        }

        public void clear() {
            if (this.firstFrame != null) {
                firstFrame.recycle();
                firstFrame = null;
            }
            gifHeader = null;
            data = null;
        }
    }
}
