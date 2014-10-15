package at.jclehner.appopsxposed.variants;

import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.R;
import at.jclehner.appopsxposed.util.Res;
import at.jclehner.appopsxposed.util.XUtils;
import de.robv.android.xposed.XC_MethodHook;
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
	protected String[] indicatorClasses()
	{
		return new String[] {
				// in com.android.settings
				"com.android.settings.lge.DeviceInfoLge",
				"com.android.settings.lge.Serial",
				// in com.lge.settings.easy
				"com.lge.settings.general.EasyGeneralFragment"
		};
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		debug("handleLoadPackage: " + lpparam.packageName);

		if(!EASY_SETTINGS_PACKAGE.equals(lpparam.packageName))
		{
			super.handleLoadPackage(lpparam);
			return;
		}

		XUtils.findAndHookMethodRecursive("com.lge.settings.general.EasyGeneralFragment", lpparam.classLoader,
				"addPreferencesFromResource", int.class, new XC_MethodHook() {

					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
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
								debug("  APPLICATION_SETTINGS at pos " + i);
								order = i + 1;
								break;
							}
						}

						final Intent intent = new Intent();
						intent.setPackage("com.android.settings");
						intent.setAction("android.settings.SETTINGS");
						intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AppOpsXposed.APP_OPS_FRAGMENT);

						final Preference p = new Preference(pf.getActivity());
						p.setTitle(Res.modRes.getString(R.string.app_ops_settings));
						// TODO change to getAppOpsHeaderIcon()
						p.setIcon(Res.modRes.getDrawable(R.drawable.ic_launcher2));
						p.setIntent(intent);
						p.setOrder(order);

						ps.addPreference(p);
					}
		});
	}

	@Override
	protected int getAppOpsHeaderIcon() {
		return Res.appOpsLauncherIcon;
	}
}
