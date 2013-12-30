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

import android.os.Bundle;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.Util;
import at.jclehner.appopsxposed.Util.XC_MethodHookRecursive;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Sony extends StockAndroid
{
	private static boolean sSkipNextWindowRebuildCall = false;

	@Override
	public String manufacturer() {
		return "Sony";
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		super.handleLoadPackage(lpparam);
		hookSwitchToHeader(lpparam);
		hookWindowManagerService(lpparam);
	}

	private void hookSwitchToHeader(LoadPackageParam lpparam) throws Throwable
	{
		Util.findAndHookMethodRecursive("com.android.settings.Settings", lpparam.classLoader,
				"switchToHeader", String.class, Bundle.class, new XC_MethodHookRecursive() {
					@Override
					protected void onBeforeHookedMethod(MethodHookParam param) throws Throwable
					{
						if(AppOpsXposed.APP_OPS_FRAGMENT.equals(param.args[0]))
						{
							log("Next call to rebuildAppWindowListLocked will be blocked!");
							sSkipNextWindowRebuildCall = true;
						}
					}
		});
	}

	private void hookWindowManagerService(LoadPackageParam lpparam) throws Throwable
	{
		// TODO consider WindowManagerService.addWindow (mTokenMap.get() returns null apparently?)

		final Class<?> displayContentClass = lpparam.classLoader.loadClass("com.android.server.wm.DisplayContent");

		XposedHelpers.findAndHookMethod("com.android.server.wm.WindowManagerService", lpparam.classLoader,
				"rebuildAppWindowListLocked", displayContentClass, new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable
					{
						if(sSkipNextWindowRebuildCall)
						{
							log("Blocked call to rebuildAppWindowListLocked!");
							sSkipNextWindowRebuildCall = false;
							param.setResult(null);
							return;
						}
						else
							debug("Will call rebuildAppWindowListLocked");
					}
		});
	}
}
