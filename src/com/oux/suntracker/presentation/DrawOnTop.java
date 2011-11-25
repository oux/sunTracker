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

public class DrawOnTop extends View implements SensorEventListener {
    private static final String mTAG = "Sun Tracker View";
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
    private int   mWidth;
    private int   mHeight;
    int mDayNumber;
    // http://engnet.anu.edu.au/DEpeople/Andres.Cuevas/Sun/help/SPguide.html
    float mRadiusX=200,mRadiusY=200;
    float mHorizontalAngle=1,mVerticalAngle=1;
    float mFi;
    float mOmegaS_winterSolstice;
    float mOmegaS_summerSolstice;
    float mDelta, deltaWinterSolstice, deltaSummerSolstice;
    float mOmegaS;
    /* Show different hours: */
    int graduation=24*4;
    int mDefinition=5;
    int pointed_hour=0;
    int summerSolsticeDay=177;
    int winterSolsticeDay=355;
    float[] sunrise_points = new float[365];
    float[] sunset_points = new float[365];
    float pointed_hour_x;
    float pointed_hour_y;
    float[] hours_points = new float[graduation*4];
    float[] omega_points = new float[graduation];
    float[] hoursPointsDisplay = new float[mWidth*3];;
    float[] hours_points_display = new float[graduation*4*3];
    // private float   mDirection;
    public static volatile float new_mDirection = (float) 0;
    public static volatile float mDirection = (float) 0;
    public static volatile float mRolling = (float) 0;
    public static volatile float mInclination = (float) 0;
    public static volatile float kFilteringFactor = (float)0.05;
    // public static volatile float kFilteringFactor = (float)0.05;
    private float[] _accelerometerValues   = null;  //Valeur de l'accéléromètre
    private float[] _magneticValues          = null;  //Valeurs du champ magnétique

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
        mRect.set(-0.5f, -0.5f, 0.5f, 0.5f);
        mPath.arcTo(mRect, 0, 180);
        changeDate();

