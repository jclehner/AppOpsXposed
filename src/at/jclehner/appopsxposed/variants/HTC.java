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

import java.lang.reflect.Method;
import java.util.List;

import android.content.Context;
import at.jclehner.appopsxposed.ApkVariant;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.R;
import at.jclehner.appopsxposed.Util;
import at.jclehner.appopsxposed.Util.XC_MethodHookRecursive;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HTC extends ApkVariant
{
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
		final Class<?> htcGenericEntryProviderClass = lpparam.classLoader.loadClass(
				"com.android.settings.framework.activity.HtcGenericEntryProvider");

		mHtcHeaderClass = lpparam.classLoader.loadClass("com.htc.preference.HtcPreferenceActivity$Header");
		mHtcWrapHeaderClass = lpparam.classLoader.loadClass("com.android.settings.framework.activity.HtcWrapHeader");
		mHtcWrapHeaderListClass = lpparam.classLoader.loadClass("com.android.settings.framework.activity.HtcWrapHeaderList");

		mWrapHeaderListSizeMethod = mHtcWrapHeaderListClass.getMethod("size");
		mAddWrapHeaderMethod = XposedHelpers.findMethodBestMatch(htcGenericEntryProviderClass, "addWrapHeader",
				mHtcWrapHeaderClass, int.class, mHtcWrapHeaderListClass, boolean.class);

		XposedHelpers.findAndHookMethod("com.android.settings.framework.activity.HtcGenericEntryProvider",
				lpparam.classLoader, "onLoadEntryList", mHtcWrapHeaderListClass, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{
						final int size = (Integer) mWrapHeaderListSizeMethod.invoke(param.args[0]);
						final Object header = onCreateHtcWrapHeader();

						debug("onLoadEntryList: size=" + size);

						mAddWrapHeaderMethod.invoke(param.args[0], header, size + 1, true);
					}
		});

		XposedHelpers.findAndHookMethod("com.android.settings.Settings", lpparam.classLoader, "onBuildHeaders",
				mHtcHeaderClass, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{
						List<?> headers = (List<?>) param.args[0];
						debug("onBuildHeaders: size=" + headers.size());
						addAppOpsHeader(headers, Util.getSettingsIdentifier("id/personal_section"));
					}

		});

		hookLoadHeadersFromResource(lpparam, new XC_MethodHookRecursive() {

			protected void onAfterHookedMethod(MethodHookParam param) throws Throwable
			{
				debug("loadHeadersFromResource: xmlResId=" + param.args[0]);
			}
		});

		addAppOpsToAppInfo(lpparam);
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
}
