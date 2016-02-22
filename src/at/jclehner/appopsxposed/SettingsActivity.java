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


import at.jclehner.appopsxposed.util.Constants;
import eu.chainfire.libsuperuser.Shell.SU;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.Arrays;

import at.jclehner.appopsxposed.util.AppOpsManagerWrapper;
import at.jclehner.appopsxposed.util.OpsLabelHelper;
import at.jclehner.appopsxposed.util.Util;

public class SettingsActivity extends Activity
{
	private static final int REQUEST_CREATE_BACKUP = 0;
	private static final int REQUEST_RESTORE_BACKUP = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Util.applyTheme(this);
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);

		if(savedInstanceState == null)
		{
			getFragmentManager().beginTransaction().replace(android.R.id.content,
					new SettingsFragment()).commit();
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		Util.fixPreferencePermissions();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if(Util.isSystemApp(this))
		{
			menu.add(R.string.uninstall)
					.setIcon(android.R.drawable.ic_menu_delete)
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item)
						{
							final AlertDialog.Builder ab = new AlertDialog.Builder(SettingsActivity.this);
							ab.setMessage(getString(R.string.uninstall) + "? " + getString(R.string.will_reboot));
							ab.setNegativeButton(android.R.string.cancel, null);
							ab.setPositiveButton(android.R.string.ok, new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									if(!SU.available())
									{
										Toast.makeText(SettingsActivity.this,
												R.string.toast_needs_root, Toast.LENGTH_SHORT).show();
									}
									else
									{
										final String[] commands = {
												"mount -o remount,rw /system",
												"rm " + LauncherActivity.SYSTEM_APK,
												"mount -o remount,ro /system",
												"sync",
												"reboot",
										};

										Toast.makeText(SettingsActivity.this, R.string.will_reboot,
												Toast.LENGTH_LONG).show();
										Util.runAsSu(commands);
									}
								}
							});

							ab.show();

							return true;
						}
					})
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}

		return true;
	}

	public static class SettingsFragment extends PreferenceFragment implements
			OnPreferenceChangeListener, OnSharedPreferenceChangeListener,
			FragmentCompat.OnRequestPermissionsResultCallback
	{
		private SharedPreferences mPrefs;

		@Override
		public void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);

			getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
			mPrefs = Util.getSharedPrefs(getActivity());

			addPreferencesFromResource(R.xml.settings);
			setupPreferences();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			final View v = inflater.inflate(R.layout.settings, container, false);
			v.findViewById(R.id.xposed_settings_warning).setVisibility(
					Util.isXposedModuleEnabled() ? View.GONE : View.VISIBLE);
			return v;
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
		{
			Util.fixPreferencePermissions();
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			if("force_variant".equals(preference.getKey()))
			{
				final String variant = (String) newValue;
				if(variant.length() == 0)
					preference.setSummary(R.string.automatic);
				else
					preference.setSummary(variant);
			}
			else if("failsafe_mode".equals(preference.getKey()))
			{
				final boolean failsafe = (Boolean) newValue;

				if(failsafe && !mPrefs.getBoolean("show_launcher_icon", true))
				{
					final CheckBoxPreference p = (CheckBoxPreference) findPreference("show_launcher_icon");
					p.setChecked(true);
				}

				findPreference("show_launcher_icon").setEnabled(!failsafe);
				findPreference("force_variant").setEnabled(!failsafe);
				//findPreference("use_layout_fix").setEnabled(!failsafe);
				findPreference("hacks").setEnabled(!failsafe);
			}
			else if("compatibility_mode".equals(preference.getKey()))
			{
				final boolean useCompatibilityMode = (Boolean) newValue;

				if(useCompatibilityMode && !mPrefs.getBoolean("show_launcher_icon", true))
				{
					final CheckBoxPreference p = (CheckBoxPreference) findPreference("show_launcher_icon");
					p.setChecked(true);

					Toast.makeText(getActivity(), R.string.must_reboot_device, Toast.LENGTH_LONG).show();
				}

				final PackageManager pm = getActivity().getPackageManager();
				pm.setComponentEnabledSetting(new ComponentName(getActivity(), "at.jclehner.appopsxposed.LauncherActivity$HtcActivity"),
						useCompatibilityMode ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
						PackageManager.DONT_KILL_APP);

				pm.setComponentEnabledSetting(new ComponentName(getActivity(), "at.jclehner.appopsxposed.LauncherActivity$HtcFragment"),
						useCompatibilityMode ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
						PackageManager.DONT_KILL_APP);
			}
			else if("show_launcher_icon".equals(preference.getKey()))
			{
				if(!Util.isXposedModuleEnabled() && !(Boolean) newValue)
					return false;

				final boolean show = (Boolean) newValue;
				final PackageManager pm = getActivity().getPackageManager();
				pm.setComponentEnabledSetting(new ComponentName(getActivity(), "at.jclehner.appopsxposed.LauncherActivity-Icon"),
						show ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
						PackageManager.DONT_KILL_APP);
			}

			return true;
		}

		@Override
		public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
		{
			if(requestCode == REQUEST_CREATE_BACKUP || requestCode == REQUEST_RESTORE_BACKUP)
			{
				if(grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
					createOrRestoreBackup(requestCode == REQUEST_CREATE_BACKUP);
				else
					Toast.makeText(getActivity(), R.string.failed, Toast.LENGTH_SHORT).show();
			}
		}

		private void setupPreferences()
		{
			final ListPreference lp = (ListPreference) findPreference("force_variant");
			callOnChangeListenerWithCurrentValue(lp);

			final CharSequence[] entries = lp.getEntries();
			final CharSequence[] values = new CharSequence[entries.length];
			System.arraycopy(entries, 0, values, 0, entries.length);
			values[0] = "";
			lp.setEntryValues(values);
			lp.setOnPreferenceChangeListener(this);

			Preference p /*= findPreference("failsafe_mode");
			callOnChangeListenerWithCurrentValue(p);
			p.setOnPreferenceChangeListener(this)*/;

			findPreference("show_launcher_icon").setOnPreferenceChangeListener(this);

			p = findPreference("use_hack_boot_completed");
			p.setEnabled(!AppOpsManagerWrapper.hasTrueBootCompletedOp());
			p.setSummary(getString(R.string.use_hack_boot_completed_summary,
					getString(R.string.app_ops_labels_post_notification),
					getString(R.string.app_ops_labels_vibrate)));

			p = findPreference("use_hack_wake_lock");
			p.setSummary(getString(R.string.use_hack_wake_lock_summary,
					OpsLabelHelper.getOpLabel(getActivity(), "OP_WAKE_LOCK")));

			p = findPreference("use_hack_pm_crash");
			if(p != null)
			{
				p.setSummary(getString(R.string.use_hack_pm_crash_summary,
						getString(R.string.app_ops_settings)));
			}

			p = findPreference("hacks");
			p.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference)
				{
					if(mPrefs.getBoolean("show_hacks_warning_dialog", true))
						showHacksWarningDialog(preference);

					return true;
				}
			});

			p = findPreference("version");
			p.setTitle("AppOpsXposed " + Util.getAoxVersion(getActivity()));
			// \u2013 is an en dash!
			p.setSummary("Copyright (C) Joseph C. Lehner 2013\u20132015\n"
					+ "<joseph.c.lehner@gmail.com> / caspase @XDA");

			if(BuildConfig.DEBUG)
			{
				p.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						final Intent intent = new Intent(getActivity(), AppOpsActivity.class);
						startActivity(intent);

						return true;
					}
				});

				p.setEnabled(true);
			}

			p = findPreference("build_bugreport");
			p.setOnPreferenceClickListener(new OnPreferenceClickListener()
			{

				@Override
				public boolean onPreferenceClick(Preference preference)
				{
					BugReportBuilder.buildAndSend(getActivity());
					return true;
				}
			});

			String[] keys = { "icon_appinfo", "icon_settings" };
			for(String key : keys)
			{
				p = findPreference(key);
				((IconPreference) p).setIcons(Constants.ICONS);
			}

			keys = new String[] { "backup_create", "backup_restore" };
			for(String key: keys)
			{
				p = findPreference(key);
				p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						final int status = ContextCompat.checkSelfPermission(getActivity(),
								Manifest.permission.WRITE_EXTERNAL_STORAGE);

						final boolean create = "backup_create".equals(preference.getKey());

						if(status == PackageManager.PERMISSION_GRANTED)
							createOrRestoreBackup(create);
						else
						{
							FragmentCompat.requestPermissions(SettingsFragment.this,
									new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
									create ? REQUEST_CREATE_BACKUP : REQUEST_RESTORE_BACKUP);
						}

						return true;
					}
				});
			}

			p = findPreference("backup_file");
			p.setSummary(Html.fromHtml("<tt><small>" + Backup.getFile(getActivity()) + "</small></tt>"));

			p = findPreference("light_theme");
			p.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
					getActivity().recreate();
					return true;
				}
			});

			if(!BuildConfig.DEBUG)
			{
				p = findPreference("use_hack_dont_group_ops");
				if(p != null)
				{
					final Preference ps = findPreference("hacks");
					if(ps instanceof PreferenceScreen)
						((PreferenceScreen) ps).removePreference(p);
				}
			}
		}

		private void createOrRestoreBackup(boolean create)
		{
			final boolean status = create ? Backup.create(getActivity())
					: Backup.restore(getActivity());

			Toast.makeText(getActivity(), status ? R.string.success
					: R.string.failed, Toast.LENGTH_SHORT).show();
		}

		private void callOnChangeListenerWithCurrentValue(Preference p)
		{
			final Object value;
			if(p instanceof CheckBoxPreference)
				value = mPrefs.getBoolean(p.getKey(), false);
			else
				value = mPrefs.getString(p.getKey(), "");

			onPreferenceChange(p, value);
		}

		private void showHacksWarningDialog(final Preference pref)
		{
			final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
			ab.setCancelable(false);
			ab.setIcon(android.R.drawable.ic_dialog_alert);
			ab.setTitle(R.string.hacks_dialog_title);
			ab.setPositiveButton(android.R.string.ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					mPrefs.edit().putBoolean("show_hacks_warning_dialog", false).apply();
				}
			});
			ab.setNegativeButton(android.R.string.cancel, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					((PreferenceScreen) pref).getDialog().dismiss();
				}
			});

			final AlertDialog dialog = ab.create();
			dialog.setMessage(Html.fromHtml(getString(R.string.hacks_dialog_message)));
			dialog.setOnShowListener(new OnShowListener() {

				@Override
				public void onShow(DialogInterface dialogInterface)
				{
					final Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
					b.setEnabled(false);
					final CharSequence origText = b.getText();

					new Thread() {
						public void run()
						{
							for(int i = 9; i > 0; --i)
							{
								final String tempText =
										origText + " (" + i + ")";

								b.post(new Runnable() {

									@Override
									public void run() {
										b.setText(tempText);
									}
								});

								try
								{
									Thread.sleep(1000);
								}
								catch (InterruptedException e)
								{
									// ignore
								}
							}

							b.post(new Runnable() {

								@Override
								public void run() {
									b.setText(origText);
									b.setEnabled(true);
								}
							});
						}
					}.start();
				}
			});

			dialog.show();
		}
	}
}





