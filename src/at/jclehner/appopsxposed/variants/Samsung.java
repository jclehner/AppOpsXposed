/*
 * AppOpsXposed - AppOps for Android 4.3+
 * Copyright (C) 2013 Joseph C. Lehner
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of  MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.jclehner.appopsxposed.variants;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Locale;

import android.content.Context;
import android.preference.PreferenceActivity.Header;
import at.jclehner.appopsxposed.ApkVariant;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.Util;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Samsung extends ApkVariant
{
	public static final String GRID_SETTINGS = "com.android.settings.GridSettings";

	private boolean mIsComplete = false;

	@Override
	protected String manufacturer() {
		return "Samsung";
	}

	@Override
	public boolean isComplete() {
		return mIsComplete;
	}

	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable
	{
		hookIsValidFragment(lpparam);
		addAppOpsToAppInfo(lpparam);

		// Adding the fragment to both arrays should pose no problem. No idea why they used
		// UpperCamelCase for a private member...
		hookConstuctorAddFragmentNameToTab(lpparam, "SettingsInMoreTab");
		hookConstuctorAddFragmentNameToTab(lpparam, "SettingsInGeneralTab");

		/*
		 * In SecSettings.apk, the regular "Manage applications" header is referenced in
		 * three xml resources:
		 *
		 * xml/settings_headers (also used in stock Android)
		 * xml/general_headers
		 * xml/management_headers
		 *
		 * To keep things simple and not having to determine which of these is actually used,
		 * we'll hook any call to loadHeadersFromResource and check if the resource argument
		 * matches any of the three. If it does, we'll just put it right after the
		 * "Manage applications" header. The chances of more than one being used at the same time
		 * should be rather slim - if they are, there'll be more than one occurence of "App ops",
		 * so no biggie.
		 */

		final int[] xmlHookResIds = {
				Util.getSettingsIdentifier("xml/settings_headers"),
				Util.getSettingsIdentifier("xml/general_headers"),
				Util.getSettingsIdentifier("xml/management_headers"),
				Util.getSettingsIdentifier("xml/grid_settings_headers")
		};
		final int manageAppsHeaderId = Util.getSettingsIdentifier("id/application_settings");

		debug("xmlHookResIds=" + Arrays.toString(xmlHookResIds));

		if(hasGridSettings(lpparam))
		{
			log("APK has " + GRID_SETTINGS);

			final int gridId = Util.getSettingsIdentifier("bool/settings_grid");
			debug("bool/settings_grid=" + gridId);

			final boolean dontHookNormalSettings = gridId != 0 && Util.settingsRes.getBoolean(gridId);

			hookIsValidFragment(lpparam, GRID_SETTINGS);
			hookLoadHeadersFromResource(lpparam, GRID_SETTINGS, xmlHookResIds, manageAppsHeaderId);

			if(dontHookNormalSettings)
			{
				debug(GRID_SETTINGS + " is enabled; not hooking " + AppOpsXposed.SETTINGS_MAIN_ACTIVITY);
				mIsComplete = true;
				return;
			}
		}

		hookLoadHeadersFromResource(lpparam, AppOpsXposed.SETTINGS_MAIN_ACTIVITY, xmlHookResIds, manageAppsHeaderId);

		mIsComplete = true;
	}

	@Override
	protected Object onCreateAppOpsHeader(Context context, int addAfterHeaderId)
	{
		final Header header = (Header) super.onCreateAppOpsHeader(context, addAfterHeaderId);
		if(addAfterHeaderId == Util.getSettingsIdentifier("xml/grid_settings_headers"))
			header.iconRes = Util.getSettingsIdentifier("drawable/ic_setting_grid_applicationpermissions");

		return header;
	}

	@Override
	protected boolean onMatch(LoadPackageParam lpparam) {
		return lpparam.appInfo.sourceDir.toLowerCase(Locale.US).endsWith("SecSettings.apk".toLowerCase(Locale.US));
	}

	protected final boolean hookConstuctorAddFragmentNameToTab(LoadPackageParam lpparam, final String tabsFieldName)
	{
		try
		{
			final Class<?> settingsClazz = lpparam.classLoader.loadClass(AppOpsXposed.SETTINGS_MAIN_ACTIVITY);
			final Field f = XposedHelpers.findField(settingsClazz, tabsFieldName);
			if(f.getType() != String[].class)
			{
				log("Field exists, but not a string array: " + tabsFieldName);
				return false;
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

	private boolean hasGridSettings(LoadPackageParam lpparam)
	{
		try
		{
			lpparam.classLoader.loadClass(GRID_SETTINGS);
			return true;
		}
		catch(ClassNotFoundException e)
		{
			return false;
		}
	}
}
