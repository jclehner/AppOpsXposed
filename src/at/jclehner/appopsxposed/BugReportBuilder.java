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
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v4.content.FileProvider;
import android.widget.Toast;
import eu.chainfire.libsuperuser.Shell;

public class BugReportBuilder
{
	private Context mContext;
	private String mDeviceId;
	private String mReportTime;
	private File mBugReportDir;
	private File mBugReportFile;

	public static void buildAndSend(final Context context)
	{
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
				//target.setData(Uri.fromParts("mailto", "joseph.c.lehner@gmail.com", null));
				target.putExtra(Intent.EXTRA_EMAIL, new String[] { "joseph.c.lehner+aox@gmail.com" });
				target.putExtra(Intent.EXTRA_SUBJECT, "[REPORT][AppOpsXposed " + Util.getAoxVersion(context) + "] " + Build.FINGERPRINT);
				target.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
				//target.putExtra(Intent.EXTRA_STREAM, result);
				target.putExtra(Intent.EXTRA_TEXT, "Please describe your issue");
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
		mDeviceId = generateDeviceId();
		mReportTime = Long.toString(System.currentTimeMillis());
		mBugReportDir = new File(context.getCacheDir(), "reports");
		mBugReportFile = new File(mBugReportDir, "report_" + mDeviceId + ".txt");
	}

	public Uri build()
	{
		final StringBuilder sb = new StringBuilder();

		sb.append("\nAOX : " + Util.getAoxVersion(mContext));
		sb.append("\nTIME: " + mReportTime);
		sb.append("\nID  : " + mDeviceId);

		collectDeviceInfo(sb);
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

	private String generateDeviceId()
	{
		final String raw =
				Build.FINGERPRINT + Build.SERIAL + Build.TIME;

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

		return sb.toString();
	}

	private void collectDeviceInfo(StringBuilder sb)
	{
		sb.append("\n---------------------------------------------------");
		sb.append("\n------------------- DEVICE INFO -------------------");
		sb.append("\nAndroid version: " + Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")");
		sb.append("\nFingerprint    : " + Build.FINGERPRINT);
		sb.append("\nDevice name    : " + Build.MANUFACTURER + " " + Build.MODEL
				+ " (" + Build.PRODUCT + "/" + Build.HARDWARE + ")");
		sb.append("\nSettings APK   : " + getSettingsAppInfo().publicSourceDir);
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
		sb.append("\n--------------------- SYSPROPS --------------------\n");
		runAsRoot(sb, "getprop");
	}

	private void collectLogcat(StringBuilder sb)
	{
		sb.append("\n---------------------------------------------------");
		sb.append("\n---------------------- LOGCAT ---------------------\n");
		runAsRoot(sb, "logcat -d -b main -v time");
	}

	private void runAsRoot(StringBuilder sb, String command)
	{
		for(String line : Shell.SU.run(command))
			sb.append(line + "\n");
	}

	private ApplicationInfo getSettingsAppInfo()
	{
		final Intent intent = new Intent();
		intent.setAction("android.settings.SETTINGS");

		final List<ResolveInfo> infos = mContext.getPackageManager().queryIntentActivities(intent, 0);
		if(infos.size() == 0)
			return null;

		return infos.get(0).activityInfo.applicationInfo;
	}
}
