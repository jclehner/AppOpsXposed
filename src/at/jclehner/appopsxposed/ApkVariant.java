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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceActivity.Header;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;
import at.jclehner.appopsxposed.Util.XC_MethodHookRecursive;
import at.jclehner.appopsxposed.variants.AOSP;
import at.jclehner.appopsxposed.variants.HTC;
import at.jclehner.appopsxposed.variants.Samsung;
import at.jclehner.appopsxposed.variants.Sony;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
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
	private static final boolean USE_INDICATOR_CLASSES = false;

	protected final String ANY = "";

	private String mLogTag = null;

	private static final ApkVariant[] VARIANTS = {
		new HTC(),
		new Samsung(),
		new Sony(),
		new AOSP()
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

	@Override
	public abstract void handleLoadPackage(LoadPackageParam lpparam) throws Throwable;

	protected Object onCreateAppOpsHeader(Context context)
	{
		final Header appOpsHeader = new Header();
		appOpsHeader.title = getAppOpsTitle();
		appOpsHeader.id = R.id.app_ops_settings;
		appOpsHeader.iconRes = getAppOpsIcon();
		appOpsHeader.fragment = AppOpsXposed.APP_OPS_FRAGMENT;

		return appOpsHeader;
	}

	protected int getAppOpsIcon() {
		return Util.getSettingsIdentifier("drawable/ic_settings_applications");
	}

	protected String getAppOpsTitle()
	{
		final int appOpsTitleId = Util.getSettingsIdentifier("string/app_ops_setting");
		if(appOpsTitleId != 0)
			return Util.getSettingsString(appOpsTitleId);

		return Util.getModString(R.string.app_ops_title);
	}

	protected long getIdFromHeader(Object header) {
		return ((Header) header).id;
	}

	/**
	 * Get the version name of this variant (as specified by <code>android:versionName</code>).
	 * <p>
	 * This function is not used at the moment!
	 */
	protected String versionName() {
		return ANY;
	}

	/**
	 * Get the matching device manufacturer of this variant (as specified in {@link Build.VERSION.MANUFACTURER}).
	 * <p>
	 * The strings are converted to lower case and matched using {@link String#contains(CharSequence)}
	 */
	protected String manufacturer() {
		return ANY;
	}

	/**
	 * Get the matching release of this variant (as specified in {@link Build.VERSION.RELEASE}).
	 */
	protected String release() {
		return ANY;
	}

	/**
	 * Get the matching build id of this variant (as specified in {@link Build.VERSION.ID}).
	 */
	protected String buildId() {
		return ANY;
	}

	/**
	 * Get the matching MD5 hash of this variant's APK file.
	 */
	protected String md5Sum() {
		return ANY;
	}

	/**
	 * Get the matching Android API level of this variant.
	 * <p>
	 * A return value of <code>0</code> means any API level.
	 */
	protected int apiLevel() {
		return 0;
	}

	/**
	 * Get the names of classes of which at least one is required
	 * to be present in the APK for this variant to match.
	 * <p>
	 * Returning <code>null</code> here will skip the check.
	 */
	protected String[] indicatorClasses() {
		return null;
	}


	/**
	 * Check if the variant is complete.
	 * <p>
	 * Return <code>true</code> here if the variant installs all hooks required for "App ops" functionality. That way, no other
	 * variants will be tried, even if they would match.
	 * <p>
	 * Note: The return value need not be constant and is only checked after a call to {@link #handleLoadPackage(LoadPackageParam)}
	 */
	protected abstract boolean isComplete();

	/**
	 * Checks if the {@link ApkVariant} matches the current configuration.
	 * <p>
	 * This method is only called if all other property checks ({@link #manufacturer()},
	 * {@link #apiLevel()}, etc.) succeeded.
	 */
	protected boolean onMatch(LoadPackageParam lpparam) {
		return true;
	}

	protected final void hookLoadHeadersFromResource(LoadPackageParam lpparam, final int[] hookResIds, final int addAfterHeaderId) throws Throwable
	{
		hookLoadHeadersFromResource(lpparam, new XC_MethodHookRecursive() {

				@SuppressWarnings("unchecked")
				@Override
				protected void onAfterHookedMethod(MethodHookParam param) throws Throwable
				{
					final int xmlResId = (Integer) param.args[0];
					debug("loadHeadersFromResource: xmlResId=" + xmlResId);

					for(int hookResId : hookResIds)
					{
						if(xmlResId == hookResId)
						{
							addAppOpsHeader((List<Header>) param.args[1], addAfterHeaderId, (Context) param.thisObject);
							break;
						}
					}
				}
		});
	}

	protected final void hookLoadHeadersFromResource(LoadPackageParam lpparam, final int hookResId, final int addAfterHeaderId) throws Throwable {
		hookLoadHeadersFromResource(lpparam, new int[] { hookResId }, addAfterHeaderId);
	}

	protected final void hookLoadHeadersFromResource(LoadPackageParam lpparam, XC_MethodHookRecursive hook) throws Throwable
	{
		Util.findAndHookMethodRecursive("com.android.settings.Settings", lpparam.classLoader,
				"loadHeadersFromResource", int.class, List.class, hook);
	}

	private static final String[] VALID_FRAGMENTS = {
		AppOpsXposed.APP_OPS_FRAGMENT, AppOpsXposed.APP_OPS_DETAILS_FRAGMENT
	};

	protected final void hookIsValidFragment(Class<?> clazz) throws Throwable
	{
		try
		{
			Util.findAndHookMethodRecursive(clazz, "isValidFragment",
					String.class, new XC_MethodHookRecursive() {

						@Override
						protected void onAfterHookedMethod(MethodHookParam param) throws Throwable
						{
							if((Boolean) param.getResult())
								return;

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
				XposedBridge.log(e);
		}
	}

	protected final void hookIsValidFragment(LoadPackageParam lpparam) throws Throwable {
		hookIsValidFragment(lpparam.classLoader.loadClass("com.android.settings.Settings"));
	}

	protected void addAppOpsToAppInfo(LoadPackageParam lpparam)
	{
		XposedHelpers.findAndHookMethod("com.android.settings.applications.InstalledAppDetails", lpparam.classLoader,
				"onCreateOptionsMenu", Menu.class, MenuInflater.class, new XC_MethodHookRecursive() {

					@Override
					protected void onAfterHookedMethod(final MethodHookParam param) throws Throwable
					{
						final Menu menu = (Menu) param.args[0];
						final MenuItem item = menu.add(getAppOpsTitle());
						item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
						//item.setIcon(Util.modRes.getDrawable(R.mipmap.ic_launcher2));
						item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

							@Override
							public boolean onMenuItemClick(MenuItem item)
							{
								final Fragment f = (Fragment) param.thisObject;
								final PreferenceActivity pa = (PreferenceActivity) f.getActivity();

								debug("onMenuItemClick:" + "\n  intent=" + pa.getIntent() +
										"\n  extras=" + pa.getIntent().getExtras());

								final Bundle args = f.getArguments() == null ? new Bundle() : f.getArguments();
								if(!args.containsKey("package"))
								{
									String pkg;

									try
									{
										pkg = pa.getIntent().getData().getSchemeSpecificPart();
										log("Package obtained from Intent: " + pkg);
									}
									catch(NullPointerException e)
									{
										log(e);
										pkg = null;
									}

									if(pkg == null || pkg.isEmpty())
									{
										Toast.makeText(f.getActivity(), "Error!", Toast.LENGTH_SHORT).show();
										return true;
									}

									args.putString("package", pkg);
								}
								else
									log("Package obtained from Fragment args: " + args.getString("package"));


								pa.startPreferencePanel(AppOpsXposed.APP_OPS_DETAILS_FRAGMENT, args,
										Util.getSettingsIdentifier("string/app_ops_settings"), null, f, 1);
								return true;
							}
						});

						param.setResult(true);
					}
		});
	}

	@SuppressWarnings("rawtypes")
	protected final void addAppOpsHeader(List headers, int addAfterHeaderId) {
		addAppOpsHeader(headers, addAfterHeaderId, null);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected final void addAppOpsHeader(List headers, int addAfterHeaderId, Context context)
	{
		if(headers == null || headers.isEmpty())
		{
			debug("addAppOpsHeader: list is empty or null");
			return;
		}

		int addAfterHeaderIndex = -1;

		for(int i = 0; i != headers.size(); ++i)
		{
			if(getIdFromHeader(headers.get(i)) == addAfterHeaderId)
				addAfterHeaderIndex = i;
			else if(getIdFromHeader(headers.get(i)) == R.id.app_ops_settings)
			{
				debug("addAppOpsHeader: header was already added");
				return;
			}
		}

		final Object appOpsHeader = onCreateAppOpsHeader(context);

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

	protected boolean isMatching(LoadPackageParam lpparam)
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

		if(!onMatch(lpparam))
			return false;

		final String[] classes = indicatorClasses();
		if(classes == null || classes.length == 0)
			return true;

		debug("Checking " + classes.length + " indicator classes");

		for(String className : classes)
		{
			try
			{
				lpparam.classLoader.loadClass(className);
				debug("  OK: " + className);
				return true;
			}
			catch(ClassNotFoundException e)
			{
				debug("  N/A: " + className);
				// continue
			}
		}

		if(!USE_INDICATOR_CLASSES)
		{
			debug("No indicator classes present; continuing anyways");
			return true;
		}

		return false;
	}

	private String getLogPrefix()
	{
		if(mLogTag == null)
			mLogTag = getClass().getSimpleName() + ": ";

		return mLogTag;
	}
}
