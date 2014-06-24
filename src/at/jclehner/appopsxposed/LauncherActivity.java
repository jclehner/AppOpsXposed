/*
 * AppOpsXposed - AppOps for Android 4.3+
 * Copyright (C) 2013 Joseph C. Lehner
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

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class LauncherActivity extends Activity implements OnClickListener
{
	private static final String TAG = "AppOpsXposed";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if(!isXposedInstalled())
		{
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
			{
				Log.i(TAG, "Xposed Framework not installed, but running pre-KitKat Android");

				if(isSonyStockRom())
				{
					Log.i(TAG, "Running Sony stock ROM");
					addAppListFragment();
					return;
				}
			}
			else
			{
				setContentView(R.layout.launcher_info);
				findViewById(R.id.btn_launch).setOnClickListener(this);
				findViewById(R.id.btn_help).setOnClickListener(this);
				((TextView) findViewById(R.id.info_text)).setText(getString(R.string.xposed_not_installed,
						getString(R.string.app_ops_settings), getString(R.string.btn_launch)));

				return;
			}
		}

		launchAppOpsSummary();
	}

	@Override
	public void onClick(View v)
	{
		if(v.getId() == R.id.btn_launch)
			launchAppOpsSummary();
		else if(v.getId() == R.id.btn_help)
		{
			final Uri uri = Uri.parse("http://repo.xposed.info/module/de.robv.android.xposed.installer");
			final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
		}
	}

	private void launchAppOpsSummary()
	{
		final Intent intent = new Intent();
		intent.setAction("android.settings.SETTINGS");
		intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AppOpsXposed.APP_OPS_FRAGMENT);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

		startActivity(intent);
		finish();
	}

	private void addAppListFragment()
	{
		final AppListFragment f = new AppListFragment();
		getFragmentManager().beginTransaction().add(android.R.id.content, f).commit();
	}

	private boolean isSonyStockRom()
	{
		if(!Util.containsManufacturer("Sony"))
			return false;

		final String[] permissions = {
				"com.sonyericsson.r2r.client.permission.START_R2R",
				"com.sonyericsson.permission.IDD"
		};

		final PackageManager pm = getPackageManager();

		for(String permission : permissions)
		{
			final int status = pm.checkPermission(permission, AppOpsXposed.SETTINGS_PACKAGE);
			if(status == PackageManager.PERMISSION_GRANTED)
				return true;
		}

		for(String entry : new File("/system/framework").list())
		{
			// matches both com.sonymobile and com.sonyericsson
			if(entry.startsWith("com.sony"))
				return true;
		}

		return false;
	}

	private boolean isXposedInstalled()
	{
		try
		{
			getPackageManager().getApplicationInfo("de.robv.android.xposed.installer", 0);
			return true;
		}
		catch(NameNotFoundException e)
		{
			return false;
		}
	}
}
