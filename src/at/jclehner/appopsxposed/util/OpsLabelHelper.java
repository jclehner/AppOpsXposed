package at.jclehner.appopsxposed.util;

import java.lang.reflect.Field;
import java.util.Locale;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
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
		final boolean hasFakeBootCompleted = AppOpsManagerWrapper.isBootCompletedHackEnabled();
		int maxOp = 0;

		Log.d("AOX", "getOpLabelsOrSummaries: hasFakeBootCompleted=" + hasFakeBootCompleted);

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
					if(getLabels)
					{
						strings.append(op, context.getString(R.string.app_ops_labels_vibrate) + "/" +
								context.getString(R.string.app_ops_labels_post_notification));
					}
					else
					{
						strings.append(op, context.getString(R.string.app_ops_summaries_vibrate) + "/" +
								context.getString(R.string.app_ops_summaries_post_notification));
					}
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
				Log.d("AOX", "No ops string for " + opName);
		}

		final String[] ret = new String[maxOp + 1];

		for(int op = 0; op != ret.length; ++op)
		{
			ret[op] = strings.get(op, AppOpsManagerWrapper.opToName(op));
		}

		return ret;
	}

	private static String getAppOpsString(Context context, String opName, boolean getLabel) {
		return getAppOpsString(context, opName, getLabel, true);
	}

	private static String getAppOpsString(Context context, String opName, boolean getLabel, boolean tryOther)
	{
		final String id = "app_ops_" + (getLabel ? "labels" : "summaries") + "_" +
				opName.substring(3).toLowerCase(Locale.US);

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
			return null;
		}

		final int resId = res.getIdentifier(Constants.MODULE_PACKAGE + ":string/" + id, null, null);
		if(resId == 0)
		{
			if(tryOther)
			{
				final String other = getAppOpsString(context, opName, !getLabel, false);
				if(other != null)
					return Util.capitalizeFirst(other);
			}

			return opLabelFromPermission(context, opName);
		}

		try
		{
			return context.getString(resId);
		}
		catch(NotFoundException e)
		{
			if(tryOther)
			{
				final String other = getAppOpsString(context, opName, !getLabel, false);
				if(other != null && getLabel)
					return Util.capitalizeFirst(other);

				return other;
			}

			return opLabelFromPermission(context, opName);
		}
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

		return (getLabel ? sOpLabels : sOpSummaries)[op];
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
		final String perm = AppOpsManagerWrapper.opToPermission(op);

		if(perm != null)
			return getPermissionLabel(context, perm, null).toString();

		return null;
	}

	private static CharSequence getPermissionLabel(Context context, String permission, String defValue)
	{
		try
		{
			final PackageManager pm = context.getPackageManager();
			return pm.getPermissionInfo(permission, 0).loadLabel(pm);
		}
		catch(NameNotFoundException e)
		{
			return defValue;
		}
	}
}