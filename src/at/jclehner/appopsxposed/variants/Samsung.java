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

	@Override
	protected String manufacturer() {
		return "Samsung";
	}

	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable
	{
		addAppOpsToAppInfo(lpparam);

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

		final int manageAppsHeaderId = Res.getSettingsIdentifier("id/application_settings");

		hookLoadHeadersFromResource(lpparam, AppOpsXposed.SETTINGS_MAIN_ACTIVITY, xmlHookResIds, manageAppsHeaderId);

		if(hasGridSettings(lpparam))
			hookLoadHeadersFromResource(lpparam, GRID_SETTINGS, xmlHookResIds, manageAppsHeaderId);
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
