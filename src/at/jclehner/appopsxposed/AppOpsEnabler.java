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
import android.os.Build;
import android.preference.PreferenceActivity.Header;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AppOpsEnabler implements IXposedHookLoadPackage
{
	private static final String APP_OPS_FRAGMENT = "com.android.settings.applications.AppOpsSummary";

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		if(!lpparam.packageName.equals("com.android.settings"))
			return;

		final XModuleResources settingsRes = XModuleResources.createInstance(lpparam.appInfo.sourceDir, null);
		final int personalHeaderId = settingsRes.getIdentifier("id/personal_section", null, lpparam.appInfo.packageName);
		final int appOpsIcon = settingsRes.getIdentifier("drawable/ic_settings_applications", null, lpparam.appInfo.packageName);
		final int appOpsTitleId = settingsRes.getIdentifier("string/app_ops_setting", null, lpparam.appInfo.packageName);

		final String appOpsTitle = appOpsTitleId != 0 ? settingsRes.getString(appOpsTitleId) : "App ops";

		findAndHookMethod("com.android.settings.Settings", lpparam.classLoader,
				"onBuildHeaders", List.class, new XC_MethodHook() {

				@SuppressWarnings("unchecked")
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable
				{
					final List<Header> headers = (List<Header>) param.args[0];
					if(headers == null || headers.isEmpty())
					{
						XposedBridge.log("No headers available; not adding AppOps");
						return;
					}

					final Header appOpsHeader = new Header();
					appOpsHeader.fragment = APP_OPS_FRAGMENT;
					appOpsHeader.title = appOpsTitle;
					appOpsHeader.id = R.id.app_ops_settings;
					appOpsHeader.iconRes = appOpsIcon;

					for(int i = 0; i != headers.size(); ++i)
					{
						if(headers.get(i).id == personalHeaderId)
						{
							headers.add(i + 1, appOpsHeader);
							return;
						}
					}

					// If we reach this point, personalHeaderId was not found, so we
					// just put the AppOps header last

					headers.add(appOpsHeader);
				}
		});

		try
		{
			findAndHookMethod("com.android.settings.Settings", lpparam.classLoader,
					"isValidFragment", String.class, new XC_MethodHook() {

					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{
						if(!((Boolean) param.getResult()) && param.args[0].equals(APP_OPS_FRAGMENT))
							param.setResult(true);
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
}
