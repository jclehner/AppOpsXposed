package at.jclehner.appopsxposed;

import at.jclehner.appopsxposed.hacks.BootCompletedHack;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Hack implements IXposedHookLoadPackage, IXposedHookZygoteInit
{
	public static final Hack[] HACKS = {
		new BootCompletedHack()
	};
	
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
	
	public void handleLoadFrameworkPackage(LoadPackageParam lpparam) throws Throwable {
		
	}
	
	public void handleLoadSettingsPackage(LoadPackageParam lpparam) throws Throwable {
		
	}
	
	public void handleLoadAnyPackage(LoadPackageParam lpparam) throws Throwable {
		
	}
}
