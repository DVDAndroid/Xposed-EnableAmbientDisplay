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

package com.dvd.android.xposed.enableambientdisplay.hook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.widget.ImageView;

import com.dvd.android.xposed.enableambientdisplay.utils.Utils;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;

import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.ACTION_HOT_REBOOT;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.ACTION_PREFS_CHANGED;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.ACTION_SLEEP;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_ALPHA;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_IN;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_OUT;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_PICK_UP;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_PULSE_SCHEDULE;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_RESETS;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_SUPP;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_VISIBILITY;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.EXTRA_KEY;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.EXTRA_VALUE;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.logD;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.logE;
import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodExactIfExists;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

public class SystemUiHook {

    private static final String TAG = "SystemUiHook";
    private static final String CLASS_DOZE_PARAMETERS_PATH = "com.android.systemui.statusbar.phone.DozeParameters";
    private static final String CLASS_KEYGUARD = "com.android.systemui.keyguard.KeyguardViewMediator";
    private static final String CLASS_NOTIFICATION_VIEW = "com.android.systemui.statusbar.NotificationTemplateViewWrapper";
    private static Context sContext;

    private static int VALUE_DOZE_IN = 1000;
    private static int VALUE_DOZE_OUT = 1000;
    private static int VALUE_DOZE_VISIBILITY = 3000;
    private static int VALUE_DOZE_RESETS = 1;
    private static int VALUE_DOZE_ALPHA = 222;
    private static String VALUE_DOZE_PULSE_SCHEDULE = "10s,30s,60s";

    private static BroadcastReceiver sPrefsChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_PREFS_CHANGED:
                    logD(TAG, "Preference change broadcast signal received");

                    String key = intent.getStringExtra(EXTRA_KEY);
                    if (key == null) break;

                    logD(TAG, "Changed preference: " + key);

