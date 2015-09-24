package at.jclehner.appopsxposed.hacks;

import at.jclehner.appopsxposed.Hack;
import at.jclehner.appopsxposed.util.Constants;
import at.jclehner.appopsxposed.util.Util;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


public class MiuiHacks extends Hack
{
	@Override
	protected String onGetKeySuffix()
	{
		return "miui";
	}

	@Override
	protected boolean isEnabledByDefault()
	{
		try
		{
			Class.forName("miui.os.Build");
			log("Hack enabled");
			return true;
		}
		catch(ClassNotFoundException e)
		{
			return false;
		}
	}

	@Override
	protected void handleLoadFrameworkPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable
	{
		try
		{
			final Class<?> appOpsSvcClazz = loadClass("com.android.server.AppOpsService");
			XposedBridge.hookAllMethods(appOpsSvcClazz, "isMiuiAllowed",
					XC_MethodReplacement.returnConstant(true));
			XposedBridge.hookAllMethods(appOpsSvcClazz, "inMiuiAllowedBlackList",
					XC_MethodReplacement.returnConstant(false));

			XposedHelpers.findAndHookMethod(appOpsSvcClazz, "checkSystemApp",
					new Class<?>[] { int.class, int.class, String.class }, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable
						{
							Boolean result = (Boolean) param.getResult();
							if(result == null || result)
								return;

							if(Constants.MODULE_PACKAGE.equals(param.args[2]))
								param.setResult(true);
						}
					}
			);

			log("Hooked: isMiuiAllowed, inMiuiAllowedBlackList, checkSystemApp");
		}
		catch(Throwable t)
		{
			Util.log(t);
		}
	}
}
