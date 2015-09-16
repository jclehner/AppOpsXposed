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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TaskStackBuilder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import at.jclehner.appopsxposed.util.Util;
import eu.chainfire.libsuperuser.Shell.SU;

public class LauncherActivity extends Activity
{
	public static class HtcActivity2 extends Activity {}
	public static class HtcFragment2 {}

	public static class ThisIsNotXposedFragment extends DialogFragment implements
			OnClickListener, OnCheckedChangeListener
	{
		static ThisIsNotXposedFragment newInstance(boolean hasRoot, boolean isFirstDisplay)
		{
			final ThisIsNotXposedFragment f = new ThisIsNotXposedFragment();
			final Bundle args = new Bundle();
			args.putBoolean("has_root", hasRoot);
			args.putBoolean("is_first_display", isFirstDisplay);
			f.setArguments(args);
			return f;
		}

		private boolean mDontShowAgain = false;

		@SuppressLint("InflateParams")
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState)
		{
			final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
			if(getArguments().getBoolean("has_root"))
			{
				ab.setMessage(R.string.install_as_system_app);
				ab.setNegativeButton(android.R.string.no, this);
				ab.setPositiveButton(android.R.string.yes, this);
			}
			else
			{
				ab.setMessage(R.string.no_xposed_no_root);
				ab.setNegativeButton(android.R.string.ok, this);
			}

			if(!getArguments().getBoolean("is_first_display"))
			{
				final View v = getActivity().getLayoutInflater().inflate(
						R.layout.checkable_text, null);

				((TextView) v.findViewById(android.R.id.text1)).setText(R.string.dont_show_again);
				((CompoundButton) v.findViewById(android.R.id.checkbox)).setOnCheckedChangeListener(this);

				ab.setView(v);
			}

			return ab.create();
		}

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			if(which == DialogInterface.BUTTON_POSITIVE)
			{
				final String apk = getActivity().getApplicationInfo().sourceDir.replace("'", "\\'");

				final String[] commands = {
						"mount -o remount,rw /system",
						"cat '" + apk + "' > " + SYSTEM_APK,
						"rm '" + apk + "'",
						"chmod 644 " + SYSTEM_APK,
						"chown root:root " + SYSTEM_APK,
						"mount -o remount,ro /system",
						"sync",
						"reboot"
				};

				Toast.makeText(getActivity(), R.string.will_reboot, Toast.LENGTH_LONG).show();
				Util.runAsSu(commands);
			}
			else if(which == DialogInterface.BUTTON_NEGATIVE)
				((LauncherActivity) getActivity()).launchAppOpsSummary();

			Util.getSharedPrefs(getActivity()).edit()
					.putBoolean(KEY_DONT_SHOW_DIALOG, mDontShowAgain)
					.putBoolean(KEY_IS_FIRST_DIALOG, false)
					.commit();
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			mDontShowAgain = isChecked;
		}
	}

	public static final String TAG = "AOX";

	public static final String KEY_DONT_SHOW_DIALOG = "launcher_activity_dont_show_dialog_";
	public static final String KEY_IS_FIRST_DIALOG = "launcher_activity_is_first_dialog";
	public static final String SYSTEM_APK = "/system/" +
			(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
			"priv-app" : "app") + "/AppOpsXposed.apk";

	private SharedPreferences mPrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mPrefs = Util.getSharedPrefs(this);

		if(!Util.isXposedModuleEnabled())
		{
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT && !isSonyStockRom())
			{
				launchAppOpsSummary();
				return;
			}
			else if(!Util.isSystemApp(this))
			{
				Toast.makeText(this, R.string.checking_root, Toast.LENGTH_SHORT).show();

				if(!mPrefs.getBoolean(KEY_DONT_SHOW_DIALOG, false))
				{
					new AsyncTask<Void, Void, Boolean>() {
						@Override
						protected Boolean doInBackground(Void... params)
						{
							return SU.available();
						}

						@Override
						protected void onPostExecute(Boolean result)
						{

							ThisIsNotXposedFragment.newInstance(result,
									mPrefs.getBoolean(KEY_IS_FIRST_DIALOG, true)).show(
									getFragmentManager(), "dialog");
						}
					}.execute();
					return;
				}
			}
			else
			{
				launchAppOpsSummary(true);
				return;
			}
		}

		launchAppOpsSummary();
	}

	protected Intent onCreateSettingsIntent()
	{
		final Intent intent = new Intent();
		intent.setPackage("com.android.settings");
		intent.setAction("android.settings.SETTINGS");
		return intent;
	}

	private void launchAppOpsSummary()
	{
		launchAppOpsSummary(true);
		//launchAppOpsSummary(mPrefs.getBoolean("compatibility_mode", false));
	}

	private void launchAppOpsSummary(boolean useOwnFragments)
	{
		Log.i(TAG, "Launching AppOps from launcher icon");

		final TaskStackBuilder tsb = TaskStackBuilder.create(this);

		if(!useOwnFragments)
		{
			final Intent intent = onCreateSettingsIntent();
			intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AppOpsXposed.APP_OPS_FRAGMENT);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

			tsb.addNextIntent(intent);
			//tsb.addNextIntent(onCreateSettingsIntent());
		}
		else
			tsb.addNextIntent(Util.createAppOpsIntent(null));

		tsb.startActivities();
		finish();
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
