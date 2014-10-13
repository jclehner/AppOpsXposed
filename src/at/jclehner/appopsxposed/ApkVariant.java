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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Fragment;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceActivity.Header;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;
import at.jclehner.appopsxposed.util.Res;
import at.jclehner.appopsxposed.util.Util;
import at.jclehner.appopsxposed.util.XUtils;
import at.jclehner.appopsxposed.variants.AOSP;
import at.jclehner.appopsxposed.variants.CyanogenMod;
import at.jclehner.appopsxposed.variants.HTC;
import at.jclehner.appopsxposed.variants.LG;
import at.jclehner.appopsxposed.variants.OmniROM;
import at.jclehner.appopsxposed.variants.Oppo;
import at.jclehner.appopsxposed.variants.Samsung;
import at.jclehner.appopsxposed.variants.Sony;
import dalvik.system.DexFile;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
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
public abstract class ApkVariant implements IXposedHookLoadPackage, IXposedHookInitPackageResources
{
	public interface ClassChecker
	{
		boolean exists(String className);
	}

	protected static final String ANY = "";

	private String mLogTag = null;

	private static final ApkVariant[] VARIANTS = {
		new HTC(),
		new Samsung(),
		new Sony.JellyBean(),
		new Sony.KitKat(),
		new LG(),
		new CyanogenMod(),
		new OmniROM(),
		new Oppo(),
		new AOSP() // must be the last entry!
	};

	public static boolean isSettingsPackage(String packageName)
	{
		for(ApkVariant variant : VARIANTS)
		{
			for(String pkg : variant.targetPackages())
			{
				if(packageName.equals(pkg))
					return true;
			}
		}

		return false;
	}

	public static boolean isSettingsPackage(LoadPackageParam lpparam) {
		return isSettingsPackage(lpparam.packageName);
	}

	public static List<ApkVariant> getAllMatching(LoadPackageParam lpparam) {
		return getAllMatching(lpparam.appInfo, new ClassCheckerWithLoader(lpparam.classLoader));
	}

	public static List<ApkVariant> getAllMatching(String packageName)
	{
		final ApplicationInfo appInfo = XUtils.getApplicationInfo(packageName);
		if(appInfo == null)
			return Collections.emptyList();

		return getAllMatching(appInfo, new ClassCheckerWithApk(appInfo.sourceDir));
	}

	private static List<ApkVariant> getAllMatching(ApplicationInfo appInfo, ClassChecker classChecker)
	{
		List<ApkVariant> variants = new ArrayList<ApkVariant>();

		for(ApkVariant variant : VARIANTS)
		{
			if(variant.isMatching(appInfo, classChecker))
				variants.add(variant);
		}

		return variants;
	}

