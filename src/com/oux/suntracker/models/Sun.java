package com.oux.suntracker.models;

import java.util.Calendar;

public class Sun {

    public double mLatitude;
    public double mDeclination;
    public double mAltitude;
    public double mAzimuth;
    public double mSunRise;
    public double mSunSet;
    private double mOmega;
    private double mOmegaS;
    public int mDayOfYear;;

	public Sun(Calendar date) {
        mDayOfYear = date.get(Calendar.DAY_OF_YEAR);
        computeData();
    }

	public Sun(Calendar date, double omega) {
        this(date);
        setDeclination(omega);
    }

    public void setDeclination(double omega) {
        mOmega = omega;
        mAltitude = getAltitude(mDeclination,mOmega);
        mAzimuth = getAzimuth(mDeclination,mOmega,mAltitude);
    }

    public void computeFromAzimuth(double psi){
        mAzimuth  = psi;
        mAltitude = getAltitudeFromAzimuth(mAzimuth);
    }

    // http://herve.silve.pagesperso-orange.fr/solaire.htm
    // Omega = hour angle (x)
    public double getAltitudeFromAzimuth(double psi) {
        double alfa=0;
        //TODO: To check
        // http://www.stjarnhimlen.se/comp/riset.html#1
        // http://itacanet.org/eng/elec/solar/sun3.pdf
        alfa = -2*(Math.atan(
                    (Math.sin(mLatitude)+Math.sqrt(
                                                   -Math.pow(Math.sin(mDeclination),2)
                                                       +Math.pow(Math.cos(mLatitude),2)*Math.pow(Math.cos(psi),2)
                                                       +Math.pow(Math.sin(mLatitude),2)
                                                    )
                                                  )/(Math.sin(mDeclination)+Math.cos(mLatitude)*Math.cos(psi))
                    ));
        if (alfa > 0) {
            alfa = alfa-Math.PI;
        } else {
            alfa = alfa+Math.PI;
        }
        return alfa;
    }

    private void computeData() {
        mLatitude = getLatitude();
        mDeclination = getDeclination(mDayOfYear);
        mOmegaS = Math.acos(-(Math.tan(mLatitude) * Math.tan(mDeclination)));
        mSunRise = -getAzimuth(mDeclination,mOmegaS,getAltitude(mDeclination,mOmegaS));
        mSunSet = -mSunRise;
    }

    // Geolocation dependence
    private double getLatitude() {
        return Math.toRadians(43.59); //TODO: to dynamise
    }

    // Delta = Declination : Date dependence
    private double getDeclination(int day) {
        return Math.toRadians(23.45) * Math.sin(((day - 81) * 2 * Math.PI / 365.25));
    }

    // Alfa = Altitude (y)
    private double getAltitude(double delta, double omega) {
        return Math.asin(Math.sin(delta) * Math.sin(mLatitude) + Math.cos(delta) * Math.cos(mLatitude) * Math.cos(omega));
    }

    // Psi = Azimuth (x)
    private double getAzimuth(double delta, double omega,double alfa) {
        double psi;
        psi = 0;
        if (omega < 0) {
            // TODO: remove 2PI shift here and on mDirection compute if possible
            psi = -(Math.PI - Math.acos(
                    (Math.cos(mLatitude) * Math.sin(delta) - Math.cos(delta) * Math.sin(mLatitude) * Math.cos(omega)
                    ) / Math.cos (alfa)
                    ));
        } else if (omega < Math.PI) {
            // TODO: remove 2PI shift here and on mDirection compute if possible
            psi = Math.PI - Math.acos(
                    (Math.cos(mLatitude) * Math.sin(delta) - Math.cos(delta) * Math.sin(mLatitude) * Math.cos(omega)
                    ) / Math.cos (alfa)
                    );
        } else {
            psi = Math.PI + Math.acos(
                    (Math.cos(mLatitude) * Math.sin(delta) - Math.cos(delta) * Math.sin(mLatitude) * Math.cos(omega)
                    ) / Math.cos (alfa)
                    );
        }
        return psi;
    }
}
// vim:et:
