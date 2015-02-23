package at.jclehner.appopsxposed.hacks;

import at.jclehner.appopsxposed.Hack;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;

public class FixOpsPruneHack extends Hack {

	@Override
	protected String onGetKeySuffix() {
		return "fix_prune";
	}

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable
	{
		final Class<?> appOpsSvcClazz = Class.forName("com.android.server.AppOpsService");
		// This is extremely crude, but should suffice for a temporary fix. See
		// 9bb3f6785d6 in CyanogenMod/android_frameworks_base for an infinitely more
		// elegant solution.
		XposedBridge.hookAllMethods(appOpsSvcClazz, "systemReady", XC_MethodReplacement.DO_NOTHING);
	}
}
