package org.qiyi.basecore.utils;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 图片压缩工具类（参照http://104zz.iteye.com/blog/1694762文章）
 *
 * @author lijunqing
 */
public class BitmapUtils {

    public static Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        try {
            while (baos.toByteArray().length / 1024 > 100) {    //循环判断如果压缩后图片是否大于100kb,大于继续压缩
                baos.reset();//重置baos即清空baos
                image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
                options -= 10;//每次都减少10
            }

            ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
            Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片
            return bitmap;
        } catch (Exception e) {
            image.recycle();
            try {
                baos.close();
            } catch (IOException e1) {
                ExceptionUtils.printStackTrace(e1);
            }
            //baos = null;
            return null;
        }

    }

    public static Bitmap centerCrop(Bitmap thumbnail, Point size) {

        final int tw = thumbnail.getWidth();
        final int th = thumbnail.getHeight();
        if (tw != size.x || th != size.y) {
            Bitmap bm = Bitmap.createBitmap(size.x, size.y, thumbnail.getConfig());

            // Use ScaleType.CENTER_CROP, except we leave the top edge at the top.
            float scale;
            float dx = 0, dy = 0;
            if (tw * size.x > size.y * th) {
                scale = (float) size.x / (float) th;
                dx = (size.y - tw * scale) * 0.5f;
            } else {
                scale = (float) size.y / (float) tw;
                dy = (size.x - th * scale) * 0.5f;
            }
            Matrix matrix = new Matrix();
            matrix.setScale(scale, scale);
            matrix.postTranslate((int) (dx + 0.5f), 0);

            Canvas canvas = new Canvas(bm);
            canvas.drawBitmap(thumbnail, matrix, null);
            canvas.setBitmap(null);
            thumbnail = bm;
        }
        return thumbnail;
    }

    /**
     * 将bitmap写入文件
     *
     * @param path
     * @param img
     */
    public static void saveBitmap(String path, Bitmap img) {
        FileOutputStream ops = null;
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ops = new FileOutputStream(path);
            img.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            ops.write(baos.toByteArray());
            ops.flush();
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if (ops != null) {
                FileUtils.silentlyCloseCloseable(ops);
            }
        }
    }


    /**
     * 将图片进行高斯模糊
     *
     * @param sentBitmap
     * @param radius
     * @return
     */
    public static Bitmap createBlurBitmap(Bitmap sentBitmap, int radius) {
        if (sentBitmap == null || radius < 1) {
            return (null);
        }
        Bitmap bitmap = null;
        if (sentBitmap.getConfig() != null) {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
            try {
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();
                int[] pix = new int[w * h];
                bitmap.getPixels(pix, 0, w, 0, 0, w, h);
                int wm = w - 1;
                int hm = h - 1;
                int wh = w * h;
                int div = radius + radius + 1;
                int r[] = new int[wh];
                int g[] = new int[wh];
                int b[] = new int[wh];
                int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
                int vmin[] = new int[Math.max(w, h)];
                int divsum = (div + 1) >> 1;
                divsum *= divsum;
                int dv[] = new int[256 * divsum];
                for (i = 0; i < 256 * divsum; i++) {
                    dv[i] = (i / divsum);
                }
                yw = yi = 0;
                int[][] stack = new int[div][3];
                int stackpointer;
                int stackstart;
                int[] sir;
                int rbs;
                int r1 = radius + 1;
                int routsum, goutsum, boutsum;
                int rinsum, ginsum, binsum;
                for (y = 0; y < h; y++) {
                    rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                    for (i = -radius; i <= radius; i++) {
                        p = pix[yi + Math.min(wm, Math.max(i, 0))];
                        sir = stack[i + radius];
                        sir[0] = (p & 0xff0000) >> 16;
                        sir[1] = (p & 0x00ff00) >> 8;
                        sir[2] = (p & 0x0000ff);
                        rbs = r1 - Math.abs(i);
                        rsum += sir[0] * rbs;
                        gsum += sir[1] * rbs;
                        bsum += sir[2] * rbs;
                        if (i > 0) {
                            rinsum += sir[0];
                            ginsum += sir[1];
                            binsum += sir[2];
                        } else {
                            routsum += sir[0];
                            goutsum += sir[1];
                            boutsum += sir[2];
                        }
                    }
                    stackpointer = radius;
                    for (x = 0; x < w; x++) {
                        r[yi] = dv[rsum];
                        g[yi] = dv[gsum];
                        b[yi] = dv[bsum];
                        rsum -= routsum;
                        gsum -= goutsum;
                        bsum -= boutsum;
                        stackstart = stackpointer - radius + div;
                        sir = stack[stackstart % div];
                        routsum -= sir[0];
                        goutsum -= sir[1];
                        boutsum -= sir[2];
                        if (y == 0) {
                            vmin[x] = Math.min(x + radius + 1, wm);
                        }
                        p = pix[yw + vmin[x]];
                        sir[0] = (p & 0xff0000) >> 16;
                        sir[1] = (p & 0x00ff00) >> 8;
                        sir[2] = (p & 0x0000ff);
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                        rsum += rinsum;
                        gsum += ginsum;
                        bsum += binsum;
                        stackpointer = (stackpointer + 1) % div;
                        sir = stack[(stackpointer) % div];
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                        rinsum -= sir[0];
                        ginsum -= sir[1];
                        binsum -= sir[2];
                        yi++;
                    }
                    yw += w;
                }
                for (x = 0; x < w; x++) {
                    rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                    yp = -radius * w;
                    for (i = -radius; i <= radius; i++) {
                        yi = Math.max(0, yp) + x;
                        sir = stack[i + radius];
                        sir[0] = r[yi];
                        sir[1] = g[yi];
                        sir[2] = b[yi];
                        rbs = r1 - Math.abs(i);
                        rsum += r[yi] * rbs;
                        gsum += g[yi] * rbs;
                        bsum += b[yi] * rbs;
                        if (i > 0) {
                            rinsum += sir[0];
                            ginsum += sir[1];
                            binsum += sir[2];
                        } else {
                            routsum += sir[0];
                            goutsum += sir[1];
                            boutsum += sir[2];
                        }
                        if (i < hm) {
                            yp += w;
                        }
                    }
                    yi = x;
                    stackpointer = radius;
                    for (y = 0; y < h; y++) {
                        pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];
                        rsum -= routsum;
                        gsum -= goutsum;
                        bsum -= boutsum;
                        stackstart = stackpointer - radius + div;
                        sir = stack[stackstart % div];
                        routsum -= sir[0];
                        goutsum -= sir[1];
                        boutsum -= sir[2];
                        if (x == 0) {
                            vmin[y] = Math.min(y + r1, hm) * w;
                        }
                        p = x + vmin[y];
                        sir[0] = r[p];
                        sir[1] = g[p];
                        sir[2] = b[p];
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                        rsum += rinsum;
                        gsum += ginsum;
                        bsum += binsum;
                        stackpointer = (stackpointer + 1) % div;
                        sir = stack[stackpointer];
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                        rinsum -= sir[0];
                        ginsum -= sir[1];
                        binsum -= sir[2];
                        yi += w;
                    }
                }
                bitmap.setPixels(pix, 0, w, 0, 0, w, h);
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }
        }
        return (bitmap);
    }

    /**
     * 添加遮罩
     *
     * @param bitmap 原图
     * @param color  遮罩颜色
     */
    public static void addMask(Bitmap bitmap, int color) {
        if (bitmap == null) {
            return;
        }
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Canvas canvas = new Canvas(bitmap);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        canvas.drawColor(color);
    }

    /**
     * 图片等比压缩
     *
     * @param bmp
     * @param file
     * @param minEdge 最小边
     */
    public static void compressBitmapToFile(Bitmap bmp, File file, int minEdge, int degree, int quality) {
        int curWidth = bmp.getWidth();
        int curHeight = bmp.getHeight();
        float scale;
        if (curWidth < curHeight) {
            scale = 1.0f * minEdge / curWidth;
        } else {
            scale = 1.0f * minEdge / curHeight;
        }
        // 压缩Bitmap到对应尺寸并且调整图片方向
        Matrix matrix = new Matrix();
        if (scale < 1.0f) {
            matrix.setScale(scale, scale);
        }
        matrix.postRotate(degree);
        Bitmap result = Bitmap.createBitmap(bmp, 0, 0, curWidth, curHeight, matrix, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 把压缩后的数据存放到baos中
        result.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        FileOutputStream fos = null;
        try {
             fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.flush();

        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if(fos != null) {
                try{
                    fos.close();
                }catch (IOException e){
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
    }
}

