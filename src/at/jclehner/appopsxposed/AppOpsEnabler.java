package at.jclehner.appopsxposed;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.util.List;

import android.content.res.XModuleResources;
import android.preference.PreferenceActivity.Header;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AppOpsEnabler implements IXposedHookZygoteInit, IXposedHookLoadPackage
{
	private XModuleResources mRes;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable
	{
		mRes = XModuleResources.createInstance(startupParam.modulePath, null);
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		if(!lpparam.packageName.equals("com.android.settings"))
			return;

		final XModuleResources settingsRes = XModuleResources.createInstance(lpparam.appInfo.sourceDir, null);
		final int personalHeaderId = settingsRes.getIdentifier("personal_section", "id", lpparam.appInfo.packageName);
		final int appOpsIcon = settingsRes.getIdentifier("ic_settings_applications", "drawable", lpparam.appInfo.packageName);
		final int appOpsTitleId = settingsRes.getIdentifier("app_ops_setting", "string", lpparam.appInfo.packageName);

		final String appOpsTitle = appOpsTitleId != 0 ? settingsRes.getString(appOpsTitleId) : "App ops";

		findAndHookMethod("com.android.settings.Settings", lpparam.classLoader,
				"updateHeaderList", List.class, new XC_MethodHook() {

					@SuppressWarnings("unchecked")
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable
					{
						final Header appOpsHeader = new Header();
						appOpsHeader.fragment = "com.android.settings.applications.AppOpsSummary";
						appOpsHeader.title = appOpsTitle;
						appOpsHeader.id = R.id.app_ops_settings;
						appOpsHeader.iconRes = appOpsIcon;

						final List<Header> headers = (List<Header>) param.args[0];
						for(int i = 0; i != headers.size(); ++i)
						{
							if(headers.get(i).id == personalHeaderId)
							{
								headers.add(i + 1, appOpsHeader);
								return;
							}
						}

						// If we reach this point, personalHeaderId was not found, so we
						// just put the AppOps header last

						headers.add(appOpsHeader);
					}
		});

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
