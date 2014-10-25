package at.jclehner.appopsxposed.util;

import java.lang.reflect.Field;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.util.Log;
import at.jclehner.appopsxposed.AppOpsXposed;

public class OpsLabelHelper
{
	private static final String TAG = "AOX:OpsResolver";

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

						Log.w(TAG, "Length mismatch in " + idName + ": "
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