package at.jclehner.appopsxposed.variants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.os.Build;
import at.jclehner.appopsxposed.Util;

public class CyanogenMod extends AOSP
{
	private static final String CM_VERSION = getCmVersion();

	@Override
	protected boolean onMatch(LoadPackageParam lpparam) {
		return CM_VERSION != null;
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		// for now...
		super.handleLoadPackage(lpparam);
		log("ro.cm.version=" + CM_VERSION);
	}

	public static boolean isCm11NightlyAfter20140128()
	{
		if(CM_VERSION == null)
			return false;

		final Matcher m = Pattern.compile("([0-9]+)-([0-9]{8})-NIGHTLY-.*").matcher(CM_VERSION);
		if(!m.matches())
		{
			if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT)
			{
				XposedBridge.log("Detected CyanogenMod running post-KitKat; assuming it's affected");
				return true;
			}

			return false;
		}

		final int cmMajor = Integer.parseInt(m.group(1));
		final int cmDate = Integer.parseInt(m.group(2));

		return cmMajor >= 11 && cmDate >= 20140128;
	}

	private static String getCmVersion() {
		return Util.getSystemProperty("ro.cm.version", null);
	}
}
