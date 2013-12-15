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
import at.jclehner.appopsxposed.variants.StockAndroid;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AppOpsEnabler implements IXposedHookZygoteInit, IXposedHookLoadPackage
{
	public static final String MODULE_PACKAGE = AppOpsEnabler.class.getPackage().getName();
	static final String SETTINGS_PACKAGE = "com.android.settings";


	@Override
	public void initZygote(StartupParam startupParam) throws Throwable
	{
		Util.modRes = XModuleResources.createInstance(startupParam.modulePath, null);
		Util.modPrefs = new XSharedPreferences(MODULE_PACKAGE);
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		if(!lpparam.packageName.equals(SETTINGS_PACKAGE))
			return;

		Util.settingsRes = XModuleResources.createInstance(lpparam.appInfo.sourceDir, null);

		boolean blockStockVariant = false;

		for(ApkVariant variant : ApkVariant.getAllMatching(lpparam))
		{
			try
			{
				variant.handleLoadPackage(lpparam);
				blockStockVariant |= variant.blocksStockVariant();
				log(variant.getClass() + ": [OK]");
			}
			catch(Throwable t)
			{
				log(variant.getClass() + ": [!!]");
				log(t);
			}
		}

		if(!blockStockVariant)
			new StockAndroid().handleLoadPackage(lpparam);
	}
}
