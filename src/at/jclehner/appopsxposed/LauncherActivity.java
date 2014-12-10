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
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import at.jclehner.appopsxposed.util.Util;
import eu.chainfire.libsuperuser.Shell.SU;

public class LauncherActivity extends Activity implements DialogInterface.OnClickListener
{
	private static final String TAG = "AOX";
	private SharedPreferences mPrefs;
	private Handler mHandler;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mHandler = new Handler();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if(!Util.isXposedModuleEnabled())
		{
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT && !isSonyStockRom())
			{
				launchAppOpsSummary();
				return;
			}
			else if(!isSystemApp())
			{
				Toast.makeText(this, R.string.checking_root, Toast.LENGTH_SHORT).show();

				new Thread() {
					public void run()
					{
						final AlertDialog.Builder ab = new AlertDialog.Builder(LauncherActivity.this);

						if(SU.available())
						{
							ab.setMessage(R.string.install_as_system_app);
							ab.setNegativeButton(android.R.string.no, LauncherActivity.this);
							ab.setPositiveButton(android.R.string.yes, LauncherActivity.this);
						}
						else
						{
							ab.setMessage(R.string.no_xposed_no_root);
							ab.setNegativeButton(android.R.string.ok, LauncherActivity.this);
						}

						mHandler.post(new Runnable() {
							@Override
							public void run()
							{
								ab.show();
							}
						});
					}
				}.start();
				return;
			}
			else
			{
				launchAppOpsSummary(true);
				return;
			}
		}

		launchAppOpsSummary();
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if(which == DialogInterface.BUTTON_POSITIVE)
		{
			final String[] commands = {
					"mount -o remount,rw /system",
					"mv '" + getApplicationInfo().sourceDir.replace("'", "\\'") +
							"' /system/priv-app/AppOpsXposed.apk",
					"chmod 777 /system/priv-app/AppOpsXposed.apk",
					"chown root:root /system/priv-app/AppOpsXposed.apk",
					"reboot"
			};

			for(String command : commands)
			{
				final List<String> out = SU.run(command);
				if(!out.isEmpty())
				{
					Log.i(TAG, "cmd: " + command + "\n---> " + out.get(0));
					Toast.makeText(LauncherActivity.this, command + "\n" + out.get(0),
							Toast.LENGTH_LONG).show();
					break;
				}
			}
		}
		else if(which == DialogInterface.BUTTON_NEGATIVE)
			finish();
	}

	private void launchAppOpsSummary() {
		launchAppOpsSummary(mPrefs.getBoolean("compatibility_mode", false));
	}

	private void launchAppOpsSummary(boolean useOwnFragments)
	{
		Log.i(TAG, "Launching AppOps from launcher icon");

		final Intent intent;

		if(!useOwnFragments)
		{
			intent = new Intent();
			intent.setPackage("com.android.settings");
			intent.setAction("android.settings.SETTINGS");
			intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AppOpsXposed.APP_OPS_FRAGMENT);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		}
		else
			intent = Util.getCompatibilityModeIntent(null);

		startActivity(intent);
		finish();
	}

	private boolean isSystemApp()
	{
		try
		{
			final ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), 0);
			return 0 != (appInfo.flags & ApplicationInfo.FLAG_SYSTEM);
		}
		catch(NameNotFoundException e)
		{
			Log.w(TAG, e);
		}

		return false;
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
}
