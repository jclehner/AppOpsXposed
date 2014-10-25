package at.jclehner.appopsxposed;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.android.settings.applications.AppOpsCategory;
import com.android.settings.applications.AppOpsDetails;
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
		return AppOpsSummary.class.getName().equals(fragmentName)
				|| AppOpsDetails.class.getName().equals(fragmentName)
				|| AppOpsCategory.class.getName().equals(fragmentName)
				|| AppListFragment.class.getName().equals(fragmentName);
	}
}
