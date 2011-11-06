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
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.content.res.Configuration;
import android.app.Activity;
import android.app.Dialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.widget.DatePicker;
import android.app.DatePickerDialog;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.view.ViewGroup; 
import android.view.ViewGroup.LayoutParams; 
import android.util.Log;
import android.util.FloatMath;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

/**
 * Wrapper activity demonstrating the use of {@link GLSurfaceView}, a view
 * that uses OpenGL drawing into a dedicated surface.
 */
public class sunTrackerActivity extends Activity {
    private Preview mPreview;
    Camera  mCamera;
    int defaultCameraId;
    int numberOfCameras;
    int cameraCurrentlyLocked;
    private SensorManager mSensorManager;
    private	DrawOnTop mDraw;
    private static final String TAG = "Sun Tracker Activity";
    private static final int CHANGE_DATE_ID = Menu.FIRST;
    private Calendar date;
    private int mYear;
    private int mMonth;
    private int mDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        date = Calendar.getInstance();
        mYear=date.get(Calendar.YEAR);
        mMonth=date.get(Calendar.MONTH);
        mDay=date.get(Calendar.DAY_OF_MONTH);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Create our Preview view and set it as the content of our
        // Activity
        mPreview = new Preview(this);
		mDraw = new DrawOnTop(this);

        // To draw draws + camera
        setContentView(mPreview);
		addContentView(mDraw, new LayoutParams
               (LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        // Find the total number of cameras available
        numberOfCameras = Camera.getNumberOfCameras();

        // Find the ID of the default camera
        CameraInfo cameraInfo = new CameraInfo();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                    defaultCameraId = i;
                }
            }
    }

    @Override
    protected void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onPause();
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }

    @Override 
        protected void onResume() {
            super.onResume();
            mCamera = Camera.open();
            cameraCurrentlyLocked = defaultCameraId;
            mPreview.setCamera(mCamera);
            mSensorManager.registerListener(mDraw,
                    SensorManager.SENSOR_ACCELEROMETER | 
                    SensorManager.SENSOR_MAGNETIC_FIELD |
                    SensorManager.SENSOR_ORIENTATION,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

    @Override 
        protected void onStop() {
            mSensorManager.unregisterListener(mDraw);
            super.onStop();
        }

    @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            super.onCreateOptionsMenu(menu);
            // Entrees du menu:
            // * Change date
            // * maj data
            // * select city
            // * favoris (mais ca peut peut etre etre sur a long touch overlayitem
            menu.add(0, CHANGE_DATE_ID, 0, R.string.menu_change_date);
            // menu.add(0, REFRESH_ID, 0, R.string.menu_refresh);
            // menu.add(0, UPDATEDB_ID, 0, R.string.menu_updatedb);
            // menu.add(0, DISP_FAVS_ID, 0, "Favs");
            return true;
        }

    // the callback received when the user "sets" the date in the dialog
    private DatePickerDialog.OnDateSetListener mDateSetListener =
        new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, 
                    int monthOfYear, int dayOfMonth) {
                date = Calendar.getInstance();
                date.set(year,monthOfYear,dayOfMonth,0,0,0);
                mDraw.changeDate(date);
                mDraw.setWillNotDraw(false);
            }
        };

    @Override
        protected Dialog onCreateDialog(int id) {
            switch (id) {
                case CHANGE_DATE_ID:
                    return new DatePickerDialog(this,
                            mDateSetListener, mYear, mMonth, mDay);
            }
            return null;
        }

    @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle all of the possible menu actions.
            switch (item.getItemId()) {
                case CHANGE_DATE_ID:
                    mDraw.setWillNotDraw(true);
                    mYear=date.get(Calendar.YEAR);
                    mMonth=date.get(Calendar.MONTH);
                    mDay=date.get(Calendar.DAY_OF_MONTH);
                    showDialog(CHANGE_DATE_ID);
                    break;
            }
            return super.onOptionsItemSelected(item);
        }

}