                    switch (key) {
                        case DOZE_IN:
                            VALUE_DOZE_IN = intent.getIntExtra(EXTRA_VALUE, 1000);
                            break;
                        case DOZE_OUT:
                            VALUE_DOZE_OUT = intent.getIntExtra(EXTRA_VALUE, 1000);
                            break;
                        case DOZE_VISIBILITY:
                            VALUE_DOZE_VISIBILITY = intent.getIntExtra(EXTRA_VALUE, 3000);
                            break;
                        case DOZE_RESETS:
                            VALUE_DOZE_RESETS = intent.getIntExtra(EXTRA_VALUE, 1);
                            break;
                        case DOZE_ALPHA:
                            VALUE_DOZE_ALPHA = intent.getIntExtra(EXTRA_VALUE, 222);
                        case DOZE_PULSE_SCHEDULE:
                            VALUE_DOZE_PULSE_SCHEDULE = intent.getStringExtra(EXTRA_VALUE);
                            break;
                    }
                    break;
                case ACTION_SLEEP:
                    logD(TAG, "Sleep broadcast signal received");
                    try {
                        PowerManager powerManager = (PowerManager) sContext.getSystemService(Context.POWER_SERVICE);

                        callMethod(powerManager, "goToSleep", SystemClock.uptimeMillis());
                    } catch (Throwable t) {
                        logE(TAG, t.getMessage(), t);
                    }
                    break;
                case ACTION_HOT_REBOOT:
                    logD(TAG, "Hot reboot broadcast signal received");
                    //from gravitybox
                    try {
                        Class<?> classSm = findClass("android.os.ServiceManager", null);
                        Class<?> classIpm = findClass("android.os.IPowerManager.Stub", null);
                        IBinder b = (IBinder) callStaticMethod(classSm, "getService", Context.POWER_SERVICE);
                        Object ipm = callStaticMethod(classIpm, "asInterface", b);
                        callMethod(ipm, "crash", "Hot reboot");
                    } catch (Throwable t) {
                        try {
                            SystemProp.set("ctl.restart", "surfaceflinger");
                            SystemProp.set("ctl.restart", "zygote");
                        } catch (Throwable t2) {
                            logE(TAG, "Hot reboot error: ", t);
                            logE(TAG, "Hot reboot error: ", t2);
                        }
                    }
            }
        }
    };

    public static void hook(final ClassLoader classLoader, XSharedPreferences prefs) {
        try {
            Class<?> keyguardClass = XposedHelpers.findClass(CLASS_KEYGUARD, classLoader);

            String setupMethodName = Build.VERSION.SDK_INT >= 22 ? "setupLocked" : "setup";
            findAndHookMethod(keyguardClass, setupMethodName, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    sContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

                    registerReceiver(sContext);
                }
            });

            final Class<?> hookClass = findClass(CLASS_DOZE_PARAMETERS_PATH, classLoader);

            initPrefs(prefs);

            Method pulseInApi21 = findMethodExactIfExists(hookClass, "getPulseInDuration");
            Method pulseInAOSP = findMethodExactIfExists(hookClass, "getPulseInDuration", boolean.class);
            Method pulseInCM = findMethodExactIfExists(hookClass, "getPulseInDuration", int.class);

            if (pulseInApi21 != null)
                hookPulseInMethod(pulseInApi21);
            if (pulseInAOSP != null)
                hookPulseInMethod(pulseInAOSP);
            if (pulseInCM != null)
                hookPulseInMethod(pulseInCM);

            findAndHookMethod(hookClass, "getPulseVisibleDuration", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(VALUE_DOZE_VISIBILITY);
                }
            });
            findAndHookMethod(hookClass, "getPulseOutDuration", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(VALUE_DOZE_OUT);
                }
            });
            findAndHookMethod(hookClass, "getPulseScheduleResets", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(VALUE_DOZE_RESETS);
                }
            });

            final Class<?> pulseSchedule = findClass(CLASS_DOZE_PARAMETERS_PATH + "$PulseSchedule", classLoader);
            findAndHookMethod(hookClass, "getPulseSchedule", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);

                    param.setResult(callMethod(pulseSchedule.newInstance(), "parse", VALUE_DOZE_PULSE_SCHEDULE));
                }
            });

            if (Build.VERSION.SDK_INT >= 22) {
                Class<?> notificationView = findClass(CLASS_NOTIFICATION_VIEW, classLoader);
                findAndHookMethod(notificationView, "fadeIconAlpha", ImageView.class, boolean.class, long.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        setObjectField(param.thisObject, "mIconDarkAlpha", VALUE_DOZE_ALPHA);
                    }
                });
                findAndHookMethod(notificationView, "updateIconAlpha", ImageView.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        setObjectField(param.thisObject, "mIconDarkAlpha", VALUE_DOZE_ALPHA);
                    }
                });
            }
        } catch (Throwable t) {
            logE(TAG, t.getMessage(), t);
        }
    }

    private static void hookPulseInMethod(Method method) {
        if (!Utils.isSamsung()) {
            hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(VALUE_DOZE_IN);
                }
            });
        }
    }

    public static void hookRes(XC_InitPackageResources.InitPackageResourcesParam resParam, XSharedPreferences prefs) {
        resParam.res.setReplacement(Utils.PACKAGE_SYSTEMUI, "bool", DOZE_SUPP, true);
        resParam.res.setReplacement(Utils.PACKAGE_SYSTEMUI, "bool", DOZE_PICK_UP, true);

        initPrefs(prefs);

        if (Build.VERSION.SDK_INT < 22) {
            resParam.res.setReplacement(Utils.PACKAGE_SYSTEMUI, "integer", DOZE_ALPHA, VALUE_DOZE_ALPHA);
        }

        if (Utils.isSamsung()) {
            resParam.res.setReplacement(Utils.PACKAGE_SYSTEMUI, "integer", DOZE_IN, VALUE_DOZE_IN);
            resParam.res.setReplacement(Utils.PACKAGE_SYSTEMUI, "integer", DOZE_OUT, VALUE_DOZE_OUT);
            resParam.res.setReplacement(Utils.PACKAGE_SYSTEMUI, "integer", DOZE_VISIBILITY, VALUE_DOZE_VISIBILITY);
            resParam.res.setReplacement(Utils.PACKAGE_SYSTEMUI, "integer", DOZE_RESETS, VALUE_DOZE_RESETS);
            resParam.res.setReplacement(Utils.PACKAGE_SYSTEMUI, "integer", DOZE_ALPHA, VALUE_DOZE_ALPHA);
            resParam.res.setReplacement(Utils.PACKAGE_SYSTEMUI, "integer", DOZE_PULSE_SCHEDULE, VALUE_DOZE_PULSE_SCHEDULE);
        }
    }

    private static void initPrefs(XSharedPreferences prefs) {
        prefs.reload();
        VALUE_DOZE_IN = prefs.getInt(DOZE_IN, 1000);
        VALUE_DOZE_OUT = prefs.getInt(DOZE_OUT, 1000);
        VALUE_DOZE_VISIBILITY = prefs.getInt(DOZE_VISIBILITY, 3000);
        VALUE_DOZE_RESETS = prefs.getInt(DOZE_RESETS, 1);
        VALUE_DOZE_ALPHA = prefs.getInt(DOZE_ALPHA, 222);
        VALUE_DOZE_PULSE_SCHEDULE = prefs.getString(DOZE_PULSE_SCHEDULE, "10s,30s,60s");
    }

    private static void registerReceiver(final Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SLEEP);
        intentFilter.addAction(ACTION_PREFS_CHANGED);
        intentFilter.addAction(ACTION_HOT_REBOOT);
        context.registerReceiver(sPrefsChange, intentFilter);
    }

    //from gravitybox
    static class SystemProp {
        // Set the value for the given key
        public static void set(String key, String val) {
            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                callStaticMethod(classSystemProperties, "set", key, val);
            } catch (Throwable t) {
                logE(TAG, "SystemProp.set failed: ", t);
            }
        }
    }

}
