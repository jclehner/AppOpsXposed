/*
 * AppOpsXposed - AppOps for Android 4.3+
 * Copyright (C) 2013-2015 Joseph C. Lehner
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of  MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.jclehner.appopsxposed;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import at.jclehner.appopsxposed.util.Constants;
import at.jclehner.appopsxposed.util.Util;

import com.android.settings.applications.AppOpsCategory;
import com.android.settings.applications.AppOpsDetails;
import com.android.settings.applications.AppOpsSummary;

public class AppOpsActivity extends PreferenceActivity
{
	@Override
	public Intent getIntent()
	{
		final Intent intent = new Intent(super.getIntent());
		intent.putExtra(EXTRA_NO_HEADERS, true);

		final String pkg = intent.getStringExtra("package");
		if("android.settings.APP_OPS_SETTINGS".equals(intent.getAction()) || pkg != null)
		{
			if(pkg != null)
			{
				intent.putExtra(EXTRA_SHOW_FRAGMENT, AppOpsDetails.class.getName());
				intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, intent.getExtras());
			}
			else
				intent.putExtra(EXTRA_SHOW_FRAGMENT, AppOpsSummary.class.getName());
		}
		else if(!intent.hasExtra(EXTRA_SHOW_FRAGMENT))
			intent.putExtra(EXTRA_SHOW_FRAGMENT, AppOpsSummary.class.getName());

		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Util.applyTheme(this);
		super.onCreate(savedInstanceState);

		if(!Util.hasAppOpsPermissions(this))
		{
			final AlertDialog.Builder ab = new AlertDialog.Builder(this);
			ab.setMessage(getString(R.string.permissions_not_granted,
					getString(R.string.app_ops_settings),
					getString(R.string.compatibility_mode_title)));
			ab.setPositiveButton(android.R.string.ok, null);
			ab.show();
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		Util.fixPreferencePermissions();
	}

	@Override
	protected boolean isValidFragment(String fragmentName)
	{
		return AppOpsSummary.class.getName().equals(fragmentName)
				|| AppOpsDetails.class.getName().equals(fragmentName)
				|| AppOpsCategory.class.getName().equals(fragmentName)
				|| AppListFragment.class.getName().equals(fragmentName);
	}
}
