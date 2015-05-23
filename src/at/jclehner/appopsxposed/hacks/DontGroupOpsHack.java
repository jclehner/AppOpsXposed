package at.jclehner.appopsxposed.hacks;

import java.lang.reflect.Method;

import android.app.AppOpsManager;
import android.util.Log;
import at.jclehner.appopsxposed.Hack;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class DontGroupOpsHack extends Hack {

	@Override
	protected void handleLoadAnyPackage(LoadPackageParam lpparam) throws Throwable
	{
		XposedHelpers.findAndHookMethod(AppOpsManager.class,
				"opToSwitch", int.class, new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(param.args[0]);
					}
		});
	}

	@Override
	protected void handleLoadModulePackage(LoadPackageParam lpparam) throws Throwable {
		final Method opToSwitch = AppOpsManager.class.getDeclaredMethod(
				"opToSwitch", int.class);

		XposedHelpers.findAndHookMethod(AppOpsManagerWrapper.class,
				"opToGroup", int.class, new XC_MethodHook() {

					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable
					{
						final int opGroup = (int) XposedBridge.invokeOriginalMethod(
								opToSwitch, null, param.args);

						param.setResult(opGroup);
						Log.i("AOX", "opToGroup called: op=" + param.args[0] + "; opGroup=" + opGroup);

					}

		});
	}

	@Override
	protected String onGetKeySuffix() {
		return "dont_group_ops";
	}

}
