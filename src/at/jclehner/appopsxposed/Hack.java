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

import at.jclehner.appopsxposed.hacks.BootCompletedHack;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Hack implements IXposedHookLoadPackage, IXposedHookZygoteInit
{
	public static final Hack[] HACKS = {
		new BootCompletedHack()
	};

	private String mLogTag;

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

	protected void handleLoadFrameworkPackage(LoadPackageParam lpparam) throws Throwable {

	}

	protected void handleLoadSettingsPackage(LoadPackageParam lpparam) throws Throwable {

	}

	protected final void log(String message) {
		XposedBridge.log(getLogPrefix() + message);
	}

	protected final void log(Throwable t) {
		XposedBridge.log(t);
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
