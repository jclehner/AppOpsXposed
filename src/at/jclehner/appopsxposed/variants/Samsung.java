package at.jclehner.appopsxposed.variants;

import java.lang.reflect.Field;
import java.util.Arrays;

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
	// No idea why they used UpperCamelCase for a private member...

	private static final String[][] TAB_INFOS = {
		{ "SettingsInGeneralTab", "general_headers" },
		{ "SettingsInMoreTab", "more_headers" }
	};

	private boolean mIsComplete = false;

	@Override
	public String manufacturer() {
		return "Samsung";
	}

	@Override
	public boolean isComplete() {
		return mIsComplete;
	}

	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable
	{
		String xmlResName = null;

		for(String[] tabInfo : TAB_INFOS)
		{
			if(hookConstuctorAddFragmentNameToTab(lpparam, tabInfo[0]))
			{
				xmlResName = tabInfo[1];
				break;
			}
		}

		if(xmlResName == null)
		{
			log("No matching tab-info found; defaulting to general_headers");
			xmlResName = "general_headers";
		}
		else
			debug("Using xmlResName=" + xmlResName);

		final int xmlResId = Util.getSettingsIdentifier("xml/" + xmlResName);
		if(xmlResId == 0)
		{
			log("xml/" + xmlResId + " not found in resources");
			return;
		}

		hookOnCreate(lpparam, xmlResId);
		hookIsValidFragment(lpparam);

		mIsComplete = true;
	}

	public static boolean hookConstuctorAddFragmentNameToTab(LoadPackageParam lpparam, final String tabsFieldName)
	{
		try
		{
			final Class<?> settingsClazz = lpparam.classLoader.loadClass("com.android.settings.Settings");
			final Field f = XposedHelpers.findField(settingsClazz, tabsFieldName);
			if(f.getType() != String[].class)
			{
				log("Warning: field exists, but not a string array: " + tabsFieldName);
				return true;
			}

			XposedBridge.hookAllConstructors(settingsClazz, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{

						final Field f = XposedHelpers.findField(settingsClazz, tabsFieldName);
						final String[] settingsInMoreTab = (String[]) f.get(param.thisObject);

						f.set(param.thisObject, Util.appendToStringArray(settingsInMoreTab, "AppOpsSummary"));
						debug(tabsFieldName + "=" + f.get(param.thisObject));
					}
			});

			return true;
		}
		catch(Throwable t)
		{
			log("No " + tabsFieldName + " field?");
			debug(t);
			return false;
		}
	}

	public static boolean hookOnCreate(final LoadPackageParam lpparam, final int headersXmlResId)
	{
		try
		{
			final Class<?> settingsClazz = lpparam.classLoader.loadClass("com.android.settings.Settings");
			final Field isTabStyleField = XposedHelpers.findField(settingsClazz, "mPhoneTabStyle");

			XposedHelpers.findAndHookMethod(settingsClazz, "onCreate",
					Bundle.class, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable
						{
							final boolean isTabStyle = isTabStyleField.getBoolean(param.thisObject);
							debug("isTabStyle=" + isTabStyle);
							StockAndroid.hookLoadHeadersFromResource(lpparam, headersXmlResId);
						}
			});

			return true;
		}
		catch(NoSuchFieldError e)
		{
			log("mPhoneTabStyle not found?; trying xml/settings_headers");
			return false;
		}
		catch(Throwable t)
		{
			log(t);
			return false;
		}
	}
}
