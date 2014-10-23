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

	public static <T> T getStatic(Class<?> clazz, String fieldName, T defValue)
	{
		try
		{
			return getStatic(clazz, fieldName);
		}
		catch(ReflectiveException e)
		{
			e.printStackTrace();
			return defValue;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T getStatic(Class<?> clazz, String fieldName)
	{
		try
		{
			return (T) clazz.getField(fieldName).get(null);
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
		return callInternal(mObj.getClass(), mObj, methodName, parameterTypes, args);
	}

	public <T> T callStatic(String methodName, Object... args) {
		return callStatic(methodName, getTypes(args), args);
	}

	public <T> T callStatic(String methodName, Class<?>[] parameterTypes, Object... args) {
		return callInternal(mObj.getClass(), null, methodName, parameterTypes, args);
	}

	public static <T> T callStatic(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object... args) {
		return callInternal(clazz, null, methodName, parameterTypes, args);
	}

	public static <T> T callStatic(Class<?> clazz, String methodName, Object... args) {
		return callStatic(clazz, methodName, getTypes(args), args);
	}

	@SuppressWarnings("unchecked")
	private static <T> T callInternal(Class<?> clazz, Object receiver, String methodName, Class<?>[] parameterTypes, Object... args)
	{
		try
		{
			return (T) clazz.getMethod(methodName, parameterTypes).invoke(receiver, args);
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
