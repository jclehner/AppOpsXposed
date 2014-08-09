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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import at.jclehner.appopsxposed.hacks.BootCompletedHack;
import at.jclehner.appopsxposed.hacks.FixWakeLock;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class Hack implements IXposedHookLoadPackage, IXposedHookZygoteInit
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
		new FixWakeLock()
	};

	private String mLogTag;

	public static List<Hack> getAllEnabled()
	{
		final List<Hack> hacks = new ArrayList<Hack>();

		Util.log("Enabled hacks:");

		for(Hack hack : HACKS)
		{
			if(Util.modPrefs.getBoolean(hack.getKey(), false))
			{
				hacks.add(hack);
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
	}

	public final PreferenceInfo getPrefernceInfo(Context context) {
		return new PreferenceInfo(getKey(), onGetPreferenceTitle(context), onGetPreferenceSummary(context));
	}

	protected void handleLoadFrameworkPackage(LoadPackageParam lpparam) throws Throwable {

	}

	protected void handleLoadSettingsPackage(LoadPackageParam lpparam) throws Throwable {

	}

	protected String onGetPreferenceTitle(Context context) {
		return getPreferenceString(context, "title");
	}

	protected String onGetPreferenceSummary(Context context) {
		return getPreferenceString(context, "summary");
	}

	protected abstract String onGetKeySuffix();

	protected final void log(String message) {
		XposedBridge.log(getLogPrefix() + message);
	}

	protected final void log(Throwable t) {
		XposedBridge.log(t);
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
}
