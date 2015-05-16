package at.jclehner.appopsxposed.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.http.MethodNotSupportedException;

import android.util.Log;


public class ObjectWrapper
{
	public static class ReflectiveException extends RuntimeException
	{
		private static final long serialVersionUID = 4913462741847291648L;

		public ReflectiveException(Throwable cause) {
			super(cause);
		}
	}

	protected final Object mObj;

	public ObjectWrapper(Object obj) {
		mObj = obj;
	}

	public Class<?> getWrappedClass() {
		return mObj.getClass();
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String fieldName)
	{
		try
		{
			return (T) getField(mObj.getClass(), fieldName).get(mObj);
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
			Log.w("ObjectWrapper", e);
			return defValue;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T getStatic(Class<?> clazz, String fieldName)
	{
		try
		{
			return (T) getField(clazz, fieldName).get(null);
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

	public void set(String fieldName, Object value)
	{
		try
		{
			getField(mObj.getClass(), fieldName).set(mObj, value);
		}
		catch(NoSuchFieldException e)
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
			final Throwable targetException = e.getTargetException();
			if(targetException instanceof RuntimeException)
				throw (RuntimeException) targetException;

			throw new ReflectiveException(e);
		}
	}

	private static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException
	{
		Field f;

		try
		{
			f = clazz.getDeclaredField(fieldName);
		}
		catch(NoSuchFieldException e)
		{
			f = clazz.getField(fieldName);
		}

		f.setAccessible(true);
		return f;
	}

	private static Class<?>[] getTypes(Object[] args)
	{
		final Class<?>[] types = new Class<?>[args.length];

		for(int i = 0; i != args.length; ++i)
			types[i] = args[i].getClass();

		return types;
	}
}
