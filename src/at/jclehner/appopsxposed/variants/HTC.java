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

import android.content.Context;
import at.jclehner.appopsxposed.ApkVariant;
import at.jclehner.appopsxposed.AppOpsXposed;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;


/*
 * HTC Sense offers a surprisingly easy method to add items to its
 * "Settings" app (kudos to Mikanoshi@XDA), by using meta-data in
 * AndroidManifest.xml.
 *
 */
public class HTC extends ApkVariant
{
	public static final String APP_OPS_DETAILS_FRAGMENT =
			"com.android.settings.framework.activity.application.appops.HtcAppOpsDetails";

	@Override
	protected String manufacturer() {
		return "HTC";
	}

	@Override
	public boolean canUseLayoutFix() {
		return false;
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		addAppOpsToAppInfo(lpparam);
		fixLaunchTarget(lpparam);
	}

	@Override
	protected String[] indicatorClasses()
	{
		final String[] classes = {
				"com.htc.preference.HtcPreferenceActivity",
				"com.htc.preference.HtcPreferenceActivity$Header",
				"com.android.settings.framework.activity.HtcWrapHeader",
				"com.android.settings.framework.activity.HtcWrapHeaderList",
				"com.android.settings.framework.activity.HtcGenericEntryProvider"
		};

		return classes;
	}

	@Override
	protected String getAppOpsDetailsFragmentName()
	{
		// TODO: use HtcAppOpsDetails if available?
		return super.getAppOpsDetailsFragmentName();
	}

	private void fixLaunchTarget(LoadPackageParam lpparam) throws ClassNotFoundException
	{
		try
		{
			XposedBridge.hookAllMethods(lpparam.classLoader.loadClass(
					"com.android.settings.framework.core.firstpage.plugin.HtcPluginMetadata"),
					"fillHeaderLaunchTarget", new XC_MethodHook() {

						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable
						{
							try
							{
								final Object header = XposedHelpers.getObjectField(param.args[0], "info");
								if(AppOpsXposed.APP_OPS_FRAGMENT.equals(XposedHelpers.getObjectField(header, "fragment")))
									XposedHelpers.setObjectField(header, "fragmentArguments", null);
							}
							catch(Throwable t)
							{
								debug(t);
							}

						}
			});
		}
		catch(ClassNotFoundException e)
		{
			debug(e);
		}
	}
}
