package at.jclehner.appopsxposed.hacks;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.AppOpsManager;
import android.content.Context;
import at.jclehner.appopsxposed.Hack;
import at.jclehner.appopsxposed.Util;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class GmsLocationHack extends Hack
{
	private static final boolean DEBUG = true;

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
		if(!hasCheckOp())
			return;

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

		log(sb.toString());
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
					"removeLocationUpdates"
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
		final Class<?> apiBuilderClazz;
		final Class<?> locationSvcClazz;
		final Object locationApi;
		try
		{
			apiBuilderClazz = classLoader.loadClass(
					"com.google.android.gms.common.api.GoogleApiClient$Builder");

			locationSvcClazz = classLoader.loadClass(
					"com.google.android.gms.location.LocationServices");

			locationApi = XposedHelpers.getStaticObjectField(locationSvcClazz,
					"API");

			hookCtorForContextGrab(apiBuilderClazz);
			XposedBridge.hookAllMethods(apiBuilderClazz, "addApi",
					new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable
						{
							if(param.args[0] == locationApi)
							{
								final Context c = grabContext(param);
								if(c != null && isAnyLocationOpDisabled(c))
								{
									log("Denying GMS location API to app " + c.getPackageName());
									param.setResult(null);
								}
							}
						}
			});

			return true;
		}
		catch(Throwable t)
		{
			return false;
		}
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
		for(int op : LOCATION_OPS)
		{
			if(checkOp(context, op) != AppOpsManager.MODE_ALLOWED)
				return true;
		}

		return false;
	}

	private static String toString(MethodHookParam param) {
		return param.thisObject.getClass().getSimpleName() + "." + param.method.getName();
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

	static class StringList
	{
		private final List<String> mList = new ArrayList<String>();

		void add(String string) {
			mList.add(string);
		}

		boolean isEmpty() {
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
}

class ObfuscationHelper
{





}



