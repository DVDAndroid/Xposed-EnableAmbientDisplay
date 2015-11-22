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

import static android.content.res.XResources.setSystemWideReplacement;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.ACTION_SLEEP;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_ALPHA;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_BRIGHTNESS;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_IN;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_OUT;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_PICK_UP;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_RESETS;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_SUPP;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.DOZE_VISIBILTY;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.SYS_UI_PKG_NAME;
import static com.dvd.android.xposed.enableambientdisplay.utils.Utils.THIS_PKG_NAME;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XResources;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedMod extends XC_MethodHook
		implements IXposedHookInitPackageResources, IXposedHookZygoteInit,
		IXposedHookLoadPackage {

	private static Context mContext;
	private XSharedPreferences mPrefs;
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ACTION_SLEEP)) {
				sleep();
			}
		}
	};

	private static void sleep() {
		try {
			final PowerManager powerManager = (PowerManager) mContext
					.getSystemService(Context.POWER_SERVICE);

			XposedHelpers.callMethod(powerManager, "goToSleep",
					SystemClock.uptimeMillis());
		} catch (Exception e) {
			XposedBridge.log(e);
		}

	}

	@Override
	public void initZygote(IXposedHookZygoteInit.StartupParam startupParam)
			throws Throwable {

		mPrefs = new XSharedPreferences(THIS_PKG_NAME);
		mPrefs.makeWorldReadable();

		// change values in framework-res
		XResources.setSystemWideReplacement("android", "string",
				"config_dozeComponent",
				"com.android.systemui/com.android.systemui.doze.DozeService");
		XResources.setSystemWideReplacement("android", "bool",
				"config_dozeAfterScreenOff", true);
		XResources.setSystemWideReplacement("android", "bool",
				"config_powerDecoupleInteractiveModeFromDisplay", true);

		setSystemWideReplacement("android", "integer", DOZE_BRIGHTNESS,
				mPrefs.getInt(DOZE_BRIGHTNESS, 17));
	}

	@Override
	public void handleInitPackageResources(
			XC_InitPackageResources.InitPackageResourcesParam resParam)
					throws Throwable {
		String packageName = resParam.packageName;

		if (!packageName.equals(SYS_UI_PKG_NAME))
			return;

		resParam.res.setReplacement(SYS_UI_PKG_NAME, "bool", DOZE_SUPP, true);
		resParam.res.setReplacement(SYS_UI_PKG_NAME, "bool", DOZE_PICK_UP,
				true);

		resParam.res.setReplacement(SYS_UI_PKG_NAME, "integer", DOZE_IN,
				mPrefs.getInt(DOZE_IN, 1000));

		resParam.res.setReplacement(SYS_UI_PKG_NAME, "integer", DOZE_VISIBILTY,
				mPrefs.getInt(DOZE_VISIBILTY, 3000));

		resParam.res.setReplacement(SYS_UI_PKG_NAME, "integer", DOZE_OUT,
				mPrefs.getInt(DOZE_OUT, 1000));

		resParam.res.setReplacement(SYS_UI_PKG_NAME, "integer", DOZE_ALPHA,
				mPrefs.getInt(DOZE_ALPHA, 222));

		resParam.res.setReplacement(SYS_UI_PKG_NAME, "integer", DOZE_RESETS,
				mPrefs.getInt(DOZE_RESETS, 1));

		resParam.res.setReplacement(SYS_UI_PKG_NAME, "integer", DOZE_ALPHA,
				mPrefs.getInt(DOZE_ALPHA, 222));

	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam)
			throws Throwable {
		String packageName = lpparam.packageName;

		if (packageName.equals(THIS_PKG_NAME)) {
			findAndHookMethod(THIS_PKG_NAME + ".MainActivity",
					lpparam.classLoader, "isEnabled",
					XC_MethodReplacement.returnConstant(true));
		}

		if (packageName.equals(SYS_UI_PKG_NAME)) {
			String classKeyguard = "com.android.systemui.keyguard.KeyguardViewMediator";

			final Class<?> keyguardClass = XposedHelpers
					.findClass(classKeyguard, lpparam.classLoader);

			String setupMethodName = Build.VERSION.SDK_INT >= 22 ? "setupLocked"
					: "setup";

			XposedHelpers.findAndHookMethod(keyguardClass, setupMethodName,
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(
								final MethodHookParam param) throws Throwable {
							mContext = (Context) XposedHelpers.getObjectField(
									param.thisObject, "mContext");

							IntentFilter intentFilter = new IntentFilter();
							intentFilter.addAction(ACTION_SLEEP);
							mContext.registerReceiver(mBroadcastReceiver,
									intentFilter);
						}
					});
		}
	}
}