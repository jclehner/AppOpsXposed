/*
 * AppOpsXposed - AppOps for Android 4.4.2
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

package at.jclehner.appopsxposed;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.util.List;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.os.Build;
import android.preference.PreferenceActivity.Header;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AppOpsEnabler implements IXposedHookZygoteInit, IXposedHookLoadPackage
{
	static final String MODULE_PACKAGE = AppOpsEnabler.class.getPackage().getName();
	static final String SETTINGS_APK_PACKAGE = "com.android.settings";

	static final String APP_OPS_FRAGMENT = "com.android.settings.applications.AppOpsSummary";
	static final String APP_OPS_DETAILS_FRAGMENT = "com.android.settings.applications.AppOpsDetails";

	private static final String[] VALID_FRAGMENTS = {
		APP_OPS_FRAGMENT, APP_OPS_DETAILS_FRAGMENT
	};

	//private String mModPath = null;
	private XModuleResources mModRes = null;
	private XModuleResources mSettingsRes = null;
	private XSharedPreferences mPrefs = null;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable
	{
		log("initZygote: modulePath=" + startupParam.modulePath);

		mModRes = XModuleResources.createInstance(startupParam.modulePath, null);
		mPrefs = new XSharedPreferences(MODULE_PACKAGE);
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		if(!lpparam.packageName.equals(SETTINGS_APK_PACKAGE))
			return;

		mSettingsRes = XModuleResources.createInstance(lpparam.appInfo.sourceDir, null);

		findAndHookMethod("com.android.settings.Settings", lpparam.classLoader,
				"onBuildHeaders", List.class, new XC_MethodHook() {

				@SuppressWarnings("unchecked")
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable
				{
					final List<Header> headers = (List<Header>) param.args[0];
					addAppOpsHeader(headers);
				}
		});

		try
		{
			findAndHookMethod("com.android.settings.Settings", lpparam.classLoader,
					"isValidFragment", String.class, new XC_MethodHook() {

					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable
					{
						for(String name : VALID_FRAGMENTS)
						{
							if(name.equals(param.args[0]))
							{
								param.setResult(true);
								return;
							}
						}
					}
			});

		}
		catch(NoSuchMethodError e)
		{
			// Apps before KitKat didn't need to override PreferenceActivity.isValidFragment,
			// so ignore a NoSuchMethodError in that case

			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
				throw e;
		}
	}

	private void addAppOpsHeader(List<Header> headers)
	{
		if(headers == null || headers.isEmpty())
		{
			log("addAppOpsHeader: list is empty or null");
			return;
		}

		final int personalHeaderId = mSettingsRes.getIdentifier("id/personal_section", null, SETTINGS_APK_PACKAGE);
		final int appOpsIcon = mSettingsRes.getIdentifier("drawable/ic_settings_applications", null, SETTINGS_APK_PACKAGE);
		final int appOpsTitleId = mSettingsRes.getIdentifier("string/app_ops_setting", null, SETTINGS_APK_PACKAGE);
		final String appOpsTitle = appOpsTitleId != 0 ? mSettingsRes.getString(appOpsTitleId) : mModRes.getString(R.string.app_ops_setting);

		final Header appOpsHeader = new Header();
		appOpsHeader.fragment = APP_OPS_FRAGMENT;
		appOpsHeader.title = appOpsTitle;
		appOpsHeader.id = R.id.app_ops_settings;
		appOpsHeader.iconRes = appOpsIcon;

		int personalHeaderIndex = -1;

		for(int i = 0; i != headers.size(); ++i)
		{
			if(headers.get(i).id == personalHeaderId)
				personalHeaderIndex = i;
			else if(headers.get(i).id == R.id.app_ops_settings)
			{
				log("addAppOpsHeader: header was already added");
				return;
			}
		}

		if(personalHeaderIndex != -1)
			headers.add(personalHeaderIndex + 1, appOpsHeader);
		else
			headers.add(appOpsHeader);
	}

	private static void findAndHookMethodRecursive(String className, ClassLoader classLoader,
			String methodName, Object... parameterTypesAndCallback)
	{
		try
		{
			findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
		}
		catch(NoSuchMethodError e)
		{
			final Class<?> superClass;
			try
			{
				superClass = classLoader.loadClass(className).getSuperclass();
			}
			catch(ClassNotFoundException e1)
			{
				throw new IllegalStateException(e1);
			}

			log("findAndHookMethodRecursive: trying " + superClass);
			findAndHookMethodRecursive(superClass.getName(), classLoader, methodName, parameterTypesAndCallback);
		}
	}

	private static void log(String message)
	{
		if(!BuildConfig.DEBUG)
			return;

		XposedBridge.log("AppOpsXposed: " + message);
	}
}
