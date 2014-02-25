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

import static de.robv.android.xposed.XposedHelpers.getObjectField;

import java.lang.reflect.Method;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import at.jclehner.appopsxposed.ApkVariant;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.R;
import at.jclehner.appopsxposed.Util;
import at.jclehner.appopsxposed.Util.XC_MethodHookRecursive;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;


/*
 * The HTC Sense framework means trouble, as many classes are heavily modified (and often
 * use new names, prefixed with Htc*). HTC also made changes to AppOpsDetails, which
 * interestingly are located in another class - which extends AppOpsDetails - called
 * "com.android.settings.framework.activity.application.appops.HtcAppOpsDetails".
 *
 * HtcAppOpsDetails uses HtcCheckBox instead of the switch found on AOSP, but the state
 * is apparently lost when scrolling off-screen (which usually isn't an issue when an
 * app has only few permissions).
 *
 */
public class HTC extends ApkVariant
{
	public static final String APP_OPS_DETAILS_FRAGMENT =
			"com.android.settings.framework.activity.application.appops.HtcAppOpsDetails";

	private Class<?> mHtcHeaderClass;
	private Class<?> mHtcWrapHeaderClass;
	private Class<?> mHtcWrapHeaderListClass;

	private Method mWrapHeaderListSizeMethod;
	private Method mAddWrapHeaderMethod;

	@Override
	protected String manufacturer() {
		return "HTC";
	}

	@Override
	public boolean isComplete() {
		return true;
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		addAppOpsToAppInfo(lpparam);

		try
		{
			lpparam.classLoader.loadClass(APP_OPS_DETAILS_FRAGMENT);
		}
		catch(ClassNotFoundException e)
		{
			log(e);
		}

		try
		{
			if(Util.modPrefs.getBoolean("htc_use_google_app_ops_category", false))
				hookStartPreferencePanel(lpparam);

			addHtcAppOpsHeader(lpparam);
		}
		catch(Throwable t)
		{
			log(t);
		}
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
	protected Object onCreateAppOpsHeader(Context context) {
		return onCreateHtcHeader();
	}

	@Override
	protected long getIdFromHeader(Object header) {
		return XposedHelpers.getLongField(header, "id");
	}

	@Override
	protected String getAppOpsDetailsFragmentName() {
		return APP_OPS_DETAILS_FRAGMENT;
	}

	private Object onCreateHtcHeader()
	{
		final Object header = XposedHelpers.newInstance(mHtcHeaderClass);
		XposedHelpers.setObjectField(header, "fragment", AppOpsXposed.APP_OPS_FRAGMENT);
		XposedHelpers.setObjectField(header, "title", getAppOpsTitle());
		XposedHelpers.setIntField(header, "iconRes", getAppOpsIcon());
		XposedHelpers.setLongField(header, "id", R.id.app_ops_settings);

		return header;
	}

	private Object onCreateHtcWrapHeader()
	{
		final Object wrapHeader = XposedHelpers.newInstance(mHtcWrapHeaderClass);
		XposedHelpers.setObjectField(wrapHeader, "info", onCreateHtcHeader());
		XposedHelpers.setBooleanField(wrapHeader, "hide", false);

		return wrapHeader;
	}

	private void hookStartPreferencePanel(LoadPackageParam lpparam) throws Throwable
	{
		XposedHelpers.findAndHookMethod("com.htc.preference.HtcPreferenceActivity", lpparam.classLoader,
				"startPreferencePanel", String.class, Bundle.class, CharSequence.class, int.class,
				new XC_MethodHook() {

					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable
					{
						if(((String) param.args[0]).endsWith(".HtcAppOpsDetails"))
						{
							debug("Rewriting fragment name for HtcPreferenceActivity.startPreferencePanel");
							param.args[0] = AppOpsXposed.APP_OPS_DETAILS_FRAGMENT;
						}
					}
		});
	}

	private void addHtcAppOpsHeader(LoadPackageParam lpparam) throws Throwable
	{
		final Class<?> htcGenericEntryProviderClass = lpparam.classLoader.loadClass(
				"com.android.settings.framework.activity.HtcGenericEntryProvider");

		mHtcHeaderClass = lpparam.classLoader.loadClass("com.htc.preference.HtcPreferenceActivity$Header");
		mHtcWrapHeaderClass = lpparam.classLoader.loadClass("com.android.settings.framework.activity.HtcWrapHeader");
		mHtcWrapHeaderListClass = lpparam.classLoader.loadClass("com.android.settings.framework.activity.HtcWrapHeaderList");

		mWrapHeaderListSizeMethod = mHtcWrapHeaderListClass.getMethod("size");
		mAddWrapHeaderMethod = XposedHelpers.findMethodExact(htcGenericEntryProviderClass, "addWrapHeader",
				mHtcWrapHeaderClass, int.class, mHtcWrapHeaderListClass, boolean.class);

		XposedHelpers.findAndHookMethod(htcGenericEntryProviderClass,
				"onLoadEntryList", mHtcWrapHeaderListClass, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{
						final int size = (Integer) mWrapHeaderListSizeMethod.invoke(param.args[0]);
						final Object header = onCreateHtcWrapHeader();

						debug("onLoadEntryList: size=" + size);

						mAddWrapHeaderMethod.invoke(param.thisObject, param.args[0], header, size + 1, true);
					}
		});

		hookLoadHeadersFromResource(lpparam, new XC_MethodHookRecursive() {

			protected void onAfterHookedMethod(MethodHookParam param) throws Throwable
			{
				debug("loadHeadersFromResource: xmlResId=" + param.args[0]);
			}
		});
	}
}
