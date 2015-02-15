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

import android.content.res.XResources;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;

public class XposedMod implements IXposedHookInitPackageResources,
		IXposedHookZygoteInit {

	@Override
	public void initZygote(IXposedHookZygoteInit.StartupParam startupParam)
			throws Throwable {

		// change values in framework-res
		XResources.setSystemWideReplacement("android", "string",
				"config_dozeComponent",
				"com.android.systemui/com.android.systemui.doze.DozeService");
		XResources.setSystemWideReplacement("android", "bool",
				"config_dozeAfterScreenOff", true);
		XResources.setSystemWideReplacement("android", "bool",
				"config_powerDecoupleInteractiveModeFromDisplay", true);
		XResources.setSystemWideReplacement("android", "integer",
				"config_screenBrightnessDoze", 17);
	}

	@Override
	public void handleInitPackageResources(
			XC_InitPackageResources.InitPackageResourcesParam initPackageResourcesParam)
			throws Throwable {

		// change values in com.android.systemui
		if (!initPackageResourcesParam.packageName
				.equals("com.android.systemui"))
			return;

		initPackageResourcesParam.res.setReplacement("com.android.systemui",
				"bool", "doze_display_state_supported", true);
		initPackageResourcesParam.res.setReplacement("com.android.systemui",
				"bool", "doze_pulse_on_pick_up", true);
	}
}
