package at.jclehner.appopsxposed.variants;

import static at.jclehner.appopsxposed.Util.debug;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.os.Build;
import android.preference.PreferenceActivity.Header;
import at.jclehner.appopsxposed.ApkVariant;
import at.jclehner.appopsxposed.Util;
import at.jclehner.appopsxposed.R;

public class StockAndroid extends ApkVariant
{
	public static final String APP_OPS_FRAGMENT = "com.android.settings.applications.AppOpsSummary";
	public static final String APP_OPS_DETAILS_FRAGMENT = "com.android.settings.applications.AppOpsDetails";

	private static final String[] VALID_FRAGMENTS = {
		APP_OPS_FRAGMENT, APP_OPS_DETAILS_FRAGMENT
	};


	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		hookOnBuildHeaders(lpparam);
		hookIsValidFragment(lpparam);
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

	public static void hookLoadHeadersFromResource(LoadPackageParam lpparam, final int hookXmlResId)
	{
		Util.findAndHookMethodRecursive("com.android.settings.Settings", lpparam.classLoader,
				"loadHeadersFromResource", int.class, List.class, new XC_MethodHook() {

				@SuppressWarnings("unchecked")
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable
				{
					int xmlResId = (Integer) param.args[0];
					if(xmlResId == hookXmlResId)
					{
						final List<Header> headers = (List<Header>) param.args[1];
						addAppOpsHeader(headers);
					}
				}
		});
	}

	public static void hookIsValidFragment(LoadPackageParam lpparam)
	{
		try
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
		catch(NoSuchMethodError e)
		{
			// Apps before KitKat didn't need to override PreferenceActivity.isValidFragment,
			// so we ignore a NoSuchMethodError in that case.

			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
				throw e;
		}
	}

	public static void addAppOpsHeader(List<Header> headers)
	{
		if(headers == null || headers.isEmpty())
		{
			debug("addAppOpsHeader: list is empty or null");
			return;
		}

		final int personalHeaderId = Util.getSettingsIdentifier("id/personal_section");
		final int appOpsIcon = Util.getSettingsIdentifier("drawable/ic_settings_applications");
		final int appOpsTitleId = Util.getSettingsIdentifier("string/app_ops_setting");

		final String appOpsTitle;
		if(appOpsTitleId != 0)
			appOpsTitle = Util.getSettingsString(appOpsTitleId);
		else
			appOpsTitle = Util.getModString(R.string.app_ops_title);

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
