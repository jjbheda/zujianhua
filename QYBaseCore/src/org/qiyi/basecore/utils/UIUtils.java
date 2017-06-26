package org.qiyi.basecore.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import org.qiyi.android.corejar.debug.DebugLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UIUtils {
    // 全局Context，用于获取Resource
    @SuppressLint("StaticFieldLeak")
    private static Context sContext = null;

    public static void init(Context context) {
        sContext = context.getApplicationContext();
    }

    public static Bitmap resource2Bitmap(Context mContext, int id) {
        return null == mContext ? null : BitmapFactory.decodeResource(mContext.getResources(), id);
    }


    public static BitmapNull resource2BitmapNull(Context mContext, int id) {

        if (null == mContext) {
            return new BitmapNull(0, 0);
        }

        Bitmap temp = BitmapFactory.decodeResource(mContext.getResources(), id);

        if (temp == null) {
            return new BitmapNull(0, 0);
        }

        BitmapNull ret = new BitmapNull(temp.getWidth(), temp.getHeight());

        temp.recycle();
        temp = null;

        return ret;
    }

    public static int computeSampleSize(BitmapFactory.Options options,
                                        int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength,
                maxNumOfPixels);
        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options,
                                                int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels == -1) ? 1 :
                (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 :
                (int) Math.min(Math.floor(w / minSideLength),
                        Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }

        if ((maxNumOfPixels == -1) &&
                (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    public static Bitmap zoomBitmap(Context context, byte[] _b) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int w = displayMetrics.widthPixels;
        int h = displayMetrics.heightPixels;
        int d = displayMetrics.densityDpi;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;

        try {
            BitmapFactory.decodeByteArray(_b, 0, _b.length, opts);

            //int x = 2;
            int x = computeSampleSize(opts, w > h ? w : h, w * h);
            opts.inTargetDensity = d;
            opts.inSampleSize = x;
            opts.inJustDecodeBounds = false;

            opts.inDither = false;
            opts.inPurgeable = true;

            return BitmapFactory.decodeByteArray(_b, 0, _b.length, opts);
        } catch (OutOfMemoryError e) {
            ExceptionUtils.printStackTrace(e);
            System.gc();
            //			VMRuntime.getRuntime().setTargetHeapUtilization(0.75f);
        }
        return null;
    }



    public static View inflateView(Context context, int resource, ViewGroup root) {
        View ret = null;
        try {
            ret = View.inflate(context, resource, root);
        } catch (Throwable e) {
            if (DebugLog.isDebug()) {
                ExceptionUtils.printStackTrace(e);
                throw new RuntimeException(e);
            }
        }
        return ret;
    }


    /**
     * dip转px值
     */
    public static int dip2px(@Nullable Context ctx, float dipValue) {
        if (ctx == null) {
            return dip2px(dipValue);
        }
        final float scale = ctx.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * dip转px值
     */
    public static int dip2px(float dipValue) {
        float scale;
        if (sContext != null) {
            scale = sContext.getResources().getDisplayMetrics().density;
        } else {
            scale = Resources.getSystem().getDisplayMetrics().density;
        }

        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * px转dip
     */
    public static int px2dip(@Nullable Context ctx, float pxValue) {
        if (ctx == null) {
            return px2dip(pxValue);
        }
        final float scale = ctx.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * px转dip
     */
    public static int px2dip(float pxValue) {
        float scale;
        if (sContext != null) {
            scale = sContext.getResources().getDisplayMetrics().density;
        } else {
            scale = Resources.getSystem().getDisplayMetrics().density;
        }

        return (int) (pxValue / scale + 0.5f);
    }

    public static BitmapFactory.Options getBitmapOption(Context mContext) {
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inScaled = true;
        option.inDensity = DisplayMetrics.DENSITY_DEFAULT;
        option.inTargetDensity = mContext.getResources().getDisplayMetrics().densityDpi;

        return option;
    }



    public static byte[] Bitmap2Bytes(Bitmap _b) {
        if (null == _b) {
            return null;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        _b.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    public static Bitmap drawable2Bitmap(Drawable _d) {
        return null == _d ? null : ((BitmapDrawable) _d).getBitmap();
    }

    /**
     * liuzm
     * 获取边界压缩的bitmap流
     *
     * @param context
     * @param _b
     * @return
     */
    public static Bitmap byteArray2ImgBitmap(Context context, byte[] _b) {
        return zoomBitmap(context, _b);
    }

    public static Bitmap resizeImage(Bitmap bitmap, int w, int h) {

        // load the origial Bitmap
        Bitmap BitmapOrg = bitmap;

        try {
            int width = BitmapOrg.getWidth();
            int height = BitmapOrg.getHeight();
            int newWidth = w;
            int newHeight = h;

            // calculate the scale
            float scaleWidth = ((float) newWidth) / width;
            float scaleHeight = ((float) newHeight) / height;

            // create a matrix for the manipulation
            Matrix matrix = new Matrix();
            // resize the Bitmap
            matrix.postScale(scaleWidth, scaleHeight);
            // if you want to rotate the Bitmap
            // matrix.postRotate(45);

            // recreate the new Bitmap
            Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
                    height, matrix, true);

            // make a Drawable from Bitmap to allow to set the Bitmap
            // to the ImageView, ImageButton or what ever
            return resizedBitmap;
        } catch (Throwable e) {
            if (DebugLog.isDebug()) {
                throw new RuntimeException(e);
            }
        }

        return BitmapOrg;
    }

    public static void showSoftKeyboard(Activity mActivity) {
        InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (null != mActivity.getCurrentFocus()) {
            imm.showSoftInput(mActivity.getCurrentFocus(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static void hideSoftkeyboard(Activity mActivity) {
        if (null != mActivity && null != mActivity.getCurrentFocus()) {
            InputMethodManager mInputMethodManager = (InputMethodManager) mActivity.getSystemService(Context
                    .INPUT_METHOD_SERVICE);
            if (null != mInputMethodManager) {
                mInputMethodManager.hideSoftInputFromWindow(mActivity.getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }


    /**
     * @author dragon
     *         只用于保存图片大小
     */
    public static class BitmapNull {
        private int mWidth = 0;
        private int mHeight = 0;

        public BitmapNull(int w, int h) {
            mWidth = w;
            mHeight = h;
        }

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }
    }

    /**
     * 创建图片Uri
     */
    public static Uri getImageUri(Activity activity) {
        //获取系统相册存储地址
        Uri uri_DCIM = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String DCIMPath = "";
        Cursor cr = activity.getContentResolver().query(uri_DCIM,
                new String[]{MediaStore.Images.Media.DATA}, null, null,
                MediaStore.Images.Media.DATE_MODIFIED + " desc");

        if (null == cr) {
            return null;
        }

        if (cr.moveToNext()) {
            DCIMPath = cr.getString(cr.getColumnIndex(MediaStore.Images.Media.DATA));
        }

        cr.close();
        DCIMPath = DCIMPath.substring(0, DCIMPath.lastIndexOf("/") + 1);

        //使用系统当前日期加以调整作为照片的名称
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "'IMG'_yyyyMMdd_HHmmss");
        String picName = dateFormat.format(date) + ".png";

        String IMAGE_FILE_LOCATION = "file://" + DCIMPath + picName;
        return Uri.parse(IMAGE_FILE_LOCATION);
    }

    /**
     * 读取图片属性：旋转的角度
     *
     * @param path 图片绝对路径
     * @return degree旋转的角度
     */
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface
                    .ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        }
        return degree;
    }

    /*
      * 旋转图片
      * @param angle
      * @param bitmap
      * @return Bitmap
      */
    public static Bitmap rotaingImageView(int angle, Bitmap bitmap) {
        //旋转图片 动作
        Matrix matrix = new Matrix();
        ;
        matrix.postRotate(angle);
        // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizedBitmap;
    }


    /**
     * 转换图片成圆形
     *
     * @param source 传入Bitmap对象
     * @return
     */
    public static Bitmap toRoundBitmap(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int min = Math.min(width, height);
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        Bitmap target = Bitmap.createBitmap(min, min, Config.ARGB_8888);
        /**
         * 产生一个同样大小的画布
         */
        Canvas canvas = new Canvas(target);
        /**
         * 首先绘制圆形
         */
        canvas.drawCircle(min / 2.0f, min / 2.0f, min / 2.0f, paint);
        /**
         * 使用SRC_IN，两个绘制的效果叠加后取交集展现后图
         */
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        /**
         * 绘制图片
         */
        canvas.drawBitmap(source, 0, 0, paint);
        return target;
    }

    public static boolean isKeyboardShowing(Context context) {
        boolean isShowing = false;

        try {
            isShowing = ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).isAcceptingText();
        } catch (Exception e) {
            if (DebugLog.isDebug()) {
                ExceptionUtils.printStackTrace(e);
            }
        }

        DebugLog.v(UIUtils.class.getSimpleName(), "isKeyboardShowing: " + isShowing);

        return isShowing;
    }

    /**
     * 获取导航栏高度
     */
    public static int getNaviHeight(Context context) {
        return dip2px(context, 50);
    }

    /**
     * 获取Android状态栏的高度
     *
     * @param activity
     * @return
     */
    public static int getStatusBarHeight(Activity activity) {
        int result = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = activity.getResources().getDimensionPixelSize(resourceId);
        }
        if (result <= 0) {
            Rect rectangle = new Rect();
            Window window = activity.getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
            int statusBarHeight = rectangle.top;
            int contentViewTop =
                    window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            result = contentViewTop - statusBarHeight;
        }
        return result;
    }

}
