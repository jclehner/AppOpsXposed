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

import android.content.Context;
import at.jclehner.appopsxposed.ApkVariant;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;


/*
 * HTC Sense offers a surprisingly easy method to add items to its
 * "Settings" app (kudos to Mikanoshi@XDA), by using meta-data in
 * AndroidManifest.xml.
 *
 */
public class HTC extends ApkVariant
{
	public static final String APP_OPS_DETAILS_FRAGMENT =
			"com.android.settings.framework.activity.application.appops.HtcAppOpsDetails";

	@Override
	protected String manufacturer() {
		return "HTC";
	}

	@Override
	public boolean canUseLayoutFix() {
		return false;
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		addAppOpsToAppInfo(lpparam);
	}

	@Override
	protected String[] indicatorClasses()
	{
		final String[] classes = {
				"com.htc.preference.HtcPreferenceActivity",
				"com.htc.preference.HtcPreferenceActivity$Header",
				"com.android.settings.framework.activity.HtcWrapHeader",
				"com.android.settings.framework.activity.HtcWrapHeaderList",
				"com.android.settings.framework.activity.HtcGenericEntryProvider"
		};

		return classes;
	}

	@Override
	protected String getAppOpsDetailsFragmentName()
	{
		// TODO: use HtcAppOpsDetails if available?
		return super.getAppOpsDetailsFragmentName();
	}
}
