package at.jclehner.appopsxposed.variants;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import at.jclehner.appopsxposed.ApkVariant;
import at.jclehner.appopsxposed.Util;

public class SamsungTabbed extends ApkVariant
{
	@Override
	public String manufacturer() {
		return "Samsung";
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		final Class<?> settingsClazz = lpparam.classLoader.loadClass("com.android.settings.Settings");

		XposedBridge.hookAllConstructors(settingsClazz, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{
						final Field f = XposedHelpers.findField(settingsClazz,"SettingsInMoreTab");
						final String[] settingsInMoreTab = (String[]) f.get(param.thisObject);

						f.set(param.thisObject, Util.appendToStringArray(settingsInMoreTab, "applications.AppOpsSummary"));



					}
		});

		Stock.hookIsValidFragment(lpparam);



	}
}
