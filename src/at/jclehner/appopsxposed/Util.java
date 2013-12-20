package at.jclehner.appopsxposed;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.res.XModuleResources;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public final class Util
{
	private static final boolean DEBUG = true;

	public static XModuleResources settingsRes;
	public static XModuleResources modRes;
	public static XSharedPreferences modPrefs;

	public static int getSettingsIdentifier(String name) {
		return settingsRes.getIdentifier(name, null, AppOpsXposed.SETTINGS_PACKAGE);
	}

	public static String getSettingsString(int resId) {
		return settingsRes.getString(resId);
	}

	public static String getModString(int resId) {
		return modRes.getString(resId);
	}

	public static void debug(String message)
	{
		if(!DEBUG)
			return;

		XposedBridge.log(message);
	}

	public static void debug(Throwable t)
	{
		if(!DEBUG)
			return;

		XposedBridge.log(t);
	}

	public static String[] appendToStringArray(String[] array, String str)
	{
		final ArrayList<String> list = new ArrayList<String>(Arrays.asList(array));
		list.add(str);

		final String[] newArray = new String[list.size()];
		return list.toArray(newArray);
	}

	public static void findAndHookMethodRecursive(String className, ClassLoader classLoader,
			String methodName, Object... parameterTypesAndCallback)
	{
		try
		{
			findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
		}
		catch(NoSuchMethodError e)
		{
			final Class<?> superClass;
			try
			{
				superClass = classLoader.loadClass(className).getSuperclass();
			}
			catch(ClassNotFoundException e1)
			{
				throw new IllegalStateException(e1);
			}

			debug("findAndHookMethodRecursive: trying " + superClass);
			findAndHookMethodRecursive(superClass.getName(), classLoader, methodName, parameterTypesAndCallback);
		}
	}

	private Util() {}
}
