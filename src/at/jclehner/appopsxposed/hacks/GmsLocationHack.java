package at.jclehner.appopsxposed.hacks;

import android.app.AppOpsManager;
import android.content.Context;
import at.jclehner.appopsxposed.Hack;
import at.jclehner.appopsxposed.Util;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class GmsLocationHack extends Hack
{
	final int[] LOCATION_OPS = {
			Util.getOpValue("OP_COARSE_LOCATION"),
			Util.getOpValue("OP_FINE_LOCATION"),
			Util.getOpValue("OP_MONITOR_LOCATION"),
			Util.getOpValue("OP_MONITOR_HIGH_POWER_LOCATION")
	};

	@Override
	protected String onGetKeySuffix() {
		return "gms_location_hack";
	}

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable
	{
		try
		{
			hackLocationApi(ClassLoader.getSystemClassLoader());
		}
		catch(Throwable t)
		{
			log(t);
		}
	}

	private void hackLocationApi(ClassLoader classLoader) throws Throwable
	{
		if(!hasCheckOpNoThrow())
			return;

		final Class<?> apiBuilderClazz = classLoader.loadClass(
				"com.google.android.gms.common.api.GoogleApiClient$Builder");

		final Class<?> locationSvcClazz = classLoader.loadClass(
				"com.google.android.gms.location.LocationServices");

		final Object locationApi = XposedHelpers.getStaticObjectField(locationSvcClazz,
				"API");

		XposedBridge.hookAllConstructors(apiBuilderClazz, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				for(Object arg : param.args)
				{
					if(arg instanceof Context)
						XposedHelpers.setAdditionalInstanceField(param.thisObject, "context", arg);
				}
			}

		});

		XposedBridge.hookAllMethods(apiBuilderClazz, "addApi",
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable
					{
						if(param.args[0] == locationApi)
						{
							final Context ctx = (Context)
									XposedHelpers.getAdditionalInstanceField(param.thisObject, "context");
							if(ctx == null)
							{
								log("Context not available; cannot check ops");
								return;
							}

							boolean isLocationDisabled = false;

							for(int op : LOCATION_OPS)
							{
								if(checkOpNoThrow(op, ctx) == AppOpsManager.MODE_IGNORED)
								{
									isLocationDisabled = true;
									break;
								}
							}

							if(isLocationDisabled)
							{
								log("Denying GMS location API to app " + ctx.getPackageName());
								param.setResult(null);
							}
						}
					}
		});
	}
}
