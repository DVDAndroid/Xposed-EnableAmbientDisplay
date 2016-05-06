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
import android.os.PowerManager;

import com.dvd.android.xposed.enableambientdisplay.utils.Utils;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;

import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_WITH_POWER_KEY;
import static de.robv.android.xposed.XposedBridge.invokeOriginalMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class AndroidHook {

    private static final String TAG = "AndroidHook";
    private static final String CLASS_PHONE_WINDOW_MANAGER = "com.android.internal.policy.impl.PhoneWindowManager";
    private static final String CLASS_PHONE_WINDOW_MANAGER_23 = "com.android.server.policy.PhoneWindowManager";
    private static boolean POWER_KEY_OVERWRITE = false;

    public static void hook(ClassLoader classLoader, final XSharedPreferences prefs) {
        if (Build.VERSION.SDK_INT == 21) return;

        String hookClass = Build.VERSION.SDK_INT >= 23 ? CLASS_PHONE_WINDOW_MANAGER_23 : CLASS_PHONE_WINDOW_MANAGER;

        findAndHookMethod(hookClass, classLoader, "wakeUpFromPowerKey", long.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {

                        Context mContext = (Context) getObjectField(param.thisObject, "mContext");
                        registerReceiver(mContext);

                        sendAction(mContext, param, prefs);
                        return null;
                    }
        });
    }

    private static void registerReceiver(final Context context) {
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                POWER_KEY_OVERWRITE = intent.getBooleanExtra(DOZE_WITH_POWER_KEY, false);
            }
        }, new IntentFilter(Utils.ACTION_PREFS_CHANGED));
    }

    private static void initPrefs(XSharedPreferences prefs) {
        prefs.reload();
        POWER_KEY_OVERWRITE = prefs.getBoolean(DOZE_WITH_POWER_KEY, false);
    }

    private static void sendAction(final Context context, final XC_MethodHook.MethodHookParam param, XSharedPreferences prefs) throws InvocationTargetException, IllegalAccessException {
        initPrefs(prefs);
        PowerManager mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (POWER_KEY_OVERWRITE) {
            if (mPowerManager.isPowerSaveMode()) {
                invokeOriginalMethod(param.method, param.thisObject, param.args);
                Utils.logD(TAG, "Cannot doze. Power saving mode on");
            } else {
                context.sendBroadcast(new Intent(Utils.ACTION_DOZE));
                Utils.logD(TAG, "Device turned up. Dozing");
            }
        } else {
            invokeOriginalMethod(param.method, param.thisObject, param.args);
        }
    }

}
