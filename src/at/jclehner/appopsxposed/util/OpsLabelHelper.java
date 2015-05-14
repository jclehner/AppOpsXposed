package at.jclehner.appopsxposed.util;

import java.lang.reflect.Field;
import java.util.Locale;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.util.Log;
import android.util.SparseArray;
import at.jclehner.appopsxposed.AppOpsXposed;
import at.jclehner.appopsxposed.R;

public class OpsLabelHelper
{
	private static boolean sTryLoadOpNames = true;
	private static String[] sOpLabels;
	private static String[] sOpSummaries;

	public static CharSequence getPermissionLabel(Context context, String permission)
	{
		try
		{
			final PackageManager pm = context.getPackageManager();
			return pm.getPermissionInfo(permission, 0).loadLabel(pm);
		}
		catch(NameNotFoundException e)
		{
			return permission;
		}
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
		final boolean hasFakeBootCompleted = AppOpsManagerWrapper.hasFakeBootCompletedOp();
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
		}

		final String[] ret = new String[maxOp + 1];

		for(int op = 0; op != ret.length; ++op)
			ret[op] = strings.get(op, AppOpsManagerWrapper.opToName(op));

		return ret;
	}

	private static String getAppOpsString(Context context, String opName, boolean getLabel)
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
			if(getLabel)
			{
				final String label = getAppOpsString(context, opName, false);
				return label != null ? Util.capitalizeFirst(label) : null;
			}

			//Log.d("AOX", "No such id: " + id);

			return null;
		}

		return context.getString(resId);
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
		else if(opName == null)
		{
			try
			{
				opName = "OP_" + AppOpsManagerWrapper.opToName(op);
			}
			catch(RuntimeException e)
			{
				opName = "OP_#" + op;
			}
		}

		String[] array = getLabel ? sOpLabels : sOpSummaries;

		if(array == null && sTryLoadOpNames)
		{
			try
			{
				final Resources r = context.getPackageManager()
						.getResourcesForApplication(AppOpsXposed.SETTINGS_PACKAGE);

				final String idName =
						AppOpsXposed.SETTINGS_PACKAGE + ":array/app_ops_" +
						(getLabel ? "labels" : "summaries");

				final int id = r.getIdentifier(idName, null, null);

				final int opsCount = getOpValue("_NUM_OP");
				sTryLoadOpNames = opsCount != -1;

				if(sTryLoadOpNames)
				{
					array = r.getStringArray(id);

					if(array.length != opsCount)
					{
						// If the array length doesn't match, it could mean that the known
						// ops defined in AppOpsManager aren't in sync with the Settings app.
						// Since the order of ops cannot be guaranteed in that case, ignore
						// the array.

						Util.log("Length mismatch in " + idName + ": "
								+ array.length + " vs _NUM_OP " + opsCount);

						sTryLoadOpNames = false;
					}
					else
					{
						if(getLabel)
							sOpLabels = array;
						else
							sOpSummaries = array;
					}
				}
			}
			catch(NameNotFoundException e)
			{
				sTryLoadOpNames = false;
			}
			catch(Resources.NotFoundException e)
			{
				sTryLoadOpNames = false;
			}
		}

		if(array == null || op >= array.length)
		{
			if(op != -1)
			{
				final String permission = AppOpsManagerWrapper.opToPermission(op);
				if(permission != null)
					return getPermissionLabel(context, permission).toString();
			}

			return opName;
		}

		return array[op];
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
}