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

import static at.jclehner.appopsxposed.Util.log;
import android.content.res.XModuleResources;
import at.jclehner.appopsxposed.variants.CyanogenMod;
import at.jclehner.appopsxposed.R;
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
		Util.modPrefs = new XSharedPreferences(AppOpsXposed.class.getPackage().getName());
		if(!Util.modPrefs.makeWorldReadable())
			log("Failed to make preference file world-readable");
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable
	{
		if(!resparam.packageName.equals("com.android.settings"))
			return;

		if(Util.modPrefs.getBoolean("failsafe_mode", false))
			return;

		Util.modRes = XModuleResources.createInstance(mModPath, null);

		if(Util.modPrefs.getBoolean("use_layout_fix", true))
		{
			if(!CyanogenMod.isCm11After20140128())
			{
				resparam.res.setReplacement("com.android.settings", "layout", "app_ops_details_item",
						Util.modRes.fwd(R.layout.app_ops_details_item));
			}
			else
				log("Detected cm11 nightly >= 20140128; not using layout fix despite setting");
		}
		else
			log("Not using layout fix");
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

		if(Util.modPrefs.getBoolean("failsafe_mode", false))
		{
			log("Running in failsafe mode");
			ApkVariant.hookIsValidFragment(lpparam);
			return;
		}

		final String forceVariant = Util.modPrefs.getString("force_variant", "");
		if(forceVariant.length() == 0)
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
			final Class<?> variantClazz = Class.forName("at.jclehner.appopsxposed.variants." + forceVariant);
			((ApkVariant) variantClazz.newInstance()).handleLoadPackage(lpparam);
		}
	}
}
