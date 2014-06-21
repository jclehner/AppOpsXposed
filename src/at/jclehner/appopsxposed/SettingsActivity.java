package at.jclehner.appopsxposed;

import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);

		if(savedInstanceState == null)
		{
			getFragmentManager().beginTransaction().replace(android.R.id.content,
					new SettingsFragment()).commit();
		}
	}

	public static class SettingsFragment extends PreferenceFragment implements OnPreferenceChangeListener
	{
		private SharedPreferences mPrefs;

		@Override
		public void onCreate(Bundle savedInstanceState)
		{
			mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.settings);
			setupPreferences();
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
				findPreference("use_layout_fix").setEnabled(!failsafe);
				findPreference("use_boot_completed_hack").setEnabled(!failsafe);
			}
			else if("show_launcher_icon".equals(preference.getKey()))
			{
				final boolean show = (Boolean) newValue;
				final PackageManager pm = getActivity().getPackageManager();
				pm.setComponentEnabledSetting(new ComponentName(getActivity(), "at.jclehner.appopsxposed.LauncherActivity-Icon"),
						show ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
						PackageManager.DONT_KILL_APP);
			}

			return true;
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

			Preference p = findPreference("failsafe_mode");
			callOnChangeListenerWithCurrentValue(p);
			p.setOnPreferenceChangeListener(this);

			findPreference("show_launcher_icon").setOnPreferenceChangeListener(this);
			
			p = findPreference("use_boot_completed_hack");
			p.setSummary(getString(R.string.use_boot_completed_hack_summary, "OP_POST_NOTIFICATION", "OP_VIBRATE"));						
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
	}
}
