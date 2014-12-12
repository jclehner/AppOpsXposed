package at.jclehner.appopsxposed.variants;

import android.content.Context;
import android.preference.PreferenceActivity.Header;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.util.Util;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Oppo extends AOSP
{
	private boolean mForceCompatibilityMode = true;

	@Override
	protected String manufacturer() {
		return "OPPO";
	}

	@Override
	protected String[] indicatorClasses()
	{
		return new String[] {
				"com.oppo.settings.SettingsActivity"
		};
	}

	@Override
	protected int getDefaultAppOpsHeaderIcon() {
		return ICON_LAUNCHER;
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		try
		{
			lpparam.classLoader.loadClass(AppOpsXposed.APP_OPS_FRAGMENT);
			mForceCompatibilityMode = false;
		}
		catch(ClassNotFoundException e)
		{
			log("No " + AppOpsXposed.APP_OPS_FRAGMENT + " in " + lpparam.packageName);
		}

		super.handleLoadPackage(lpparam);
	}

	@Override
	protected Object onCreateAppOpsHeader(Context context, int addAfterHeaderId)
	{
		final Header header = (Header) super.onCreateAppOpsHeader(context, addAfterHeaderId);
		if(mForceCompatibilityMode)
		{
			header.fragment = null;
			header.intent = Util.getCompatibilityModeIntent(null);
		}
		return header;
	}
}
