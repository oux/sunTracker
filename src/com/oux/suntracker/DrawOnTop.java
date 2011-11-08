package com.oux.suntracker;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.view.View;
import android.content.Context;
import java.util.Calendar;
import android.util.FloatMath;

class DrawOnTop extends View implements SensorEventListener {
    private static final String TAG = "Sun Tracker View";
    private float   mLastValues[] = new float[3*2];
    private final int matrix_size = 16;
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
    private float[] _accelerometerValues   = null;  //Valeur de l'accéléromètre
    private float[] _magneticValues          = null;  //Valeurs du champ magnétique

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
        return (float)((angle)*radius_y);
        // return (float)(Math.tan(angle)*radius_y);
    }

    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            if (mBitmap != null) {
                final Canvas canvas = mCanvas;
                final Paint paint = mPaint;
                //Récupération des valeurs
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                    _accelerometerValues = event.values.clone();
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                    _magneticValues = event.values.clone();

                if (_accelerometerValues != null && _magneticValues != null)
                {
                    // http://blogah.arvyoo.com/2011/02/android-obtenir-les-valeurs-dinclinaisons-du-smartphone/
                    float inR[] = new float[9]; //Notre matrice de rotation
                    float outR[] = new float[9]; //Notre matrice de rotation dans un autre système de coordonnée
                    float I[] = new float[9];
                    if (SensorManager.getRotationMatrix(inR, I, _accelerometerValues, _magneticValues)
                            && SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR))
                    {
                        float orientation[] = new float[3];
                        orientation = SensorManager.getOrientation(outR, orientation);
                        //Transformation en degrées
                        direction = (float) ((orientation[0] * kFilteringFactor) + 
                                (direction * (1.0 - kFilteringFactor)));
                        inclination = (float) ((orientation[1] * kFilteringFactor) + 
                                (inclination * (1.0 - kFilteringFactor)));
                        rolling = (float) ((orientation[2] * kFilteringFactor) + 
                                (rolling * (1.0 - kFilteringFactor)));
                    }
                }
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
//                canvas.drawText("Direction: " + (float)(direction), 10, 20, paint);
//                canvas.drawText("Inclination: " + (float)(inclination), 10, 40, paint);
//                canvas.drawText("Rolling: " + (float)(rolling), 10, 60, paint);
                canvas.save(Canvas.MATRIX_SAVE_FLAG);
                canvas.translate(this.getWidth() / 2,this.getHeight() / 2);
                canvas.rotate(-(float)Math.toDegrees(rolling));
                for (int i=1; i < 365 ; i++)
                {
                    paint.setColor(0xFFFFFF00);
                    canvas.drawPoint(i-150,translate_x((float)(sunrise_points[i]-direction)),paint);
                    canvas.drawPoint(i-150,translate_x((float)(sunset_points[i]-direction)),paint);
                }
                pointed_hour=0;
                for (int hour=0; hour < graduation*4; hour+=2)
                {
                    hours_points_display[hour]=translate_x((float)(hours_points[hour]-direction));
                }
                for (int hour=1; hour < graduation*4; hour+=2)
                {
                    hours_points_display[hour]=-translate_y((float)(hours_points[hour]+inclination));
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
                canvas.drawLine(translate_x((float)(Math.PI+omega_s-direction)), -this.getHeight(),
                        translate_x((float)(Math.PI+omega_s-direction)), this.getHeight(), paint);
                paint.setColor(0xFF0000FF);
                canvas.drawLine(translate_x((float)(Math.PI-omega_s-direction)), -this.getHeight(),
                        translate_x((float)(Math.PI-omega_s-direction)), this.getHeight(), paint);
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
                            translate_x((float)(psi-inclination)),
                            -translate_y((float)(alfa+direction)),
                            paint);
                    }
                }
                paint.setColor(0xFFCCCCCC);
                // Vertical line in the center of screen (to target)
                canvas.drawLine(0, -this.getHeight(), 0, this.getHeight(), paint);
                canvas.drawLine(-this.getWidth(), -translate_y(direction), this.getWidth(), -translate_y(direction), paint);
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

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }
}
