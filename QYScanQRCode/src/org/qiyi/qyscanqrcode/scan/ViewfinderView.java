/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qiyi.qyscanqrcode.scan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.View;

import org.qiyi.qyscanqrcode.scan.camera.CameraManager;
import org.qiyi.qyscanqrcode.scan.zxing.ResultPoint;
import org.qiyi.basecore.utils.UIUtils;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder
 * rectangle and partial transparency outside it, as well as the laser scanner
 * animation and result points.
 * 
 */
public final class ViewfinderView extends View {

	private int ScreenRate;
	private static final int CORNER_WIDTH = 5;
	private Paint paint;
	private final int maskColor;
	private Rect frame;
	
	public ViewfinderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		paint = new Paint();
		maskColor = Color.parseColor("#c3000000");
		ScreenRate = UIUtils.dip2px(20);
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (frame == null) {
			frame = CameraManager.get().getFramingRect();
		}
		if (frame == null) {
			return;
		}
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		paint.setColor(maskColor);
		//画蒙层
		canvas.save();
		canvas.clipRect(frame, Region.Op.DIFFERENCE);
		canvas.drawRect(0, 0, width, height, paint);
		canvas.restore();
		//画边角
		paint.setColor(Color.parseColor("#0bbe06"));
		canvas.drawRect(frame.left, frame.top, frame.left + ScreenRate,
				frame.top + CORNER_WIDTH, paint);
		canvas.drawRect(frame.left, frame.top, frame.left + CORNER_WIDTH, frame.top
				+ ScreenRate, paint);
		canvas.drawRect(frame.right - ScreenRate, frame.top, frame.right,
				frame.top + CORNER_WIDTH, paint);
		canvas.drawRect(frame.right - CORNER_WIDTH, frame.top, frame.right, frame.top
				+ ScreenRate, paint);
		canvas.drawRect(frame.left, frame.bottom - CORNER_WIDTH, frame.left
				+ ScreenRate, frame.bottom, paint);
		canvas.drawRect(frame.left, frame.bottom - ScreenRate,
				frame.left + CORNER_WIDTH, frame.bottom, paint);
		canvas.drawRect(frame.right - ScreenRate, frame.bottom - CORNER_WIDTH,
				frame.right, frame.bottom, paint);
		canvas.drawRect(frame.right - CORNER_WIDTH, frame.bottom - ScreenRate,
				frame.right, frame.bottom, paint);
	}

	public void addPossibleResultPoint(ResultPoint point) {
		//不做任何事
	}

}
