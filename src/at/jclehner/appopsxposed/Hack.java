package at.jclehner.appopsxposed;

import at.jclehner.appopsxposed.hacks.BootCompletedHack;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Hack implements IXposedHookLoadPackage, IXposedHookZygoteInit
{
	public static final Hack[] HACKS = {
		new BootCompletedHack()
	};

	private String mLogTag;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {

	}

	@Override
	public final void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		if("android".equals(lpparam.packageName))
			handleLoadFrameworkPackage(lpparam);
		else if(AppOpsXposed.SETTINGS_PACKAGE.equals(lpparam.packageName))
			handleLoadSettingsPackage(lpparam);

		handleLoadAnyPackage(lpparam);
	}

	protected void handleLoadFrameworkPackage(LoadPackageParam lpparam) throws Throwable {

	}

	protected void handleLoadSettingsPackage(LoadPackageParam lpparam) throws Throwable {

	}

	protected void handleLoadAnyPackage(LoadPackageParam lpparam) throws Throwable {

	}

	protected final void log(String message) {
		XposedBridge.log(getLogPrefix() + message);
	}

	protected final void log(Throwable t) {
		XposedBridge.log(getLogPrefix() + t);
	}

	private String getLogPrefix()
	{
		if(mLogTag == null)
		{
			final String name = getClass().getName().replace('$', '.');
			final String pkgName = getClass().getPackage().getName();

			if(name.startsWith(pkgName))
				mLogTag = "AOX:" + name.substring(pkgName.length() + 1) + ": ";
			else
				mLogTag = "AOX:" + name + ": ";
		}

		return mLogTag;
	}
}
