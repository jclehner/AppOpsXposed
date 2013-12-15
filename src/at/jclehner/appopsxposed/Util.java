package at.jclehner.appopsxposed;

import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XposedBridge;

public final class Util
{
	public static void log(String message) {
		XposedBridge.log("AppOpsXposed: " + message);
	}

	public static void log(Throwable t) {
		XposedBridge.log(t);
	}

	public static void debug(String message)
	{
		if(!BuildConfig.DEBUG)
			return;

		log(message);
	}

	public static void debug(Throwable t)
	{
		if(!BuildConfig.DEBUG)
			return;

		log(t);
	}

	public static String[] appendToStringArray(String[] array, String str)
	{
		final ArrayList<String> list = new ArrayList<String>(Arrays.asList(array));
		list.add(str);

		final String[] newArray = new String[list.size()];
		return list.toArray(newArray);
	}

	private Util() {}
}
