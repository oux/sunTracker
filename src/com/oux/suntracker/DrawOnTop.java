package com.oux.suntracker;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.view.View;
import android.content.Context;
import android.util.FloatMath;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.util.Log;

import java.util.Calendar;

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
    float radius_x=200,radius_y=200;
    float mHorizontalAngle=1,mVerticalAngle=1;
    float fi,omega;
    float delta_y;
    float omega_s_winterSolstice;
    float omega_s_summerSolstice;
    float delta, deltaWinterSolstice, deltaSummerSolstice;
    float omega_s;
    float alfa, pointed_alfa=0;
    float psi, pointed_psi=0;
    /* Show different hours: */
    int graduation=24*4;
    int pointed_hour=0;
    int summerSolsticeDay=177;
    int winterSolsticeDay=355;
    float[] sunrise_points = new float[365];
    float[] sunset_points = new float[365];
    float pointed_hour_x;
    float pointed_hour_y;
    float[] hours_points = new float[graduation*4];
    float[] omega_points = new float[graduation];
    float[] hours_points_display = new float[graduation*4*3];
    // private float   direction;
    public static volatile float new_direction = (float) 0;
    public static volatile float direction = (float) 0;
    public static volatile float rolling = (float) 0;
    public static volatile float inclination = (float) 0;
    public static volatile float kFilteringFactor = (float)0.05;
    private float[] _accelerometerValues   = null;  //Valeur de l'accéléromètre
    private float[] _magneticValues          = null;  //Valeurs du champ magnétique

	public DrawOnTop(Context context) {
		super(context);
        // Log.v(TAG,"DrawOnTop...");
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

        deltaWinterSolstice = (float)Math.toRadians((float)23.45 * FloatMath.sin ((float)((winterSolsticeDay - 81) * 2*(float)Math.PI / 365)));
        omega_s_winterSolstice = (float)Math.acos(-(float)Math.tan(fi) * (float)Math.tan(deltaWinterSolstice));
        sunrise_points[0]=(float)Math.PI+omega_s_winterSolstice;
        sunset_points[0]=(float)Math.PI-omega_s_winterSolstice;
        deltaSummerSolstice = (float)Math.toRadians((float)23.45 * FloatMath.sin ((float)((summerSolsticeDay - 81) * 2*(float)Math.PI / 365)));
        omega_s_summerSolstice = (float)Math.acos(-(float)Math.tan(fi) * (float)Math.tan(deltaSummerSolstice));
        sunrise_points[1]=(float)Math.PI+omega_s_summerSolstice;
        sunset_points[1]=(float)Math.PI-omega_s_summerSolstice;
	}

    public void setViewAngles(float horizontalAngle, float verticalAngle) {
        mHorizontalAngle=(float)Math.toRadians(horizontalAngle);
        mVerticalAngle=(float)Math.toRadians(verticalAngle);
    }

    public void changeDate() {
        Calendar date = Calendar.getInstance();
        changeDate(date);
    }

    public void changeDate(Calendar date) {
        // Initialisation regarding the localisation.
        day_number = date.get(Calendar.DAY_OF_YEAR);
        // http://engnet.anu.edu.au/DEpeople/Andres.Cuevas/Sun/help/SPguide.html
        fi=(float)Math.toRadians((float)43.49); // to dynamise
        // fi=(float)Math.PI / 4;
        // delta = deg_to_rad((float)23.45 * FloatMath.sin ((float)((day_number + 254) * 2*(float)Math.PI / 365)));
        delta = (float)Math.toRadians((float)23.45 * FloatMath.sin ((float)((day_number - 81) * 2*(float)Math.PI / 365)));
        omega_s = (float)Math.acos(-((float)Math.tan(fi) * (float)Math.tan(delta)));
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
                // TODO: remove 2PI shift here and on direction compute if possible
                psi = 2 * (float)Math.PI -(float)Math.acos(
                    (FloatMath.cos(fi) * FloatMath.sin(delta) - FloatMath.cos(delta) * FloatMath.sin(fi) * FloatMath.cos(omega)
                    ) / FloatMath.cos (alfa)
                   );
            }
            if (hour == 0){
                omega_points[hour]=omega;
                hours_points[hour*4]=psi;
                hours_points[hour*4+1]=alfa;
            } else if (hour == graduation - 1) {
                hours_points[hour*4-2]=psi;
                hours_points[hour*4-1]=alfa;
            } else {
                omega_points[hour]=omega;
                hours_points[hour*4-2]=psi;
                hours_points[hour*4-1]=alfa;
                hours_points[hour*4]=psi;
                hours_points[hour*4+1]=alfa;
            }
            // Log.v(TAG,"hours_points["+hour*4+"]="+hours_points[hour*4]+","+hours_points[hour+1]);
        }
    }

    /*
    private null drawLineWithCheck() {
        float retval=mHeight;
        if (angle < mVerticalAngle)
            retval=(float)(Math.cos(angle)*Math.tan(angle)*mHeight);
        return retval;
        // return (float)(Math.tan(angle)*radius_y);
    }
    */

    private float translate_x(double angle) {
        float retval=1000;
        // return (float)(Math.tan(angle));
        if (angle < 0 && angle < -mHorizontalAngle) {
            retval=-mWidth;
        } else if (angle > 0 && angle > mHorizontalAngle ) {
            retval=mWidth;
        } else if ((angle > 0 && angle < mHorizontalAngle) || (angle < 0 && angle > -mHorizontalAngle)) {
            retval=(float)(Math.tan(angle)*radius_x);
            // retval=(float)(Math.cos(angle)*Math.tan(angle)*mWidth);
        }
        return retval;
        // return (float)(Math.tan(angle)*radius_x);
    }

    private float translate_y(double angle) {
        float retval=1000;
        if (angle < 0 && angle < -mVerticalAngle) {
            retval=-mHeight;
        } else if (angle > 0 && angle > mVerticalAngle ) {
            retval=mHeight;
        } else if ((angle > 0 && angle < mVerticalAngle) || (angle < 0 && angle > -mVerticalAngle)) {
            retval=(float)(Math.tan(angle)*radius_x);
            // retval=(float)(Math.cos(angle)*Math.tan(angle)*mHeight);
        }
        return retval;
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
                        //Orientation in rad
                        // Translate -PI<>+PI range to 0<>2PI range
                        if (orientation[0] < 0) {
                            new_direction=(float)(orientation[0]+2*Math.PI);
                        } else {
                            new_direction=(float)orientation[0];
                        }
                        if (new_direction >3*(Math.PI/2) && (direction < Math.PI/2) || (new_direction < Math.PI/2) && (direction > 3*(Math.PI/2))) {
                            // to avoid big slide on -PI to + PI transition:
                            direction = new_direction;
                        } else {
                            direction = (float) ((new_direction * kFilteringFactor) + 
                                    (direction * (1.0 - kFilteringFactor)));
                        }
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
//            Log.d(TAG, "Horizontal View Angle: " + mHorizontalAngle);
//            Log.d(TAG, "Vertical View Angle: " + mVerticalAngle);
//            Log.d(TAG, "Width: " + getWidth());
//            Log.d(TAG, "height: " + getHeight());
            // X * Tan(mHorizontalAngle) = mWidth
            radius_x=mWidth/(float)Math.tan(mHorizontalAngle);
            radius_y=mHeight/(float)Math.tan(mVerticalAngle);
//            Log.d(TAG, "Horizontal Radius: " + radius_x);
//            Log.d(TAG, "Vertical Radius: " + radius_y);
            super.onSizeChanged(w, h, oldw, oldh);
        }

	@Override
        protected void onDraw(Canvas canvas) {
            Paint paint = new Paint();
            if (mBitmap != null) {
                //paint.setStyle(Paint.Style.FILL);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.RED);
                paint.setTextSize(18);
                paint.setStrokeWidth(1);
//                canvas.drawText("Sunrise angle: " + (float)(sunrise_points[0]-direction), 10, 20, paint);
//                canvas.drawText("Sunset angle: " + (float)(sunset_points[0]-direction), 10, 40, paint);
//                canvas.drawText("Sunrise shift: " + (float)(translate_x(sunrise_points[0]-direction)), 10, 60, paint);
//                canvas.drawText("Sunset shift: " + (float)(translate_x(sunset_points[0]-direction)), 10, 80, paint);
                canvas.save(Canvas.MATRIX_SAVE_FLAG);
                /*
                 ********* Take care of rolling + reference on center ********
                 */
                // TODO: reference could be on the middle top of the screen:
                // canvas.translate(mWidth / 2,mHeight / 4);
                canvas.translate(mWidth / 2,mHeight / 2);
                canvas.rotate(-(float)Math.toDegrees(rolling) - 90);
                /*
                 ********* Vertical line in the center of screen (to target) ********
                 */
                paint.setColor(0xFFCCCCCC);
                canvas.drawLine(0, -mHeight, 0, mHeight, paint);
                canvas.drawLine(-mWidth, -translate_y(inclination), mWidth, -translate_y(inclination), paint);
                /*
                 ********* Display sunrise and sunset for whole year ********
                 */
                paint.setColor(0xFFFFFF00);
                paint.setStrokeWidth(3);
                canvas.drawLine(translate_x(sunrise_points[0]-direction),-translate_y(inclination),translate_x(sunrise_points[1]-direction),-translate_y(inclination),paint);
                canvas.drawLine(translate_x(sunset_points[0]-direction),-translate_y(inclination),translate_x(sunset_points[1]-direction),-translate_y(inclination),paint);

                pointed_hour=0;
                pointed_hour_x=Math.abs(hours_points_display[0]);
                for (int hour=0; hour < graduation*4; hour+=2)
                {
                    hours_points_display[hour]=translate_x(hours_points[hour]-direction);
                    hours_points_display[hour+1]=-translate_y(hours_points[hour+1]+inclination);
                    // TODO: use direction and convert to hour:minutes
                    if (Math.abs(hours_points_display[hour]) < pointed_hour_x ) {
                        pointed_hour_x=Math.abs(hours_points_display[hour]);
                        pointed_hour_y=hours_points_display[hour+1];
                        pointed_psi=hours_points[hour];
                        pointed_alfa=hours_points[hour+1];
                        pointed_hour=hour/4;
                        // Log.v(TAG,"Y["+(hour+1)+"]="+hours_points_display[hour+1]+" from:"+hours_points[hour+1]+"+"+inclination);
                    }
                }
                paint.setColor(Color.GREEN);
                paint.setStrokeWidth(2);
                /*
                 ********* Print sun path of day ********
                 */
                canvas.drawLines(hours_points_display,paint);
                paint.setColor(Color.YELLOW);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(0, hours_points_display[pointed_hour*4+1], 10, paint);
                paint.setColor(0xFFFF0000);
                canvas.drawText("Hour: " + (float)(24 * pointed_hour / (float)graduation), 20, hours_points_display[pointed_hour*4+1], paint);
                canvas.drawText("Hour: " + (float)(24 * pointed_hour / (float)graduation), 20, hours_points_display[pointed_hour*4+1], paint);
                canvas.drawText("omega_s: "+ omega_s, -10, hours_points[pointed_hour*4+1]-20, paint);
                canvas.drawText("omega: "+ omega_points[pointed_hour], -10, hours_points[pointed_hour*4+1], paint);
                canvas.drawText("direction: "+ direction, -10, hours_points[pointed_hour*4+1]+20, paint);
                canvas.drawText("alfa: "+ pointed_alfa, -10, hours_points[pointed_hour*4+1]+40, paint);
                canvas.drawText("pointed x: "+ pointed_hour_x, -10, hours_points[pointed_hour*4+1]+60, paint);
                canvas.drawText("pointed y: "+ pointed_hour_y, -10, hours_points[pointed_hour*4+1]+80, paint);
                canvas.drawLine(translate_x(Math.PI+omega_s-direction), -mHeight,
                        translate_x(Math.PI+omega_s-direction), mHeight, paint);
                paint.setColor(0xFF0000FF);
                canvas.drawLine(translate_x(Math.PI-omega_s-direction), -mHeight,
                        translate_x(Math.PI-omega_s-direction), mHeight, paint);
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
                        omega=(float)(Math.PI*2*hour / 24);
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
                        /*
                        ********* Print seasons ********
                        */
                        canvas.drawPoint(
                            translate_x(psi-direction),
                            -translate_y(alfa+inclination),
                            paint);
                    }
                }
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
