package at.jclehner.appopsxposed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Build;
import android.preference.PreferenceActivity.Header;
import at.jclehner.appopsxposed.Util.XC_MethodHookRecursive;
import at.jclehner.appopsxposed.variants.Samsung;
import at.jclehner.appopsxposed.variants.StockAndroid;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
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
	protected final String ANY = "";

	private String mLogTag = null;

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
	 * The strings are converted to lower case and matched using {@link String#contains(CharSequence)}
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
	public abstract boolean isComplete();

	protected final void hookLoadHeadersFromResource(LoadPackageParam lpparam, final int[] hookResIds, final int addAfterHeaderId)
	{
		hookLoadHeadersFromResource(lpparam, new XC_MethodHookRecursive() {

				@SuppressWarnings("unchecked")
				@Override
				protected void onAfterHookedMethod(MethodHookParam param) throws Throwable
				{
					int xmlResId = (Integer) param.args[0];

					for(int hookResId : hookResIds)
					{
						if(xmlResId == hookResId)
						{
							addAppOpsHeader((List<Header>) param.args[1], addAfterHeaderId);
							break;
						}
					}
				}
		});
	}

	protected final void hookLoadHeadersFromResource(LoadPackageParam lpparam, final int hookResId, final int addAfterHeaderId) {
		hookLoadHeadersFromResource(lpparam, new int[] { hookResId }, addAfterHeaderId);
	}

	protected final void hookLoadHeadersFromResource(LoadPackageParam lpparam, XC_MethodHookRecursive hook)
	{
		Util.findAndHookMethodRecursive("com.android.settings.Settings", lpparam.classLoader,
				"loadHeadersFromResource", int.class, List.class, hook);
	}

	protected final void addAppOpsHeader(List<Header> headers, int addAfterHeaderId)
	{
		if(headers == null || headers.isEmpty())
		{
			debug("addAppOpsHeader: list is empty or null");
			return;
		}

		//final int personalHeaderId = Util.getSettingsIdentifier("id/personal_section");
		final int appOpsIcon = Util.getSettingsIdentifier("drawable/ic_settings_applications");
		final int appOpsTitleId = Util.getSettingsIdentifier("string/app_ops_setting");

		final String appOpsTitle;
		if(appOpsTitleId != 0)
			appOpsTitle = Util.getSettingsString(appOpsTitleId);
		else
			appOpsTitle = Util.getModString(R.string.app_ops_title);

		final Header appOpsHeader = new Header();
		appOpsHeader.title = appOpsTitle;
		appOpsHeader.id = R.id.app_ops_settings;
		appOpsHeader.iconRes = appOpsIcon;

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
		{
			appOpsHeader.intent = new Intent("android.settings.APP_OPS_SETTINGS");
			appOpsHeader.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		}
		else
			appOpsHeader.fragment = AppOpsXposed.APP_OPS_FRAGMENT;

		int addAfterHeaderIndex = -1;

		for(int i = 0; i != headers.size(); ++i)
		{
			if(headers.get(i).id == addAfterHeaderId)
				addAfterHeaderIndex = i;
			else if(headers.get(i).id == R.id.app_ops_settings)
			{
				debug("addAppOpsHeader: header was already added");
				return;
			}
		}

		if(addAfterHeaderIndex != -1)
			headers.add(addAfterHeaderIndex + 1, appOpsHeader);
		else
		{
			debug("Appending appOpsHeader to header list!");
			headers.add(appOpsHeader);
		}
	}

	protected final void log(String message) {
		XposedBridge.log(getLogPrefix() + message);
	}

	protected final void debug(String message) {
		Util.debug(getLogPrefix() + message);
	}

	protected final void log(Throwable t) {
		XposedBridge.log(t);
	}

	protected final void debug(Throwable t) {
		Util.debug(t);
	}

	private boolean isMatching(LoadPackageParam lpparam)
	{
		if(manufacturer() != ANY)
		{
			if(!Build.MANUFACTURER.toLowerCase().contains(manufacturer().toLowerCase()))
				return false;
		}

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
			log("Failed to get MD5 hash of " + lpparam.appInfo.sourceDir);
			debug(e);
			return false;
		}

		if(apiLevel() != 0 && Build.VERSION.SDK_INT != apiLevel())
			return false;

		return true;
	}

	private String getLogPrefix()
	{
		if(mLogTag == null)
			mLogTag = getClass().getSimpleName() + ": ";

		return mLogTag;
	}
}
