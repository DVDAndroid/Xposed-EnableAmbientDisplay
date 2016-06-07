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

package com.dvd.android.xposed.enableambientdisplay.utils;

import java.io.File;

import static de.robv.android.xposed.XposedBridge.log;

public class Utils {

	public static final String ACTION_DOZE = "com.android.systemui.doze.pulse";
	public static final String ACTION_SLEEP = "dvd.ACTION_SLEEP";
    public static final String ACTION_PREFS_CHANGED = "dvd.ACTION_PREFS_CHANGED";
    public static final String ACTION_HOT_REBOOT = "dvd.ACTION_HOT_REBOOT";
    public static final String EXTRA_KEY = "dvd.EXTRA_KEY";
    public static final String EXTRA_VALUE = "dvd.EXTRA_VALUE";

    public static final String PACKAGE_SYSTEMUI = "com.android.systemui";
    public static final String PACKAGE_ANDROID = "android";
    public static final String THIS_PKG_NAME = "com.dvd.android.xposed.enableambientdisplay";

    public static final String DOZE_SUPP = "doze_display_state_supported";
    public static final String DOZE_PICK_UP = "doze_pulse_on_pick_up";
    public static final String DOZE_IN = "doze_pulse_duration_in";
	public static final String DOZE_OUT = "doze_pulse_duration_out";
    public static final String DOZE_VISIBILITY = "doze_pulse_duration_visible";
    public static final String DOZE_ALPHA = "doze_small_icon_alpha";
    public static final String DOZE_RESETS = "doze_pulse_schedule_resets";
    public static final String DOZE_PULSE_SCHEDULE = "doze_pulse_schedule";
    public static final String DOZE_PROXIMITY = "doze_proximity";
    public static final String DOZE_BRIGHTNESS = "config_screenBrightnessDoze";
    public static final String DOZE_WITH_POWER_KEY = "doze_power_key";

    private static final String LOG_FORMAT = "[Enable Ambient Display] %1$s %2$s: %3$s";
    public static boolean debug = false;

    public static void logD(String tag, String msg) {
        if (debug) log(String.format(LOG_FORMAT, "[DEBUG]", tag, msg));
    }

    public static void logE(String tag, String msg, Throwable t) {
        log(String.format(LOG_FORMAT, "[ERROR]", tag, msg));
        if (t != null)
            log(t);
    }

    public static void logW(String tag, String msg) {
        log(String.format(LOG_FORMAT, "[WARNING]", tag, msg));
    }

    public static boolean isSamsung() {
        return new File("/system/framework/twframework.jar").isFile();
    }
}
