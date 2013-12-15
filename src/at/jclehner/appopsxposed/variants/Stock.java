package at.jclehner.appopsxposed.variants;

import static at.jclehner.appopsxposed.AppOpsEnabler.modResources;
import static at.jclehner.appopsxposed.AppOpsEnabler.modPreferences;
import static at.jclehner.appopsxposed.AppOpsEnabler.settingsResources;
import static at.jclehner.appopsxposed.Util.debug;
import static at.jclehner.appopsxposed.Util.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.preference.PreferenceActivity.Header;
import at.jclehner.appopsxposed.ApkVariant;
import at.jclehner.appopsxposed.R;

public class Stock extends ApkVariant
{
	static final String SETTINGS_PACKAGE = "com.android.settings";

	static final String APP_OPS_FRAGMENT = "com.android.settings.applications.AppOpsSummary";
	static final String APP_OPS_DETAILS_FRAGMENT = "com.android.settings.applications.AppOpsDetails";



	private static final String[] VALID_FRAGMENTS = {
		APP_OPS_FRAGMENT, APP_OPS_DETAILS_FRAGMENT
	};


	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		// TODO Auto-generated method stub

	}

	public static void hookOnBuildHeaders(LoadPackageParam lpparam)
	{
		findAndHookMethod("com.android.settings.Settings", lpparam.classLoader,
				"onBuildHeaders", List.class, new XC_MethodHook() {

				@SuppressWarnings("unchecked")
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable
				{
					final List<Header> headers = (List<Header>) param.args[0];
					addAppOpsHeader(headers);
				}
		});
	}

	public static void hookIsValidFragment(LoadPackageParam lpparam)
	{
		findAndHookMethod("com.android.settings.Settings", lpparam.classLoader,
				"isValidFragment", String.class, new XC_MethodHook() {

				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable
				{
					for(String name : VALID_FRAGMENTS)
					{
						if(name.equals(param.args[0]))
						{
							param.setResult(true);
							return;
						}
					}
				}
		});
	}

	public static void addAppOpsHeader(List<Header> headers)
	{
		if(headers == null || headers.isEmpty())
		{
			debug("addAppOpsHeader: list is empty or null");
			return;
		}

		final int personalHeaderId = settingsResources().getIdentifier("id/personal_section", null, SETTINGS_PACKAGE);
		final int appOpsIcon = settingsResources().getIdentifier("drawable/ic_settings_applications", null, SETTINGS_PACKAGE);
		final int appOpsTitleId = settingsResources().getIdentifier("string/app_ops_setting", null, SETTINGS_PACKAGE);
		final String appOpsTitle = appOpsTitleId != 0 ? settingsResources().getString(appOpsTitleId) : modResources().getString(R.string.app_ops_setting);

		final Header appOpsHeader = new Header();
		appOpsHeader.fragment = APP_OPS_FRAGMENT;
		appOpsHeader.title = appOpsTitle;
		appOpsHeader.id = R.id.app_ops_settings;
		appOpsHeader.iconRes = appOpsIcon;

		int personalHeaderIndex = -1;

		for(int i = 0; i != headers.size(); ++i)
		{
			if(headers.get(i).id == personalHeaderId)
				personalHeaderIndex = i;
			else if(headers.get(i).id == R.id.app_ops_settings)
			{
				debug("addAppOpsHeader: header was already added");
				return;
			}
		}

		if(personalHeaderIndex != -1)
			headers.add(personalHeaderIndex + 1, appOpsHeader);
		else
			headers.add(appOpsHeader);
	}
}
