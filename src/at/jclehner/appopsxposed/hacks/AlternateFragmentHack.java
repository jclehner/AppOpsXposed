package at.jclehner.appopsxposed.hacks;

import android.support.v4.view.ViewPager;
import at.jclehner.appopsxposed.BuildConfig;
import at.jclehner.appopsxposed.Hack;
import at.jclehner.appopsxposed.R;
import at.jclehner.appopsxposed.util.Res;

import com.android.settings.applications.AppOpsCategory;
import com.android.settings.applications.AppOpsDetails;
import com.android.settings.applications.AppOpsState;
import com.android.settings.applications.AppOpsSummary;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AlternateFragmentHack extends Hack
{
	@Override
	protected String onGetKeySuffix() {
		return "alternate_fragment";
	}

	@Override
	protected boolean isEnabledByDefault() {
		return BuildConfig.DEBUG;
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable
	{
		Res.layout_app_ops_summary = resparam.res.addResource(Res.modRes, Res.layout_app_ops_summary);
	}

	@Override
	protected void handleLoadSettingsPackage(LoadPackageParam lpparam) throws Throwable
	{
		super.handleLoadSettingsPackage(lpparam);

		XposedBridge.hookAllMethods(ClassLoader.class, "loadClass", new XC_MethodHook() {

			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable
			{
				final String name = (String) param.args[0];

				if(name.startsWith("com.android.settings.applications.AppOps") && !name.contains("$"))
				{
					log("Intercepting ClassLoader.loadClass with name=" + name);

					if("com.android.settings.applications.AppOpsSummary".equals(name))
						param.setResult(AppOpsSummary.class);
					else if("com.android.settings.applications.AppOpsDetails".equals(name))
						param.setResult(AppOpsDetails.class);
					else if("com.android.settings.applications.AppOpsState".equals(name))
						param.setResult(AppOpsState.class);
					else if("com.android.settings.applications.AppOpsCategory".equals(name))
						param.setResult(AppOpsCategory.class);
				}
				else if(name.startsWith("android.support.v4"))
				{
					if("android.support.v4.view.ViewPager".equals(name))
						param.setResult(ViewPager.class);
				}
			}
		});
	}
}
