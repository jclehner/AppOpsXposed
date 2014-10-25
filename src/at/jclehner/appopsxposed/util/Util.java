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

package at.jclehner.appopsxposed.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import dalvik.system.DexFile;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public final class Util
{
	public interface Logger
	{
		void log(String s);
		void log(Throwable t);
	}

	public static Logger logger = new Logger() {

		@Override
		public void log(Throwable t)
		{
			Log.w("AOX", t);
		}

		@Override
		public void log(String s)
		{
			Log.i("AOX", s);
		}
	};

	private static boolean sDebug = true;

	public static void log(Throwable t) {
		logger.log(t);
	}

	public static void log(String s) {
		logger.log(s);
	}

	public static void debug(Throwable t)
	{
		if(sDebug)
			logger.log(t);
	}

	public static void debug(String s)
	{
		if(sDebug)
			logger.log(s);
	}

	public static boolean isXposedModuleEnabled() {
		return false;
	}

	public static boolean isUsingBootCompletedHack(Context context)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				"use_hack_boot_completed", false);
	}

	public static boolean containsManufacturer(String str) {
		return Build.MANUFACTURER.toLowerCase(Locale.US).contains(str.toLowerCase());
	}

	public static String getAoxVersion(Context context)
	{
		final PackageManager pm = context.getPackageManager();
		try
		{
			return pm.getPackageInfo(context.getPackageName(), 0).versionName;
		}
		catch(NameNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static String[] appendToStringArray(String[] array, String str)
	{
		final ArrayList<String> list = new ArrayList<String>(Arrays.asList(array));
		list.add(str);

		final String[] newArray = new String[list.size()];
		return list.toArray(newArray);
	}

	public static String getSystemProperty(String key, String defValue)
	{
		try
		{
			final Method m = Class.forName("android.os.SystemProperties").
					getMethod("get", String.class, String.class);

			return (String) m.invoke(null, key, defValue);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return defValue;
		}
	}

	public static void dumpViewHierarchy(View v)
	{
		debug("dumpViewHierarchy: ");
		dumpViewHierarchyInternal(v, 0);
	}

	public static Set<String> getClassList(ApplicationInfo appInfo, String packageName, boolean getSubPackages)
	{
		final DexFile df;
		try
		{
			df = new DexFile(appInfo.sourceDir);
		}
		catch(IOException e)
		{
			log(e);
			return null;
		}

		final Enumeration<String> entries = df.entries();
		final Set<String> classes = new HashSet<String>();

		while(entries.hasMoreElements())
		{
			final String entry = entries.nextElement();

			if(packageName != null)
			{
				if(!entry.startsWith(packageName))
					continue;

				if(!getSubPackages && entry.substring(packageName.length() + 1).contains("."))
					continue;
			}

			classes.add(entry);
		}

		return classes;
	}

	public static Set<String> getClassList(LoadPackageParam lpparam) {
		return getClassList(lpparam, null, true);
	}

	public static Set<String> getClassList(LoadPackageParam lpparam, String packageName, boolean getSubPackages)
	{
		if(lpparam.appInfo == null)
			return null;

		return getClassList(lpparam.appInfo, packageName, getSubPackages);
	}

	private static void dumpViewHierarchyInternal(View view, int level)
	{
		debug(pad(2 * level) + view);

		if(view instanceof ViewGroup)
		{
			final ViewGroup vg = (ViewGroup) view;
			for(int i = 0; i != vg.getChildCount(); ++i)
				dumpViewHierarchyInternal(vg.getChildAt(i), level + 1);
		}
	}

	private static String pad(int length)
	{
		final StringBuilder sb = new StringBuilder(length);
		for(int i = 0; i != length; ++i)
			sb.append(' ');

		return sb.toString();
	}

	public static String capitalizeFirst(CharSequence text)
	{
		if(text == null)
			return null;

		if(text.length() == 0 || !Character.isLowerCase(text.charAt(0)))
			return text.toString();

		return Character.toUpperCase(text.charAt(0)) + text.subSequence(1, text.length()).toString();
	}

	@TargetApi(19)
	public static int getOpValue(String opName)
	{
		try
		{
			return AppOpsManager.class.getField(opName).getInt(null);
		}
		catch(NoSuchFieldException e)
		{
			// ignore
		}
		catch(IllegalAccessException e)
		{
			// ignore
		}
		catch(IllegalArgumentException e)
		{
			// ignore
		}

		return -1;
	}

	public static class StringList
	{
		private final List<CharSequence> mList = new ArrayList<CharSequence>();

		public void add(String string) {
			mList.add(string);
		}

		public boolean isEmpty() {
			return mList.isEmpty();
		}

		@Override
		public String toString()
		{
			final StringBuilder sb = new StringBuilder();

			for(int i = 0; i != mList.size(); ++i)
			{
				if(i != 0)
					sb.append(", ");

				sb.append(mList.get(i));
			}

			return sb.toString();
		}

	}

	private Util() {}
}
