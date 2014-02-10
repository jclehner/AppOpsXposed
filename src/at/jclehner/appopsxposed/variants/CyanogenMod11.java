package at.jclehner.appopsxposed.variants;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.os.Build;
import at.jclehner.appopsxposed.ApkVariant;

public class CyanogenMod11 extends ApkVariant
{
	@Override
	protected boolean isComplete() {
		return true;
	}

	@Override
	protected boolean onMatch(LoadPackageParam lpparam) {
		return isCm11NightlyAfter20140128();
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		log("Detected cm11 built after 2014-01-28; most features are disabled");
		hookIsValidFragment(lpparam);
	}

	public static boolean isCm11NightlyAfter20140128()
	{
		if(!System.getProperty("os.version").toLowerCase().contains("cyanogenmod"))
			return false;

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
			return false;

		// The module apparently crashes on cm11 nightlies >= 2014-01-28, so we ignore
		// all builds with a date before 2014-01-29.
		return Build.TIME >= 1390953600000L;
	}
}
