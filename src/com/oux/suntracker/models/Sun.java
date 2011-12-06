package com.oux.suntracker.models;

import android.util.FloatMath;
import java.util.Calendar;

public class Sun {

    public float mLatitude;
    public float mDeclination;
    public float mAltitude;
    public float mAzimuth;
    public float mSunRise;
    public float mSunSet;
    private float mOmega;
    private float mOmegaS;
    public int mDayOfYear;;

	public Sun(Calendar date) {
        mDayOfYear = date.get(Calendar.DAY_OF_YEAR);
        computeData();
    }

	public Sun(Calendar date, float omega) {
        this(date);
        setDeclination(omega);
    }

    public void setDeclination(float omega) {
        mOmega = omega;
        mAltitude = getAltitude(mDeclination,mOmega);
        mAzimuth = getAzimuth(mDeclination,mOmega,mAltitude);
    }

    public void computeFromAzimuth(float psi){
        mAzimuth  = psi;
        mAltitude = getAltitudeFromAzimuth(mAzimuth);
    }

    // http://herve.silve.pagesperso-orange.fr/solaire.htm
    // Omega = hour angle (x)
    public float getAltitudeFromAzimuth(float psi) {
        float alfa=0;
        //TODO: To check
        // http://www.stjarnhimlen.se/comp/riset.html#1
        // http://itacanet.org/eng/elec/solar/sun3.pdf
        alfa=-2*((float)Math.atan(
                    (Math.sin(mLatitude)+Math.sqrt(
                                                   -Math.pow(Math.sin(mDeclination),2)
                                                       +Math.pow(Math.cos(mLatitude),2)*Math.pow(Math.cos(psi),2)
                                                       +Math.pow(Math.sin(mLatitude),2)
                                                    )
                                                  )/(Math.sin(mDeclination)+Math.cos(mLatitude)*Math.cos(psi))
                    ));
        if (alfa > 0) {
            alfa=alfa-(float)Math.PI;
        } else {
            alfa=alfa+(float)Math.PI;
        }
        return alfa;
    }

    private void computeData() {
        mLatitude=getLatitude();
        mDeclination = getDeclination(mDayOfYear);
        mOmegaS = (float)Math.acos(-((float)Math.tan(mLatitude) * (float)Math.tan(mDeclination)));
        //mSunRise = getAzimuth(mDeclination,(float)Math.PI+mOmegaS,getAltitude(mDeclination,(float)Math.PI+mOmegaS));
        //mSunSet = getAzimuth(mDeclination,(float)Math.PI-mOmegaS,getAltitude(mDeclination,(float)Math.PI-mOmegaS));
        mSunRise = (float)Math.PI+getAzimuth(mDeclination,mOmegaS,getAltitude(mDeclination,mOmegaS));
        mSunSet = (float)Math.PI-getAzimuth(mDeclination,mOmegaS,getAltitude(mDeclination,mOmegaS));
        //mSunRise = (float)Math.PI+mOmegaS;
        //mSunSet = (float)Math.PI-mOmegaS;
    }

    // Geolocation dependence
    private float getLatitude() {
        return (float)Math.toRadians(43.59); //TODO: to dynamise
    }

    // Delta = Declination : Date dependence
    private float getDeclination(int day) {
        return (float)Math.toRadians(23.45) * FloatMath.sin ((float)((day - 81) * 2*(float)Math.PI / 365));
    }

    // Alfa = Altitude (y)
    private float getAltitude(float delta, float omega) {
        return (float)Math.asin(FloatMath.sin((float)delta) * FloatMath.sin(mLatitude) + FloatMath.cos((float)delta) * FloatMath.cos(mLatitude) * FloatMath.cos((float)omega));
    }

    // Psi = Azimuth (x)
    private float getAzimuth(float delta, float omega,float alfa) {
        float psi;
        psi = 0;
        if (omega < Math.PI) {
            // TODO: remove 2PI shift here and on mDirection compute if possible
            psi = (float)Math.PI -(float)Math.acos(
                    (FloatMath.cos(mLatitude) * FloatMath.sin(delta) - FloatMath.cos(delta) * FloatMath.sin(mLatitude) * FloatMath.cos((float)omega)
                    ) / FloatMath.cos (alfa)
                    );
        } else {
            psi = (float)Math.PI +(float)Math.acos(
                    (FloatMath.cos(mLatitude) * FloatMath.sin(delta) - FloatMath.cos(delta) * FloatMath.sin(mLatitude) * FloatMath.cos((float)omega)
                    ) / FloatMath.cos (alfa)
                    );
        }
        return psi;
    }

}
// vim:et:
