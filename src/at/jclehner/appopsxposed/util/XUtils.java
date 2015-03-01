package at.jclehner.appopsxposed.util;

import android.content.pm.ApplicationInfo;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XC_MethodHookRecursive;
import de.robv.android.xposed.XposedHelpers;

public final class XUtils
{
	public static boolean isInFailsafeMode() {
		return Res.modPrefs.getBoolean("failsafe_mode", false);
	}

	public static boolean isCompatibilityModeEnabled()
	{
		return true;
		//return Res.modPrefs.getBoolean("compatibility_mode", false);
	}

	public static ApplicationInfo getApplicationInfo(String packageName)
	{
		final Class<?> atClazz = XposedHelpers.findClass("android.app.ActivityThread", null);
		final Object pm = XposedHelpers.callStaticMethod(atClazz, "getPackageManager");

		try
		{
			final ApplicationInfo ai = (ApplicationInfo) XposedHelpers.callMethod(pm,
					"getApplicationInfo", new Class<?>[] { String.class, int.class, int.class },
					packageName, 0, 0);

			if(ai != null)
				return ai;
		}
		catch(Exception e)
		{
			Util.debug(e);
		}

		Util.log("getApplicationInfo failed for " + packageName);

		return null;
	}

	public static Unhook findAndHookMethodRecursive(String className, ClassLoader classLoader,
			String methodName, Object... parameterTypesAndCallback) throws Throwable
	{
		return findAndHookMethodRecursive(classLoader.loadClass(className), methodName, parameterTypesAndCallback);
	}

	public static Unhook findAndHookMethodRecursive(Class<?> clazz,
			String methodName, Object... parameterTypesAndCallback) throws Throwable
	{
		XC_MethodHook hook = (XC_MethodHook) parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
		if(!(hook instanceof XC_MethodHookRecursive))
		{
			hook = new XC_MethodHookRecursive(hook, clazz);
			parameterTypesAndCallback[parameterTypesAndCallback.length - 1] = hook;
		}

		try
		{
			return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
		}
		catch(NoSuchMethodError e)
		{
			final Class<?> superClass = clazz.getSuperclass();
			if(superClass == null)
			{
				// We can't just rethrow the caught exception since the message would not be
				// meaningful (clazz is Object.class at this point).
				final Class<?> targetClass = ((XC_MethodHookRecursive) hook).getTargetClass();
				throw new NoSuchMethodError(targetClass.getName() + "." + methodName);
			}

			Util.debug("findAndHookMethodRecursive: trying " + superClass + "." + methodName);
			return findAndHookMethodRecursive(superClass, methodName, parameterTypesAndCallback);
		}
	}


	private XUtils() {}
}
