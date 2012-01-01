package com.oux.suntracker.presentation;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.view.View;
import android.content.Context;
// To optimise: instead of java.lang.Math
// import android.util.FloatMath;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.util.Log;

import java.util.Locale;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import com.oux.suntracker.models.Sun;

public class DrawOnTop extends View implements SensorEventListener {
    private static final String mTAG = "Sun Tracker View";
    private int     mColors[] = new int[3*2];
    private float   mLastX;
    private float   mScale[] = new float[2];
    private float   mYOffset;
    private float   mMaxX;
    private float   mSpeed = 1.0f;
    private Paint   mPaint = new Paint();
    private Canvas  mCanvas = new Canvas();
    private Bitmap  mBitmap;
    private int   mWidth;
    private int   mHeight;
    private int   mBottom;
    private int   mLeft;
    // http://engnet.anu.edu.au/DEpeople/Andres.Cuevas/Sun/help/SPguide.html
    float mRadiusX=200,mRadiusY=200;
    float mHorizontalAngle=1,mVerticalAngle=1;
    /* Show different hours: */
    int graduation=24*4;
    int mDefinition=5;
    Sun sunSummerSolstice;
    Sun sunWinterSolstice;
    Calendar mTargetedSunTime;
    Calendar dateSummerSolstice;
    Calendar dateWinterSolstice;
    TimeZone mLocal;
    int summerSolsticeDay=174;
    int winterSolsticeDay=355;
    Sun[] sun_points = new Sun[graduation];
    float[] hoursPointsDisplay = new float[mWidth*3];;
    private static SimpleDateFormat sdfUTC = new SimpleDateFormat("HH:mm");
    private static SimpleDateFormat sdfLocal = new SimpleDateFormat("HH:mm");
    public static volatile float new_mDirection = (float) 0;
    public static volatile float mDirection = (float) 0;
    public static volatile float mRolling = (float) 0;
    public static volatile float mInclination = (float) 0;
    public static volatile float kFilteringFactor = (float)0.05;
    // public static volatile float kFilteringFactor = (float)0.05;
    private float[] _accelerometerValues   = null;  //Valeur de l'accéléromètre
    private float[] _magneticValues          = null;  //Valeurs du champ magnétique


    /*
     * mDirection :
     *      0 = North
     *      Range = 0 ~ 2PI
     * Psi:
     *      0 = South
     *      Range = -PI ~ +PI
     * omega:
     *      0 = midi solaire
     *      Range = 0 ~ 2PI
     *      Ne jamais utiliser omega pour la conversion en X => convertir omega en psi !
     */

	public DrawOnTop(Context context) {
		super(context);
        this.setKeepScreenOn(true);
        // Log.v(mTAG,"DrawOnTop...");
        mColors[0] = Color.argb(192, 255, 64, 64);
        mColors[1] = Color.argb(192, 64, 128, 64);
        mColors[2] = Color.argb(192, 64, 64, 255);
        mColors[3] = Color.argb(192, 64, 255, 255);
        mColors[4] = Color.argb(192, 128, 64, 128);
        mColors[5] = Color.argb(192, 255, 255, 64);

        // sdfLocal.setTimeZone(TimeZone.getTimeZone("GMT+01"));
        sdfUTC.setTimeZone(TimeZone.getTimeZone("UTC"));

        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        changeDate();

        dateWinterSolstice = Calendar.getInstance();
        dateWinterSolstice.set(Calendar.DAY_OF_YEAR,winterSolsticeDay);
        sunWinterSolstice = new Sun(dateWinterSolstice);
        dateSummerSolstice = Calendar.getInstance();
        dateSummerSolstice.set(Calendar.DAY_OF_YEAR,summerSolsticeDay);
        sunSummerSolstice = new Sun(dateSummerSolstice);
	}

