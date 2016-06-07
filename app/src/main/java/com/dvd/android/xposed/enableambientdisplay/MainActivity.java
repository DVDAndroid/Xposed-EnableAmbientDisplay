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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceFragment;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.dvd.android.xposed.enableambientdisplay.services.SensorService;
import com.dvd.android.xposed.enableambientdisplay.utils.Utils;

import java.io.File;

import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.ACTION_DOZE;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.ACTION_HOT_REBOOT;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.ACTION_PREFS_CHANGED;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.ACTION_SLEEP;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_ALPHA;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_BRIGHTNESS;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_IN;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_OUT;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_PROXIMITY;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_PULSE_SCHEDULE;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_RESETS;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_VISIBILITY;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_WITH_POWER_KEY;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.EXTRA_KEY;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.EXTRA_VALUE;

public class MainActivity extends Activity {

    private static MenuItem mServiceItem;

    public static boolean isEnabled() {
        return false;
    }

    private static void updateMenuItem(boolean enabled) {
        if (mServiceItem == null) return;

        mServiceItem.setVisible(enabled);

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                    mServiceItem.setTitle(SensorService.isRunning() ? R.string.stop_service : R.string.start_service);
            }
        }, 500);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null)
            getFragmentManager().beginTransaction().replace(R.id.fragment, new SettingsFragment()).commit();
    }

    @Override
    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        mServiceItem = menu.findItem(R.id.service);


        //noinspection ConstantConditions
        if (!isEnabled()) {
            menu.removeItem(R.id.hot_reboot);
            menu.removeItem(R.id.service);
        } else {
            SharedPreferences prefs = getSharedPreferences(MainActivity.class.getSimpleName(), MODE_WORLD_READABLE);
            updateMenuItem(prefs.getBoolean(DOZE_PROXIMITY, false));
        }
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
            case R.id.service:
                Intent service = new Intent().setClass(this, SensorService.class);
                if (SensorService.isRunning()) {
                    Toast.makeText(this, R.string.service_stopped, Toast.LENGTH_SHORT).show();
                    stopService(service);
                } else {
                    Toast.makeText(this, R.string.service_started, Toast.LENGTH_SHORT).show();
                    startService(service);
                }
                updateMenuItem(true);
                break;
            case R.id.hot_reboot:
                sendBroadcast(new Intent(ACTION_HOT_REBOOT));
                break;
            case R.id.info:
                AlertDialog d = new AlertDialog.Builder(this)
                        .setPositiveButton(android.R.string.ok, null)
                        .setTitle(R.string.info)
                        .setMessage(R.string.about)
                        .create();
                d.show();

                ((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private Intent mIntent;
        private SharedPreferences mPrefs;
        private String mKey;

        @Override
        @SuppressLint("CommitPrefEdits")
        @SuppressWarnings("deprecation")
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            getPreferenceManager().setSharedPreferencesName(MainActivity.class.getSimpleName());
            addPreferencesFromResource(R.xml.prefs);
            // SELinux test, see XposedMod
            getPreferenceManager().getSharedPreferences().edit().putBoolean("can_read_prefs", true).commit();

            SharedPreferences prefs = getActivity().getPreferences(Context.MODE_WORLD_READABLE);

            //noinspection ConstantConditions
            if (isEnabled()) {
                getPreferenceScreen().removePreference(findPreference("not_enabled"));
            }

            if (prefs.getBoolean("welcome", true)) {
                new AlertDialog.Builder(getActivity())
                        .setPositiveButton(android.R.string.ok, null)
                        .setTitle(R.string.info)
                        .setMessage(R.string.aosp)
                        .show();

                prefs.edit().putBoolean("welcome", false).apply();
            }

        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        @SuppressLint("SetWorldReadable")
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            File sharedPrefsDir = new File(getActivity().getFilesDir(), "../shared_prefs");
            File sharedPrefsFile = new File(sharedPrefsDir, MainActivity.class.getSimpleName() + ".xml");
            if (sharedPrefsFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                sharedPrefsFile.setReadable(true, false);
            }
        }


        @Override
        @SuppressLint("CommitPrefEdits")
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            mIntent = new Intent();
            mPrefs = prefs;
            mKey = key;

            switch (key) {
                case DOZE_BRIGHTNESS:
                    Toast.makeText(getActivity(), R.string.reboot_required, Toast.LENGTH_SHORT).show();
                    break;
                case DOZE_IN:
                case DOZE_OUT:
                    if (Utils.isSamsung()) {
                        Toast.makeText(getActivity(), R.string.reboot_required, Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        updateIntent(1000);
                    }
                    break;
                case DOZE_VISIBILITY:
                    if (Utils.isSamsung()) {
                        Toast.makeText(getActivity(), R.string.reboot_required, Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        updateIntent(3000);
                    }
                    break;
                case DOZE_RESETS:
                    if (Utils.isSamsung()) {
                        Toast.makeText(getActivity(), R.string.reboot_required, Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        updateIntent(1);
                    }
                    break;
                case DOZE_ALPHA:
                    if (Build.VERSION.SDK_INT >= 22) {
                        if (Utils.isSamsung()) {
                            Toast.makeText(getActivity(), R.string.reboot_required, Toast.LENGTH_SHORT).show();
                            return;
                        } else {
                            updateIntent(222);
                        }
                    } else {
                        Toast.makeText(getActivity(), R.string.reboot_required, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case DOZE_WITH_POWER_KEY:
                    updateIntent(false);

                    if (mPrefs.getBoolean(mKey + "_warning", true)) {
                        new AlertDialog.Builder(getActivity()).setTitle(R.string.caution)
                                .setMessage(R.string.caution_power_key)
                                .setPositiveButton(android.R.string.ok, null).show();

                        mPrefs.edit().putBoolean(mKey + "_warning", false).apply();
                    }
                    break;
                case DOZE_PULSE_SCHEDULE:
                    if (Utils.isSamsung()) {
                        Toast.makeText(getActivity(), R.string.reboot_required, Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        updateIntent("10s,30s,60s");
                    }
                    break;
                case DOZE_PROXIMITY:
                    mIntent.setClass(getActivity(), SensorService.class);

                    //noinspection ConstantConditions
                    if (prefs.getBoolean(key, false) && isEnabled()) {
                        Toast.makeText(getActivity(), R.string.service_started, Toast.LENGTH_SHORT).show();
                        getActivity().startService(mIntent);
                    } else {
                        if (SensorService.isRunning()) {
                            Toast.makeText(getActivity(), R.string.service_stopped, Toast.LENGTH_SHORT).show();
                            getActivity().stopService(mIntent);
                        }
                    }
                    updateMenuItem(prefs.getBoolean(key, false));
            }

            if (mIntent.getAction() != null) {
                prefs.edit().commit();
                getActivity().sendBroadcast(mIntent);
            }
        }

        private void updateIntent(Object defaultValue) {
            mIntent.setAction(ACTION_PREFS_CHANGED);
            mIntent.putExtra(EXTRA_KEY, mKey);
            if (defaultValue instanceof Boolean)
                mIntent.putExtra(EXTRA_VALUE, mPrefs.getBoolean(mKey, (Boolean) defaultValue));
            if (defaultValue instanceof String)
                mIntent.putExtra(EXTRA_VALUE, mPrefs.getString(mKey, (String) defaultValue));
            if (defaultValue instanceof Integer)
                mIntent.putExtra(EXTRA_VALUE, mPrefs.getInt(mKey, (int) defaultValue));
        }
    }
}