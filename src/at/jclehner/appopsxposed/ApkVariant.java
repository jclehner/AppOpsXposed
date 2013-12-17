package at.jclehner.appopsxposed;

import static at.jclehner.appopsxposed.Util.debug;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.os.Build;
import android.preference.PreferenceActivity.Header;
import at.jclehner.appopsxposed.variants.Samsung;
import at.jclehner.appopsxposed.variants.StockAndroid;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * Describes a variant of the Settings.apk file.
 * <p>
 * Extend this class to provide specializations for vendor-specific
 * APK variants. A variant's properties are matched against the
 * current configuration; {@link #handleLoadPackage(LoadPackageParam)}
 * will be called only if there's a match.
 * <p>
 * When extending this class, note that you <em>must</em> return the wildcard value
 * {@link #ANY} for a string property that should be ignored.
 */
public abstract class ApkVariant implements IXposedHookLoadPackage
{
	private static final String[] VALID_FRAGMENTS = {
		AppOpsXposed.APP_OPS_FRAGMENT, AppOpsXposed.APP_OPS_DETAILS_FRAGMENT
	};

	protected final String ANY = "";

	private static final ApkVariant[] VARIANTS = {
		new Samsung(),
		new StockAndroid()
	};

	public static List<ApkVariant> getAllMatching(LoadPackageParam lpparam)
	{
		List<ApkVariant> variants = new ArrayList<ApkVariant>();

		for(ApkVariant variant : VARIANTS)
		{
			if(variant.isMatching(lpparam))
				variants.add(variant);
			else
				Util.debug(variant.getClass().getName() + ": no match!");
		}

		return variants;
	}

	/**
	 * Get the version name of this variant (as specified by <code>android:versionName</code>).
	 * <p>
	 * This function is not used at the moment!
	 */
	public String versionName() {
		return ANY;
	}

	/**
	 * Get the matching device manufacturer of this variant (as specified in {@link Build.VERSION.MANUFACTURER}).
	 * <p>
	 * The strings are compared using {@link String#equalsIgnoreCase(String)}.
	 */
	public String manufacturer() {
		return ANY;
	}

	/**
	 * Get the matching release of this variant (as specified in {@link Build.VERSION.RELEASE}).
	 */
	public String release() {
		return ANY;
	}

	/**
	 * Get the matching build id of this variant (as specified in {@link Build.VERSION.ID}).
	 */
	public String buildId() {
		return ANY;
	}

	/**
	 * Get the matching MD5 hash of this variant's APK file.
	 */
	public String md5Sum() {
		return ANY;
	}

	/**
	 * Get the matching Android API level of this variant.
	 * <p>
	 * A return value of <code>0</code> means any API level.
	 */
	public int apiLevel() {
		return 0;
	}


	/**
	 * Check if the variant is complete.
	 * <p>
	 * Return <code>true</code> here if the variant installs all hooks required for "App ops" functionality. That way, no other
	 * variants will be tried, even if they would match.
	 * <p>
	 * Note: The return value need not be constant and is only checked after a call to {@link #handleLoadPackage(LoadPackageParam)}
	 */
	public boolean isComplete() {
		return false;
	}



	public static void hookOnBuildHeaders(LoadPackageParam lpparam)
	{
		findAndHookMethod("com.android.settings.Settings", lpparam.classLoader,
				"onBuildHeaders", List.class, new XC_MethodHook() {

				@SuppressWarnings("unchecked")
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable
				{
					final List<Header> headers = (List<Header>) param.args[0];
					addAppOpsHeader(headers);
				}
		});
	}

	public static void hookLoadHeadersFromResource(LoadPackageParam lpparam, final int hookXmlResId)
	{
		Util.findAndHookMethodRecursive("com.android.settings.Settings", lpparam.classLoader,
				"loadHeadersFromResource", int.class, List.class, new XC_MethodHook() {

				@SuppressWarnings("unchecked")
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable
				{
					int xmlResId = (Integer) param.args[0];
					if(xmlResId == hookXmlResId)
					{
						final List<Header> headers = (List<Header>) param.args[1];
						addAppOpsHeader(headers);
					}
				}
		});
	}

	public static void hookIsValidFragment(LoadPackageParam lpparam)
	{
		try
		{
			findAndHookMethod("com.android.settings.Settings", lpparam.classLoader,
					"isValidFragment", String.class, new XC_MethodHook() {

					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable
					{
						for(String name : VALID_FRAGMENTS)
						{
							if(name.equals(param.args[0]))
							{
								param.setResult(true);
								return;
							}
						}
					}
			});

		}
		catch(NoSuchMethodError e)
		{
			// Apps before KitKat didn't need to override PreferenceActivity.isValidFragment,
			// so we ignore a NoSuchMethodError in that case.

			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
				throw e;
		}
	}

	public static void addAppOpsHeader(List<Header> headers)
	{
		if(headers == null || headers.isEmpty())
		{
			debug("addAppOpsHeader: list is empty or null");
			return;
		}

		final int personalHeaderId = Util.getSettingsIdentifier("id/personal_section");
		final int appOpsIcon = Util.getSettingsIdentifier("drawable/ic_settings_applications");
		final int appOpsTitleId = Util.getSettingsIdentifier("string/app_ops_setting");

		final String appOpsTitle;
		if(appOpsTitleId != 0)
			appOpsTitle = Util.getSettingsString(appOpsTitleId);
		else
			appOpsTitle = Util.getModString(R.string.app_ops_title);

		final Header appOpsHeader = new Header();
		appOpsHeader.fragment = APP_OPS_FRAGMENT;
		appOpsHeader.title = appOpsTitle;
		appOpsHeader.id = R.id.app_ops_settings;
		appOpsHeader.iconRes = appOpsIcon;

		int personalHeaderIndex = -1;

		for(int i = 0; i != headers.size(); ++i)
		{
			if(headers.get(i).id == personalHeaderId)
				personalHeaderIndex = i;
			else if(headers.get(i).id == R.id.app_ops_settings)
			{
				debug("addAppOpsHeader: header was already added");
				return;
			}
		}

		if(personalHeaderIndex != -1)
			headers.add(personalHeaderIndex + 1, appOpsHeader);
		else
			headers.add(appOpsHeader);
	}

	private boolean isMatching(LoadPackageParam lpparam)
	{
		if(manufacturer() != ANY && !Build.MANUFACTURER.equalsIgnoreCase(manufacturer()))
			return false;

		if(release() != ANY && !Build.VERSION.RELEASE.equals(release()))
			return false;

		if(buildId() != ANY && !Build.ID.equals(buildId()))
			return false;

		try
		{
			if(md5Sum() != ANY && !XposedHelpers.getMD5Sum(lpparam.appInfo.sourceDir).equals(md5Sum()))
				return false;
		}
		catch(IOException e)
		{
			Util.log("Failed to get MD5 hash of " + lpparam.appInfo.sourceDir);
			Util.log(e);
			return false;
		}

		if(Build.VERSION.SDK_INT != apiLevel())
			return false;

		return true;
	}
}