	@Override
	public abstract void handleLoadPackage(LoadPackageParam lpparam) throws Throwable;

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		// empty
	}

	public boolean canUseLayoutFix() {
		return true;
	}

	protected Object onCreateAppOpsHeader(Context context, int addAfterHeaderId)
	{
		final Header appOpsHeader = new Header();
		appOpsHeader.title = getAppOpsTitle();
		appOpsHeader.id = R.id.app_ops_settings;
		appOpsHeader.iconRes = getAppOpsHeaderIcon();
		appOpsHeader.fragment = AppOpsXposed.APP_OPS_FRAGMENT;

		return appOpsHeader;
	}

	/**
	 * The package names of the target APKs.
	 */
	protected String[] targetPackages() {
		return new String[] { "com.android.settings" };
	}

	protected int getAppOpsHeaderIcon() {
		return Res.appOpsLauncherIcon;
	}

	protected String getAppOpsTitle()
	{
		final int appOpsTitleId = Res.getSettingsIdentifier("string/app_ops_setting");
		if(appOpsTitleId != 0)
			return Res.getSettingsString(appOpsTitleId);

		return Res.getModString(R.string.app_ops_settings);
	}

	protected String getAppOpsDetailsFragmentName() {
		return AppOpsXposed.APP_OPS_DETAILS_FRAGMENT;
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
	 * Checks if the {@link ApkVariant} matches the current configuration.
	 * <p>
	 * This method is only called if all other property checks ({@link #manufacturer()},
	 * {@link #apiLevel()}, etc.) succeeded.
	 */
	protected boolean onMatch(ApplicationInfo appInfo, ClassChecker classChecker) {
		return true;
	}

	protected void hookLoadHeadersFromResource(LoadPackageParam lpparam, String className, final int[] hookResIds, final int addAfterHeaderId) throws Throwable
	{
		hookLoadHeadersFromResource(lpparam, className, new XC_MethodHook() {

				@SuppressWarnings("unchecked")
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable
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

	protected final void hookLoadHeadersFromResource(LoadPackageParam lpparam, int hookResId, int addAfterHeaderId) throws Throwable {
		hookLoadHeadersFromResource(lpparam, "com.android.settings.Settings", new int[] { hookResId }, addAfterHeaderId);
	}

	protected final void hookLoadHeadersFromResource(LoadPackageParam lpparam, String className, XC_MethodHook hook) throws Throwable
	{
		XUtils.findAndHookMethodRecursive(className, lpparam.classLoader,
				"loadHeadersFromResource", int.class, List.class, hook);
	}

	private static final String[] VALID_FRAGMENTS = {
		AppOpsXposed.APP_OPS_FRAGMENT, AppOpsXposed.APP_OPS_DETAILS_FRAGMENT, HTC.APP_OPS_DETAILS_FRAGMENT
	};

	protected static void hookIsValidFragment(Class<?> clazz) throws Throwable
	{
		try
		{
			XUtils.findAndHookMethodRecursive(clazz, "isValidFragment",
					String.class, new XC_MethodHook() {

						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable
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

	public static void hookIsValidFragment(LoadPackageParam lpparam) throws Throwable {
		hookIsValidFragment(lpparam, "com.android.settings.Settings");
	}

	public static void hookIsValidFragment(LoadPackageParam lpparam, String className) throws Throwable {
		hookIsValidFragment(lpparam.classLoader.loadClass(className));
	}

	protected void addMenuToAppOpsSummary(LoadPackageParam lpparam) throws Throwable
	{
		XUtils.findAndHookMethodRecursive(AppOpsXposed.APP_OPS_FRAGMENT, lpparam.classLoader,
				"onCreate", Bundle.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{
						((Fragment) param.thisObject).setHasOptionsMenu(true);
					}
		});

		XUtils.findAndHookMethodRecursive(AppOpsXposed.APP_OPS_FRAGMENT, lpparam.classLoader,
				"onCreateOptionsMenu", Menu.class, MenuInflater.class, new XC_MethodHook() {

					@Override
					protected void afterHookedMethod(final MethodHookParam param) throws Throwable
					{
						final Menu menu = (Menu) param.args[0];
						menu.add(Res.getModString(R.string.show_changed_only_title))
								.setOnMenuItemClickListener(new OnMenuItemClickListener() {

									@Override
									public boolean onMenuItemClick(MenuItem item)
									{
										final Fragment f = (Fragment) param.thisObject;
										((PreferenceActivity) f.getActivity()).startPreferenceFragment(
												AppListFragment.newInstance(true), true);
										return true;
									}
						});
					}
		});

		// Without this, an orientation change crashes the settings app,
		// because its class loader fails to find AppListFragment.
		XposedBridge.hookAllMethods(Fragment.class, "instantiate", new XC_MethodHook() {

			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable
			{
				if(param.args[1].equals(AppListFragment.class.getName()))
				{
					final AppListFragment f = new AppListFragment();
					if(param.args.length >= 3)
						f.setArguments((Bundle) param.args[2]);
					param.setResult(f);
				}
			}
		});
	}

	protected void addAppOpsToAppInfo(LoadPackageParam lpparam)
	{
		XposedHelpers.findAndHookMethod("com.android.settings.applications.InstalledAppDetails", lpparam.classLoader,
				"onCreateOptionsMenu", Menu.class, MenuInflater.class, new XC_MethodHook() {

					@Override
					protected void afterHookedMethod(final MethodHookParam param) throws Throwable
					{
						final Menu menu = (Menu) param.args[0];
						final MenuItem item = menu.add(getAppOpsTitle());
						item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
						item.setIcon(Res.modRes.getDrawable(R.mipmap.ic_launcher2));
						item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

							@Override
							public boolean onMenuItemClick(MenuItem item)
							{
								final Fragment f = (Fragment) param.thisObject;
								final PreferenceActivity pa = (PreferenceActivity) f.getActivity();

								debug("onMenuItemClick:\n" +
										"  intent=" + pa.getIntent() + "\n" +
										"  extras=" + pa.getIntent().getExtras() + "\n" +
										"  args=" + f.getArguments());

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
										log("No package in intent or args:\n" +
												"  intent=" + pa.getIntent() + "\n" +
												"  extras=" + pa.getIntent().getExtras() + "\n" +
												"  args=" + args);
										Toast.makeText(pa, "Error!", Toast.LENGTH_SHORT).show();
										return true;
									}

									args.putString("package", pkg);
								}
								else
									log("Package obtained from Fragment args: " + args.getString("package"));

								if("android.settings.APPLICATION_DETAILS_SETTINGS".equals(pa.getIntent().getAction()))
								{
									debug("Launching AppOps using startActivities()");

									final Intent intent = new Intent("android.settings.SETTINGS");
									intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, getAppOpsDetailsFragmentName());
									intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
									intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_TITLE, Res.getModString(R.string.app_ops_settings));

									final TaskStackBuilder tsb = TaskStackBuilder.create(pa);
									tsb.addNextIntent(intent);

									tsb.startActivities();
								}
								else
								{
									debug("Launching AppOps using startPreferencePanel()");

									// This method only works when "App info" was opened from "Apps". When the launcher icon
									// onto "App info", the method above is used.
									pa.startPreferencePanel(getAppOpsDetailsFragmentName(), args, 0,
											Res.getModString(R.string.app_ops_settings), f, 0);
								}

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

		final Object appOpsHeader = onCreateAppOpsHeader(context, addAfterHeaderId);

		if(addAfterHeaderIndex != -1)
			headers.add(addAfterHeaderIndex + 1, appOpsHeader);
		else
		{
			debug("addAppOpsHeader: appending to header list!");
			headers.add(appOpsHeader);
		}
	}

	protected final void log(String message) {
		Util.log(getLogPrefix() + message);
	}

	protected final void debug(String message) {
		Util.debug(getLogPrefix() + message);
	}

	protected final void log(Throwable t) {
		Util.log(t);
	}

	protected final void debug(Throwable t) {
		Util.debug(t);
	}

	private boolean isMatching(ApplicationInfo appInfo, ClassChecker classChecker)
	{
		boolean havePackageMatch = false;

		for(String packageName : targetPackages())
		{
			if(appInfo.packageName.equals(packageName))
				havePackageMatch = true;
		}

		if(!havePackageMatch)
			return false;

		if(manufacturer() != ANY && !Util.containsManufacturer(manufacturer()))
			return false;

		if(release() != ANY && !Build.VERSION.RELEASE.equals(release()))
			return false;

		if(buildId() != ANY && !Build.ID.equals(buildId()))
			return false;

		try
		{
			if(md5Sum() != ANY && !XposedHelpers.getMD5Sum(appInfo.sourceDir).equals(md5Sum()))
				return false;
		}
		catch(IOException e)
		{
			log("Failed to get MD5 hash of " + appInfo.sourceDir);
			debug(e);
			return false;
		}

		if(apiLevel() != 0 && Build.VERSION.SDK_INT != apiLevel())
			return false;

		if(!onMatch(appInfo, classChecker))
			return false;

		final String[] classes = indicatorClasses();
		if(classes == null || classes.length == 0)
			return true;

		debug("Checking " + classes.length + " indicator classes");

		for(String className : classes)
		{
			if(classChecker.exists(className))
			{
				debug("  OK: " + className);
				return true;
			}
			else
			{
				debug("  N/A: " + className);
				// continue
			}
		}

		return false;
	}

	private String getLogPrefix()
	{
		if(mLogTag == null)
		{
			final String name = getClass().getName().replace('$', '.');
			final String pkgName = getClass().getPackage().getName();

			if(name.startsWith(pkgName))
				mLogTag = "AOX:" + name.substring(pkgName.length() + 1) + ": ";
			else
				mLogTag = "AOX:" + name + ": ";
		}

		return mLogTag;
	}
}

class ClassCheckerWithApk implements ApkVariant.ClassChecker
{
	private final Set<String> mClassSet;

	public ClassCheckerWithApk(String apkFile)
	{
		mClassSet = new HashSet<String>();

		try
		{
			final Enumeration<String> classes = new DexFile(apkFile).entries();
			while(classes.hasMoreElements())
				mClassSet.add(classes.nextElement());

		}
		catch(IOException e)
		{
			Util.log(e);
		}
	}

	@Override
	public boolean exists(String className) {
		return mClassSet.contains(className);
	}
}

class ClassCheckerWithLoader implements ApkVariant.ClassChecker
{
	private final ClassLoader mCl;

	ClassCheckerWithLoader(ClassLoader classLoader) {
		mCl = classLoader;
	}

	@Override
	public boolean exists(String className)
	{
		try
		{
			mCl.loadClass(className);
			return true;
		}
		catch(ClassNotFoundException e)
		{
			return false;
		}
	}
}
