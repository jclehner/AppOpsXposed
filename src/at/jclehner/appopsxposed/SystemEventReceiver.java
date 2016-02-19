package at.jclehner.appopsxposed;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import at.jclehner.appopsxposed.util.Util;

public class SystemEventReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		if(intent.getData() != null && "package".equals(intent.getData().getScheme()))
			showPackageNotification(context, intent);
	}

	private void showPackageNotification(Context context, Intent intent)
	{
		final SharedPreferences sp = Util.getSharedPrefs(context);
		if(!sp.getBoolean("show_pkg_notifications", true) || !Util.isXposedModuleOrSystemApp(context))
			return;

		final PackageManager pm = context.getPackageManager();
		final PackageInfo pi = getPkgInfo(context, intent);
		if(pi == null || pi.applicationInfo.packageName.equals(context.getPackageName()))
			return;

		final Notification.Builder nb = new Notification.Builder(context);
		nb.setLargeIcon(drawableToBitmap(pi.applicationInfo.loadIcon(pm)));
		nb.setSmallIcon(R.drawable.ic_appops_cog_white);
		nb.setContentTitle(context.getString(R.string.package_updated));
		nb.setContentText(pi.applicationInfo.loadLabel(pm) + " " + pi.versionName);
		nb.setAutoCancel(true);
		nb.setPriority(Notification.PRIORITY_LOW);
		nb.setContentIntent(PendingIntent.getActivity(context, 0,
				Util.createAppOpsIntent(pi.packageName), PendingIntent.FLAG_CANCEL_CURRENT));

		final NotificationManager nm =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(pi.applicationInfo.uid, nb.build());
	}

	private PackageInfo getPkgInfo(Context context, Intent intent)
	{
		final Uri data = intent.getData();
		if(data != null)
		{
			try
			{
				return context.getPackageManager().getPackageInfo(
						data.getEncodedSchemeSpecificPart(), 0);
			}
			catch(PackageManager.NameNotFoundException e)
			{
				// ignore
			}
		}
		return null;
	}

	private static Bitmap drawableToBitmap(Drawable drawable)
	{
		if(drawable instanceof BitmapDrawable)
			return ((BitmapDrawable) drawable).getBitmap();

		int width = drawable.getIntrinsicWidth();
		width = width > 0 ? width : 1;
		int height = drawable.getIntrinsicHeight();
		height = height > 0 ? height : 1;

		final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}
}
