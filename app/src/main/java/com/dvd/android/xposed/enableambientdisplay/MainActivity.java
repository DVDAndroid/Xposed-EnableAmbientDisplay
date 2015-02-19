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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.NumberPicker;

public class MainActivity extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	@Override
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesMode(
				Context.MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.prefs);
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (sharedPreferences.getString(key, "").equals("")) {
			if (key.equals("doze_pulse_duration_visible")) {
				sharedPreferences.edit().putString(key, "3000").apply();
			} else {
				sharedPreferences.edit().putString(key, "1000").apply();
			}
			EditTextPreference editTextPreference = (EditTextPreference) findPreference(key);
			editTextPreference.setText(sharedPreferences.getString(key, ""));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.reboot:
				try {
					Process proc = Runtime.getRuntime().exec(
							new String[] { "su", "-c", "reboot" });
					proc.waitFor();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				break;
			case R.id.hot_reboot:
				try {
					Process proc = Runtime.getRuntime().exec(
							new String[] { "su", "-c",
									"busybox killall system_server" });
					proc.waitFor();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {

		switch (preference.getKey()) {
			case "doze_small_icon_alpha":
				createAlert(preference.getKey(), "222", 255);
				break;
			case "config_screenBrightnessDoze":
				createAlert(preference.getKey(), "17", 100);
				break;
		}

		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@SuppressWarnings("deprecation")
	private void createAlert(final String key, String defaultValue, int maxValue) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getResources().getIdentifier(key, "string",
				getPackageName()));
		final NumberPicker np = new NumberPicker(this);
		np.setMinValue(1);
		np.setMaxValue(maxValue);/**/
		np.setValue(Integer.parseInt(getPreferenceManager()
				.getSharedPreferences().getString(key, defaultValue)));
		alert.setView(np);
		alert.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						getPreferenceManager()
								.getSharedPreferences()
								.edit()
								.putString(key, Integer.toString(np.getValue()))
								.apply();
					}
				});
		alert.show();
	}
}