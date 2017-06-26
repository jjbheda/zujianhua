package org.qiyi.qyscanqrcode.scan.zxing;

import android.graphics.Bitmap;

import org.qiyi.qyscanqrcode.scan.zxing.common.BitMatrix;


public class CodeUtil {
	/**
	 * 用字符串生成二维码
	 * 
	 * @param str
	 * @return
	 * @throws WriterException
	 */
	public static Bitmap Create2DCode(String str, int codeWidth, int codeHeight)
			throws WriterException {
		// 生成二维矩阵,编码时指定大小,不要生成了图片以后再进行缩放,这样会模糊导致识别失败
		BitMatrix matrixTemp = new MultiFormatWriter().encode(str,
				BarcodeFormat.QR_CODE, codeWidth, codeHeight);
		BitMatrix	matrix = deleteWhite(matrixTemp);
		int width = matrix.getWidth();
		int height = matrix.getHeight();
		// 二维矩阵转为一维像素数组,也就是一直横着排了
		int[] pixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (matrix.get(x, y)) {
					pixels[y * width + x] = 0xff000000;
				}

			}
		}
		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		// 通过像素数组生成bitmap,具体参考api
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}

	public static BitMatrix deleteWhite(BitMatrix matrix) {
		int[] rec = matrix.getEnclosingRectangle();
		int resWidth = rec[2] + 1;
		int resHeight = rec[3] + 1;

		BitMatrix resMatrix = new BitMatrix(resWidth, resHeight);
		resMatrix.clear();
		for (int i = 0; i < resWidth; i++) {
			for (int j = 0; j < resHeight; j++) {
				if (matrix.get(i + rec[0], j + rec[1]))
					resMatrix.set(i, j);
			}
		}
		return resMatrix;
	}
}
