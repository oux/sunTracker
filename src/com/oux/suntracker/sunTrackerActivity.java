/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.oux.suntracker;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.hardware.SensorManager;
import android.hardware.SensorListener;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.content.Context;
import android.hardware.Camera;
import android.graphics.PixelFormat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.View;
import android.view.ViewGroup.LayoutParams; 
import java.io.IOException;
import android.util.Log;

/**
 * Wrapper activity demonstrating the use of {@link GLSurfaceView}, a view
 * that uses OpenGL drawing into a dedicated surface.
 */
public class sunTrackerActivity extends Activity {
    private Preview mPreview;
    private SensorManager mSensorManager;
	private	DrawOnTop mDraw;
    private GLSurfaceView mGLSurfaceView;
    private static final String TAG = "Sun Tracker Activity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Create our Preview view and set it as the content of our
        // Activity
        mGLSurfaceView = new GLSurfaceView(this);
        // We want an 8888 pixel format because that's required for
        // a translucent window.
        // And we want a depth buffer.
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        // Tell the cube renderer that we want to render a translucent version
        // of the cube:
        mGLSurfaceView.setRenderer(new CubeRenderer(true));
        // Use a surface format with an Alpha channel:
        mGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        // mGLSurfaceView.setRenderer(new CubeRenderer(false));
        mPreview = new Preview(this);
		mDraw = new DrawOnTop(this);

