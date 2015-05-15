/*
 * AppOpsXposed - AppOps for Android 4.3+
 * Copyright (C) 2013-2015 Joseph C. Lehner
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
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class FixOpsPruneHack extends Hack {

	@Override
	protected String onGetKeySuffix() {
		return "fix_prune";
	}

	@Override
	public void handleLoadFrameworkPackage(LoadPackageParam lpparam) throws Throwable
	{
		final Class<?> appOpsSvcClazz = lpparam.classLoader.loadClass(
				"com.android.server.AppOpsService");
		// This is extremely crude, but should suffice for a temporary fix. See
		// 9bb3f6785d6 in CyanogenMod/android_frameworks_base for an infinitely more
		// elegant solution.
		XposedBridge.hookAllMethods(appOpsSvcClazz, "systemReady",
				XC_MethodReplacement.DO_NOTHING);
	}
}
