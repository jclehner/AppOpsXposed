package at.jclehner.appopsxposed.variants;

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import at.jclehner.appopsxposed.Util;

public class OmniROM extends AOSP
{
	@Override
	protected boolean onMatch(LoadPackageParam lpparam) {
		return Util.getSystemProperty("ro.omni.version", "").length() != 0;
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		addAppOpsToAppInfo(lpparam);
	}
}
