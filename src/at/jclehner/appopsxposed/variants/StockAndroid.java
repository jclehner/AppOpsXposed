package at.jclehner.appopsxposed.variants;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import at.jclehner.appopsxposed.ApkVariant;

public class StockAndroid extends ApkVariant
{
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		hookOnBuildHeaders(lpparam);
		hookIsValidFragment(lpparam);
	}
}
