package at.jclehner.appopsxposed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import at.jclehner.appopsxposed.variants.Sony;

public class LauncherActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		final Intent intent = new Intent();

		if(false)
		{
			Log.i("AppOpsXposed", "Using Sony-specific Intent");
			intent.setClassName(AppOpsXposed.SETTINGS_PACKAGE, Sony.SLAVE_ACTIVITY_NAME);
			intent.putExtra(Sony.EXTRA_LAUNCH_APP_OPS, true);
		}
		else
		{
			Log.i("AppOpsXposed", "Using standard Intent");
			intent.setAction("android.settings.SETTINGS");
			intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AppOpsXposed.APP_OPS_FRAGMENT);
		}

		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

		startActivity(intent);
		finish();
	}
}
