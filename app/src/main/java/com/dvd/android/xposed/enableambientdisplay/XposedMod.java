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
