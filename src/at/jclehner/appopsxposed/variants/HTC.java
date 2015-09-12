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

import android.app.Activity;
import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import at.jclehner.appopsxposed.ApkVariant;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.util.Constants;
import de.robv.android.xposed.XC_MethodHook;
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
			XposedHelpers.findAndHookMethod("com.android.settings.framework.content.plugin.HtcPluginMetadata", lpparam.classLoader,
					"fillLaunchTarget", "com.htc.preference.HtcPreferenceActivity.Header", ActivityInfo.class, new XC_MethodHook() {

					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{
						try
						{
							Object header = param.args[0];
							if(AppOpsXposed.APP_OPS_FRAGMENT.equals(XposedHelpers.getObjectField(header, "fragment")))
							{
								Bundle fragmentArguments = new Bundle();
								fragmentArguments.putString("android.intent.extra.PACKAGE_NAME", AppOpsXposed.SETTINGS_PACKAGE);
								XposedHelpers.setObjectField(header, "fragmentArguments", fragmentArguments);
							}
						}
						catch(Throwable t)
						{
							debug(t);
						}
					}
			});

			XposedHelpers.findAndHookMethod(AppOpsXposed.APP_OPS_FRAGMENT, lpparam.classLoader,
					"onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {

					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{
						try
						{
							Object mHandlerWrapper = XposedHelpers.callMethod(param.thisObject, "getActivityHandlerWrapper");
							if(mHandlerWrapper != null)
							{
								Activity act = ((Fragment) param.thisObject).getActivity();
								XposedHelpers.callMethod(mHandlerWrapper, "setTitle", act.getResources().getIdentifier("app_ops_settings", "string", AppOpsXposed.SETTINGS_PACKAGE));
							}
						}
						catch(Throwable t)
						{
							debug(t);
						}
					}
			});
		}
		catch(Throwable t)
		{
			debug(t);
		}
	}

	@Override
	protected int getDefaultAppOpsHeaderIcon() {
		return Constants.ICON_COG_GREY;
	}
}
