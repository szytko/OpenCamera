/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.almalence.util;

import java.io.Closeable;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.CameraScreenActivity;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

/**
 * Collection of utility functions used in this package.
 */
public final class Util
{

	// Orientation hysteresis amount used in rounding, in degrees
	private static final int	ORIENTATION_HYSTERESIS		= 5;

	private static Matrix		mMeteringMatrix				= new Matrix();

	private Util()
	{
	}

	public static void initialize(Context context)
	{
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(metrics);
	}

	// Rotates the bitmap by the specified degree.
	// If a new bitmap is created, the original bitmap is recycled.
	public static Bitmap rotate(Bitmap b, int degrees)
	{
		return rotateAndMirror(b, degrees, false);
	}

	// Rotates and/or mirrors the bitmap. If a new bitmap is created, the
	// original bitmap is recycled.
	public static Bitmap rotateAndMirror(Bitmap b, int degrees, boolean mirror)
	{
		if ((degrees != 0 || mirror) && b != null)
		{
			Matrix m = new Matrix();
			// Mirror first.
			// horizontal flip + rotation = -rotation + horizontal flip
			if (mirror)
			{
				m.postScale(-1, 1);
				degrees = (degrees + 360) % 360;
				if (degrees == 0 || degrees == 180)
				{
					m.postTranslate((float) b.getWidth(), 0);
				} else if (degrees == 90 || degrees == 270)
				{
					m.postTranslate((float) b.getHeight(), 0);
				} else
				{
					throw new IllegalArgumentException("Invalid degrees=" + degrees);
				}
			}
			if (degrees != 0)
			{
				// clockwise
				m.postRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
			}

			try
			{
				Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
				if (b != b2)
				{
					b.recycle();
					b = b2;
				}
			} catch (OutOfMemoryError ex)
			{
				// We have no memory to rotate. Return the original bitmap.
			}
		}
		return b;
	}

	private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels)
	{
		double w = options.outWidth;
		double h = options.outHeight;

		int lowerBound = (maxNumOfPixels < 0) ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
		int upperBound = (minSideLength < 0) ? 128 : (int) Math.min(Math.floor(w / minSideLength),
				Math.floor(h / minSideLength));

		if (upperBound < lowerBound)
		{
			// return the larger one when there is no overlapping zone.
			return lowerBound;
		}

		if (maxNumOfPixels < 0 && minSideLength < 0)
		{
			return 1;
		} else if (minSideLength < 0)
		{
			return lowerBound;
		} else
		{
			return upperBound;
		}
	}


	public static void closeSilently(Closeable c)
	{
		if (c == null)
			return;
		try
		{
			c.close();
		} catch (Exception t)
		{
		}
	}


	public static int clamp(int x, int min, int max)
	{
		if (x > max)
			return max;
		if (x < min)
			return min;
		return x;
	}


	public static int roundOrientation(int orientation, int orientationHistory)
	{
		boolean changeOrientation = false;
		if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN)
		{
			changeOrientation = true;
		} else
		{
			int dist = Math.abs(orientation - orientationHistory);
			dist = Math.min(dist, 360 - dist);
			changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
		}
		if (changeOrientation)
		{
			return ((orientation + 45) / 90 * 90) % 360;
		}
		return orientationHistory;
	}



	public static void rectFToRect(RectF rectF, Rect rect)
	{
		rect.left = Math.round(rectF.left);
		rect.top = Math.round(rectF.top);
		rect.right = Math.round(rectF.right);
		rect.bottom = Math.round(rectF.bottom);
	}

	public static void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation, int viewWidth,
			int viewHeight)
	{
		// Need mirror for front camera.
		matrix.setScale(mirror ? -1 : 1, 1);
		// This is the value for android.hardware.Camera.setDisplayOrientation.
		matrix.postRotate(displayOrientation);
		// Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
		// UI coordinates range from (0, 0) to (width, height).
		matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
		matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
	}

	public static boolean isNumeric(String str)
	{
		NumberFormat formatter = NumberFormat.getInstance();
		ParsePosition pos = new ParsePosition(0);
		formatter.parse(str, pos);
		return str.length() == pos.getIndex();
	}

	public static long FreeDeviceMemory()
	{
		StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
		long blockSize = statFs.getBlockSize();
		long availableBloks = statFs.getAvailableBlocks();
		return (availableBloks * blockSize) / 1048576;
	}

	public static long AvailablePictureCount()
	{
		long freeMemory = Util.FreeDeviceMemory() - 5;
		if (freeMemory < 5)
			return 0;

		// RAW size of picture is width*height*3 (rgb). JPEG compress picture on
		// average 86% (compress quality 95)
		CameraController.Size saveImageSize = CameraController.getCameraImageSize();
		double imageSize = ((saveImageSize.getWidth() * saveImageSize.getHeight() * 3 * 0.14) / 1048576d);
		if (imageSize == 0)
			return 0;

		return Math.round(freeMemory / imageSize);
	}

	public static void initializeMeteringMatrix()
	{
		Matrix matrix = new Matrix();
		Util.prepareMatrix(matrix, CameraController.isFrontCamera(), 0, CameraScreenActivity.getPreviewWidth(),
				CameraScreenActivity.getPreviewHeight());
		matrix.invert(mMeteringMatrix);
	}

	public static Rect convertToDriverCoordinates(Rect rect)
	{
		RectF rectF = new RectF(rect.left, rect.top, rect.right, rect.bottom);
		mMeteringMatrix.mapRect(rectF);
		Util.rectFToRect(rectF, rect);

		if (rect.left < -1000)
			rect.left = -1000;
		if (rect.left > 1000)
			rect.left = 1000;

		if (rect.right < -1000)
			rect.right = -1000;
		if (rect.right > 1000)
			rect.right = 1000;

		if (rect.top < -1000)
			rect.top = -1000;
		if (rect.top > 1000)
			rect.top = 1000;

		if (rect.bottom < -1000)
			rect.bottom = -1000;
		if (rect.bottom > 1000)
			rect.bottom = 1000;

		return rect;
	}

	public static String toString(final Object[] objects, final char separator)
	{
		final StringBuilder stringBuilder = new StringBuilder();

		for (final Object object : objects)
		{
			stringBuilder.append(object.toString());
			stringBuilder.append(separator);
		}

		return stringBuilder.toString();
	}

	public static boolean shouldRemapOrientation(final int orientationProc, final int rotation)
	{
		return (orientationProc == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_0)
				|| (orientationProc == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_180)
				|| (orientationProc == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_90)
				|| (orientationProc == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_270);
	}
}
