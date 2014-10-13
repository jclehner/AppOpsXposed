package at.jclehner.appopsxposed.util;

import de.robv.android.xposed.XSharedPreferences;
import android.content.res.XModuleResources;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.R;

public class Res
{
	public static int appOpsLauncherIcon = 0;
	public static int appOpsPreferenceIcon = 0;
	public static int appListItemLayout = R.layout.app_list_item;

	public static XModuleResources settingsRes;
	public static XModuleResources modRes;
	public static XSharedPreferences modPrefs;

	public static int getSettingsIdentifier(String name) {
		return settingsRes.getIdentifier(name, null, AppOpsXposed.SETTINGS_PACKAGE);
	}

	public static String getSettingsString(int resId) {
		return settingsRes.getString(resId);
	}

	public static String getModString(int resId) {
		return modRes.getString(resId);
	}
}
