package at.jclehner.appopsxposed;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AppOpsEnabler implements IXposedHookLoadPackage
{
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		if(!lpparam.packageName.equals("com.android.settings"))
			return;

		findAndHookMethod("com.android.settings.Settings", lpparam.classLoader,
				"isValidFragment", String.class, new XC_MethodReplacement() {

					@Override
					protected Object replaceHookedMethod(MethodHookParam param) throws Throwable
					{
						return true;
					}
		});
	}
}
