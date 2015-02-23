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

package at.jclehner.appopsxposed;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import at.jclehner.appopsxposed.hacks.BootCompletedHack;
import at.jclehner.appopsxposed.hacks.FixOpsPruneHack;
import at.jclehner.appopsxposed.hacks.FixWakeLock;
import at.jclehner.appopsxposed.hacks.PackageManagerHacks;
import at.jclehner.appopsxposed.util.Res;
import at.jclehner.appopsxposed.util.Util;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class Hack implements IXposedHookLoadPackage, IXposedHookZygoteInit,
		IXposedHookInitPackageResources
{
	public static class PreferenceInfo
	{
		public final boolean defaultValue = false;
		public final String key;
		public final String title;
		public final String summary;

		PreferenceInfo(String key, String title, String summary)
		{
			this.key = key;
			this.title = title;
			this.summary = summary;
		}
	}

	public static final Hack[] HACKS = {
		new BootCompletedHack(),
		new FixWakeLock(),
		new FixOpsPruneHack(),
		//new GmsLocationHack(),
		new PackageManagerHacks()
	};

	private String mLogTag;

	private static final Method sCheckOp = findCheckOp();
	private static final Method sNoteOp = findNoteOp();

	public static List<Hack> getAllEnabled(boolean quiet)
	{
		final List<Hack> hacks = new ArrayList<Hack>();

		if(!quiet)
			Util.log("Enabled hacks:");

		for(Hack hack : HACKS)
		{
			if(Res.modPrefs.getBoolean(hack.getKey(), hack.isEnabledByDefault()))
			{
				hacks.add(hack);
				if(!quiet)
					Util.log("  " + hack.getClass().getSimpleName());
			}
		}

		return hacks;
	}

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {

	}

	@Override
	public final void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		if("android".equals(lpparam.packageName))
			handleLoadFrameworkPackage(lpparam);
		else if(AppOpsXposed.SETTINGS_PACKAGE.equals(lpparam.packageName))
			handleLoadSettingsPackage(lpparam);

		handleLoadAnyPackage(lpparam);
	}

	public final PreferenceInfo getPrefernceInfo(Context context) {
		return new PreferenceInfo(getKey(), onGetPreferenceTitle(context), onGetPreferenceSummary(context));
	}

	protected void handleLoadFrameworkPackage(LoadPackageParam lpparam) throws Throwable {

	}

	protected void handleLoadSettingsPackage(LoadPackageParam lpparam) throws Throwable {

	}

	protected void handleLoadAnyPackage(LoadPackageParam lpparam) throws Throwable {

	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {

	}

	protected String onGetPreferenceTitle(Context context) {
		return getPreferenceString(context, "title");
	}

	protected String onGetPreferenceSummary(Context context) {
		return getPreferenceString(context, "summary");
	}

	protected boolean isEnabledByDefault() {
		return false;
	}

	protected abstract String onGetKeySuffix();

	protected final void log(String message) {
		Util.log(getLogPrefix() + message);
	}

	protected final void debug(String message) {
		Util.debug(getLogPrefix() + message);
	}

	protected final void debug(Throwable t) {
		Util.debug(t);
	}

	protected final String getPreferenceString(Context context, String suffix, Object... formatArgs)
	{
		final String name = "use_hack_" + onGetKeySuffix() + "_" + suffix;
		final int resId = context.getResources().getIdentifier(
				name, "string", AppOpsXposed.MODULE_PACKAGE);

		if(resId != 0)
			return context.getString(resId, formatArgs);

		return name;
	}

	protected static boolean hasCheckOp() {
		return sCheckOp != null;
	}

	@TargetApi(19)
	protected static void noteOperation(Context context, int op, int uid, String packageName)
	{
		if(sNoteOp == null)
			return;

		try
		{
			sNoteOp.invoke(context.getSystemService(Context.APP_OPS_SERVICE),
					op, uid, packageName);
		}
		catch(IllegalAccessException e)
		{
			Util.log(e);
		}
		catch(IllegalArgumentException e)
		{
			Util.log(e);
		}
		catch(InvocationTargetException e)
		{
			Util.log(e);
		}
	}

	@TargetApi(19)
	protected static int checkOp(Context context, int op, int uid, String packageName)
	{
		if(hasCheckOp())
		{
			try
			{
				return (Integer) sCheckOp.invoke(
						context.getSystemService(Context.APP_OPS_SERVICE), op, uid, packageName);
			}
			catch(IllegalAccessException e)
			{
				Util.log(e);
			}
			catch(IllegalArgumentException e)
			{
				Util.log(e);
			}
			catch(InvocationTargetException e)
			{
				Util.log(e);
			}
		}

		return AppOpsManager.MODE_ALLOWED;
	}

	protected static int checkOp(Context context, int op)
	{
		final ApplicationInfo info = context.getApplicationInfo();
		return checkOp(context, op, info.uid, info.packageName);
	}

	private String getKey() {
		return "use_hack_" + onGetKeySuffix();
	}

	private String getLogPrefix()
	{
		if(mLogTag == null)
		{
			final String name = getClass().getName().replace('$', '.');
			final String pkgName = getClass().getPackage().getName();

			if(name.startsWith(pkgName))
				mLogTag = "AOX:" + name.substring(pkgName.length() + 1) + ": ";
			else
				mLogTag = "AOX:" + name + ": ";
		}

		return mLogTag;
	}

	@TargetApi(19)
	private static Method findCheckOp()
	{
		try
		{
			return XposedHelpers.findMethodExact(AppOpsManager.class, "checkOp",
					int.class, int.class, String.class);
		}
		catch(Throwable t)
		{
			Util.log("AppOpsManager.checkOp not available");
			return null;
		}
	}

	@TargetApi(19)
	private static Method findNoteOp()
	{
		try
		{
			return XposedHelpers.findMethodExact(AppOpsManager.class, "noteOp",
					int.class, int.class, String.class);
		}
		catch(Throwable t)
		{
			Util.log("AppOpsManager.noteOp not available");
			return null;
		}
	}
}
