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


import java.util.Arrays;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.R;
import at.jclehner.appopsxposed.Util;
import at.jclehner.appopsxposed.Util.XC_MethodHookRecursive;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Sony extends AOSP
{
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

					private Unhook mUnhook;

					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						mUnhook = hookActivityFinish(param);
					}

					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						mUnhook.unhook();
					}
		});

		XposedHelpers.findAndHookMethod(AppOpsXposed.APP_OPS_DETAILS_FRAGMENT, lpparam.classLoader,
				"refreshUi", new XC_MethodHook() {

					private Unhook mUnhook;

					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						mUnhook = hookLayoutInflater();
					}

					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						mUnhook.unhook();
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

	protected Unhook hookActivityFinish(MethodHookParam param) throws Throwable
	{
		final Fragment f = (Fragment) param.thisObject;

		return Util.findAndHookMethodRecursive(	f.getActivity().getClass(),
				"finish", new XC_MethodHookRecursive() {

					@Override
					protected void onBeforeHookedMethod(MethodHookParam param) throws Throwable
					{
						log("Blocked " + param.thisObject.getClass().getName() + ".finish()");
						param.setResult(null);
					}
		});
	}

	protected XC_MethodHook.Unhook hookLayoutInflater() throws Throwable
	{
		return Util.findAndHookMethodRecursive(LayoutInflater.class, "inflate",
				int.class, ViewGroup.class, boolean.class, new XC_MethodHookRecursive() {
					@Override
					protected void onAfterHookedMethod(MethodHookParam param) throws Throwable
					{
						final int layoutResId = (Integer) param.args[0];
						if(layoutResId != Util.getSettingsIdentifier("layout/app_ops_details_item"))
							return;

						debug("In LayoutInflater hook");

						final View view = (View) param.getResult();
						final Spinner spinner = (Spinner) view.findViewWithTag("spinnerWidget");
						if(spinner != null)
						{
							view.findViewWithTag("switchWidget").setVisibility(View.GONE);
							spinner.setVisibility(View.VISIBLE);

							if(spinner.getCount() == 0)
							{
								debug("No items in spinnerWidget");

								final Context context = ((LayoutInflater) param.thisObject).getContext();
								final int arrayResId = Util.getSettingsIdentifier("array/app_ops_permissions");
								final String[] options;

								if(arrayResId != 0)
									options = Util.settingsRes.getStringArray(arrayResId);
								else
									options = Util.modRes.getStringArray(R.array.app_ops_permissions);

								debug("arrayResId=" + arrayResId);
								debug("options=" + Arrays.toString(options));

								final ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
										android.R.layout.simple_spinner_item, options);
								adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

								spinner.setAdapter(adapter);
							}
							else
								debug("spinnerWidget has " + spinner.getCount() + " items");
						}
						else
							log("No spinnerWidget in layout?");

						Util.dumpViewHierarchy(view);
					}
		});
	}
}