        deltaWinterSolstice = (float)Math.toRadians((float)23.45 * FloatMath.sin ((float)((winterSolsticeDay - 81) * 2*(float)Math.PI / 365)));
        mOmegaS_winterSolstice = (float)Math.acos(-(float)Math.tan(mFi) * (float)Math.tan(deltaWinterSolstice));
        sunrise_points[0]=(float)Math.PI+mOmegaS_winterSolstice;
        sunset_points[0]=(float)Math.PI-mOmegaS_winterSolstice;
        deltaSummerSolstice = (float)Math.toRadians((float)23.45 * FloatMath.sin ((float)((summerSolsticeDay - 81) * 2*(float)Math.PI / 365)));
        mOmegaS_summerSolstice = (float)Math.acos(-(float)Math.tan(mFi) * (float)Math.tan(deltaSummerSolstice));
        sunrise_points[1]=(float)Math.PI+mOmegaS_summerSolstice;
        sunset_points[1]=(float)Math.PI-mOmegaS_summerSolstice;
	}

    public void setViewAngles(float verticalAngle, float horizontalAngle) {
        mHorizontalAngle=(float)Math.toRadians(horizontalAngle);
        mVerticalAngle=(float)Math.toRadians(verticalAngle);
    }

    // Get hours angle from pixel X
    private float getHourAngle(int i) {
        float angle;
        //if (i < mWidth/2) {
            angle=(float)Math.PI-(float)Math.atan(i/mRadiusX)-mDirection;
            /*
        } else {
            angle=(float)Math.PI+(float)Math.atan(i/mRadiusX)-mDirection;
        }
        */
        return angle;
    }

    // Get hours:minutes from Radian angle
    private float hourToRadian(Calendar date) {
        return (float)(2*Math.PI*date.get(Calendar.HOUR_OF_DAY)/24)+(float)(2*Math.PI*date.get(Calendar.MINUTE)/(24*60));
    }

    // Get hours:minutes from Radian angle
    private String radianToHour(float angle) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        Date date = new Date(24*(long)Math.toDegrees(Math.PI-angle)*10*1000);
        return sdf.format(date);
    }

    // Geolocation dependence
    private float getFi() {
        return (float)Math.toRadians(43.49); //TODO: to dynamise
    }

    // Date dependence
    private float getDelta(int day) {
        return (float)Math.toRadians((float)23.45 * FloatMath.sin ((float)((day - 81) * 2*(float)Math.PI / 365)));
    }

    // Alfa = Altitude (y)
    private float getAlfa(float delta, float omega) {
        return (float)Math.asin(FloatMath.sin((float)delta) * FloatMath.sin(mFi) + FloatMath.cos((float)delta) * FloatMath.cos(mFi) * FloatMath.cos((float)omega));
    }

    // Psi = hour angle (x)
    private float getPsi(float delta, float omega) {
        float alfa;
        float psi;
        psi = 0;
        alfa = getAlfa(delta,omega);
        if (omega < Math.PI) {
            // TODO: remove 2PI shift here and on mDirection compute if possible
            psi = 2 * (float)Math.PI -(float)Math.acos(
                    (FloatMath.cos(mFi) * FloatMath.sin(delta) - FloatMath.cos(delta) * FloatMath.sin(mFi) * FloatMath.cos((float)omega)
                    ) / FloatMath.cos (alfa)
                    );
        } else {
            psi = (float)Math.acos(
                    (FloatMath.cos(mFi) * FloatMath.sin(delta) - FloatMath.cos(delta) * FloatMath.sin(mFi) * FloatMath.cos((float)omega)
                    ) / FloatMath.cos (alfa)
                    );
        }
        return psi;
    }

    private float translateX(double angle) {
        float retval=1000;
        // return (float)(Math.tan(angle));
        if (angle < 0 && angle < -mHorizontalAngle) {
            retval=-mWidth;
        } else if (angle > 0 && angle > mHorizontalAngle ) {
            retval=mWidth;
        } else if ((angle > 0 && angle < mHorizontalAngle) || (angle < 0 && angle > -mHorizontalAngle)) {
            retval=(float)(Math.tan(angle)*mRadiusX);
            // retval=(float)(Math.cos(angle)*Math.tan(angle)*mWidth);
        }
        return retval;
        // return (float)(Math.tan(angle)*mRadiusX);
    }

    private float translateY(double angle) {
        float retval=1000;
        if (angle < 0 && angle < -mVerticalAngle) {
            retval=-mHeight;
        } else if (angle > 0 && angle > mVerticalAngle ) {
            retval=mHeight;
        } else if ((angle > 0 && angle < mVerticalAngle) || (angle < 0 && angle > -mVerticalAngle)) {
            retval=(float)(Math.tan(angle)*mRadiusY);
            // retval=(float)(Math.cos(angle)*Math.tan(angle)*mHeight);
        }
        return retval;
        // return (float)(Math.tan(angle)*mRadiusY);
    }

    // TODO: Debug bijection between getX and getHourAngle (Xradius problem)
    private float getX(int dayOfYear, float omega) {
        float  delta=getDelta(dayOfYear);
        return translateX(getPsi(delta,omega)-mDirection);
    }

    private float getY(int dayOfYear, float omega) {
        float delta=getDelta(dayOfYear);
        return -translateY(getAlfa(delta,omega)+mInclination);
    }

    private float getX(float omega) {
        return translateX(getPsi(mDelta,omega)-mDirection);
    }

    private float getY(float omega) {
        return -translateY(getAlfa(mDelta,omega)+mInclination);
    }

    public void changeDate() {
        Calendar date = Calendar.getInstance();
        changeDate(date);
    }

    public void changeDate(Calendar date) {
        float omega;
        float alfa;
        float psi;
        // Initialisation regarding the localisation.
        mDayNumber = date.get(Calendar.DAY_OF_YEAR);
        // http://engnet.anu.edu.au/DEpeople/Andres.Cuevas/Sun/help/SPguide.html
        mDelta = getDelta(mDayNumber);
        mFi=getFi();
        mOmegaS = (float)Math.acos(-((float)Math.tan(mFi) * (float)Math.tan(mDelta)));
        for (int hour=0; hour < graduation; hour++)
        {
            omega= (float)( (Math.PI * 2 * hour / graduation) - Math.PI);
            // omega= (float)( (Math.PI * 2 * hour / graduation) + Math.PI);
            psi = getPsi(mDelta,omega);
            alfa = getAlfa(mDelta,omega);
            // TODO: getAlfaPsi(omega, alfa, psi);
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
            // Log.v(mTAG,"hours_points["+hour*4+"]="+hours_points[hour*4]+","+hours_points[hour+1]);
        }
    }

    /*
    private null drawLineWithCheck() {
        float retval=mHeight;
        if (angle < mVerticalAngle)
            retval=(float)(Math.cos(angle)*Math.tan(angle)*mHeight);
        return retval;
        // return (float)(Math.tan(angle)*mRadiusY);
    }
    */

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
            float omega=0;
            int x=0,i=0;
            float delta_y;
            float alfa, pointed_alfa=0;
            float psi, pointed_psi=0;
            Calendar date=Calendar.getInstance();;
            float currentSunDirection;
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
                canvas.drawLine(translateX(sunrise_points[0]-mDirection),-translateY(mInclination),translateX(sunrise_points[1]-mDirection),-translateY(mInclination),paint);
                canvas.drawLine(translateX(sunset_points[0]-mDirection),-translateY(mInclination),translateX(sunset_points[1]-mDirection),-translateY(mInclination),paint);

                pointed_hour=0;
                pointed_hour_x=Math.abs(hours_points_display[0]);

                for (int hour=0; hour < graduation*4; hour+=2)
                {
                    hours_points_display[hour]=translateX(hours_points[hour]-mDirection);
                    hours_points_display[hour+1]=-translateY(hours_points[hour+1]+mInclination);
                    // TODO: use mDirection and convert to hour:minutes
                    if (Math.abs(hours_points_display[hour]) < pointed_hour_x ) {
                        pointed_hour_x=Math.abs(hours_points_display[hour]);
                        pointed_hour_y=hours_points_display[hour+1];
                        pointed_psi=hours_points[hour];
                        pointed_alfa=hours_points[hour+1];
                        pointed_hour=hour/4;
                        // Log.v(mTAG,"Y["+(hour+1)+"]="+hours_points_display[hour+1]+" from:"+hours_points[hour+1]+"+"+mInclination);
                    }
                }
                // Draw current sun position
                currentSunDirection=hourToRadian(date);
                paint.setColor(Color.YELLOW);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(getX(currentSunDirection), getY(currentSunDirection), 20, paint);
                canvas.drawText(radianToHour(currentSunDirection),getX(currentSunDirection)+20, getY(currentSunDirection)-20, paint);

                // Draw targeted sun position
                paint.setColor(Color.GREEN);
                paint.setStrokeWidth(3);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(0, getY(getHourAngle(0)), 20, paint);

                // Draw sun Path
                // canvas.drawLines(hours_points_display,paint);
                // Display just necessary :
                for (i=0; i < (mWidth*3/mDefinition-4); i+=2)
                {
                    x=(i*mDefinition)-(mWidth/2);
                    omega=getHourAngle(x);
                    // hoursPointsDisplay[i*2]=getX(omega); // TODO : should be works.
                    hoursPointsDisplay[i]=x;//translateX(getPsi(omega)-mDirection);
                    hoursPointsDisplay[i+1]=getY(omega);
                }
                canvas.drawLines(hoursPointsDisplay,paint);
                paint.setTextSize(12);
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                paint.setShadowLayer(2,0,0,Color.BLACK);
                canvas.drawText(radianToHour(getHourAngle(0)), 20, getY(getHourAngle(0))+20, paint);
