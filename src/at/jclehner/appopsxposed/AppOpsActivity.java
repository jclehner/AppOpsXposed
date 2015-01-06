package at.jclehner.appopsxposed;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import at.jclehner.appopsxposed.util.Constants;

import com.android.settings.applications.AppOpsCategory;
import com.android.settings.applications.AppOpsDetails;
import com.android.settings.applications.AppOpsSummary;

public class AppOpsActivity extends PreferenceActivity
{
	@Override
	public Intent getIntent()
	{
		final Intent intent = new Intent(super.getIntent());
		if(!intent.hasExtra(EXTRA_SHOW_FRAGMENT))
			intent.putExtra(EXTRA_SHOW_FRAGMENT, AppOpsSummary.class.getName());
		intent.putExtra(EXTRA_NO_HEADERS, true);
		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		checkForAppOpsPermissions();
	}

	@Override
	protected boolean isValidFragment(String fragmentName)
	{
		return AppOpsSummary.class.getName().equals(fragmentName)
				|| AppOpsDetails.class.getName().equals(fragmentName)
				|| AppOpsCategory.class.getName().equals(fragmentName)
				|| AppListFragment.class.getName().equals(fragmentName);
	}

	private void checkForAppOpsPermissions()
	{
		for(String perm : Constants.APP_OPS_PERMISSIONS)
		{
			if(checkCallingOrSelfPermission(perm) != PackageManager.PERMISSION_GRANTED)
			{
				final AlertDialog.Builder ab = new AlertDialog.Builder(this);
				ab.setMessage(getString(R.string.permissions_not_granted,
						getString(R.string.app_ops_settings),
						getString(R.string.compatibility_mode_title)));
				ab.setPositiveButton(android.R.string.ok, null);
				ab.show();

				break;
			}
		}
	}
}
