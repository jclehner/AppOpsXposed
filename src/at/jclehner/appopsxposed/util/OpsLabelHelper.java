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

package at.jclehner.appopsxposed.util;

import java.lang.reflect.Field;
import java.util.Locale;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.R;

public class OpsLabelHelper
{
	private static String[] sOpLabels;
	private static String[] sOpSummaries;

	public static CharSequence getPermissionLabel(Context context, String permission) {
		return getPermissionLabel(context, permission, permission);
	}

	public static String getOpLabel(Context context, int op) {
		return getOpLabelOrSummary(context, null, op, true);
	}

	public static String getOpLabel(Context context, String opName) {
		return getOpLabelOrSummary(context, opName, -1, true);
	}

	public static String getOpSummary(Context context, int op) {
		return getOpLabelOrSummary(context, null, op, false);
	}

	public static String getOpSummary(Context context, String opName) {
		return getOpLabelOrSummary(context, opName, -1, false);
	}

	public static String[] getOpSummaries(Context context) {
		return getOpLabelsOrSummaries(context, false);
	}

	public static String[] getOpLabels(Context context) {
		return getOpLabelsOrSummaries(context, true);
	}

	private static String[] getOpLabelsOrSummaries(Context context, boolean getLabels)
	{
		final SparseArray<String> strings = new SparseArray<String>();
		final boolean hasFakeBootCompleted = Util.isBootCompletedHackWorking() &&
				AppOpsManagerWrapper.OP_POST_NOTIFICATION == AppOpsManagerWrapper.getBootCompletedOp();
		int maxOp = 0;

		for(Field field : AppOpsManagerWrapper.class.getDeclaredFields())
		{
			final String opName = field.getName();
			if(!opName.startsWith("OP_") || "OP_NONE".equals(opName))
				continue;

			final int op = AppOpsManagerWrapper.opFromName(opName);
			if(op == -1)
				continue;
			else if(op > maxOp)
				maxOp = op;

			if(hasFakeBootCompleted)
			{
				// Don't use op == because that would be true for
				// OP_BOOT_COMPLETED as well in this case
				if("OP_POST_NOTIFICATION".equals(opName))
					continue;
				else if(op == AppOpsManagerWrapper.OP_VIBRATE)
				{
					strings.append(op, context.getString(R.string.app_ops_labels_vibrate) + "/" +
							context.getString(R.string.app_ops_labels_post_notification));
					continue;
				}
				/*else if(op == AppOpsManagerWrapper.OP_BOOT_COMPLETED)
				{
					strings.append(op, context.getString(getLabels ? R.string.app_ops_labels_boot_completed :
						R.string.app_ops_summaries_boot_completed));

					Log.d("AOX", "OP_BOOT_COMPLETED!");
					continue;
				}*/
			}

			final String str = getAppOpsString(context, opName, getLabels);
			if(str != null)
				strings.append(op, str);
			else
				Util.debug("No ops string for " + opName);
		}

		final String[] ret = new String[AppOpsManagerWrapper._NUM_OP];

		for(int op = 0; op <= maxOp; ++op)
			ret[op] = strings.get(op, AppOpsManagerWrapper.opToName(op));

		if(maxOp + 1 != AppOpsManagerWrapper._NUM_OP)
		{
			for(int op = maxOp + 1; op != AppOpsManagerWrapper._NUM_OP; ++op)
			{
				final String opName = AppOpsManagerWrapper.opToName(op);
				if (opName != null)
					ret[op] = getAppOpsString(context, opName, true);

				if(ret[op] == null)
					ret[op] = opName != null ? opName : "OP #" + op;
			}
		}

		return ret;
	}

	private static String getAppOpsString(Context context, String opName, boolean getLabel)
	{
		final String str = getAppOpsString(context, opName, getLabel, true);
		return str != null ? Util.capitalizeFirst(str) : null;
	}

	private static String getAppOpsString(Context context, String opName, boolean getLabel, boolean tryOther)
	{
		if(opName.startsWith("OP_"))
			opName = opName.substring(3);

		final String id = "app_ops_" + (getLabel ? "labels" : "summaries") + "_" +
				opName.toLowerCase(Locale.US);

		final Resources res;

		try
		{
			if(Constants.MODULE_PACKAGE.equals(context.getPackageName()))
				res = context.getResources();
			else
				res = context.getPackageManager().getResourcesForApplication("at.jclehner.appopsxposed");
		}
		catch(NameNotFoundException e)
		{
			Log.w("AOX", e);
			return opName;
		}

		final int resId = res.getIdentifier(Constants.MODULE_PACKAGE + ":string/" + id, null, null);
		if(resId != 0)
		{
			try
			{
				final String str = context.getString(resId);
				if(str != null)
					return str;
			}
			catch(NotFoundException e)
			{
				//Util.debug(e);
			}

			Util.debug("Failed to get string " + id);
		}

		return getFallbackString(context, opName, getLabel, tryOther);
	}

	private static String getOpLabelOrSummary(Context context, String opName, int op, boolean getLabel)
	{
		if(opName == null && op == -1)
			throw new IllegalArgumentException("Must specify either opName or op");

		if(op == -1)
		{
			op = getOpValue(opName);
			if(op == -1)
				return opName;
		}

		if(sOpLabels == null)
		{
			sOpLabels = getOpLabels(context);
			sOpSummaries = getOpSummaries(context);
		}

		final String[] array = getLabel ? sOpLabels : sOpSummaries;
		if(op < array.length && !TextUtils.isEmpty(array[op]))
			return array[op];

		// Now that getOpLabelsOrSummaries has been fixed, this shouldn't happen
		Util.log("op #" + op + " (" + opName +") has no valid array entry");
		return opName != null ? opName : "OP #" + op;
	}

	private static String getFallbackString(Context context, String opName, boolean getLabel, boolean tryOther)
	{
		if(tryOther)
		{
			final String other = getAppOpsString(context, opName, !getLabel, false);
			if(other != null)
				return other;
		}

		final String fromPerm = opLabelFromPermission(context, opName);
		return fromPerm != null ? fromPerm : opName;
	}

	public static int getOpValue(String name)
	{
		try
		{
			final Field f = AppOpsManager.class.getField(name);
			f.setAccessible(true);
			return f.getInt(null);
		}
		catch (IllegalAccessException e)
		{
			// ignore
		}
		catch (IllegalArgumentException e)
		{
			// ignore
		}
		catch (NoSuchFieldException e)
		{
			// ignore
		}

		return -1;
	}

	private static String opLabelFromPermission(Context context, String opName)
	{
		final int op = AppOpsManagerWrapper.opFromName(opName);
		if (op == -1) {
			return null;
		}
		final String perm = AppOpsManagerWrapper.opToPermission(op);
		return perm != null ? getPermissionLabel(context, perm, null) : null;
	}

	private static String getPermissionLabel(Context context, String permission, String defValue)
	{
		try
		{
			final PackageManager pm = context.getPackageManager();
			final PermissionInfo pi = pm.getPermissionInfo(permission, 0);

			final CharSequence label = pi.loadLabel(pm);
			if(label != null && !label.toString().equals(permission))
				return label.toString();
		}
		catch(NameNotFoundException e)
		{
			// om nom nom
		}

		return defValue;
	}
}