        setContentView(mDraw);
//        setContentView(mPreview);
//		addContentView(mDraw, new LayoutParams
//                (LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        // setContentView(mGLSurfaceView, new LayoutParams
        //             (LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onPause();
        mGLSurfaceView.onPause();
    }

    @Override 
        protected void onResume() {
            super.onResume();
            mGLSurfaceView.onResume();
            mSensorManager.registerListener(mDraw,
                    SensorManager.SENSOR_ACCELEROMETER | 
                    SensorManager.Ss.getWidth() / 2,ENSOR_MAGNETIC_FIELD |
                    SensorManager.SENSOR_ORIENTATION,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

    @Override 
        protected void onStop() {
            mSensorManager.unregisterListener(mDraw);
            super.onStop();
        }

}

class DrawOnTop extends View implements SensorListener {
    private static final String TAG = "Sun Tracker View";
    private float   mLastValues[] = new float[3*2];
    private float   mOrientationValues[] = new float[3];
    private int     mColors[] = new int[3*2];
    private Path    mPath = new Path();
    private RectF   mRect = new RectF();
    private float   mLastX;
    private float   mScale[] = new float[2];
    private float   mYOffset;
    private float   mMaxX;
    private float   mSpeed = 1.0f;
    private Paint   mPaint = new Paint();
    private Canvas  mCanvas = new Canvas();
    private Bitmap  mBitmap;
    private float   mWidth;
    private float   mHeight;


    public void onSensorChanged(int sensor, float[] values) {
        // Log.d(TAG, "sensor: " + sensor + ", x: " + values[0] + ", y: " + values[1] + ", z: " + values[2]);
        synchronized (this) {
            if (mBitmap != null) {
                final Canvas canvas = mCanvas;
                final Paint paint = mPaint;
                if (sensor == SensorManager.SENSOR_ORIENTATION) {
                    for (int i=0 ; i<3 ; i++) {
                        mOrientationValues[i] = values[i];
                    }
                } else {
                    float deltaX = mSpeed;
                    float newX = mLastX + deltaX;

                    int j = (sensor == SensorManager.SENSOR_MAGNETIC_FIELD) ? 1 : 0;
                    for (int i=0 ; i<3 ; i++) {
                        int k = i+j*3;
                        final float v = mYOffset + values[i] * mScale[j];
                        paint.setColor(mColors[k]);
                        canvas.drawLine(mLastX, mLastValues[k], newX, v, paint);
                        mLastValues[k] = v;
                    }
                    if (sensor == SensorManager.SENSOR_MAGNETIC_FIELD)
                        mLastX += mSpeed;
                }
                invalidate();
            }
        }
    }


	public DrawOnTop(Context context) {
		super(context);
        mColors[0] = Color.argb(192, 255, 64, 64);
        mColors[1] = Color.argb(192, 64, 128, 64);
        mColors[2] = Color.argb(192, 64, 64, 255);
        mColors[3] = Color.argb(192, 64, 255, 255);
        mColors[4] = Color.argb(192, 128, 64, 128);
        mColors[5] = Color.argb(192, 255, 255, 64);

        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mRect.set(-0.5f, -0.5f, 0.5f, 0.5f);
        mPath.arcTo(mRect, 0, 180);
	}

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
            mCanvas.setBitmap(mBitmap);
            mCanvas.drawColor(0x5555);
            mYOffset = h * 0.5f;
            mScale[0] = - (h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
            mScale[1] = - (h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
            mWidth = w;
            mHeight = h;
            if (mWidth < mHeight) {
                mMaxX = w;
            } else {
                mMaxX = w-50;
            }
            mLastX = mMaxX;
            super.onSizeChanged(w, h, oldw, oldh);
        }

	@Override
        protected void onDraw(Canvas canvas) {
            Paint paint = new Paint();

            if (mBitmap != null) {
                final Path path = mPath;
                final int outer = 0xFFC0C0C0;
                final int inner = 0xFFff7010;

                if (mLastX >= mMaxX) {
                    mLastX = 0;
                    final Canvas cavas = mCanvas;
                    final float yoffset = mYOffset;
                    final float maxx = mMaxX;
                    final float oneG = SensorManager.STANDARD_GRAVITY * mScale[0];
                    paint.setColor(0xFFAAAAAA);
                    cavas.drawColor(0xFFFFFFFF);
                    cavas.drawLine(0, yoffset,      maxx, yoffset,      paint);
                    cavas.drawLine(0, yoffset+oneG, maxx, yoffset+oneG, paint);
                    cavas.drawLine(0, yoffset-oneG, maxx, yoffset-oneG, paint);
                }

                float[] values = mOrientationValues;
                if (mWidth < mHeight) {
                    // 1/3 de l'écran
                    float w0 = mWidth * 0.333333f;
                    // un peu moins d'1/3 de l'écran
                    float w  = w0 - 32;
                    //  la moitier du 1/3
                    float x = w0*0.5f;
                    for (int i=2 ; i<3 ; i++) {

                        canvas.save(Canvas.MATRIX_SAVE_FLAG);
                        // x = w0/2 + n * w0
                        canvas.translate(x, w*0.5f + 4.0f);
                        
                        canvas.save(Canvas.MATRIX_SAVE_FLAG);
                        paint.setColor(outer);
                        canvas.scale(w, w);
                        canvas.drawOval(mRect, paint);
                        canvas.restore();
                        
                        canvas.scale(w-5, w-5);
                        paint.setColor(inner);
                        canvas.rotate(-values[i]);
                        canvas.drawPath(path, paint);
                        
                        canvas.restore();
                        x += w0;
                    }
                } else {
                    float h0 = mHeight * 0.333333f;
                    float h  = h0 - 32;
                    float y = h0*0.5f;
                    for (int i=0 ; i<3 ; i++) {
                        canvas.save(Canvas.MATRIX_SAVE_FLAG);
                        canvas.translate(mWidth - (h*0.5f + 4.0f), y);
                        canvas.save(Canvas.MATRIX_SAVE_FLAG);
                        paint.setColor(outer);
                        canvas.scale(h, h);
                        canvas.drawOval(mRect, paint);
                        canvas.restore();
                        canvas.scale(h-5, h-5);
                        paint.setColor(inner);
                        canvas.rotate(-values[i]);
                        canvas.drawPath(path, paint);
                        canvas.restore();
                        y += h0;
                    }
                }
                //paint.setStyle(Paint.Style.FILL);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.RED);
                paint.setTextSize(18);
                paint.setStrokeWidth(2);
//                canvas.drawText("X: " + mOrientationValues[1], 20, 40, paint);
//                canvas.drawText("Y: " + mOrientationValues[2], 20, 60, paint);
                canvas.save(Canvas.MATRIX_SAVE_FLAG);
                canvas.translate(this.getWidth() / 2,this.getHeight() / 2);
                canvas.drawText("Z: " + mOrientationValues[0], 20, 20, paint);
                canvas.drawText("Z: " + values[0], 0, 0, paint);
                canvas.rotate(-values[2]);
                canvas.drawLine(0, -this.getHeight(), 0, this.getHeight(), paint);
                canvas.restore();
//                canvas.save(Canvas.MATRIX_SAVE_FLAG);
//                for (int i =10; i > 360; i+=10) {
//                    canvas.rotate(-i);
//                    canvas.drawText("Z: " + i, 150, 150, paint);
//                }
//                canvas.restore();
//                canvas.drawArc(new RectF(0,   0, 50, 30), 0, 30, false, paint);
//                canvas.drawArc(new RectF(0,  50, 50, 100), 90, 90, false, paint);
//                canvas.drawArc(new RectF(0, 100, 50, 130), 90, 180, false, paint);
//                canvas.drawArc(new RectF(0, 150, 50, 200), 180, 270, false, paint);
//                canvas.drawArc(new RectF(0, 200, 50, 230), 270, 0, false, paint);
//                canvas.drawArc(new RectF(0, 250, 50, 300), 120, 270, false, paint);
            }

            super.onDraw(canvas);
        }

    public void onAccuracyChanged(int sensor, int accuracy) {
        // TODO Auto-generated method stub
    }
}

class Preview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "Sun Tracker Preview";
    SurfaceHolder mHolder;
    Camera mCamera;

    Preview(Context context) {
        super(context);
        
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        mCamera = Camera.open();
        try {
           mCamera.setPreviewDisplay(holder);
        } catch (IOException exception) {
            mCamera.release();
            mCamera = null;
            // TODO: add more exception handling logic here
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        // Log.d("Matrix:" + Camera.getMatrix());
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(w, h);
        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

}
