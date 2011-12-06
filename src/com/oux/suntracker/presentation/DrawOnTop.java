package com.oux.suntracker.presentation;

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
    // http://engnet.anu.edu.au/DEpeople/Andres.Cuevas/Sun/help/SPguide.html
    float mRadiusX=200,mRadiusY=200;
    float mHorizontalAngle=1,mVerticalAngle=1;
    /* Show different hours: */
    int graduation=24*4;
    int mDefinition=5;
    Sun sunSummerSolstice;
    Sun sunWinterSolstice;
    Calendar mTargetedDate;
    Calendar dateSummerSolstice;
    Calendar dateWinterSolstice;
    int summerSolsticeDay=174;
    int winterSolsticeDay=355;
    Sun[] sun_points = new Sun[graduation];
    float[] hoursPointsDisplay = new float[mWidth*3];;
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
        // Log.v(mTAG,"DrawOnTop...");
        mColors[0] = Color.argb(192, 255, 64, 64);
        mColors[1] = Color.argb(192, 64, 128, 64);
        mColors[2] = Color.argb(192, 64, 64, 255);
        mColors[3] = Color.argb(192, 64, 255, 255);
        mColors[4] = Color.argb(192, 128, 64, 128);
        mColors[5] = Color.argb(192, 255, 255, 64);

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
        mHorizontalAngle=(float)Math.toRadians(horizontalAngle);
        mVerticalAngle=(float)Math.toRadians(verticalAngle);
    }

    // Get hours angle from pixel X
    private float getAzimuthFromX(int i) {
        float angle;
        //if (i < mWidth/2) {
            angle=(float)(Math.atan(i/mRadiusX)-Math.PI)+mDirection;
            /*
        } else {
            angle=(float)Math.PI+(float)Math.atan(i/mRadiusX)-mDirection;
        }
        */
        return angle;
    }

    // Get hours:minutes from Radian angle
    private float hourToRadian(Calendar date) {
        return (float)(2*Math.PI*date.get(Calendar.HOUR_OF_DAY)/24)+(float)(2*Math.PI*date.get(Calendar.MINUTE)/(24*60))-(float)Math.PI;
    }

    // Get hours:minutes from Radian angle
    private String radianToHour(float angle) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        Date date = new Date(24*(long)Math.toDegrees(angle)*10*1000);
        return sdf.format(date);
    }

    private int translateX(double angle) {
        int retval=1000;
        // return (float)(Math.tan(angle));
        if (angle < 0 && angle < -mHorizontalAngle) {
            retval=-mWidth;
        } else if (angle > 0 && angle > mHorizontalAngle ) {
            retval=mWidth;
        } else if ((angle > 0 && angle < mHorizontalAngle) || (angle < 0 && angle > -mHorizontalAngle)) {
            retval=(int)(Math.tan(angle)*mRadiusX);
            // retval=(float)(Math.cos(angle)*Math.tan(angle)*mWidth);
        }
        return retval;
        // return (float)(Math.tan(angle)*mRadiusX);
    }

    private int translateY(double angle) {
        int retval=1000;
        if (angle < 0 && angle < -mVerticalAngle) {
            retval=-mHeight;
        } else if (angle > 0 && angle > mVerticalAngle ) {
            retval=mHeight;
        } else if ((angle > 0 && angle < mVerticalAngle) || (angle < 0 && angle > -mVerticalAngle)) {
            retval=(int)(Math.tan(angle)*mRadiusY);
            // retval=(float)(Math.cos(angle)*Math.tan(angle)*mHeight);
        }
        return retval;
        // return (float)(Math.tan(angle)*mRadiusY);
    }

    private int getX(float azimuth) {
        return translateX(azimuth+(float)Math.PI-mDirection);
    }

    private int getY(float altitude) {
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
        mTargetedDate = date;
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
                            new_mDirection=(float)(orientation[0]+2*Math.PI);
                        } else {
                            new_mDirection=(float)orientation[0];
                        }
                        if (new_mDirection >3*(Math.PI/2) && (mDirection < Math.PI/2) || (new_mDirection < Math.PI/2) && (mDirection > 3*(Math.PI/2))) {
                            // to avoid big slide on -PI to + PI transition:
                            mDirection = new_mDirection;
                        } else {
                            mDirection = (float) ((new_mDirection * kFilteringFactor) +
                                    (mDirection * (1.0 - kFilteringFactor)));
                        }
                        mInclination = (float) ((orientation[1] * kFilteringFactor) +
                                (mInclination * (1.0 - kFilteringFactor)));
                        mRolling = (float) ((orientation[2] * kFilteringFactor) +
                                (mRolling * (1.0 - kFilteringFactor)));
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
            mRadiusX=(mWidth/2)/(float)Math.tan(mHorizontalAngle/2);
            mRadiusY=(mHeight/2)/(float)Math.tan(mVerticalAngle/2);
            hoursPointsDisplay= new float[(int)(mWidth*3/mDefinition)];
