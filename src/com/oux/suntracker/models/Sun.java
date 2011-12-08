package com.oux.suntracker.models;

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
        alfa = -2*((float)Math.atan(
                    (Math.sin(mLatitude)+Math.sqrt(
                                                   -Math.pow(Math.sin(mDeclination),2)
                                                       +Math.pow(Math.cos(mLatitude),2)*Math.pow(Math.cos(psi),2)
                                                       +Math.pow(Math.sin(mLatitude),2)
                                                    )
                                                  )/(Math.sin(mDeclination)+Math.cos(mLatitude)*Math.cos(psi))
                    ));
        if (alfa > 0) {
            alfa = alfa-(float)Math.PI;
        } else {
            alfa = alfa+(float)Math.PI;
        }
        return alfa;
    }

    private void computeData() {
        mLatitude = getLatitude();
        mDeclination = getDeclination(mDayOfYear);
        mOmegaS = (float)Math.acos(-((float)Math.tan(mLatitude) * (float)Math.tan(mDeclination)));
        mSunRise = -getAzimuth(mDeclination,mOmegaS,getAltitude(mDeclination,mOmegaS));
        mSunSet = -mSunRise;
    }

    // Geolocation dependence
    private float getLatitude() {
        return (float)Math.toRadians(43.59); //TODO: to dynamise
    }

    // Delta = Declination : Date dependence
    private float getDeclination(int day) {
        return (float)Math.toRadians(23.45) * (float)Math.sin(((day - 81) * 2 * (float)Math.PI / 365.25));
    }

    // Alfa = Altitude (y)
    private float getAltitude(float delta, float omega) {
        return (float)Math.asin(Math.sin(delta) * Math.sin(mLatitude) + Math.cos(delta) * Math.cos(mLatitude) * Math.cos(omega));
    }

    // Psi = Azimuth (x)
    private float getAzimuth(float delta, float omega,float alfa) {
        float psi;
        psi = 0;
        if (omega < 0) {
            // TODO: remove 2PI shift here and on mDirection compute if possible
            psi = -((float)Math.PI - (float)Math.acos(
                    (Math.cos(mLatitude) * Math.sin(delta) - Math.cos(delta) * Math.sin(mLatitude) * Math.cos(omega)
                    ) / Math.cos (alfa)
                    ));
        } else if (omega < Math.PI) {
            // TODO: remove 2PI shift here and on mDirection compute if possible
            psi = (float)Math.PI - (float)Math.acos(
                    (Math.cos(mLatitude) * Math.sin(delta) - Math.cos(delta) * Math.sin(mLatitude) * Math.cos(omega)
                    ) / Math.cos (alfa)
                    );
        } else {
            psi = (float)Math.PI + (float)Math.acos(
                    (Math.cos(mLatitude) * Math.sin(delta) - Math.cos(delta) * Math.sin(mLatitude) * Math.cos(omega)
                    ) / Math.cos (alfa)
                    );
        }
        return psi;
    }
}
// vim:et:
