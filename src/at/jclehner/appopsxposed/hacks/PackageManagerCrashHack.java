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

package at.jclehner.appopsxposed.hacks;

import at.jclehner.appopsxposed.Hack;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;


public class PackageManagerCrashHack extends Hack
{
	@Override
	public void initZygote(StartupParam param) throws Throwable
	{
		final Class<?> pmSvcClazz = Class.forName("com.android.server.pm.PackageManagerService");
		XposedBridge.hookAllMethods(pmSvcClazz, "addPackageHoldingPermissions",
				new XC_MethodHook(XC_MethodHook.PRIORITY_LOWEST) {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{
						final Throwable t = param.getThrowable();
						if(t != null && (t instanceof NullPointerException))
						{
							param.setResult(null);
							log("Consumed NPE:");
							log(t);
						}
					}
		});
	}

	@Override
	protected String onGetKeySuffix() {
		return "pm_crash";
	}

	@Override
	protected boolean isEnabledByDefault() {
		return true;
	}
}