@SuppressWarnings("deprecation")
class DrawOnTop extends View implements SensorListener {
    private static final String TAG = "Sun Tracker View";
    private float   mLastValues[] = new float[3*2];
    private final int matrix_size = 16;
    private float   mOrientationValues[] = new float[matrix_size];
    private float   outR[]= new float[matrix_size];
    private float   inR[]= new float[matrix_size];
    private float   i[]= new float[matrix_size];
    private float   values_acc[]= new float[matrix_size];
    private float   values_mag[]= new float[matrix_size];
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
    int day_number;
    // http://engnet.anu.edu.au/DEpeople/Andres.Cuevas/Sun/help/SPguide.html
    float radius_x,radius_y;
    float fi,omega;
    float delta_0_deg;
    float delta_0;
    float delta_y_deg;
    float delta_y;
    float omega_s_0;
    float delta, delta_deg;
    float omega_s;
    float alfa, pointed_alfa=0;
    float psi, pointed_psi=0;
    /* Show different hours: */
    int graduation=24*2;
    int angle_view_x=50;
    int pointed_hour=0;
    float[] sunrise_points = new float[365];
    float[] sunset_points = new float[365];
    float[] hours_points = new float[graduation*4];
    float[] hours_points_display = new float[graduation*4];
    // private float   direction;
    public static volatile float direction = (float) 0;
    public static volatile float rolling = (float) 0;
    public static volatile float inclination = (float) 0;
    public static volatile float kFilteringFactor = (float)0.05;

    public void changeDate() {
        Calendar date = Calendar.getInstance();
        changeDate(date);
    }

	public DrawOnTop(Context context) {
		super(context);
        // Log.v(TAG,"DrawOnTop...");
        this.radius_x=200; //this.getWidth();
        this.radius_y=200; // this.getHeight();
        mColors[0] = Color.argb(192, 255, 64, 64);
        mColors[1] = Color.argb(192, 64, 128, 64);
        mColors[2] = Color.argb(192, 64, 64, 255);
        mColors[3] = Color.argb(192, 64, 255, 255);
        mColors[4] = Color.argb(192, 128, 64, 128);
        mColors[5] = Color.argb(192, 255, 255, 64);

        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mRect.set(-0.5f, -0.5f, 0.5f, 0.5f);
        mPath.arcTo(mRect, 0, 180);
        changeDate();
        for (int i=1; i < 365 ; i++)
        {
            delta_0 = (float)Math.toRadians((float)23.45 * FloatMath.sin ((float)((i - 81) * 2*(float)Math.PI / 365)));
            omega_s_0 = (float)Math.acos(-(float)Math.tan(fi) * (float)Math.tan(delta_0));
            sunrise_points[i]=(float)Math.PI+omega_s_0;
            sunset_points[i]=(float)Math.PI-omega_s_0;
        }
	}

    public void changeDate(Calendar date) {
        // Initialisation regarding the localisation.
        day_number = date.get(Calendar.DAY_OF_YEAR);
        // http://engnet.anu.edu.au/DEpeople/Andres.Cuevas/Sun/help/SPguide.html
        fi=(float)Math.toRadians((float)43.49); // to dynamise
        // fi=(float)Math.PI / 4;
        // delta = deg_to_rad((float)23.45 * FloatMath.sin ((float)((day_number + 254) * 2*(float)Math.PI / 365)));
        delta = (float)Math.toRadians((float)23.45 * FloatMath.sin ((float)((day_number - 81) * 2*(float)Math.PI / 365)));
        omega_s = (float)Math.acos(-(float)Math.tan(fi) * (float)Math.tan(delta));
        for (int hour=0; hour < graduation; hour++)
        {
            omega= (float)( (Math.PI * 2 * hour / graduation) - Math.PI);
            // omega= (float)( (Math.PI * 2 * hour / graduation) + Math.PI);
            alfa = (float)Math.asin(FloatMath.sin(delta) * FloatMath.sin(fi) + FloatMath.cos(delta) * FloatMath.cos(fi) * FloatMath.cos(omega));
            psi = 0;
            if (omega < 0) {
                psi = (float)Math.acos(
                    (FloatMath.cos(fi) * FloatMath.sin(delta) - FloatMath.cos(delta) * FloatMath.sin(fi) * FloatMath.cos(omega)
                    ) / FloatMath.cos (alfa)
                    );
            } else {
                psi = 2 * (float)Math.PI -(float)Math.acos(
                    (FloatMath.cos(fi) * FloatMath.sin(delta) - FloatMath.cos(delta) * FloatMath.sin(fi) * FloatMath.cos(omega)
                    ) / FloatMath.cos (alfa)
                   );
            }
            if (hour == 0){
                hours_points[hour*4]=psi;
                hours_points[hour*4+1]=alfa;
            } else if (hour == graduation - 1) {
                hours_points[hour*4-2]=psi;
                hours_points[hour*4-1]=alfa;
            } else {
                hours_points[hour*4-2]=psi;
                hours_points[hour*4-1]=alfa;
                hours_points[hour*4]=psi;
                hours_points[hour*4+1]=alfa;
            }
            // Log.v(TAG,"hours_points["+hour*4+"]="+hours_points[hour*4]+","+hours_points[hour+1]);
        }
    }

