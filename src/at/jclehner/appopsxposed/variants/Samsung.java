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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.preference.PreferenceActivity;
import android.preference.PreferenceActivity.Header;
import at.jclehner.appopsxposed.ApkVariant;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.util.Res;
import at.jclehner.appopsxposed.util.Util;
import at.jclehner.appopsxposed.util.XUtils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Samsung extends ApkVariant
{
	public static final String GRID_SETTINGS = "com.android.settings.GridSettings";

	private boolean mIsUsingGridSettings = false;

	@Override
	protected String manufacturer() {
		return "Samsung";
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
		 * three (or four) xml resources:
		 *
		 * xml/settings_headers (also used in stock Android)
		 * xml/general_headers
		 * xml/management_headers
		 * xml/grid_settings (on S5 ROMs)
		 *
		 * To keep things simple and not having to determine which of these is actually used,
		 * we'll hook any call to loadHeadersFromResource and check if the resource argument
		 * matches any of the three. If it does, we'll just put it right after the
		 * "Manage applications" header. The chances of more than one being used at the same time
		 * should be rather slim - if they are, there'll be more than one occurence of "App ops",
		 * so no biggie.
		 */

		final int[] xmlHookResIds = {
				Res.getSettingsIdentifier("xml/settings_headers"),
				Res.getSettingsIdentifier("xml/general_headers"),
				Res.getSettingsIdentifier("xml/management_headers"),
				Res.getSettingsIdentifier("xml/grid_settings_headers")
		};

		debug("xmlHookResIds=" + Arrays.toString(xmlHookResIds));

		final int manageAppsHeaderId = Res.getSettingsIdentifier("id/application_settings");

		hookLoadHeadersFromResource(lpparam, AppOpsXposed.SETTINGS_MAIN_ACTIVITY, xmlHookResIds, manageAppsHeaderId);

		if(hasGridSettings(lpparam))
		{
			log("APK has " + GRID_SETTINGS);

			final int gridId = Res.getSettingsIdentifier("bool/settings_grid");
			debug("bool/settings_grid=" + gridId);

			mIsUsingGridSettings = gridId != 0 && Res.settingsRes.getBoolean(gridId);

			hookIsValidFragment(lpparam, GRID_SETTINGS);
			hookLoadHeadersFromResource(lpparam, GRID_SETTINGS, xmlHookResIds, manageAppsHeaderId);
			hookGridSettingsHeaderClick(lpparam);
		}
	}

	@Override
	protected Object onCreateAppOpsHeader(Context context, int addAfterHeaderId)
	{
		final Header header = (Header) super.onCreateAppOpsHeader(context, addAfterHeaderId);
		if(mIsUsingGridSettings && header.intent == null)
		{
			header.intent = new Intent();
			header.intent.setClassName(context.getPackageName(), AppOpsXposed.SETTINGS_MAIN_ACTIVITY);
			header.intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AppOpsXposed.APP_OPS_FRAGMENT);
		}

		return header;
	}

	@Override
	protected String[] indicatorClasses() {
		return new String[] {
				"com.sec.android.samsungapps.util.ServiceBinder",
				"com.sec.android.samsungapps.util.PreloadUpdate",
				"com.sec.android.touchwiz.widget.TwTouchPunchView",
				"com.android.settings.helpdialog.TwTouchPunchView",
				GRID_SETTINGS
		};
	}

	protected final boolean hookConstuctorAddFragmentNameToTab(LoadPackageParam lpparam, final String tabsFieldName)
	{
		try
		{
			final Class<?> settingsClazz = lpparam.classLoader.loadClass(AppOpsXposed.SETTINGS_MAIN_ACTIVITY);
			final Field f = XposedHelpers.findField(settingsClazz, tabsFieldName);
			if(f.getType() != String[].class)
			{
				debug("Field exists, but not a string array: " + tabsFieldName);
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

	private void hookGridSettingsHeaderClick(LoadPackageParam lpparam) throws Throwable
	{
		XUtils.findAndHookMethodRecursive(GRID_SETTINGS, lpparam.classLoader,
				"onHeaderClick", Header.class, int.class, new XC_MethodHook() {

					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						setRegularSettingsEnabled(param, true);
					}

					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						setRegularSettingsEnabled(param, false);
					}

					private void setRegularSettingsEnabled(MethodHookParam param, boolean enabled)
					{
						try
						{
							final Context ctx = (Context) param.thisObject;
							ctx.getPackageManager().setComponentEnabledSetting(
									new ComponentName(ctx.getPackageName(), AppOpsXposed.SETTINGS_MAIN_ACTIVITY),
									enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
									: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
									PackageManager.DONT_KILL_APP);
						}
						catch(Throwable t)
						{
							debug(t);
						}
					}
		});
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
