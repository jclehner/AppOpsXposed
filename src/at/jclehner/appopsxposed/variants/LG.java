package at.jclehner.appopsxposed.variants;

import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import at.jclehner.appopsxposed.ApkVariant;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.R;
import at.jclehner.appopsxposed.Util;
import at.jclehner.appopsxposed.Util.XC_MethodHookRecursive;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/*
 * LG decided to go overboard by providing another settings app,
 * for some obscure reason called 'easy' settings with a tabbed interface
 * rather than the usual list interface. LGSettings.apk is compatible with
 * the AOSP variant, LGEasySettings.apk however is mostly
 * a new interface, with many preferences actually launching intents to the
 * 'regular' com.android.settings package (LGSettings.apk).
 */
public class LG extends AOSP
{
	private static final String EASY_SETTINGS_PACKAGE =
			"com.lge.settings.easy";

	@Override
	protected String manufacturer() {
		return "LGE";
	}

	@Override
	protected String[] targetPackages()
	{
		return new String[] { AppOpsXposed.SETTINGS_PACKAGE,
				EASY_SETTINGS_PACKAGE
		};
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		if(AppOpsXposed.SETTINGS_PACKAGE.equals(lpparam.packageName))
		{
			super.handleLoadPackage(lpparam);
			return;
		}

		Util.findAndHookMethodRecursive("com.lge.settings.general.EasyGeneralFragment", lpparam.classLoader,
				"addPreferencesFromResource", int.class, new XC_MethodHookRecursive() {

					@Override
					protected void onAfterHookedMethod(MethodHookParam param) throws Throwable
					{
						debug("addPreferencesFromResource: resId=" + param.args[0]);

						final PreferenceFragment pf = ((PreferenceFragment) param.thisObject);
						final PreferenceScreen ps = pf.getPreferenceScreen();
						int order = Preference.DEFAULT_ORDER;

						for(int i = 0; i != ps.getPreferenceCount(); ++i)
						{
							final Intent intent = ps.getPreference(i).getIntent();
							if(intent != null && "android.settings.APPLICATION_SETTINGS"
									.equals(intent.getAction()))
							{
								order = i;
								break;
							}
						}

						final Intent intent = new Intent();
						intent.setPackage("com.android.settings");
						intent.setAction("android.settings.SETTINGS");
						intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AppOpsXposed.APP_OPS_FRAGMENT);

						final Preference p = new Preference(pf.getActivity());
						p.setTitle(Util.modRes.getString(R.string.app_ops_settings));
						p.setIcon(Util.modRes.getDrawable(R.mipmap.ic_launcher2));
						p.setIntent(intent);
						p.setOrder(order);

						ps.addPreference(p);
					}
		});
	}
}