//            Log.d(mTAG, "Horizontal Radius: " + mRadiusX);
//            Log.d(mTAG, "Vertical Radius: " + mRadiusY);
            super.onSizeChanged(w, h, oldw, oldh);
        }

	@Override
        protected void onDraw(Canvas canvas) {
            // TODO: for more economy, see : postDelayed(this, DELAY);
            int x=0,i=0;
            Calendar date=Calendar.getInstance();;
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
                canvas.drawLine(-mWidth, -translateY(mInclination), mWidth, -translateY(mInclination), paint);
                /*
                 ********* Display sunrise and sunset for whole year ********
                 */
                paint.setColor(0xFFFFFF00);
                paint.setStrokeWidth(3);
                // TODO : Use getX and getY
                canvas.drawLine(
                        translateX(sunWinterSolstice.mSunRise-mDirection),
                        -translateY(mInclination),
                        translateX(sunSummerSolstice.mSunRise-mDirection),
                        -translateY(mInclination),paint);
                canvas.drawLine(
                        translateX(sunWinterSolstice.mSunSet-mDirection),
                        -translateY(mInclination),
                        translateX(sunSummerSolstice.mSunSet-mDirection),
                        -translateY(mInclination),paint);
                // Draw current sun position
                currentSun=new Sun(Calendar.getInstance(),hourToRadian(date));
                paint.setColor(Color.YELLOW);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(getX(currentSun), getY(currentSun), 20, paint);
                canvas.drawText(radianToHour(currentSun.mAzimuth),getX(currentSun)+20, getY(currentSun)-20, paint);

                // Draw targeted sun position
                targetedSun=new Sun(mTargetedDate);
                // mDirection=PI on south => sun azimuth=0 on south
                targetedSun.computeFromAzimuth(mDirection-(float)Math.PI);
                paint.setColor(Color.GREEN);
                paint.setStrokeWidth(3);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(0, getY(targetedSun), 20, paint);
                // Should be the same:
                // canvas.drawCircle(getX(targetedSun), getY(targetedSun), 20, paint);

                // Draw sun Path
                // Display just necessary :
                for (i=0; i < (mWidth*3/mDefinition-4); i+=2)
                {
                    x=(i*mDefinition)-(mWidth/2);
                    Sun sun = new Sun(mTargetedDate);
                    sun.computeFromAzimuth(getAzimuthFromX(x));
                    hoursPointsDisplay[i]=x;
                    hoursPointsDisplay[i+1]=getY(sun);
                }
                canvas.drawLines(hoursPointsDisplay,paint);
                canvas.drawLine(translateX(targetedSun.mSunRise-mDirection), -mHeight,
                        translateX(targetedSun.mSunRise-mDirection), mHeight, paint);
                canvas.drawLine(translateX(targetedSun.mSunSet-mDirection), -mHeight,
                        translateX(targetedSun.mSunSet-mDirection), mHeight, paint);
                paint.setColor(0xFFFF0000);
                canvas.drawLine(translateX(currentSun.mSunRise-mDirection), -mHeight,
                        translateX(currentSun.mSunRise-mDirection), mHeight, paint);
                paint.setColor(0xFF0000FF);
                canvas.drawLine(translateX(currentSun.mSunSet-mDirection), -mHeight,
                        translateX(currentSun.mSunSet-mDirection), mHeight, paint);
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
                paint.setTextSize(12);
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                paint.setShadowLayer(2,0,0,Color.BLACK);
                String[] infos = {
                    "Rolling: " + String.format("%.2f",Math.toDegrees(mRolling))+"(" + String.format("%.2f",mRolling)+")",
                    "Inclination: " + String.format("%.2f",Math.toDegrees(mInclination)),
                    "Direction: " + String.format("%.2f",Math.toDegrees(mDirection))+ "("+String.format("%.2f",mDirection)+")",
                    "Current Sunset direction: " + String.format("%.2f",Math.toDegrees(currentSun.mSunSet)),
                    "Current Sunrise direction: " + String.format("%.2f",Math.toDegrees(currentSun.mSunRise)),
                    "Targeted sun azimuth: " + String.format("%.2f",Math.toDegrees(targetedSun.mAzimuth)) + ", X:" + getX(targetedSun.mAzimuth),
                    "Targeted sun altitude: " + String.format("%.2f",Math.toDegrees(targetedSun.mAltitude)) + ", Y:" + getY(targetedSun.mAltitude),
                };
                for(i = 0; i < infos.length; i++)
                {
                    canvas.drawText(infos[i], 10, mHeight - 20*(i+1), paint);
                }
            }
            super.onDraw(canvas);
        }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }
}
// vim:et:
