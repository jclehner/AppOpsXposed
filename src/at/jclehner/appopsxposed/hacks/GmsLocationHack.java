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

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import android.app.AppOpsManager;
import android.content.Context;
import at.jclehner.appopsxposed.Hack;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper;
import at.jclehner.appopsxposed.util.Util;
import at.jclehner.appopsxposed.util.Util.StringList;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class GmsLocationHack extends Hack
{
	private static final boolean DEBUG = true;

	private static final int RESULT_SUCCESS = 0;

	private static final int[] LOCATION_OPS = {
			Util.getOpValue("OP_COARSE_LOCATION"),
			Util.getOpValue("OP_FINE_LOCATION"),
			Util.getOpValue("OP_MONITOR_LOCATION"),
			Util.getOpValue("OP_MONITOR_HIGH_POWER_LOCATION")
	};

	private Set<String> mClasses;

	@Override
	protected String onGetKeySuffix() {
		return "gms_location";
	}

	@Override
	protected void handleLoadAnyPackage(LoadPackageParam lpparam) throws Throwable
	{
		mClasses = Util.getClassList(lpparam, "com.google.android.gms.location", true);
		if(mClasses == null || mClasses.isEmpty())
			return;

		final StringBuilder sb = new StringBuilder(lpparam.packageName);

		if(mClasses.contains("com.google.android.gms.location.a"))
			sb.append(" [obfuscated?]");

		sb.append(": ");

		final StringList sl = new StringList();
		if(hackLocationServices(lpparam.classLoader, lpparam.packageName))
			sl.add("LocationServices");

		if(hackLocationClient(lpparam.classLoader, lpparam.packageName))
			sl.add("LocationClient");

		if(hackGeofenceApi(lpparam.classLoader, lpparam.packageName))
			sl.add("Geofence");

		if(!sl.isEmpty())
			sb.append(sl.toString());
		else
			sb.append("(no hooks installed!)");

		Util.debug(sb.toString());
	}

	private boolean hackLocationClient(ClassLoader classLoader, String packageName)
	{
		final Class<?> locationClientClazz;
		try
		{
			locationClientClazz = classLoader.loadClass(
					"com.google.android.gms.location.LocationClient");

			hookCtorForContextGrab(locationClientClazz);

			final String[] functions = {
					"connect",
					"requestLocationUpdates",
					"removeLocationUpdates",
					"getLastLocation"
			};

			for(String f : functions)
			{
				XposedBridge.hookAllMethods(locationClientClazz,
						f, mBlockerHook).size();
			}

			return true;
		}
		catch(Throwable t)
		{
			return false;
		}
	}

	private boolean hackLocationServices(ClassLoader classLoader, String packageName)
	{
		final Class<?> locationSvcClazz;

		try
		{
			locationSvcClazz = classLoader.loadClass(
					"com.google.android.gms.location.LocationServices");
		}
		catch(Throwable t)
		{
			return false;
		}

		final Class<?> fusedLpClazz;
		final Class<?> geofencingApiClazz;

		try
		{
			fusedLpClazz = locationSvcClazz.getField("FusedLocationProviderApi").getClass();
			geofencingApiClazz = locationSvcClazz.getField("GeofencingApi").getClass();
		}
		catch(Throwable t)
		{
			Util.log(t);
			return false;
		}

		final String[] fusedLpMethods = {
				"requestLocationUpdates",
				"removeLocationUpdates"
		};

		for(String m : fusedLpMethods)
			XposedBridge.hookAllMethods(fusedLpClazz, m, mBlockerHookWithStatus);

		XposedBridge.hookAllMethods(fusedLpClazz, "getLastLocation", mBlockerHook);

		final String[] geofencingMethods = {
				"addGeofences",
				"removeGeofences"
		};

		for(String m : geofencingMethods)
			XposedBridge.hookAllMethods(geofencingApiClazz, m, mBlockerHookWithStatus);

		return true;
	}

	private boolean hackGeofenceApi(ClassLoader classLoader, String packageName)
	{
		final Class<?> geofenceBuilderClazz;
		try
		{
			geofenceBuilderClazz = classLoader.loadClass(
					"com.google.android.gms.location.Geofence$Builder");

			final String[] functions = {
					"setCircularRegion",
					"setExpirationDuration",
					"setLoiteringDelay",
					"setNotificationResponsiveness",
					"setRequestId",
					"setTransitionTypes"
			};

			for(String f : functions)
			{
				XposedBridge.hookAllMethods(geofenceBuilderClazz,
						f, mBuilderHook).size();
			}

			return true;
		}
		catch(Throwable t)
		{
			return false;
		}
	}

	private static void hookCtorForContextGrab(Class<?> clazz)
	{
		XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				for(Object arg : param.args)
				{
					if(arg instanceof Context)
					{
						XposedHelpers.setAdditionalInstanceField(param.thisObject, "context",
								new WeakReference<Context>((Context) arg));
						break;
					}
				}
			}

		});
	}

	@SuppressWarnings("unchecked")
	private static Context grabContext(MethodHookParam param)
	{
		return ((WeakReference<Context>) XposedHelpers.getAdditionalInstanceField(param.thisObject,
				"context")).get();
	}

	private static boolean isAnyLocationOpDisabled(MethodHookParam param)
	{
		final Context c = grabContext(param);
		return c != null && isAnyLocationOpDisabled(c);
	}

	private static boolean isAnyLocationOpDisabled(Context context)
	{
		final AppOpsManagerWrapper appOps = AppOpsManagerWrapper.from(context);
		final int uid = context.getApplicationInfo().uid;

		for(int op : LOCATION_OPS)
		{
			if(appOps.checkOpNoThrow(op, uid, context.getPackageName()) != AppOpsManager.MODE_ALLOWED)
				return true;
		}

		return false;
	}

	private static String toString(MethodHookParam param) {
		return param.thisObject.getClass().getSimpleName() + "." + param.method.getName();
	}

	private static Object createPendingResult(final ClassLoader classLoader, final int statusCode)
	{
		final Class<?>[] interfaces = {
				XposedHelpers.findClass("com.google.android.gms.common.api.PendingResult",
						classLoader)
		};

		return Proxy.newProxyInstance(classLoader, interfaces, new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
			{
				if("await".equals(method.getName()))
				{
					final Class<?> clazz = XposedHelpers.findClass(
							"com.google.android.gms.common.api.Status", classLoader);
					return XposedHelpers.newInstance(clazz, new Class<?>[] { int.class }, statusCode);
				}
				else if("isCanceled".equals(method.getName()))
					return false;

				return null;
			}
		});
	}

	private final XC_MethodHook mBlockerHook = new XC_MethodHook() {

		protected void beforeHookedMethod(MethodHookParam param) throws Throwable
		{
			final Context c = grabContext(param);
			if(c != null && isAnyLocationOpDisabled(c))
			{
				param.setResult(null);
				if(DEBUG)
					log("Blocked " + GmsLocationHack.toString(param) + " for app " + c.getPackageName());
			}
			else if(DEBUG && c == null)
				log("Context is null in hook for " + param.method.getName());
		}
	};

	private final XC_MethodHook mBlockerHookWithStatus = new XC_MethodHook()
	{
		protected void beforeHookedMethod(MethodHookParam param) throws Throwable
		{
			if(!isAnyLocationOpDisabled(param))
				return;

			final ClassLoader cl = param.thisObject.getClass().getClassLoader();
			param.setResult(createPendingResult(cl, 0));
			if(DEBUG)
				log("Blocked " + GmsLocationHack.toString(param) + " for app " + grabContext(param).getPackageName());
		}
	};

	private final XC_MethodHook mBuilderHook = new XC_MethodHook() {
		@Override
		protected void beforeHookedMethod(MethodHookParam param) throws Throwable
		{
			final Context c = grabContext(param);
			if(c != null && isAnyLocationOpDisabled(c))
			{
				param.setResult(param.thisObject);
				if(DEBUG)
					log("Blocked " + param.method.getName() + " for app " + c.getPackageName());
			}
			else if(DEBUG && c == null)
				log("Context is null in hook for " + param.method.getName());
		}
	};
}



