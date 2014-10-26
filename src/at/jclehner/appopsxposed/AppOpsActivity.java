package at.jclehner.appopsxposed;

import android.content.Intent;
import android.preference.PreferenceActivity;

import com.android.settings.applications.AppOpsCategory;
import com.android.settings.applications.AppOpsDetails;
import com.android.settings.applications.AppOpsSummary;

public class AppOpsActivity extends PreferenceActivity
{
	@Override
	public Intent getIntent()
	{
		final Intent intent = new Intent(super.getIntent());
		intent.putExtra(EXTRA_SHOW_FRAGMENT, AppOpsSummary.class.getName());
		intent.putExtra(EXTRA_NO_HEADERS, true);
		return intent;
	}

	protected boolean isValidFragment(String fragmentName)
	{
		return AppOpsSummary.class.getName().equals(fragmentName)
				|| AppOpsDetails.class.getName().equals(fragmentName)
				|| AppOpsCategory.class.getName().equals(fragmentName)
				|| AppListFragment.class.getName().equals(fragmentName);
	}
}
