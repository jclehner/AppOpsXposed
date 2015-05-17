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

package at.jclehner.appopsxposed;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcelable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper;
import at.jclehner.appopsxposed.util.Util;
import eu.chainfire.libsuperuser.Shell;
import eu.chainfire.libsuperuser.Shell.SU;

public class BugReportBuilder
{
	private Context mContext;
	private String mDeviceId;
	private String mReportTime;
	private File mBugReportDir;
	private File mBugReportFile;

	public static void buildAndSend(final Context context)
	{
		if(!SU.available())
		{
			Toast.makeText(context, R.string.toast_needs_root,
					Toast.LENGTH_SHORT).show();
			return;
		}

		Toast.makeText(context, R.string.building_toast,
				Toast.LENGTH_LONG).show();

		final BugReportBuilder brb = new BugReportBuilder(context);

		new AsyncTask<Void, Void, Uri>() {

			@Override
			protected Uri doInBackground(Void... params)
			{
				return brb.build();
			}

			@Override
			protected void onPostExecute(Uri result)
			{
				final ArrayList<Parcelable> uris = new ArrayList<Parcelable>();
				uris.add(result);


				final Intent target = new Intent(Intent.ACTION_SEND_MULTIPLE);
				target.setType("text/plain");
				target.putExtra(Intent.EXTRA_SUBJECT, "[REPORT][AppOpsXposed " + Util.getAoxVersion(context) + "] " + Build.FINGERPRINT);
				target.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
				//target.putExtra(Intent.EXTRA_STREAM, result);
				target.putExtra(Intent.EXTRA_TEXT, "!!! BUG REPORTS WITHOUT ADDITIONAL INFO WILL BE IGNORED !!!");
				//target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

				final Intent intent = Intent.createChooser(target, null);
				//intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

				context.startActivity(intent);
			}
		}.execute();
	}

	public BugReportBuilder(Context context)
	{
		mContext = context;
		mDeviceId = getDeviceId();
		mReportTime = getReportTime();
		mBugReportDir = new File(context.getCacheDir(), "reports");
		mBugReportFile = new File(mBugReportDir, "report_" + mDeviceId
				+ "_" + mReportTime + ".txt");
	}

	public Uri build()
	{
		final StringBuilder sb = new StringBuilder();

		sb.append("\nAOX : " + Util.getAoxVersion(mContext));
		sb.append("\nTIME: " + mReportTime);
		sb.append("\nID  : " + mDeviceId);

		collectDeviceInfo(sb);
		collectApkInfo(sb);
		collectAppOpsInfos(sb);

		Log.d("AOX", "-------------------------");
		Log.d("AOX", sb.toString());
		Log.d("AOX", "\n-------------------------");

		collectXposedLogs(sb);
		collectProps(sb);
		collectLogcat(sb);

		PrintWriter pw = null;

		try
		{
			if(!mBugReportDir.mkdirs() && !mBugReportDir.isDirectory())
				throw new RuntimeException("Failed to create " + mBugReportDir);

			pw = new PrintWriter(mBugReportFile);
			pw.println(sb);
		}
		catch(FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			if(pw != null)
				pw.close();
		}

		return FileProvider.getUriForFile(mContext, "at.jclehner.appopsxposed.files", mBugReportFile);
	}

	public Uri getBugReportFileUri() {
		return Uri.fromFile(mBugReportFile);
	}

	@SuppressLint("SimpleDateFormat")
	private String getReportTime()
	{
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		return sdf.format(new Date());
	}

	private String getDeviceId()
	{
		final String raw = Build.SERIAL + Build.FINGERPRINT;

		byte[] bytes;

		try
		{
			final MessageDigest md = MessageDigest.getInstance("SHA1");
			bytes = md.digest(raw.getBytes());
		}
		catch(NoSuchAlgorithmException e)
		{
			bytes = raw.getBytes();
		}

		final StringBuilder sb = new StringBuilder(bytes.length);
		for(byte b : bytes)
			sb.append(Integer.toString((b & 0xff ) + 0x100, 16).substring(1));

		return sb.substring(0, 12);
	}

