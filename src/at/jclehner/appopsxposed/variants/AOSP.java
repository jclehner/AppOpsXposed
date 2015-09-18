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

package at.jclehner.appopsxposed.variants;

import java.util.List;

import android.content.Context;
import android.os.Build;
import android.preference.PreferenceActivity.Header;
import at.jclehner.appopsxposed.ApkVariant;
import at.jclehner.appopsxposed.R;
import at.jclehner.appopsxposed.util.Constants;
import at.jclehner.appopsxposed.util.Res;
import at.jclehner.appopsxposed.util.Util;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AOSP extends ApkVariant
{
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		addAppOpsToAppInfo(lpparam);

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
			addAppOpsHeader(lpparam);
		else
			addAppOpsDashboardTile(lpparam);
	}

	@Override
	protected int getDefaultAppOpsHeaderIcon() {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
				? Constants.ICON_SHIELD_WHITE : Constants.ICON_SHIELD_TEAL;
	}

	private void addAppOpsHeader(LoadPackageParam lpparam)
	{
		final int settingsHeadersId = Res.getSettingsIdentifier("xml/settings_headers");
		final int personalSectionId = Res.getSettingsIdentifier("id/personal_section");

		if(settingsHeadersId != 0)
		{
			try
			{
				hookLoadHeadersFromResource(lpparam, settingsHeadersId, personalSectionId);
				return;
			}
			catch(Throwable t)
			{
				debug(t);
			}
		}

		// This is a last resort only, since we might end up with multiple occurences of
		// "App ops" within settings, which is ugly.

		log("Hooking onBuildHeaders :-(");

		XposedHelpers.findAndHookMethod("com.android.settings.Settings", lpparam.classLoader,
				"onBuildHeaders", List.class, new XC_MethodHook() {
					@SuppressWarnings("unchecked")
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{
						addAppOpsHeader((List<Header>) param.args[0], personalSectionId, (Context) param.thisObject);
					}
		});
	}

	@SuppressWarnings("RawTypes")
	private void addAppOpsDashboardTile(LoadPackageParam lpparam) throws Throwable
	{
		final Class<?> tileClazz = lpparam.classLoader.loadClass(
				"com.android.settings.dashboard.DashboardTile");

		final int categoriesXmlResId = Res.getSettingsIdentifier("xml/dashboard_categories");
		final int personalSectionResId = Res.getSettingsIdentifier("id/personal_section");

		XposedHelpers.findAndHookMethod("com.android.settings.SettingsActivity",
				lpparam.classLoader, "loadCategoriesFromResource", int.class, List.class,
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable
					{
						if(categoriesXmlResId != (int) param.args[0])
							return;

						for(Object category : (List<?>) param.args[1])
						{
							if(personalSectionResId == XposedHelpers.getLongField(category, "id"))
							{
								final Object tile = tileClazz.newInstance();
								XposedHelpers.setLongField(tile, "id", R.id.app_ops_settings);
								XposedHelpers.setIntField(tile, "iconRes", getAppOpsHeaderIcon());
								XposedHelpers.setObjectField(tile, "title", getAppOpsTitle());
								XposedHelpers.setObjectField(tile, "intent", Util.createAppOpsIntent(null));

								// Try three approaches: addTile(int, DashboardTile) may not be
								// available, so we try to add it to the list managed by this
								// DashboardCategory. If that fails, addTile() should work, but
								// adds our entry at the end of the list.

								for(int i = 0; i != 3; ++i)
								{
									try
									{
										if(i == 0)
										{
											XposedHelpers.callMethod(category, "addTile",
													new Class<?>[] { int.class, tileClazz },
													0, tile);
										}
										else if(i == 1)
										{
											((List) XposedHelpers.getObjectField(category, "tiles"))
													.add(0, tile);
										}
										else
										{
											XposedHelpers.callMethod(category, "addTile", tile);
										}

										break;
									}
									catch(Throwable t)
									{
										debug(t);
									}
								}

								return;
							}
						}
					}
		});

	}
}