    private float translate_x(float angle) {
        return (float)((angle)*radius_x);
        // return (float)(Math.tan(angle)*radius_x);
    }

    private float translate_y(float angle) {
        return (float)((angle+Math.PI/2)*radius_y);
        // return (float)(Math.tan(angle)*radius_y);
    }

    public void onSensorChanged(int sensor, float[] values) {
        // Log.d(TAG, "sensor swapped  : " + sensor + ", x: " + values[0] + ", y: " + values[1] + ", z: " + values[2]);
        // Log.d(TAG, "sensor unswapped: " + sensor + ", x: " + values[3] + ", y: " + values[4] + ", z: " + values[5]);
        synchronized (this) {
            if (mBitmap != null) {
                final Canvas canvas = mCanvas;
                final Paint paint = mPaint;
                if (sensor == SensorManager.SENSOR_ORIENTATION) {
					// We have to use:
//					SensorManager.getRotationMatrix(inR, i, values_acc, values_mag);
//                  SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
//					SensorManager.getOrientation(outR, values);
//					SensorManager.getOrientation(inR, values);
					mOrientationValues = values.clone();
                    direction = (float) ((mOrientationValues[1] * kFilteringFactor) + 
                            (direction * (1.0 - kFilteringFactor)));

                    rolling = (float) ((mOrientationValues[2] * kFilteringFactor) + 
                            (rolling * (1.0 - kFilteringFactor)));

                    inclination = (float) ((mOrientationValues[3] * kFilteringFactor) + 
                            (inclination * (1.0 - kFilteringFactor)));
//					mOrientationValues[0] = (float) Math.toDegrees(values[0]);
//					mOrientationValues[1] = (float) Math.toDegrees(values[1]);
//					mOrientationValues[2] = (float) Math.toDegrees(values[2]);
//                    Log.d(TAG, "sensor swapped  : " + sensor + ", x: " + values[0] + ", y: " + values[1] + ", z: " + values[2]);
//                    Log.d(TAG, "sensor unswapped: " + sensor + ", x: " + values[3] + ", y: " + values[4] + ", z: " + values[5]);
//                    Log.d(TAG, "sensor swapped  : " + sensor + ", x: " + mOrientationValues[0] + ", y: " + mOrientationValues[1] + ", z: " + mOrientationValues[2]);
                    // Log.d(TAG, "sensor unswapped: " + sensor + ", x: " + mOrientationValues[3] + ", y: " + mOrientationValues[4] + ", z: " + mOrientationValues[5]);

                }
				/* else {
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
				*/
                invalidate();
            }
        }
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
/*
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
                    // 1/3 of screen
                    float w0 = mWidth * 0.333333f;
                    // less then 1/3 of screen
                    float w  = w0 - 32;
                    //  la half of 1/3
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
                        canvas.rotate(-values[i+3]);
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
                        canvas.rotate(-values[i+3]);
                        canvas.drawPath(path, paint);
                        canvas.restore();
                        y += h0;
                    }
                }
                */
                //paint.setStyle(Paint.Style.FILL);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.RED);
                paint.setTextSize(18);
                paint.setStrokeWidth(2);
                canvas.save(Canvas.MATRIX_SAVE_FLAG);
                canvas.translate(this.getWidth() / 2,this.getHeight() / 2);
                // canvas.drawText("X: " + mOrientationValues[1] + " Direction: "+direction, -100, 100, paint);
                // canvas.drawText("Y: " + mOrientationValues[2], -100, 120, paint);
                // canvas.drawText("Z: " + inclination + " Inclination: "+ Math.toRadians(inclination), -100, 140, paint);
                // canvas.drawText("Z: " + values[5], 10, 0, paint);
                canvas.rotate(-rolling);
                for (int i=1; i < 365 ; i++)
                {
                    paint.setColor(0xFFFFFF00);
                    canvas.drawPoint(translate_x((float)(sunrise_points[i]-Math.toRadians(inclination))),i-150,paint);
                    canvas.drawPoint(translate_x((float)(sunset_points[i]-Math.toRadians(inclination))),i-150,paint);
                }
                pointed_hour=0;
                for (int hour=0; hour < graduation*4; hour+=2)
                {
                    hours_points_display[hour]=translate_x((float)(hours_points[hour]-Math.toRadians(inclination)));
                }
                for (int hour=1; hour < graduation*4; hour+=2)
                {
                    hours_points_display[hour]=-translate_y((float)(hours_points[hour]+Math.toRadians(direction)));
                }
                for (int hour=0; hour < graduation*4; hour+=4)
                {
                    if (Math.abs(hours_points_display[hour]) < (2*Math.PI*radius_x / graduation) ) {
                        pointed_hour=hour/4;
                    }
                }
                paint.setColor(Color.GREEN);
                paint.setStrokeWidth(2);
                canvas.drawLines(hours_points_display,paint);
                paint.setColor(0xFFFF0000);
                canvas.drawText("Hour: " + (float)((float)24 * pointed_hour / (float)graduation), 10, hours_points_display[pointed_hour*4+1], paint);
                // canvas.drawText("omega_s: "+ omega_s, 10, hours_points[pointed_hour*4+1]+20, paint);
                // canvas.drawText("psi: "+ pointed_psi, 10, hours_points[pointed_hour*4+1]+40, paint);
                // canvas.drawText("alfa: "+ pointed_alfa, 10, hours_points[pointed_hour*4+1]+60, paint);
                canvas.drawLine(translate_x((float)(Math.PI+omega_s-Math.toRadians(inclination))), -this.getHeight(),
                        translate_x((float)(Math.PI+omega_s-Math.toRadians(inclination))), this.getHeight(), paint);
                paint.setColor(0xFF0000FF);
                canvas.drawLine(translate_x((float)(Math.PI-omega_s-Math.toRadians(inclination))), -this.getHeight(),
                        translate_x((float)(Math.PI-omega_s-Math.toRadians(inclination))), this.getHeight(), paint);
                /* Show different seasons: */
                for (int day=1; day < 365 ; day=day+30)
                {
                    if (day< 122) {
                        paint.setColor(Color.rgb(day%120+120,0,0));
                    }else if (day <242) {
                        paint.setColor(Color.rgb(0,day%120+120,0));
                    }else {
                        paint.setColor(Color.rgb(0,0,day%120+120));
                    }
                    delta_y = (float)Math.toRadians((float)23.45 * FloatMath.sin ((float)((day - 81) * 2 * (float)Math.PI / 365)));
                    // delta_y = Math.toRadians((float)23.45 * FloatMath.sin ((float)((day + 254) * 2 * (float)Math.PI / 365)));
                    for (int hour=0; hour <24; hour++)
                    {
                        omega=(float)( Math.PI*2*hour / 24);
                        // TODO:A regarder pourquoi les saisons sont inversees
                        alfa = (float)Math.asin(FloatMath.sin(delta_y) * FloatMath.sin(fi) + FloatMath.cos(delta_y) * FloatMath.cos(fi) * FloatMath.cos(omega));
                        if (omega < Math.PI) {
                            // First part of the day
                            psi  = 2*(float)Math.PI - (float)Math.acos(
                                (FloatMath.cos(fi) * FloatMath.sin(delta_y) - FloatMath.cos(delta_y) * FloatMath.sin(fi) * FloatMath.cos(omega)
                                 ) / FloatMath.cos (alfa));
                        } else {
                            // second part of the day
                            psi  = (float)Math.acos(
                                (FloatMath.cos(fi) * FloatMath.sin(delta_y) - FloatMath.cos(delta_y) * FloatMath.sin(fi) * FloatMath.cos(omega)
                                 ) / FloatMath.cos (alfa));
                        }
                        canvas.drawPoint(
                            translate_x((float)(psi-Math.toRadians(inclination))),
                            -translate_y((float)(alfa+Math.toRadians(direction))),
                            paint);
                    }
                }
                paint.setColor(0xFFCCCCCC);
                // Vertical line in the center of screen (to target)
                canvas.drawLine(0, -this.getHeight(), 0, this.getHeight(), paint);
                canvas.drawLine(-this.getWidth(), -translate_y((float)(Math.toRadians(direction))), this.getWidth(), -translate_y((float)(Math.toRadians(direction))), paint);
// canvas.drawText("delta: "+ delta, 10, hours_points[pointed_hour*4+1]+20, paint);
                canvas.restore();
//                canvas.save(Canvas.MATRIX_SAVE_FLAG);
//                for (int i =10; i > 360; i+=10) {
//                    canvas.rotate(-i);
//                    canvas.drawText("Z: " + i, 150, 150, paint);
//                }
//                canvas.restore();
//                canvas.drawOval(new RectF(0,   0, 50, 30), paint);
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
    Size mPreviewSize;
    List<Size> mSupportedPreviewSizes;
    Camera mCamera;

    Preview(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
      if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        mCamera.setParameters(parameters);
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

}
