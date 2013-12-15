package at.jclehner.appopsxposed.variants;

import java.lang.reflect.Field;

import static at.jclehner.appopsxposed.Util.debug;
import static at.jclehner.appopsxposed.Util.log;

import android.os.Bundle;
import at.jclehner.appopsxposed.ApkVariant;
import at.jclehner.appopsxposed.Util;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Samsung extends ApkVariant
{
	@Override
	public String manufacturer() {
		return "Samsung";
	}

	@Override
	public boolean blocksStockVariant() {
		return true;
	}

	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable
	{
		final Class<?> settingsClazz = lpparam.classLoader.loadClass("com.android.settings.Settings");

		XposedBridge.hookAllConstructors(settingsClazz, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{
						// No idea why they used UpperCamelCase for a private member...
						final Field f = XposedHelpers.findField(settingsClazz, "SettingsInGeneralTab");
						final String[] settingsInMoreTab = (String[]) f.get(param.thisObject);

						f.set(param.thisObject, Util.appendToStringArray(settingsInMoreTab, "AppOpsSummary"));
						debug("SettingsInGeneralTab=" + f.get(param.thisObject));
					}
		});

		XposedHelpers.findAndHookMethod(settingsClazz, "onCreate",
				Bundle.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{

						try
						{
							final boolean isTabStyle = XposedHelpers.getBooleanField(param.thisObject, "mPhoneTabStyle");
							debug("Samsung: isTabStyle=" + isTabStyle);

							final int xmlResId = Util.getSettingsIdentifier(isTabStyle ? "xml/general_headers" : "xml/settings_headers");
							StockAndroid.hookLoadHeadersFromResource(lpparam, xmlResId);
						}
						catch(NoSuchFieldError e)
						{
							log("Samsung: isTabStyle not found; assuming list style!");
							StockAndroid.hookOnBuildHeaders(lpparam);
						}
					}
		});

		StockAndroid.hookIsValidFragment(lpparam);
	}
}