	private void collectDeviceInfo(StringBuilder sb)
	{
		sb.append("\n---------------------------------------------------");
		sb.append("\n------------------- DEVICE INFO -------------------");
		sb.append("\nAndroid version: " + Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")");
		sb.append("\nFingerprint    : " + Build.FINGERPRINT);
		sb.append("\nDevice name    : " + Build.MANUFACTURER + " " + Build.MODEL
				+ " (" + Build.PRODUCT + "/" + Build.HARDWARE + ")");
		sb.append("\nAppOpsManager  : ");

		try
		{
			Class.forName("android.app.AppOpsManager");
			sb.append("YES");
		}
		catch(ClassNotFoundException e)
		{
			sb.append("NO!");
		}
	}

	private void collectApkInfo(StringBuilder sb)
	{
		sb.append("\n---------------------------------------------------");
		sb.append("\n--------------------- APK INFO --------------------");

		final Intent intent = new Intent();
		intent.setAction("android.settings.SETTINGS");

		final HashMap<String, List<String>> appMap = new HashMap<String, List<String>>();

		final List<ResolveInfo> rInfos = mContext.getPackageManager().queryIntentActivities(intent,
				PackageManager.GET_DISABLED_COMPONENTS);
		for(ResolveInfo rInfo : rInfos)
		{
			final ActivityInfo aInfo = rInfo.activityInfo;
			final String key = aInfo.applicationInfo.sourceDir + " (" + aInfo.packageName + ")"
					+ toTickedBox(rInfo.activityInfo.applicationInfo.enabled);

			final List<String> activityList;

			if(!appMap.containsKey(key))
			{
				activityList = new ArrayList<String>();
				appMap.put(key, activityList);
			}
			else
				activityList = appMap.get(key);

			activityList.add(aInfo.name + toTickedBox(aInfo.enabled));
		}

		for(String key : appMap.keySet())
		{
			sb.append("\n" + key);
			for(String activity : appMap.get(key))
				sb.append("\n  " + activity);
		}
	}

	private void collectAppOpsInfos(StringBuilder sb)
	{
		sb.append("\n---------------------------------------------------");
		sb.append("\n------------------- APPOPS INFO -------------------\n");

		sb.append("\nMODES:\n");

		for(int mode : AppOpsManagerWrapper.getAllValidModes())
			sb.append("  " + AppOpsManagerWrapper.modeToName(mode) + " = " + mode + "\n");

		sb.append("\n_NUM_OP: " + AppOpsManagerWrapper._NUM_OP + "\n");

		if(AppOpsManagerWrapper._NUM_OP > 0)
		{
			final TreeMap<Integer, ArrayList<String>> switches = new TreeMap<>();

			for(int op : AppOpsManagerWrapper.getAllValidOps())
			{
				final int switchOp = AppOpsManagerWrapper.opToSwitch(op);

				ArrayList<String> list = switches.get(switchOp);
				if(list == null)
					switches.put(switchOp, list = new ArrayList<>());

				String s = "OP_" + AppOpsManagerWrapper.opToName(op) + " = " + op;

				final int mode = AppOpsManagerWrapper.opToDefaultMode(op);
				if(mode != AppOpsManagerWrapper.MODE_ALLOWED)
					s += " (def: " + AppOpsManagerWrapper.modeToName(mode) + ")";

				list.add(s);
			}

			for(int switchOp : switches.keySet())
			{
				final ArrayList<String> list = switches.get(switchOp);
				if(list.size() > 1)
				{
					sb.append("  SWITCH " + AppOpsManagerWrapper.opToName(switchOp) + " = " + switchOp + "\n");
					for(String line : list)
						sb.append("    " + line + "\n");

				}
				else
					sb.append("  " + list.get(0) + "\n");
			}
		}
	}

	private void collectXposedLogs(StringBuilder sb)
	{
		sb.append("\n---------------------------------------------------");
		sb.append("\n------------------- XPOSED LOGS -------------------\n");
		runAsRoot(sb, "cat /data/data/de.robv.android.xposed.installer/log/error.log");
	}

	private void collectProps(StringBuilder sb)
	{
		sb.append("\n---------------------------------------------------");
		sb.append("\n-------------------- BUILD.PROP -------------------\n");
		runAsRoot(sb, "cat /system/build.prop");
	}

	private void collectLogcat(StringBuilder sb)
	{
		sb.append("\n---------------------------------------------------");
		sb.append("\n---------------------- LOGCAT ---------------------\n");
		runAsRoot(sb, "logcat -d -v time");
	}

	private void runAsRoot(StringBuilder sb, String command)
	{
		for(String line : Shell.SU.run(command))
			sb.append(line + "\n");
	}

	private static String toTickedBox(boolean b) {
		return b ? " [*]" : " [ ]";
	}
}
