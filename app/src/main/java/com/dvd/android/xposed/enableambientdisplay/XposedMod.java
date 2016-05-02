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


import com.dvd.android.xposed.enableambientdisplay.hook.AndroidHook;
import com.dvd.android.xposed.enableambientdisplay.hook.SystemUiHook;
import com.dvd.android.xposed.enableambientdisplay.utils.Utils;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static android.content.res.XResources.setSystemWideReplacement;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_BRIGHTNESS;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.PACKAGE_SYSTEMUI;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.THIS_PKG_NAME;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.logD;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.logW;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class XposedMod implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

    private static final String TAG = "XposedMod";
    private static XSharedPreferences sPrefs;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        sPrefs = new XSharedPreferences(Utils.THIS_PKG_NAME, MainActivity.class.getSimpleName());
        logD(TAG, sPrefs.toString());
        Utils.debug = sPrefs.getBoolean("debug", false);

        if (!sPrefs.getBoolean("can_read_prefs", false)) {
            // With SELinux enforcing, it might happen that we don't have access
            // to the prefs file. Test this by reading a test key that should be
            // set to true. If it is false, we either can't read the file or the
            // user has never opened the preference screen before.
            // Credits to AndroidN-ify
            logW(TAG, "Can't read prefs file, default values will be applied in hooks!");
        }

        setSystemWideReplacement("android", "string", "config_dozeComponent", "com.android.systemui/com.android.systemui.doze.DozeService");
        setSystemWideReplacement("android", "bool", "config_dozeAfterScreenOff", true);
        setSystemWideReplacement("android", "bool", "config_powerDecoupleInteractiveModeFromDisplay", true);

        setSystemWideReplacement("android", "integer", DOZE_BRIGHTNESS, sPrefs.getInt(DOZE_BRIGHTNESS, 17));
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        switch (lpparam.packageName) {
            case PACKAGE_SYSTEMUI:
                logD(TAG, "Hooking SystemUI");
                SystemUiHook.hook(lpparam.classLoader, sPrefs);
                break;
            case Utils.PACKAGE_ANDROID:
                logD(TAG, "Hooking Android package");
                AndroidHook.hook(lpparam.classLoader, sPrefs);
                break;
            case THIS_PKG_NAME:
                logD(TAG, "Hooking this module");
                findAndHookMethod(THIS_PKG_NAME + ".MainActivity", lpparam.classLoader, "isEnabled", XC_MethodReplacement.returnConstant(true));
                break;
        }
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        if (resparam.packageName.equals(PACKAGE_SYSTEMUI)) {
            logD(TAG, "Hooking SystemUI resources");
            SystemUiHook.hookRes(resparam, sPrefs);
        }
    }
}