//                canvas.drawText("omega: "+ getHourAngle(0), -10, hours_points[pointed_hour*4+1]-20, paint);
//                canvas.drawText("solar Y: "+ getY(getHourAngle(0)), -10, hours_points[pointed_hour*4+1], paint);
                // canvas.drawText("omega: "+ omega_points[pointed_hour], -10, hours_points[pointed_hour*4+1], paint);
//                canvas.drawText("mDirection: "+ mDirection, -10, hours_points[pointed_hour*4+1]+20, paint);
//                canvas.drawText("alfa: "+ pointed_alfa, -10, hours_points[pointed_hour*4+1]+40, paint);
//                canvas.drawText("pointed x: "+ pointed_hour_x, -10, hours_points[pointed_hour*4+1]+60, paint);
//                canvas.drawText("pointed y: "+ pointed_hour_y, -10, hours_points[pointed_hour*4+1]+80, paint);
                paint.setColor(0xFF0000FF);
                canvas.drawLine(translateX(Math.PI-mOmegaS-mDirection), -mHeight,
                        translateX(Math.PI-mOmegaS-mDirection), mHeight, paint);
                paint.setColor(0xFFFF0000);
                canvas.drawLine(translateX(Math.PI+mOmegaS-mDirection), -mHeight,
                        translateX(Math.PI+mOmegaS-mDirection), mHeight, paint);
                /* Show different seasons: */
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
                        /*
                        ********* Print seasons ********
                        */
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
// canvas.drawText("mDelta: "+ mDelta, 10, hours_points[pointed_hour*4+1]+20, paint);
                canvas.restore();
                paint.setTextSize(12);
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                paint.setShadowLayer(2,0,0,Color.BLACK);
                int shift=20;
                canvas.drawText("Rolling: " + String.format("%.2f",Math.toDegrees(mRolling)), 10, mHeight - shift, paint);
                shift+=20;
                canvas.drawText("Inclination: " + String.format("%.2f",Math.toDegrees(mInclination)), 10, mHeight - shift, paint);
                shift+=20;
                canvas.drawText("Direction: " + String.format("%.2f",Math.toDegrees(mDirection)), 10, mHeight - shift, paint);
                shift+=20;
                canvas.drawText("Current Sun direction: " + String.format("%.2f",Math.toDegrees(currentSunDirection)), 10, mHeight - shift, paint);
                shift+=20;
                canvas.drawText("Current Sunset direction: " + String.format("%.2f",Math.toDegrees(Math.PI-mOmegaS)), 10, mHeight - shift, paint);
                shift+=20;
                canvas.drawText("Current Sunrise direction: " + String.format("%.2f",Math.toDegrees(Math.PI+mOmegaS)), 10, mHeight - shift, paint);
//                canvas.drawText("Sunrise angle: " + (float)(sunrise_points[0]-mDirection), 10, 20, paint);
//                canvas.drawText("Sunset angle: " + (float)(sunset_points[0]-mDirection), 10, 40, paint);
//                canvas.drawText("Sunrise shift: " + (float)(translateX(sunrise_points[0]-mDirection)), 10, 60, paint);
//                canvas.drawText("Sunset shift: " + (float)(translateX(sunset_points[0]-mDirection)), 10, 80, paint);
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
// vim:et:
