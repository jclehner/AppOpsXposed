/*
 * AppOpsXposed - AppOps for Android 4.3+
 * Copyright (C) 2013-2015 Joseph C. Lehner
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
import android.preference.PreferenceActivity.Header;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.util.Constants;
import at.jclehner.appopsxposed.util.Util;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Oppo extends AOSP
{
	private boolean mForceCompatibilityMode = true;

	@Override
	protected String manufacturer() {
		return "OPPO";
	}

	@Override
	protected String[] indicatorClasses()
	{
		return new String[] {
				"com.oppo.settings.SettingsActivity"
		};
	}

	@Override
	protected int getDefaultAppOpsHeaderIcon() {
		return Constants.ICON_LAUNCHER;
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable
	{
		try
		{
			lpparam.classLoader.loadClass(AppOpsXposed.APP_OPS_FRAGMENT);
			mForceCompatibilityMode = false;
		}
		catch(ClassNotFoundException e)
		{
			log("No " + AppOpsXposed.APP_OPS_FRAGMENT + " in " + lpparam.packageName);
		}

		super.handleLoadPackage(lpparam);
	}

	@Override
	protected Object onCreateAppOpsHeader(Context context, int addAfterHeaderId)
	{
		final Header header = (Header) super.onCreateAppOpsHeader(context, addAfterHeaderId);
		if(mForceCompatibilityMode)
		{
			header.fragment = null;
			header.intent = Util.getCompatibilityModeIntent(null);
		}
		return header;
	}
}
