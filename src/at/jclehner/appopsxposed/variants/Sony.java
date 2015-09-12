/*
 * AppOpsXposed - AppOps for Android 4.3+
 * Copyright (C) 2013, 2014 Joseph C. Lehner
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


import at.jclehner.appopsxposed.util.Constants;


/*
 * The Settings app on Sony devices (Xperia ROMs at least) is surprisingly
 * vanilla, meaning we can use AOSP.handleLoadPackage().
 *
 * However, Sony decided to make the following modifications:
 *
 * 1) AppOpsSummary can't be launched using the PreferenceActivity.SHOW_FRAGMENT
 *    extra since the hosting Activity is finish()'ed in AppOpsSummary.onCreateView()
 *
 * 2) On JellyBean, in layout/app_ops_details_item, the Switch has been replaced by a
 *    Spinner offering three (!) choices, one of them being "Ask always".
 *
 * The first issue is dealt with by hooking Activity.finish() for the duration of the
 * call to AppOpsSummary.onCreateView(). The second issue is addressed by hooking into
 * LayoutInflater.inflate() in AppOpsDetails.refreshUi, where we can hide the Switch
 * and show the Spinner.
 */
public abstract class Sony extends AOSP
{
	// Keep these for now, so we don't break the "force_variant" setting

	public static class JellyBean extends Sony
	{
		@Override
		protected int apiLevel() {
			return 18;
		}
	}

	public static class KitKat extends Sony
	{
		@Override
		protected int apiLevel() {
			return 0;
		}
	}

	@Override
	protected abstract int apiLevel();

	@Override
	protected String manufacturer() {
		return "Sony";
	}

	@Override
	protected int getDefaultAppOpsHeaderIcon() {
		return Constants.ICON_LAUNCHER;
	}

	@Override
	protected String[] indicatorClasses()
	{
		final String[] classes = {
				"com.sonymobile.settings.SomcSettingsHeader",
				"com.sonymobile.settings.preference.util.SomcPreferenceActivity",
				"com.sonymobile.settings.preference.util.SomcSettingsPreferenceFragment"
		};

		return classes;
	}
}
