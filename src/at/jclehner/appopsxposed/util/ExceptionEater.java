package at.jclehner.appopsxposed.util;


import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;

public class ExceptionEater extends XC_MethodHook
{
	private final Object mResult;
	private final Class<? extends Throwable>[] mExceptionClasses;

	public ExceptionEater(Object result, Class<? extends Throwable>... classes)
	{
		mResult = result;
		mExceptionClasses = classes;
	}

	public ExceptionEater(Class<? extends Throwable>... types)
	{
		this(null, types);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable
	{
		final Throwable t = param.getThrowable();
		final Class<? extends Throwable> tCls = t.getClass();

		if(t != null)
		{
			for(Class<? extends Throwable> cls : mExceptionClasses)
			{
				if(cls.isAssignableFrom(tCls))
				{
					param.setResult(mResult);
					return;
				}
				else
				{
					// Retry in case both classes are from different class loaders
					try
					{
						cls = (Class<? extends Throwable>) tCls.getClassLoader().loadClass(cls.getName());
						if(cls.isAssignableFrom(tCls))
						{
							param.setResult(mResult);
							return;
						}
					}
					catch(ClassNotFoundException e)
					{
						Util.debug(e);
					}
					catch(ClassCastException e)
					{
						Util.debug(e);
					}
				}
			}
		}
	}
}
