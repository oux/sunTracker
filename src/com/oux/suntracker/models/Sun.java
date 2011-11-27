package com.oux.suntracker.models;

import android.util.FloatMath;
import java.util.Calendar;

public class Sun {

    public float mFi;
    public float mDelta;
    public float mAlfa;
    public float mPsi;
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
        setOmega(omega);
    }

    public void setOmega(float omega) {
        mOmega = omega;
        mAlfa = getAlfa(mDelta,mOmega);
        mPsi = getPsi(mDelta,mOmega,mAlfa);
    }

    public void setPsi(float psi) {
        mOmega = getOmega(mDelta,psi);
        mAlfa = getAlfa(mDelta,mOmega);
        mPsi = getPsi(mDelta,mOmega,mAlfa);
    }

    private void computeData() {
        mFi=getFi();
        mDelta = getDelta(mDayOfYear);
        mOmegaS = (float)Math.acos(-((float)Math.tan(mFi) * (float)Math.tan(mDelta)));
        //mSunRise = getPsi(mDelta,(float)Math.PI+mOmegaS,getAlfa(mDelta,(float)Math.PI+mOmegaS));
        //mSunSet = getPsi(mDelta,(float)Math.PI-mOmegaS,getAlfa(mDelta,(float)Math.PI-mOmegaS));
        mSunRise = (float)Math.PI+getPsi(mDelta,mOmegaS,getAlfa(mDelta,mOmegaS));
        mSunSet = (float)Math.PI-getPsi(mDelta,mOmegaS,getAlfa(mDelta,mOmegaS));
        //mSunRise = (float)Math.PI+mOmegaS;
        //mSunSet = (float)Math.PI-mOmegaS;
    }

    // Geolocation dependence
    private float getFi() {
        return (float)Math.toRadians(43.59); //TODO: to dynamise
    }

    // Delta = Declination : Date dependence
    private float getDelta(int day) {
        return (float)Math.toRadians((float)23.45 * FloatMath.sin ((float)((day - 81) * 2*(float)Math.PI / 365)));
    }

    // Alfa = Altitude (y)
    private float getAlfa(float delta, float omega) {
        return (float)Math.asin(FloatMath.sin((float)delta) * FloatMath.sin(mFi) + FloatMath.cos((float)delta) * FloatMath.cos(mFi) * FloatMath.cos((float)omega));
    }

    // Omega = hour angle (x)
    private float getOmega(float delta, float psi) {
        //TODO: To check
        // http://www.stjarnhimlen.se/comp/riset.html#1
        return (float) Math.asin(Math.sin(mFi) * Math.sin(delta) + Math.cos(mFi) * Math.cos(delta) * Math.cos(psi));
    }

    // Psi = Azimuth (x)
    private float getPsi(float delta, float omega,float alfa) {
        float psi;
        psi = 0;
        if (omega < Math.PI) {
            // TODO: remove 2PI shift here and on mDirection compute if possible
            psi = (float)Math.PI -(float)Math.acos(
                    (FloatMath.cos(mFi) * FloatMath.sin(delta) - FloatMath.cos(delta) * FloatMath.sin(mFi) * FloatMath.cos((float)omega)
                    ) / FloatMath.cos (alfa)
                    );
        } else {
            psi = (float)Math.PI +(float)Math.acos(
                    (FloatMath.cos(mFi) * FloatMath.sin(delta) - FloatMath.cos(delta) * FloatMath.sin(mFi) * FloatMath.cos((float)omega)
                    ) / FloatMath.cos (alfa)
                    );
        }
        return psi;
    }

}
