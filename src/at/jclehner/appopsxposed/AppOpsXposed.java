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
import android.content.res.XModuleResources;
import at.jclehner.appopsxposed.variants.Sony;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AppOpsXposed implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources
{
	public static final String MODULE_PACKAGE = AppOpsXposed.class.getPackage().getName();
	public static final String SETTINGS_PACKAGE = "com.android.settings";
	public static final String APP_OPS_FRAGMENT = "com.android.settings.applications.AppOpsSummary";
	public static final String APP_OPS_DETAILS_FRAGMENT = "com.android.settings.applications.AppOpsDetails";

	private String mModPath;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable
	{
		mModPath = startupParam.modulePath;
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable
	{
		if(!resparam.packageName.equals("com.android.settings"))
			return;

		log("AppOpsXposed: handleInitPackageResources");

		Util.modRes = XModuleResources.createInstance(mModPath, null);

		resparam.res.setReplacement("com.android.settings", "layout", "app_ops_details_item",
				Util.modRes.fwd(R.layout.app_ops_details_item));
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		if(!lpparam.packageName.equals("com.android.settings"))
			return;

		log("AppOpsXposed: handleLoadPackage");

		try
		{
			lpparam.classLoader.loadClass(APP_OPS_FRAGMENT);
		}
		catch(ClassNotFoundException e)
		{
			log(APP_OPS_FRAGMENT + " class not found; bailing out!");
			return;
		}

		Util.settingsRes = XModuleResources.createInstance(lpparam.appInfo.sourceDir, null);

		if(true)
		{
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
		else
		{
			new Sony().handleLoadPackage(lpparam);
		}
	}


}
