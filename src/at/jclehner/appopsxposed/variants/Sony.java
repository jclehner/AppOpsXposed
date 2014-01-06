/*
 * AppOpsXposed - AppOps for Android 4.3+
 * Copyright (C) 2013, 2014 Joseph C. Lehner
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


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.Util;
import at.jclehner.appopsxposed.Util.XC_MethodHookRecursive;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Sony extends AOSP
{
	private static boolean sIgnoreActivityFinish = false;

	@Override
	protected String manufacturer() {
		return "Sony";
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		super.handleLoadPackage(lpparam);

		/**
		 * The Settings app on Sony devices (Xperia ROMs at least) is surprisingly
		 * vanilla, meaning we can use AOSP.handleLoadPackage().
		 *
		 * Sony decided to disable AppOps by calling getActivity().finish() in
		 * AppOpSummary.onCreateView(), so we block all calls to finish() that happen
		 * in onCreateView.
		 */

		XposedHelpers.findAndHookMethod(AppOpsXposed.APP_OPS_FRAGMENT, lpparam.classLoader,
				"onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class,
				new XC_MethodHook() {

					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						sIgnoreActivityFinish = true;
					}

					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						sIgnoreActivityFinish = false;
					}
		});

		Util.findAndHookMethodRecursive("com.android.settings.Settings", lpparam.classLoader,
				"finish", new XC_MethodHookRecursive() {

				@Override
				protected void onBeforeHookedMethod(MethodHookParam param) throws Throwable
				{
					if(sIgnoreActivityFinish)
					{
						log("Blocked " + param.thisObject.getClass().getName() + ".finish()");
						param.setResult(null);
					}
				}
		});
	}

	@Override
	protected String[] indicatorClasses()
	{
		final String[] classes = {
				"com.sonymobile.settings.SomcSettingsHeader",
				"com.sonymobile.settings.preference.util.SomcPreferenceActivity",
				"com.sonymobile.settings.preference.util.SomcSettingsPreferenceFragment"
		};

		return classes;
	}
}



