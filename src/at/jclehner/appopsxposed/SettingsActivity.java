package at.jclehner.appopsxposed;

import android.app.Activity;
import android.content.SharedPreferences;
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

	static class SettingsFragment extends PreferenceFragment implements OnPreferenceChangeListener
	{
		@Override
		public void onCreate(Bundle savedInstanceState)
		{
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

				findPreference("force_variant").setEnabled(!failsafe);
				findPreference("use_layout_fix").setEnabled(!failsafe);
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
		}

		private void callOnChangeListenerWithCurrentValue(Preference p)
		{
			final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
					getActivity());

			final Object value;
			if(p instanceof CheckBoxPreference)
				value = sp.getBoolean(p.getKey(), false);
			else
				value = sp.getString(p.getKey(), null);

			onPreferenceChange(p, value);
		}
	}
}
