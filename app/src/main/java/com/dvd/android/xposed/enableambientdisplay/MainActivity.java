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

package com.dvd.android.xposed.enableambientdisplay;

import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.ACTION_DOZE;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.ACTION_SLEEP;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_ALPHA;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_BRIGHTNESS;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_IN;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_OUT;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_PROXIMITY;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_RESETS;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_VISIBILTY;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.REBOOT;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.SOFT_REBOOT;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeoutException;

import com.dvd.android.xposed.enableambientdisplay.services.SensorService;

public class MainActivity extends PreferenceActivity
		implements SharedPreferences.OnSharedPreferenceChangeListener {

	@Override
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager()
				.setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.prefs);

		SharedPreferences mPrefs = getPreferences(Context.MODE_PRIVATE);

		if (mPrefs.getBoolean("welcome", true)) {
			new AlertDialog.Builder(this)
					.setPositiveButton(android.R.string.ok, null)
					.setTitle(R.string.info).setMessage(R.string.aosp).show();

			mPrefs.edit().putBoolean("welcome", false).apply();
		}

		if (isEnabled()) {
			getPreferenceScreen()
					.removePreference(findPreference("not_enabled"));
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void onResume() {
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		super.onResume();
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void onPause() {
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	public boolean isEnabled() {
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.testIt:
				sendBroadcast(new Intent().setAction(ACTION_SLEEP));

				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						sendBroadcast(new Intent().setAction(ACTION_DOZE));
					}
				}, 2000);
				break;
			case R.id.start_service:
				if (SensorService.isRunning) {
					Toast.makeText(this, R.string.service_already,
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, R.string.service_started,
							Toast.LENGTH_SHORT).show();
					startService(
							new Intent().setClass(this, SensorService.class));
				}
				break;
			case R.id.reboot:
				try {
					RootShell.getShell(true).add(new Command(0, REBOOT));
				} catch (IOException | TimeoutException
						| RootDeniedException e) {
					Toast.makeText(MainActivity.this, e.getMessage(),
							Toast.LENGTH_SHORT).show();
				}
				break;
			case R.id.hot_reboot:
				try {
					RootShell.getShell(true).add(new Command(0, SOFT_REBOOT));
				} catch (IOException | TimeoutException
						| RootDeniedException e) {
					Toast.makeText(MainActivity.this, e.getMessage(),
							Toast.LENGTH_SHORT).show();
				}
				break;
			case R.id.info:
				AlertDialog d = new AlertDialog.Builder(this)
						.setPositiveButton(android.R.string.ok, null)
						.setTitle(R.string.info).setMessage(R.string.about)
						.create();
				d.show();

				((TextView) d.findViewById(android.R.id.message))
						.setMovementMethod(LinkMovementMethod.getInstance());

				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("unchecked")
	public void set(String key, String val) {
		try {
			Class SystemProperties = Class
					.forName("android.os.SystemProperties");

			Method set = SystemProperties.getMethod("set", String.class,
					String.class);
			set.invoke(SystemProperties, key, val);
		} catch (IllegalArgumentException | ClassNotFoundException
				| NoSuchMethodException | IllegalAccessException
				| InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		switch (key) {
			case DOZE_BRIGHTNESS:
			case DOZE_ALPHA:
			case DOZE_IN:
			case DOZE_OUT:
			case DOZE_VISIBILTY:
			case DOZE_RESETS:
				Toast.makeText(this, R.string.reboot_required,
						Toast.LENGTH_SHORT).show();
				break;
			case DOZE_PROXIMITY:
				break;
		}
	}
}