    public void setViewAngles(float verticalAngle, float horizontalAngle) {
        // verticalAngle = 50 ,constated = 30  60%
        // horizontalAbgle = 65, constated = 50 76%
        mHorizontalAngle=(float)Math.toRadians(horizontalAngle);
        mHorizontalAngle=(float)Math.toRadians(50);
        mVerticalAngle=(float)Math.toRadians(verticalAngle);
        mVerticalAngle=(float)Math.toRadians(30);
    }

    // Get hours angle from pixel X
    private double getAzimuthFromX(int i) {
        double angle;
        //if (i < mWidth/2) {
            angle=(Math.atan(i/mRadiusX)-Math.PI)+mDirection;
            /*
        } else {
            angle=(float)Math.PI+(float)Math.atan(i/mRadiusX)-mDirection;
        }
        */
        return angle;
    }

    // Get hours:minutes from Radian angle
    private double hourToRadian(Calendar date) {
        return (2*Math.PI*date.get(Calendar.HOUR_OF_DAY)/24)+(2*Math.PI*date.get(Calendar.MINUTE)/(24*60))-Math.PI;
    }

    // Get hours:minutes from Radian angle
    private String radianToLocalHour(double angle) {
        Date date = new Date(24*(long)Math.toDegrees(angle-Math.PI)*10*1000);
        return sdfLocal.format(date);
    }

    private int translateX(double angle) {
        int retval=1000;
        // return (float)(Math.tan(angle));
        if (angle < 0 && angle < -mHorizontalAngle) {
            retval=-mWidth;
        } else if (angle > 0 && angle > mHorizontalAngle ) {
            retval=mWidth;
        } else if ((angle >= 0 && angle < mHorizontalAngle) || (angle <= 0 && angle > -mHorizontalAngle)) {
            retval=(int)(Math.tan(angle)*mRadiusX);
            // retval=(float)(Math.cos(angle)*Math.tan(angle)*mWidth);
        }
        return retval;
        // return (float)(Math.tan(angle)*mRadiusX);
    }

    private int translateY(double angle) {
        int retval=1000;
        if (angle < 0 && angle < -mVerticalAngle) {
            retval = -mHeight;
        } else if (angle > 0 && angle > mVerticalAngle ) {
            retval = mHeight;
        } else if ((angle >= 0 && angle < mVerticalAngle) || (angle <= 0 && angle > -mVerticalAngle)) {
            retval = (int)(Math.tan(angle)*mRadiusY);
            // retval=(float)(Math.cos(angle)*Math.tan(angle)*mHeight);
        }
        return retval;
        // return (float)(Math.tan(angle)*mRadiusY);
    }

    private int getX(double azimuth) {
        return translateX(azimuth+Math.PI-mDirection);
    }

    private int getY(double altitude) {
        return -translateY(altitude+mInclination);
    }

    private int getX(Sun sun) {
        return translateX(sun.mAzimuth+Math.PI-mDirection);
    }

    private int getY(Sun sun) {
        return -translateY(sun.mAltitude+mInclination);
    }

    public void changeDate() {
        Calendar date = Calendar.getInstance();
        changeDate(date);
    }

    public void changeDate(Calendar date) {
        mLocal = TimeZone.getDefault();
        mTargetedSunTime = (Calendar) date.clone();
        mTargetedSunTime.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Initialisation regarding the localisation.
        // http://engnet.anu.edu.au/DEpeople/Andres.Cuevas/Sun/help/SPguide.html
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
                            new_mDirection = (float)(orientation[0]+2*Math.PI);
                        } else {
                            new_mDirection = (float)orientation[0];
                        }
                        if (new_mDirection >3*(Math.PI/2) && (mDirection < Math.PI/2) || (new_mDirection < Math.PI/2) && (mDirection > 3*(Math.PI/2))) {
                            // to avoid big slide on -PI to + PI transition:
                            mDirection = new_mDirection;
                        } else {
                            if (Math.abs(new_mDirection - mDirection) > 6*Math.PI/180)
                            {
                                mDirection = (float) ((new_mDirection * kFilteringFactor) +
                                        (mDirection * (1.0 - kFilteringFactor)));
                            }
                            else
                            {
                                if (Math.abs(new_mDirection - mDirection) > 2*Math.PI/180)
                                {
                                    mDirection = (float) ((new_mDirection * (kFilteringFactor/(Math.PI-Math.abs(new_mDirection-mDirection)))) +
                                            (mDirection * (1.0 - (kFilteringFactor/(Math.PI-Math.abs(new_mDirection-mDirection))))));
                                }
                            }
                        }

