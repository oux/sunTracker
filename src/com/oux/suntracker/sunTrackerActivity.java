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

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.view.Window;
import android.view.WindowManager;
import android.content.res.Configuration;
import android.app.Activity;
import android.app.Dialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.widget.DatePicker;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.graphics.PixelFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams; 

import java.io.IOException;
import java.util.Calendar;

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
            Camera.Parameters parameters = mCamera.getParameters();
            mDraw.setViewAngles(parameters.getVerticalViewAngle(),parameters.getHorizontalViewAngle());
            mPreview.setCamera(mCamera);
            mSensorManager.registerListener(mDraw,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(mDraw, 
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
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
