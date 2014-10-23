package de.robv.android.xposed;

import at.jclehner.appopsxposed.util.Util;

public final class XC_MethodHookRecursive extends XC_MethodHook
{
	private final XC_MethodHook mHook;
	private final Class<?> mTargetClass;

	public XC_MethodHookRecursive(XC_MethodHook hook, Class<?> targetClass)
	{
		mHook = hook;
		mTargetClass = targetClass;
	}

	public Class<?> getTargetClass() {
		return mTargetClass;
	}

	@Override
	protected final void beforeHookedMethod(MethodHookParam param) throws Throwable
	{
		if(!isValidThisObject(param))
			Util.debug("Skipping beforeHookedMethod with " + param.method.getDeclaringClass() + "#" + param.method.getName());
		else
			mHook.beforeHookedMethod(param);
	}

	@Override
	protected final void afterHookedMethod(MethodHookParam param) throws Throwable
	{
		if(!isValidThisObject(param))
			Util.debug("Skipping afterHookedMethod with " + param.method.getDeclaringClass() + "#" + param.method.getName());
		else
			mHook.afterHookedMethod(param);
	}

	private boolean isValidThisObject(MethodHookParam param) {
		return param.thisObject == null || mTargetClass == null || mTargetClass.isInstance(param.thisObject);
	}
}
