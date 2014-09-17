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


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
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


/*
 * The Settings app on Sony devices (Xperia ROMs at least) is surprisingly
 * vanilla, meaning we can use AOSP.handleLoadPackage().
 *
 * However, Sony decided to make the following modifications:
 *
 * 1) AppOpsSummary can't be launched using the PreferenceActivity.SHOW_FRAGMENT
 *    extra since the hosting Activity is finish()'ed in AppOpsSummary.onCreateView()
 *
 * 2) On JellyBean, in layout/app_ops_details_item, the Switch has been replaced by a
 *    Spinner offering three (!) choices, one of them being "Ask always".
 *
 * The first issue is dealt with by hooking Activity.finish() for the duration of the
 * call to AppOpsSummary.onCreateView(). The second issue is addressed by hooking into
 * LayoutInflater.inflate() in AppOpsDetails.refreshUi, where we can hide the Switch
 * and show the Spinner.
 */
public abstract class Sony extends AOSP
{
	public static class JellyBean extends Sony
	{
		@Override
		protected int apiLevel() {
			return 18;
		}

		@Override
		public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
		{
			super.handleLoadPackage(lpparam);

			if(Util.modPrefs.getBoolean("use_layout_fix", true))
			{
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
		}
	}

	public static class KitKat extends Sony
	{
		@Override
		protected int apiLevel() {
			return 0;
		}
	}

	// only used by fixGetCombinedText at the moment
	private WeakReference<Context> mContextRef = null;

	@Override
	protected abstract int apiLevel();

	@Override
	protected String manufacturer() {
		return "Sony";
	}

	@Override
	protected int getAppOpsHeaderIcon() {
		return Util.appOpsLauncherIcon;
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		super.handleLoadPackage(lpparam);

		fixGetCombinedText(lpparam);

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

		return (Unhook) Util.findAndHookMethodRecursive(f.getActivity().getClass(),
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
		return (Unhook) Util.findAndHookMethodRecursive(LayoutInflater.class, "inflate",
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

	protected void fixGetCombinedText(LoadPackageParam lpparam)
	{
		XposedHelpers.findAndHookMethod(AppOpsXposed.APP_OPS_DETAILS_FRAGMENT, lpparam.classLoader,
						"refreshUi", new XC_MethodHook(XC_MethodHook.PRIORITY_HIGHEST) {
							@Override
							protected void beforeHookedMethod(MethodHookParam param) throws Throwable
							{
								mContextRef = new WeakReference<Context>(
										((Fragment) param.thisObject).getActivity().getApplicationContext());
							}
		});

		XposedHelpers.findAndHookMethod("com.android.settings.applications.AppOpsState$AppOpEntry",
				lpparam.classLoader, "getCombinedText", ArrayList.class, CharSequence[].class, new XC_MethodHook() {

					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{
						if(param.hasThrowable())
						{
							final Throwable t = param.getThrowable();
							if(t instanceof ArrayIndexOutOfBoundsException)
							{
								debug(t);
								param.setResult(getCombinedText((ArrayList<?>) param.args[0], (CharSequence[]) param.args[1]));
							}
						}
					}

					private CharSequence getCombinedText(ArrayList<?> ops, CharSequence[] items)
					{
						if(ops.size() == 1)
							return getOpString(items, ops.get(0));

						final StringBuilder sb = new StringBuilder();
						for(int i = 0; i != ops.size(); ++i)
						{
							if(i != 0)
								sb.append(", ");

							sb.append(getOpString(items, ops.get(i)));
						}

						return sb.toString();
					}

					@TargetApi(19)
					private CharSequence getOpString(CharSequence[] items, Object opObj)
					{
						try
						{
							final int op = (Integer) XposedHelpers.callMethod(opObj, "getOp");
							if(op < items.length)
								return items[op];

							final CharSequence opStr = Util.capitalizeFirst(
									getOpStringFromPermission(mContextRef.get(), op));
							if(opStr != null)
								return opStr;

							return (String) XposedHelpers.callStaticMethod(
									AppOpsManager.class, "opToName", op);
						}
						catch(Throwable t)
						{
							log(t);
							return "?????";
						}
					}


		});
	}

	@TargetApi(19)
	private CharSequence getOpStringFromPermission(Context context, int op)
	{
		try
		{
			final String permission = (String) XposedHelpers.callStaticMethod(
					AppOpsManager.class, "opToPermission", op);

			if(permission != null)
				return Util.getPermissionLabel(context, permission);
		}
		catch(Throwable t)
		{
			debug(t);
		}

		return null;
	}
}
