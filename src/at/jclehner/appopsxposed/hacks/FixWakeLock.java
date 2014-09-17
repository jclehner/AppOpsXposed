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

import java.util.Set;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import at.jclehner.appopsxposed.Hack;
import at.jclehner.appopsxposed.Util;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;


@TargetApi(19)
public class FixWakeLock extends Hack
{
	public static final FixWakeLock INSTANCE = new FixWakeLock();

	private static final boolean ENABLE_PER_TAG_FILTERING = false;
	private static final boolean DEBUG = true;

	private static final int OP_WAKE_LOCK = getOpWakeLock();

	private Set<Unhook> mUnhooks;

	@Override
	public void initZygote(StartupParam param) throws Throwable
	{
		if(OP_WAKE_LOCK == -1)
		{
			log("No OP_WAKE_LOCK; bailing out!");
			return;
		}
		else if(!hasCheckOp())
			return;

		hookMethods(ClassLoader.getSystemClassLoader());
	}

	@Override
	protected void handleLoadFrameworkPackage(LoadPackageParam lpparam) throws Throwable
	{
		hookMethods(lpparam.classLoader);
	}

	@Override
	protected String onGetKeySuffix() {
		return "wake_lock";
	}

	private void hookMethods(ClassLoader classLoader) throws Throwable
	{
		final Class<?> pwrMgrSvcClazz = classLoader.loadClass(
				"com.android.server.power.PowerManagerService");

		mUnhooks = XposedBridge.hookAllMethods(pwrMgrSvcClazz, "acquireWakeLock", mAcquireHook);
		log("Hooked " + mUnhooks.size() + " functions");
	}

	private boolean canAcquire(String packageName, String tag)
	{
		if(ENABLE_PER_TAG_FILTERING)
		{
			final String blacklistKey = "wakelock_hack_is_blacklist/" + packageName;
			if(!Util.modPrefs.contains(blacklistKey))
				return false;

			final boolean isBlacklist = Util.modPrefs.getBoolean(blacklistKey, true);
			final Set<String> tags = Util.modPrefs.getStringSet("wakelock_hack_tags/" + packageName, null);
			if(tags == null || tags.isEmpty() || !tags.contains(tag))
				return isBlacklist;

			return !isBlacklist;
		}

		return false;
	}

	private static int getOpWakeLock()
	{
		try
		{
			return XposedHelpers.getStaticIntField(AppOpsManager.class, "OP_WAKE_LOCK");
		}
		catch(Throwable t)
		{
			return -1;
		}
	}

	private final XC_MethodHook mAcquireHook = new XC_MethodHook() {
		@Override
		protected void beforeHookedMethod(MethodHookParam param) throws Throwable
		{
			final IBinder lock = (IBinder) param.args[0];
			final String tag = (String) param.args[2];
			final String packageName = (String) param.args[3];

			final int uid = Binder.getCallingUid();

			// Since we want this hack to replicate the expected behaviour,
			// we have to do some sanity checks first. On error we return
			// and let the hooked function throw an exception.

			if(lock == null || packageName == null)
				return;

			final Context ctx = (Context)
					XposedHelpers.getObjectField(param.thisObject, "mContext");
			if(ctx == null)
				return;

			ctx.enforceCallingOrSelfPermission(
					android.Manifest.permission.WAKE_LOCK, null);

			if(checkOp(ctx, OP_WAKE_LOCK, uid, packageName) != AppOpsManager.MODE_ALLOWED)
			{
				if(tag != null && canAcquire(packageName, tag))
				{
					if(DEBUG)
					{
						log("Allowing acquisition of WakeLock '" + tag +
								"' for app " + packageName);
					}
					return;
				}

				if(DEBUG)
				{
					log("Prevented acquisition of WakeLock '" + tag +
							"' for app " + packageName);
				}

				param.setResult(null);
			}
		}
	};
}

