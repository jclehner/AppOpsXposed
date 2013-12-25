package at.jclehner.appopsxposed;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.res.XModuleResources;

import de.robv.android.xposed.XC_MethodHook;
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
		final Object callback = parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
		if(callback instanceof XC_MethodHookRecursive)
		{
			XC_MethodHookRecursive hook = (XC_MethodHookRecursive) callback;
			if(!hook.hasClass())
				hook.setClass(className, classLoader);
		}
		else
			throw new IllegalArgumentException("Callback must extend XC_MethodHookRecursive");

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

	public static class XC_MethodHookRecursive extends XC_MethodHook
	{
		private Class<?> mClass = null;

		/*public XC_MethodHookRecursive(String className, ClassLoader classLoader)
		{
			try
			{
				mClass = classLoader.loadClass(className);
			}
			catch(ClassNotFoundException e)
			{
				throw new IllegalArgumentException(e);
			}
		}

		public XC_MethodHookRecursive(Class<?> clazz) {
			mClass = clazz;
		}*/

		/* package */ void setClass(Class<?> clazz) {
			mClass = clazz;
		}

		/* package */ void setClass(String className, ClassLoader classLoader)
		{
			try
			{
				mClass = classLoader.loadClass(className);
			}
			catch(ClassNotFoundException e)
			{
				throw new IllegalArgumentException(e);
			}
		}

		/* package */ boolean hasClass() {
			return mClass != null;
		}

		@Override
		protected final void beforeHookedMethod(MethodHookParam param) throws Throwable
		{
			if(isValidThisObject(param))
				onBeforeHookedMethod(param);
			else
				debug("Skipping beforeHookedMethod with this=" + param.thisObject.getClass());
		}

		@Override
		protected final void afterHookedMethod(MethodHookParam param) throws Throwable
		{
			if(isValidThisObject(param))
				onAfterHookedMethod(param);
			else
				debug("Skipping afterHookedMethod with this=" + param.thisObject.getClass());
		}

		protected void onBeforeHookedMethod(MethodHookParam param) throws Throwable {}
		protected void onAfterHookedMethod(MethodHookParam param) throws Throwable {}

		private boolean isValidThisObject(MethodHookParam param) {
			return mClass == null || mClass.isInstance(param.thisObject);
		}
	}

	private Util() {}
}
