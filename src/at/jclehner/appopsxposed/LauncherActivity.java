package at.jclehner.appopsxposed;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class LauncherActivity extends Activity implements OnClickListener
{
	private static final String TAG = "AppOpsXposed";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if(!isXposedInstalled())
		{
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
			{
				Log.i(TAG, "Xposed Framework not installed, but running pre-KitKat Android");

				if(isSonyStockRom())
				{
					Log.i(TAG, "Running Sony stock ROM");
					final AppListFragment f = new AppListFragment();
					getFragmentManager().beginTransaction().add(android.R.id.content, f).commit();
					// don't call launchAppOpsSummary() as it won't work
					return;
				}
			}
			else
			{
				setContentView(R.layout.launcher_info);
				findViewById(R.id.btn_launch).setOnClickListener(this);
				findViewById(R.id.btn_help).setOnClickListener(this);
				((TextView) findViewById(R.id.info_text)).setText(getString(R.string.xposed_not_installed,
						getString(R.string.app_ops_settings), getString(R.string.btn_launch)));

				return;
			}
		}

		launchAppOpsSummary();
	}

	@Override
	public void onClick(View v)
	{
		if(v.getId() == R.id.btn_launch)
			launchAppOpsSummary();
		else if(v.getId() == R.id.btn_help)
		{
			final Uri uri = Uri.parse("http://repo.xposed.info/module/de.robv.android.xposed.installer");
			final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
		}
	}

	private void launchAppOpsSummary()
	{
		final Intent intent = new Intent();
		intent.setAction("android.settings.SETTINGS");
		intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AppOpsXposed.APP_OPS_FRAGMENT);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

		startActivity(intent);
		finish();
	}

	private boolean isSonyStockRom()
	{
		if(!Util.containsManufacturer("Sony"))
			return false;

		final String[] permissions = {
				"com.sonyericsson.r2r.client.permission.START_R2R",
				"com.sonyericsson.permission.IDD"
		};

		final PackageManager pm = getPackageManager();

		for(String permission : permissions)
		{
			final int status = pm.checkPermission(permission, AppOpsXposed.SETTINGS_PACKAGE);
			if(status == PackageManager.PERMISSION_GRANTED)
				return true;
		}

		for(String entry : new File("/system/framework").list())
		{
			// matches both com.sonymobile and com.sonyericsson
			if(entry.startsWith("com.sony"))
				return true;
		}

		return false;
	}

	private boolean isXposedInstalled()
	{
		try
		{
			getPackageManager().getApplicationInfo("de.robv.android.xposed.installer", 0);
			return true;
		}
		catch(NameNotFoundException e)
		{
			return false;
		}
	}
}
