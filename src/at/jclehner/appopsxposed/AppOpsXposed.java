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

import static at.jclehner.appopsxposed.Util.log;

import android.content.res.XModuleResources;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AppOpsXposed implements IXposedHookZygoteInit, IXposedHookLoadPackage
{
	public static final String MODULE_PACKAGE = AppOpsXposed.class.getPackage().getName();
	public static final String SETTINGS_PACKAGE = "com.android.settings";
	public static final String APP_OPS_FRAGMENT = "com.android.settings.applications.AppOpsSummary";
	public static final String APP_OPS_DETAILS_FRAGMENT = "com.android.settings.applications.AppOpsDetails";

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

		Util.settingsRes = XModuleResources.createInstance(lpparam.appInfo.sourceDir, null);

		for(ApkVariant variant : ApkVariant.getAllMatching(lpparam))
		{
			try
			{
				variant.handleLoadPackage(lpparam);
				if(variant.isComplete())
				{
					log(variant.getClass() + ": [OK+]");
					break;
				}
				log(variant.getClass() + ": [OK]");
			}
			catch(Throwable t)
			{
				log(variant.getClass() + ": [!!]");
				log(t);
			}
		}
	}
}
