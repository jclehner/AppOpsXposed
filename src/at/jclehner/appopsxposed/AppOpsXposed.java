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

package at.jclehner.appopsxposed;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import android.content.res.XModuleResources;
import android.os.Build;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AppOpsXposed implements IXposedHookZygoteInit, IXposedHookLoadPackage
{
	public static final String MODULE_PACKAGE = AppOpsXposed.class.getPackage().getName();
	public static final String SETTINGS_PACKAGE = "com.android.settings";
	public static final String APP_OPS_FRAGMENT = "com.android.settings.applications.AppOpsSummary";
	public static final String APP_OPS_DETAILS_FRAGMENT = "com.android.settings.applications.AppOpsDetails";

	private static final String[] VALID_FRAGMENTS = {
		AppOpsXposed.APP_OPS_FRAGMENT, AppOpsXposed.APP_OPS_DETAILS_FRAGMENT
	};

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable
	{
		Util.modRes = XModuleResources.createInstance(startupParam.modulePath, null);
		Util.modPrefs = new XSharedPreferences(MODULE_PACKAGE);
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		if(!lpparam.packageName.equals("com.android.settings"))
			return;

		try
		{
			lpparam.classLoader.loadClass(APP_OPS_FRAGMENT);
		}
		catch(ClassNotFoundException e)
		{
			log(APP_OPS_FRAGMENT + " class not found; bailing out!");
			return;
		}

		// Do this here as it's not dependent on a specific APK version.
		hookIsValidFragment(lpparam);

		Util.settingsRes = XModuleResources.createInstance(lpparam.appInfo.sourceDir, null);

		log("Trying variants...");

		for(ApkVariant variant : ApkVariant.getAllMatching(lpparam))
		{
			final String variantName = "  " + variant.getClass().getSimpleName();

			try
			{
				variant.handleLoadPackage(lpparam);
				if(variant.isComplete())
				{
					log(variantName + ": [OK+]");
					break;
				}
				log(variantName + ": [OK]");
			}
			catch(Throwable t)
			{
				log(variantName + ": [!!]");
				log(t);
			}
		}
	}

	private static void hookIsValidFragment(LoadPackageParam lpparam)
	{
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
			// so we ignore a NoSuchMethodError in that case.

			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
				throw e;
		}
	}
}
