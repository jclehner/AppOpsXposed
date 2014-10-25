package at.jclehner.appopsxposed;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.android.settings.applications.AppOpsSummary;

public class AppOpsActivity extends PreferenceActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//startPreferenceFragment(new AppOpsSummary(), false);
	}

	protected boolean isValidFragment(String fragmentName)
	{
		return AppOpsXposed.APP_OPS_FRAGMENT.equals(fragmentName)
				|| AppOpsXposed.APP_OPS_DETAILS_FRAGMENT.equals(fragmentName);
	}
}
