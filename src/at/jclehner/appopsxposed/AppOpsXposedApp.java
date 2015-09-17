package at.jclehner.appopsxposed;

import android.app.Application;

import at.jclehner.appopsxposed.util.Util;

public class AppOpsXposedApp extends Application
{
	@Override
	public void onCreate()
	{
		super.onCreate();
		Util.fixPreferencePermissions();
	}
}
