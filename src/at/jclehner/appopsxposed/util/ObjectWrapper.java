package at.jclehner.appopsxposed.util;

import java.lang.reflect.InvocationTargetException;


public class ObjectWrapper
{
	public static class ReflectiveException extends RuntimeException
	{
		private static final long serialVersionUID = 4913462741847291648L;

		public ReflectiveException(Exception cause) {
			super(cause);
		}
	}

	protected final Object mObj;

	public ObjectWrapper(Object obj) {
		mObj = obj;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String fieldName)
	{
		try
		{
			return (T) mObj.getClass().getField(fieldName).get(mObj);
		}
		catch(IllegalAccessException e)
		{
			throw new ReflectiveException(e);
		}
		catch(IllegalArgumentException e)
		{
			throw new ReflectiveException(e);
		}
		catch(NoSuchFieldException e)
		{
			throw new ReflectiveException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getStatic(String fieldName)
	{
		try
		{
			return (T) mObj.getClass().getField(fieldName).get(null);
		}
		catch(IllegalAccessException e)
		{
			throw new ReflectiveException(e);
		}
		catch(IllegalArgumentException e)
		{
			throw new ReflectiveException(e);
		}
		catch(NoSuchFieldException e)
		{
			throw new ReflectiveException(e);
		}
	}

	public <T> T call(String methodName, Object... args) {
		return call(methodName, getTypes(args), args);
	}

	public <T> T call(String methodName, Class<?>[] parameterTypes, Object... args) {
		return callInternal(false, methodName, parameterTypes, args);
	}

	public <T> T callStatic(String methodName, Object... args) {
		return callInternal(true, methodName, getTypes(args), args);
	}

	public <T> T callStatic(String methodName, Class<?>[] parameterTypes, Object... args) {
		return callInternal(true, methodName, parameterTypes, args);
	}

	@SuppressWarnings("unchecked")
	private <T> T callInternal(boolean isStatic, String methodName, Class<?>[] parameterTypes, Object... args)
	{
		try
		{
			return (T) mObj.getClass().getMethod(methodName, parameterTypes).invoke(isStatic ? null : mObj, args);
		}
		catch(NoSuchMethodException e)
		{
			throw new ReflectiveException(e);
		}
		catch(IllegalAccessException e)
		{
			throw new ReflectiveException(e);
		}
		catch(IllegalArgumentException e)
		{
			throw new ReflectiveException(e);
		}
		catch(InvocationTargetException e)
		{
			throw new ReflectiveException(e);
		}
	}

	private static Class<?>[] getTypes(Object[] args)
	{
		final Class<?>[] types = new Class<?>[args.length];

		for(int i = 0; i != args.length; ++i)
			types[i] = args[i].getClass();

		return types;
	}
}
