/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 DVDAndroid
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dvd.android.xposed.enableambientdisplay.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.ACTION_DOZE;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_PROXIMITY;

public class SensorService extends Service
		implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final int DELAY_BETWEEN_DOZES_IN_MS = 2500;
    private static final String TAG = "SensorService";
    private static boolean isRunning;
    private ProximitySensor mSensor;
    private PowerManager mPowerManager;

	private long mLastDoze;
	private SharedPreferences mPrefs;
	private boolean mDozeProximity;
	private boolean displayTurnedOff = false;
	private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				onDisplayOff();
			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				onDisplayOn();
			}
		}
	};
    private Context mContext;

    public static boolean isRunning() {
        return isRunning;
    }

    @Override
    public void onCreate() {
        mContext = this;
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mSensor = new ProximitySensor(mContext);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mPrefs.registerOnSharedPreferenceChangeListener(this);
		mDozeProximity = mPrefs.getBoolean(DOZE_PROXIMITY, true);

		if (!isInteractive() && mDozeProximity) {
			mSensor.enable();
		}

		isRunning = true;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenStateReceiver, screenStateFilter);

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		isRunning = false;
        mContext.unregisterReceiver(mScreenStateReceiver);
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void launchDozePulse() {
		mContext.sendBroadcast(new Intent(ACTION_DOZE));
		mLastDoze = System.currentTimeMillis();
	}

	private boolean isInteractive() {
		return mPowerManager.isInteractive();
	}

	private void onDisplayOn() {
		mSensor.disable();
	}

	private void onDisplayOff() {
		if (mDozeProximity) {
			mSensor.enable();
		}
		displayTurnedOff = true;
	}

	@Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		if (key.equals(DOZE_PROXIMITY))
			mDozeProximity = sharedPreferences.getBoolean(DOZE_PROXIMITY, true);

	}

	class ProximitySensor implements SensorEventListener {
		private SensorManager mSensorManager;
		private Sensor mSensor;

		private boolean mIsNear = false;

		public ProximitySensor(Context context) {
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

		@Override
		public void onSensorChanged(SensorEvent event) {
			long now = System.currentTimeMillis();
			mIsNear = event.values[0] < mSensor.getMaximumRange();

            if (!mIsNear && (now - mLastDoze > DELAY_BETWEEN_DOZES_IN_MS) && !displayTurnedOff) {
                launchDozePulse();
            }

			displayTurnedOff = false;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			/* Empty */
		}

		public void enable() {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

		public void disable() {
			mSensorManager.unregisterListener(this, mSensor);
		}
	}
}