                        if (Math.abs(mInclination - orientation[1]) > 2*Math.PI/180)
                        {
                            mInclination = (float) ((orientation[1] * kFilteringFactor) +
                                    (mInclination * (1.0 - kFilteringFactor)));
                        }

                        if (orientation[2] > Math.PI/2 && mRolling < -Math.PI/2 || orientation[2] < -Math.PI/2 && mRolling > Math.PI/2) {
                            mRolling = orientation[2];
                        } else {
                            if (Math.abs(mRolling - orientation[2]) > 2*Math.PI/180)
                            {
                                mRolling = (float) ((orientation[2] * kFilteringFactor) +
                                        (mRolling * (1.0 - kFilteringFactor)));
                            }
                        }
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
//            Log.d(mTAG, "Horizontal View Angle: " + mHorizontalAngle);
//            Log.d(mTAG, "Vertical View Angle: " + mVerticalAngle);
//            Log.d(mTAG, "Width: " + getWidth());
//            Log.d(mTAG, "height: " + getHeight());
            // X * Tan(mHorizontalAngle) = mWidth
            mRadiusX = (mWidth/2)/(float)Math.tan(mHorizontalAngle/2);
            mRadiusY = (mHeight/2)/(float)Math.tan(mVerticalAngle/2);
            hoursPointsDisplay = new float[(int)(mWidth*5/mDefinition)];
//            Log.d(mTAG, "Horizontal Radius: " + mRadiusX);
//            Log.d(mTAG, "Vertical Radius: " + mRadiusY);
            super.onSizeChanged(w, h, oldw, oldh);
        }

	@Override
        protected void onDraw(Canvas canvas) {
            // TODO: for more economy, see : postDelayed(this, DELAY);
            int x = 0, i = 0;
            Sun sun;
            Calendar date=Calendar.getInstance();
            date.setTimeZone(TimeZone.getTimeZone("UTC"));
            Sun currentSun;
            Sun targetedSun;
            // TODO: make the choise:
            // Paint paint = mPaint;
            Paint paint = new Paint();
            if (mBitmap != null) {
                canvas.save(Canvas.MATRIX_SAVE_FLAG);
                /*
                 ********* Take care of mRolling + reference on center ********
                 */
                // TODO: reference could be on the middle top of the screen:
                // canvas.translate(mWidth / 2,mHeight / 4);
                canvas.translate(mWidth / 2,mHeight / 2);
                canvas.rotate(-(float)Math.toDegrees(mRolling) - 90);
                /*
                 ********* Vertical line in the center of screen (to target) ********
                 */
                paint.setColor(0xFFCCCCCC);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(0, -mHeight, 0, mHeight, paint);
                canvas.drawLine(-mWidth, getY(0), mWidth, getY(0), paint);
                /*
                 ********* Display sunrise and sunset for whole year ********
                 */
                paint.setColor(0xFFFFFF00);
                paint.setStrokeWidth(3);
                // TODO : Use getX and getY
                canvas.drawLine(
                        getX(sunWinterSolstice.mSunRise),
                        getY(0),
                        getX(sunSummerSolstice.mSunRise),
                        getY(0),paint);
                canvas.drawLine(
                        getX(sunWinterSolstice.mSunSet),
                        getY(0),
                        getX(sunSummerSolstice.mSunSet),
                        getY(0),paint);
                // Draw current sun position
                currentSun = new Sun(date,hourToRadian(date));
                paint.setColor(Color.YELLOW);
                paint.setStyle(Paint.Style.FILL);
                paint.setShadowLayer(2,0,0,Color.BLACK);
                canvas.drawCircle(getX(currentSun), getY(currentSun), 20, paint);
                if (currentSun.mAzimuth > 0) {
                    canvas.drawText(radianToLocalHour(currentSun.mAzimuth), getX(currentSun)+20, getY(currentSun)-20, paint);
                } else {
                    canvas.drawText(radianToLocalHour(currentSun.mAzimuth), getX(currentSun)-50, getY(currentSun)-20, paint);
                }
                sun = new Sun(date);
                for (i=0; i < (mWidth/mDefinition); i++)
                {
                    x = (i*mDefinition)-(mWidth/2);
                    sun.computeFromAzimuth(getAzimuthFromX(x));
                    if (i == 0){
                        hoursPointsDisplay[i*4]=x;
                        hoursPointsDisplay[i*4+1]=getY(sun);
                    } else if (i == (mWidth/mDefinition) - 1) {
                        hoursPointsDisplay[i*4-2]=x;
                        hoursPointsDisplay[i*4-1]=getY(sun);
                    } else {
                        hoursPointsDisplay[i*4-2]=x;
                        hoursPointsDisplay[i*4-1]=getY(sun);
                        hoursPointsDisplay[i*4]=x;
                        hoursPointsDisplay[i*4+1]=getY(sun);
                    }
                }
                canvas.drawLines(hoursPointsDisplay,paint);

                // Draw targeted sun position
                targetedSun = new Sun(mTargetedSunTime);
                // mDirection=PI on south => sun azimuth=0 on south
                targetedSun.computeFromAzimuth(mDirection - Math.PI);
                // Should be the same:
                // paint.setColor(Color.RED);
                // canvas.drawCircle(getX(targetedSun), getY(targetedSun), 20, paint);
                // canvas.drawText(radianToLocalHour(targetedSun.mAzimuth),getX(targetedSun)+20, getY(targetedSun)+20, paint);
                paint.setColor(Color.GREEN);
                if (targetedSun.mAzimuth > 0) {
                    canvas.drawText(radianToLocalHour(targetedSun.mAzimuth), -50, getY(targetedSun)+20, paint);
                } else {
                    canvas.drawText(radianToLocalHour(targetedSun.mAzimuth), 20, getY(targetedSun)+20, paint);
                }
                paint.setStrokeWidth(3);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(0, getY(targetedSun), 20, paint);

                // Draw sun Path
                // Display just necessary :
                sun = new Sun(mTargetedSunTime);
                for (i=0; i < (mWidth/mDefinition); i++)
                {
                    x = (i*mDefinition)-(mWidth/2);
                    sun.computeFromAzimuth(getAzimuthFromX(x));
                    if (i == 0){
                        hoursPointsDisplay[i*4]=x;
                        hoursPointsDisplay[i*4+1]=getY(sun);
                    } else if (i == (mWidth/mDefinition) - 1) {
                        hoursPointsDisplay[i*4-2]=x;
                        hoursPointsDisplay[i*4-1]=getY(sun);
                    } else {
                        hoursPointsDisplay[i*4-2]=x;
                        hoursPointsDisplay[i*4-1]=getY(sun);
                        hoursPointsDisplay[i*4]=x;
                        hoursPointsDisplay[i*4+1]=getY(sun);
                    }
                }
                canvas.drawLines(hoursPointsDisplay,paint);
                canvas.drawLine(getX(targetedSun.mSunRise), -mHeight,
                        getX(targetedSun.mSunRise), mHeight, paint);
                canvas.drawLine(getX(targetedSun.mSunSet), -mHeight,
                        getX(targetedSun.mSunSet), mHeight, paint);
                paint.setColor(0xFFFF0000);
                canvas.drawLine(getX(currentSun.mSunRise), -mHeight,
                        getX(currentSun.mSunRise), mHeight, paint);
                paint.setColor(0xFF0000FF);
                canvas.drawLine(getX(currentSun.mSunSet), -mHeight,
                        getX(currentSun.mSunSet), mHeight, paint);
                /* Show different seasons: */
                        /*
                for (int day=1; day < 365 ; day=day+30)
                {
                    int color=0;
                    if (day< 122) {
                        color = Color.rgb(day%120+120,0,0);
                    }else if (day <242) {
                        color = Color.rgb(0,day%120+120,0);
                    }else {
                        color = Color.rgb(0,0,day%120+120);
                    }
                    delta_y = getDelta(day);
                    // delta_y = Math.toRadians((float)23.45 * FloatMath.sin ((float)((day + 254) * 2 * (float)Math.PI / 365)));
                    for (int hour=0; hour <24; hour++)
                    {
                        omega=(float)(Math.PI*2*hour / 24);
                        // TODO:A regarder pourquoi les saisons sont inversees
                        alfa = getAlfa(delta_y, omega);
                        psi  = getPsi(delta_y, omega);
                        paint.setColor(color);
                        canvas.drawPoint(
                            translateX(psi-mDirection)-3,
                            -translateY(alfa+mInclination),
                            paint);
                        paint.setColor(Color.YELLOW);
                        canvas.drawPoint(
                            getX(day,omega)+3,
                            getY(day,omega),
                            paint);
                        if (day == 1) {
                            paint.setColor(Color.GREEN);
                            canvas.drawPoint(
                                    getX(omega)+3,
                                    getY(omega),
                                    paint);
                            canvas.drawText(String.format("%.2f",Math.toDegrees(omega)), getX(omega)+15, getY(omega)-15, paint);
                        }
                    }
                }
                */
                canvas.restore();
                String[] infos = {
                    "Sun TZ: " + mTargetedSunTime.getTimeZone().getDisplayName(),
                    "Locale Time: "+ sdfLocal.format(mTargetedSunTime.getTime()),
                    "Sun Time: " + sdfUTC.format(mTargetedSunTime.getTime()),
                    "Rolling: " + String.format("%3.0f",Math.toDegrees(mRolling))+"(" + String.format("%1.2f",mRolling)+")",
                    "Inclination: " + String.format("%3.0f",Math.toDegrees(mInclination))+"(" + String.format("%1.2f",mInclination)+")",
                    "Direction: " + String.format("%3.0f",Math.toDegrees(mDirection))+ "("+String.format("%1.2f",mDirection)+")",
                    "Targeted Azimuth: " + String.format("%3.0f",Math.toDegrees(getAzimuthFromX(0)))+ "(" + String.format("%1.2f",getAzimuthFromX(0))+")",
                    "Targeted sun azimuth: " + String.format("%3.0f",Math.toDegrees(targetedSun.mAzimuth)) + ", X:" + getX(targetedSun.mAzimuth),
                    "Targeted sun altitude: " + String.format("%3.0f",Math.toDegrees(targetedSun.mAltitude)) + ", Y:" + getY(targetedSun.mAltitude),
                };
                mBottom = mHeight;
                mLeft = 0;
                if (mRolling < -3*Math.PI/4 || mRolling > 3*Math.PI/4) {
                    canvas.save(Canvas.MATRIX_SAVE_FLAG);
                    canvas.rotate(90);
                    canvas.translate(0,-mWidth);
                    mBottom = mWidth;
                } else if (mRolling > Math.PI/4) {
                    canvas.save(Canvas.MATRIX_SAVE_FLAG);
                    canvas.rotate(-180);
                    canvas.translate(-mWidth,-mHeight);
                } else if (mRolling > -Math.PI/4) {
                    canvas.save(Canvas.MATRIX_SAVE_FLAG);
                    canvas.rotate(-90);
                    canvas.translate(-mHeight,0);
                    mBottom = mWidth;
                }
                paint.setTextSize(12);
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                paint.setShadowLayer(2,0,0,Color.BLACK);
                for(i = 0; i < infos.length; i++)
                {
                    canvas.drawText(infos[i], mLeft + 10, mBottom - 20*(i+1), paint);
                }
                canvas.restore();
            }
            super.onDraw(canvas);
        }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }
}
// vim:et:
