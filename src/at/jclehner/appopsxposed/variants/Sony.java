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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceActivity.Header;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.Util;
import at.jclehner.appopsxposed.Util.XC_MethodHookRecursive;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Sony extends AOSP
{
	public static final String SLAVE_ACTIVITY_NAME = "com.android.settings.applications.InstalledAppDetailsTop";

	public static final String EXTRA_LAUNCH_APP_OPS = "at.jclehner.appopsxposed.LAUNCH_APP_OPS";
	public static final String EXTRA_LAUNCH_APP_OPS_DETAILS = "at.jclehner.appopsxposed.LAUNCH_APP_OPS_DETAILS";

	private Class<?> mSlaveActivityClass;

	@Override
	protected String manufacturer() {
		return "Sony";
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		/* For some obscure reason, launching AppOps on Sony Xperia devices
		 * fails silently. The intent is delivered, but the AppOps screen is never
		 * displayed. The reason behind this is not clear to me, but it appears to
		 * have something to do with the activity which hosts the AppOpsSummary Fragment.
		 *
		 * To work around this limitation, I'm hooking an arbitrary Activity, in this
		 * case the activity that hosts the "App info" page, and make it host the AppOps
		 * Fragments.
		 *
		 * The following extras are used EXTRA_LAUNCH_APP_OPS forces the Activity to load
		 * the AppOps overview, while EXTRA_LAUNCH_APP_OPS_DETAILS loads the AppOps details.
		 */

		mSlaveActivityClass = lpparam.classLoader.loadClass(SLAVE_ACTIVITY_NAME);

		log("Enslaving " + SLAVE_ACTIVITY_NAME);

		hookIsValidFragment(mSlaveActivityClass);

		XposedHelpers.findAndHookMethod(mSlaveActivityClass, "getIntent", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				final Intent intent = (Intent) param.getResult();
				// XXX only for debugging purposes - prevents multiple calls to getIntent from
				// spamming the logs
				final Intent origIntent = new Intent(intent);

				if(intent.getBooleanExtra(EXTRA_LAUNCH_APP_OPS, false))
					intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AppOpsXposed.APP_OPS_FRAGMENT);
				else if(intent.getBooleanExtra(EXTRA_LAUNCH_APP_OPS_DETAILS, false))
					intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AppOpsXposed.APP_OPS_DETAILS_FRAGMENT);

				if(!intent.equals(origIntent))
					debug("getIntent:" + "\n  intent=" + intent + "\n  extras=" + intent.getExtras());
			}
		});

		Util.findAndHookMethodRecursive(mSlaveActivityClass,
				"onBuildStartFragmentIntent", String.class, Bundle.class,
				int.class, int.class, new XC_MethodHookRecursive() {

					@Override
					protected void onAfterHookedMethod(MethodHookParam param) throws Throwable
					{
						final Intent intent = (Intent) param.getResult();

						if(AppOpsXposed.APP_OPS_DETAILS_FRAGMENT.equals(param.args[0]))
							intent.putExtra(EXTRA_LAUNCH_APP_OPS_DETAILS, true);
						else if(AppOpsXposed.APP_OPS_FRAGMENT.equals(param.args[0]))
							intent.putExtra(EXTRA_LAUNCH_APP_OPS, true);

						debug("onBuildStartFragment: name=" + param.args[0]);
					}
		});

		try
		{
			Util.findAndHookMethodRecursive(mSlaveActivityClass,
					"onCreate", Bundle.class, new XC_MethodHookRecursive() {

						@Override
						protected void onAfterHookedMethod(MethodHookParam param) throws Throwable
						{
							final PreferenceActivity pa = (PreferenceActivity) param.thisObject;
							final Intent intent = pa.getIntent();
							if(intent != null)
							{
								if(intent.getBooleanExtra(EXTRA_LAUNCH_APP_OPS_DETAILS, false))
									pa.setTitle(Util.getSettingsIdentifier("string/app_ops_settings"));
								else if(intent.getBooleanExtra(EXTRA_LAUNCH_APP_OPS, false))
									pa.setTitle(Util.getSettingsIdentifier("string/app_ops_settings"));

								debug("onCreate:" + "\n  intent=" + intent + "\n  extras=" + intent.getExtras());
							}
						}
			});
		}
		catch(Throwable t)
		{
			log(t);
		}

		super.handleLoadPackage(lpparam);
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

	@Override
	protected Object onCreateAppOpsHeader(Context context)
	{
		final Header header = (Header) super.onCreateAppOpsHeader(context);
		header.fragment = null;
		header.intent = new Intent(context, mSlaveActivityClass);
		header.intent.putExtra(EXTRA_LAUNCH_APP_OPS, true);

		return header;
	}
}
